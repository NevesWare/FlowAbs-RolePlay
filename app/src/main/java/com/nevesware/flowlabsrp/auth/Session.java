package com.nevesware.flowlabsrp.auth;

import android.content.Context;
import android.content.SharedPreferences;

/** Sessão do jogador logado (conta local, Google, Discord ou convidado). */
public final class Session {

    public static final String LOCAL = "local";
    public static final String GOOGLE = "google";
    public static final String DISCORD = "discord";
    public static final String GUEST = "guest";

    public String type;
    public String userId;
    public String nickname;   // nome no RP: Nome_Sobrenome
    public String email;
    public String displayName;
    public String accessToken;
    public String refreshToken;
    public long expiresAt;
    public long createdAt;

    private static final String PREFS = "flowlabs_session";

    public boolean isGuest() {
        return GUEST.equals(type);
    }

    public boolean hasNickname() {
        return nickname != null && nickname.length() > 0;
    }

    public String typeLabel() {
        if (GOOGLE.equals(type)) return "Google";
        if (DISCORD.equals(type)) return "Discord";
        if (GUEST.equals(type)) return "Convidado";
        return "Conta FlowLabs";
    }

    public static Session load(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!p.contains("type")) return null;
        Session s = new Session();
        s.type = p.getString("type", LOCAL);
        s.userId = p.getString("user_id", "");
        s.nickname = p.getString("nickname", "");
        s.email = p.getString("email", "");
        s.displayName = p.getString("display_name", "");
        s.accessToken = p.getString("access_token", "");
        s.refreshToken = p.getString("refresh_token", "");
        s.expiresAt = p.getLong("expires_at", 0);
        s.createdAt = p.getLong("created_at", 0);
        return s;
    }

    public void save(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("type", type)
                .putString("user_id", userId)
                .putString("nickname", nickname == null ? "" : nickname)
                .putString("email", email == null ? "" : email)
                .putString("display_name", displayName == null ? "" : displayName)
                .putString("access_token", accessToken == null ? "" : accessToken)
                .putString("refresh_token", refreshToken == null ? "" : refreshToken)
                .putLong("expires_at", expiresAt)
                .putLong("created_at", createdAt)
                .apply();
    }

    public static void clear(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
