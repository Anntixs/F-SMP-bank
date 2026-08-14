package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.util.Items;
import ru.fsmp.bank.util.Lang;

import java.util.List;

/**
 * Список корпоративных счетов (ИП) игрока.
 */
public class CorpMenu extends BankMenu {

    private List<Account> shown;

    public CorpMenu(FSMPBank plugin) {
        super(plugin);
    }

    @Override
    protected void build(Player player) {
        this.inventory = Bukkit.createInventory(this, 27, Lang.color("&aКорпоративные счета (ИП)"));
        Lang lang = plugin.lang();

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        shown = plugin.bank().getCorporateAccounts(player.getUniqueId());
        int slot = 10;
        for (Account account : shown) {
            if (slot > 16) {
                break;
            }
            boolean owner = player.getUniqueId().equals(account.getOwner());
            inventory.setItem(slot, Items.of(Material.EMERALD_BLOCK, "&a" + account.getName(),
                    "&7Номер счёта: &e" + account.getNumber(),
                    "&7Баланс: &a" + lang.money(account.getBalance()),
                    "&7Роль: " + (owner ? "&6владелец" : "&bсотрудник"),
                    "&7Сотрудников: &e" + account.getMembers().size(),
                    "",
                    "&8Пополнить: /bank corp deposit " + account.getNumber() + " <сумма>",
                    "&8Снять: /bank corp withdraw " + account.getNumber() + " <сумма>"));
            slot++;
        }

        inventory.setItem(22, Items.of(Material.WRITABLE_BOOK, "&2Открыть новый ИП",
                "&7Стоимость открытия: &e"
                        + lang.money(plugin.bank().getCorpOpenCost()),
                "",
                "&eКоманда: /bank corp create <название>"));
        inventory.setItem(26, Items.of(Material.ARROW, "&7Назад"));
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        if (slot == 26) {
            new MainMenu(plugin).open(player);
            return;
        }
        if (slot == 22) {
            player.closeInventory();
            player.sendMessage(Lang.color("&7Чтобы открыть ИП, введи: &e/bank corp create <название>"));
            return;
        }
        if (shown != null && slot >= 10 && slot <= 16) {
            int index = slot - 10;
            if (index < shown.size()) {
                Account account = shown.get(index);
                player.closeInventory();
                player.sendMessage(Lang.color("&aИП &6" + account.getName()
                        + " &7— номер счёта: &e" + account.getNumber()));
                player.sendMessage(Lang.color("&7Скидывать деньги сюда: &e/bank pay "
                        + account.getNumber() + " <сумма>"));
            }
        }
    }
}
