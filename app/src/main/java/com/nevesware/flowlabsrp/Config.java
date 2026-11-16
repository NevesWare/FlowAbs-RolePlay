package com.nevesware.flowlabsrp;

import com.nevesware.flowlabsrp.net.ServerInfo;
import com.nevesware.flowlabsrp.net.NewsItem;

/**
 * Configuração central do launcher FlowLabs Roleplay.
 * Tudo que muda entre ambientes (links, Supabase, servidores) fica aqui.
 */
public final class Config {

    private Config() {}

    // ---------------- Marca ----------------
    public static final String STUDIO = "FlowLabs Studio";
    public static final String GAME_NAME = "FlowLabs Roleplay";
    public static final String SHORT_TAG = "FLRP";
    public static final String VERSION_NAME = "1.0.0";
    public static final int VERSION_CODE = 1;
    public static final String COPYRIGHT =
            "© 2026 FlowLabs Studio. Todos os direitos reservados.";
    public static final String DISCLAIMER =
            "FlowLabs Roleplay não é afiliado à Rockstar Games nem à Take-Two Interactive. "
          + "Requer uma cópia original de GTA San Andreas instalada no aparelho. "
          + "O launcher não distribui arquivos do jogo.";

    // ---------------- Links ----------------
    // Troque pelos links reais da comunidade.
    public static final String DISCORD_URL = "https://discord.gg/flowlabs";
    public static final String INSTAGRAM_URL = "https://instagram.com/flowlabsrp";
    public static final String TIKTOK_URL = "https://tiktok.com/@flowlabsrp";
    public static final String SITE_URL = "https://flowlabs.gg";
    public static final String TERMS_URL = "https://flowlabs.gg/termos";
    public static final String PRIVACY_URL = "https://flowlabs.gg/privacidade";
    public static final String STORE_URL = "https://flowlabs.gg/loja";

    // ---------------- Supabase (login Google / Discord) ----------------
    // Deixe vazio enquanto o Supabase não estiver conectado: o launcher usa contas locais.
    // Ao conectar: Project URL e a chave "anon"/publishable do painel do Supabase.
    // No Supabase, em Authentication > URL Configuration, adicione o redirect:
    //     flowlabsrp://auth/callback
    public static final String SUPABASE_URL = "";
    public static final String SUPABASE_ANON_KEY = "";
    public static final String OAUTH_REDIRECT = "flowlabsrp://auth/callback";

    public static boolean supabaseReady() {
        return SUPABASE_URL.length() > 0 && SUPABASE_ANON_KEY.length() > 0;
    }

    // ---------------- Contas ----------------
    /** Botão "Jogar como convidado". Desligue (false) quando o Supabase estiver no ar. */
    public static final boolean ALLOW_GUEST = true;

    // ---------------- Conteúdo remoto (opcional) ----------------
    // JSONs hospedados no site. Vazio = usa a lista embutida abaixo.
    //  servers.json: [{"id","name","description","host","port","maxPlayers","tags":[..],"comingSoon"}]
    //  news.json:    [{"tag","title","date","body"}]
    //  update.json:  {"versionCode":2,"versionName":"1.0.1","url":"https://...apk","changelog":"..."}
    public static final String SERVERS_URL = "";
    public static final String NEWS_URL = "";
    public static final String UPDATE_URL = "";

    // ---------------- Jogo ----------------
    /** Pacote oficial do GTA San Andreas na Play Store. */
    public static final String GTA_PACKAGE = "com.rockstargames.gtasa";

    // ---------------- Lista embutida ----------------
    public static ServerInfo[] defaultServers() {
        return new ServerInfo[] {
            new ServerInfo("flrp-1", "FlowLabs Roleplay #1",
                    "Cidade principal. Empregos, facções, empresas e economia real.",
                    "rp1.flowlabs.gg", 22003, 500,
                    new String[] {"Roleplay", "Economia", "Facções"}, false),
            new ServerInfo("flrp-2", "FlowLabs Roleplay #2",
                    "Segunda cidade para quando a #1 lotar.",
                    "rp2.flowlabs.gg", 22003, 500,
                    new String[] {"Roleplay", "Novatos"}, true),
            new ServerInfo("fl-drift", "FlowLabs Drift & Corrida",
                    "Rachas, drift e eventos semanais com premiação.",
                    "drift.flowlabs.gg", 22003, 200,
                    new String[] {"Corrida", "Eventos"}, true),
        };
    }

    public static NewsItem[] defaultNews() {
        return new NewsItem[] {
            new NewsItem("NOVIDADE", "Bem-vindo ao FlowLabs Roleplay",
                    "16/11/2026",
                    "O launcher oficial está no ar. Crie sua conta, importe os arquivos do seu GTA San Andreas original e fique de olho no Discord para a data de abertura da cidade."),
            new NewsItem("WHITELIST", "Whitelist aberta no Discord",
                    "16/11/2026",
                    "Entre no nosso Discord, leia as regras e faça a whitelist para garantir sua vaga no primeiro dia."),
            new NewsItem("EQUIPE", "Estamos contratando",
                    "16/11/2026",
                    "Procuramos scripters Lua, mappers e moderadores. Mande sua candidatura no canal #vagas."),
        };
    }
}
