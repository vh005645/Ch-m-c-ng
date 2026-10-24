package com.chamcong.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.function.Consumer;

/** Giao diện popup toàn màn hình (dùng cho cả lớp phủ và AlertActivity). */
final class PopupView {

    static View build(Context c, Popup.Content ct, final Consumer<Boolean> cb) {
        float d = c.getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.parseColor("#0B1F3A"));
        root.setPadding((int) (28 * d), (int) (72 * d), (int) (28 * d), (int) (56 * d));

        TextView kicker = tv(c, ct.kicker, 15, Color.parseColor("#FFD54F"), true);
        kicker.setLetterSpacing(0.08f);
        root.addView(kicker);

        TextView time = tv(c, ct.time, 76, Color.WHITE, true);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
        tl.topMargin = (int) (16 * d);
        root.addView(time, tl);

        TextView title = tv(c, ct.title, 26, Color.WHITE, true);
        LinearLayout.LayoutParams t2 = new LinearLayout.LayoutParams(-2, -2);
        t2.topMargin = (int) (8 * d);
        root.addView(title, t2);

        TextView body = tv(c, ct.body, 18, Color.parseColor("#C9D6E8"), false);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-2, -2);
        bl.topMargin = (int) (20 * d);
        root.addView(body, bl);

        View spacer = new View(c);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));

        TextView ok = button(c, ct.confirmText, "#2E9E4F");
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cb.accept(true);
            }
        });
        LinearLayout.LayoutParams ol = new LinearLayout.LayoutParams(-1, (int) (76 * d));
        root.addView(ok, ol);

        TextView off = button(c, ct.offText, "#D93636");
        off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cb.accept(false);
            }
        });
        LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(-1, (int) (76 * d));
        fl.topMargin = (int) (14 * d);
        root.addView(off, fl);
        return root;
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
