package com.nevesware.flowlabsrp;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nevesware.flowlabsrp.auth.Session;
import com.nevesware.flowlabsrp.ui.Ui;

/** Tela de carregamento: arte "FlowLabs Studio apresenta" + barra de progresso. */
public class SplashActivity extends Activity {

    private static final String[] STEPS = {
            "Iniciando o launcher...",
            "Verificando atualizações...",
            "Carregando servidores...",
            "Tudo pronto!",
    };

    private ValueAnimator anim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);

        ImageView art = new ImageView(this);
        art.setImageResource(R.drawable.loading);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(art, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        root.addView(Ui.bottomScrim(this), new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        // Barra inferior: status, versão e progresso
        LinearLayout bottom = Ui.vbox(this);
        int side = Ui.dp(this, 32);
        bottom.setPadding(side, 0, side, Ui.dp(this, 22));

        LinearLayout row = Ui.hbox(this);
        final TextView status = Ui.text(this, STEPS[0], 12, Ui.MUTED, Ui.MEDIUM);
        row.addView(status);
        row.addView(Ui.flex(this));
        row.addView(Ui.text(this, "v" + Config.VERSION_NAME, 11, Ui.FAINT, Ui.MEDIUM));
        bottom.addView(row, Ui.lp(Ui.MATCH, Ui.WRAP));
        bottom.addView(Ui.space(this, 10));

        FrameLayout track = new FrameLayout(this);
        track.setBackground(Ui.rounded(this, 0x26FFFFFF, 3, 0));
        final View fill = new View(this);
        fill.setBackground(Ui.gradient(this, new int[] {Ui.AMBER, Ui.MAGENTA}, 3,
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT));
        track.addView(fill, new FrameLayout.LayoutParams(0, Ui.MATCH));
        bottom.addView(track, Ui.lp(Ui.MATCH, Ui.dp(this, 4)));
        bottom.addView(Ui.space(this, 10));
        TextView copy = Ui.text(this, Config.COPYRIGHT, 10, Ui.FAINT, Ui.REGULAR);
        bottom.addView(copy);

        root.addView(bottom, Ui.frame(Ui.MATCH, Ui.WRAP, Gravity.BOTTOM));
        setContentView(root);
        Ui.immersive(this);

        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(1900);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                float p = (Float) a.getAnimatedValue();
                View parent = (View) fill.getParent();
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fill.getLayoutParams();
                lp.width = (int) (parent.getWidth() * p);
                fill.setLayoutParams(lp);
                status.setText(STEPS[Math.min(STEPS.length - 1, (int) (p * STEPS.length))]);
            }
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                if (isFinishing()) return;
                Session s = Session.load(SplashActivity.this);
                startActivity(new Intent(SplashActivity.this, s == null ? AuthActivity.class : MainActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });
        anim.setStartDelay(300);
        anim.start();
    }

    @Override
    protected void onDestroy() {
        if (anim != null) anim.cancel();
        super.onDestroy();
    }
}
