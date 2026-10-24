package com.chamcong.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;

/** Popup nhắc nhở: thông báo toàn màn hình + lớp phủ che màn hình + rung (không âm thanh). */
final class Popup {
    static final int NOTIF_ID = 1001;
    static final String CHANNEL = "nhac_nho_cham_cong";
    static final long MAX_MS = 15 * 60 * 1000L;
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private static Context appCtx;
    static View overlay;
    static AlertActivity activity;
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static final Runnable TIMEOUT = new Runnable() {
        @Override
        public void run() {
            if (appCtx != null) dismiss(appCtx);
        }
    };

    static final class Content {
        String kicker, time, title, body, confirmText = "XÁC NHẬN", offText = "NGHỈ";
    }

    static Content content(Context c, String day, int type) {
        Content ct = new Content();
        if ("TEST".equals(day)) {
            ct.kicker = "THỬ NGHIỆM";
            ct.time = LocalTime.now().format(HM);
            ct.title = "Thử popup nhắc nhở";
            ct.body = "Máy rung đến khi bạn bấm Xác nhận hoặc Nghỉ.\nBấm nút nào cũng không ảnh hưởng dữ liệu chấm công.";
            return ct;
        }
        Schedule.Info in = Schedule.info(LocalDate.parse(day), new Store(c).kip());
        String start = in.start != null ? in.start.format(HM) : "";
        String end = in.end != null ? in.end.format(HM) : "";
        switch (type) {
            case 0:
                ct.kicker = "CHẤM CÔNG VÀO CA";
                ct.time = start;
                ct.title = "Sắp vào " + in.label.toLowerCase();
                ct.body = in.label + " bắt đầu lúc " + start + " (còn 5 phút).\nHãy chấm công vào ca.";
                break;
            case 1:
                ct.kicker = "KÝ SỔ CÔNG VIỆC";
                ct.time = start;
                ct.title = "Nhớ ký sổ công việc";
                ct.body = "Đã qua giờ vào " + in.label.toLowerCase() + " (" + start + ") 5 phút.\nHãy ký sổ công việc.";
                break;
            default:
                ct.kicker = "HẾT CA - CHẤM CÔNG RA VỀ";
                ct.time = end;
                ct.title = in.label + " đã kết thúc";
                ct.body = "Hết ca lúc " + end + ".\nBạn đã chấm công rời công ty chưa?\nBấm XÁC NHẬN để tính công hôm nay.";
                ct.confirmText = "XÁC NHẬN (TÍNH CÔNG)";
                break;
        }
        return ct;
    }

    static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Nhắc nhở chấm công",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Popup nhắc chấm công, ký sổ, xác nhận ra về (rung, không âm thanh)");
        ch.setSound(null, null);
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 600, 300, 600});
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    static void show(Context ctx, String day, int type) {
        Context c = ctx.getApplicationContext();
        appCtx = c;
        dismiss(c);
        ensureChannel(c);
        final Content ct = content(c, day, type);
        postNotification(c, day, type, ct);
        startVibration(c);
        showOverlay(c, day, type, ct);
        H.removeCallbacks(TIMEOUT);
        H.postDelayed(TIMEOUT, MAX_MS);
    }

    private static PendingIntent actionPi(Context c, String day, int type, boolean confirm) {
        Intent i = new Intent(c, ActionReceiver.class);
        i.setAction(ActionReceiver.ACTION);
        i.putExtra("day", day);
        i.putExtra("type", type);
        i.putExtra("confirm", confirm);
        return PendingIntent.getBroadcast(c, 100 + (confirm ? 1 : 0), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void postNotification(Context c, String day, int type, Content ct) {
        Intent fsi = new Intent(c, AlertActivity.class);
        fsi.putExtra("day", day);
        fsi.putExtra("type", type);
        fsi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fsPi = PendingIntent.getActivity(c, 10, fsi,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Icon icon = Icon.createWithResource(c, R.drawable.ic_stat_clock);
        Notification.Action okA = new Notification.Action.Builder(icon,
                "Xác nhận", actionPi(c, day, type, true)).build();
        Notification.Action offA = new Notification.Action.Builder(icon,
                "Nghỉ", actionPi(c, day, type, false)).build();

        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_clock)
                .setContentTitle(ct.kicker + " - " + ct.time)
                .setContentText(ct.title)
                .setStyle(new Notification.BigTextStyle().bigText(ct.body))
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(fsPi)
                .setFullScreenIntent(fsPi, true)
                .addAction(okA)
                .addAction(offA)
                .build();
        try {
            c.getSystemService(NotificationManager.class).notify(NOTIF_ID, n);
        } catch (Throwable ignored) {
        }
    }

    private static void startVibration(Context c) {
        try {
            Vibrator v = c.getSystemService(Vibrator.class);
            if (v == null) return;
            // Rung 0,7s - nghỉ 0,7s, kéo dài tối đa ~10 phút (hữu hạn để chắc chắn tự dừng)
            int cycles = 430;
            long[] pat = new long[1 + cycles * 2];
            pat[0] = 0;
            for (int i = 0; i < cycles; i++) {
                pat[1 + i * 2] = 700;
                pat[2 + i * 2] = 700;
            }
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            v.vibrate(VibrationEffect.createWaveform(pat, -1), aa);
        } catch (Throwable ignored) {
        }
    }

    private static void stopVibration(Context c) {
        try {
            Vibrator v = c.getSystemService(Vibrator.class);
            if (v != null) v.cancel();
        } catch (Throwable ignored) {
        }
    }

    private static void showOverlay(final Context c, final String day, final int type, Content ct) {
        if (!Settings.canDrawOverlays(c)) return;
        try {
            WindowManager wm = c.getSystemService(WindowManager.class);
            View v = PopupView.build(c, ct, new java.util.function.Consumer<Boolean>() {
                @Override
                public void accept(Boolean confirm) {
                    ActionReceiver.apply(c, day, type, confirm);
                }
            });
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.OPAQUE);
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            wm.addView(v, lp);
            overlay = v;
        } catch (Throwable ignored) {
            overlay = null;
        }
    }

    /** Tắt rung, xóa thông báo, gỡ lớp phủ, đóng màn hình popup. */
    static void dismiss(Context ctx) {
        Context c = ctx.getApplicationContext();
        H.removeCallbacks(TIMEOUT);
        stopVibration(c);
        try {
            c.getSystemService(NotificationManager.class).cancel(NOTIF_ID);
        } catch (Throwable ignored) {
        }
        if (overlay != null) {
            try {
                c.getSystemService(WindowManager.class).removeView(overlay);
            } catch (Throwable ignored) {
            }
            overlay = null;
        }
        if (activity != null) {
            try {
                activity.finish();
            } catch (Throwable ignored) {
            }
            activity = null;
        }
    }
}
