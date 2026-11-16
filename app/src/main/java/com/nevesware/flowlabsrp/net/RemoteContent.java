package com.nevesware.flowlabsrp.net;

import android.os.Handler;
import android.os.Looper;

import com.nevesware.flowlabsrp.Config;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Busca servidores, notícias e atualizações nos JSONs do site (Config.*_URL).
 * Se a URL estiver vazia ou falhar, usa a lista embutida no app.
 */
public final class RemoteContent {

    private RemoteContent() {}

    public interface Result<T> {
        void onResult(T value);
    }

    public static class UpdateInfo {
        public int versionCode;
        public String versionName;
        public String url;
        public String changelog;
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void servers(final Result<ServerInfo[]> cb) {
        if (Config.SERVERS_URL.length() == 0) {
            cb.onResult(Config.defaultServers());
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                ServerInfo[] list = Config.defaultServers();
                try {
                    Http.Response r = Http.get(Config.SERVERS_URL, null);
                    if (r.ok()) {
                        JSONArray a = new JSONArray(r.body);
                        ServerInfo[] remote = new ServerInfo[a.length()];
                        for (int i = 0; i < a.length(); i++) {
                            remote[i] = ServerInfo.fromJson(a.getJSONObject(i));
                        }
                        if (remote.length > 0) list = remote;
                    }
                } catch (Exception ignored) {}
                post(cb, list);
            }
        }).start();
    }

    public static void news(final Result<NewsItem[]> cb) {
        if (Config.NEWS_URL.length() == 0) {
            cb.onResult(Config.defaultNews());
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                NewsItem[] list = Config.defaultNews();
                try {
                    Http.Response r = Http.get(Config.NEWS_URL, null);
                    if (r.ok()) {
                        JSONArray a = new JSONArray(r.body);
                        NewsItem[] remote = new NewsItem[a.length()];
                        for (int i = 0; i < a.length(); i++) {
                            remote[i] = NewsItem.fromJson(a.getJSONObject(i));
                        }
                        if (remote.length > 0) list = remote;
                    }
                } catch (Exception ignored) {}
                post(cb, list);
            }
        }).start();
    }

    /** Retorna a atualização se existir uma versão mais nova que a instalada; senão null. */
    public static void checkUpdate(final Result<UpdateInfo> cb) {
        if (Config.UPDATE_URL.length() == 0) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                UpdateInfo info = null;
                try {
                    Http.Response r = Http.get(Config.UPDATE_URL, null);
                    if (r.ok()) {
                        JSONObject o = new JSONObject(r.body);
                        if (o.optInt("versionCode", 0) > Config.VERSION_CODE) {
                            info = new UpdateInfo();
                            info.versionCode = o.optInt("versionCode");
                            info.versionName = o.optString("versionName");
                            info.url = o.optString("url");
                            info.changelog = o.optString("changelog");
                        }
                    }
                } catch (Exception ignored) {}
                if (info != null) post(cb, info);
            }
        }).start();
    }

    private static <T> void post(final Result<T> cb, final T value) {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                cb.onResult(value);
            }
        });
    }
}
