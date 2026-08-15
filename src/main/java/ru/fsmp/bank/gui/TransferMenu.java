package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.manager.BankManager;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.util.Lang;

import java.util.List;
import java.util.Optional;

/**
 * UI-поток перевода: выбор получателя (голова онлайн-игрока или ввод номера
 * счёта в чат) → выбор суммы → подтверждение.
 */
public final class TransferMenu {

    private TransferMenu() {
    }

    public static void open(FSMPBank plugin, Player payer) {
        Account from = plugin.bank().getPersonalAccount(payer.getUniqueId());
        if (from == null) {
            payer.sendMessage(plugin.lang().msg("no-account"));
            return;
        }
        new PlayerPickerMenu(plugin, "&6Кому перевести",
                target -> onPickPlayer(plugin, payer, from, target),
                () -> new MainMenu(plugin).open(payer),
                "&eПо номеру счёта", "&7Ввести номер в чат",
                () -> promptAccount(plugin, payer, from)
        ).open(payer);
    }

    private static void onPickPlayer(FSMPBank plugin, Player payer, Account from, OfflinePlayer target) {
        Account to = plugin.bank().getPersonalAccount(target.getUniqueId());
        if (to == null) {
            payer.sendMessage(Lang.color("&cУ игрока &e" + target.getName() + " &cнет счёта."));
            open(plugin, payer);
            return;
        }
        chooseAmount(plugin, payer, from, to, target.getName());
    }

    private static void promptAccount(FSMPBank plugin, Player payer, Account from) {
        payer.closeInventory();
        payer.sendMessage(Lang.color("&eВведи номер счёта получателя в чат &7(или «отмена»)&e:"));
        plugin.prompts().await(payer.getUniqueId(), input -> {
            Optional<Account> target = plugin.bank().resolveTarget(input);
            if (target.isEmpty()) {
                payer.sendMessage(plugin.lang().msg("target-not-found"));
                open(plugin, payer);
                return;
            }
            String label = target.get().isCorporate() && !target.get().getName().isEmpty()
                    ? target.get().getName() : ("счёт " + target.get().getNumber());
            chooseAmount(plugin, payer, from, target.get(), label);
        });
    }

    private static void chooseAmount(FSMPBank plugin, Player payer, Account from, Account to, String label) {
        Lang lang = plugin.lang();
        int max = (int) Math.floor(from.getBalance());
        if (max <= 0) {
            payer.sendMessage(lang.msg("not-enough-money"));
            new MainMenu(plugin).open(payer);
            return;
        }
        new AmountMenu(plugin, "&6Перевод: " + label, lang.getCurrencyMaterial(), 1, 1, max,
                List.of(Lang.color("&7Получатель: &e" + label),
                        Lang.color("&7Счёт: &e" + to.getNumber())),
                amount -> {
                    BankManager.Result result = plugin.bank().transfer(from, to, amount, "перевод");
                    switch (result) {
                        case OK -> {
                            payer.sendMessage(lang.msg("transfer-sent",
                                    "amount", lang.money(amount), "target", to.getNumber()));
                            Player receiver = Bukkit.getPlayer(to.getOwner());
                            if (receiver != null && receiver.isOnline() && !receiver.equals(payer)) {
                                receiver.sendMessage(lang.msg("transfer-received",
                                        "amount", lang.money(amount), "from", from.getNumber()));
                            }
                        }
                        case NOT_ENOUGH -> payer.sendMessage(lang.msg("not-enough-money"));
                        case SAME_ACCOUNT -> payer.sendMessage(Lang.color("&cНельзя переводить самому себе."));
                        default -> payer.sendMessage(lang.msg("invalid-amount"));
                    }
                    new MainMenu(plugin).open(payer);
                },
                () -> open(plugin, payer)
        ).open(payer);
    }
}
