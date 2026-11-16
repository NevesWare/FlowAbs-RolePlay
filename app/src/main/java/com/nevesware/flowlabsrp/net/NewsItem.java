package com.nevesware.flowlabsrp.net;

import org.json.JSONObject;

/** Uma notícia do feed do launcher. */
public class NewsItem {

    public final String tag;
    public final String title;
    public final String date;
    public final String body;

    public NewsItem(String tag, String title, String date, String body) {
        this.tag = tag;
        this.title = title;
        this.date = date;
        this.body = body;
    }

    public static NewsItem fromJson(JSONObject o) {
        return new NewsItem(o.optString("tag", "NOVIDADE"), o.optString("title"),
                o.optString("date"), o.optString("body"));
    }
}
