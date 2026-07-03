package com.trojan.troteslimat.gui;

import com.trojan.troteslimat.Troteslimat;
import com.trojan.troteslimat.manager.DeliveryManager;
import com.trojan.troteslimat.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Haftalık teslimat sıralamasını gösteren sayfalı GUI.
 *
 * <p>Her giriş bir oyuncu kafası olarak gösterilir;
 * üst 3 oyuncu madalya önekiyle vurgulanır.
 *
 * @author TroJan_real
 */
public class TopDeliveryGUI implements Listener {

    // ─── Sabitler ────────────────────────────────────────────────────────────

    public static final String TITLE_PREFIX = "§6§lTop Teslimat »  ";

    private static final int PAGE_SIZE     = 36;
    private static final int CONTENT_START = 9;   // İçerik 9. slottan başlar

    private static final int SLOT_PREV    = 45;
    private static final int SLOT_CLOSE   = 47;
    private static final int SLOT_SUMMARY = 49;
    private static final int SLOT_NEXT    = 53;

    private static final String[] MEDALS = {"§6🥇 ", "§f🥈 ", "§c🥉 "};

    // ─── Durum ───────────────────────────────────────────────────────────────

    private final Troteslimat    plugin;
    private final DeliveryManager manager;

    /** UUID → mevcut sayfa */
    private final Map<UUID, Integer> openPlayers = new HashMap<>();
    private final Set<UUID>          navigating  = new HashSet<>();

    // ─── Başlatma ────────────────────────────────────────────────────────────

    public TopDeliveryGUI(Troteslimat plugin) {
        this.plugin  = plugin;
        this.manager = plugin.getDeliveryManager();
    }

    // ─── GUI Açma ────────────────────────────────────────────────────────────

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        List<Map.Entry<UUID, Integer>> allDeliveries = manager.getAllDeliveriesSorted();
        int totalEntries = allDeliveries.size();
        int totalPages   = Math.max(1, (int) Math.ceil((double) totalEntries / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String    title = TITLE_PREFIX + (page + 1) + "/" + totalPages;
        Inventory gui   = Bukkit.createInventory(null, 54, title);

        // Üst dekorasyon çubuğu
        ItemStack topGlass = GuiUtil.filler(Material.ORANGE_STAINED_GLASS_PANE);
        for (int s = 0; s <= 8; s++) gui.setItem(s, topGlass);

        // Bilgi öğesi
        gui.setItem(4, GuiUtil.makeItem(Material.GOLD_BLOCK,
                "§6§lHaftalık Teslimat Sıralaması",
                "§7Toplam katılımcı: §e" + totalEntries,
                "§7Sayfa: §e" + (page + 1) + "/" + totalPages,
                "",
                "§7Eşya: §e" + DeliveryManager.formatMaterialName(
                        manager.getWeeklyMaterial())));

        // İçerik
        if (totalEntries == 0) {
            gui.setItem(22, GuiUtil.makeItem(Material.BARRIER,
                    "§cHenüz teslimat yok",
                    "§7Bu hafta kimse teslimat yapmamış."));
        } else {
            int start = page * PAGE_SIZE;
            int end   = Math.min(start + PAGE_SIZE, totalEntries);

            for (int i = start; i < end; i++) {
                Map.Entry<UUID, Integer> entry = allDeliveries.get(i);
                int    rank   = i + 1;
                String prefix = rank <= 3 ? MEDALS[rank - 1] : "§7#" + rank + " ";

                OfflinePlayer op   = Bukkit.getOfflinePlayer(entry.getKey());
                String        name = op.getName() != null ? op.getName() : "Bilinmeyen";

                ItemStack head     = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
                if (skullMeta != null) {
                    skullMeta.setOwningPlayer(op);
                    skullMeta.setDisplayName(prefix + "§e" + name);
                    skullMeta.setLore(Arrays.asList(
                            "§7Teslimat sayısı: §f" + entry.getValue(),
                            "§7Sıralama: §f#" + rank
                    ));
                    head.setItemMeta(skullMeta);
                }
                gui.setItem(CONTENT_START + (i - start), head);
            }
        }

        // Alt navigasyon çubuğu
        ItemStack filler = GuiUtil.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int s = 45; s <= 53; s++) gui.setItem(s, filler);

        gui.setItem(SLOT_CLOSE, GuiUtil.makeItem(Material.BARRIER, "§cKapat"));

        gui.setItem(SLOT_SUMMARY, GuiUtil.makeItem(Material.COMPASS,
                "§6Özet",
                "§7Katılımcı: §e" + totalEntries + " oyuncu",
                "§7Sayfa: §e" + (page + 1) + "/" + totalPages));

        if (page > 0)
            gui.setItem(SLOT_PREV, GuiUtil.makeItem(Material.ARROW,
                    "§e« Önceki Sayfa",
                    "§7Sayfa " + page + "/" + totalPages));

        if (page < totalPages - 1)
            gui.setItem(SLOT_NEXT, GuiUtil.makeItem(Material.ARROW,
                    "§eSonraki Sayfa »",
                    "§7Sayfa " + (page + 2) + "/" + totalPages));

        openPlayers.put(player.getUniqueId(), page);
        player.openInventory(gui);
    }

    // ─── Event Handlers ──────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith(TITLE_PREFIX)) return;
        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        if (!openPlayers.containsKey(uuid)) return;

        int slot        = event.getRawSlot();
        int currentPage = openPlayers.get(uuid);
        if (slot < 0 || slot >= 54) return;

        List<Map.Entry<UUID, Integer>> all        = manager.getAllDeliveriesSorted();
        int                            totalPages = Math.max(1,
                (int) Math.ceil((double) all.size() / PAGE_SIZE));

        if (slot == SLOT_PREV && currentPage > 0) {
            navigating.add(uuid);
            open(player, currentPage - 1);
        } else if (slot == SLOT_NEXT && currentPage < totalPages - 1) {
            navigating.add(uuid);
            open(player, currentPage + 1);
        } else if (slot == SLOT_CLOSE) {
            openPlayers.remove(uuid);
            player.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().startsWith(TITLE_PREFIX)) return;
        UUID uuid = event.getPlayer().getUniqueId();

        if (navigating.remove(uuid)) return;
        openPlayers.remove(uuid);
    }
}
