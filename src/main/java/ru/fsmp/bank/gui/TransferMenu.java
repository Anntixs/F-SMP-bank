package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.manager.BankManager;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.model.Card;
import ru.fsmp.bank.model.PaymentSource;
import ru.fsmp.bank.util.Lang;

import java.util.List;
import java.util.Optional;

/**
 * UI-поток перевода/оплаты: получатель → источник (счёт или карта) → сумма.
 */
public final class TransferMenu {

    private TransferMenu() {
    }

    public static void open(FSMPBank plugin, Player payer) {
        if (!plugin.bank().hasPersonalAccount(payer.getUniqueId())) {
            payer.sendMessage(plugin.lang().msg("no-account"));
            return;
        }
        new PlayerPickerMenu(plugin, "&6Кому перевести",
                target -> onPickPlayer(plugin, payer, target),
                () -> new MainMenu(plugin).open(payer),
                "&eПо номеру счёта", "&7Ввести номер в чат",
                () -> promptAccount(plugin, payer)
        ).open(payer);
    }

    private static void onPickPlayer(FSMPBank plugin, Player payer, OfflinePlayer target) {
        Account to = plugin.bank().getPersonalAccount(target.getUniqueId());
        if (to == null) {
            payer.sendMessage(Lang.color("&cУ игрока &e" + target.getName() + " &cнет счёта."));
            open(plugin, payer);
            return;
        }
        chooseSource(plugin, payer, to, target.getName());
    }

    private static void promptAccount(FSMPBank plugin, Player payer) {
        payer.closeInventory();
        payer.sendMessage(Lang.color("&eВведи номер счёта или карты получателя в чат &7(или «отмена»)&e:"));
        plugin.prompts().await(payer.getUniqueId(), input -> {
            Optional<Account> target = plugin.bank().resolveTarget(input);
            if (target.isEmpty()) {
                payer.sendMessage(plugin.lang().msg("target-not-found"));
                open(plugin, payer);
                return;
            }
            String label = target.get().isCorporate() && !target.get().getName().isEmpty()
                    ? target.get().getName() : ("счёт " + target.get().getNumber());
            chooseSource(plugin, payer, target.get(), label);
        });
    }

    private static void chooseSource(FSMPBank plugin, Player payer, Account to, String label) {
        List<PaymentSource> sources = plugin.bank().getPaymentSources(payer.getUniqueId());
        if (sources.isEmpty()) {
            payer.sendMessage(plugin.lang().msg("no-account"));
            return;
        }
        if (sources.size() == 1) {
            chooseAmount(plugin, payer, sources.get(0), to, label);
            return;
        }
        new SourceMenu(plugin, "&6Чем оплатить", sources,
                source -> chooseAmount(plugin, payer, source, to, label),
                () -> open(plugin, payer)
        ).open(payer);
    }

    private static void chooseAmount(FSMPBank plugin, Player payer, PaymentSource source, Account to, String label) {
        Lang lang = plugin.lang();
        int max = (int) Math.floor(source.getAccount().getBalance());
        Card card = source.getCard();
        if (card != null && card.getDailyLimit() > 0) {
            max = Math.min(max, (int) Math.floor(Math.max(0, card.remainingToday())));
        }
        if (card != null && card.isFrozen()) {
            payer.sendMessage(lang.msg("card-frozen"));
            new MainMenu(plugin).open(payer);
            return;
        }
        if (max <= 0) {
            payer.sendMessage(card != null && card.getDailyLimit() > 0
                    ? lang.msg("card-over-limit") : lang.msg("not-enough-money"));
            new MainMenu(plugin).open(payer);
            return;
        }
        String sourceLabel = card == null ? "личный счёт" : "карта " + BankManager.formatCard(card.getNumber());
        new AmountMenu(plugin, "&6Перевод: " + label, lang.getCurrencyMaterial(), 1, 1, max,
                List.of(Lang.color("&7Получатель: &e" + label),
                        Lang.color("&7Счёт: &e" + to.getNumber()),
                        Lang.color("&7Источник: &e" + sourceLabel)),
                amount -> execute(plugin, payer, source, to, amount),
                () -> chooseSource(plugin, payer, to, label)
        ).open(payer);
    }

    private static void execute(FSMPBank plugin, Player payer, PaymentSource source, Account to, int amount) {
        Lang lang = plugin.lang();
        BankManager.Result result = plugin.bank().spend(source.getAccount(), source.getCard(), to, amount, "перевод");
        switch (result) {
            case OK -> {
                payer.sendMessage(lang.msg("transfer-sent",
                        "amount", lang.money(amount), "target", to.getNumber()));
                Player receiver = Bukkit.getPlayer(to.getOwner());
                if (receiver != null && receiver.isOnline() && !receiver.equals(payer)) {
                    receiver.sendMessage(lang.msg("transfer-received",
                            "amount", lang.money(amount), "from", source.getAccount().getNumber()));
                }
            }
            case NOT_ENOUGH -> payer.sendMessage(lang.msg("not-enough-money"));
            case CARD_FROZEN -> payer.sendMessage(lang.msg("card-frozen"));
            case CARD_LIMIT -> payer.sendMessage(lang.msg("card-over-limit"));
            case SAME_ACCOUNT -> payer.sendMessage(Lang.color("&cНельзя переводить самому себе."));
            default -> payer.sendMessage(lang.msg("invalid-amount"));
        }
        new MainMenu(plugin).open(payer);
    }
}
