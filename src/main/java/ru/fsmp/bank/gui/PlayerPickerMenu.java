package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.util.Items;
import ru.fsmp.bank.util.Lang;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Выбор игрока из числа онлайн (головы). Плюс необязательная кнопка
 * с дополнительным действием (например, ввод номера счёта из чата).
 */
public class PlayerPickerMenu extends BankMenu {

    private final String title;
    private final Consumer<OfflinePlayer> onPick;
    private final Runnable onBack;
    private final String extraLabel;
    private final String extraDesc;
    private final Runnable extraAction;

    private List<Player> shown;

    public PlayerPickerMenu(FSMPBank plugin, String title, Consumer<OfflinePlayer> onPick,
                            Runnable onBack, String extraLabel, String extraDesc, Runnable extraAction) {
        super(plugin);
        this.title = title;
        this.onPick = onPick;
        this.onBack = onBack;
        this.extraLabel = extraLabel;
        this.extraDesc = extraDesc;
        this.extraAction = extraAction;
    }

    @Override
    protected void build(Player player) {
        this.inventory = Bukkit.createInventory(this, 54, Lang.color(title));

        shown = new ArrayList<>(Bukkit.getOnlinePlayers());
        int slot = 0;
        for (Player online : shown) {
            if (slot > 44) {
                break;
            }
            inventory.setItem(slot, head(online));
            slot++;
        }

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, Items.filler(Material.BLACK_STAINED_GLASS_PANE));
        }
        inventory.setItem(45, Items.of(Material.ARROW, "&7Назад"));
        if (extraLabel != null && extraAction != null) {
            inventory.setItem(49, Items.of(Material.NAME_TAG, extraLabel,
                    extraDesc == null ? "" : extraDesc));
        }
    }

    private ItemStack head(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skull) {
            skull.setOwningPlayer(target);
        }
        if (meta != null) {
            meta.setDisplayName(Lang.color("&e" + target.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(Lang.color("&7Нажми, чтобы выбрать"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        if (slot == 45) {
            if (onBack != null) {
                onBack.run();
            } else {
                player.closeInventory();
            }
            return;
        }
        if (slot == 49 && extraLabel != null && extraAction != null) {
            extraAction.run();
            return;
        }
        if (shown != null && slot >= 0 && slot < shown.size() && slot <= 44) {
            onPick.accept(shown.get(slot));
        }
    }
}
