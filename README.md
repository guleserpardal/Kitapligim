# Kitaplığım

Güleser, İlker, Elis ve Lila için krem ve zeytin tonlarında kişisel Android kitaplığı.

## GitHub ve geliştirme

Bu depo Kitaplığım uygulamasının kaynak kodunu içerir. GitHub'a yükleme adımları `GITHUB.md`, derleme ayrıntıları `GELISTIRME.md` dosyasındadır. İmza anahtarları ve parolalar pakete dahil edilmemiştir. Java 17 ve Android SDK 35 ile Android Studio'da açılabilir; imza anahtarı olmadan debug APK derlenebilir.

## 1.4.0 — Günlük okuma hedefi ve zincir

- Her profil için günlük sayfa hedefi. Ana sayfadaki **Bugünkü okuman** kartından hedefini seç; 10, 20, 30 sayfalık kısayollar veya kendi sayını kullan.
- Kitap detayında kaldığın sayfayı kaydettiğinde okuduğun fark güne eklenir. Birden fazla kitaptaki okumalar toplanır. Aynı sayfayı yeniden kaydetmek veya yalnız not düzenlemek tekrar sayılmaz.
- Geriye yapılan sayfa düzeltmesi bugünkü kaydı düzeltir. Önceki kayıtlı sayfalar ve yeni eklenen bir kitabın eski ilerlemesi bugüne yazılmaz. Sadece rafı “Okudum” yapmak otomatik okuma kredisi oluşturmaz.
- **Hedefler → Okuma zincirim**: aylık takvim, seçilen günün sayfası/hedefi, mevcut ve en uzun zincir. Yeşil gün hedefi tamamladığın, açık sarı gün biraz okuduğun gündür. Gelecek günler seçilemez.
- Zincir, günlük hedefin tamamlandığı ardışık günlerden oluşur. Bugün henüz bitmediyse dünkü zincir gün sonuna kadar görünür. Hedef değişikliği bugüne ve sonraki okumalara uygulanır; geçmiş hedefleri değiştirmez.
- Okuma günlüğü ve hedefler cihazda, internet olmadan kaydedilir. Bağlı Notion kitaplığında uygulama **Okuma Günlüğü** metin alanını ekleyip kitap başına günlük kayıtları eşitler. Alan ekleme yetkisi yoksa günlük telefonda korunur ve Ayarlar'da açık uyarı gösterilir. Normal kitap eşitlemesi devam eder. Gerçek hesapla canlı doğrulama telefonda yapılmalıdır.
- Takvim, uygulamanın sayfa kaydetme işlemlerini izler; Notion'da yalnız Kaldığım Sayfa alanını elle değiştirmek kendiliğinden okuma günü oluşturmaz. Günlük hedef cihazda profil bazında saklanır; diğer cihazda ilk hedef ayrıca seçilebilir, eşitlenen günlerdeki hedefler geçmişte korunur.
- `versionName 1.4.0`, `versionCode 6`. Önceki imzayla güncelleme olarak kurulabilir.

## 1.3.0 — Kendi kitap köşen

- Ana sayfa sıcak krem tonlarında, ahşap raf çizgileri ve kapaklarla yeniden düzenlendi. Okumaya devam et, Sıradaki kitaplar, Bitirdiğim kitaplar ve **Ara verdiklerim** aynı sayfada yer alır.
- Her bölümde **Tümünü gör** ile o kişinin rafındaki bütün kitaplar açılır; raf içinde kitap/yazar araması yapılabilir. Ana sayfa önizlemesinde gösterilmeyen kitaplar raf listesinden erişilebilir.
- Sağ alttaki **+** açılır menüsü: Kitap ara, Danışmana sor, Kapaktan bul, ISBN tara, Elle ekle. Telefonun geri tuşu ve Escape menüyü kapatır.
- Hedef ve ilerleme, seçili kişinin gerçek verilerinden gelir; örnek kitap eklenmez. Sayfa güncelleme, alıntılar ve Notion eşitlemesi mevcut kayıtlarla çalışmaya devam eder.
- `versionName 1.3.0`, `versionCode 5`. Önceki kişisel imzayla derlendiğinde uygulamayı kaldırmadan güncellenir.

## 1.2.0 güncellemesi

- **Fiyat ve satın alma linki:** Arama sonuçlarında kitabın güncel fiyatı görünür (Olimpos ve D&R; ISBN ile eşleşen baskı). Kitabı rafına eklerken fiyat, mağaza ve satın alma linki de kaydedilir; kitap detayındaki **Satın al** düğmesi bu sayfayı açar. Fiyat bulunamazsa D&R araması kaydedilir. Eski kitaplar için detaydaki **Fiyatını ve satın alma linkini bul** düğmesi kullanılabilir. Fiyat eklendiği günkü fiyattır. **Trendyol ve Hepsiburada** fiyatları da uygulamanın içinde, gizli bir tarayıcı penceresiyle mağazanın kendi arama sayfasından okunur (yapay zekâ kullanılmaz); en ucuz teklif kaydedilecek link olur, diğerleri fiyat kartının altında düğme olarak durur. **İdefix** arama sayfasının doğrudan açılmasını engellediği için yalnız arama düğmesi olarak bulunur.
- **Yeni kitap ekleme ekranı:** üstte arama kutusu ile Kapaktan bul / ISBN tara, kapaklı kitap kartı (kapağa fotoğraf eklenebilir), büyük profil yuvarlakları, dört raf kartı (Ara verdim dahil), sayfa alanları; tarihler, puan, not, fiyat ve link "Diğer bilgiler" altında.
- **Altını çiz:** Kitap detayındaki "Altını çizdiklerim" bölümüne istediğin kadar cümle eklenir; her birine sayfa ve not yazılabilir. Cümle elle yazılır ya da sayfanın fotoğrafı çekilip satırlara dokunarak seçilir (yazı cihazda okunur, fotoğraf saklanmaz). Notion'da **Altı Çizilenler** sütununda okunur biçimde durur.
- Notion'daki Aile Kitaplığımız tablosuna **Fiyat**, **Mağaza** ve **Satın Alma Linki** sütunları eklendi. Bu sütunlar olmayan bir tabloda eşitleme eskisi gibi çalışır, telefondaki fiyat silinmez.
- `versionCode 4`: aynı imzayla Güncelle'ye dokunarak yüklenir, kayıtların korunur.

## 1.1.0 güncellemesi

- **Kitap Danışmanı:** Ara ekranındaki Danışman sekmesinde sohbet ederek kitap bulur, aklında kalan bir sahneyi ya da ruh halini anlatarak öneri alırsın. PC'deki Claude ile çalışır; kurulum için `server/README.md`. Sunucu kapalıyken uygulamanın geri kalanı normal çalışır.
- **Yeni tasarım:** dört sekmeli alt menü, ana ekranda raflar, tam sayfa kitap detayı, adım düğmeli sayfa girişi.
- `versionCode 3`: aynı imzayla Güncelle'ye dokunarak yüklenir, kayıtların korunur.

## 1.0.1 güncellemesi

- Samsung/Android sistem gezinme çubuğu için boşluk, WebView'ın dışındaki yerleşimde uygulanır. Kaydet ve Vazgeç düğmeleri alt çubuğun üstünde kalır; formun içi kaydırılır. Klavye açıldığında pencereye yeniden uyum sağlar.
- Olimpos'un yayımladığı Türkçe kitap listesi aramaya eklendi. Freida McFadden'ın 18 Türkçe kitap kaydı küçük bir katalog olarak paketlendi; bunlar aile raflarına otomatik eklenmez. Hizmetçi'nin Türkçe baskısı 9786256411302 ISBN ve 352 sayfa bilgisiyle bulunur.
- Türkçe karakterli/karaktersiz arama, doğru Türkçe baskının seçimi, kaynaklar arasında sonuçların korunması ve ISBN ile yinelenen sonuçların ayıklanması geliştirildi.
- Bu sürüm versionCode 2'dir ve öncekiyle aynı kişisel imzayı kullanır. **Önceki uygulamayı kaldırmadan yeni APK'yı açıp Güncelle'ye dokun.** Telefonda kaydettiğin kitaplar korunur.

## Telefona kurulum

1. Güncel `Kitapligim-1.4.0.apk` dosyasını telefona indirip aç.
2. Android isterse dosyayı açtığın uygulama için “Bu kaynaktan yüklemeye izin ver” seçeneğini aç; ardından Yükle'ye dokun.
3. Kitaplığım'ı aç ve üstteki dört profilden birini seç.

Teslim edilen APK, Samsung Galaxy S24 Ultra gibi **ARM64, Android 11 ve üstü** telefonlar içindir. İnternet olmadan kitap ekleme, not ve sayfa güncelleme çalışır. Geniş katalog araması ve Notion eşitlemesi internet ister; paketlenen küçük Türkçe katalog kaynak kesintilerinde de kullanılabilir.

## Neler var?

- Dört ayrı aile profili ve her kişinin kendi kitapları, notları, puanları, ilerlemesi.
- Okuyacaklarım, Okuyorum, Okuduklarım ve Ara verdiklerim rafları.
- Başlama/bitiş tarihleri, sayfa sayısı, kaldığın sayfa, 1–5 yıldız, not ve alıntılar.
- Kitap, yazar veya ISBN ile Olimpos Türkçe yayınevi kataloğu, Google Books ve Open Library araması. Google Books anahtar olmadan kullanılır; kota veya erişim hatalarında Open Library devreye girer. Her Türkçe baskının bulunacağı garanti edilemez. Bulunamayan kitap elle eklenir.
- Kameradan ya da galeriden kapak ekleme. **Paketin içindeki ML Kit modeli** yazı ve ISBN barkodunu cihazda okur; fotoğrafı okumak için ücretli bir yapay zekâ servisi veya API anahtarı gerekmez. Bulunan metin kaydetmeden önce kontrol edilir.
- Yıllık okuma hedefi ve aile özeti. Yıllık sayı, bitiş tarihi ilgili yılda olan kitaplardan hesaplanır. Hedefi kullanıcı belirler; önceden atanmış bir hedef yoktur.
- JSON dışa aktarma: kitap bilgileri ve notlar dahil, fotoğraf dosyaları ve Notion anahtarı hariç.

## Notion bağlantısı — bir kez yapılacak

Hazırlanan kitaplık: **[Aile Kitaplığımız](https://app.notion.com/p/37a24f3eb21c4508ad5e5ea3c8c2e438)**.

Bu tablo Notion → Güleser'in Beyni → projects → proj-kitap-takip altında oluşturuldu. Kullanıcının gerçek kitapları henüz girilmedi; teslimatta örnek kitaplar bulunmaz.

1. [Notion bağlantı yönetimini](https://www.notion.so/profile/integrations) aç ve kendi çalışma alanın için bir **dahili bağlantı / internal connection** oluştur.
2. Okuma, içerik ekleme ve içerik güncelleme yetkilerini açık tut.
3. Aile Kitaplığımız sayfasını aç. Sağ üstteki `…` menüsünden **Bağlantılar / Connections** alanına girip yeni bağlantıyı ekle. Gerekirse üstteki proj-kitap-takip sayfasına da aynı bağlantıyı ekle.
4. Notion bağlantı ayarlarındaki gizli anahtarı kopyala.
5. Kitaplığım → **Ayarlar → Notion'u bağla** ekranına gir; anahtarı oraya yapıştır ve Bağlantıyı kur'a dokun.

**Gizli anahtarı sohbete göndermek gerekmez.** Anahtar uygulama paketinde bulunmaz; cihazdaki Android Keystore anahtarıyla AES-GCM kullanılarak şifrelenir. Bu sohbetin Notion bağlantısı telefon uygulamasına otomatik olarak aktarılmaz.

### İki yönde nasıl çalışır?

- Telefonda eklenen veya değişen kitaplar Notion'a gönderilir.
- Notion'da eklenen ve değişen satırlar telefona alınır. Notion'dan yeni kitap eklerken **Kitap, Profil ve Durum** alanlarını doldur; `Uygulama ID` alanını yeni satır için boş bırakabilirsin.
- Notion'dan fotoğraf eklemek için **Kapak Fotoğrafı** dosya alanına görsel ekle. Telefonda seçilen kapak fotoğrafı aynı alana yüklenir. İnternet kapakları `Kapak` URL alanında tutulur.
- Aynı kitabın kaydı iki tarafta da ayrı ayrı değiştiğinde otomatik olarak biri silinmez: Ayarlar'dan iki sürüm karşılaştırılır, kullanıcı hangi sürümün kalacağını seçer.
- Uygulamadan kaldırılan kitap, Notion'da `Silindi` olarak işaretlenir. Notion'da bunu kaldırmak kitabı geri getirir. Notion çöpüne atılmış bir kitap da bilinen sayfa kimliğiyle kontrol edilir.
- Aktarım bekleyen kayıtlar internet hatalarında telefonda kalır. Notion isteği belirsiz biçimde kesilirse sonraki turda Uygulama ID üzerinden yeniden bulunur; aynı kaydı yeniden oluşturmadan önce bütün satırlar sorgulanır.
- Uygulama açıldığında, kayıt değiştiğinde ve **Şimdi eşitle** ile kontrol edilir. Android ayrıca yaklaşık 15 dakikalık en kısa aralıkla arka plan görevi planlar. Pil tasarrufu, Doze ve uygulamayı zorla durdurma bu görevi geciktirebilir. Sürekli anlık eşitleme veya sunucu bildirimi kullanılmaz.
- Notion API'si koşullu/atomik satır güncellemesi sunmadığından kontrol ile yazma arasında çok dar bir yarış aralığı kalabilir. Eşzamanlı düzenlemeler için sürüm kontrolü ve kullanıcı seçimi vardır; sunucu tarafı kilit veya işlem garantisi yoktur.

Tablonun alan adlarını değiştirme; alan kaldırılırsa uygulama eşitleme hatası gösterir. Profiller bir erişim sınırı değildir: uygulamayı veya bu Notion tablosunu açabilen kişi ailedeki tüm kitapları görebilir.

## İlk kullanım

Profilini seç → Kitap ekle → kitap adıyla ara veya fotoğrafını ekle → bulunan kitabı seç → bilgileri kontrol et → rafını seç → kaydet. Ana sayfadaki okumakta olduğun kitaba dokunup kaldığın sayfayı güncelleyebilirsin.

## Doğrulama ve sınırlar

- APK derlemesi, 19 eşitleme/veri ve arama testi ve Android lint kontrolü çalıştırıldı.
- Mobil arayüzde kitap ekleme, yeniden açılışta verinin korunması, profillerin ayrılığı, sayfa sınırları, kaydedilmemiş taslak, kitabı bitirme, yıllık hedef ve kaldırma akışları tarayıcıda doğrulandı.
- Gerçek Notion hesabıyla uçtan uca eşitleme, kullanıcı telefonda bağlantı anahtarını tanımladıktan sonra doğrulanabilir. Sohbet bağlantısı ile tablo oluşturulmuş olması, APK'nın Notion'a bağlandığı anlamına gelmez.
- Gerçek kamerayla ve farklı kapak tasarımlarıyla OCR doğruluğu cihazda kontrol edilmelidir. Işık, netlik, yazı tipi ve kapak tasarımı sonucu etkiler.

Geliştirme ayrıntıları `GELISTIRME.md` dosyasındadır.
