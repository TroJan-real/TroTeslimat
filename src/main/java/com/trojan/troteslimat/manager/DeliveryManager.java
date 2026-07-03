package com.trojan.troteslimat.manager;

import com.trojan.troteslimat.Troteslimat;
import com.trojan.troteslimat.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Haftalık teslimat iş mantığını yönetir.
 *
 * <p>Sorumluluklar:
 * <ul>
 *   <li>Teslimat kaydetme, doğrulama ve envanter manipülasyonu</li>
 *   <li>Sıralama listesi oluşturma ve önbelleğe alma</li>
 *   <li>Hafta sıfırlama ve ödül dağıtımı</li>
 * </ul>
 *
 * <p>Thread güvenliği: weeklyDeliveries ConcurrentHashMap ile korunur.
 * Sıralama önbelleği single-thread (main thread) varsayımıyla çalışır.
 *
 * @author TroJan_real
 */
public class DeliveryManager {

    // ─── Sabitler ────────────────────────────────────────────────────────────

    private static final int   TOP_LIMIT            = 5;
    /** Sıralama önbelleğinin geçerlilik süresi (ms). */
    private static final long  CACHE_TTL_MS         = 5_000L;
    private static final String UNKNOWN_PLAYER_NAME = "Bilinmeyen";

    // ─── Durum ───────────────────────────────────────────────────────────────

    private final Troteslimat plugin;
    private final Map<UUID, Integer> weeklyDeliveries = new ConcurrentHashMap<>();

    private Material weeklyMaterial;
    private int      amountPerDelivery;

    /** Önbellek: sıralı liste + son geçersizleştirme zamanı. */
    private List<Map.Entry<UUID, Integer>> sortedCache;
    private long                           cacheTimestamp = 0L;

    // ─── Başlatma ────────────────────────────────────────────────────────────

    public DeliveryManager(Troteslimat plugin) {
        this.plugin = plugin;
        reloadConfig();
        loadFromData();
    }

    /**
     * Config değerlerini yeniden yükler.
     * Admin reload komutu çağrıldığında da bu metod çağrılmalıdır.
     */
    public void reloadConfig() {
        String materialName = plugin.getConfig()
                .getString("weekly-item.material", "DIAMOND")
                .toUpperCase();
        try {
            weeklyMaterial = Material.valueOf(materialName);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning(
                    "[DeliveryManager] Geçersiz materyal adı: '" + materialName
                    + "'. DIAMOND'a geri dönülüyor.");
            weeklyMaterial = Material.DIAMOND;
        }
        amountPerDelivery = Math.max(1,
                plugin.getConfig().getInt("weekly-item.amount-per-delivery", 64));
        invalidateCache();
    }

    // ─── Veri yükleme / kaydetme ─────────────────────────────────────────────

    private void loadFromData() {
        weeklyDeliveries.putAll(plugin.getDataManager().loadDeliveries());
        invalidateCache();
    }

    /** Bellekteki teslimat verisini diske yazar. */
    public void saveToData() {
        plugin.getDataManager().saveDeliveries(weeklyDeliveries);
    }

    // ─── Teslimat Mantığı ────────────────────────────────────────────────────

    /**
     * Oyuncunun envanterinden belirli miktarda eşyayı alır ve teslimat olarak kaydeder.
     *
     * @param player Teslim yapan oyuncu
     * @param amount Teslim edilecek eşya adedi
     */
    public void deliverAmount(Player player, int amount) {
        if (amount <= 0) return;

        int playerHas = getPlayerItemCount(player);
        String matName = formatMaterialName(weeklyMaterial);

        if (playerHas < amount) {
            MessageUtil.send(player, plugin.getConfig(), "deliver-not-enough",
                    "%required%", String.valueOf(amount),
                    "%has%", String.valueOf(playerHas),
                    "%material%", matName);
            return;
        }

        int deliveryCount = amount / amountPerDelivery;
        if (deliveryCount <= 0) {
            MessageUtil.send(player, plugin.getConfig(), "deliver-not-enough",
                    "%required%", String.valueOf(amountPerDelivery),
                    "%has%", String.valueOf(playerHas),
                    "%material%", matName);
            return;
        }

        removeItemsFromInventory(player, amount);
        weeklyDeliveries.merge(player.getUniqueId(), deliveryCount, Integer::sum);
        invalidateCache();

        int totalNow = getPlayerDeliveries(player.getUniqueId());
        MessageUtil.send(player, plugin.getConfig(), "deliver-success",
                "%amount%", String.valueOf(amount),
                "%deliveries%", String.valueOf(deliveryCount),
                "%total%", String.valueOf(totalNow));

        saveToData();
    }

    /**
     * Oyuncunun envanterindeki tüm haftalık eşyaları teslim eder.
     */
    public void deliverAll(Player player) {
        int total = getPlayerItemCount(player);
        if (total == 0) {
            MessageUtil.send(player, plugin.getConfig(), "deliver-none",
                    "%material%", formatMaterialName(weeklyMaterial));
            return;
        }
        deliverAmount(player, total);
    }

    // ─── Ödüller & Sıfırlama ─────────────────────────────────────────────────

    /**
     * Hafta sonu ödüllerini dağıtır ve Discord embed'ini tetikler.
     * Hem otomatik resetde hem de admin reset komutunda çağrılır.
     */
    public void giveWeeklyRewards() {
        List<Map.Entry<UUID, Integer>> top = getTop5();
        if (top.isEmpty()) {
            plugin.getLogger().info("[DeliveryManager] Ödül dağıtılacak oyuncu yok.");
            return;
        }

        if (plugin.getDiscordBot() != null) {
            plugin.getDiscordBot().sendWeeklyTopEmbed(top);
        }

        for (int i = 0; i < top.size(); i++) {
            int    place = i + 1;
            UUID   uuid  = top.get(i).getKey();
            String name  = getPlayerName(uuid);

            List<String> commands = plugin.getConfig()
                    .getStringList("rewards." + place + ".commands");
            for (String cmd : commands) {
                String finalCmd = cmd
                        .replace("%player%", name)
                        .replace("%place%", String.valueOf(place));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            }

            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (op.isOnline() && op.getPlayer() != null) {
                op.getPlayer().sendMessage(
                        "§6§l🏆 §e" + place + ". §foldun! Ödüllerin verildi.");
            }
        }
    }

    /**
     * Teslimat verisini sıfırlar; dosyaları silmez.
     * Veri dosyasında yalnızca teslimatlar temizlenir.
     */
    public void resetWeek() {
        weeklyDeliveries.clear();
        invalidateCache();
        plugin.getDataManager().saveDeliveries(weeklyDeliveries);
        plugin.getDataManager().setLastResetTimestamp(System.currentTimeMillis());
    }

    /**
     * Tam sıfırlama — teslimat verisini siler ve veri dosyalarını kaldırır.
     * Admin "reset" komutu tarafından kullanılır.
     */
    public void resetWeekFull() {
        weeklyDeliveries.clear();
        invalidateCache();
        plugin.getDataManager().deleteAllData();
        plugin.getDataManager().setLastResetTimestamp(System.currentTimeMillis());
    }

    // ─── Sıralama / Sorgulama ────────────────────────────────────────────────

    /** Envanterdeki haftalık eşya adedini sayar. */
    public int getPlayerItemCount(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == weeklyMaterial) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public int getPlayerDeliveries(UUID uuid) {
        return weeklyDeliveries.getOrDefault(uuid, 0);
    }

    public int getTotalParticipants() {
        return weeklyDeliveries.size();
    }

    /**
     * Tüm teslimatları azalan sırayla döner; sonuç önbelleğe alınır.
     *
     * @return Değiştirilemez sıralı liste
     */
    public List<Map.Entry<UUID, Integer>> getAllDeliveriesSorted() {
        long now = System.currentTimeMillis();
        if (sortedCache != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return sortedCache;
        }
        sortedCache = weeklyDeliveries.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .collect(Collectors.toUnmodifiableList());
        cacheTimestamp = now;
        return sortedCache;
    }

    /** İlk {@value TOP_LIMIT} oyuncuyu döner. */
    public List<Map.Entry<UUID, Integer>> getTop5() {
        return getAllDeliveriesSorted().stream()
                .limit(TOP_LIMIT)
                .toList();
    }

    // ─── Haftalık Eşya Yönetimi ──────────────────────────────────────────────

    public Material getWeeklyMaterial() {
        return weeklyMaterial;
    }

    public void setWeeklyMaterial(Material material) {
        Objects.requireNonNull(material, "material null olamaz");
        this.weeklyMaterial = material;
        plugin.getConfig().set("weekly-item.material", material.name());
        plugin.saveConfig();
        plugin.getLogger().info("[DeliveryManager] Haftalık eşya değiştirildi: " + material.name());
    }

    public int getAmountPerDelivery() {
        return amountPerDelivery;
    }

    // ─── Yardımcı Metodlar ───────────────────────────────────────────────────

    private void removeItemsFromInventory(Player player, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (remaining <= 0) break;
            if (item == null || item.getType() != weeklyMaterial) continue;

            int toRemove = Math.min(item.getAmount(), remaining);
            item.setAmount(item.getAmount() - toRemove);
            remaining -= toRemove;
        }
    }

    private void invalidateCache() {
        sortedCache    = null;
        cacheTimestamp = 0L;
    }

    private String getPlayerName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : UNKNOWN_PLAYER_NAME;
    }

    public static String formatMaterialName(Material material) {
        return material.name().replace("_", " ");
    }
}
