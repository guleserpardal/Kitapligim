package com.pardal.kitapligim;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReadingProgressTest {
    private final String date="2026-10-25",time="2026-10-25T17:00:00Z";
    private JSONObject b(int page) throws Exception{return Book.normalize(new JSONObject().put("id","b1").put("title","Bir kitap").put("profile","Güleser").put("status","reading").put("currentPage",page).put("totalPages",500));}
    private JSONObject with(JSONObject book,JSONArray rows) throws Exception{return Book.copy(book).put("readingDays",rows);}
    @Test public void oldPositionIsBaselineAndRepeatedSaveDoesNotAddAgain() throws Exception {
        JSONArray rows=ReadingProgress.record(b(87),b(97),date,20,time);assertEquals(10,rows.getJSONObject(0).getInt("end")-rows.getJSONObject(0).getInt("start"));
        rows=ReadingProgress.record(with(b(97),rows),b(97),date,20,time);assertEquals(1,rows.length());assertEquals(87,rows.getJSONObject(0).getInt("start"));
        rows=ReadingProgress.record(with(b(97),rows),b(107),date,20,time);assertEquals(20,rows.getJSONObject(0).getInt("end")-rows.getJSONObject(0).getInt("start"));
    }
    @Test public void newBooksAndProfileTransfersDoNotInventReading() throws Exception {
        assertEquals(0,ReadingProgress.record(null,b(200),date,20,time).length());
        assertEquals(0,ReadingProgress.record(b(87),b(97).put("profile","Lila"),date,20,time).length());
    }
    @Test public void correctionAdjustsTodayInsteadOfAddingMorePages() throws Exception {
        JSONArray a=ReadingProgress.record(b(87),b(117),date,20,time);
        JSONArray corrected=ReadingProgress.record(with(b(117),a),b(92),date,20,"2026-10-25T18:00:00Z");
        assertEquals(87,corrected.getJSONObject(0).getInt("start"));assertEquals(92,corrected.getJSONObject(0).getInt("end"));
        JSONArray merged=ReadingProgress.merge(a,corrected);assertEquals(92,merged.getJSONObject(0).getInt("end"));
    }
    @Test public void newDayStartsAtLastSavedPositionAndKeepsYesterday() throws Exception {
        JSONArray a=ReadingProgress.record(b(87),b(107),date,20,time);
        JSONArray next=ReadingProgress.record(with(b(107),a),b(112),"2026-10-26",20,"2026-10-26T05:00:00Z");assertEquals(2,next.length());assertEquals(107,next.getJSONObject(1).getInt("start"));
    }
    @Test public void changingGoalDoesNotChangePastDaysOrOtherProfiles() throws Exception {
        JSONArray a=ReadingProgress.record(b(87),b(107),date,20,time);
        a=ReadingProgress.record(with(b(107),a),b(112),"2026-10-26",20,"2026-10-26T05:00:00Z");
        JSONArray n=ReadingProgress.goalForToday(a,"Güleser","2026-10-26",10,"2026-10-26T06:00:00Z");assertEquals(20,n.getJSONObject(0).getInt("goal"));assertEquals(10,n.getJSONObject(1).getInt("goal"));
        assertEquals(ReadingProgress.key(n),ReadingProgress.key(ReadingProgress.goalForToday(n,"Elis","2026-10-26",50,time)));
    }
    @Test public void notebookRoundTripsAndMissingColumnKeepsPhoneHistory() throws Exception {
        JSONObject book=with(b(97),ReadingProgress.record(b(87),b(97),date,20,time));
        JSONObject props=NotionCodec.toProperties(book),page=new JSONObject().put("id","remote").put("last_edited_time",time).put("properties",props);
        JSONObject remote=NotionCodec.fromPage(page);assertEquals(ReadingProgress.key(book.getJSONArray("readingDays")),ReadingProgress.key(remote.getJSONArray("readingDays")));
        props.remove(ReadingProgress.COLUMN);remote=NotionCodec.fromPage(page);assertFalse(remote.has("readingDays"));assertEquals(1,SyncMerge.synced(book,remote).getJSONArray("readingDays").length());
    }
    @Test public void clearedOrOlderRemoteHistoryCannotEraseLocalDays() throws Exception {
        JSONObject local=with(b(97),ReadingProgress.record(b(87),b(97),date,20,time)).put("syncStatus","synced"),remote=Book.copy(local).put("readingDays",new JSONArray());
        JSONObject merged=SyncMerge.merge(local,remote);assertEquals(1,merged.getJSONArray("readingDays").length());assertEquals("pending",merged.getString("syncStatus"));
    }
    @Test public void mergeDeduplicatesAndKeepsLatestRecordedDay() throws Exception {
        JSONArray a=ReadingProgress.record(b(87),b(97),date,20,time),n=ReadingProgress.record(with(b(97),a),b(107),date,20,"2026-10-25T18:00:00Z");
        JSONArray r=ReadingProgress.merge(a,n);assertEquals(1,r.length());assertEquals(87,r.getJSONObject(0).getInt("start"));assertEquals(107,r.getJSONObject(0).getInt("end"));
    }
    @Test public void malformedOrNegativeHistoryIsRejected() throws Exception {
        JSONArray a=ReadingProgress.record(b(87),b(97),date,20,time);a.getJSONObject(0).put("start",-1);assertThrows(IllegalArgumentException.class,()->ReadingProgress.normalize(a));
    }
    @Test public void automaticPositionChangesDoNotLeakIntoTheNextReadingUpdate() throws Exception {
        JSONArray rows=ReadingProgress.record(b(87),b(97),date,20,time);
        JSONObject done=with(b(208),rows);rows=ReadingProgress.rebase(with(b(97),rows),done,date,"2026-10-25T18:00:00Z");
        assertEquals(10,rows.getJSONObject(0).getInt("end")-rows.getJSONObject(0).getInt("start"));
        rows=ReadingProgress.merge(ReadingProgress.record(b(87),b(97),date,20,time),rows);assertEquals(198,rows.getJSONObject(0).getInt("start"));
        rows=ReadingProgress.record(with(b(208),rows),b(200),date,20,"2026-10-25T19:00:00Z");assertEquals(2,rows.getJSONObject(0).getInt("end")-rows.getJSONObject(0).getInt("start"));
    }
}
