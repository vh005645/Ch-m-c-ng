package com.chamcong.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;
import java.util.function.Consumer;

/** Giao diện popup toàn màn hình (dùng cho cả lớp phủ và AlertActivity). */
final class PopupView {

    private static final Locale VI = Locale.forLanguageTag("vi");

    /** @param cb nhận mã hành động ActionReceiver.ACT_* của nút được bấm. */
    static View build(Context c, Popup.Content ct, final Consumer<Integer> cb) {
        float d = c.getResources().getDisplayMetrics().density;
        int navy = Color.parseColor("#0B1F3A");

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(navy);
        root.setPadding((int) (28 * d), (int) (64 * d), (int) (28 * d), (int) (40 * d));

        // Chữ THỬ NGHIỆM nhấp nháy trên đầu (chỉ khi bấm "Thử popup" bên Cài đặt)
        if (ct.test) {
            TextView banner = tv(c, "THỬ NGHIỆM", 20, Color.WHITE, true);
            banner.setLetterSpacing(0.12f);
            banner.setPadding((int) (26 * d), (int) (8 * d), (int) (26 * d), (int) (8 * d));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#D93636"));
            bg.setCornerRadius(24 * d);
            banner.setBackground(bg);
            AlphaAnimation blink = new AlphaAnimation(1f, 0.1f);
            blink.setDuration(550);
            blink.setRepeatMode(Animation.REVERSE);
            blink.setRepeatCount(Animation.INFINITE);
            banner.startAnimation(blink);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
            bp.bottomMargin = (int) (18 * d);
            root.addView(banner, bp);
        }

        TextView kicker = tv(c, ct.kicker, 15, Color.parseColor("#FFD54F"), true);
        kicker.setLetterSpacing(0.08f);
        root.addView(kicker);

        TextView time = tv(c, ct.time, 76, Color.WHITE, true);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
        tl.topMargin = (int) (12 * d);
        root.addView(time, tl);

        TextView title = tv(c, ct.title, 26, Color.WHITE, true);
        LinearLayout.LayoutParams t2 = new LinearLayout.LayoutParams(-2, -2);
        t2.topMargin = (int) (8 * d);
        root.addView(title, t2);

        TextView body = tv(c, ct.body, 18, Color.parseColor("#C9D6E8"), false);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-2, -2);
        bl.topMargin = (int) (18 * d);
        root.addView(body, bl);

        View spacer = new View(c);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));

        int btnH = (int) ((ct.labels.length >= 3 ? 64 : 76) * d);
        for (int k = 0; k < ct.labels.length; k++) {
            final int action = ct.actions[k];
            TextView b = button(c, ct.labels[k].toUpperCase(VI), ct.colors[k]);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cb.accept(action);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, btnH);
            if (k > 0) lp.topMargin = (int) (12 * d);
            root.addView(b, lp);
        }

        // Bọc trong ScrollView để máy màn hình nhỏ / chữ lớn vẫn bấm được hết các nút
        ScrollView sv = new ScrollView(c);
        sv.setFillViewport(true);
        sv.setBackgroundColor(navy);
        sv.addView(root, new FrameLayout.LayoutParams(-1, -2));
        return sv;
    }

    private static TextView tv(Context c, String text, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private static TextView button(Context c, String text, String color) {
        float d = c.getResources().getDisplayMetrics().density;
        TextView b = tv(c, text, 20, Color.WHITE, true);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(color));
        g.setCornerRadius(18 * d);
        b.setBackground(g);
        b.setClickable(true);
        return b;
    }
}
