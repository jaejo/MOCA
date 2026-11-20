package com.jj.moca.core.common.util;

import java.util.Locale;

public final class EmailFormatter {

    private EmailFormatter() {
    }

    /**
     * 이메일 앞뒤 공백을 제거하고 소문자로 정규화합니다.
     */
    public static String normalize(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 로그 출력 시 개인정보 보호를 위해 이메일 로컬 파트를 마스킹합니다. (예: te***@moca.com)
     */
    public static String mask(String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }
        String normalized = normalize(email);
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String localPart = normalized.substring(0, atIndex);
        String domainPart = normalized.substring(atIndex);
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***" + domainPart;
        }
        return localPart.substring(0, 2) + "***" + domainPart;
    }
}
