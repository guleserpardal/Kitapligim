# Kitaplığım sunucusu

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
- `POST /api/chat` (`X-Token` gerekir): `{messages:[{role,content}], profile, library:[{title,author,status,rating}]}` → `{reply, books:[{title,author,why,isbn,totalPages,cover,source,verified}]}`. `verified:false` kitap, Google Books/Open Library'de doğrulanamamıştır; uygulama bunu kullanıcıya belirtir.

## Test

`npm test` (sahte Claude ile çalışır, gerçek Claude çağırmaz).
