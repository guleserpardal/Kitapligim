package com.pardal.kitapligim;
import org.junit.Test;
import org.json.*;
import java.nio.file.*;
import static org.junit.Assert.*;
public class BookSearchTest {
 private String shopHtml() throws Exception{return new String(Files.readAllBytes(Paths.get("../server/test/fixtures/trendyol-books.html")),java.nio.charset.StandardCharsets.UTF_8);}
 @Test public void trendyolBookSearchUsesDisplayedMetadataAndIgnoresRecommendations() throws Exception {
  JSONArray books=BookSearch.trendyol(shopHtml(),"aranan kitap");assertEquals(1,books.length());JSONObject b=books.getJSONObject(0);
  assertEquals("Aranan Kitap / Örnek Yazar",b.getString("title"));assertEquals("",b.getString("author"));assertEquals("",b.getString("isbn"));assertEquals(0,b.getInt("totalPages"));
  assertEquals("https://www.trendyol.com/yayinevi/aranan-kitap-p-100?merchantId=1&x=2",b.getString("buyUrl"));assertEquals("https://cdn.dsmcdn.com/cover.jpg",b.getString("cover"));
  assertEquals("Aranan Kitap 5 Kitap Seti",BookSearch.trendyol(shopHtml(),"aranan kitap seti").getJSONObject(0).getString("title"));
 }
 @Test public void trendyolStillFindsBookWhenOtherCatalogsFail() throws Exception {
  BookSearch search=new BookSearch(url->{if(url.contains("trendyol.com/sr?wc=91"))return shopHtml();throw new java.io.IOException();},new JSONArray());
  assertEquals("Aranan Kitap / Örnek Yazar",search.search("aranan kitap").getJSONObject(0).getString("title"));
 }
 @Test public void pastedIsbnWithHyphensUsesSameCatalogRecord() throws Exception {
  BookSearch search=new BookSearch(url->{throw new java.io.IOException();},catalog());
  assertEquals("Hizmetçi",search.search("ISBN: 978-625-6411-30-2").getJSONObject(0).getString("title"));
 }
 @Test public void googleRetriesEmptyLanguageFilterButNotQuotaErrors() throws Exception {
  java.util.concurrent.atomic.AtomicInteger requests=new java.util.concurrent.atomic.AtomicInteger();
  BookSearch search=new BookSearch(url->{if(!url.contains("googleapis"))throw new java.io.IOException();requests.incrementAndGet();return url.contains("langRestrict")?"{\"items\":[]}":"{\"items\":[{\"volumeInfo\":{\"title\":\"Aranan Kitap\"}}]}";},new JSONArray());
  assertEquals("Aranan Kitap",search.search("aranan kitap").getJSONObject(0).getString("title"));assertEquals(2,requests.get());requests.set(0);
  BookSearch outage=new BookSearch(url->{if(url.contains("googleapis"))requests.incrementAndGet();throw new java.io.IOException("429");},new JSONArray());
  assertThrows(java.io.IOException.class,()->outage.search("aranan kitap"));assertEquals(1,requests.get());
 }
 private JSONArray catalog() throws Exception{return new JSONArray(new String(Files.readAllBytes(Paths.get("src/main/assets/catalog-tr.json")),java.nio.charset.StandardCharsets.UTF_8));}
 @Test public void hizmetciFoundWithTurkishOrAsciiQueryDuringOutage() throws Exception {
  BookSearch search=new BookSearch(url->{throw new java.io.IOException("unavailable");},catalog());
  for(String query:new String[]{"hizmetçi","hizmetci","HİZMETÇİ","9786256411302","hizmetci freida"}){
   JSONArray results=search.search(query);boolean found=false;for(int i=0;i<results.length();i++){JSONObject b=results.getJSONObject(i);if(b.optString("isbn").equals("9786256411302")){found=true;assertEquals("Hizmetçi",b.getString("title"));assertEquals("Freida McFadden",b.getString("author"));assertEquals(352,b.getInt("totalPages"));}}
   assertTrue(query,found);
  }
 }
 @Test public void authorSearchFindsVerifiedTurkishTitles() throws Exception {BookSearch search=new BookSearch(url->{throw new java.io.IOException();},catalog());assertTrue(search.search("Freida").length()>=15);}
 @Test public void exactTitleIsFirst() throws Exception {BookSearch search=new BookSearch(url->{throw new java.io.IOException();},catalog());assertEquals("Hizmetçi",search.search("hizmetci").getJSONObject(0).getString("title"));}
 @Test public void sourceFailureDoesNotDiscardSuccessfulSource() throws Exception {
  BookSearch search=new BookSearch(url->{if(url.contains("googleapis"))return "{\"items\":[{\"volumeInfo\":{\"title\":\"Aranan kitap\",\"authors\":[\"Bir yazar\"]}}]}";throw new java.io.IOException();},new JSONArray());
  assertEquals("Aranan kitap",search.search("aranan").getJSONObject(0).getString("title"));
 }
 @Test public void unavailableSourcesAreNotMisreportedAsNoMatchingBook() throws Exception {BookSearch search=new BookSearch(url->{throw new java.io.IOException();},new JSONArray());assertThrows(java.io.IOException.class,()->search.search("unknown"));}
 @Test public void publisherCardsUseDecodedTextAndRealMetadata() throws Exception {
  String html="<div class='Product' data-prd-barcode='9786256411302'><div class='Product-content'><div class='name'><a href='/hizmetci'>Hizmetçi</a></div><div class='writer'><a>Freida McFadden</a></div></div><img class='prd_img' src='/i/1x1.png' data-src='https://example.com/cover.webp'></div>";
  JSONObject book=BookSearch.publisher(html).getJSONObject(0);assertEquals("Hizmetçi",book.getString("title"));assertEquals("9786256411302",book.getString("isbn"));assertEquals("https://example.com/cover.webp",book.getString("cover"));assertEquals("https://www.olimposdukkan.com/hizmetci",book.getString("sourceUrl"));
 }
 @Test public void turkishEditionMetadataIsUsedInsteadOfUnrelatedWorkIsbn() throws Exception {
  JSONObject response=new JSONObject("{\"docs\":[{\"title\":\"Original title\",\"author_name\":[\"Yazar\"],\"isbn\":[\"0000000000\"],\"editions\":{\"docs\":[{\"title\":\"Türkçe Başlık\",\"language\":[\"tur\"],\"isbn\":[\"9786256411302\"]}]}}]}");
  JSONObject book=BookSearch.openLibrary(response,"başlık").getJSONObject(0);assertEquals("Türkçe Başlık",book.getString("title"));assertEquals("9786256411302",book.getString("isbn"));
 }
}
