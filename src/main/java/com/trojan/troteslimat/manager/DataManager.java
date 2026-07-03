package com.trojan.troteslimat.manager;

import com.trojan.troteslimat.Troteslimat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Teslimat verilerinin YAML tabanlı kalıcı depolamasını yönetir.
 *
 * <p>Sorumluluklar:
 * <ul>
 *   <li>data.yml dosyasını oluştur / yükle / kaydet</li>
 *   <li>UUID → teslimat sayısı eşlemesini oku/yaz</li>
 *   <li>Son sıfırlama zaman damgasını yönet</li>
 * </ul>
 *
 * @author TroJan_real
 */
public class DataManager {

    private static final String DATA_FILE      = "data.yml";
    private static final String KEY_DELIVERIES = "deliveries";
    private static final String KEY_LAST_RESET = "last-reset";

    private final Troteslimat plugin;
    private File dataFile;
    private FileConfiguration data;

    public DataManager(Troteslimat plugin) {
        this.plugin = plugin;
        loadData();
    }

    // ─── Yükleme / Kaydetme ──────────────────────────────────────────────────

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), DATA_FILE);

        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE,
                        "[DataManager] data.yml oluşturulamadı!", ex);
            }
        }

        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    /** Bellekteki veriyi diske yazar. */
    public void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE,
                    "[DataManager] data.yml kaydedilemedi!", ex);
        }
    }

    // ─── Teslimat CRUD ───────────────────────────────────────────────────────

    /**
     * Teslimat eşlemesini data.yml'e yazar.
     *
     * @param deliveries UUID → teslimat sayısı eşlemesi
     */
    public void saveDeliveries(Map<UUID, Integer> deliveries) {
        data.set(KEY_DELIVERIES, null);
        deliveries.forEach((uuid, count) ->
                data.set(KEY_DELIVERIES + "." + uuid, count));
        saveData();
    }

    /**
     * data.yml'den UUID → teslimat sayısı eşlemesini yükler.
     *
     * @return Yüklenen eşleme; dosya yoksa boş Map döner
     */
    public Map<UUID, Integer> loadDeliveries() {
        Map<UUID, Integer> deliveries = new HashMap<>();
        var section = data.getConfigurationSection(KEY_DELIVERIES);
        if (section == null) return deliveries;

        for (String rawUuid : section.getKeys(false)) {
            try {
                deliveries.put(UUID.fromString(rawUuid),
                        data.getInt(KEY_DELIVERIES + "." + rawUuid));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning(
                        "[DataManager] Geçersiz UUID atlandı: " + rawUuid);
            }
        }
        return deliveries;
    }

    // ─── Son Sıfırlama Zaman Damgası ─────────────────────────────────────────

    public long getLastResetTimestamp() {
        return data.getLong(KEY_LAST_RESET, 0L);
    }

    public void setLastResetTimestamp(long timestamp) {
        data.set(KEY_LAST_RESET, timestamp);
        saveData();
    }

    // ─── Tam Temizlik ────────────────────────────────────────────────────────

    /**
     * Veri klasöründeki tüm .yml dosyalarını (config.yml hariç) siler
     * ve sonra data.yml'i temiz olarak yeniden başlatır.
     */
    public void deleteAllData() {
        File folder = plugin.getDataFolder();
        if (folder.exists()) {
            File[] files = folder.listFiles(
                    (dir, name) -> name.endsWith(".yml") && !name.equals("config.yml"));
            if (files != null) {
                for (File f : files) {
                    if (f.delete()) {
                        plugin.getLogger().info("[DataManager] Silindi: " + f.getName());
                    } else {
                        plugin.getLogger().warning(
                                "[DataManager] Silinemedi: " + f.getName());
                    }
                }
            }
        }
        loadData();
    }

    /** Ham FileConfiguration nesnesini döner (ileri düzey kullanım için). */
    public FileConfiguration getData() {
        return data;
    }
}
