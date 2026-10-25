# Geliştirme ve yeniden derleme

Bu kaynak paketi Android Studio'da açılabilen bağımsız bir Gradle projesidir. Kaynak deposu: https://github.com/guleserpardal/Kitapligim.

- Java 17 JDK, Android SDK Platform 35, Build Tools 35.0.0.
- Gradle Wrapper 8.9, Android Gradle Plugin 8.7.3.
- En düşük Android sürümü: 11 / API 30. Hedef API 35.
- ABI'ler: arm64-v8a (teslim edilen telefon APK'sı), x86_64 (emülatör).
- Uygulama kimliği: `com.pardal.kitapligim`, sürüm: `1.4.0` / kod `6`.
- Yerel HTML/CSS/JavaScript ekranları Android WebViewAssetLoader ile güvenli yerel HTTPS kökeninden sunulur. Ağ araması, OCR, kamera, disk, Android Keystore ve Notion bağlantısı Java katmanındadır. Web sitesi veya kurulum bağlantısı gerektirmez.
- Kitaplar `AtomicFile` ile uygulamanın özel alanına yazılır. Tüm yerel yazmalar ve eşitleme tek iş parçacıklı yürütücüden geçer.
- Notion REST sürümü: 2025-09-03; tek aile veri kaynağı: `a77420e6-f540-49a2-84c0-45c3bffa711a`.
- Kayıt kimliği, uzak sayfa kimliği, son Notion düzenleme zamanı ve bekleyen/çakışan/eşitlenmiş durumları saklanır. Uzak fotoğrafların süreli bağlantıları özel yerel kopyalara dönüştürülür.
- Uygulama Notion'da kullanıcının eklediği sayfa içerik bloklarını değiştirmez; yalnız kitap alanlarını ve gerektiğinde kapağı günceller.

## Komutlar

Android Studio SDK konumunu otomatik bulabilir. Gerekirse proje kökünde `local.properties` oluşturup `sdk.dir=/tam/yol/android-sdk` yaz.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

ARM64 geliştirme APK'sı: `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`.

İmza dosyaları olmadan `assembleRelease` imzasız APK üretir; bu dosya doğrudan telefona kurulamaz. Kişisel imza dosyalarını yerelde ekledikten sonra `./gradlew :app:assembleRelease` ile imzalı güncelleme üretilebilir.

Test sınıfı: `app/src/test/java/com/pardal/kitapligim/SyncRulesTest.java`.

## İmza ve güncellemeler

GitHub için hazırlanan bu pakette `signing/` klasörü yoktur; `.gitignore` tarafından da dışlanır. İlk teslim edilen özel kaynak yedeğindeki `signing/kitapligim.jks` ve `signing/signing.properties` dosyalarını bilgisayarında bu projenin `signing/` klasörüne kopyala. Bu iki dosyayı özel yedeğinde sakla; GitHub'a yükleme. Güncellemelerde aynı imzayı kullanıp `versionCode` değerini yükselt; böylece uygulamayı kaldırmadan yüklenebilir ve telefondaki kayıtlar korunur. Teslim edilen release APK'da hata ayıklama kapalıdır. Debug sürümü ayrı geliştirme sertifikası kullanır; mevcut release uygulamasının üzerine güncelleme olarak kurulamaz. Mevcut kayıtları korumak için aynı kişisel imzayla release üret.

Kaynak pakette Notion anahtarı, başka bir kullanıcı sırrı veya gerçek kitap verisi yoktur. İnternet katalog araması için kullanıcıdan anahtar istenmez. API erişimleri sağlayıcı kotasına tabidir.

## Bilinen işleyiş

- Notion bağlantısı cihaz başına kurulur. Aynı aile tablosuna bağlanan başka telefonlar da kendi kurulumlarıyla eşitlenebilir; ayrı Notion erişimleri tanımlamak gerekir.
- Notion'da `Profil` boş veya tanınmayan bir kayıt varsa sessizce bir kişiye atanmaz; hata gösterilir ve alanın düzeltilmesi istenir.
- Okuma içeriği indirme, kitap PDF'si veya mağazadan satın alma sunulmaz. Öneriler yalnızca kullanıcı Kitap Danışmanı'na yazdığında gelir; kitaplığa otomatik eklenmez.
- Kitap Danışmanı (1.1.0) için ücretli bir yapay zekâ servisi yoktur; ailenin kendi PC'sinde çalışan `server/` klasöründeki sunucu, o PC'de oturum açık Claude Code'u kullanır (kurulum: `server/README.md`). Sunucu yokken uygulamanın geri kalanı çalışır. Fotoğraflar kullanıcı Notion'a bağlayana kadar cihazda tutulur.

## 1.0.1 arama ve ekran düzeltmeleri

WindowInsets artık WebView yerine dış FrameLayout üzerinde uygulanır; CSS sabit alt elemanlar Android gezinme çubuğuna taşmaz. Görsel pencere yüksekliği klavye açılışında yenilenir ve modal alt eylem alanı küçülmez. BookSearch, üç kaynağı bağımsız paralel sorgular; bir servis kesintisi diğer sonuçları düşürmez. Yayınevi HTML kartlarının yalnız başlık, yazar, ISBN ve görsel metaverileri jsoup ile okunur. Doğrulanmış 18 kayıt assets/catalog-tr.json içinde bulunur; fiyat, kitap metni veya kullanıcının okuma kaydı içermez. Kaynak: https://www.olimposdukkan.com/freida-mcfadden (2026-10-18). Yayınevi sayfa yapısı değişirse bu kaynağın ayrıştırıcısı da güncellenmelidir.

## 1.1.0 Kitap Danışmanı ve yeni tasarım

- Ara ekranındaki Danışman sekmesi, `Assistant.java` üzerinden PC sunucusuna (`POST /api/chat`) bağlanır. Sunucu adresi, yedek (Tailscale) adresi ve `APP_TOKEN` anahtarı Android Keystore ile şifreli saklanır; arayüze anahtar hiç gönderilmez. Önce ev adresi, ulaşılamazsa yedek adres denenir.
- Şifresiz HTTP yalnızca `res/xml/network_security_config.xml` içindeki iki adrese (192.168.1.12, 100.68.128.112) izinlidir. Sunucunun adresi değişirse bu dosyaya eklenip yeni APK derlenmelidir.
- Sohbet geçmişi telefonda (WebView yerel depolaması, profil başına son 40 mesaj) tutulur. Sunucuya yalnızca son 12 mesaj, profil adı ve kitaplık listesi (ad, yazar, raf, puan) gider.
- Sunucu testleri: `cd server && npm test`. İstemci testi: `AssistantTest`.
- Arayüz yeniden tasarlandı: dört sekme (Kitaplığım, Ara, Hedefler, Ayarlar), raf süzgeçleri ana ekranda, tam sayfa Kitap detayı. Profil avatarları şimdilik baş harflidir.

## 1.3.0 ana sayfa ve arayüz kontrolü

Dört raf aynı ana sayfada ayrı bölümlerle gösterilir. `Tümünü gör`, rafın tam listesini ve kitap/yazar aramasını açar. Ana sayfa her rafın en çok altı kapağını ve en son güncellenen okunmakta olan kitabı gösterir; diğer kayıtlar tam raf listesinden erişilir. Filtre düğmeleri veya raf açılır listesi kullanılmaz.

`library-corner.svg` yerel dekoratif görseldir; kişisel veri veya ağ isteği içermez. Ekleme menüsü beş mevcut akışa yönlendirir, Escape/Android geri ile kapanır. Notion ve kitap veri modeli değiştirilmemiştir.

Arayüz akışlarını yerelde kontrol etmek için Node 18+ ve Python 3 gerekir:

```sh
npm ci
npx playwright install chromium
npm run test:ui
```

Test yerel bir HTTP sunucusu açıp kapatır; örnek verileri yalnız tarayıcının test bağlamında tutar, gerçek Notion/kamera servisini çağırmaz. Dört rafı, listelerin tamamını, aramayı, profil ayrımını, kaydedilen notu, ara verilen kitabın ilerlemesini, beş ekleme seçeneğini, geri tuşunu ve dar ekranları kontrol eder. `test-results/` görselleri Git dışında tutulur. Farklı bir Chromium ikilisi için `PLAYWRIGHT_CHROMIUM_EXECUTABLE` ortam değişkeni kullanılabilir.

## 1.4.0 günlük kayıt modeli

`ReadingProgress` telefonun yerel gününde, kitap/profil/tarih başına tek düzeltilebilir kayıt tutar. İlk kaydın `start` değeri önceki sayfadır; `end` son kaydedilen sayfadır. Günlük katkı `max(0, end-start)` değeridir. `_recordReading` yalnız açık sayfa düzenlemelerinde kullanılır ve saklanmaz. Yeni kitap/eski ilerleme başlangıç sayılır. Raf/Notion kaynaklı otomatik konum değişimi varsa o günün başlangıcı yeniden hizalanır; sonraki güncellemede aradaki otomatik fark okuma sayılmaz.

Kayıt tarihleri kaydedildikleri yerel gün olarak korunur; takvim gün aritmetiği UTC öğlenle yapılır. Aynı gün tekrar kaydetme, sayfa düzeltme, gün değişimi, profil aktarımı ve silinen kitapların geçmişi testlidir. Bir kitabın günlük kaydı iki kaynaktan gelirse tarih/profil anahtarıyla tekilleştirilir ve son düzenlenen kayıt korunur. Çevrimdışı iki cihazın aynı kitabı aynı gün eşzamanlı düzenlemesinde son kayıt esas alınır; bu kayıtlar toplanarak iki kez sayılmaz.

`dailyGoals` cihazda profil bazlıdır; günlük kayıttaki `goal` o günün hedefini taşır. Bugünkü hedef düzenlenebilir; geçmiş satırların hedefi değiştirilmez. Okuma günlüğü `Okuma Günlüğü` adlı rich_text sütununda sürümlenebilir kompakt JSON satırları olarak tutulur (tarih, profil indeksi, başlangıç, bitiş, hedef, düzenleme zamanı). Mevcut sayfa bloklarına ve eski alanlara dokunulmaz. Yeni sütun eksikse yalnız geçmiş olduğunda eklenir; yetki hatası normal kitap senkronunu durdurmadan görünür uyarı olur. Boş/eski bir Notion günlüğü telefondaki günleri silemez.

```sh
npm test                 # sayfa hesabı, zincir, takvim tarihi ve hedef kuralları
npm run test:ui         # ana sayfa ve günlük hedef/takvim akışları
```

`ReadingProgressTest` Notion dönüşümünü ve günlük kayıt birleştirmesini de test eder. Notion'un canlı hesabı, cihaz kamerası ve Android kurulum ekranı bu tarayıcı testlerinin kapsamında değildir.
