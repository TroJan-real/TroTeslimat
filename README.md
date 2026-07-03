# TroTeslimat

**Haftalık Teslimat Yarışması** — Sunucunuzu canlandıracak profesyonel bir rekabet sistemi!

Oyuncular her hafta belirli bir eşyayı (örneğin elmas, netherite, shulker vs.) teslim ederek leaderboard'da yükseliyor. Hem eğlenceli hem de uzun vadeli oyuncu tutma sağlayan popüler bir sistem.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-brightgreen)
![Java](https://img.shields.io/badge/Java-21-blue)
![License](https://img.shields.io/badge/License-GPLv3-red)

### Neden TroTeslimat?

Birçok sunucuda oyuncular zamanla sıkılır. **TroTeslimat** ile sunucunuza haftalık bir hedef ve rekabet ortamı katıyorsunuz. Oyuncular "Bu hafta ne kadar teslimat yapacağım?" diye motive oluyor.

### ✨ Başlıca Özellikler

- **Şık ve Kolay Kullanımlı Teslimat Arayüzü**  
  Oyuncular tek tıkla 64, 128, 256, 512, 1024 adet veya tüm envanterlerindeki eşyayı teslim edebiliyor.

- **Profesyonel Leaderboard**  
  Sayfalı ve görsel olarak güzel bir Top Teslimat menüsü. Oyuncu kafalarıyla birlikte sıralama gösterimi.

- **Discord Entegrasyonu**  
  - Discord’da `/teslimat` komutu ile anlık sıralama  
  - Her hafta sonunda otomatik olarak @everyone ile haftalık sonuçları kanalınıza gönderir

- **Güçlü Admin Kontrolü**  
  - Haftalık eşyayı görsel arayüzden değiştirme  
  - İstediğiniz zaman hafta sıfırlama ve ödül dağıtma  
  - Mevcut sıralamayı chat’te görme  
  - Config yeniden yükleme

- **Tamamen Özelleştirilebilir**  
  Haftalık eşya, ödül komutları, Discord ayarları, mesajlar, reset günü ve saati tamamen config üzerinden kontrol edilir.

- **Otomatik Sistem**  
  Belirlediğiniz gün ve saatte (örneğin Pazar 23:59) sistem otomatik sıfırlanır ve ödüller dağıtılır.

- **Optimizasyon**  
  Cache sistemi sayesinde çok sayıda oyuncu olsa bile akıcı çalışır.

### 📌 Desteklenen Sürümler
- **Minecraft 1.21** ve üzeri
- Spigot, Paper, Purpur ve diğer 1.21+ tabanlı sunucularla tam uyumludur.

### ⚠️ Lisans ve Kullanım Koşulları

Bu plugin **GNU General Public License v3.0** ile lisanslanmıştır.

**İzin verilenler:**
- Plugini ücretsiz olarak indirmek ve kendi sunucunuzda kullanmak
- Config dosyasını kendi zevkinize göre düzenlemek

**Kesinlikle yasak olanlar:**
- Plugini kendi adınıza değiştirerek yayınlamak
- Plugini satmak, ücretli kaynaklarda paylaşmak veya ticari amaçla kullanmak
- Geliştirici bilgisi olan "TroJan_real" kısmını kaldırmak

Her türlü ihlal yasal takibe tabidir.

### 📋 Komutlar

| Komut              | Açıklama                                   | Yetki                    |
|--------------------|--------------------------------------------|--------------------------|
| `/teslimat`        | Teslimat menüsünü açar                     | `troteslimat.use`        |
| `/topteslimat`     | Haftalık sıralama GUI’sini açar            | `troteslimat.use`        |
| `/troteadmin`      | Admin komutlarını kullanmanızı sağlar     | `troteslimat.admin`      |

**Admin Alt Komutları:** `setitem`, `reset`, `top`, `reload`

### 🚀 Kurulum Adımları

1. [Releases](https://github.com/TroJan-real/TroTeslimat/releases) sayfasından en son sürümü indirin.
2. İndirdiğiniz `.jar` dosyasını sunucunuzun `plugins` klasörüne atın.
3. Sunucuyu bir kez başlatın.
4. Oluşan `plugins/TroTeslimat/config.yml` dosyasını dikkatlice düzenleyin.
5. Sunucuyu yeniden başlatın.

**Geliştirici:** [TroJan_real](https://github.com/TroJan-real)

---
