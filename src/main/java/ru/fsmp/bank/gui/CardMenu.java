package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.manager.BankManager;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.util.Items;
import ru.fsmp.bank.util.Lang;

import java.util.List;

/**
 * Управление картами: выпуск (до лимита), просмотр номера, удаление (Shift+ЛКМ).
 */
public class CardMenu extends BankMenu {

    public CardMenu(FSMPBank plugin) {
        super(plugin);
    }

    @Override
    protected void build(Player player) {
        int max = plugin.bank().getMaxCards();
        int size = 27;
        this.inventory = Bukkit.createInventory(this, size, Lang.color("&bМои карты"));

        for (int i = 0; i < size; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        Account account = plugin.bank().getPersonalAccount(player.getUniqueId());
        List<String> cards = account.getCards();

        int[] slots = {10, 12, 14, 16, 19, 21, 23};
        for (int i = 0; i < max && i < slots.length; i++) {
            int slot = slots[i];
            if (i < cards.size()) {
                String card = cards.get(i);
                inventory.setItem(slot, Items.of(Material.PAPER, "&eКарта №" + (i + 1),
                        "&7" + BankManager.formatCard(card),
                        "&7Счёт: &e" + account.getNumber(),
                        "",
                        "&cShift+ЛКМ — удалить карту"));
            } else {
                inventory.setItem(slot, Items.of(Material.LIME_DYE, "&aВыпустить карту",
                        "&7Свободный слот",
                        "",
                        "&eНажми, чтобы выпустить новую карту"));
            }
        }

        inventory.setItem(26, Items.of(Material.ARROW, "&7Назад"));
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        Lang lang = plugin.lang();
        Account account = plugin.bank().getPersonalAccount(player.getUniqueId());

        if (slot == 26) {
            new MainMenu(plugin).open(player);
            return;
        }

        int max = plugin.bank().getMaxCards();
        int[] slots = {10, 12, 14, 16, 19, 21, 23};
        for (int i = 0; i < max && i < slots.length; i++) {
            if (slots[i] != slot) {
                continue;
            }
            List<String> cards = account.getCards();
            if (i < cards.size()) {
                String card = cards.get(i);
                if (click.isShiftClick()) {
                    plugin.bank().removeCard(account, card);
                    player.sendMessage(lang.msg("card-removed", "number", BankManager.formatCard(card)));
                    build(player);
                    player.openInventory(inventory);
                } else {
                    player.sendMessage(lang.msg("card-created", "number", BankManager.formatCard(card)));
                }
            } else {
                String newCard = plugin.bank().issueCard(account);
                if (newCard == null) {
                    player.sendMessage(lang.msg("card-limit", "max", String.valueOf(max)));
                } else {
                    player.sendMessage(lang.msg("card-created", "number", BankManager.formatCard(newCard)));
                }
                build(player);
                player.openInventory(inventory);
            }
            return;
        }
    }
}
