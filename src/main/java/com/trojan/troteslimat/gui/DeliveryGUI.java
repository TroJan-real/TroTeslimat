package com.trojan.troteslimat.gui;

import com.trojan.troteslimat.Troteslimat;
import com.trojan.troteslimat.manager.DeliveryManager;
import com.trojan.troteslimat.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Oyuncuların haftalık teslimat yapabileceği 54-slot GUI.
 *
 * <p>Durum yönetimi:
 * <ul>
 *   <li>{@code true}  → GUI açık, gerçek kapat olayına tepki ver</li>
 *   <li>{@code false} → Yenileniyor, onClose olayını yoksay</li>
 * </ul>
 *
 * @author TroJan_real
 */
public class DeliveryGUI implements Listener {

    // ─── Sabitler ────────────────────────────────────────────────────────────

    public static final String TITLE = "§6§lTeslimat Menüsü";

    private static final int   SLOT_INFO        = 13;
    private static final int   SLOT_TOP_BUTTON  = 22;
    private static final int   SLOT_DELIVER_ALL = 40;

    private static final int[] BUTTON_AMOUNTS = {64, 128, 256, 512, 1024};
    private static final int[] BUTTON_SLOTS   = {29, 30, 31, 32, 33};

    private static final int[] DECO_SLOTS =
            {0,1,2,3,4,5,6,7,8, 45,46,47,48,49,50,51,52,53};

    // ─── Durum ───────────────────────────────────────────────────────────────

    private final Troteslimat   plugin;
    private final DeliveryManager manager;

    /** UUID → GUI durumu: true=açık, false=yenileniyor */
    private final Map<UUID, Boolean> openPlayers = new HashMap<>();

    // ─── Başlatma ────────────────────────────────────────────────────────────

    public DeliveryGUI(Troteslimat plugin) {
        this.plugin  = plugin;
        this.manager = plugin.getDeliveryManager();
    }

    // ─── GUI Açma ────────────────────────────────────────────────────────────

    public void open(Player player) {
        Material material      = manager.getWeeklyMaterial();
        int      perDelivery   = manager.getAmountPerDelivery();
        int      playerHas     = manager.getPlayerItemCount(player);
        int      totalDeliveries = manager.getPlayerDeliveries(player.getUniqueId());
        int      deliverable   = (playerHas / perDelivery) * perDelivery;
        String   matName       = DeliveryManager.formatMaterialName(material);

        Inventory gui = Bukkit.createInventory(null, 54, TITLE);

        // Bilgi öğesi
        gui.setItem(SLOT_INFO, GuiUtil.makeItem(Material.BOOK,
                "§6§lHaftalık Teslimat",
                "§7Eşya: §e" + matName,
                "§7Her §f" + perDelivery + " §7adet = §f1 §7teslimat",
                "",
                "§7Elinde: §e" + playerHas + " §7adet",
                "§7Teslim edilebilir: §a" + deliverable + " §7adet",
                "§7Toplam teslimatın: §6" + totalDeliveries));

        // Sıralama butonu
        gui.setItem(SLOT_TOP_BUTTON, GuiUtil.makeItem(Material.GOLD_BLOCK,
                "§6§lSıralamayı Gör",
                "§7Haftalık teslimat sıralamasını görmek için tıkla.",
                "",
                "§eTıkla →"));

        // Miktar butonları
        for (int i = 0; i < BUTTON_AMOUNTS.length; i++) {
            gui.setItem(BUTTON_SLOTS[i],
                    buildDeliverButton(BUTTON_AMOUNTS[i], playerHas));
        }

        // Tümünü teslim et
        if (deliverable > 0) {
            gui.setItem(SLOT_DELIVER_ALL, GuiUtil.makeItem(Material.EMERALD_BLOCK,
                    "§2§l§nTÜMÜNÜ TESLİM ET",
                    "§aElindeki tüm " + matName + " teslim edilir.",
                    "§7Miktar: §f" + deliverable + " §7adet"
                    + " (§f" + (deliverable / perDelivery) + " §7teslimat)"));
        } else {
            gui.setItem(SLOT_DELIVER_ALL, GuiUtil.makeItem(Material.RED_STAINED_GLASS_PANE,
                    "§cTeslim edilecek eşya yok",
                    "§7Envanterinde §e" + matName + " §7bulunamadı."));
        }

        // Dekorasyon
        ItemStack glass = GuiUtil.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int s : DECO_SLOTS) gui.setItem(s, glass);

        openPlayers.put(player.getUniqueId(), true);
        player.openInventory(gui);
    }

    // ─── Event Handlers ──────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!openPlayers.containsKey(uuid)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String displayName = meta.getDisplayName();

        // Sıralama butonu
        if (clicked.getType() == Material.GOLD_BLOCK
                && displayName.contains("Sıralamayı")) {
            openPlayers.remove(uuid);
            plugin.getTopDeliveryGUI().open(player);
            return;
        }

        boolean acted = false;

        // Tümünü teslim et
        if (clicked.getType() == Material.EMERALD_BLOCK
                && displayName.contains("TÜMÜNÜ")) {
            manager.deliverAll(player);
            acted = true;
        }
        // Belirli miktar teslim et
        else if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE
                 && displayName.contains("Tane Teslim Et")) {
            int amount = extractFirstNumber(displayName);
            if (amount > 0) {
                manager.deliverAmount(player, amount);
                acted = true;
            }
        }

        if (acted) {
            openPlayers.put(uuid, false);   // yenileniyor — onClose'u yoksay
            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 1L);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        UUID uuid = event.getPlayer().getUniqueId();

        Boolean state = openPlayers.get(uuid);
        if (state == null) return;

        if (state) {
            // Gerçek kapanma — ESC veya başka envanter
            openPlayers.remove(uuid);
        }
        // state == false → yenileniyor, kaldırma
    }

    // ─── Yardımcı ────────────────────────────────────────────────────────────

    private ItemStack buildDeliverButton(int amount, int playerHas) {
        if (playerHas >= amount) {
            return GuiUtil.makeItem(Material.LIME_STAINED_GLASS_PANE,
                    "§a" + amount + " Tane Teslim Et",
                    "§7Tıkla ve §f" + amount + " §7adet teslim et.");
        } else {
            return GuiUtil.makeItem(Material.RED_STAINED_GLASS_PANE,
                    "§c" + amount + " Tane Teslim Et",
                    "§cYetersiz! §7Elinde §f" + playerHas + "/" + amount + " §7adet var.");
        }
    }

    /**
     * Görünen isimdeki (renk kodları soyulmuş) ilk sayıyı çıkarır.
     * Örn. "§a64 Tane Teslim Et" → 64
     */
    private static int extractFirstNumber(String str) {
        String digits = str.replaceAll("§.", "").replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
