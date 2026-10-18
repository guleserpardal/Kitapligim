# Geliştirme ve yeniden derleme

Bu kaynak paketi Android Studio'da açılabilen bağımsız bir Gradle projesidir. Harici bir Git deposuna bağlı değildir.

- Java 17 JDK, Android SDK Platform 35, Build Tools 35.0.0.
- Gradle Wrapper 8.9, Android Gradle Plugin 8.7.3.
- En düşük Android sürümü: 11 / API 30. Hedef API 35.
- ABI'ler: arm64-v8a (teslim edilen telefon APK'sı), x86_64 (emülatör).
- Uygulama kimliği: `com.pardal.kitapligim`, sürüm: `1.0.1` / kod `2`.
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
- Kullanıcıya otomatik kitap önerisi, okuma içeriği indirme, kitap PDF'si veya mağazadan satın alma sunulmaz.
- Yapay zekâ servis ücreti ve uygulamaya ait sunucu yoktur. Fotoğraflar kullanıcı Notion'a bağlayana kadar cihazda tutulur.

## 1.0.1 arama ve ekran düzeltmeleri

WindowInsets artık WebView yerine dış FrameLayout üzerinde uygulanır; CSS sabit alt elemanlar Android gezinme çubuğuna taşmaz. Görsel pencere yüksekliği klavye açılışında yenilenir ve modal alt eylem alanı küçülmez. BookSearch, üç kaynağı bağımsız paralel sorgular; bir servis kesintisi diğer sonuçları düşürmez. Yayınevi HTML kartlarının yalnız başlık, yazar, ISBN ve görsel metaverileri jsoup ile okunur. Doğrulanmış 18 kayıt assets/catalog-tr.json içinde bulunur; fiyat, kitap metni veya kullanıcının okuma kaydı içermez. Kaynak: https://www.olimposdukkan.com/freida-mcfadden (2026-10-18). Yayınevi sayfa yapısı değişirse bu kaynağın ayrıştırıcısı da güncellenmelidir.
