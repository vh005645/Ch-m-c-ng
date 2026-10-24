package com.chamcong.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/** Chỉ đặt MỘT báo thức kế tiếp (setAlarmClock) - không service, không chạy ngầm. */
final class Scheduler {
    static final int RC_MAIN = 1;
    static final int RC_TEST = 2;

    private static PendingIntent firePi(Context c, int rc, String day, int type) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setAction(AlarmReceiver.ACTION_FIRE);
        if (day != null) {
            i.putExtra("day", day);
            i.putExtra("type", type);
        }
        return PendingIntent.getBroadcast(c, rc, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent showPi(Context c) {
        Intent i = new Intent(c, MainActivity.class);
        return PendingIntent.getActivity(c, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void scheduleNext(Context ctx) {
        Context c = ctx.getApplicationContext();
        AlarmManager am = c.getSystemService(AlarmManager.class);
        Scheduler.Next n = peek(c);
        if (n == null) {
            am.cancel(firePi(c, RC_MAIN, null, 0));
            return;
        }
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(n.event.time, showPi(c)),
                firePi(c, RC_MAIN, n.event.day, n.event.type));
    }

    static final class Next {
        Schedule.Event event;
    }

    /** Sự kiện kế tiếp theo cài đặt hiện tại (null nếu app đang tắt / chưa chọn kíp). */
    static Next peek(Context c) {
        Store st = new Store(c);
        if (!st.enabled()) return null;
        Schedule.Event e = Schedule.next(st.kip(), System.currentTimeMillis(), st);
        if (e == null) return null;
        Next n = new Next();
        n.event = e;
        return n;
    }

    static void scheduleTest(Context ctx, long delayMs) {
        Context c = ctx.getApplicationContext();
        AlarmManager am = c.getSystemService(AlarmManager.class);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(System.currentTimeMillis() + delayMs, showPi(c)),
                firePi(c, RC_TEST, "TEST", 0));
    }
}
