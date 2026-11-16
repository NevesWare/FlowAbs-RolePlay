package com.nevesware.flowlabsrp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Design system do FlowLabs: cores, fontes e componentes, tudo montado em código
 * (sem layouts XML) para ficar fácil de editar e compilar no AIDE.
 */
public final class Ui {

    private Ui() {}

    // ---------------- Cores ----------------
    public static final int BG = 0xFF07080F;
    public static final int GLASS = 0xE00C0D18;       // painel de vidro escuro
    public static final int GLASS_SOFT = 0x99101222;  // botões sobre a arte
    public static final int FIELD = 0x14FFFFFF;       // fundo de campo
    public static final int LINE = 0x1FFFFFFF;        // bordas sutis
    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFFA9ABC4;
    public static final int FAINT = 0xFF6C6F8A;
    public static final int VIOLET = 0xFF7C4DFF;
    public static final int MAGENTA = 0xFFE0457B;
    public static final int AMBER = 0xFFFFB547;
    public static final int SUCCESS = 0xFF2BD576;
    public static final int WARNING = 0xFFFFB547;
    public static final int DANGER = 0xFFFF5370;
    public static final int DISCORD = 0xFF5865F2;

    public static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    public static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    // ---------------- Fontes ----------------
    public static final int REGULAR = 0, MEDIUM = 1, SEMIBOLD = 2, BOLD = 3, DISPLAY = 4;
    private static final String[] FONT_FILES = {
            "fonts/Poppins-Regular.ttf", "fonts/Poppins-Medium.ttf", "fonts/Poppins-SemiBold.ttf",
            "fonts/Poppins-Bold.ttf", "fonts/Anton-Regular.ttf"};
    private static final Typeface[] FONTS = new Typeface[FONT_FILES.length];

    public static Typeface font(Context c, int weight) {
        if (FONTS[weight] == null) {
            try {
                FONTS[weight] = Typeface.createFromAsset(c.getAssets(), FONT_FILES[weight]);
            } catch (Exception e) {
                FONTS[weight] = weight >= SEMIBOLD ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
            }
        }
        return FONTS[weight];
    }

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    public static int screenHeightDp(Context c) {
        return Math.round(c.getResources().getDisplayMetrics().heightPixels
                / c.getResources().getDisplayMetrics().density);
    }

    // ---------------- Fundos ----------------
    public static GradientDrawable rounded(Context c, int color, float radiusDp, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        if (strokeColor != 0) g.setStroke(Math.max(1, dp(c, 1)), strokeColor);
        return g;
    }

    public static GradientDrawable gradient(Context c, int[] colors, float radiusDp,
                                            GradientDrawable.Orientation o) {
        GradientDrawable g = new GradientDrawable(o, colors);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static GradientDrawable circle(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    public static Drawable pressable(Drawable normal, Drawable pressed) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[] {android.R.attr.state_pressed}, pressed);
        s.addState(new int[] {}, normal);
        return s;
    }

    public static int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | (a << 24);
    }

    /** Painel de vidro padrão. */
    public static GradientDrawable glass(Context c, float radiusDp) {
        return rounded(c, GLASS, radiusDp, LINE);
    }

    // ---------------- Layout ----------------
    public static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, WRAP, w);
    }

    public static FrameLayout.LayoutParams frame(int w, int h, int gravity) {
        return new FrameLayout.LayoutParams(w, h, gravity);
    }

    public static View space(Context c, int dpH) {
        View v = new View(c);
        v.setLayoutParams(lp(1, dp(c, dpH)));
        return v;
    }

    public static View hspace(Context c, int dpW) {
        View v = new View(c);
        v.setLayoutParams(lp(dp(c, dpW), 1));
        return v;
    }

    public static View flex(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1));
        return v;
    }

    public static View line(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(lp(MATCH, Math.max(1, dp(c, 1))));
        return v;
    }

    // ---------------- Texto ----------------
    public static TextView text(Context c, CharSequence s, float sp, int color, int weight) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(font(c, weight));
        t.setIncludeFontPadding(false);
        return t;
    }

    public static TextView oneLine(TextView t) {
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    public static TextView caps(Context c, CharSequence s, int color) {
        TextView t = text(c, s, 10, color, SEMIBOLD);
        t.setLetterSpacing(0.14f);
        t.setAllCaps(true);
        return t;
    }

    public static TextView chip(Context c, String s, int color) {
        TextView t = text(c, s, 9.5f, color, SEMIBOLD);
        t.setLetterSpacing(0.1f);
        t.setAllCaps(true);
        t.setBackground(rounded(c, alpha(color, 0x24), 20, 0));
        t.setPadding(dp(c, 9), dp(c, 4), dp(c, 9), dp(c, 4));
        return t;
    }

    // ---------------- Ícones ----------------
    public static ImageView icon(Context c, int res, float sizeDp, int tint) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(res);
        iv.setColorFilter(tint);
        iv.setLayoutParams(lp(dp(c, sizeDp), dp(c, sizeDp)));
        return iv;
    }

    /** Botão quadrado arredondado só com ícone (vidro). */
    public static FrameLayout iconButton(Context c, int res, int bg, int tint, float sizeDp,
                                         View.OnClickListener l) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(pressable(rounded(c, bg, 12, LINE), rounded(c, alpha(0xFFFFFFFF, 0x30), 12, LINE)));
        ImageView iv = icon(c, res, sizeDp * 0.46f, tint);
        f.addView(iv, frame(dp(c, sizeDp * 0.46f), dp(c, sizeDp * 0.46f), Gravity.CENTER));
        f.setLayoutParams(lp(dp(c, sizeDp), dp(c, sizeDp)));
        f.setClickable(true);
        f.setOnClickListener(l);
        return f;
    }

    // ---------------- Botões ----------------
    private static LinearLayout baseButton(Context c, int iconRes, String label, int fg, float sp) {
        LinearLayout b = hbox(c);
        b.setGravity(Gravity.CENTER);
        if (iconRes != 0) {
            b.addView(icon(c, iconRes, sp + 4, fg));
            b.addView(hspace(c, 8));
        }
        TextView t = text(c, label, sp, fg, SEMIBOLD);
        t.setLetterSpacing(0.03f);
        b.addView(t);
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    /** Botão principal com o degradê "pôr do sol" do FlowLabs. */
    public static LinearLayout primary(Context c, int iconRes, String label, View.OnClickListener l) {
        LinearLayout b = baseButton(c, iconRes, label, Color.WHITE, 14);
        b.setBackground(pressable(
                gradient(c, new int[] {AMBER, MAGENTA}, 12, GradientDrawable.Orientation.LEFT_RIGHT),
                gradient(c, new int[] {0xFFD9923A, 0xFFB93765}, 12, GradientDrawable.Orientation.LEFT_RIGHT)));
        b.setPadding(dp(c, 18), 0, dp(c, 18), 0);
        b.setMinimumHeight(dp(c, 44));
        b.setOnClickListener(l);
        return b;
    }

    public static LinearLayout secondary(Context c, int iconRes, String label, View.OnClickListener l) {
        LinearLayout b = baseButton(c, iconRes, label, TEXT, 13);
        b.setBackground(pressable(rounded(c, FIELD, 12, LINE), rounded(c, alpha(0xFFFFFFFF, 0x2A), 12, LINE)));
        b.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        b.setMinimumHeight(dp(c, 40));
        b.setOnClickListener(l);
        return b;
    }

    public static LinearLayout solid(Context c, int iconRes, String label, int bg, int fg, View.OnClickListener l) {
        LinearLayout b = baseButton(c, iconRes, label, fg, 13);
        b.setBackground(pressable(rounded(c, bg, 12, 0), rounded(c, alpha(bg, 0xB0), 12, 0)));
        b.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        b.setMinimumHeight(dp(c, 40));
        b.setOnClickListener(l);
        return b;
    }

    public static TextView link(Context c, String label, int color, View.OnClickListener l) {
        TextView t = text(c, label, 12, color, MEDIUM);
        t.setPadding(0, dp(c, 6), 0, dp(c, 6));
        t.setClickable(true);
        t.setOnClickListener(l);
        return t;
    }

    // ---------------- Campos ----------------
    public static class Field {
        public final LinearLayout view;
        public final EditText edit;

        Field(LinearLayout view, EditText edit) {
            this.view = view;
            this.edit = edit;
        }

        public String value() {
            return edit.getText().toString().trim();
        }
    }

    public static Field field(Context c, int iconRes, String hint, int inputType) {
        LinearLayout box = hbox(c);
        box.setBackground(rounded(c, FIELD, 12, LINE));
        box.setPadding(dp(c, 12), 0, dp(c, 8), 0);
        box.setMinimumHeight(dp(c, 42));
        box.addView(icon(c, iconRes, 16, FAINT));
        box.addView(hspace(c, 10));
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(FAINT);
        e.setTextColor(TEXT);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        e.setTypeface(font(c, REGULAR));
        e.setSingleLine(true);
        e.setInputType(inputType);
        e.setBackground(null);
        e.setPadding(0, dp(c, 10), 0, dp(c, 10));
        box.addView(e, new LinearLayout.LayoutParams(0, WRAP, 1));
        return new Field(box, e);
    }

    public static Field password(Context c, String hint) {
        return field(c, com.nevesware.flowlabsrp.R.drawable.ic_lock, hint,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }

    // ---------------- Controles ----------------
    public interface Picker {
        void pick(int index);
    }

    public static LinearLayout segmented(final Context c, String[] options, int selected, final Picker picker) {
        LinearLayout seg = hbox(c);
        seg.setBackground(rounded(c, FIELD, 10, LINE));
        seg.setPadding(dp(c, 3), dp(c, 3), dp(c, 3), dp(c, 3));
        final TextView[] views = new TextView[options.length];
        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            TextView t = text(c, options[i], 12, MUTED, MEDIUM);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(c, 7), 0, dp(c, 7));
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    for (int j = 0; j < views.length; j++) styleSeg(c, views[j], j == idx);
                    picker.pick(idx);
                }
            });
            views[i] = t;
            styleSeg(c, t, i == selected);
            seg.addView(t, weight(1));
        }
        return seg;
    }

    private static void styleSeg(Context c, TextView t, boolean sel) {
        t.setTextColor(sel ? TEXT : MUTED);
        t.setTypeface(font(c, sel ? SEMIBOLD : MEDIUM));
        t.setBackground(sel ? rounded(c, VIOLET, 8, 0) : null);
    }

    public static Switch toggle(Context c, String label, boolean value,
                                android.widget.CompoundButton.OnCheckedChangeListener l) {
        Switch s = new Switch(c);
        s.setText(label);
        s.setTextColor(TEXT);
        s.setTextSize(13);
        s.setTypeface(font(c, REGULAR));
        s.setChecked(value);
        s.setOnCheckedChangeListener(l);
        s.setPadding(0, dp(c, 6), 0, dp(c, 6));
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int[][] st = {new int[] {android.R.attr.state_checked}, new int[] {}};
            s.setThumbTintList(new ColorStateList(st, new int[] {AMBER, 0xFFB8BACF}));
            s.setTrackTintList(new ColorStateList(st, new int[] {alpha(AMBER, 0x70), alpha(0xFFFFFFFF, 0x30)}));
        }
        return s;
    }

    // ---------------- Marca ----------------

    /** Logo "FLOWLABS" + selo "ROLEPLAY" (mesma identidade do ícone e da tela de carregamento). */
    public static LinearLayout logo(Context c, float scale, int gravity) {
        LinearLayout l = vbox(c);
        l.setGravity(gravity);
        TextView top = text(c, "FLOWLABS", 44 * scale, TEXT, DISPLAY);
        top.setLetterSpacing(0.02f);
        top.setShadowLayer(dp(c, 10), 0, dp(c, 3), 0xC0000000);
        TextView bottom = text(c, "R O L E P L A Y", 12 * scale, 0xFF120C1C, DISPLAY);
        bottom.setBackground(rounded(c, AMBER, 5 * scale, 0));
        bottom.setPadding(dp(c, 10 * scale), dp(c, 3 * scale), dp(c, 10 * scale), dp(c, 3 * scale));
        l.addView(top);
        l.addView(space(c, Math.round(4 * scale)));
        l.addView(bottom);
        return l;
    }

    /** Avatar redondo com a inicial do nome. */
    public static TextView avatar(Context c, String name, float sizeDp) {
        String initial = name != null && name.length() > 0 ? name.substring(0, 1).toUpperCase() : "?";
        TextView a = text(c, initial, sizeDp * 0.42f, Color.WHITE, BOLD);
        a.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {VIOLET, MAGENTA});
        g.setShape(GradientDrawable.OVAL);
        a.setBackground(g);
        a.setLayoutParams(lp(dp(c, sizeDp), dp(c, sizeDp)));
        return a;
    }

    /** Escurece a arte de fundo para o texto ficar legível. */
    public static View artScrim(Context c, boolean strongLeft) {
        View v = new View(c);
        GradientDrawable h = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                strongLeft ? new int[] {0xF207080F, 0xB307080F, 0x4007080F}
                           : new int[] {0x9907080F, 0x3307080F, 0x9907080F});
        v.setBackground(h);
        return v;
    }

    public static View bottomScrim(Context c) {
        View v = new View(c);
        v.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0x0007080F, 0x0007080F, 0xE607080F}));
        return v;
    }

    // ---------------- Utilidades ----------------
    public static void openUrl(Activity a, String url) {
        if (url == null || url.length() == 0) return;
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {}
    }

    public static void immersive(Activity a) {
        a.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
              | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_FULLSCREEN
              | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
