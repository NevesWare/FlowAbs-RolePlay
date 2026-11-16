package com.nevesware.flowlabsrp;

import android.content.Context;
import android.content.SharedPreferences;

/** Configurações do jogador (gráficos, FPS, servidor escolhido...). */
public final class Settings {

    public static final String[] GRAPHICS = {"Baixo", "Médio", "Alto", "Ultra"};
    public static final int[] FPS = {30, 60, 90, 120};

    private final SharedPreferences p;

    public Settings(Context c) {
        p = c.getSharedPreferences("flowlabs_settings", Context.MODE_PRIVATE);
    }

    public int graphics() { return p.getInt("graphics", 1); }
    public void setGraphics(int v) { p.edit().putInt("graphics", v).apply(); }

    public int fps() { return p.getInt("fps", 60); }
    public void setFps(int v) { p.edit().putInt("fps", v).apply(); }

    public boolean showFps() { return p.getBoolean("show_fps", false); }
    public void setShowFps(boolean v) { p.edit().putBoolean("show_fps", v).apply(); }

    public boolean voiceChat() { return p.getBoolean("voice_chat", true); }
    public void setVoiceChat(boolean v) { p.edit().putBoolean("voice_chat", v).apply(); }

    public boolean notifications() { return p.getBoolean("notifications", true); }
    public void setNotifications(boolean v) { p.edit().putBoolean("notifications", v).apply(); }

    public boolean vibration() { return p.getBoolean("vibration", true); }
    public void setVibration(boolean v) { p.edit().putBoolean("vibration", v).apply(); }

    public String selectedServer() { return p.getString("server", "flrp-1"); }
    public void setSelectedServer(String id) { p.edit().putString("server", id).apply(); }

    public void reset() { p.edit().clear().apply(); }
}
