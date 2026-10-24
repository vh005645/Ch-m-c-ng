package com.chamcong.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Xử lý các nút (Chấm công / Nghỉ phép / Nghỉ tự do / Xác nhận) bấm từ thông báo. */
public class ActionReceiver extends BroadcastReceiver {
    static final String ACTION = "com.chamcong.app.RESOLVE";

    static final int ACT_OK = 0;      // Chấm công (lần 1) / Xác nhận (lần 2, 3)
    static final int ACT_LEAVE = 1;   // Nghỉ phép
    static final int ACT_OFF = 2;     // Nghỉ tự do

    @Override
    public void onReceive(Context c, Intent i) {
        apply(c, i.getStringExtra("day"), i.getIntExtra("type", 0),
                i.getIntExtra("action", ACT_OK));
    }

    /**
     * Nghỉ tự do -> ghi nghỉ (không tính công).
     * Nghỉ phép  -> ghi nghỉ phép (vẫn tính công).
     * Chấm công ở lần nhắc 1 (type 0) -> ghi đã chấm công.
     * Xác nhận ở lần nhắc 3 (type 2) -> nếu hôm đó chưa có trạng thái thì tính 1 công
     *   (phòng khi lỡ bỏ qua lần nhắc 1).
     * Xác nhận ở lần nhắc 2 (ký sổ) -> chỉ tắt nhắc.
     */
    static void apply(Context ctx, String day, int type, int action) {
        Context c = ctx.getApplicationContext();
        if (day != null && !"TEST".equals(day)) {
            Store s = new Store(c);
            if (action == ACT_OFF) {
                s.setStatus(day, Store.OFF);
            } else if (action == ACT_LEAVE) {
                s.setStatus(day, Store.LEAVE);
            } else if (type == 0) {
                s.setStatus(day, Store.WORK);
            } else if (type == 2 && s.status(day) == null) {
                s.setStatus(day, Store.WORK);
            }
            // Đặt lại báo thức kế tiếp (bỏ qua các lần nhắc còn lại nếu đã chọn nghỉ)
            Scheduler.scheduleNext(c);
        }
        Popup.dismiss(c);
    }
}
