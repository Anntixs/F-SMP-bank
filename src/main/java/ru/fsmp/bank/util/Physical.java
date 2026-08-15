package ru.fsmp.bank.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Работа с физической валютой (АР — алмазная руда) в инвентаре игрока.
 * 1 единица валюты = 1 предмет заданного материала.
 */
public final class Physical {

    private Physical() {
    }

    /** Сколько единиц валюты у игрока в инвентаре. */
    public static int count(Player player, Material material) {
        int total = 0;
        ItemStack[] contents = player.getInventory().getStorageContents();
        if (contents == null) {
            return 0;
        }
        for (ItemStack item : contents) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /** Забрать из инвентаря заданное количество валюты (по стакам). */
    public static void remove(Player player, Material material, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int take = Math.min(64, remaining);
            player.getInventory().removeItem(new ItemStack(material, take));
            remaining -= take;
        }
    }

    /** Выдать игроку валюту; что не влезло в инвентарь — падает под ноги. */
    public static void give(Player player, Material material, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int give = Math.min(64, remaining);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(material, give));
            for (ItemStack extra : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), extra);
            }
            remaining -= give;
        }
    }
}
