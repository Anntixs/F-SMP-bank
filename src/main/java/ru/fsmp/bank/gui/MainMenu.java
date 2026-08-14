package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.util.Items;
import ru.fsmp.bank.util.Lang;

/**
 * Главное меню банка: баланс, карты, корпоративные счета.
 */
public class MainMenu extends BankMenu {

    public MainMenu(FSMPBank plugin) {
        super(plugin);
    }

    @Override
    protected void build(Player player) {
        Lang lang = plugin.lang();
        this.inventory = Bukkit.createInventory(this, 27, Lang.color("&6Банк F-SMP"));

        Account account = plugin.bank().getPersonalAccount(player.getUniqueId());
        long corpCount = plugin.bank().getCorporateAccounts(player.getUniqueId()).size();

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        inventory.setItem(11, Items.of(Material.GOLD_INGOT, "&6Мой счёт",
                "&7Номер: &e" + account.getNumber(),
                "&7Баланс: &a" + lang.money(account.getBalance()),
                "",
                "&8Переводы: /bank pay <кому> <сумма>"));

        inventory.setItem(13, Items.of(Material.PAPER, "&bМои карты",
                "&7Карт выпущено: &e" + account.getCards().size() + "/" + plugin.bank().getMaxCards(),
                "",
                "&eНажми, чтобы управлять картами"));

        inventory.setItem(15, Items.of(Material.EMERALD, "&aКорпоративные счета (ИП)",
                "&7У тебя счетов: &e" + corpCount,
                "",
                "&eНажми, чтобы открыть список"));

        inventory.setItem(22, Items.of(Material.BARRIER, "&cЗакрыть"));
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        switch (slot) {
            case 13 -> new CardMenu(plugin).open(player);
            case 15 -> new CorpMenu(plugin).open(player);
            case 22 -> player.closeInventory();
            default -> {
            }
        }
    }
}
