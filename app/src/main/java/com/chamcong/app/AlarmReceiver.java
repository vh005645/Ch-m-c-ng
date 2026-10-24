package com.chamcong.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDate;

public class AlarmReceiver extends BroadcastReceiver {
    static final String ACTION_FIRE = "com.chamcong.app.FIRE";

    @Override
    public void onReceive(Context c, Intent i) {
        String action = i.getAction();
        Store st = new Store(c);
        st.prune(LocalDate.now().getYear());
        if (ACTION_FIRE.equals(action)) {
            String day = i.getStringExtra("day");
            int type = i.getIntExtra("type", 0);
            if (day != null) {
                boolean test = "TEST".equals(day);
                if (test || (st.enabled() && st.shouldRemind(day, type))) {
                    try {
                        Popup.show(c, day, type);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        // Boot / đổi giờ / đổi múi giờ / cập nhật app / sau mỗi lần nổ: đặt lại báo thức kế tiếp
        Scheduler.scheduleNext(c);
    }
}
