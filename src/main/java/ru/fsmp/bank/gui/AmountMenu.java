package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.util.Items;
import ru.fsmp.bank.util.Lang;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Универсальное окно выбора суммы (в АР). Кнопками ±1/±10/±64 и «Максимум».
 * Результат уходит в переданный обработчик — так все денежные операции
 * выполняются через UI.
 */
public class AmountMenu extends BankMenu {

    private final String title;
    private final Material icon;
    private final List<String> infoLore;
    private int amount;
    private final int min;
    private final int max;            // 0 = без верхнего предела
    private final IntConsumer onConfirm;
    private final Runnable onBack;

    public AmountMenu(FSMPBank plugin, String title, Material icon, int start, int min, int max,
                      List<String> infoLore, IntConsumer onConfirm, Runnable onBack) {
        super(plugin);
        this.title = title;
        this.icon = icon;
        this.min = Math.max(1, min);
        this.max = max;
        this.infoLore = infoLore;
        this.amount = clamp(start);
        this.onConfirm = onConfirm;
        this.onBack = onBack;
    }

    private int upper() {
        return max > 0 ? max : Integer.MAX_VALUE;
    }

    private int clamp(int value) {
        return Math.max(min, Math.min(upper(), value));
    }

    @Override
    protected void build(Player player) {
        Lang lang = plugin.lang();
        this.inventory = Bukkit.createInventory(this, 27, Lang.color(title));
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        List<String> lore = new ArrayList<>();
        if (infoLore != null) {
            lore.addAll(infoLore);
        }
        lore.add("");
        lore.add("&7Сумма: &a" + lang.money(amount));
        if (max > 0) {
            lore.add("&7Максимум: &e" + lang.money(max));
        }
        inventory.setItem(13, Items.of(icon, "&6Выберите сумму", lore));

        inventory.setItem(10, Items.of(Material.RED_STAINED_GLASS_PANE, "&c-64"));
        inventory.setItem(11, Items.of(Material.RED_STAINED_GLASS_PANE, "&c-10"));
        inventory.setItem(12, Items.of(Material.RED_STAINED_GLASS_PANE, "&c-1"));
        inventory.setItem(14, Items.of(Material.LIME_STAINED_GLASS_PANE, "&a+1"));
        inventory.setItem(15, Items.of(Material.LIME_STAINED_GLASS_PANE, "&a+10"));
        inventory.setItem(16, Items.of(Material.LIME_STAINED_GLASS_PANE, "&a+64"));

        inventory.setItem(21, Items.of(Material.EMERALD_BLOCK, "&aПодтвердить",
                "&7Сумма: &a" + lang.money(amount)));
        if (max > 0) {
            inventory.setItem(22, Items.of(Material.LAPIS_LAZULI, "&bМаксимум",
                    "&7Поставить &e" + lang.money(max)));
        }
        inventory.setItem(23, Items.of(Material.BARRIER, "&cОтмена"));
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        switch (slot) {
            case 10 -> change(player, -64);
            case 11 -> change(player, -10);
            case 12 -> change(player, -1);
            case 14 -> change(player, 1);
            case 15 -> change(player, 10);
            case 16 -> change(player, 64);
            case 22 -> {
                if (max > 0) {
                    amount = max;
                    reopen(player);
                }
            }
            case 21 -> {
                if (amount >= min) {
                    onConfirm.accept(amount);
                }
            }
            case 23 -> {
                if (onBack != null) {
                    onBack.run();
                } else {
                    player.closeInventory();
                }
            }
            default -> {
            }
        }
    }

    private void change(Player player, int delta) {
        amount = clamp(amount + delta);
        reopen(player);
    }

    private void reopen(Player player) {
        build(player);
        player.openInventory(inventory);
    }
}
