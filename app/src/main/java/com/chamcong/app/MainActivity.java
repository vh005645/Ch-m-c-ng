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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class MainActivity extends Activity {

    private static final int GREEN = 0xFF2E9E4F, RED = 0xFFD93636, AMBER = 0xFFFFE3A3,
            BLUE = 0xFF1E73D9, DARK = 0xFF1F2933, GRAY = 0xFF7B8794, BG = 0xFFF4F6FA,
            TAB_ON = 0xFF4DA3FF, TAB_OFF = 0xFF8A94A0;
    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] DOW = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};
    private static final String[] DOW_SHORT = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private Store store;
    private YearMonth shown;
    private boolean updating = false;

    private Switch sw;
    private FrameLayout content;
    private View calView, setView;
    private TextView tabCal, tabSet;
    private LinearLayout colCal, colSet;
    private View indCal, indSet;

    private TextView dateLine, kipLine, monthLabel, summary, summary2;
    private Button prev, next;
    private LinearLayout grid;

    private RadioGroup kipGroup;
    private TextView planText;
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
        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        final LinearLayout header = new LinearLayout(this);
        final LinearLayout tabs = new LinearLayout(this);

        // Header trắng ôm sát thanh trạng thái; thanh tab đen ôm sát thanh điều hướng
        root.setOnApplyWindowInsetsListener((v, ins) -> {
            Insets i = ins.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(i.left, 0, i.right, 0);
            header.setPadding(dp(16), i.top + dp(8), dp(16), dp(8));
            tabs.setPadding(0, 0, 0, i.bottom);
            return WindowInsets.CONSUMED;
        });

        // ---- Header: logo (góc trái) + "Nhiệt Khí Thải" bên dưới; công tắc nhắc nhở bên phải
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(Color.WHITE);
        header.setPadding(dp(16), dp(8), dp(16), dp(8));

        // Logo và chữ cùng căn giữa theo một trục dọc (chữ nằm ngay dưới, chính giữa logo)
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo_ht);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("Logo");
        brand.addView(logo, new LinearLayout.LayoutParams(-2, dp(54)));
        TextView dept = text("Nhiệt Khí Thải", 13, DARK, true);
        dept.setGravity(Gravity.CENTER);
        dept.setPadding(0, dp(2), 0, 0);
        brand.addView(dept, new LinearLayout.LayoutParams(-2, -2));
        header.addView(brand, new LinearLayout.LayoutParams(-2, -2));
        header.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));   // đẩy công tắc sang phải

        sw = new Switch(this);
        sw.setText("Bật nhắc nhở  ");
        sw.setTextSize(14);
        sw.setTextColor(DARK);
        sw.setChecked(store.enabled());
        sw.setOnCheckedChangeListener(swListener);
        header.addView(sw);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        View line = new View(this);
        line.setBackgroundColor(0xFFDDE3EC);
        root.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));

        content = new FrameLayout(this);
        calView = buildCalendar();
        setView = buildSettings();
        content.addView(calView, new FrameLayout.LayoutParams(-1, -1));
        content.addView(setView, new FrameLayout.LayoutParams(-1, -1));
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));

        // ---- Thanh tab: nền đen, 2 tab ngăn cách bằng vạch dọc, tab đang chọn có gạch sáng phía trên
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        tabs.setBackgroundColor(Color.BLACK);
        tabCal = text("Lịch chấm công", 15, TAB_OFF, true);
        tabSet = text("Cài đặt ca-kíp", 15, TAB_OFF, true);
        indCal = new View(this);
        indSet = new View(this);
        colCal = tabColumn(tabCal, indCal, 0);
        colSet = tabColumn(tabSet, indSet, 1);
        tabs.addView(colCal, new LinearLayout.LayoutParams(0, dp(56), 1f));
        View sep = new View(this);
        sep.setBackgroundColor(0xFF5A6470);
        tabs.addView(sep, new LinearLayout.LayoutParams(dp(1), dp(32)));
        tabs.addView(colSet, new LinearLayout.LayoutParams(0, dp(56), 1f));
        root.addView(tabs, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
        WindowInsetsController ic = getWindow().getInsetsController();
        if (ic != null) {
            // Thanh trạng thái (nền trắng): icon tối. Thanh điều hướng (nền đen): icon sáng.
            ic.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
    }

    private LinearLayout tabColumn(TextView label, View indicator, final int idx) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        label.setGravity(Gravity.CENTER);
        col.addView(indicator, new LinearLayout.LayoutParams(-1, dp(3)));
        col.addView(label, new LinearLayout.LayoutParams(-1, 0, 1f));
        col.setOnClickListener(v -> showTab(idx));
        return col;
    }

    private void showTab(int i) {
        calView.setVisibility(i == 0 ? View.VISIBLE : View.GONE);
        setView.setVisibility(i == 1 ? View.VISIBLE : View.GONE);
        tabCal.setTextColor(i == 0 ? Color.WHITE : TAB_OFF);
        tabSet.setTextColor(i == 1 ? Color.WHITE : TAB_OFF);
        indCal.setBackgroundColor(i == 0 ? TAB_ON : Color.TRANSPARENT);
        indSet.setBackgroundColor(i == 1 ? TAB_ON : Color.TRANSPARENT);
        colCal.setBackgroundColor(i == 0 ? 0xFF1C2127 : Color.BLACK);
        colSet.setBackgroundColor(i == 1 ? 0xFF1C2127 : Color.BLACK);
    }

    // ------------------------------------------------------------ Tab 1: lịch

    private View buildCalendar() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(10), dp(4), dp(10), dp(4));

        // Dòng 1: ngày tháng năm. Dòng 2: Kíp ... - trạng thái (ca 1 đầu, ca 2 cuối, tour xịn ...)
        dateLine = text("", 15, GRAY, true);
        dateLine.setPadding(dp(6), dp(2), dp(6), 0);
        v.addView(dateLine);
        kipLine = text("", 18, DARK, true);
        kipLine.setPadding(dp(6), 0, dp(6), dp(6));
        v.addView(kipLine);

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
        legend.addView(legendItem(GREEN, "Chấm công"));
        legend.addView(legendItem(RED, "Chấm nghỉ"));
        legend.addView(legendItem(AMBER, "Chưa chấm"));
        v.addView(legend);

        TextView hint = text("đ = ca đầu, c = ca cuối, chấm xanh nhỏ = nghỉ phép (vẫn tính công). Bấm vào ngày để sửa.", 11, GRAY, false);
        hint.setGravity(Gravity.CENTER);
        v.addView(hint);

        summary = text("", 19, DARK, true);
        summary.setGravity(Gravity.CENTER);
        summary.setPadding(0, dp(8), 0, 0);
        v.addView(summary);
        summary2 = text("", 13, GRAY, false);
        summary2.setGravity(Gravity.CENTER);
        summary2.setPadding(0, 0, 0, dp(8));
        v.addView(summary2);
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

        dateLine.setText(DOW[today.getDayOfWeek().getValue() - 1] + ", " + today.format(DMY));
        if (kip < 0) {
            kipLine.setText("Chưa chọn kíp - vào tab Cài đặt ca-kíp để chọn.");
        } else {
            Schedule.Info in = Schedule.info(today, kip);
            String state = (kip == Schedule.KIP_HC && in.work) ? "Đi làm" : in.label;
            kipLine.setText(Schedule.KIP_NAMES[kip] + " - " + state);
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
        if (cnt[2] > 0) {
            summary2.setText("(trong đó " + cnt[2] + " ngày nghỉ phép, vẫn tính công)");
            summary2.setVisibility(View.VISIBLE);
        } else {
            summary2.setText("");
            summary2.setVisibility(View.GONE);
        }
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
        boolean leave = false;
        if (Store.WORK.equals(st) || Store.OT.equals(st)) {
            bg = GREEN;
            fg = Color.WHITE;
            label = Store.OT.equals(st) ? "TC" : in.shortLabel;
        } else if (Store.LEAVE.equals(st)) {
            // Nghỉ phép: vẫn hiện ô đỏ như nghỉ, thêm chấm xanh ở góc dưới (vẫn tính công)
            bg = RED;
            fg = Color.WHITE;
            label = "nghỉ";
            leave = true;
        } else if (Store.OFF.equals(st)) {
            bg = RED;
            fg = Color.WHITE;
            label = "nghỉ";
        } else if (in.work) {
            bg = date.isBefore(today) ? AMBER : Color.WHITE;
            label = in.shortLabel;
        } else {
            // Ngày không làm việc: hiện mờ, chữ "tour"
            bg = Color.WHITE;
            alpha = 0.35f;
            label = kip < 0 ? "" : "tour";
        }

        FrameLayout cell = new FrameLayout(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(bg);
        g.setCornerRadius(dp(10));
        if (date.equals(today)) g.setStroke(dp(3), BLUE);
        else g.setStroke(dp(1), 0xFFDDE3EC);
        cell.setBackground(g);
        cell.setAlpha(alpha);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.addView(text(String.valueOf(date.getDayOfMonth()), 17, fg, true));
        inner.addView(text(label, 11, fg, false));
        cell.addView(inner, new FrameLayout.LayoutParams(-1, -1));

        if (leave) {
            View dot = new View(this);
            GradientDrawable dg = new GradientDrawable();
            dg.setShape(GradientDrawable.OVAL);
            dg.setColor(GREEN);
            dg.setStroke(dp(1), Color.WHITE);
            dot.setBackground(dg);
            FrameLayout.LayoutParams dl = new FrameLayout.LayoutParams(dp(9), dp(9));
            dl.gravity = Gravity.BOTTOM | Gravity.END;
            dl.setMargins(0, 0, dp(4), dp(4));
            cell.addView(dot, dl);
        }
        cell.setOnClickListener(x -> editDay(date));
        return cell;
    }

    private void editDay(final LocalDate d) {
        Schedule.Info in = Schedule.info(d, store.kip());
        final String key = d.toString();
        final String[] items;
        final String[] vals;
        if (in.work) {
            items = new String[]{"Đã chấm công", "Nghỉ phép", "Nghỉ tự do", "Xóa trạng thái"};
            vals = new String[]{Store.WORK, Store.LEAVE, Store.OFF, null};
        } else {
            items = new String[]{"Tăng ca (tính công)", "Xóa trạng thái"};
            vals = new String[]{Store.OT, null};
        }
        String title = DOW[d.getDayOfWeek().getValue() - 1] + ", " + d.format(DM) + "/" + d.getYear()
                + " - " + in.label;
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(items, (dlg, which) -> {
                    store.setStatus(key, vals[which]);
                    Scheduler.scheduleNext(this);
                    refreshCal();
                    refreshSettings();
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

        l.addView(section("Lịch 8 ngày tới"));
        planText = text("", 14, DARK, false);
        planText.setLineSpacing(dp(3), 1f);
        l.addView(planText);

        l.addView(section("Giờ làm"));
        TextView hours = text("Ca 1: 06:00 - 14:00\nCa 2: 14:00 - 22:00\nCa 3: 22:00 - 06:00 hôm sau\n"
                + "Hành chính: 07:00 - 11:30, 13:30 - 17:00\n\n"
                + "Nhắc 3 lần mỗi ca (chỉ nhắc ngày đi làm, ngày tour không nhắc):\n"
                + "1) Trước giờ vào 5 phút: chấm công (Chấm công / Nghỉ phép / Nghỉ tự do)\n"
                + "2) Sau giờ vào 5 phút: ký sổ công việc (Xác nhận)\n"
                + "3) Trước giờ về 5 phút: chấm công ra về (Xác nhận)", 14, DARK, false);
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
            Toast.makeText(this, "Hãy khóa màn hình hoặc thoát app - popup thử nghiệm sẽ hiện sau 10 giây",
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
