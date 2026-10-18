package com.pardal.kitapligim;

import org.json.*;
import java.util.*;
import java.time.*;

/** Pure data rules shared by storage, Notion mapping, and sync tests. */
public final class Book {
    public static final List<String> PROFILES = List.of("Güleser", "İlker", "Elis", "Lila");
    public static final List<String> STATUSES = List.of("want", "reading", "done", "paused");
    public static final String[] FIELDS = {"profile","title","author","isbn","cover","totalPages","currentPage","status","rating","notes","startedAt","finishedAt","photoKey","deleted"};
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
        return b;
    }
    public static boolean same(JSONObject a, JSONObject b) {
        for (String key : FIELDS) if (!Objects.equals(a.opt(key), b.opt(key))) return false;
        return true;
    }
    public static String statusName(String id) { return switch (id) { case "reading" -> "Okuyorum"; case "done" -> "Okudum"; case "paused" -> "Ara verdim"; default -> "Okuyacağım"; }; }
    public static String statusId(String name) { return switch (name) { case "Okuyorum" -> "reading"; case "Okudum" -> "done"; case "Ara verdim" -> "paused"; default -> "want"; }; }
}
