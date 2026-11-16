package com.nevesware.flowlabsrp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.nevesware.flowlabsrp.auth.AccountStore;
import com.nevesware.flowlabsrp.auth.OAuth;
import com.nevesware.flowlabsrp.auth.Session;
import com.nevesware.flowlabsrp.ui.Sheet;
import com.nevesware.flowlabsrp.ui.Ui;

/** Entrar / Criar conta / Google / Discord / Convidado. Tudo cabe numa tela só. */
public class AuthActivity extends Activity {

    private AccountStore accounts;
    private FrameLayout root;
    private FrameLayout formHost;
    private TextView tabLogin, tabRegister;
    private View loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accounts = new AccountStore(this);
        setContentView(build());
        Ui.immersive(this);
        showLogin();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Ui.immersive(this);
    }

    @Override
    public void onBackPressed() {
        if (!Sheet.closeTop(root)) super.onBackPressed();
    }

    // =========================== Layout ===========================

    private View build() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);

        ImageView art = new ImageView(this);
        art.setImageResource(R.drawable.bg_city);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(art, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        root.addView(Ui.artScrim(this, true), new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        LinearLayout content = Ui.hbox(this);
        content.setPadding(Ui.dp(this, 36), Ui.dp(this, 10), Ui.dp(this, 28), Ui.dp(this, 10));

        // ----- Esquerda: marca -----
        LinearLayout brand = Ui.vbox(this);
        brand.addView(Ui.logo(this, 1.15f, Gravity.START));
        brand.addView(Ui.space(this, 18));
        brand.addView(Ui.text(this, "Sua vida. Sua história. Sua cidade.", 17, Ui.TEXT, Ui.SEMIBOLD));
        brand.addView(Ui.space(this, 6));
        TextView sub = Ui.text(this, "Roleplay brasileiro feito para o celular.\nCrie sua conta e entre para a cidade.",
                12.5f, Ui.MUTED, Ui.REGULAR);
        sub.setLineSpacing(0, 1.25f);
        brand.addView(sub);
        brand.addView(Ui.space(this, 20));

        LinearLayout social = Ui.hbox(this);
        social.addView(Ui.iconButton(this, R.drawable.ic_chat, Ui.DISCORD, Color.WHITE, 40, open(Config.DISCORD_URL)));
        social.addView(Ui.hspace(this, 8));
        social.addView(Ui.iconButton(this, R.drawable.ic_camera, Ui.GLASS_SOFT, Color.WHITE, 40, open(Config.INSTAGRAM_URL)));
        social.addView(Ui.hspace(this, 8));
        social.addView(Ui.iconButton(this, R.drawable.ic_music, Ui.GLASS_SOFT, Color.WHITE, 40, open(Config.TIKTOK_URL)));
        social.addView(Ui.hspace(this, 8));
        social.addView(Ui.iconButton(this, R.drawable.ic_globe, Ui.GLASS_SOFT, Color.WHITE, 40, open(Config.SITE_URL)));
        brand.addView(social);
        content.addView(brand, new LinearLayout.LayoutParams(0, Ui.WRAP, 1));
        content.addView(Ui.hspace(this, 24));

        // ----- Direita: cartão do formulário -----
        LinearLayout card = Ui.vbox(this);
        card.setBackground(Ui.glass(this, 20));
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 14));

        LinearLayout tabs = Ui.hbox(this);
        tabs.setBackground(Ui.rounded(this, Ui.FIELD, 12, Ui.LINE));
        tabs.setPadding(Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3));
        tabLogin = tab("Entrar", new View.OnClickListener() {
            @Override public void onClick(View v) { showLogin(); }
        });
        tabRegister = tab("Criar conta", new View.OnClickListener() {
            @Override public void onClick(View v) { showRegister(); }
        });
        tabs.addView(tabLogin, Ui.weight(1));
        tabs.addView(tabRegister, Ui.weight(1));
        card.addView(tabs, Ui.lp(Ui.MATCH, Ui.WRAP));
        card.addView(Ui.space(this, 12));
        formHost = new FrameLayout(this);
        card.addView(formHost, Ui.lp(Ui.MATCH, Ui.WRAP));

        ScrollView safety = new ScrollView(this);
        safety.setVerticalScrollBarEnabled(false);
        safety.setOverScrollMode(View.OVER_SCROLL_NEVER);
        safety.setFillViewport(true);
        FrameLayout center = new FrameLayout(this);
        center.addView(card, Ui.frame(Ui.MATCH, Ui.WRAP, Gravity.CENTER_VERTICAL));
        safety.addView(center, new ScrollView.LayoutParams(Ui.MATCH, Ui.MATCH));
        content.addView(safety, new LinearLayout.LayoutParams(Ui.dp(this, 380), Ui.MATCH));

        root.addView(content, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        TextView copy = Ui.text(this, Config.COPYRIGHT, 10, Ui.FAINT, Ui.REGULAR);
        FrameLayout.LayoutParams cp = Ui.frame(Ui.WRAP, Ui.WRAP, Gravity.BOTTOM | Gravity.START);
        cp.leftMargin = Ui.dp(this, 36);
        cp.bottomMargin = Ui.dp(this, 14);
        root.addView(copy, cp);
        return root;
    }

    private TextView tab(String label, View.OnClickListener l) {
        TextView t = Ui.text(this, label, 13, Ui.MUTED, Ui.MEDIUM);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(this, 7), 0, Ui.dp(this, 7));
        t.setOnClickListener(l);
        return t;
    }

    private void selectTab(boolean login) {
        style(tabLogin, login);
        style(tabRegister, !login);
    }

    private void style(TextView t, boolean sel) {
        t.setTextColor(sel ? Ui.TEXT : Ui.MUTED);
        t.setTypeface(Ui.font(this, sel ? Ui.SEMIBOLD : Ui.MEDIUM));
        t.setBackground(sel ? Ui.rounded(this, Ui.VIOLET, 10, 0) : null);
    }

    // =========================== Entrar ===========================

    private void showLogin() {
        selectTab(true);
        formHost.removeAllViews();
        LinearLayout f = Ui.vbox(this);

        final Ui.Field user = Ui.field(this, R.drawable.ic_person, "E-mail ou nome no RP",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        final Ui.Field pass = Ui.password(this, "Senha");
        f.addView(user.view, Ui.lp(Ui.MATCH, Ui.WRAP));
        f.addView(Ui.space(this, 8));
        f.addView(pass.view, Ui.lp(Ui.MATCH, Ui.WRAP));
        f.addView(Ui.space(this, 12));
        f.addView(Ui.primary(this, 0, "Entrar", new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    finishLogin(accounts.login(user.value(), pass.edit.getText().toString()));
                } catch (IllegalArgumentException e) {
                    error(e.getMessage());
                }
            }
        }), Ui.lp(Ui.MATCH, Ui.WRAP));

        addSocial(f);

        LinearLayout links = Ui.hbox(this);
        links.addView(Ui.link(this, "Esqueci minha senha", Ui.MUTED, new View.OnClickListener() {
            @Override public void onClick(View v) { forgotPassword(); }
        }));
        links.addView(Ui.flex(this));
        if (Config.ALLOW_GUEST) {
            links.addView(Ui.link(this, "Jogar como convidado", Ui.AMBER, new View.OnClickListener() {
                @Override public void onClick(View v) { guest(); }
            }));
        }
        f.addView(Ui.space(this, 4));
        f.addView(links, Ui.lp(Ui.MATCH, Ui.WRAP));
        formHost.addView(f);
    }

    // =========================== Criar conta ===========================

    private void showRegister() {
        selectTab(false);
        formHost.removeAllViews();
        LinearLayout f = Ui.vbox(this);

        final Ui.Field nick = Ui.field(this, R.drawable.ic_person, "Nome_Sobrenome",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        final Ui.Field email = Ui.field(this, R.drawable.ic_mail, "E-mail",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        final Ui.Field pass = Ui.password(this, "Senha");
        final Ui.Field pass2 = Ui.password(this, "Confirmar");

        LinearLayout r1 = Ui.hbox(this);
        r1.addView(nick.view, Ui.weight(1));
        r1.addView(Ui.hspace(this, 8));
        r1.addView(email.view, Ui.weight(1));
        f.addView(r1, Ui.lp(Ui.MATCH, Ui.WRAP));
        f.addView(Ui.space(this, 8));
        LinearLayout r2 = Ui.hbox(this);
        r2.addView(pass.view, Ui.weight(1));
        r2.addView(Ui.hspace(this, 8));
        r2.addView(pass2.view, Ui.weight(1));
        f.addView(r2, Ui.lp(Ui.MATCH, Ui.WRAP));
        f.addView(Ui.space(this, 4));

        LinearLayout termsRow = Ui.hbox(this);
        final CheckBox terms = new CheckBox(this);
        terms.setText("Aceito os Termos e a Privacidade");
        terms.setTextColor(Ui.MUTED);
        terms.setTextSize(12);
        terms.setTypeface(Ui.font(this, Ui.REGULAR));
        terms.setMinHeight(0);
        terms.setMinimumHeight(0);
        terms.setScaleX(0.92f);
        terms.setScaleY(0.92f);
        terms.setPivotX(0);
        termsRow.addView(terms);
        termsRow.addView(Ui.flex(this));
        termsRow.addView(Ui.link(this, "Ler termos", Ui.AMBER, open(Config.TERMS_URL)));
        f.addView(termsRow, Ui.lp(Ui.MATCH, Ui.WRAP));
        f.addView(Ui.space(this, 6));

        f.addView(Ui.primary(this, 0, "Criar conta", new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!pass.edit.getText().toString().equals(pass2.edit.getText().toString())) {
                    error("As senhas não conferem.");
                    return;
                }
                if (!terms.isChecked()) {
                    error("Aceite os Termos para criar a conta.");
                    return;
                }
                try {
                    Session s = accounts.register(nick.value(), email.value(), pass.edit.getText().toString());
                    finishLogin(s);
                } catch (IllegalArgumentException e) {
                    error(e.getMessage());
                }
            }
        }), Ui.lp(Ui.MATCH, Ui.WRAP));

        addSocial(f);
        formHost.addView(f);
    }

    // =========================== Google / Discord / Convidado ===========================

    private void addSocial(LinearLayout f) {
        f.addView(Ui.space(this, 10));
        LinearLayout or = Ui.hbox(this);
        or.addView(divider(), new LinearLayout.LayoutParams(0, Math.max(1, Ui.dp(this, 1)), 1));
        TextView ou = Ui.text(this, "ou continue com", 11, Ui.FAINT, Ui.REGULAR);
        ou.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        or.addView(ou);
        or.addView(divider(), new LinearLayout.LayoutParams(0, Math.max(1, Ui.dp(this, 1)), 1));
        f.addView(or, Ui.lp(Ui.MATCH, Ui.WRAP));
        f.addView(Ui.space(this, 8));

        LinearLayout row = Ui.hbox(this);
        LinearLayout google = Ui.solid(this, 0, "Google", Color.WHITE, 0xFF1F1F1F, new View.OnClickListener() {
            @Override public void onClick(View v) { social(Session.GOOGLE); }
        });
        TextView g = Ui.text(this, "G", 15, 0xFF4285F4, Ui.BOLD);
        g.setPadding(0, 0, Ui.dp(this, 8), 0);
        google.addView(g, 0);
        row.addView(google, Ui.weight(1));
        row.addView(Ui.hspace(this, 8));
        row.addView(Ui.solid(this, R.drawable.ic_chat, "Discord", Ui.DISCORD, Color.WHITE, new View.OnClickListener() {
            @Override public void onClick(View v) { social(Session.DISCORD); }
        }), Ui.weight(1));
        f.addView(row, Ui.lp(Ui.MATCH, Ui.WRAP));
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(Ui.LINE);
        return v;
    }

    private void guest() {
        Sheet.confirm(this, root, "Jogar como convidado?",
                "Convidados não salvam progresso, dinheiro nem itens. Crie uma conta para guardar sua história na cidade.",
                "Continuar", false, new Runnable() {
                    @Override public void run() { finishLogin(AccountStore.guest()); }
                });
    }

    private void social(String provider) {
        String name = Session.GOOGLE.equals(provider) ? "Google" : "Discord";
        if (!Config.supabaseReady()) {
            Sheet.info(this, root, "Login com " + name,
                    "O login com " + name + " já está pronto no launcher e será ativado assim que o "
                  + "servidor de contas for conectado.\n\nPor enquanto, crie sua conta FlowLabs na aba \"Criar conta\".");
            return;
        }
        try {
            OAuth.start(this, provider);
        } catch (Exception e) {
            error("Não foi possível abrir o login com " + name + ".");
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        Uri data = intent.getData();
        if (!OAuth.isCallback(data)) return;
        final OAuth.Tokens tokens;
        try {
            tokens = OAuth.parseCallback(this, data);
        } catch (IllegalArgumentException e) {
            error("Login cancelado: " + e.getMessage());
            return;
        }
        showLoading(true);
        new Thread(new Runnable() {
            @Override public void run() {
                Session s = null;
                String err = null;
                try {
                    s = OAuth.fetchUser(tokens);
                } catch (Exception e) {
                    err = e.getMessage();
                }
                final Session fs = s;
                final String ferr = err;
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        showLoading(false);
                        if (fs != null) finishLogin(fs);
                        else error(ferr == null ? "Falha no login." : ferr);
                    }
                });
            }
        }).start();
    }

    private void showLoading(boolean on) {
        if (on && loading == null) {
            FrameLayout f = new FrameLayout(this);
            f.setBackgroundColor(0xCC000000);
            f.setClickable(true);
            LinearLayout box = Ui.vbox(this);
            box.setGravity(Gravity.CENTER);
            box.addView(new android.widget.ProgressBar(this));
            box.addView(Ui.space(this, 10));
            box.addView(Ui.text(this, "Entrando...", 13, Ui.TEXT, Ui.MEDIUM));
            f.addView(box, Ui.frame(Ui.WRAP, Ui.WRAP, Gravity.CENTER));
            loading = f;
            root.addView(f, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        } else if (!on && loading != null) {
            root.removeView(loading);
            loading = null;
        }
    }

    private void forgotPassword() {
        Sheet.info(this, root, "Recuperar senha", Config.supabaseReady()
                ? "Use o login com Google ou Discord, ou chame o suporte no nosso Discord."
                : "Por enquanto as contas ficam salvas só neste aparelho. A recuperação por e-mail será "
                  + "ativada junto com o servidor de contas. Precisa de ajuda? Chame o suporte no Discord.");
    }

    // =========================== Util ===========================

    private View.OnClickListener open(final String url) {
        return new View.OnClickListener() {
            @Override public void onClick(View v) { Ui.openUrl(AuthActivity.this, url); }
        };
    }

    private void error(String msg) {
        Sheet.snack(this, root, msg, Ui.DANGER);
    }

    private void finishLogin(Session s) {
        s.save(this);
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
