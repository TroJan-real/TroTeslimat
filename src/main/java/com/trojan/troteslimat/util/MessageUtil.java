package com.trojan.troteslimat.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.CommandSender;

/**
 * Mesaj okuma ve renk çözümleme yardımcı sınıfı.
 * Config'den mesaj okuma mantığını tek bir yerde tutar.
 *
 * @author TroJan_real
 */
public final class MessageUtil {

    private MessageUtil() {}

    /**
     * Config'den messages.{key} yolundan mesajı okur, & renk kodlarını çözer
     * ve isteğe bağlı placeholder çiftlerini uygular.
     *
     * <p>Kullanım: {@code msg(cfg, "deliver-success", "%amount%", "64", "%total%", "3")}
     *
     * @param config       Plugin config dosyası
     * @param key          messages. altındaki anahtar
     * @param replacements Çift sayıda string: [aranan, değer, aranan, değer, ...]
     * @return Renk kodları çözülmüş, placeholder'lar uygulanmış mesaj
     */
    public static String msg(FileConfiguration config, String key, String... replacements) {
        String raw = config.getString("messages." + key, "§cMesaj bulunamadı: " + key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    /**
     * Mesajı doğrudan bir CommandSender'a gönderir.
     */
    public static void send(CommandSender sender, FileConfiguration config,
                            String key, String... replacements) {
        sender.sendMessage(msg(config, key, replacements));
    }
}
