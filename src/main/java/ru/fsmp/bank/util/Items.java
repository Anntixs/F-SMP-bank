package ru.fsmp.bank.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Небольшой помощник для сборки предметов интерфейса.
 */
public final class Items {

    private Items() {
    }

    public static ItemStack of(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Lang.color(name));
            if (lore.length > 0) {
                List<String> lines = new ArrayList<>();
                for (String line : lore) {
                    lines.add(Lang.color(line));
                }
                meta.setLore(lines);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack of(Material material, String name, List<String> lore) {
        return of(material, name, lore.toArray(new String[0]));
    }

    public static ItemStack filler(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static List<String> lore(String... lines) {
        return new ArrayList<>(Arrays.asList(lines));
    }
}
