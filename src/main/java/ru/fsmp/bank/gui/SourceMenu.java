package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.manager.BankManager;
import ru.fsmp.bank.model.Card;
import ru.fsmp.bank.model.PaymentSource;
import ru.fsmp.bank.util.Items;
import ru.fsmp.bank.util.Lang;

import java.util.List;
import java.util.function.Consumer;

/**
 * Выбор источника оплаты: личный счёт или одна из выданных карт.
 */
public class SourceMenu extends BankMenu {

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    private final String title;
    private final List<PaymentSource> sources;
    private final Consumer<PaymentSource> onPick;
    private final Runnable onBack;

    public SourceMenu(FSMPBank plugin, String title, List<PaymentSource> sources,
                      Consumer<PaymentSource> onPick, Runnable onBack) {
        super(plugin);
        this.title = title;
        this.sources = sources;
        this.onPick = onPick;
        this.onBack = onBack;
    }

    @Override
    protected void build(Player player) {
        Lang lang = plugin.lang();
        this.inventory = Bukkit.createInventory(this, 36, Lang.color(title));
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        for (int i = 0; i < sources.size() && i < SLOTS.length; i++) {
            PaymentSource source = sources.get(i);
            if (!source.isCard()) {
                inventory.setItem(SLOTS[i], Items.of(lang.getCurrencyMaterial(), "&6Личный счёт",
                        "&7Счёт: &e" + source.getAccount().getNumber(),
                        "&7Баланс: &a" + lang.money(source.getAccount().getBalance())));
            } else {
                Card card = source.getCard();
                inventory.setItem(SLOTS[i], Items.of(
                        card.isFrozen() ? Material.RED_STAINED_GLASS_PANE : Material.PAPER,
                        "&bКарта " + BankManager.formatCard(card.getNumber()),
                        "&7Счёт: &e" + card.getAccountNumber(),
                        "&7Баланс счёта: &a" + lang.money(source.getAccount().getBalance()),
                        card.getDailyLimit() <= 0 ? "&7Лимит: &aбез лимита"
                                : "&7Остаток лимита: &e" + lang.money(Math.max(0, card.remainingToday())),
                        card.isFrozen() ? "&cЗаморожена" : "&aАктивна"));
            }
        }

        inventory.setItem(31, Items.of(Material.ARROW, "&7Назад"));
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        if (slot == 31) {
            if (onBack != null) {
                onBack.run();
            } else {
                player.closeInventory();
            }
            return;
        }
        for (int i = 0; i < SLOTS.length && i < sources.size(); i++) {
            if (SLOTS[i] == slot) {
                onPick.accept(sources.get(i));
                return;
            }
        }
    }
}
