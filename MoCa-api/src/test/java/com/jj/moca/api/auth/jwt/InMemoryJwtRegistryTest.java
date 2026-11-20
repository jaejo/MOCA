package com.jj.moca.api.auth.jwt;

import com.jj.moca.api.auth.jwt.response.JwtInformation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryJwtRegistryTest {

    private static final Instant BASE_TIME = Instant.parse("2026-11-19T06:00:00Z");

    @Test
    @DisplayName("maxActiveJwtCount가 1 미만이면 생성 시 예외가 발생한다")
    void throwsWhenMaxActiveCountIsInvalid() {
        Clock clock = Clock.fixed(BASE_TIME, ZoneOffset.UTC);

        assertThatThrownBy(() -> new InMemoryJwtRegistry(0, clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InMemoryJwtRegistry(-1, clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("최대 활성 세션 수를 초과해 등록하면 가장 오래된 세션부터 제거된다")
    void evictsOldestSessionWhenExceedingMaxActiveCount() {
        MutableClock clock = new MutableClock(BASE_TIME);
        InMemoryJwtRegistry registry = new InMemoryJwtRegistry(2, clock);
        UUID userId = UUID.randomUUID();
        Instant expiresAt = BASE_TIME.plusSeconds(3600);

        JwtInformation first = new JwtInformation(userId, "access-1", "refresh-1", expiresAt);
        JwtInformation second = new JwtInformation(userId, "access-2", "refresh-2", expiresAt);
        JwtInformation third = new JwtInformation(userId, "access-3", "refresh-3", expiresAt);

        registry.registerJwtInformation(first);
        registry.registerJwtInformation(second);
        registry.registerJwtInformation(third);

        assertThat(registry.hasActiveJwtInformationByAccessToken("access-1")).isFalse();
        assertThat(registry.hasActiveJwtInformationByRefreshToken("refresh-1")).isFalse();
        assertThat(registry.hasActiveJwtInformationByAccessToken("access-2")).isTrue();
        assertThat(registry.hasActiveJwtInformationByRefreshToken("refresh-2")).isTrue();
        assertThat(registry.hasActiveJwtInformationByAccessToken("access-3")).isTrue();
        assertThat(registry.hasActiveJwtInformationByRefreshToken("refresh-3")).isTrue();
    }

    @Test
    @DisplayName("사용자 ID로 무효화하면 해당 사용자의 모든 세션이 제거된다")
    void invalidatesAllSessionsForUser() {
        MutableClock clock = new MutableClock(BASE_TIME);
        InMemoryJwtRegistry registry = new InMemoryJwtRegistry(3, clock);
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Instant expiresAt = BASE_TIME.plusSeconds(3600);

        registry.registerJwtInformation(new JwtInformation(userId, "u1-a1", "u1-r1", expiresAt));
        registry.registerJwtInformation(new JwtInformation(userId, "u1-a2", "u1-r2", expiresAt));
        registry.registerJwtInformation(new JwtInformation(otherUserId, "u2-a1", "u2-r1", expiresAt));

        registry.invalidateJwtInformationByUserId(userId);

        assertThat(registry.hasActiveJwtInformationByAccessToken("u1-a1")).isFalse();
        assertThat(registry.hasActiveJwtInformationByAccessToken("u1-a2")).isFalse();
        assertThat(registry.hasActiveJwtInformationByRefreshToken("u1-r1")).isFalse();
        assertThat(registry.hasActiveJwtInformationByRefreshToken("u1-r2")).isFalse();
        assertThat(registry.hasActiveJwtInformationByAccessToken("u2-a1")).isTrue();
        assertThat(registry.hasActiveJwtInformationByRefreshToken("u2-r1")).isTrue();
    }

    @Test
    @DisplayName("rotateJwtInformation은 기존 리프레시 토큰이 활성 상태일 때만 교체하고 true를 반환한다")
    void rotatesOnlyWhenRefreshTokenExistsAndActive() {
        MutableClock clock = new MutableClock(BASE_TIME);
        InMemoryJwtRegistry registry = new InMemoryJwtRegistry(2, clock);
        UUID userId = UUID.randomUUID();
        Instant expiresAt = BASE_TIME.plusSeconds(3600);

        JwtInformation current = new JwtInformation(userId, "access-old", "refresh-old", expiresAt);
        JwtInformation next = new JwtInformation(userId, "access-new", "refresh-new", expiresAt.plusSeconds(600));
        registry.registerJwtInformation(current);

        boolean rotated = registry.rotateJwtInformation("refresh-old", next);

        assertThat(rotated).isTrue();
        assertThat(registry.hasActiveJwtInformationByAccessToken("access-old")).isFalse();
        assertThat(registry.hasActiveJwtInformationByRefreshToken("refresh-old")).isFalse();
        assertThat(registry.hasActiveJwtInformationByAccessToken("access-new")).isTrue();
        assertThat(registry.getJwtInformationByRefreshToken("refresh-new")).isEqualTo(next);

        // 이미 소비된 토큰으로 다시 로테이션 시도하면 false 반환 및 새 토큰 미등록
        JwtInformation duplicateNext = new JwtInformation(userId, "access-dup", "refresh-dup", expiresAt.plusSeconds(1200));
        boolean duplicateRotated = registry.rotateJwtInformation("refresh-old", duplicateNext);

        assertThat(duplicateRotated).isFalse();
        assertThat(registry.hasActiveJwtInformationByAccessToken("access-dup")).isFalse();
        assertThat(registry.hasActiveJwtInformationByRefreshToken("refresh-dup")).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 조회되지 않으며 clearExpiredJwtInformation 호출 시 메모리에서 정리된다")
    void clearsExpiredJwtInformation() {
        MutableClock clock = new MutableClock(BASE_TIME);
        InMemoryJwtRegistry registry = new InMemoryJwtRegistry(3, clock);
        UUID expiredUser = UUID.randomUUID();
        UUID activeUser = UUID.randomUUID();

        registry.registerJwtInformation(
                new JwtInformation(expiredUser, "expired-access", "expired-refresh", BASE_TIME.plusSeconds(60))
        );
        registry.registerJwtInformation(
                new JwtInformation(activeUser, "active-access", "active-refresh", BASE_TIME.plusSeconds(3600))
        );

        // 60초 경과 -> expiredUser의 토큰 만료
        clock.advanceSeconds(60);

        assertThat(registry.hasActiveJwtInformationByAccessToken("expired-access")).isFalse();
        assertThat(registry.hasActiveJwtInformationByRefreshToken("expired-refresh")).isFalse();
        assertThat(registry.getJwtInformationByRefreshToken("expired-refresh")).isNull();
        assertThat(registry.hasActiveJwtInformationByAccessToken("active-access")).isTrue();

        registry.clearExpiredJwtInformation();

        // 정리 후에도 만료 세션은 로테이션 불가, 활성 세션은 정상 유지
        boolean rotatedExpired = registry.rotateJwtInformation(
                "expired-refresh",
                new JwtInformation(expiredUser, "new-a", "new-r", BASE_TIME.plusSeconds(7200))
        );
        assertThat(rotatedExpired).isFalse();
        assertThat(registry.hasActiveJwtInformationByAccessToken("active-access")).isTrue();
        assertThat(registry.hasActiveJwtInformationByRefreshToken("active-refresh")).isTrue();
    }

    @Test
    @DisplayName("rollbackRotateJwtInformation은 새 토큰을 제거하고 기존 토큰을 복원한다")
    void rollsBackRotatedToken() {
        MutableClock clock = new MutableClock(BASE_TIME);
        InMemoryJwtRegistry registry = new InMemoryJwtRegistry(2, clock);
        UUID userId = UUID.randomUUID();
        Instant expiresAt = BASE_TIME.plusSeconds(3600);

        JwtInformation oldInfo = new JwtInformation(userId, "access-old", "refresh-old", expiresAt);
        JwtInformation newInfo = new JwtInformation(userId, "access-new", "refresh-new", expiresAt.plusSeconds(600));

        registry.registerJwtInformation(oldInfo);
        assertThat(registry.rotateJwtInformation("refresh-old", newInfo)).isTrue();

        registry.rollbackRotateJwtInformation("refresh-old", oldInfo, "refresh-new");

        assertThat(registry.hasActiveJwtInformationByAccessToken("access-new")).isFalse();
        assertThat(registry.hasActiveJwtInformationByRefreshToken("refresh-new")).isFalse();
        assertThat(registry.hasActiveJwtInformationByAccessToken("access-old")).isTrue();
        assertThat(registry.hasActiveJwtInformationByRefreshToken("refresh-old")).isTrue();
    }

    @Test
    @DisplayName("동일한 리프레시 토큰으로 동시 로테이션 요청 시 정확히 하나의 요청만 성공한다")
    void allowsOnlySingleWinnerOnConcurrentRotation() throws Exception {
        MutableClock clock = new MutableClock(BASE_TIME);
        InMemoryJwtRegistry registry = new InMemoryJwtRegistry(5, clock);
        UUID userId = UUID.randomUUID();
        Instant expiresAt = BASE_TIME.plusSeconds(3600);

        registry.registerJwtInformation(new JwtInformation(userId, "access-init", "refresh-init", expiresAt));

        int threadCount = 16;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    JwtInformation next = new JwtInformation(
                            userId,
                            "access-next-" + idx,
                            "refresh-next-" + idx,
                            expiresAt.plusSeconds(600)
                    );
                    return registry.rotateJwtInformation("refresh-init", next);
                }));
            }

            ready.await();
            start.countDown();

            int successCount = 0;
            int winnerIdx = -1;
            for (int i = 0; i < threadCount; i++) {
                if (futures.get(i).get()) {
                    successCount++;
                    winnerIdx = i;
                }
            }

            assertThat(successCount).isEqualTo(1);
            assertThat(registry.hasActiveJwtInformationByRefreshToken("refresh-init")).isFalse();
            assertThat(registry.hasActiveJwtInformationByRefreshToken("refresh-next-" + winnerIdx)).isTrue();

            for (int i = 0; i < threadCount; i++) {
                if (i != winnerIdx) {
                    assertThat(registry.hasActiveJwtInformationByRefreshToken("refresh-next-" + i)).isFalse();
                    assertThat(registry.hasActiveJwtInformationByAccessToken("access-next-" + i)).isFalse();
                }
            }
        }
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;

        private MutableClock(Instant initial) {
            this.current = new AtomicReference<>(initial);
        }

        void advanceSeconds(long seconds) {
            current.updateAndGet(t -> t.plusSeconds(seconds));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }
}
