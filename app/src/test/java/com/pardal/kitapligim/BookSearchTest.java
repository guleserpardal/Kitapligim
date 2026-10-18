package com.pardal.kitapligim;
import org.junit.Test;
import org.json.*;
import java.nio.file.*;
import static org.junit.Assert.*;
public class BookSearchTest {
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
