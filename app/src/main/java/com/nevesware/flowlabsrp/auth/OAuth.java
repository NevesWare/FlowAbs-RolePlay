package com.nevesware.flowlabsrp.auth;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.nevesware.flowlabsrp.Config;
import com.nevesware.flowlabsrp.net.Http;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Login com Google / Discord através do Supabase Auth.
 *
 * Fluxo:
 *  1. start() abre o navegador em SUPABASE_URL/auth/v1/authorize?provider=google&redirect_to=flowlabsrp://auth/callback
 *  2. O jogador entra com a conta Google/Discord.
 *  3. O Supabase redireciona para flowlabsrp://auth/callback#access_token=...&refresh_token=...
 *  4. O Android abre a AuthActivity com essa URL -> parseCallback() -> fetchUser().
 *
 * Para ativar: preencher SUPABASE_URL e SUPABASE_ANON_KEY em Config.java e
 * habilitar os provedores Google e Discord no painel do Supabase.
 */
public final class OAuth {

    private OAuth() {}

    public static class Tokens {
        public String accessToken;
        public String refreshToken;
        public long expiresAt;
        public String provider;
    }

    public static void start(Activity a, String provider) throws Exception {
        String url = Config.SUPABASE_URL + "/auth/v1/authorize?provider=" + provider
                + "&redirect_to=" + URLEncoder.encode(Config.OAUTH_REDIRECT, "UTF-8");
        a.getSharedPreferences("flowlabs_oauth", 0).edit().putString("provider", provider).apply();
        a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    public static boolean isCallback(Uri uri) {
        return uri != null && "flowlabsrp".equals(uri.getScheme()) && "auth".equals(uri.getHost());
    }

    /** Lê os tokens do retorno. Lança IllegalArgumentException com a mensagem de erro, se houver. */
    public static Tokens parseCallback(Activity a, Uri uri) {
        Map<String, String> params = new HashMap<String, String>();
        addParams(params, uri.getEncodedQuery());
        addParams(params, uri.getEncodedFragment());

        if (params.containsKey("error")) {
            String d = params.get("error_description");
            throw new IllegalArgumentException(d != null ? d : params.get("error"));
        }
        String token = params.get("access_token");
        if (token == null || token.length() == 0) {
            throw new IllegalArgumentException("O login não retornou um token. Tente de novo.");
        }
        Tokens t = new Tokens();
        t.accessToken = token;
        t.refreshToken = params.get("refresh_token");
        long expiresIn = 3600;
        try { expiresIn = Long.parseLong(params.get("expires_in")); } catch (Exception ignored) {}
        t.expiresAt = System.currentTimeMillis() + expiresIn * 1000L;
        t.provider = a.getSharedPreferences("flowlabs_oauth", 0).getString("provider", Session.GOOGLE);
        return t;
    }

    /** Busca os dados do usuário no Supabase. Chamar fora da thread principal. */
    public static Session fetchUser(Tokens t) throws Exception {
        Map<String, String> h = new HashMap<String, String>();
        h.put("apikey", Config.SUPABASE_ANON_KEY);
        h.put("Authorization", "Bearer " + t.accessToken);
        Http.Response r = Http.get(Config.SUPABASE_URL + "/auth/v1/user", h);
        if (!r.ok()) throw new Exception("Falha ao buscar o usuário (" + r.code + ").");

        JSONObject u = new JSONObject(r.body);
        JSONObject meta = u.optJSONObject("user_metadata");
        Session s = new Session();
        s.type = Session.DISCORD.equals(t.provider) ? Session.DISCORD : Session.GOOGLE;
        s.userId = u.optString("id");
        s.email = u.optString("email");
        s.displayName = meta == null ? "" : meta.optString("full_name", meta.optString("name", ""));
        // O nome de RP é escolhido depois (vem do perfil no banco quando existir).
        s.nickname = meta == null ? "" : meta.optString("rp_nickname", "");
        s.accessToken = t.accessToken;
        s.refreshToken = t.refreshToken;
        s.expiresAt = t.expiresAt;
        s.createdAt = System.currentTimeMillis();
        return s;
    }

    private static void addParams(Map<String, String> out, String encoded) {
        if (encoded == null) return;
        String[] parts = encoded.split("&");
        for (String part : parts) {
            int i = part.indexOf('=');
            if (i <= 0) continue;
            out.put(Uri.decode(part.substring(0, i)), Uri.decode(part.substring(i + 1)));
        }
    }
}
