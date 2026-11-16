package com.nevesware.flowlabsrp.net;

import org.json.JSONArray;
import org.json.JSONObject;

/** Um servidor da lista do launcher. */
public class ServerInfo {

    public final String id;
    public final String name;
    public final String description;
    public final String host;
    public final int port;
    public final int maxPlayers;
    public final String[] tags;
    public final boolean comingSoon;

    // Estado em tempo de execução (preenchido pelo ServerPinger).
    public volatile int pingMs = -1;      // -1 = ainda não medido
    public volatile boolean online = false;
    public volatile boolean checked = false;
    public volatile int players = -1;     // -1 = desconhecido (vem da API do servidor no futuro)

    public ServerInfo(String id, String name, String description, String host, int port,
                      int maxPlayers, String[] tags, boolean comingSoon) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.host = host;
        this.port = port;
        this.maxPlayers = maxPlayers;
        this.tags = tags == null ? new String[0] : tags;
        this.comingSoon = comingSoon;
    }

    public String address() {
        return host + ":" + port;
    }

    public static ServerInfo fromJson(JSONObject o) {
        JSONArray t = o.optJSONArray("tags");
        String[] tags = new String[t == null ? 0 : t.length()];
        for (int i = 0; i < tags.length; i++) tags[i] = t.optString(i);
        ServerInfo s = new ServerInfo(
                o.optString("id"), o.optString("name"), o.optString("description"),
                o.optString("host"), o.optInt("port", 22003), o.optInt("maxPlayers", 0),
                tags, o.optBoolean("comingSoon", false));
        if (o.has("players")) s.players = o.optInt("players", -1);
        return s;
    }
}
