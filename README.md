# TroTeslimat

**Haftalık Teslimat Yarışması** — Profesyonel Minecraft Sunucu Plugini

Oyuncularınızın her hafta belirli bir eşyayı (örneğin elmas, netherite vs.) teslim ederek birbirleriyle rekabet ettiği, eğlenceli ve motive edici bir sistem.

### ✨ Özellikler

- **Modern ve Kullanıcı Dostu GUI**  
  Oyuncular 64, 128, 256, 512, 1024 adet veya "Tümünü Teslim Et" butonlarıyla kolayca teslimat yapabilir.

- **Sayfalı Leaderboard GUI**  
  Haftalık teslimat sıralamasını oyuncu kafalarıyla birlikte detaylı şekilde gösterir.

- **Discord Entegrasyonu**  
  - `/teslimat` slash komutu ile anlık sıralama embed’i  
  - Hafta sonunda otomatik @everyone’lu haftalık sonuç embed’i

- **Gelişmiş Admin Sistemi**  
  - `/troteadmin setitem` → Haftalık eşyayı değiştirmek için görsel arayüz  
  - `/troteadmin reset` → Haftayı sıfırlayıp ödülleri dağıtma  
  - `/troteadmin top` → Chat’te sıralama görme  
  - `/troteadmin reload` → Config’i yeniden yükleme

- **Tamamen Yapılandırılabilir**  
  Haftalık eşya, ödül komutları, Discord ayarları, mesajlar, reset günü/saati tamamen config üzerinden kontrol edilir.

- **Otomatik Haftalık Sıfırlama**  
  İstediğiniz gün ve saatte (örneğin Pazar 23:59) otomatik olarak çalışır.

- **Performans Odaklı**  
  Leaderboard cache sistemi ve optimize kod yapısı sayesinde büyük sunucularda da sorunsuz çalışır.

### 📌 Desteklenen Sürümler
- **Minecraft 1.21** ve üzeri
- Spigot, Paper, Purpur ve Fork’larla uyumludur

### ⚠️ Lisans ve Kullanım Koşulları

Bu plugin **GNU General Public License v3.0 (GPLv3)** ile lisanslanmıştır.

**İzin Verilen:**
- Plugini ücretsiz indirmek ve kendi sunucunuzda kullanmak
- Config dosyasını kendi sunucunuza göre düzenlemek

**Kesinlikle Yasak:**
- Plugini kendi adınıza değiştirerek yayınlamak
- Plugini satmak, ücretli paketlerde dağıtmak veya ticari amaçla kullanmak
- "TroJan_real" authorship bilgisini kaldırmak

İhlal durumunda yasal işlem başlatılacaktır.

### 📋 Komutlar

| Komut              | Açıklama                              | Yetki                    |
|--------------------|---------------------------------------|--------------------------|
| `/teslimat`        | Teslimat menüsünü açar                | `troteslimat.use`        |
| `/topteslimat`     | Haftalık top GUI’sini açar            | `troteslimat.use`        |
| `/troteadmin`      | Admin komutlarını gösterir            | `troteslimat.admin`      |

**Admin Alt Komutları:** `setitem`, `reset`, `top`, `reload`

### 🚀 Kurulum

1. [Releases](https://github.com/TroJan-real/TroTeslimat/releases) sekmesinden en son sürümü indirin.
2. İndirdiğiniz `.jar` dosyasını sunucunuzun `plugins` klasörüne atın.
3. Sunucuyu başlatın.
4. Oluşan `plugins/TroTeslimat/config.yml` dosyasını dikkatlice düzenleyin.
5. Sunucuyu yeniden başlatın.

**Geliştirici:** [TroJan_real](https://github.com/TroJan-real)

---
