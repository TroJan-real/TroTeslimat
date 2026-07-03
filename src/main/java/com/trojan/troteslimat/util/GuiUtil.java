package com.trojan.troteslimat.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * GUI yardımcı sınıfı — tüm GUI sınıflarında tekrarlanan
 * makeItem mantığını tek bir yerde toplar.
 *
 * @author TroJan_real
 */
public final class GuiUtil {

    private GuiUtil() {}

    /**
     * Belirtilen materyal, isim ve lore ile yeni bir ItemStack oluşturur.
     *
     * @param material ItemStack materyali
     * @param name     Görünen isim (& renk kodları desteklenir)
     * @param lore     İsteğe bağlı lore satırları
     * @return Hazırlanmış ItemStack
     */
    public static ItemStack makeItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Lore listesiyle ItemStack oluşturur.
     */
    public static ItemStack makeItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(name);
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Dekorasyon için görünmez (isim yok) cam panel oluşturur. */
    public static ItemStack filler(Material material) {
        return makeItem(material, "§r");
    }
}
