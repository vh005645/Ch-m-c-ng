package com.chamcong.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Lưu dữ liệu bằng SharedPreferences (rất nhẹ). */
final class Store {
    static final String WORK = "W";   // đã chấm công
    static final String OFF = "O";    // nghỉ tự do (không tính công)
    static final String LEAVE = "P";  // nghỉ phép (VẪN tính công)
    static final String OT = "T";     // tăng ca (ngày không theo lịch, vẫn tính công)

    private final SharedPreferences p;

    Store(Context c) {
        p = c.getApplicationContext().getSharedPreferences("chamcong", Context.MODE_PRIVATE);
    }

    boolean enabled() {
        return p.getBoolean("enabled", true);
    }

    void setEnabled(boolean v) {
        p.edit().putBoolean("enabled", v).apply();
    }

    /** 0..3 = kíp A..D, 4 = hành chính, -1 = chưa chọn. */
    int kip() {
        return p.getInt("kip", -1);
    }

    void setKip(int k) {
        p.edit().putInt("kip", k).apply();
    }

    String status(String day) {
        return p.getString("s_" + day, null);
    }

    void setStatus(String day, String v) {
        if ("TEST".equals(day)) return;
        SharedPreferences.Editor e = p.edit();
        if (v == null) e.remove("s_" + day);
        else e.putString("s_" + day, v);
        e.apply();
    }

    /** Xóa dữ liệu của các năm cũ (chạy khi sang năm mới dương lịch). */
    void prune(int currentYear) {
        List<String> del = new ArrayList<>();
        for (Map.Entry<String, ?> en : p.getAll().entrySet()) {
            String k = en.getKey();
            if (k.startsWith("s_") && k.length() >= 6) {
                try {
                    int y = Integer.parseInt(k.substring(2, 6));
                    if (y < currentYear) del.add(k);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (del.isEmpty()) return;
        SharedPreferences.Editor e = p.edit();
        for (String k : del) e.remove(k);
        e.apply();
    }

    /**
     * Có cần nhắc nhở cho sự kiện này không?
     * - Đã chọn nghỉ phép / nghỉ tự do -> không nhắc gì nữa trong ngày đó.
     * - Đã chấm công rồi -> không nhắc "chấm công vào ca" (type 0) nữa,
     *   nhưng vẫn nhắc ký sổ (type 1) và chấm công ra về (type 2).
     */
    boolean shouldRemind(String day, int type) {
        String s = status(day);
        if (OFF.equals(s) || LEAVE.equals(s)) return false;
        if ((WORK.equals(s) || OT.equals(s)) && type == 0) return false;
        return true;
    }

    /** @return {số công (đã gồm cả nghỉ phép), số nghỉ tự do, số ngày nghỉ phép} của tháng. */
    int[] count(YearMonth ym) {
        int work = 0, off = 0, leave = 0;
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            String s = status(ym.atDay(d).toString());
            if (s == null) continue;
            if (OFF.equals(s)) {
                off++;
            } else {
                work++;                       // chấm công / tăng ca / nghỉ phép đều tính công
                if (LEAVE.equals(s)) leave++;
            }
        }
        return new int[]{work, off, leave};
    }
}
