package com.nevesware.flowlabsrp.ui;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.nevesware.flowlabsrp.R;

/**
 * Painel que abre por cima da tela (estilo app moderno), em vez de trocar de página.
 * Cada painel foi desenhado para caber inteiro na tela do celular deitado.
 */
public class Sheet {

    private final Activity activity;
    private final FrameLayout host;
    private final FrameLayout overlay;
    private final LinearLayout card;
    public final LinearLayout body;
    public final LinearLayout headerActions;
    private Runnable onDismiss;

    public Sheet(Activity a, FrameLayout host, String title, int widthDp) {
        this.activity = a;
        this.host = host;

        overlay = new FrameLayout(a);
        overlay.setBackgroundColor(0xB3000000);
        overlay.setClickable(true);
        overlay.setTag(this);
        overlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dismiss(); }
        });
        int pad = Ui.dp(a, 14);
        overlay.setPadding(pad, pad, pad, pad);

        card = Ui.vbox(a);
        card.setBackground(Ui.glass(a, 20));
        card.setClickable(true); // não fecha ao tocar dentro
        card.setPadding(Ui.dp(a, 20), Ui.dp(a, 14), Ui.dp(a, 20), Ui.dp(a, 18));

        LinearLayout header = Ui.hbox(a);
        TextView t = Ui.text(a, title, 17, Ui.TEXT, Ui.SEMIBOLD);
        header.addView(t);
        header.addView(Ui.flex(a));
        headerActions = Ui.hbox(a);
        header.addView(headerActions);
        header.addView(Ui.hspace(a, 8));
        header.addView(Ui.iconButton(a, R.drawable.ic_close, Ui.FIELD, Ui.MUTED, 34, new View.OnClickListener() {
            @Override public void onClick(View v) { dismiss(); }
        }));
        card.addView(header, Ui.lp(Ui.MATCH, Ui.WRAP));
        card.addView(Ui.space(a, 12));

        // Rolagem só como segurança em telas muito pequenas (sem barra visível).
        ScrollView sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        body = Ui.vbox(a);
        sv.addView(body, new ScrollView.LayoutParams(Ui.MATCH, Ui.WRAP));
        card.addView(sv, Ui.lp(Ui.MATCH, Ui.WRAP));

        int maxW = a.getResources().getDisplayMetrics().widthPixels - 2 * pad;
        overlay.addView(card, Ui.frame(Math.min(Ui.dp(a, widthDp), maxW), Ui.WRAP, Gravity.CENTER));
    }

    public Sheet onDismiss(Runnable r) {
        onDismiss = r;
        return this;
    }

    public Sheet show() {
        host.addView(overlay, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        overlay.setAlpha(0f);
        overlay.animate().alpha(1f).setDuration(160).start();
        card.setScaleX(0.96f);
        card.setScaleY(0.96f);
        card.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
        return this;
    }

    public void dismiss() {
        if (overlay.getParent() == null) return;
        host.removeView(overlay);
        if (onDismiss != null) onDismiss.run();
    }

    public boolean isShowing() {
        return overlay.getParent() != null;
    }

    /** Fecha o painel do topo (botão voltar). Retorna true se fechou algum. */
    public static boolean closeTop(FrameLayout host) {
        for (int i = host.getChildCount() - 1; i >= 0; i--) {
            View v = host.getChildAt(i);
            if (v.getTag() instanceof Sheet) {
                ((Sheet) v.getTag()).dismiss();
                return true;
            }
        }
        return false;
    }

    // ---------------- Atalhos ----------------

    public static Sheet info(Activity a, FrameLayout host, String title, String message) {
        Sheet s = new Sheet(a, host, title, 420);
        TextView m = Ui.text(a, message, 13, Ui.MUTED, Ui.REGULAR);
        m.setLineSpacing(0, 1.25f);
        s.body.addView(m);
        s.body.addView(Ui.space(a, 16));
        final Sheet fs = s;
        s.body.addView(Ui.primary(a, 0, "Entendi", new View.OnClickListener() {
            @Override public void onClick(View v) { fs.dismiss(); }
        }), Ui.lp(Ui.MATCH, Ui.WRAP));
        return s.show();
    }

    public static Sheet confirm(Activity a, FrameLayout host, String title, String message,
                                String okLabel, boolean danger, final Runnable onOk) {
        final Sheet s = new Sheet(a, host, title, 420);
        TextView m = Ui.text(a, message, 13, Ui.MUTED, Ui.REGULAR);
        m.setLineSpacing(0, 1.25f);
        s.body.addView(m);
        s.body.addView(Ui.space(a, 16));
        LinearLayout row = Ui.hbox(a);
        row.addView(Ui.secondary(a, 0, "Cancelar", new View.OnClickListener() {
            @Override public void onClick(View v) { s.dismiss(); }
        }), Ui.weight(1));
        row.addView(Ui.hspace(a, 10));
        View.OnClickListener ok = new View.OnClickListener() {
            @Override public void onClick(View v) {
                s.dismiss();
                onOk.run();
            }
        };
        row.addView(danger ? Ui.solid(a, 0, okLabel, Ui.DANGER, Color.WHITE, ok) : Ui.primary(a, 0, okLabel, ok),
                Ui.weight(1));
        s.body.addView(row, Ui.lp(Ui.MATCH, Ui.WRAP));
        return s.show();
    }

    /** Aviso rápido no topo da tela (substitui o Toast padrão). */
    public static void snack(Activity a, FrameLayout host, String message, int color) {
        final TextView t = Ui.text(a, message, 12.5f, Ui.TEXT, Ui.MEDIUM);
        t.setBackground(Ui.rounded(a, 0xF0121424, 24, Ui.alpha(color, 0x90)));
        t.setPadding(Ui.dp(a, 16), Ui.dp(a, 10), Ui.dp(a, 16), Ui.dp(a, 10));
        t.setMaxWidth(Ui.dp(a, 520));
        FrameLayout.LayoutParams p = Ui.frame(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        p.topMargin = Ui.dp(a, 14);
        final FrameLayout h = host;
        h.addView(t, p);
        t.setAlpha(0f);
        t.setTranslationY(-Ui.dp(a, 12));
        t.animate().alpha(1f).translationY(0).setDuration(180).start();
        t.postDelayed(new Runnable() {
            @Override public void run() {
                t.animate().alpha(0f).setDuration(200).withEndAction(new Runnable() {
                    @Override public void run() { h.removeView(t); }
                }).start();
            }
        }, 2800);
    }
}
