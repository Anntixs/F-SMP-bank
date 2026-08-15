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
 * Окно банкира для конкретного счёта: пополнить (выдать наличные) или снять.
 * Пополнение здесь — функция кассы/банкомата: средства зачисляются игроку.
 */
public class BankerActionMenu extends BankMenu {

    private final String accountNumber;
    private final String label;

    public BankerActionMenu(FSMPBank plugin, String accountNumber, String label) {
        super(plugin);
        this.accountNumber = accountNumber;
        this.label = label;
    }

    @Override
    protected void build(Player player) {
        Lang lang = plugin.lang();
        this.inventory = Bukkit.createInventory(this, 27, Lang.color("&6Касса: " + label));
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        Account account = plugin.bank().getByNumber(accountNumber);
        double balance = account == null ? 0 : account.getBalance();

        inventory.setItem(4, Items.of(lang.getCurrencyMaterial(), "&6" + label,
                "&7Счёт: &e" + accountNumber,
                "&7Баланс: &a" + lang.money(balance)));

        inventory.setItem(11, Items.of(Material.CHEST, "&aПополнить счёт",
                "&7Зачислить средства игроку."));
        inventory.setItem(15, Items.of(Material.HOPPER, "&cСнять со счёта",
                "&7Списать средства со счёта."));
        inventory.setItem(22, Items.of(Material.ARROW, "&7Назад"));
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        switch (slot) {
            case 11 -> deposit(player);
            case 15 -> withdraw(player);
            case 22 -> BankerMenu.open(plugin, player);
            default -> {
            }
        }
    }

    private void deposit(Player banker) {
        Lang lang = plugin.lang();
        new AmountMenu(plugin, "&aПополнить: " + label, lang.getCurrencyMaterial(), 1, 1, 0,
                List.of(Lang.color("&7Сколько зачислить на счёт &e" + accountNumber)),
                amount -> {
                    Account account = plugin.bank().getByNumber(accountNumber);
                    if (account == null) {
                        banker.sendMessage(plugin.lang().msg("target-not-found"));
                        return;
                    }
                    plugin.bank().deposit(account, amount, "касса " + banker.getName());
                    banker.sendMessage(lang.msg("deposit-done",
                            "amount", lang.money(amount), "player", label));
                    Player owner = Bukkit.getPlayer(account.getOwner());
                    if (owner != null && owner.isOnline()) {
                        owner.sendMessage(lang.msg("transfer-received",
                                "amount", lang.money(amount), "from", "касса"));
                    }
                    new BankerActionMenu(plugin, accountNumber, label).open(banker);
                },
                () -> new BankerActionMenu(plugin, accountNumber, label).open(banker)
        ).open(banker);
    }

    private void withdraw(Player banker) {
        Lang lang = plugin.lang();
        Account account = plugin.bank().getByNumber(accountNumber);
        int max = account == null ? 0 : (int) Math.floor(account.getBalance());
        if (max <= 0) {
            banker.sendMessage(plugin.lang().msg("not-enough-money"));
            return;
        }
        new AmountMenu(plugin, "&cСнять: " + label, lang.getCurrencyMaterial(), max, 1, max,
                List.of(Lang.color("&7Сколько списать со счёта &e" + accountNumber)),
                amount -> {
                    Account acc = plugin.bank().getByNumber(accountNumber);
                    if (acc == null) {
                        banker.sendMessage(plugin.lang().msg("target-not-found"));
                        return;
                    }
                    BankManager.Result result = plugin.bank().withdraw(acc, amount, "касса " + banker.getName());
                    if (result == BankManager.Result.OK) {
                        banker.sendMessage(lang.msg("withdraw-done",
                                "amount", lang.money(amount), "player", label));
                    } else {
                        banker.sendMessage(plugin.lang().msg("not-enough-money"));
                    }
                    new BankerActionMenu(plugin, accountNumber, label).open(banker);
                },
                () -> new BankerActionMenu(plugin, accountNumber, label).open(banker)
        ).open(banker);
    }
}
