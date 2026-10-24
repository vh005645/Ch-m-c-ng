package com.chamcong.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import java.util.function.Consumer;

/** Màn hình popup hiện trên màn hình khóa (full-screen intent). */
public class AlertActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Popup.activity = this;
        render(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Popup.activity = this;
        render(intent);
    }

    private void render(Intent i) {
        final String day = i.getStringExtra("day");
        final int type = i.getIntExtra("type", 0);
        if (day == null) {
            finish();
            return;
        }
        Popup.Content ct = Popup.content(this, day, type);
        View v = PopupView.build(this, ct, new Consumer<Integer>() {
            @Override
            public void accept(Integer action) {
                ActionReceiver.apply(AlertActivity.this, day, type, action);
                finish();
            }
        });
        setContentView(v);
        WindowInsetsController ic = getWindow().getInsetsController();
        if (ic != null) {
            ic.setSystemBarsAppearance(0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
    }

    @Override
    protected void onDestroy() {
        if (Popup.activity == this) Popup.activity = null;
        super.onDestroy();
    }
}
