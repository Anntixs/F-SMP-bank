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
 * Главное меню банка — точка входа во все операции (всё выполняется через UI).
 */
public class MainMenu extends BankMenu {

    public MainMenu(FSMPBank plugin) {
        super(plugin);
    }

    @Override
    protected void build(Player player) {
        Lang lang = plugin.lang();
        this.inventory = Bukkit.createInventory(this, 36, Lang.color("&6Банк F-SMP"));
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        // Нет счёта — предлагаем открыть кнопкой
        if (!plugin.bank().hasPersonalAccount(player.getUniqueId())) {
            inventory.setItem(13, Items.of(Material.EMERALD, "&aОткрыть счёт",
                    "&7У тебя ещё нет счёта.",
                    "",
                    "&eНажми, чтобы открыть"));
            inventory.setItem(31, Items.of(Material.BARRIER, "&cЗакрыть"));
            return;
        }

        Account account = plugin.bank().getPersonalAccount(player.getUniqueId());
        long corpCount = plugin.bank().getCorporateAccounts(player.getUniqueId()).size();

        inventory.setItem(10, Items.of(lang.getCurrencyMaterial(), "&6Мой счёт",
                "&7Номер: &e" + account.getNumber(),
                "&7Баланс: &a" + lang.money(account.getBalance())));

        inventory.setItem(11, Items.of(Material.PAPER, "&bМои карты",
                "&7Карт: &e" + account.getCards().size() + "/" + plugin.bank().getMaxCards(),
                "",
                "&eУправление картами"));

        inventory.setItem(12, Items.of(Material.GOLD_NUGGET, "&eПеревод / оплата",
                "&7Отправить " + lang.getCurrencyName() + " другому игроку или на счёт.",
                "",
                "&eВыбрать получателя"));

        inventory.setItem(14, Items.of(Material.EMERALD, "&aИП (корп. счета)",
                "&7Твоих счетов: &e" + corpCount,
                "",
                "&eОткрыть список"));

        inventory.setItem(15, Items.of(Material.BOOK, "&6История",
                "&7Последние операции по счёту.",
                "",
                "&eОткрыть историю"));

        if (plugin.bank().isBanker(player.getUniqueId()) || player.hasPermission("fsmpbank.banker")) {
            inventory.setItem(16, Items.of(Material.GOLD_BLOCK, "&6Банкир (касса)",
                    "&7Пополнять и снимать средства игрокам.",
                    "",
                    "&eОткрыть кассу"));
        }

        if (player.hasPermission("fsmpbank.admin")) {
            inventory.setItem(22, Items.of(Material.NETHER_STAR, "&4Администрирование",
                    "&7Назначение банкиров, баланс.",
                    "",
                    "&eОткрыть панель"));
        }

        inventory.setItem(31, Items.of(Material.BARRIER, "&cЗакрыть"));
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        // Нет счёта: единственная активная кнопка — открыть
        if (!plugin.bank().hasPersonalAccount(player.getUniqueId())) {
            if (slot == 13) {
                Account acc = plugin.bank().openPersonalAccount(player.getUniqueId());
                player.sendMessage(plugin.lang().msg("account-created", "number", acc.getNumber()));
                new MainMenu(plugin).open(player);
            } else if (slot == 31) {
                player.closeInventory();
            }
            return;
        }

        switch (slot) {
            case 11 -> new CardMenu(plugin).open(player);
            case 12 -> TransferMenu.open(plugin, player);
            case 14 -> new CorpMenu(plugin).open(player);
            case 15 -> new HistoryMenu(plugin).open(player);
            case 16 -> {
                if (plugin.bank().isBanker(player.getUniqueId()) || player.hasPermission("fsmpbank.banker")) {
                    BankerMenu.open(plugin, player);
                }
            }
            case 22 -> {
                if (player.hasPermission("fsmpbank.admin")) {
                    new AdminMenu(plugin).open(player);
                }
            }
            case 31 -> player.closeInventory();
            default -> {
            }
        }
    }
}
