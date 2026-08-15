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
import ru.fsmp.bank.util.Physical;

import java.util.List;

/**
 * Банкомат: внести или снять физическую валюту (АР) со своего счёта.
 */
public class AtmMenu extends BankMenu {

    public AtmMenu(FSMPBank plugin) {
        super(plugin);
    }

    @Override
    protected void build(Player player) {
        Lang lang = plugin.lang();
        this.inventory = Bukkit.createInventory(this, 27, Lang.color("&2Банкомат"));
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        Account account = plugin.bank().getPersonalAccount(player.getUniqueId());
        int inInventory = Physical.count(player, lang.getCurrencyMaterial());

        inventory.setItem(4, Items.of(lang.getCurrencyMaterial(), "&6Счёт " + account.getNumber(),
                "&7Баланс: &a" + lang.money(account.getBalance()),
                "&7" + lang.getCurrencyName() + " в инвентаре: &e" + inInventory + " АР"));

        inventory.setItem(11, Items.of(Material.CHEST, "&aВнести АР",
                "&7Положить " + lang.getCurrencyName() + " из инвентаря на счёт.",
                "&7Доступно: &e" + inInventory + " АР"));
        inventory.setItem(15, Items.of(Material.HOPPER, "&cСнять АР",
                "&7Получить " + lang.getCurrencyName() + " со счёта в инвентарь.",
                "&7На счёте: &e" + lang.money(account.getBalance())));

        inventory.setItem(22, Items.of(Material.ARROW, "&7Назад"));
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        Account account = plugin.bank().getPersonalAccount(player.getUniqueId());
        switch (slot) {
            case 11 -> deposit(player, account);
            case 15 -> withdraw(player, account);
            case 22 -> new MainMenu(plugin).open(player);
            default -> {
            }
        }
    }

    private void deposit(Player player, Account account) {
        Lang lang = plugin.lang();
        int have = Physical.count(player, lang.getCurrencyMaterial());
        if (have <= 0) {
            player.sendMessage(Lang.color("&cУ тебя нет " + lang.getCurrencyName() + " в инвентаре."));
            return;
        }
        new AmountMenu(plugin, "&2Внести АР", lang.getCurrencyMaterial(), have, 1, have,
                List.of(Lang.color("&7Сколько " + lang.getCurrencyName() + " внести на счёт")),
                amount -> {
                    int real = Math.min(amount, Physical.count(player, lang.getCurrencyMaterial()));
                    if (real <= 0) {
                        player.sendMessage(Lang.color("&cНедостаточно " + lang.getCurrencyName() + "."));
                        new AtmMenu(plugin).open(player);
                        return;
                    }
                    Physical.remove(player, lang.getCurrencyMaterial(), real);
                    plugin.bank().deposit(account, real, "внесение АР");
                    player.sendMessage(Lang.color("&aВнесено &e" + lang.money(real) + " &aна счёт."));
                    new AtmMenu(plugin).open(player);
                },
                () -> new AtmMenu(plugin).open(player)).open(player);
    }

    private void withdraw(Player player, Account account) {
        Lang lang = plugin.lang();
        int max = (int) Math.floor(account.getBalance());
        if (max <= 0) {
            player.sendMessage(Lang.color("&cНа счёте нет средств для снятия."));
            return;
        }
        new AmountMenu(plugin, "&cСнять АР", lang.getCurrencyMaterial(), max, 1, max,
                List.of(Lang.color("&7Сколько " + lang.getCurrencyName() + " снять со счёта")),
                amount -> {
                    BankManager.Result result = plugin.bank().withdraw(account, amount, "снятие АР");
                    if (result == BankManager.Result.OK) {
                        Physical.give(player, lang.getCurrencyMaterial(), amount);
                        player.sendMessage(Lang.color("&aСнято &e" + lang.money(amount) + " &aсо счёта."));
                    } else {
                        player.sendMessage(plugin.lang().msg("not-enough-money"));
                    }
                    new AtmMenu(plugin).open(player);
                },
                () -> new AtmMenu(plugin).open(player)).open(player);
    }
}
