package com.chamcong.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Xử lý nút Xác nhận / Nghỉ bấm từ thông báo. */
public class ActionReceiver extends BroadcastReceiver {
    static final String ACTION = "com.chamcong.app.RESOLVE";

    @Override
    public void onReceive(Context c, Intent i) {
        apply(c, i.getStringExtra("day"), i.getIntExtra("type", 0),
                i.getBooleanExtra("confirm", true));
    }

    /**
     * Nghỉ  -> ghi nghỉ cho ngày đó.
     * Xác nhận ở popup hết ca (type 2) -> tính 1 công.
     * Xác nhận ở popup vào ca / ký sổ -> chỉ tắt nhắc.
     */
    static void apply(Context ctx, String day, int type, boolean confirm) {
        Context c = ctx.getApplicationContext();
        if (day != null && !"TEST".equals(day)) {
            Store s = new Store(c);
            if (!confirm) s.setStatus(day, Store.OFF);
            else if (type == 2) s.setStatus(day, Store.WORK);
        }
        Popup.dismiss(c);
    }
}
