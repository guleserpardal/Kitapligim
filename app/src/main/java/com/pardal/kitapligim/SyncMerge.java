package com.pardal.kitapligim;

import org.json.*;

/** Never overwrite both-sided edits silently; keep the local and remote alternatives. */
public final class SyncMerge {
    public static JSONObject merge(JSONObject local, JSONObject remote) throws JSONException {
        if (local == null) return synced(remote, remote);
        boolean dirty = !"synced".equals(local.optString("syncStatus"));
        boolean changed = !local.optString("remoteEditedAt").equals(remote.optString("remoteEditedAt"));
        if (!dirty || Book.same(local, remote)) return synced(local, remote);
        if (!changed) { JSONObject b=Book.copy(local); b.put("remotePageId", remote.optString("remotePageId")); return b; }
        JSONObject result=Book.copy(local);
        result.put("syncStatus", "conflict").put("conflictRemote", remote);
        return result;
    }
    public static JSONObject synced(JSONObject local, JSONObject remote) throws JSONException {
        JSONObject result=Book.copy(remote);
        result.put("id", local.optString("id", remote.optString("id")));
        if (!local.optString("localCover").isEmpty()) result.put("localCover", local.optString("localCover"));
        // A Notion table without the newer columns must not erase what was saved on the phone.
        for (String key : Book.PURCHASE) if (!result.has(key) && local.has(key)) result.put(key, local.get(key));
        if (!result.has("highlights") && local.has("highlights")) result.put("highlights", local.get("highlights"));
        // Notion does not keep passage ids or dates; keep the phone's when the passage is unchanged.
        else if (result.has("highlights") && local.has("highlights") && Book.highlightKey(result.optJSONArray("highlights")).equals(Book.highlightKey(local.optJSONArray("highlights")))) result.put("highlights", local.get("highlights"));
        result.put("syncStatus", "synced"); result.remove("conflictRemote");
        return result;
    }
    public static JSONObject resolve(JSONObject local, boolean useRemote) throws JSONException {
        JSONObject remote=local.getJSONObject("conflictRemote");
        if (useRemote) return synced(local, remote);
        JSONObject result=Book.copy(local); result.put("remoteEditedAt", remote.getString("remoteEditedAt")).put("remotePageId", remote.getString("remotePageId")).put("syncStatus", "pending"); result.remove("conflictRemote"); return result;
    }
}
