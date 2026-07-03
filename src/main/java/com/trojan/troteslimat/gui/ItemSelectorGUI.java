package com.trojan.troteslimat.gui;

import com.trojan.troteslimat.Troteslimat;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin eşya seçim GUI'si — sayfalı materyal listesi.
 *
 * <p>Özellikler:
 * <ul>
 *   <li>Tüm geçerli (legacy olmayan, item olan) materyaller</li>
 *   <li>Mevcut seçili materyal vurgulaması</li>
 *   <li>Sayfa navigasyonu (← / →)</li>
 *   <li>Kapat butonu</li>
 * </ul>
 *
 * @author TroJan_real
 */
public class ItemSelectorGUI implements Listener {

    // ─── Sabitler ────────────────────────────────────────────────────────────

    public static final String TITLE_PREFIX = "§6Eşya Seç »  ";

    private static final int PAGE_SIZE     = 45;
    private static final int SLOT_PREV     = 45;
    private static final int SLOT_CLOSE    = 47;
    private static final int SLOT_INFO     = 49;
    private static final int SLOT_NEXT     = 53;
    private static final int NAV_ROW_START = 45;

    /** Tüm geçerli materyaller (plugin yüklenirken bir kez hesaplanır). */
    private static final List<Material> ALL_MATERIALS = Arrays.stream(Material.values())
            .filter(m -> !m.isLegacy() && m.isItem() && !m.isAir())
            .sorted(Comparator.comparing(Material::name))
            .collect(Collectors.toUnmodifiableList());

    private static final int TOTAL_PAGES =
            (int) Math.ceil((double) ALL_MATERIALS.size() / PAGE_SIZE);

    private static final ItemStack FILLER =
            GuiUtil.filler(Material.GRAY_STAINED_GLASS_PANE);

    private static final ItemStack CLOSE_BTN =
            GuiUtil.makeItem(Material.BARRIER, "§cKapat");

    // ─── Durum ───────────────────────────────────────────────────────────────

    private final Troteslimat plugin;

    /** UUID → mevcut sayfa */
    private final Map<UUID, Integer> openAdmins = new HashMap<>();
    /** Sayfa navigasyonu sırasında onClose'un kayıtı silmesini engeller. */
    private final Set<UUID>          navigating = new HashSet<>();

    // ─── Başlatma ────────────────────────────────────────────────────────────

    public ItemSelectorGUI(Troteslimat plugin) {
        this.plugin = plugin;
    }

    // ─── GUI Açma ────────────────────────────────────────────────────────────

    public void open(Player admin) {
        open(admin, 0);
    }

    public void open(Player admin, int page) {
        page = Math.max(0, Math.min(page, TOTAL_PAGES - 1));

        String    title          = TITLE_PREFIX + (page + 1) + "/" + TOTAL_PAGES;
        Inventory gui            = Bukkit.createInventory(null, 54, title);
        Material  currentMaterial = plugin.getDeliveryManager().getWeeklyMaterial();

        // Materyal öğeleri (ilk 45 slot)
        int start = page * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, ALL_MATERIALS.size());

        for (int i = start; i < end; i++) {
            Material mat        = ALL_MATERIALS.get(i);
            boolean  isSelected = mat == currentMaterial;

            List<String> lore = new ArrayList<>();
            lore.add("§7Tıkla → haftalık eşya olarak ayarla");
            if (isSelected) lore.add("§a§lŞu an seçili!");
            lore.add("§8" + mat.name());

            ItemStack item = GuiUtil.makeItem(mat,
                    (isSelected ? "§a§l✔ " : "§e") + mat.name().replace("_", " "),
                    lore);
            gui.setItem(i - start, item);
        }

        // Navigasyon satırı
        for (int s = NAV_ROW_START; s <= 53; s++) gui.setItem(s, FILLER);
        gui.setItem(SLOT_CLOSE, CLOSE_BTN);

        if (page > 0)
            gui.setItem(SLOT_PREV, GuiUtil.makeItem(Material.ARROW,
                    "§e« Önceki Sayfa",
                    "§7Sayfa " + page + "/" + TOTAL_PAGES));

        gui.setItem(SLOT_INFO, GuiUtil.makeItem(currentMaterial,
                "§6§lMevcut Eşya",
                "§e" + currentMaterial.name().replace("_", " "),
                "",
                "§7Toplam eşya: §f" + ALL_MATERIALS.size(),
                "§7Sayfa: §f" + (page + 1) + "/" + TOTAL_PAGES));

        if (page < TOTAL_PAGES - 1)
            gui.setItem(SLOT_NEXT, GuiUtil.makeItem(Material.ARROW,
                    "§eSonraki Sayfa »",
                    "§7Sayfa " + (page + 2) + "/" + TOTAL_PAGES));

        openAdmins.put(admin.getUniqueId(), page);
        admin.openInventory(gui);
    }

    // ─── Event Handlers ──────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin)) return;
        if (!event.getView().getTitle().startsWith(TITLE_PREFIX)) return;
        event.setCancelled(true);

        UUID uuid = admin.getUniqueId();
        if (!openAdmins.containsKey(uuid)) return;

        int slot        = event.getRawSlot();
        int currentPage = openAdmins.get(uuid);

        if (slot < 0 || slot >= 54) return;

        // Navigasyon & kapat
        if (slot == SLOT_PREV && currentPage > 0) {
            navigating.add(uuid);
            open(admin, currentPage - 1);
            return;
        }
        if (slot == SLOT_NEXT && currentPage < TOTAL_PAGES - 1) {
            navigating.add(uuid);
            open(admin, currentPage + 1);
            return;
        }
        if (slot == SLOT_CLOSE) {
            openAdmins.remove(uuid);
            admin.closeInventory();
            return;
        }
        if (slot >= NAV_ROW_START) return;  // Diğer nav satırı slotları

        // Materyal seçimi
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Material selected = clicked.getType();
        plugin.getDeliveryManager().setWeeklyMaterial(selected);
        admin.sendMessage("§a✔ Haftalık eşya değiştirildi: §e"
                + selected.name().replace("_", " "));
        openAdmins.remove(uuid);
        admin.closeInventory();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().startsWith(TITLE_PREFIX)) return;
        UUID uuid = event.getPlayer().getUniqueId();

        if (navigating.remove(uuid)) return;  // Sayfa değişimi → kaldırma
        openAdmins.remove(uuid);
    }
}
