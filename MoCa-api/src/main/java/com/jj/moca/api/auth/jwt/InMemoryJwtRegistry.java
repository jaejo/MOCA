package com.jj.moca.api.auth.jwt;

import com.jj.moca.api.auth.jwt.response.JwtInformation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JWT 토큰 세션을 메모리(In-Memory) 상에서 관리하는 레지스트리 구현체입니다.
 * 다중 로그인 제어(기기 대수 제한)를 위해 사용자당 Queue 구조를 사용하며,
 * O(1) 수준의 조회를 보장하기 위해 토큰 인덱스 맵을 별도로 관리합니다.
 * 사용자별 Striped ReentrantLock을 적용하여 멀티스레드 및 가상 스레드 환경에서도 원자성과 일관성을 보장합니다.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.jwt.registry", havingValue = "memory", matchIfMissing = true)
public class InMemoryJwtRegistry implements JwtRegistry {

    private static final int LOCK_POOL_SIZE = 128;

    // 사용자 식별자(UUID)별 활성 토큰 세션 큐 (기기 제한 관리용)
    private final Map<UUID, Queue<JwtInformation>> origin = new ConcurrentHashMap<>();

    // Access Token으로 세션 정보를 즉시 조회하기 위한 인덱스 맵
    private final Map<String, JwtInformation> tokenIndex = new ConcurrentHashMap<>();

    // Refresh Token으로 세션 정보를 즉시 조회하기 위한 인덱스 맵 (조회 최적화용)
    private final Map<String, JwtInformation> refreshTokenIndex = new ConcurrentHashMap<>();

    // 사용자별 동시성 제어를 위한 고정 크기 ReentrantLock 풀 (Striped Lock)
    private final ReentrantLock[] locks = initLocks();

    // 사용자당 허용할 최대 활성 토큰 세션 수 (중복 로그인 제한 개수)
    private final int maxActiveJwtCount;

    private final Clock clock;

    @Autowired
    public InMemoryJwtRegistry(@Value("${app.jwt.max-active-count}") int maxActiveJwtCount) {
        this(maxActiveJwtCount, Clock.systemUTC());
    }

    InMemoryJwtRegistry(int maxActiveJwtCount, Clock clock) {
        if (maxActiveJwtCount < 1) {
            throw new IllegalArgumentException(
                    "app.jwt.max-active-count는 1 이상이어야 합니다. 현재 값: " + maxActiveJwtCount
            );
        }
        this.maxActiveJwtCount = maxActiveJwtCount;
        this.clock = Objects.requireNonNull(clock, "clock은 null이 될 수 없습니다.");
    }

    private static ReentrantLock[] initLocks() {
        ReentrantLock[] lockArray = new ReentrantLock[LOCK_POOL_SIZE];
        for (int i = 0; i < LOCK_POOL_SIZE; i++) {
            lockArray[i] = new ReentrantLock();
        }
        return lockArray;
    }

    private ReentrantLock lockFor(UUID userId) {
        int index = Math.floorMod(userId.hashCode(), LOCK_POOL_SIZE);
        return locks[index];
    }

    @Override
    public void registerJwtInformation(JwtInformation jwtInformation) {
        if (jwtInformation == null) return;

        UUID userId = jwtInformation.userId();
        ReentrantLock lock = lockFor(userId);
        lock.lock();
        try {
            Queue<JwtInformation> userQueue = origin.computeIfAbsent(userId, ignored -> new ArrayDeque<>());

            removeExpired(userQueue, clock.instant());
            evictOverflow(userQueue);

            userQueue.offer(jwtInformation);
            putIndices(jwtInformation);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void invalidateJwtInformationByUserId(UUID userId) {
        if (userId == null) return;

        ReentrantLock lock = lockFor(userId);
        lock.lock();
        try {
            Queue<JwtInformation> removedQueue = origin.remove(userId);
            if (removedQueue != null) {
                removedQueue.forEach(this::removeIndices);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        if (accessToken == null) return false;
        JwtInformation info = tokenIndex.get(accessToken);
        return info != null && !info.isExpired(clock.instant());
    }

    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        if (refreshToken == null) return false;
        JwtInformation info = refreshTokenIndex.get(refreshToken);
        return info != null && !info.isExpired(clock.instant());
    }

    @Override
    public JwtInformation getJwtInformationByRefreshToken(String refreshToken) {
        if (refreshToken == null) return null;
        JwtInformation info = refreshTokenIndex.get(refreshToken);
        if (info == null || info.isExpired(clock.instant())) {
            return null;
        }
        return info;
    }

    @Override
    public boolean rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        if (refreshToken == null || newJwtInformation == null) {
            return false;
        }

        UUID userId = newJwtInformation.userId();
        ReentrantLock lock = lockFor(userId);
        lock.lock();
        try {
            Queue<JwtInformation> userQueue = origin.get(userId);
            if (userQueue == null || userQueue.isEmpty()) {
                return false;
            }

            Instant now = clock.instant();
            boolean removed = removeMatchingActiveToken(userQueue, refreshToken, now);
            if (!removed) {
                if (userQueue.isEmpty()) {
                    origin.remove(userId);
                }
                return false;
            }

            removeExpired(userQueue, now);
            evictOverflow(userQueue);

            userQueue.offer(newJwtInformation);
            putIndices(newJwtInformation);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void rollbackRotateJwtInformation(
            String oldRefreshToken,
            JwtInformation oldJwtInformation,
            String newRefreshToken
    ) {
        if (oldJwtInformation == null) return;

        UUID userId = oldJwtInformation.userId();
        ReentrantLock lock = lockFor(userId);
        lock.lock();
        try {
            Queue<JwtInformation> userQueue = origin.computeIfAbsent(userId, ignored -> new ArrayDeque<>());

            if (newRefreshToken != null) {
                userQueue.removeIf(info -> {
                    boolean matched = newRefreshToken.equals(info.refreshToken());
                    if (matched) {
                        removeIndices(info);
                    }
                    return matched;
                });
            }

            boolean oldTokenAlreadyExists = userQueue.stream()
                    .anyMatch(info -> oldRefreshToken != null && oldRefreshToken.equals(info.refreshToken()));

            if (!oldTokenAlreadyExists) {
                removeExpired(userQueue, clock.instant());
                evictOverflow(userQueue);

                userQueue.offer(oldJwtInformation);
                putIndices(oldJwtInformation);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Scheduled(fixedDelayString = "${app.jwt.cleanup-interval-ms:300000}")
    public void clearExpiredJwtInformation() {
        Instant now = clock.instant();
        int removedCount = 0;

        for (UUID userId : origin.keySet()) {
            ReentrantLock lock = lockFor(userId);
            lock.lock();
            try {
                Queue<JwtInformation> userQueue = origin.get(userId);
                if (userQueue == null) {
                    continue;
                }

                removedCount += removeExpired(userQueue, now);

                if (userQueue.isEmpty()) {
                    origin.remove(userId);
                }
            } finally {
                lock.unlock();
            }
        }

        if (removedCount > 0) {
            log.debug("[JwtRegistry] 만료된 JWT 세션 {}건을 정리했습니다.", removedCount);
        }
    }

    private boolean removeMatchingActiveToken(Queue<JwtInformation> userQueue, String refreshToken, Instant now) {
        Iterator<JwtInformation> iterator = userQueue.iterator();
        while (iterator.hasNext()) {
            JwtInformation info = iterator.next();
            if (refreshToken.equals(info.refreshToken())) {
                iterator.remove();
                removeIndices(info);
                return !info.isExpired(now);
            }
        }
        return false;
    }

    private int removeExpired(Queue<JwtInformation> userQueue, Instant now) {
        int removedCount = 0;
        Iterator<JwtInformation> iterator = userQueue.iterator();
        while (iterator.hasNext()) {
            JwtInformation info = iterator.next();
            if (info.isExpired(now)) {
                iterator.remove();
                removeIndices(info);
                removedCount++;
            }
        }
        return removedCount;
    }

    private void evictOverflow(Queue<JwtInformation> userQueue) {
        while (!userQueue.isEmpty() && userQueue.size() >= maxActiveJwtCount) {
            JwtInformation removed = userQueue.poll();
            if (removed != null) {
                removeIndices(removed);
            }
        }
    }

    private void putIndices(JwtInformation info) {
        if (info.accessToken() != null) {
            tokenIndex.put(info.accessToken(), info);
        }
        if (info.refreshToken() != null) {
            refreshTokenIndex.put(info.refreshToken(), info);
        }
    }

    private void removeIndices(JwtInformation info) {
        if (info.accessToken() != null) {
            tokenIndex.remove(info.accessToken());
        }
        if (info.refreshToken() != null) {
            refreshTokenIndex.remove(info.refreshToken());
        }
    }
}
