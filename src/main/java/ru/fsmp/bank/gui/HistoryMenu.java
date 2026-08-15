package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.util.Items;
import ru.fsmp.bank.util.Lang;

import java.util.ArrayList;
import java.util.List;

/**
 * Просмотр последних операций по личному счёту.
 */
public class HistoryMenu extends BankMenu {

    public HistoryMenu(FSMPBank plugin) {
        super(plugin);
    }

    @Override
    protected void build(Player player) {
        this.inventory = Bukkit.createInventory(this, 27, Lang.color("&6История операций"));
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        Account account = plugin.bank().getPersonalAccount(player.getUniqueId());
        List<String> history = account.getHistory();

        List<String> lore = new ArrayList<>();
        if (history.isEmpty()) {
            lore.add("&7Операций пока нет.");
        } else {
            int from = Math.max(0, history.size() - 20);
            for (int i = history.size() - 1; i >= from; i--) {
                lore.add("&7" + history.get(i));
            }
        }
        inventory.setItem(13, Items.of(Material.BOOK, "&6Счёт " + account.getNumber(), lore));
        inventory.setItem(22, Items.of(Material.ARROW, "&7Назад"));
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        if (slot == 22) {
            new MainMenu(plugin).open(player);
        }
    }
}
