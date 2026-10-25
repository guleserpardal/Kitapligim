# Kitaplığım sunucusu

## 1.4.1 arama düzeltmesi — mevcut kurulumu güncelleme

PC'deki sunucu ayrı çalışır; yalnız yeni APK'yı yüklemek danışman sunucusunun kodunu değiştirmez. Mevcut `server` klasörünün bir kopyasını al. `Kitapligim-Danisman-1.4.1.zip` içindeki `server` klasörünün dosyalarını mevcut klasörün üzerine kopyala; kendi `.env` dosyanı koru. Windows Görev Zamanlayıcı'da **Kitapligim Sunucu** görevini durdurup yeniden başlat. Sonra telefonda Ayarlar → Kitap Danışmanı → Bağlantıyı dene. Adres/anahtar değişmez. Yeni bir bağımlılık veya API anahtarı gerekmez.

Doğrulama sırası: uygulamayla aynı yayınevi kataloğu, Google Books, Trendyol'un kitap kategorisindeki açık arama kartları, Open Library. Google'ın alan operatörleri yerine düz başlık/yazar sorgusu kullanılır; boş dil filtresi sonucu filtresiz yeniden denenir, kota hatası aynı kaynağa tekrar gönderilmez. Eşleştirme başlık ve varsa yazarı birlikte kontrol eder; serinin başka kitabı eşleşme sayılmaz. Trendyol'da marka/yayınevi yazar olarak alınmaz; sayfa aralığından kesin sayfa sayısı, ürün numarasından ISBN üretilmez. Eksik bilgiler boş kalır. Open Library'de ISBN/sayfa yalnız eşleşen Türkçe baskıdan gelir.

`data/catalog-tr.json`, Android'deki `app/src/main/assets/catalog-tr.json` dosyasının yayınevi verisi kopyasıdır; katalog güncellenince ikisini birlikte güncelle (depo testi eşitliği denetler). Bu dosya `server` klasörünü tek başına kopyalayarak kurulumu destekler.

Danışmanın Claude görüşmesi araçsızdır; yeni/hatırlamadığı bir kitabı kendiliğinden internette keşfetmez. Sonraki katalog kontrolü yalnız üretilen adayları doğrular. Satış sayfalarının erişimi veya yapısı değişebilir; engel/CAPTCHA aşılmaz. `verified:false`, kitabın olmadığı anlamına gelmez. Uygulama aynı sorguyu Trendyol'da açma seçeneği gösterir.

Telefondaki Kitaplığım uygulamasının **Kitap Danışmanı** özelliği bu sunucuya bağlanır. Sunucu, bu PC'de oturum açık olan Claude Code CLI ile (izole, araçsız `claude -p --safe-mode`) konuşur. Ayrı bir API anahtarı gerekmez; kullanılan Claude hesabının kotasını harcar. Spor Hocam ile aynı düzendedir, farklı bir portta (8790) çalışır.

Telefona yalnızca sohbet metni, seçili profilin adı ve kitaplık listesi (ad, yazar, raf, puan) gönderilir. Notlar, alıntılar ve fotoğraflar gönderilmez. Sunucu mesaj içeriğini günlüğe yazmaz.

## Kurulum (Windows)

1. Bu `server` klasörünü PC'ye kopyala (ör. `C:\Users\ilker\dev\kitapligim\server`). Node 18+ ve oturum açılmış `claude` komutu gerekir (`claude -p "merhaba"` çalışmalı).
2. Yönetici PowerShell'de `powershell -ExecutionPolicy Bypass -File .\admin-kur.ps1`. Betik `.env` yoksa rastgele `APP_TOKEN` üretip oluşturur, güvenlik duvarında 8790 portunu yalnız yerel ağa ve Tailscale'e açar, oturum açılınca başlayan görevi kurar, sağlık kontrolü yapar ve telefona girilecek adres ile anahtarı ekrana yazar. Tekrar çalıştırmak güvenlidir.
3. Telefonda Kitaplığım → Ayarlar → Kitap Danışmanı bölümüne adresi (`http://192.168.1.12:8790`, evin dışında Tailscale adresi) ve ekrana yazılan anahtarı gir.

Elle denemek için: `.env.example`'ı `.env` yapıp `APP_TOKEN` doldur, `node src/server.js`, tarayıcıda `http://localhost:8790/api/health`.

### PC'deki Claude Code'a yaptırmak istersen

PC'de Claude Code'a şunu yapıştır: "guleserpardal/Kitapligim deposunun `ccr-adbd1460-c09byp` dalını (birleştirildiyse `main`) `C:\Users\ilker\dev\kitapligim` içine klonla, `server` klasöründe yönetici PowerShell ile `admin-kur.ps1` çalıştır, çıkan adres ve anahtarı bana göster."

Durdur/başlat: `Stop-ScheduledTask "Kitapligim Sunucu"` / `Start-ScheduledTask "Kitapligim Sunucu"`.

## Uç noktalar

- `GET /api/health`: anahtarsız, yalnız sunucunun ayakta olduğunu söyler.
- `POST /api/chat` (`X-Token` gerekir): `{messages:[{role,content}], profile, library:[{title,author,status,rating}]}` → `{reply, books:[{title,author,why,isbn,totalPages,cover,source,verified}]}`. `verified:false` kitap, erişilebilen kataloglarda doğrulanamamıştır; uygulama bunu kullanıcıya belirtir. Bir satış ilanı eşleşirse `buyUrl`, `store`, `sourceUrl` alanları da dönebilir.

## Test

`npm test` (sahte Claude ile çalışır, gerçek Claude çağırmaz).
