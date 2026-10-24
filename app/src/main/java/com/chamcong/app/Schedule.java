package com.chamcong.app;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Tính lịch làm việc theo kíp. Chu kỳ 8 ngày, mỗi kíp lệch nhau 2 ngày. */
final class Schedule {

    static final int KIP_HC = 4;
    static final String[] KIP_NAMES = {"Kíp A", "Kíp B", "Kíp C", "Kíp D", "Hành chính"};

    // Mốc: 24/10/2026  A = ca 2 cuối, B = ca 3 cuối, C = nghỉ tour xịn, D = ca 1 đầu
    static final LocalDate ANCHOR = LocalDate.of(2026, 10, 24);
    // Vị trí của từng kíp trong chu kỳ tại ngày mốc
    static final int[] KIP_OFFSET = {4, 6, 2, 0};

    // Chu kỳ: 0 ca1 đầu, 1 ca1 cuối, 2 tour xịn, 3 ca2 đầu, 4 ca2 cuối, 5 ca3 đầu, 6 ca3 cuối, 7 tour đểu
    static final int[] CYCLE_SHIFT = {1, 1, 0, 2, 2, 3, 3, 0};
    static final String[] CYCLE_LABEL = {
            "Ca 1 đầu", "Ca 1 cuối", "Tour xịn", "Ca 2 đầu",
            "Ca 2 cuối", "Ca 3 đầu", "Ca 3 cuối", "Tour đểu"};
    static final String[] CYCLE_SHORT = {"1đ", "1c", "", "2đ", "2c", "3đ", "3c", ""};

    static final class Info {
        boolean work;
        String label = "Tour";
        String shortLabel = "";
        LocalTime start;
        LocalTime end;
        boolean endNextDay;
        LocalDate date;

        long startMillis() {
            return date.atTime(start).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        long endMillis() {
            return date.plusDays(endNextDay ? 1 : 0).atTime(end)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
    }

    static Info info(LocalDate d, int kip) {
        Info in = new Info();
        in.date = d;
        if (kip < 0) {
            in.label = "Chưa chọn kíp";
            return in;
        }
        if (kip == KIP_HC) {
            DayOfWeek w = d.getDayOfWeek();
            if (w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY) {
                in.label = "Nghỉ cuối tuần";
                return in;
            }
            in.work = true;
            in.label = "Hành chính";
            in.shortLabel = "HC";
            in.start = LocalTime.of(7, 0);
            in.end = LocalTime.of(17, 0);
            return in;
        }
        long days = ChronoUnit.DAYS.between(ANCHOR, d);
        int idx = (int) (((KIP_OFFSET[kip] + days) % 8 + 8) % 8);
        in.label = CYCLE_LABEL[idx];
        in.shortLabel = CYCLE_SHORT[idx];
        int shift = CYCLE_SHIFT[idx];
        if (shift == 0) return in;
        in.work = true;
        if (shift == 1) {
            in.start = LocalTime.of(6, 0);
            in.end = LocalTime.of(14, 0);
        } else if (shift == 2) {
            in.start = LocalTime.of(14, 0);
            in.end = LocalTime.of(22, 0);
        } else {
            in.start = LocalTime.of(22, 0);
            in.end = LocalTime.of(6, 0);
            in.endNextDay = true;
        }
        return in;
    }

    static final class Event {
        String day;   // yyyy-MM-dd (ngày bắt đầu ca)
        int type;     // 0 = trước giờ vào 5 phút, 1 = sau giờ vào 5 phút, 2 = trước giờ về 5 phút
        long time;
    }

    /**
     * Sự kiện nhắc nhở kế tiếp sau thời điểm now.
     * Chỉ xét NGÀY ĐI LÀM (ngày tour/nghỉ không có nhắc nhở). Ngày đã chọn nghỉ phép /
     * nghỉ tự do cũng được bỏ qua (xem Store.shouldRemind).
     */
    static Event next(int kip, long now, Store st) {
        if (kip < 0) return null;
        LocalDate today = LocalDate.now();
        Event best = null;
        for (int i = -2; i <= 9; i++) {
            LocalDate d = today.plusDays(i);
            Info in = info(d, kip);
            if (!in.work) continue;
            long s = in.startMillis();
            long[] t = {s - 5 * 60000L, s + 5 * 60000L, in.endMillis() - 5 * 60000L};
            for (int k = 0; k < 3; k++) {
                if (st != null && !st.shouldRemind(d.toString(), k)) continue;
                if (t[k] > now && (best == null || t[k] < best.time)) {
                    best = new Event();
                    best.day = d.toString();
                    best.type = k;
                    best.time = t[k];
                }
            }
        }
        return best;
    }

    /** Ca làm việc gần nhất (từ hôm nay trở đi) - dùng cho popup thử nghiệm. */
    static Info nextWorkInfo(int kip) {
        if (kip < 0) return null;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 9; i++) {
            Info in = info(today.plusDays(i), kip);
            if (in.work) return in;
        }
        return null;
    }

    static String typeName(int type) {
        switch (type) {
            case 0: return "Chấm công vào ca";
            case 1: return "Ký sổ công việc";
            default: return "Chấm công ra về";
        }
    }
}
