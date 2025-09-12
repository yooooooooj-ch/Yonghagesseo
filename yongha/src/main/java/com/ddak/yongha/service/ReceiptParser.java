// src/main/java/com/example/ocr/util/ReceiptParser.java
package com.ddak.yongha.service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class ReceiptParser {
    private ReceiptParser() {}

    // ===== 날짜 패턴 =====
    // yyyy.mm.dd / yy.mm.dd / yyyy-mm-dd / yy-mm-dd  (+ 시간 옵션, dd 뒤에 곧바로 시간 붙어도 허용)
    private static final Pattern DATE_MIXED_OPT_TIME = Pattern.compile(
        "\\b(\\d{4}|\\d{2})\\s*([.-])\\s*(\\d{1,2})\\s*\\2\\s*(\\d{1,2})(?:\\D{0,10}(\\d{2}):(\\d{2})(?::(\\d{2}))?)?"
    );
    // 압축형(선택): 14자리 YYYYMMDDhhmmss, 12자리 YYYYMMDDhhmm
    private static final Pattern DT_COMPACT_SEC = Pattern.compile(
        "\\b(\\d{4})(\\d{2})(\\d{2})\\s*(\\d{2})(\\d{2})(\\d{2})\\b"
    );
    private static final Pattern DT_COMPACT_MIN = Pattern.compile(
        "\\b(\\d{4})(\\d{2})(\\d{2})\\s*(\\d{2})(\\d{2})\\b"
    );

    // ===== 금액 패턴 =====
    // 라인 끝에서 숫자(쉼표 그룹 허용) + (옵션 '원') + (0 또는 00 으로 끝남)
    private static final Pattern AMOUNT_AT_EOL = Pattern.compile(
        "(?<!\\d)([1-9]\\d{0,2}(?:,\\d{3})+|[1-9]\\d{3,})\\s*(?:원)?\\s*$"
    );

    // 총액 관련 키워드 (오인식 약간 포함)
    private static final Pattern AMOUNT_KEYWORDS = Pattern.compile(
	    "(" +
	      "총\\s*(?:합계|금[액앵맥]|액|금|계)"+
	      "|합계(?:\\s*금[액앵맥])?"+
	      "|결\\s*제(?:\\s*금[액앵맥])?"+     // '결 제'도 매칭됨
	      "|영수\\s*액|받을\\s*돈|받은\\s*돈|청구(?:\\s*금[액앵맥])?"+
	      "|Amount\\s*Due|TOTAL|Grand\\s*Total|SUM" +
	    ")",
	    Pattern.CASE_INSENSITIVE
	);

    /* 총금액 선택*/
    public static BigDecimal pickAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String[] lines = raw.split("\\R", -1);

        // 키워드 라인 인덱스 수집
        java.util.List<Integer> keyIdxs = new java.util.ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (AMOUNT_KEYWORDS.matcher(lines[i]).find()) keyIdxs.add(i);
        }

        // 후보 수집
        java.util.List<Cand> cands = new java.util.ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String norm = normalizeAmountLine(lines[i]);
            if (norm.isEmpty()) continue;

            java.util.regex.Matcher m = AMOUNT_AT_EOL.matcher(norm);
            if (!m.find()) continue;

            String token  = m.group(1);                         // 예: "27,600"
            String digits = token.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) continue;
            if (digits.charAt(digits.length() - 1) != '0') continue; // 끝이 0만 허용

            BigDecimal val  = new BigDecimal(digits);
            int zeroTailRaw = countTrailingZeros(digits);
            int zeroCat     = (zeroTailRaw >= 2) ? 2 : 1;      // “…00” 우선
            boolean hasWon  = norm.matches(".*\\d\\s*원\\s*$");
            boolean hasComma= token.indexOf(',') >= 0;
            int keyDist     = nearestDistance(i, keyIdxs);     // 키워드 최소 거리(없으면 큰 값)

            cands.add(new Cand(val, zeroCat, i, hasWon, hasComma, keyDist));
        }

        if (cands.isEmpty()) return null;

        // 정렬 우선순위
        cands.sort((a, b) -> {
            int byKey = Integer.compare(a.keyDist, b.keyDist);
            if (byKey != 0) return byKey;

            if (a.hasWon != b.hasWon) return a.hasWon ? -1 : 1;

            // 값 큰 금액을 zeroCat보다 먼저 우선
            int byVal = b.value.compareTo(a.value);
            if (byVal != 0) return byVal;

            int byZero = Integer.compare(b.zeroCat, a.zeroCat);
            if (byZero != 0) return byZero;

            int byLine = Integer.compare(b.lineIndex, a.lineIndex);
            if (byLine != 0) return byLine;

            return a.hasComma == b.hasComma ? 0 : (a.hasComma ? -1 : 1);
        });

        return cands.get(0).value;
    }

    /** 결제일시: 날짜만 있는 경우 자정(00:00:00)으로, 시간이 있으면 포함하여 반환 */
    public static java.time.LocalDateTime pickDateTime(String raw) {
        if (raw == null || raw.isBlank()) return null;

        // OCR 치환 + 구분자/숫자 공백 정리
        String text = raw
            .replace('O','0').replace('o','0')
            .replace('l','1').replace('I','1')
            .replace('S','5').replace('B','8')
            .replaceAll("\\s*([.-])\\s*", "$1")
            .replaceAll("(?<=\\d)\\s+(?=\\d)", " ");

        java.util.List<CandDT> cands = new java.util.ArrayList<>();

        // 혼합형(.-) + 시간옵션
        java.util.regex.Matcher m = DATE_MIXED_OPT_TIME.matcher(text);
        while (m.find()) {
            int year   = toYear(m.group(1));
            int month  = toInt(m.group(3));
            int day    = toInt(m.group(4));
            Integer hh = (m.group(5) != null) ? toInt(m.group(5)) : null;
            Integer mi = (m.group(6) != null) ? toInt(m.group(6)) : null;
            Integer ss = (m.group(7) != null) ? toInt(m.group(7)) : null;
            addIfValid(cands, year, month, day, hh, mi, ss, m.start());
        }

        // 압축형
        m = DT_COMPACT_SEC.matcher(text);
        while (m.find()) {
            addIfValid(cands, toInt(m.group(1)), toInt(m.group(2)), toInt(m.group(3)),
                       toInt(m.group(4)), toInt(m.group(5)), toInt(m.group(6)), m.start());
        }
        m = DT_COMPACT_MIN.matcher(text);
        while (m.find()) {
            addIfValid(cands, toInt(m.group(1)), toInt(m.group(2)), toInt(m.group(3)),
                       toInt(m.group(4)), toInt(m.group(5)), 0, m.start());
        }

        if (cands.isEmpty()) return null;

        // (1) 시간 있음 우선  (2) 초 있음 우선  (3) 텍스트에서 더 아래(나중) 우선
        cands.sort((a, b) -> {
            if (a.hasTime != b.hasTime) return a.hasTime ? -1 : 1;
            if (a.hasSec  != b.hasSec)  return a.hasSec  ? -1 : 1;
            return Integer.compare(b.pos, a.pos);
        });

        return cands.get(0).ldt;
    }

    // ===== 내부 유틸 =====

    // 유효성 검사 + 후보 추가
    private static void addIfValid(java.util.List<CandDT> out, int y, int mo, int d,
                                   Integer h, Integer mi, Integer s, int pos) {
        if (y < 0) return;
        if (y < 100) y = 2000 + y;                  // yy → 20yy
        if (y < 2000 || y > 2100) return;
        if (mo < 1 || mo > 12) return;
        if (d  < 1 || d  > 31) return;

        try {
            java.time.LocalDate base = java.time.LocalDate.of(y, mo, d); // 달력 유효성 검증
            int hh = (h  != null) ? h  : 0;
            int mm = (mi != null) ? mi : 0;
            int ss = (s  != null) ? s  : 0;
            if (hh < 0 || hh > 23 || mm < 0 || mm > 59 || ss < 0 || ss > 59) return;

            boolean hasTime = (h != null && mi != null);
            boolean hasSec  = (s != null);

            out.add(new CandDT(base.atTime(hh, mm, ss), hasTime, hasSec, pos));
        } catch (java.time.DateTimeException ignore) { /* invalid date */ }
    }

    // 금액 라인 정규화: 숫자/쉼표/점/공백/원/콜론만 유지, 라벨 제거, 천단위 점·공백→쉼표
    private static String normalizeAmountLine(String s) {
        if (s == null) return "";
        String t = s.replaceAll("[^0-9,\\.원\\s:]", "");
        t = t.replaceAll(".*?:\\s*", ""); // "총액: 27,600" -> "27,600"
        t = t.replaceAll("(?<=\\d)\\.(?=\\d{3}(?:\\D|$))", ",")
             .replaceAll("(?<=\\d)\\s+(?=\\d{3}(?:\\D|$))", ",");
        return t.trim();
    }

    private static int countTrailingZeros(String s) {
        int c = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            if (s.charAt(i) == '0') c++; else break;
        }
        return c;
    }

    private static int nearestDistance(int idx, java.util.List<Integer> keyIdxs) {
        int best = 9999;
        for (int k : keyIdxs) {
            int d = Math.abs(idx - k);
            if (d < best) best = d;
        }
        return best;
    }

    private static int toYear(String y) {
        return (y.length() == 2) ? 2000 + toInt(y) : toInt(y);
    }

    private static int toInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }

    // 후보 컨테이너들
    private static final class Cand {
        final BigDecimal value;
        final int zeroCat;     // 2: …00 , 1: …0
        final int lineIndex;
        final boolean hasWon;
        final boolean hasComma;
        final int keyDist;

        Cand(BigDecimal v, int z, int idx, boolean won, boolean comma, int kd) {
            this.value = v; this.zeroCat = z; this.lineIndex = idx;
            this.hasWon = won; this.hasComma = comma; this.keyDist = kd;
        }
    }

    private static final class CandDT {
        final java.time.LocalDateTime ldt;
        final boolean hasTime;
        final boolean hasSec;
        final int pos; // 텍스트 내 시작 위치(아래쪽 우선)

        CandDT(java.time.LocalDateTime ldt, boolean hasTime, boolean hasSec, int pos) {
            this.ldt = ldt; this.hasTime = hasTime; this.hasSec = hasSec; this.pos = pos;
        }
    }
}
