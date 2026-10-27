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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;

/** Popup nhắc nhở: thông báo toàn màn hình + lớp phủ che màn hình + tiếng chuông + rung
 *  (chuông và rung bật/tắt riêng trong tab Cài đặt).
 *  3 lần nhắc / ca: trước giờ vào 2' (Chấm công / Nghỉ phép / Nghỉ tự do, chuông cham_cong),
 *  sau giờ vào 2' (ký sổ, chuông ki_so_cv), trước giờ về 2' (chấm công ra về, chuông cham_cong). */
final class Popup {
    static final int NOTIF_ID = 1001;
    // Kênh mới hoàn toàn im lặng (không âm thanh, không rung): chuông và rung do app tự điều khiển
    // để bật/tắt được. Kênh cũ "nhac_nho_cham_cong" có bật rung ở cấp hệ thống nên phải xóa đi.
    static final String CHANNEL = "nhac_nho_v2";
    private static final String LEGACY_CHANNEL = "nhac_nho_cham_cong";
    private static final long SOUND_GAP_MS = 3000;   // nghỉ giữa các lần chuông lặp lại
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
        String kicker, time, title, body;
        boolean test = false;   // popup thử nghiệm: hiện chữ THỬ NGHIỆM nhấp nháy ở trên đầu
        // Các nút: mặc định 1 nút "Xác nhận"
        String[] labels = {"Xác nhận"};
        int[] actions = {ActionReceiver.ACT_OK};
        String[] colors = {"#2E9E4F"};

        /** Lần nhắc 1: Chấm công / Nghỉ phép / Nghỉ tự do. */
        void threeButtons() {
            labels = new String[]{"Chấm công", "Nghỉ phép", "Nghỉ tự do"};
            actions = new int[]{ActionReceiver.ACT_OK, ActionReceiver.ACT_LEAVE, ActionReceiver.ACT_OFF};
            colors = new String[]{"#2E9E4F", "#1E73D9", "#D93636"};
        }
    }

    static Content content(Context c, String day, int type) {
        Content ct = new Content();
        if ("TEST".equals(day)) {
            // Popup thử nghiệm: hiển thị y hệt lần nhắc chấm công thật (lần 1) của ca gần nhất
            Schedule.Info in = Schedule.nextWorkInfo(new Store(c).kip());
            fill(ct, in, 0);
            ct.test = true;
            return ct;
        }
        fill(ct, Schedule.info(LocalDate.parse(day), new Store(c).kip()), type);
        return ct;
    }

    private static void fill(Content ct, Schedule.Info in, int type) {
        boolean hasShift = in != null && in.start != null;
        String label = hasShift ? in.label : "Ca làm việc";
        String start = hasShift ? in.start.format(HM) : LocalTime.now().format(HM);
        String end = hasShift && in.end != null ? in.end.format(HM) : start;
        switch (type) {
            case 0:
                ct.kicker = "CHẤM CÔNG VÀO CA";
                ct.time = start;
                ct.title = "Sắp vào " + label.toLowerCase();
                ct.body = label + " bắt đầu lúc " + start + " (còn " + Schedule.REMIND_MIN + " phút).\nHãy chấm công vào ca.";
                ct.threeButtons();
                break;
            case 1:
                ct.kicker = "KÝ SỔ CÔNG VIỆC";
                ct.time = start;
                ct.title = "Nhớ ký sổ công việc";
                ct.body = "Đã qua giờ vào " + label.toLowerCase() + " (" + start + ") " + Schedule.REMIND_MIN + " phút.\nHãy ký sổ công việc.";
                break;
            default:
                ct.kicker = "CHẤM CÔNG RA VỀ";
                ct.time = end;
                ct.title = "Sắp hết " + label.toLowerCase();
                ct.body = label + " kết thúc lúc " + end + " (còn " + Schedule.REMIND_MIN + " phút).\nHãy chấm công ra về.";
                break;
        }
    }

    static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        try {
            nm.deleteNotificationChannel(LEGACY_CHANNEL);
        } catch (Throwable ignored) {
        }
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Nhắc nhở chấm công",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Popup nhắc chấm công, ký sổ, chấm công ra về (chuông/rung do app điều khiển)");
        ch.setSound(null, null);
        ch.enableVibration(false);
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
        Store st = new Store(c);
        if (st.sound()) startSound(c, type);
        if (st.vibrate()) startVibration(c);
        showOverlay(c, day, type, ct);
        H.removeCallbacks(TIMEOUT);
        H.postDelayed(TIMEOUT, MAX_MS);
    }

    private static PendingIntent actionPi(Context c, String day, int type, int action) {
        Intent i = new Intent(c, ActionReceiver.class);
        i.setAction(ActionReceiver.ACTION);
        i.putExtra("day", day);
        i.putExtra("type", type);
        i.putExtra("action", action);
        return PendingIntent.getBroadcast(c, 100 + action, i,
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
        Notification.Builder nb = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_clock)
                .setContentTitle((ct.test ? "[THỬ NGHIỆM] " : "") + ct.kicker + " - " + ct.time)
                .setContentText(ct.title)
                .setStyle(new Notification.BigTextStyle().bigText(ct.body))
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(fsPi)
                .setFullScreenIntent(fsPi, true);
        for (int k = 0; k < ct.labels.length; k++) {
            nb.addAction(new Notification.Action.Builder(icon, ct.labels[k],
                    actionPi(c, day, type, ct.actions[k])).build());
        }
        Notification n = nb.build();
        try {
            c.getSystemService(NotificationManager.class).notify(NOTIF_ID, n);
        } catch (Throwable ignored) {
        }
    }

    // ---- Tiếng chuông: lần nhắc 2 (ký sổ) dùng ki_so_cv.mp3, lần 1 và 3 dùng cham_cong.mp3.
    // Phát qua kênh âm lượng báo thức, lặp lại (nghỉ vài giây) cho tới khi bấm nút hoặc hết thời gian chờ.
    private static MediaPlayer player;
    private static final Runnable REPLAY = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                try {
                    player.start();
                } catch (Throwable ignored) {
                }
            }
        }
    };

    private static void startSound(Context c, int type) {
        stopSound();
        try {
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            MediaPlayer mp = MediaPlayer.create(c, type == 1 ? R.raw.ki_so_cv : R.raw.cham_cong,
                    aa, AudioManager.AUDIO_SESSION_ID_GENERATE);
            if (mp == null) return;
            mp.setWakeMode(c, PowerManager.PARTIAL_WAKE_LOCK);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer m) {
                    H.postDelayed(REPLAY, SOUND_GAP_MS);
                }
            });
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer m, int what, int extra) {
                    stopSound();
                    return true;
                }
            });
            player = mp;
            mp.start();
        } catch (Throwable ignored) {
            stopSound();
        }
    }

    private static void stopSound() {
        H.removeCallbacks(REPLAY);
        MediaPlayer mp = player;
        player = null;
        if (mp != null) {
            try {
                mp.stop();
            } catch (Throwable ignored) {
            }
            try {
                mp.release();
            } catch (Throwable ignored) {
            }
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
            View v = PopupView.build(c, ct, new java.util.function.Consumer<Integer>() {
                @Override
                public void accept(Integer action) {
                    ActionReceiver.apply(c, day, type, action);
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

    /** Tắt chuông và rung, xóa thông báo, gỡ lớp phủ, đóng màn hình popup. */
    static void dismiss(Context ctx) {
        Context c = ctx.getApplicationContext();
        H.removeCallbacks(TIMEOUT);
        stopSound();
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
