# GitHub'a yükleme

1. ZIP'i çıkar. `Kitapligim` klasörü projenin köküdür.
2. GitHub'da yeni, boş bir depo oluştur.
3. Tarayıcıdan yükleyeceksen `Kitapligim` klasörünün içindekileri deponun köküne yükle. `.gitignore` ve `.gitattributes` dosyalarını da ekle. `gradle/wrapper/gradle-wrapper.jar` dosyası dahil olmalı.
4. Git kullanıyorsan proje klasöründe aşağıdaki komutları çalıştır. `DEPO_URL` yerine oluşturduğun deponun HTTPS adresini yaz.

```sh
git init
git add .
git commit -m "Kitaplığım 1.0.1 kaynak kodu"
git branch -M main
git remote add origin DEPO_URL
git push -u origin main
```

## Derleme

Android Studio'da bu klasörü aç ve Gradle eşitlemesini tamamla. JDK 17 ve Android SDK Platform 35 gerekir. macOS/Linux'ta gerekiyorsa `chmod +x gradlew` çalıştır.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Windows'ta `./gradlew` yerine `gradlew.bat` kullan.

## Telefonda yüklü uygulamayı güncellemek

İlk verilen özel kaynak yedeğindeki `signing/` klasörünü bilgisayarında proje köküne kopyala. Bu klasörü GitHub'a yükleme. `.gitignore` yerel imza dosyalarını dışlar. Aynı anahtar korunmadan mevcut uygulamaya güncelleme kurulamaz. Yeni güncellemede `app/build.gradle` içindeki `versionCode` değerini 2'den büyük yap; `versionName` değerini de güncelle. Ardından:

```sh
./gradlew :app:assembleRelease
```

Telefon için çıktı: `app/build/outputs/apk/release/app-arm64-v8a-release.apk`.

Notion erişim anahtarı uygulamanın Ayarlar ekranında cihazda girilir; kaynak koda eklenmez. Aile Notion tablo kimliği bir erişim anahtarı değildir; başka bir tablo için NotionSync.java dosyasındaki veri kaynağı kimliğini ve ilgili alan eşlemesini düzenlemek gerekir.
