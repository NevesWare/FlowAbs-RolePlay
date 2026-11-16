package com.nevesware.flowlabsrp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nevesware.flowlabsrp.auth.AccountStore;
import com.nevesware.flowlabsrp.auth.Session;
import com.nevesware.flowlabsrp.game.GameFiles;
import com.nevesware.flowlabsrp.game.GameImporter;
import com.nevesware.flowlabsrp.game.GameLauncher;
import com.nevesware.flowlabsrp.net.NewsItem;
import com.nevesware.flowlabsrp.net.RemoteContent;
import com.nevesware.flowlabsrp.net.ServerInfo;
import com.nevesware.flowlabsrp.net.ServerPinger;
import com.nevesware.flowlabsrp.ui.Sheet;
import com.nevesware.flowlabsrp.ui.Ui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Tela principal (estilo launcher de jogo): arte em tela cheia, logo no centro,
 * atalhos nos cantos e o botão JOGAR. Servidores, loja, novidades e ajustes abrem
 * em painéis por cima, sem rolagem.
 */
public class MainActivity extends Activity {

    private static final int REQ_PICK_GTA = 41;

    private static final int SET_GRAPHICS = 0, SET_GAME = 1, SET_FILES = 2, SET_ACCOUNT = 3, SET_ABOUT = 4;
    private static final String[] SET_LABELS = {"Gráficos", "Jogo", "Arquivos", "Conta", "Sobre"};
    private static final int[] SET_ICONS = {R.drawable.ic_settings, R.drawable.ic_bell, R.drawable.ic_folder,
            R.drawable.ic_person, R.drawable.ic_badge};

    private Session session;
    private Settings settings;
    private ServerInfo[] servers = new ServerInfo[0];
    private NewsItem[] news = new NewsItem[0];

    private FrameLayout root;

    // Home (atualizados sem reconstruir a tela)
    private View srvDot;
    private TextView srvName, srvStatus, filesLine;
    private ImageView filesIcon;

    // Painel de ajustes
    private Sheet settingsSheet;
    private FrameLayout settingsContent;
    private LinearLayout[] settingsTabs;
    private int settingsTab = SET_GRAPHICS;

    // Importação
    private GameImporter importer;
    private boolean importing;
    private long importDone, importTotal;
    private String importCurrent = "";
    private View importFill;
    private TextView importText;
    private volatile GameFiles.Status fileStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = Session.load(this);
        if (session == null) {
            goToAuth();
            return;
        }
        settings = new Settings(this);
        setContentView(buildHome());
        Ui.immersive(this);
        refreshFiles(true);
        loadRemote();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Ui.immersive(this);
        if (session != null && !session.isGuest() && !session.hasNickname()) {
            root.post(new Runnable() {
                @Override public void run() { askNickname(false); }
            });
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Ui.immersive(this);
    }

    @Override
    public void onBackPressed() {
        if (root != null && Sheet.closeTop(root)) return;
        super.onBackPressed();
    }

    // =====================================================================
    // HOME
    // =====================================================================

    private View buildHome() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);

        ImageView art = new ImageView(this);
        art.setImageResource(R.drawable.bg_city);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(art, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        View top = new View(this);
        top.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xB307080F, 0x2007080F, 0x2007080F, 0xF207080F}));
        root.addView(top, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        int edge = Ui.dp(this, 20);

        // ---------- Topo ----------
        LinearLayout topBar = Ui.hbox(this);
        topBar.setPadding(edge, Ui.dp(this, 16), edge, 0);

        LinearLayout profile = Ui.hbox(this);
        profile.setBackground(Ui.pressable(Ui.rounded(this, Ui.GLASS_SOFT, 24, Ui.LINE),
                Ui.rounded(this, 0x33FFFFFF, 24, Ui.LINE)));
        profile.setPadding(Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 16), Ui.dp(this, 5));
        profile.addView(Ui.avatar(this, displayName(), 32));
        profile.addView(Ui.hspace(this, 10));
        LinearLayout who = Ui.vbox(this);
        who.addView(Ui.text(this, displayName(), 13, Ui.TEXT, Ui.SEMIBOLD));
        who.addView(Ui.space(this, 2));
        who.addView(Ui.text(this, session.isGuest() ? "Convidado" : "[" + Config.SHORT_TAG + "] " + session.typeLabel(),
                10.5f, Ui.MUTED, Ui.REGULAR));
        profile.addView(who);
        profile.setClickable(true);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openSettings(SET_ACCOUNT); }
        });
        topBar.addView(profile);
        topBar.addView(Ui.flex(this));

        LinearLayout store = Ui.hbox(this);
        store.setBackground(Ui.pressable(Ui.rounded(this, Ui.GLASS_SOFT, 12, Ui.LINE),
                Ui.rounded(this, 0x33FFFFFF, 12, Ui.LINE)));
        store.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 14), 0);
        store.setMinimumHeight(Ui.dp(this, 40));
        FrameLayout coin = new FrameLayout(this);
        coin.setBackground(Ui.rounded(this, Ui.AMBER, 8, 0));
        coin.addView(Ui.icon(this, R.drawable.ic_coin, 16, 0xFF1A1206), Ui.frame(Ui.dp(this, 16), Ui.dp(this, 16), Gravity.CENTER));
        store.addView(coin, Ui.lp(Ui.dp(this, 28), Ui.dp(this, 28)));
        store.addView(Ui.hspace(this, 10));
        store.addView(Ui.text(this, "LOJA VIP", 11.5f, Ui.TEXT, Ui.SEMIBOLD));
        store.setClickable(true);
        store.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openStore(); }
        });
        topBar.addView(store);
        topBar.addView(Ui.hspace(this, 8));
        topBar.addView(Ui.iconButton(this, R.drawable.ic_bell, Ui.GLASS_SOFT, Color.WHITE, 40, new View.OnClickListener() {
            @Override public void onClick(View v) { openNews(); }
        }));
        topBar.addView(Ui.hspace(this, 8));
        topBar.addView(Ui.iconButton(this, R.drawable.ic_settings, Ui.GLASS_SOFT, Color.WHITE, 40, new View.OnClickListener() {
            @Override public void onClick(View v) { openSettings(SET_GRAPHICS); }
        }));
        root.addView(topBar, Ui.frame(Ui.MATCH, Ui.WRAP, Gravity.TOP));

        // ---------- Redes (lateral esquerda) ----------
        LinearLayout socials = Ui.vbox(this);
        socials.addView(Ui.iconButton(this, R.drawable.ic_chat, Ui.DISCORD, Color.WHITE, 38, open(Config.DISCORD_URL)));
        socials.addView(Ui.space(this, 8));
        socials.addView(Ui.iconButton(this, R.drawable.ic_camera, Ui.GLASS_SOFT, Color.WHITE, 38, open(Config.INSTAGRAM_URL)));
        socials.addView(Ui.space(this, 8));
        socials.addView(Ui.iconButton(this, R.drawable.ic_music, Ui.GLASS_SOFT, Color.WHITE, 38, open(Config.TIKTOK_URL)));
        socials.addView(Ui.space(this, 8));
        socials.addView(Ui.iconButton(this, R.drawable.ic_globe, Ui.GLASS_SOFT, Color.WHITE, 38, open(Config.SITE_URL)));
        FrameLayout.LayoutParams sp = Ui.frame(Ui.WRAP, Ui.WRAP, Gravity.START | Gravity.CENTER_VERTICAL);
        sp.leftMargin = edge;
        root.addView(socials, sp);

        // ---------- Logo ----------
        LinearLayout center = Ui.vbox(this);
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        center.addView(Ui.logo(this, 1.45f, Gravity.CENTER_HORIZONTAL));
        center.addView(Ui.space(this, 10));
        center.addView(Ui.text(this, "um jogo do FlowLabs Studio", 11, 0xCCFFFFFF, Ui.MEDIUM));
        FrameLayout.LayoutParams cp = Ui.frame(Ui.WRAP, Ui.WRAP, Gravity.CENTER);
        cp.bottomMargin = Ui.dp(this, 36);
        root.addView(center, cp);

        // ---------- Base ----------
        LinearLayout bottom = Ui.hbox(this);
        bottom.setGravity(Gravity.BOTTOM);
        bottom.setPadding(edge, 0, edge, Ui.dp(this, 16));

        LinearLayout left = Ui.vbox(this);
        LinearLayout srv = Ui.hbox(this);
        srv.setBackground(Ui.pressable(Ui.rounded(this, Ui.GLASS_SOFT, 14, Ui.LINE),
                Ui.rounded(this, 0x33FFFFFF, 14, Ui.LINE)));
        srv.setPadding(Ui.dp(this, 10), Ui.dp(this, 9), Ui.dp(this, 12), Ui.dp(this, 9));
        FrameLayout srvIcon = new FrameLayout(this);
        srvIcon.setBackground(Ui.rounded(this, Ui.alpha(Ui.VIOLET, 0x40), 10, 0));
        srvIcon.addView(Ui.icon(this, R.drawable.ic_servers, 18, Color.WHITE),
                Ui.frame(Ui.dp(this, 18), Ui.dp(this, 18), Gravity.CENTER));
        srv.addView(srvIcon, Ui.lp(Ui.dp(this, 36), Ui.dp(this, 36)));
        srv.addView(Ui.hspace(this, 10));
        LinearLayout srvText = Ui.vbox(this);
        srvName = Ui.oneLine(Ui.text(this, "Carregando...", 13, Ui.TEXT, Ui.SEMIBOLD));
        srvText.addView(srvName);
        srvText.addView(Ui.space(this, 3));
        LinearLayout st = Ui.hbox(this);
        srvDot = new View(this);
        st.addView(srvDot, Ui.lp(Ui.dp(this, 7), Ui.dp(this, 7)));
        st.addView(Ui.hspace(this, 6));
        srvStatus = Ui.text(this, "", 11, Ui.MUTED, Ui.MEDIUM);
        st.addView(srvStatus);
        srvText.addView(st);
        srv.addView(srvText, Ui.lp(Ui.dp(this, 170), Ui.WRAP));
        srv.addView(Ui.icon(this, R.drawable.ic_chevron, 18, Ui.MUTED));
        srv.setClickable(true);
        srv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openServers(); }
        });
        left.addView(srv);
        left.addView(Ui.space(this, 8));
        left.addView(Ui.text(this, Config.COPYRIGHT, 9.5f, Ui.FAINT, Ui.REGULAR));
        bottom.addView(left);
        bottom.addView(Ui.flex(this));

        LinearLayout right = Ui.vbox(this);
        right.setGravity(Gravity.END);
        LinearLayout fl = Ui.hbox(this);
        filesIcon = Ui.icon(this, R.drawable.ic_check, 14, Ui.SUCCESS);
        fl.addView(filesIcon);
        fl.addView(Ui.hspace(this, 6));
        filesLine = Ui.text(this, "", 11.5f, Ui.MUTED, Ui.MEDIUM);
        fl.addView(filesLine);
        fl.setClickable(true);
        fl.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openSettings(SET_FILES); }
        });
        right.addView(fl);
        right.addView(Ui.space(this, 8));
        LinearLayout play = Ui.primary(this, R.drawable.ic_play, "JOGAR", new View.OnClickListener() {
            @Override public void onClick(View v) { onPlay(); }
        });
        ((TextView) play.getChildAt(2)).setTextSize(17);
        ((TextView) play.getChildAt(2)).setTypeface(Ui.font(this, Ui.BOLD));
        ((TextView) play.getChildAt(2)).setLetterSpacing(0.12f);
        right.addView(play, Ui.lp(Ui.dp(this, 230), Ui.dp(this, 54)));
        bottom.addView(right);

        root.addView(bottom, Ui.frame(Ui.MATCH, Ui.WRAP, Gravity.BOTTOM));
        refreshServerCard();
        return root;
    }

    private void refreshServerCard() {
        if (srvName == null) return;
        ServerInfo s = selectedServer();
        if (s == null) {
            srvName.setText("Carregando servidores...");
            srvStatus.setText("");
            srvDot.setBackground(Ui.circle(Ui.MUTED));
            return;
        }
        srvName.setText(s.name);
        srvStatus.setText(statusText(s));
        srvStatus.setTextColor(statusColor(s));
        srvDot.setBackground(Ui.circle(statusColor(s)));
    }

    private void refreshFiles(final boolean rescan) {
        new Thread(new Runnable() {
            @Override public void run() {
                if (rescan || fileStatus == null) fileStatus = GameFiles.check(MainActivity.this);
                runOnUiThread(new Runnable() {
                    @Override public void run() { applyFilesLine(); }
                });
            }
        }).start();
    }

    private void applyFilesLine() {
        if (filesLine == null) return;
        if (importing) {
            int pct = importTotal > 0 ? (int) (importDone * 100 / importTotal) : 0;
            filesLine.setText("Importando arquivos... " + pct + "%");
            filesLine.setTextColor(Ui.AMBER);
            filesIcon.setImageResource(R.drawable.ic_refresh);
            filesIcon.setColorFilter(Ui.AMBER);
            return;
        }
        GameFiles.Status st = fileStatus;
        boolean ok = st != null && st.state == GameFiles.READY;
        filesLine.setText(st == null ? "Verificando arquivos..."
                : ok ? "Arquivos verificados" : st.state == GameFiles.NOT_IMPORTED
                ? "Importe os arquivos do jogo" : "Arquivos incompletos");
        filesLine.setTextColor(ok ? Ui.MUTED : Ui.AMBER);
        filesIcon.setImageResource(ok ? R.drawable.ic_check : R.drawable.ic_warning);
        filesIcon.setColorFilter(ok ? Ui.SUCCESS : Ui.AMBER);
    }

    // =====================================================================
    // SERVIDORES
    // =====================================================================

    private void openServers() {
        final Sheet s = new Sheet(this, root, "Servidores", 580);
        s.headerActions.addView(Ui.iconButton(this, R.drawable.ic_refresh, Ui.FIELD, Ui.MUTED, 34, new View.OnClickListener() {
            @Override public void onClick(View v) {
                s.dismiss();
                loadRemote();
                Sheet.snack(MainActivity.this, root, "Atualizando servidores...", Ui.VIOLET);
            }
        }));
        String selId = settings.selectedServer();
        for (final ServerInfo info : servers) {
            boolean sel = info.id.equals(selId);
            LinearLayout row = Ui.hbox(this);
            row.setBackground(Ui.pressable(
                    Ui.rounded(this, sel ? Ui.alpha(Ui.VIOLET, 0x26) : Ui.FIELD, 14, sel ? Ui.VIOLET : Ui.LINE),
                    Ui.rounded(this, 0x26FFFFFF, 14, Ui.LINE)));
            row.setPadding(Ui.dp(this, 14), Ui.dp(this, 11), Ui.dp(this, 14), Ui.dp(this, 11));

            View dot = new View(this);
            dot.setBackground(Ui.circle(statusColor(info)));
            row.addView(dot, Ui.lp(Ui.dp(this, 8), Ui.dp(this, 8)));
            row.addView(Ui.hspace(this, 12));

            LinearLayout t = Ui.vbox(this);
            LinearLayout nameRow = Ui.hbox(this);
            nameRow.addView(Ui.text(this, info.name, 14, Ui.TEXT, Ui.SEMIBOLD));
            for (int i = 0; i < Math.min(2, info.tags.length); i++) {
                nameRow.addView(Ui.hspace(this, 6));
                nameRow.addView(Ui.chip(this, info.tags[i], Ui.MUTED));
            }
            t.addView(nameRow);
            t.addView(Ui.space(this, 3));
            t.addView(Ui.oneLine(Ui.text(this, info.description, 11.5f, Ui.MUTED, Ui.REGULAR)));
            row.addView(t, new LinearLayout.LayoutParams(0, Ui.WRAP, 1));
            row.addView(Ui.hspace(this, 12));

            LinearLayout side = Ui.vbox(this);
            side.setGravity(Gravity.END);
            side.addView(Ui.text(this, statusText(info), 12, statusColor(info), Ui.SEMIBOLD));
            side.addView(Ui.space(this, 3));
            side.addView(Ui.text(this, (info.players >= 0 ? String.valueOf(info.players) : "—") + "/" + info.maxPlayers,
                    11, Ui.FAINT, Ui.MEDIUM));
            row.addView(side);
            if (sel) {
                row.addView(Ui.hspace(this, 10));
                row.addView(Ui.icon(this, R.drawable.ic_check, 18, Ui.VIOLET));
            }
            row.setClickable(true);
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    settings.setSelectedServer(info.id);
                    refreshServerCard();
                    s.dismiss();
                    Sheet.snack(MainActivity.this, root, "Servidor: " + info.name, Ui.VIOLET);
                }
            });
            LinearLayout.LayoutParams p = Ui.lp(Ui.MATCH, Ui.WRAP);
            p.bottomMargin = Ui.dp(this, 8);
            s.body.addView(row, p);
        }
        s.show();
    }

    // =====================================================================
    // NOVIDADES
    // =====================================================================

    private void openNews() {
        Sheet s = new Sheet(this, root, "Novidades", 580);
        for (int i = 0; i < news.length; i++) {
            NewsItem n = news[i];
            LinearLayout item = Ui.vbox(this);
            item.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 10));
            LinearLayout head = Ui.hbox(this);
            head.addView(Ui.chip(this, n.tag, Ui.AMBER));
            head.addView(Ui.hspace(this, 8));
            head.addView(Ui.text(this, n.date, 10.5f, Ui.FAINT, Ui.MEDIUM));
            item.addView(head);
            item.addView(Ui.space(this, 6));
            item.addView(Ui.text(this, n.title, 14, Ui.TEXT, Ui.SEMIBOLD));
            item.addView(Ui.space(this, 3));
            TextView b = Ui.text(this, n.body, 12, Ui.MUTED, Ui.REGULAR);
            b.setMaxLines(2);
            b.setEllipsize(android.text.TextUtils.TruncateAt.END);
            b.setLineSpacing(0, 1.2f);
            item.addView(b);
            s.body.addView(item);
            if (i < news.length - 1) {
                s.body.addView(Ui.line(this));
                s.body.addView(Ui.space(this, 8));
            }
        }
        s.show();
    }

    // =====================================================================
    // LOJA
    // =====================================================================

    private void openStore() {
        Sheet s = new Sheet(this, root, "Loja VIP", 660);
        TextView intro = Ui.text(this, "A loja abre junto com a cidade. Os planos abaixo são uma prévia.",
                12, Ui.MUTED, Ui.REGULAR);
        s.body.addView(intro);
        s.body.addView(Ui.space(this, 12));

        String[][] plans = {
                {"Bronze", "R$ 14,90", "Tag VIP no chat\nFila prioritária\n1 veículo exclusivo"},
                {"Prata", "R$ 29,90", "Tudo do Bronze\nSalário extra\n2 vagas de garagem"},
                {"Ouro", "R$ 49,90", "Tudo do Prata\nCasa inicial\nSuporte prioritário"},
        };
        int[] colors = {0xFFE08A4A, 0xFFB9C2D6, Ui.AMBER};
        LinearLayout row = Ui.hbox(this);
        row.setGravity(Gravity.TOP);
        for (int i = 0; i < plans.length; i++) {
            LinearLayout c = Ui.vbox(this);
            boolean best = i == 2;
            c.setBackground(Ui.rounded(this, best ? Ui.alpha(Ui.AMBER, 0x1A) : Ui.FIELD, 16,
                    best ? Ui.alpha(Ui.AMBER, 0x90) : Ui.LINE));
            c.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 14));
            LinearLayout head = Ui.hbox(this);
            head.addView(Ui.chip(this, "VIP " + plans[i][0], colors[i]));
            if (best) {
                head.addView(Ui.flex(this));
                head.addView(Ui.text(this, "★", 13, Ui.AMBER, Ui.BOLD));
            }
            c.addView(head, Ui.lp(Ui.MATCH, Ui.WRAP));
            c.addView(Ui.space(this, 10));
            LinearLayout price = Ui.hbox(this);
            price.setGravity(Gravity.BOTTOM);
            price.addView(Ui.text(this, plans[i][1], 19, Ui.TEXT, Ui.BOLD));
            price.addView(Ui.text(this, " /mês", 11, Ui.FAINT, Ui.MEDIUM));
            c.addView(price);
            c.addView(Ui.space(this, 10));
            for (String perk : plans[i][2].split("\n")) {
                LinearLayout pr = Ui.hbox(this);
                pr.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
                pr.addView(Ui.icon(this, R.drawable.ic_check, 13, colors[i]));
                pr.addView(Ui.hspace(this, 6));
                pr.addView(Ui.oneLine(Ui.text(this, perk, 11.5f, Ui.MUTED, Ui.REGULAR)));
                c.addView(pr);
            }
            c.addView(Ui.space(this, 12));
            c.addView(best ? Ui.primary(this, 0, "Assinar", open(Config.STORE_URL))
                           : Ui.secondary(this, 0, "Assinar", open(Config.STORE_URL)), Ui.lp(Ui.MATCH, Ui.WRAP));
            row.addView(c, new LinearLayout.LayoutParams(0, Ui.WRAP, 1));
            if (i < plans.length - 1) row.addView(Ui.hspace(this, 10));
        }
        s.body.addView(row, Ui.lp(Ui.MATCH, Ui.WRAP));
        s.show();
    }

    // =====================================================================
    // AJUSTES (com abas: Gráficos, Jogo, Arquivos, Conta, Sobre)
    // =====================================================================

    private void openSettings(int tab) {
        settingsSheet = new Sheet(this, root, "Configurações", 720);
        settingsSheet.onDismiss(new Runnable() {
            @Override public void run() {
                settingsSheet = null;
                importFill = null;
                importText = null;
            }
        });
        LinearLayout row = Ui.hbox(this);
        row.setGravity(Gravity.TOP);

        LinearLayout tabs = Ui.vbox(this);
        settingsTabs = new LinearLayout[SET_LABELS.length];
        for (int i = 0; i < SET_LABELS.length; i++) {
            final int idx = i;
            LinearLayout t = Ui.hbox(this);
            t.setPadding(Ui.dp(this, 12), Ui.dp(this, 9), Ui.dp(this, 12), Ui.dp(this, 9));
            t.addView(Ui.icon(this, SET_ICONS[i], 16, Ui.MUTED));
            t.addView(Ui.hspace(this, 10));
            t.addView(Ui.text(this, SET_LABELS[i], 12.5f, Ui.MUTED, Ui.MEDIUM));
            t.setClickable(true);
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { showSettingsTab(idx); }
            });
            settingsTabs[i] = t;
            LinearLayout.LayoutParams p = Ui.lp(Ui.MATCH, Ui.WRAP);
            p.bottomMargin = Ui.dp(this, 4);
            tabs.addView(t, p);
        }
        row.addView(tabs, Ui.lp(Ui.dp(this, 150), Ui.WRAP));
        View sep = new View(this);
        sep.setBackgroundColor(Ui.LINE);
        LinearLayout.LayoutParams sepLp = Ui.lp(Math.max(1, Ui.dp(this, 1)), Ui.dp(this, 230));
        sepLp.leftMargin = Ui.dp(this, 12);
        sepLp.rightMargin = Ui.dp(this, 18);
        row.addView(sep, sepLp);

        settingsContent = new FrameLayout(this);
        settingsContent.setMinimumHeight(Ui.dp(this, 230));
        row.addView(settingsContent, new LinearLayout.LayoutParams(0, Ui.WRAP, 1));
        settingsSheet.body.addView(row, Ui.lp(Ui.MATCH, Ui.WRAP));
        settingsSheet.show();
        showSettingsTab(tab);
    }

    private void showSettingsTab(int tab) {
        if (settingsSheet == null) return;
        settingsTab = tab;
        for (int i = 0; i < settingsTabs.length; i++) {
            boolean sel = i == tab;
            LinearLayout t = settingsTabs[i];
            t.setBackground(sel ? Ui.rounded(this, Ui.alpha(Ui.VIOLET, 0x33), 10, 0) : null);
            ((ImageView) t.getChildAt(0)).setColorFilter(sel ? Ui.TEXT : Ui.MUTED);
            TextView tv = (TextView) t.getChildAt(2);
            tv.setTextColor(sel ? Ui.TEXT : Ui.MUTED);
            tv.setTypeface(Ui.font(this, sel ? Ui.SEMIBOLD : Ui.MEDIUM));
        }
        importFill = null;
        importText = null;
        View v;
        switch (tab) {
            case SET_GAME: v = tabGame(); break;
            case SET_FILES: v = tabFiles(); break;
            case SET_ACCOUNT: v = tabAccount(); break;
            case SET_ABOUT: v = tabAbout(); break;
            default: v = tabGraphics(); break;
        }
        settingsContent.removeAllViews();
        settingsContent.addView(v, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
    }

    private View tabGraphics() {
        LinearLayout c = Ui.vbox(this);
        c.addView(Ui.caps(this, "Qualidade gráfica", Ui.FAINT));
        c.addView(Ui.space(this, 8));
        c.addView(Ui.segmented(this, Settings.GRAPHICS, settings.graphics(), new Ui.Picker() {
            @Override public void pick(int i) { settings.setGraphics(i); }
        }), Ui.lp(Ui.MATCH, Ui.WRAP));
        c.addView(Ui.space(this, 16));
        c.addView(Ui.caps(this, "Limite de FPS", Ui.FAINT));
        c.addView(Ui.space(this, 8));
        String[] labels = new String[Settings.FPS.length];
        int sel = 1;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = Settings.FPS[i] + " FPS";
            if (Settings.FPS[i] == settings.fps()) sel = i;
        }
        c.addView(Ui.segmented(this, labels, sel, new Ui.Picker() {
            @Override public void pick(int i) { settings.setFps(Settings.FPS[i]); }
        }), Ui.lp(Ui.MATCH, Ui.WRAP));
        c.addView(Ui.space(this, 10));
        c.addView(Ui.toggle(this, "Mostrar FPS na tela", settings.showFps(), new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean v) { settings.setShowFps(v); }
        }), Ui.lp(Ui.MATCH, Ui.WRAP));
        c.addView(Ui.space(this, 6));
        c.addView(Ui.text(this, "Aplicado pelo cliente do jogo ao entrar no servidor.", 11, Ui.FAINT, Ui.REGULAR));
        return c;
    }

    private View tabGame() {
        LinearLayout c = Ui.vbox(this);
        c.addView(Ui.caps(this, "Jogo", Ui.FAINT));
        c.addView(Ui.space(this, 4));
        c.addView(Ui.toggle(this, "Chat de voz", settings.voiceChat(), new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean v) { settings.setVoiceChat(v); }
        }), Ui.lp(Ui.MATCH, Ui.WRAP));
        c.addView(Ui.toggle(this, "Notificações de eventos", settings.notifications(), new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean v) { settings.setNotifications(v); }
        }), Ui.lp(Ui.MATCH, Ui.WRAP));
        c.addView(Ui.toggle(this, "Vibração", settings.vibration(), new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean v) { settings.setVibration(v); }
        }), Ui.lp(Ui.MATCH, Ui.WRAP));
        c.addView(Ui.space(this, 14));
        LinearLayout r = Ui.hbox(this);
        r.addView(Ui.secondary(this, R.drawable.ic_refresh, "Restaurar padrão", new View.OnClickListener() {
            @Override public void onClick(View v) {
                String keep = settings.selectedServer();
                settings.reset();
                settings.setSelectedServer(keep);
                showSettingsTab(SET_GAME);
                Sheet.snack(MainActivity.this, root, "Ajustes restaurados.", Ui.VIOLET);
            }
        }));
        c.addView(r);
        return c;
    }

    private View tabFiles() {
        GameFiles.Status st = fileStatus != null ? fileStatus : GameFiles.check(this);
        fileStatus = st;
        String gta = GameFiles.installedGtaVersion(this);

        LinearLayout row = Ui.hbox(this);
        row.setGravity(Gravity.TOP);

        LinearLayout left = Ui.vbox(this);
        int color = st.state == GameFiles.READY ? Ui.SUCCESS : st.state == GameFiles.INCOMPLETE ? Ui.WARNING : Ui.DANGER;
        String label = st.state == GameFiles.READY ? "Pronto para jogar"
                : st.state == GameFiles.INCOMPLETE ? "Incompleto" : "Não importado";
        left.addView(Ui.chip(this, label, color));
        left.addView(Ui.space(this, 10));
        left.addView(kv("GTA SA (Play Store)", gta == null ? "Não encontrado" : "v" + gta));
        left.addView(kv("Itens verificados", st.foundCount() + " de " + st.checks.size()));
        left.addView(kv("Tamanho", st.fileCount == 0 ? "—" : GameFiles.formatSize(st.totalBytes)));
        left.addView(Ui.space(this, 12));

        if (importing) {
            FrameLayout track = new FrameLayout(this);
            track.setBackground(Ui.rounded(this, 0x26FFFFFF, 3, 0));
            importFill = new View(this);
            importFill.setBackground(Ui.gradient(this, new int[] {Ui.AMBER, Ui.MAGENTA}, 3,
                    GradientDrawable.Orientation.LEFT_RIGHT));
            track.addView(importFill, new FrameLayout.LayoutParams(0, Ui.MATCH));
            left.addView(track, Ui.lp(Ui.MATCH, Ui.dp(this, 5)));
            left.addView(Ui.space(this, 6));
            importText = Ui.oneLine(Ui.text(this, "", 11, Ui.MUTED, Ui.REGULAR));
            left.addView(importText);
            left.addView(Ui.space(this, 10));
            left.addView(Ui.secondary(this, R.drawable.ic_close, "Cancelar", new View.OnClickListener() {
                @Override public void onClick(View v) { if (importer != null) importer.cancel(); }
            }), Ui.lp(Ui.MATCH, Ui.WRAP));
            track.post(new Runnable() {
                @Override public void run() { updateImportViews(); }
            });
        } else {
            left.addView(Ui.primary(this, R.drawable.ic_folder,
                    st.state == GameFiles.NOT_IMPORTED ? "Importar pasta do GTA" : "Importar novamente",
                    new View.OnClickListener() {
                        @Override public void onClick(View v) { pickGtaFolder(); }
                    }), Ui.lp(Ui.MATCH, Ui.WRAP));
            left.addView(Ui.space(this, 8));
            LinearLayout b = Ui.hbox(this);
            b.addView(Ui.secondary(this, 0, "Verificar", new View.OnClickListener() {
                @Override public void onClick(View v) {
                    fileStatus = GameFiles.check(MainActivity.this);
                    applyFilesLine();
                    showSettingsTab(SET_FILES);
                    Sheet.snack(MainActivity.this, root, "Verificação concluída.", Ui.VIOLET);
                }
            }), Ui.weight(1));
            b.addView(Ui.hspace(this, 6));
            b.addView(Ui.secondary(this, 0, "Relatório", new View.OnClickListener() {
                @Override public void onClick(View v) { shareReport(); }
            }), Ui.weight(1));
            b.addView(Ui.hspace(this, 6));
            b.addView(Ui.secondary(this, 0, "Remover", new View.OnClickListener() {
                @Override public void onClick(View v) { confirmRemoveFiles(); }
            }), Ui.weight(1));
            left.addView(b, Ui.lp(Ui.MATCH, Ui.WRAP));
        }
        left.addView(Ui.space(this, 4));
        left.addView(Ui.link(this, "Como importar?", Ui.AMBER, new View.OnClickListener() {
            @Override public void onClick(View v) { howToImport(); }
        }));
        row.addView(left, new LinearLayout.LayoutParams(0, Ui.WRAP, 1.15f));
        row.addView(Ui.hspace(this, 16));

        LinearLayout right = Ui.vbox(this);
        right.setBackground(Ui.rounded(this, Ui.FIELD, 12, Ui.LINE));
        right.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        for (GameFiles.Check ch : st.checks) {
            LinearLayout l = Ui.hbox(this);
            l.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
            l.addView(Ui.icon(this, ch.found ? R.drawable.ic_check : R.drawable.ic_close, 13,
                    ch.found ? Ui.SUCCESS : Ui.DANGER));
            l.addView(Ui.hspace(this, 8));
            l.addView(Ui.oneLine(Ui.text(this, ch.req.label, 11, ch.found ? Ui.TEXT : Ui.MUTED, Ui.REGULAR)));
            right.addView(l);
        }
        row.addView(right, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        return row;
    }

    private View tabAccount() {
        LinearLayout c = Ui.vbox(this);
        LinearLayout head = Ui.hbox(this);
        head.addView(Ui.avatar(this, displayName(), 52));
        head.addView(Ui.hspace(this, 14));
        LinearLayout ht = Ui.vbox(this);
        ht.addView(Ui.text(this, displayName(), 17, Ui.TEXT, Ui.SEMIBOLD));
        ht.addView(Ui.space(this, 4));
        String email = session.email != null && session.email.length() > 0 ? "  ·  " + session.email : "";
        ht.addView(Ui.oneLine(Ui.text(this, session.typeLabel() + email, 11.5f, Ui.MUTED, Ui.REGULAR)));
        head.addView(ht, new LinearLayout.LayoutParams(0, Ui.WRAP, 1));
        c.addView(head, Ui.lp(Ui.MATCH, Ui.WRAP));
        c.addView(Ui.space(this, 14));

        String since = session.createdAt > 0
                ? new SimpleDateFormat("dd/MM/yy", new Locale("pt", "BR")).format(new Date(session.createdAt)) : "—";
        String[][] stats = {{"Nível", "1"}, {"Horas", "0h"}, {"Dinheiro", "—"}, {"Desde", since}};
        LinearLayout sr = Ui.hbox(this);
        for (int i = 0; i < stats.length; i++) {
            LinearLayout box = Ui.vbox(this);
            box.setBackground(Ui.rounded(this, Ui.FIELD, 12, Ui.LINE));
            box.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
            box.addView(Ui.caps(this, stats[i][0], Ui.FAINT));
            box.addView(Ui.space(this, 5));
            box.addView(Ui.text(this, stats[i][1], 15, Ui.TEXT, Ui.SEMIBOLD));
            sr.addView(box, new LinearLayout.LayoutParams(0, Ui.WRAP, 1));
            if (i < stats.length - 1) sr.addView(Ui.hspace(this, 8));
        }
        c.addView(sr, Ui.lp(Ui.MATCH, Ui.WRAP));
        c.addView(Ui.space(this, 6));
        c.addView(Ui.text(this, "Estatísticas sincronizam com o servidor quando a cidade abrir.", 10.5f, Ui.FAINT, Ui.REGULAR));
        c.addView(Ui.space(this, 14));

        LinearLayout acts = Ui.hbox(this);
        if (session.isGuest()) {
            acts.addView(Ui.primary(this, R.drawable.ic_person, "Criar conta", new View.OnClickListener() {
                @Override public void onClick(View v) { logout(); }
            }), Ui.weight(1));
        } else {
            acts.addView(Ui.secondary(this, R.drawable.ic_person, "Alterar nome no RP", new View.OnClickListener() {
                @Override public void onClick(View v) { askNickname(true); }
            }), Ui.weight(1));
        }
        acts.addView(Ui.hspace(this, 8));
        acts.addView(Ui.solid(this, R.drawable.ic_logout, "Sair", Ui.alpha(Ui.DANGER, 0x30), Ui.DANGER,
                new View.OnClickListener() {
                    @Override public void onClick(View v) { confirmLogout(); }
                }), Ui.weight(1));
        c.addView(acts, Ui.lp(Ui.MATCH, Ui.WRAP));
        return c;
    }

    private View tabAbout() {
        LinearLayout c = Ui.vbox(this);
        c.addView(Ui.logo(this, 0.7f, Gravity.START));
        c.addView(Ui.space(this, 14));
        c.addView(kv("Versão", Config.VERSION_NAME + " (build " + Config.VERSION_CODE + ")"));
        c.addView(kv("Estúdio", Config.STUDIO));
        c.addView(Ui.space(this, 10));
        c.addView(Ui.text(this, Config.COPYRIGHT, 11.5f, Ui.TEXT, Ui.MEDIUM));
        c.addView(Ui.space(this, 4));
        TextView d = Ui.text(this, Config.DISCLAIMER, 10.5f, Ui.FAINT, Ui.REGULAR);
        d.setLineSpacing(0, 1.2f);
        c.addView(d);
        c.addView(Ui.space(this, 8));
        LinearLayout links = Ui.hbox(this);
        links.addView(Ui.link(this, "Termos de Uso", Ui.AMBER, open(Config.TERMS_URL)));
        links.addView(Ui.hspace(this, 16));
        links.addView(Ui.link(this, "Privacidade", Ui.AMBER, open(Config.PRIVACY_URL)));
        links.addView(Ui.hspace(this, 16));
        links.addView(Ui.link(this, "Site oficial", Ui.AMBER, open(Config.SITE_URL)));
        c.addView(links);
        return c;
    }

    private View kv(String k, String v) {
        LinearLayout l = Ui.hbox(this);
        l.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
        l.addView(Ui.text(this, k, 12, Ui.MUTED, Ui.REGULAR), Ui.lp(Ui.dp(this, 130), Ui.WRAP));
        l.addView(Ui.oneLine(Ui.text(this, v, 12, Ui.TEXT, Ui.MEDIUM)), new LinearLayout.LayoutParams(0, Ui.WRAP, 1));
        return l;
    }

    // =====================================================================
    // Arquivos do jogo: importar, relatório, remover
    // =====================================================================

    private void howToImport() {
        Sheet.info(this, root, "Como importar os arquivos",
                "1. Instale o GTA San Andreas original pela Play Store e abra uma vez.\n"
              + "2. Copie Android/data/com.rockstargames.gtasa/files e os .obb de Android/obb/com.rockstargames.gtasa "
              + "para a pasta Download/GTA-SA (pelo PC ou por um gerenciador de arquivos).\n"
              + "3. Toque em \"Importar pasta do GTA\" e escolha Download/GTA-SA.\n\n"
              + "O Android não deixa um app ler a pasta de outro, por isso a cópia. "
              + "O FlowLabs nunca baixa nem distribui arquivos do jogo.");
    }

    private void pickGtaFolder() {
        try {
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQ_PICK_GTA);
        } catch (Exception e) {
            Sheet.snack(this, root, "Seu aparelho não tem seletor de pastas.", Ui.DANGER);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_GTA || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        startImport(data.getData());
    }

    private void startImport(Uri tree) {
        importing = true;
        importDone = 0;
        importTotal = 0;
        importCurrent = "Lendo a pasta...";
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        importer = new GameImporter(this, tree, new GameImporter.Listener() {
            @Override public void onProgress(long done, long total, String current) {
                if (total > 0) {
                    importDone = done;
                    importTotal = total;
                }
                importCurrent = current;
                updateImportViews();
                applyFilesLine();
            }

            @Override public void onFinished(boolean ok, String message) {
                importing = false;
                importer = null;
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                fileStatus = null;
                refreshFiles(true);
                if (isFinishing()) return;
                if (settingsSheet != null && settingsTab == SET_FILES) showSettingsTab(SET_FILES);
                Sheet.info(MainActivity.this, root, ok ? "Importação concluída" : "Não foi possível importar", message);
            }
        });
        importer.start();
        applyFilesLine();
        if (settingsSheet != null && settingsTab == SET_FILES) showSettingsTab(SET_FILES);
    }

    private void updateImportViews() {
        if (importFill == null || importText == null) return;
        View parent = (View) importFill.getParent();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) importFill.getLayoutParams();
        lp.width = importTotal > 0 ? (int) (parent.getWidth() * (importDone / (double) importTotal)) : 0;
        importFill.setLayoutParams(lp);
        String size = importTotal > 0 ? GameFiles.formatSize(importDone) + " / " + GameFiles.formatSize(importTotal) + "  ·  " : "";
        importText.setText(size + importCurrent);
    }

    private void shareReport() {
        Sheet.snack(this, root, "Gerando relatório...", Ui.VIOLET);
        new Thread(new Runnable() {
            @Override public void run() {
                final String r = GameFiles.report(MainActivity.this);
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        Intent send = new Intent(Intent.ACTION_SEND);
                        send.setType("text/plain");
                        send.putExtra(Intent.EXTRA_SUBJECT, "FlowLabs - estrutura dos arquivos");
                        send.putExtra(Intent.EXTRA_TEXT, r);
                        startActivity(Intent.createChooser(send, "Enviar relatório"));
                    }
                });
            }
        }).start();
    }

    private void confirmRemoveFiles() {
        Sheet.confirm(this, root, "Remover arquivos importados?",
                "Apaga a cópia dos arquivos feita pelo launcher. Seu GTA San Andreas instalado não é afetado.",
                "Remover", true, new Runnable() {
                    @Override public void run() {
                        new Thread(new Runnable() {
                            @Override public void run() {
                                GameFiles.deleteAll(MainActivity.this);
                                fileStatus = GameFiles.check(MainActivity.this);
                                runOnUiThread(new Runnable() {
                                    @Override public void run() {
                                        applyFilesLine();
                                        if (settingsSheet != null) showSettingsTab(SET_FILES);
                                        Sheet.snack(MainActivity.this, root, "Arquivos removidos.", Ui.VIOLET);
                                    }
                                });
                            }
                        }).start();
                    }
                });
    }

    // =====================================================================
    // Conta
    // =====================================================================

    private void askNickname(boolean cancelable) {
        if (session.isGuest()) return;
        final Sheet s = new Sheet(this, root, "Seu nome no RP", 440);
        TextView m = Ui.text(this, "Formato Nome_Sobrenome. É assim que os outros jogadores vão te ver na cidade.",
                12, Ui.MUTED, Ui.REGULAR);
        m.setLineSpacing(0, 1.2f);
        s.body.addView(m);
        s.body.addView(Ui.space(this, 12));
        final Ui.Field f = Ui.field(this, R.drawable.ic_person, "Ex.: Joao_Silva",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        if (session.hasNickname()) f.edit.setText(session.nickname);
        s.body.addView(f.view, Ui.lp(Ui.MATCH, Ui.WRAP));
        s.body.addView(Ui.space(this, 12));
        s.body.addView(Ui.primary(this, 0, "Salvar", new View.OnClickListener() {
            @Override public void onClick(View v) {
                String nick = f.value();
                String err = AccountStore.validateNickname(nick);
                if (err == null && Session.LOCAL.equals(session.type)) {
                    try {
                        new AccountStore(MainActivity.this).updateNickname(session.userId, nick);
                    } catch (IllegalArgumentException e) {
                        err = e.getMessage();
                    }
                }
                if (err != null) {
                    Sheet.snack(MainActivity.this, root, err, Ui.DANGER);
                    return;
                }
                session.nickname = nick;
                session.save(MainActivity.this);
                s.dismiss();
                rebuild();
            }
        }), Ui.lp(Ui.MATCH, Ui.WRAP));
        s.show();
    }

    private void confirmLogout() {
        Sheet.confirm(this, root, "Sair da conta?",
                session.isGuest() ? "Você está jogando como convidado." : "Você pode entrar de novo quando quiser.",
                "Sair", true, new Runnable() {
                    @Override public void run() { logout(); }
                });
    }

    private void logout() {
        Session.clear(this);
        goToAuth();
    }

    private void goToAuth() {
        Intent i = new Intent(this, AuthActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void rebuild() {
        settingsSheet = null;
        setContentView(buildHome());
        applyFilesLine();
    }

    // =====================================================================
    // JOGAR
    // =====================================================================

    private void onPlay() {
        ServerInfo s = selectedServer();
        switch (GameLauncher.preflight(this, session, s)) {
            case GameLauncher.NEED_NICKNAME:
                askNickname(true);
                break;
            case GameLauncher.NEED_FILES:
                Sheet.confirm(this, root, "Arquivos do jogo",
                        "Para jogar, importe os arquivos do seu GTA San Andreas original.",
                        "Importar", false, new Runnable() {
                            @Override public void run() { openSettings(SET_FILES); }
                        });
                break;
            case GameLauncher.SERVER_SOON:
                Sheet.info(this, root, "Em breve", (s == null ? "Esse servidor" : s.name)
                        + " ainda não abriu. Escolha outro servidor ou acompanhe as novidades no Discord.");
                break;
            case GameLauncher.SERVER_OFFLINE:
                Sheet.info(this, root, "Servidor offline", s.name
                        + " não respondeu. Tente de novo em alguns minutos ou veja os avisos no Discord.");
                break;
            default:
                Sheet.info(this, root, "Quase lá!", "Sua conta e seus arquivos estão prontos.\n\n"
                        + "O cliente multiplayer do FlowLabs está em desenvolvimento. Quando ele for liberado, "
                        + "este botão abre o jogo direto em " + s.name + ".");
                break;
        }
    }

    // =====================================================================
    // Dados remotos
    // =====================================================================

    private void loadRemote() {
        RemoteContent.servers(new RemoteContent.Result<ServerInfo[]>() {
            @Override public void onResult(ServerInfo[] list) {
                servers = list;
                refreshServerCard();
                for (ServerInfo s : servers) {
                    ServerPinger.ping(s, new ServerPinger.Callback() {
                        @Override public void onDone(ServerInfo server) {
                            runOnUiThread(new Runnable() {
                                @Override public void run() { refreshServerCard(); }
                            });
                        }
                    });
                }
            }
        });
        RemoteContent.news(new RemoteContent.Result<NewsItem[]>() {
            @Override public void onResult(NewsItem[] list) { news = list; }
        });
        RemoteContent.checkUpdate(new RemoteContent.Result<RemoteContent.UpdateInfo>() {
            @Override public void onResult(final RemoteContent.UpdateInfo u) {
                Sheet.confirm(MainActivity.this, root, "Atualização disponível (v" + u.versionName + ")",
                        u.changelog.length() > 0 ? u.changelog : "Uma nova versão do launcher está disponível.",
                        "Atualizar", false, new Runnable() {
                            @Override public void run() { Ui.openUrl(MainActivity.this, u.url); }
                        });
            }
        });
    }

    private ServerInfo selectedServer() {
        String id = settings.selectedServer();
        for (ServerInfo s : servers) if (s.id.equals(id)) return s;
        return servers.length > 0 ? servers[0] : null;
    }

    private String statusText(ServerInfo s) {
        if (s.comingSoon) return "Em breve";
        if (!s.checked) return "Verificando...";
        return s.online ? "Online · " + s.pingMs + " ms" : "Offline";
    }

    private int statusColor(ServerInfo s) {
        if (s.comingSoon) return Ui.WARNING;
        if (!s.checked) return Ui.MUTED;
        return s.online ? Ui.SUCCESS : Ui.DANGER;
    }

    private String displayName() {
        if (session.hasNickname()) return session.nickname.replace('_', ' ');
        if (session.displayName != null && session.displayName.length() > 0) return session.displayName;
        return "Jogador";
    }

    private View.OnClickListener open(final String url) {
        return new View.OnClickListener() {
            @Override public void onClick(View v) { Ui.openUrl(MainActivity.this, url); }
        };
    }
}
