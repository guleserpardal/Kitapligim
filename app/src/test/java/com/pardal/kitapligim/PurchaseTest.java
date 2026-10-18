package com.pardal.kitapligim;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class PurchaseTest {
 private JSONObject book() throws Exception {return Book.normalize(new JSONObject().put("id","b1").put("profile","Güleser").put("title","Hizmetçi").put("status","want").put("syncStatus","synced").put("remotePageId","r1").put("remoteEditedAt","2026-10-18T10:00:00Z"));}
 private JSONObject page(JSONObject props) throws Exception {return new JSONObject().put("id","r1").put("last_edited_time","2026-10-18T10:00:00Z").put("properties",props);}
 @Test public void liraUsesTurkishFormat() throws Exception {assertEquals("231,00 TL",BookSearch.lira("231.00"));assertEquals("1.250,50 TL",BookSearch.lira("1250.5"));assertEquals("",BookSearch.lira("ücretsiz"));assertEquals("",BookSearch.lira("0"));}
 @Test public void publisherCardCarriesSalePriceAndLink() throws Exception {
  String html="<div class='Product' data-prd-barcode='9786256411302'><div class='Product-content'><div class='name'><a href='https://www.olimposdukkan.com/hizmetci'>Hizmetçi</a></div><div class='writer'><a>Freida McFadden</a></div><div class='price_box'><span class='price price_list' data-price='420.00'>420</span><span class='price price_sale' data-price='231.00'>231</span></div></div></div>";
  JSONObject b=BookSearch.publisher(html).getJSONObject(0);assertEquals("231,00 TL",b.getString("price"));assertEquals("Olimpos",b.getString("store"));assertEquals("https://www.olimposdukkan.com/hizmetci",b.getString("buyUrl"));
 }
 @Test public void googleSalePriceIsUsedOnlyWhenForSale() throws Exception {
  JSONObject r=new JSONObject("{\"items\":[{\"volumeInfo\":{\"title\":\"A\"},\"saleInfo\":{\"saleability\":\"FOR_SALE\",\"retailPrice\":{\"amount\":99.9,\"currencyCode\":\"TRY\"},\"buyLink\":\"https://play.google.com/store/books/details?id=x\"}},{\"volumeInfo\":{\"title\":\"B\"},\"saleInfo\":{\"saleability\":\"NOT_FOR_SALE\"}}]}");
  JSONArray a=BookSearch.google(r);assertEquals("99,90 TL",a.getJSONObject(0).getString("price"));assertFalse(a.getJSONObject(1).has("price"));
 }
 @Test public void drProductPageGivesPriceAndCanonicalLink() throws Exception {
  String html="<html><head><link rel='canonical' href='https://www.dr.com.tr/kitap/hizmetci/urunno=0002042091001'/><script type='application/ld+json'>[{\"@type\":[\"Product\",\"Book\"],\"gtin13\":\"9786256411302\",\"description\":\"ilk satır\nikinci satır\",\"offers\":{\"@type\":\"Offer\",\"availability\":\"http://schema.org/InStock\",\"price\":\"231.0\",\"priceCurrency\":\"TRY\"}}]</script></head></html>";
  JSONObject o=BookSearch.dr(html,"9786256411302");assertEquals("231,00 TL",o.getString("price"));assertEquals("D&R",o.getString("store"));assertEquals("https://www.dr.com.tr/kitap/hizmetci/urunno=0002042091001",o.getString("buyUrl"));
  assertNull(BookSearch.dr(html,"9780000000000"));
 }
 @Test public void offerFallsBackToStoreSearchWhenNothingMatches() throws Exception {
  JSONObject o=new BookSearch(url->"<html></html>",new JSONArray()).offer("9786256411302","Hizmetçi","Freida McFadden");
  assertTrue(o.getBoolean("searchOnly"));assertEquals("https://www.dr.com.tr/search?q=9786256411302",o.getString("buyUrl"));assertEquals("",o.getString("price"));
 }
 @Test public void rejectsNonHttpsPurchaseLink() throws Exception {JSONObject b=book().put("buyUrl","http://example.com");assertThrows(IllegalArgumentException.class,()->Book.normalize(b));}
 @Test public void purchaseFieldsRoundTripThroughNotion() throws Exception {
  JSONObject b=book().put("price","231,00 TL").put("store","D&R").put("buyUrl","https://www.dr.com.tr/kitap/hizmetci");
  JSONObject back=NotionCodec.fromPage(page(NotionCodec.toProperties(Book.normalize(b))));
  assertEquals("231,00 TL",back.getString("price"));assertEquals("D&R",back.getString("store"));assertEquals("https://www.dr.com.tr/kitap/hizmetci",back.getString("buyUrl"));
 }
 @Test public void emptyNotionLinkStaysEmpty() throws Exception {JSONObject back=NotionCodec.fromPage(page(NotionCodec.toProperties(book())));assertEquals("",back.getString("buyUrl"));assertEquals("",back.getString("cover"));}
 @Test public void tableWithoutPurchaseColumnsKeepsPhoneValues() throws Exception {
  JSONObject local=Book.normalize(book().put("price","231,00 TL").put("store","D&R").put("buyUrl","https://www.dr.com.tr/x"));
  JSONObject props=NotionCodec.toProperties(local);for(String c:NotionCodec.OPTIONAL_COLUMNS)props.remove(c);
  JSONObject remote=NotionCodec.fromPage(page(props));assertFalse(remote.has("buyUrl"));assertTrue(Book.same(local,remote));
  JSONObject merged=SyncMerge.merge(local,remote);assertEquals("https://www.dr.com.tr/x",merged.getString("buyUrl"));assertEquals("231,00 TL",merged.getString("price"));
 }
 @Test public void changedPriceInNotionIsNotSame() throws Exception {JSONObject a=book().put("price","231,00 TL"),b=book().put("price","250,00 TL");assertFalse(Book.same(a,b));}
}
