package com.pardal.kitapligim;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReadingPlanTest {
    private JSONObject book() throws Exception{return Book.normalize(new JSONObject().put("id","plan-1").put("title","Bir kitap").put("profile","Güleser").put("status","want").put("syncStatus","synced").put("remoteEditedAt","old"));}
    @Test public void yearIsOptionalAndSurvivesSavedJson()throws Exception{
        assertFalse(book().has("plannedYear"));JSONObject b=Book.normalize(book().put("plannedYear",2027));
        assertEquals(2027,Book.normalize(new JSONObject(b.toString())).getInt("plannedYear"));
    }
    @Test public void invalidAndFractionalYearsAreRejected()throws Exception{
        for(Object year:new Object[]{1899,10000,2027.5,"wrong",true})assertThrows(IllegalArgumentException.class,()->Book.normalize(book().put("plannedYear",year)));
    }
    @Test public void remoteRefreshKeepsPlanAndAcceptsOtherRemoteChanges()throws Exception{
        JSONObject local=book().put("plannedYear",2027),remote=book().put("title","Yeni başlık").put("remoteEditedAt","new");
        JSONObject merged=SyncMerge.merge(local,remote);assertEquals(2027,merged.getInt("plannedYear"));assertEquals("Yeni başlık",merged.getString("title"));
    }
    @Test public void clearingPlanDoesNotDeleteBookOrReturnAfterSync()throws Exception{
        JSONObject local=book().put("plannedYear",JSONObject.NULL),remote=book().put("remoteEditedAt","new");
        JSONObject merged=SyncMerge.merge(local,remote);assertTrue(merged.isNull("plannedYear"));assertFalse(merged.optBoolean("deleted"));assertEquals("want",merged.getString("status"));
    }
    @Test public void choosingRemoteConflictKeepsDeviceReadingPlan()throws Exception{
        JSONObject local=book().put("plannedYear",2027).put("notes","Telefon").put("syncStatus","pending"),remote=book().put("notes","Notion").put("remoteEditedAt","new").put("remotePageId","r");
        JSONObject resolved=SyncMerge.resolve(SyncMerge.merge(local,remote),true);assertEquals(2027,resolved.getInt("plannedYear"));assertEquals("Notion",resolved.getString("notes"));
    }
}
