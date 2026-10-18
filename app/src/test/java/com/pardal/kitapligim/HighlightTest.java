package com.pardal.kitapligim;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class HighlightTest {
 private JSONObject book() throws Exception {return Book.normalize(new JSONObject().put("id","b1").put("profile","Güleser").put("title","Hizmetçi").put("status","reading").put("syncStatus","synced").put("remotePageId","r1").put("remoteEditedAt","2026-10-18T10:00:00Z"));}
 private JSONObject page(JSONObject props) throws Exception {return new JSONObject().put("id","r1").put("last_edited_time","2026-10-18T10:00:00Z").put("properties",props);}
 private JSONArray list() throws Exception {return new JSONArray().put(new JSONObject().put("id","a").put("text","Herkesin bir sırrı vardır.").put("page",124).put("note","Nina hakkında").put("createdAt","2026-10-18T18:00:00Z")).put(new JSONObject().put("id","b").put("text","İkinci satır\nve devamı").put("page",0).put("note",""));}
 @Test public void roundTripsThroughNotion() throws Exception {
  JSONObject b=Book.normalize(book().put("highlights",list()));
  String text=NotionCodec.highlightText(b.getJSONArray("highlights"));
  assertEquals("“Herkesin bir sırrı vardır.”\n— s. 124\nNot: Nina hakkında\n\n“İkinci satır\nve devamı”",text);
  JSONObject back=NotionCodec.fromPage(page(NotionCodec.toProperties(b)));
  JSONArray h=back.getJSONArray("highlights");assertEquals(2,h.length());
  assertEquals("Herkesin bir sırrı vardır.",h.getJSONObject(0).getString("text"));assertEquals(124,h.getJSONObject(0).getInt("page"));assertEquals("Nina hakkında",h.getJSONObject(0).getString("note"));
  assertEquals("İkinci satır\nve devamı",h.getJSONObject(1).getString("text"));
  assertTrue(Book.same(b,back));
 }
 @Test public void passageTypedInNotionWithoutFormatting() throws Exception {
  JSONArray h=NotionCodec.parseHighlights("Sade bir cümle\n\n\n“Tırnaklı” \n— s. 7");
  assertEquals(2,h.length());assertEquals("Sade bir cümle",h.getJSONObject(0).getString("text"));assertEquals(7,h.getJSONObject(1).getInt("page"));assertEquals("Tırnaklı",h.getJSONObject(1).getString("text"));
 }
 @Test public void blankLinesInsideAPassageAreFoldedSoTheyCannotSplitIt() throws Exception {
  JSONObject b=Book.normalize(book().put("highlights",new JSONArray().put(new JSONObject().put("text","bir\n\n\niki"))));
  assertEquals("bir\niki",b.getJSONArray("highlights").getJSONObject(0).getString("text"));
 }
 @Test public void emptyPassagesAreDroppedAndBadPagesRejected() throws Exception {
  assertEquals(0,Book.normalize(book().put("highlights",new JSONArray().put(new JSONObject().put("text","  ")))).getJSONArray("highlights").length());
  JSONObject bad=book().put("highlights",new JSONArray().put(new JSONObject().put("text","x").put("page",-1)));
  assertThrows(IllegalArgumentException.class,()->Book.normalize(bad));
 }
 @Test public void syncKeepsPhoneIdsAndPhoneListWhenColumnMissing() throws Exception {
  JSONObject local=Book.normalize(book().put("highlights",list()));
  JSONObject remote=NotionCodec.fromPage(page(NotionCodec.toProperties(local)));
  assertEquals("a",SyncMerge.merge(local,remote).getJSONArray("highlights").getJSONObject(0).getString("id"));
  JSONObject props=NotionCodec.toProperties(local);props.remove("Altı Çizilenler");
  JSONObject noColumn=NotionCodec.fromPage(page(props));assertFalse(noColumn.has("highlights"));
  assertEquals(2,SyncMerge.merge(local,noColumn).getJSONArray("highlights").length());
 }
 @Test public void newPassageMakesBooksDifferent() throws Exception {
  JSONObject a=Book.normalize(book().put("highlights",list())),b=Book.normalize(book().put("highlights",new JSONArray().put(list().get(0))));
  assertFalse(Book.same(a,b));
 }
}
