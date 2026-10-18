package com.pardal.kitapligim;

import org.json.*;
import java.util.*;
import java.time.*;

/** Pure data rules shared by storage, Notion mapping, and sync tests. */
public final class Book {
    public static final List<String> PROFILES = List.of("Güleser", "İlker", "Elis", "Lila");
    public static final List<String> STATUSES = List.of("want", "reading", "done", "paused");
    public static final String[] FIELDS = {"profile","title","author","isbn","cover","totalPages","currentPage","status","rating","notes","startedAt","finishedAt","photoKey","deleted"};
    /** Purchase details are optional: older saves and a Notion table without these columns simply lack the keys. */
    public static final String[] PURCHASE = {"price","buyUrl","store"};
    /** Underlined passages: each has text, an optional page and an optional note. Older saves have no list. */
    public static final int MAX_HIGHLIGHTS = 500;
    public static JSONObject copy(JSONObject b) throws JSONException { return new JSONObject(b.toString()); }
    public static String now() { return Instant.now().toString(); }
    public static JSONObject normalize(JSONObject input) throws JSONException {
        JSONObject b = copy(input);
        String title = b.optString("title").trim();
        if (title.isEmpty() || title.length() > 400) throw new IllegalArgumentException("Kitap adı 1–400 karakter olmalı.");
        if (!PROFILES.contains(b.optString("profile"))) throw new IllegalArgumentException("Bir aile profili seç.");
        if (!STATUSES.contains(b.optString("status"))) throw new IllegalArgumentException("Kitabın rafını seç.");
        int total = b.optInt("totalPages"), current = b.optInt("currentPage"), rating = b.optInt("rating");
        if (total < 0 || total > 100000 || current < 0 || current > 100000 || (total > 0 && current > total)) throw new IllegalArgumentException("Kaldığın sayfa toplam sayfayı geçemez.");
        if (rating < 0 || rating > 5) throw new IllegalArgumentException("Puan 0–5 arasında olmalı.");
        if (b.optString("notes").length() > 12000) throw new IllegalArgumentException("Notlar en fazla 12.000 karakter olabilir.");
        for (String key : List.of("startedAt", "finishedAt")) if (!b.optString(key).isEmpty()) LocalDate.parse(b.getString(key));
        if (!b.optString("startedAt").isEmpty() && !b.optString("finishedAt").isEmpty() && b.getString("finishedAt").compareTo(b.getString("startedAt")) < 0) throw new IllegalArgumentException("Bitiş tarihi başlangıçtan önce olamaz.");
        String cover = b.optString("cover");
        if (!cover.isEmpty() && !cover.startsWith("https://")) throw new IllegalArgumentException("Kapak adresi https ile başlamalı.");
        b.put("title", title).put("author", b.optString("author").trim()).put("totalPages", total).put("currentPage", current).put("rating", rating);
        for (String key : List.of("author", "isbn", "cover", "notes", "startedAt", "finishedAt", "photoKey")) b.put(key, b.optString(key));
        b.put("deleted", b.optBoolean("deleted"));
        if (b.has("price")) { String price = b.optString("price").trim(); if (price.length() > 40) throw new IllegalArgumentException("Fiyat en fazla 40 karakter olabilir."); b.put("price", price); }
        if (b.has("store")) { String store = b.optString("store").trim(); if (store.length() > 60) throw new IllegalArgumentException("Mağaza adı en fazla 60 karakter olabilir."); b.put("store", store); }
        if (b.has("buyUrl")) {
            String url = b.optString("buyUrl").trim();
            if (!url.isEmpty() && (!url.startsWith("https://") || url.length() > 2000)) throw new IllegalArgumentException("Satın alma linki https ile başlamalı.");
            b.put("buyUrl", url);
        }
        if (b.has("highlights")) b.put("highlights", highlights(b.optJSONArray("highlights")));
        return b;
    }
    public static JSONArray highlights(JSONArray input) throws JSONException {
        JSONArray out = new JSONArray(); if (input == null) return out;
        if (input.length() > MAX_HIGHLIGHTS) throw new IllegalArgumentException("Bir kitapta en fazla " + MAX_HIGHLIGHTS + " altı çizili cümle olabilir.");
        for (int i = 0; i < input.length(); i++) {
            JSONObject h = input.optJSONObject(i); if (h == null) continue;
            String text = tidy(h.optString("text")), note = tidy(h.optString("note"));
            if (text.isEmpty()) continue;
            if (text.length() > 4000) throw new IllegalArgumentException("Altı çizili bir cümle en fazla 4.000 karakter olabilir.");
            if (note.length() > 2000) throw new IllegalArgumentException("Cümle notu en fazla 2.000 karakter olabilir.");
            int page = h.optInt("page"); if (page < 0 || page > 100000) throw new IllegalArgumentException("Sayfa numarası geçersiz.");
            String id = h.optString("id"); if (id.isEmpty()) id = "h" + i;
            out.put(new JSONObject().put("id", id).put("text", text).put("page", page).put("note", note).put("createdAt", h.optString("createdAt")));
        }
        return out;
    }
    /** Blank lines separate passages in Notion, so a passage keeps single line breaks only. */
    static String tidy(String s) { return s.replace("\r", "").replaceAll("\n{2,}", "\n").trim(); }
    /** Passages compare by what the reader wrote; ids and timestamps are local bookkeeping. */
    static String highlightKey(JSONArray a) {
        StringBuilder s = new StringBuilder(); if (a == null) return "";
        for (int i = 0; i < a.length(); i++) { JSONObject h = a.optJSONObject(i); if (h != null) s.append(h.optString("text")).append('|').append(h.optInt("page")).append('|').append(h.optString("note")).append("||"); }
        return s.toString();
    }
    public static boolean same(JSONObject a, JSONObject b) {
        for (String key : FIELDS) if (!Objects.equals(a.opt(key), b.opt(key))) return false;
        for (String key : PURCHASE) if (a.has(key) && b.has(key) && !a.optString(key).equals(b.optString(key))) return false;
        if (a.has("highlights") && b.has("highlights") && !highlightKey(a.optJSONArray("highlights")).equals(highlightKey(b.optJSONArray("highlights")))) return false;
        return true;
    }
    public static String statusName(String id) { return switch (id) { case "reading" -> "Okuyorum"; case "done" -> "Okudum"; case "paused" -> "Ara verdim"; default -> "Okuyacağım"; }; }
    public static String statusId(String name) { return switch (name) { case "Okuyorum" -> "reading"; case "Okudum" -> "done"; case "Ara verdim" -> "paused"; default -> "want"; }; }
}
