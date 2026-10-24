package com.chamcong.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MainActivity extends Activity {

    private static final int GREEN = 0xFF2E9E4F, RED = 0xFFD93636, AMBER = 0xFFFFE3A3,
            BLUE = 0xFF1E73D9, DARK = 0xFF1F2933, GRAY = 0xFF7B8794, BG = 0xFFF4F6FA;
    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DMHM = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final String[] DOW = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};
    private static final String[] DOW_SHORT = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private Store store;
    private YearMonth shown;
    private boolean updating = false;

    private Switch sw;
    private FrameLayout content;
    private View calView, setView;
    private TextView tabCal, tabSet;

    private TextView todayInfo, monthLabel, summary;
    private Button prev, next;
    private LinearLayout grid;

    private RadioGroup kipGroup;
    private TextView nextInfo, planText;
    private LinearLayout permBox;

    private final CompoundButton.OnCheckedChangeListener swListener =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton b, boolean on) {
                    store.setEnabled(on);
                    if (!on) Popup.dismiss(MainActivity.this);
                    Scheduler.scheduleNext(MainActivity.this);
                    refreshSettings();
                }
            };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        store.prune(LocalDate.now().getYear());
        shown = YearMonth.now();
        buildUi();
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        showTab(store.kip() < 0 ? 1 : 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        store.prune(LocalDate.now().getYear());
        Scheduler.scheduleNext(this);
        refreshCal();
        refreshSettings();
    }

    // ------------------------------------------------------------------ UI

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, ins) -> {
            Insets i = ins.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(i.left, i.top, i.right, i.bottom);
            return WindowInsets.CONSUMED;
        });

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(10), dp(16), dp(6));
        TextView title = text("Chấm công", 22, DARK, true);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        sw = new Switch(this);
        sw.setText("Bật nhắc nhở  ");
        sw.setTextSize(14);
        sw.setTextColor(DARK);
        sw.setChecked(store.enabled());
        sw.setOnCheckedChangeListener(swListener);
        header.addView(sw);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        content = new FrameLayout(this);
        calView = buildCalendar();
        setView = buildSettings();
        content.addView(calView, new FrameLayout.LayoutParams(-1, -1));
        content.addView(setView, new FrameLayout.LayoutParams(-1, -1));
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setBackgroundColor(Color.WHITE);
        tabCal = tab("Lịch chấm công", 0);
        tabSet = tab("Cài đặt ca-kíp", 1);
        tabs.addView(tabCal, new LinearLayout.LayoutParams(0, dp(54), 1f));
        tabs.addView(tabSet, new LinearLayout.LayoutParams(0, dp(54), 1f));
        root.addView(tabs, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
        WindowInsetsController ic = getWindow().getInsetsController();
        if (ic != null) {
            ic.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
    }

    private TextView tab(String s, final int idx) {
        TextView t = text(s, 15, GRAY, true);
        t.setGravity(Gravity.CENTER);
        t.setOnClickListener(v -> showTab(idx));
        return t;
    }

    private void showTab(int i) {
        calView.setVisibility(i == 0 ? View.VISIBLE : View.GONE);
        setView.setVisibility(i == 1 ? View.VISIBLE : View.GONE);
        tabCal.setTextColor(i == 0 ? BLUE : GRAY);
        tabSet.setTextColor(i == 1 ? BLUE : GRAY);
    }

    // ------------------------------------------------------------ Tab 1: lịch

    private View buildCalendar() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(10), dp(4), dp(10), dp(4));

        todayInfo = text("", 14, DARK, false);
        todayInfo.setPadding(dp(6), 0, dp(6), dp(6));
        v.addView(todayInfo);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        prev = new Button(this);
        prev.setText("‹");
        prev.setTextSize(20);
        prev.setOnClickListener(x -> {
            if (shown.getMonthValue() > 1) {
                shown = shown.minusMonths(1);
                refreshCal();
            }
        });
        next = new Button(this);
        next.setText("›");
        next.setTextSize(20);
        next.setOnClickListener(x -> {
            if (shown.getMonthValue() < 12) {
                shown = shown.plusMonths(1);
                refreshCal();
            }
        });
        monthLabel = text("", 19, DARK, true);
        monthLabel.setGravity(Gravity.CENTER);
        nav.addView(prev, new LinearLayout.LayoutParams(dp(56), dp(44)));
        nav.addView(monthLabel, new LinearLayout.LayoutParams(0, -2, 1f));
        nav.addView(next, new LinearLayout.LayoutParams(dp(56), dp(44)));
        v.addView(nav);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 7; i++) {
            TextView t = text(DOW_SHORT[i], 13, i == 6 ? RED : GRAY, true);
            t.setGravity(Gravity.CENTER);
            head.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        head.setPadding(0, dp(6), 0, dp(4));
        v.addView(head);

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        v.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        legend.setGravity(Gravity.CENTER);
        legend.setPadding(0, dp(6), 0, dp(2));
        legend.addView(legendItem(GREEN, "Đã chấm công"));
        legend.addView(legendItem(RED, "Nghỉ"));
        legend.addView(legendItem(AMBER, "Chưa xác nhận"));
        v.addView(legend);

        TextView hint = text("đ = ca đầu, c = ca cuối. Bấm vào ngày để sửa.", 11, GRAY, false);
        hint.setGravity(Gravity.CENTER);
        v.addView(hint);

        summary = text("", 19, DARK, true);
        summary.setGravity(Gravity.CENTER);
        summary.setPadding(0, dp(8), 0, dp(8));
        v.addView(summary);
        return v;
    }

    private View legendItem(int color, String label) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(dp(8), 0, dp(8), 0);
        View sq = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(3));
        sq.setBackground(g);
        l.addView(sq, new LinearLayout.LayoutParams(dp(12), dp(12)));
        TextView t = text(" " + label, 12, DARK, false);
        l.addView(t);
        return l;
    }

    private void refreshCal() {
        if (grid == null) return;
        int kip = store.kip();
        LocalDate today = LocalDate.now();

        if (kip < 0) {
            todayInfo.setText("Chưa chọn kíp - vào tab Cài đặt ca-kíp để chọn.");
        } else {
            Schedule.Info in = Schedule.info(today, kip);
            String t = in.work
                    ? in.label + " (" + in.start.format(HM) + " - " + in.end.format(HM) + ")"
                    : in.label;
            todayInfo.setText("Hôm nay " + today.format(DM) + " - " + Schedule.KIP_NAMES[kip] + ": " + t);
        }

        monthLabel.setText("Tháng " + shown.getMonthValue() + "/" + shown.getYear());
        prev.setEnabled(shown.getMonthValue() > 1);
        next.setEnabled(shown.getMonthValue() < 12);
        prev.setAlpha(prev.isEnabled() ? 1f : 0.3f);
        next.setAlpha(next.isEnabled() ? 1f : 0.3f);

        grid.removeAllViews();
        int startCol = shown.atDay(1).getDayOfWeek().getValue() - 1;
        int len = shown.lengthOfMonth();
        int rows = (startCol + len + 6) / 7;
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            grid.addView(row, new LinearLayout.LayoutParams(-1, 0, 1f));
            for (int c = 0; c < 7; c++) {
                int dayNum = r * 7 + c - startCol + 1;
                if (dayNum < 1 || dayNum > len) {
                    row.addView(new View(this), new LinearLayout.LayoutParams(0, -1, 1f));
                    continue;
                }
                final LocalDate date = shown.atDay(dayNum);
                row.addView(makeCell(date, kip, today), cellParams());
            }
        }

        int[] cnt = store.count(shown);
        summary.setText("Tháng " + shown.getMonthValue() + ": " + cnt[0] + " công / " + cnt[1] + " nghỉ");
    }

    private LinearLayout.LayoutParams cellParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        return lp;
    }

    private View makeCell(final LocalDate date, int kip, LocalDate today) {
        Schedule.Info in = Schedule.info(date, kip);
        String st = store.status(date.toString());
        int bg, fg = DARK;
        float alpha = 1f;
        String label;
        if (Store.WORK.equals(st) || Store.OT.equals(st)) {
            bg = GREEN;
            fg = Color.WHITE;
            label = Store.OT.equals(st) ? "TC" : in.shortLabel;
        } else if (Store.OFF.equals(st)) {
            bg = RED;
            fg = Color.WHITE;
            label = "nghỉ";
        } else if (in.work) {
            bg = date.isBefore(today) ? AMBER : Color.WHITE;
            label = in.shortLabel;
        } else {
            bg = Color.WHITE;
            alpha = 0.35f;
            label = "nghỉ";
        }

        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(bg);
        g.setCornerRadius(dp(10));
        if (date.equals(today)) g.setStroke(dp(3), BLUE);
        else g.setStroke(dp(1), 0xFFDDE3EC);
        cell.setBackground(g);
        cell.setAlpha(alpha);
        cell.addView(text(String.valueOf(date.getDayOfMonth()), 17, fg, true));
        TextView l = text(label, 11, fg, false);
        cell.addView(l);
        cell.setOnClickListener(x -> editDay(date));
        return cell;
    }

    private void editDay(final LocalDate d) {
        Schedule.Info in = Schedule.info(d, store.kip());
        final String key = d.toString();
        final String[] items;
        final String[] vals;
        if (in.work) {
            items = new String[]{"Đã chấm công (tính công)", "Nghỉ", "Xóa trạng thái"};
            vals = new String[]{Store.WORK, Store.OFF, null};
        } else {
            items = new String[]{"Tăng ca (tính công)", "Xóa trạng thái"};
            vals = new String[]{Store.OT, null};
        }
        String title = DOW[d.getDayOfWeek().getValue() - 1] + ", " + d.format(DM) + "/" + d.getYear()
                + (in.work ? " - " + in.label : " - ngày nghỉ");
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(items, (dlg, which) -> {
                    store.setStatus(key, vals[which]);
                    refreshCal();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    // ----------------------------------------------------------- Tab 2: cài đặt

    private View buildSettings() {
        ScrollView sv = new ScrollView(this);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(8), dp(16), dp(24));
        sv.addView(l);

        l.addView(section("Kíp đang làm"));
        kipGroup = new RadioGroup(this);
        for (int i = 0; i < 5; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(100 + i);
            rb.setText(i < 4 ? Schedule.KIP_NAMES[i] : "Hành chính (nghỉ thứ 7, chủ nhật)");
            rb.setTextSize(17);
            rb.setTextColor(DARK);
            rb.setPadding(dp(8), dp(8), 0, dp(8));
            kipGroup.addView(rb);
        }
        kipGroup.setOnCheckedChangeListener((g, id) -> {
            if (updating || id < 100) return;
            store.setKip(id - 100);
            Scheduler.scheduleNext(this);
            refreshCal();
            refreshSettings();
        });
        l.addView(kipGroup);

        nextInfo = text("", 15, DARK, true);
        nextInfo.setPadding(0, dp(12), 0, dp(4));
        l.addView(nextInfo);

        l.addView(section("Lịch 8 ngày tới"));
        planText = text("", 14, DARK, false);
        planText.setLineSpacing(dp(3), 1f);
        l.addView(planText);

        l.addView(section("Giờ làm"));
        TextView hours = text("Ca 1: 06:00 - 14:00\nCa 2: 14:00 - 22:00\nCa 3: 22:00 - 06:00 hôm sau\n"
                + "Hành chính: 07:00 - 11:30, 13:30 - 17:00\n\n"
                + "Nhắc lúc: trước giờ vào 5 phút (chấm công), sau giờ vào 5 phút (ký sổ), và khi hết ca "
                + "(xác nhận chấm công ra về để tính công).", 14, DARK, false);
        hours.setLineSpacing(dp(3), 1f);
        l.addView(hours);

        l.addView(section("Quyền cần cấp để popup hoạt động"));
        permBox = new LinearLayout(this);
        permBox.setOrientation(LinearLayout.VERTICAL);
        l.addView(permBox);

        TextView tip = text("iQOO/OriginOS: vào Cài đặt > Ứng dụng > Chấm công > bật \"Tự khởi động\" và "
                + "Pin/Hoạt động nền = \"Không hạn chế\" để báo thức không bị hệ thống chặn. "
                + "Không vuốt \"Buộc dừng\" ứng dụng.", 13, GRAY, false);
        tip.setPadding(0, dp(8), 0, dp(8));
        l.addView(tip);

        Button appSettings = new Button(this);
        appSettings.setAllCaps(false);
        appSettings.setText("Mở cài đặt ứng dụng");
        appSettings.setOnClickListener(x -> safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()))));
        l.addView(appSettings);

        Button test = new Button(this);
        test.setAllCaps(false);
        test.setText("Thử popup sau 10 giây");
        test.setOnClickListener(x -> {
            Scheduler.scheduleTest(this, 10_000);
            Toast.makeText(this, "Hãy khóa màn hình hoặc thoát app - popup sẽ hiện sau 10 giây",
                    Toast.LENGTH_LONG).show();
        });
        l.addView(test);
        return sv;
    }

    private TextView section(String s) {
        TextView t = text(s, 16, BLUE, true);
        t.setPadding(0, dp(18), 0, dp(6));
        return t;
    }

    private void refreshSettings() {
        if (kipGroup == null) return;
        int k = store.kip();
        updating = true;
        if (k >= 0) kipGroup.check(100 + k);
        else kipGroup.clearCheck();
        sw.setOnCheckedChangeListener(null);
        sw.setChecked(store.enabled());
        sw.setOnCheckedChangeListener(swListener);
        updating = false;

        if (!store.enabled()) {
            nextInfo.setText("App đang TẮT - sẽ không có nhắc nhở nào.");
        } else if (k < 0) {
            nextInfo.setText("Hãy chọn kíp đang làm để bật nhắc nhở.");
        } else {
            Scheduler.Next n = Scheduler.peek(this);
            if (n == null) {
                nextInfo.setText("Chưa có nhắc nhở nào sắp tới.");
            } else {
                String when = Instant.ofEpochMilli(n.event.time).atZone(ZoneId.systemDefault()).format(DMHM);
                nextInfo.setText("Nhắc nhở kế tiếp: " + Schedule.typeName(n.event.type) + " - " + when);
            }
        }

        StringBuilder sb = new StringBuilder();
        if (k < 0) {
            sb.append("(chọn kíp để xem lịch)");
        } else {
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 8; i++) {
                LocalDate d = today.plusDays(i);
                Schedule.Info in = Schedule.info(d, k);
                sb.append(DOW_SHORT[d.getDayOfWeek().getValue() - 1]).append(" ").append(d.format(DM))
                        .append(": ").append(in.label);
                if (in.work) {
                    sb.append("  (").append(in.start.format(HM)).append("-").append(in.end.format(HM)).append(")");
                }
                if (i < 7) sb.append("\n");
            }
        }
        planText.setText(sb.toString());

        permBox.removeAllViews();
        addPerm("Thông báo", notifOk(), () -> safeStart(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
        addPerm("Thông báo toàn màn hình (hiện trên màn hình khóa)", fsiOk(), () -> {
            if (Build.VERSION.SDK_INT >= 34) {
                safeStart(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:" + getPackageName())));
            }
        });
        addPerm("Hiển thị trên ứng dụng khác (popup che màn hình khi đang dùng máy)",
                Settings.canDrawOverlays(this), () -> safeStart(new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        addPerm("Không tối ưu pin", batteryOk(), () -> {
            try {
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName())));
            } catch (Throwable t) {
                safeStart(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
        });
    }

    private void addPerm(String name, boolean ok, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = text((ok ? "✔ " : "✘ ") + name, 14, ok ? GREEN : RED, false);
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        if (!ok) {
            Button b = new Button(this);
            b.setAllCaps(false);
            b.setText("Cấp quyền");
            b.setOnClickListener(x -> action.run());
            row.addView(b, new LinearLayout.LayoutParams(-2, dp(44)));
        }
        permBox.addView(row);
    }

    private boolean notifOk() {
        return getSystemService(NotificationManager.class).areNotificationsEnabled();
    }

    private boolean fsiOk() {
        if (Build.VERSION.SDK_INT >= 34) {
            return getSystemService(NotificationManager.class).canUseFullScreenIntent();
        }
        return true;
    }

    private boolean batteryOk() {
        return getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName());
    }

    private void safeStart(Intent i) {
        try {
            startActivity(i);
        } catch (Throwable t) {
            Toast.makeText(this, "Không mở được màn hình cài đặt, hãy vào Cài đặt > Ứng dụng > Chấm công",
                    Toast.LENGTH_LONG).show();
        }
    }
}
