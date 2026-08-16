package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.manager.BankManager;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.model.Card;
import ru.fsmp.bank.model.PaymentSource;
import ru.fsmp.bank.util.Items;
import ru.fsmp.bank.util.Lang;

import java.util.List;

/**
 * Окно оплаты по платёжной табличке [Bank]. Можно выбрать источник оплаты
 * (личный счёт или выданную карту) и сумму (если табличка без фикс. цены).
 */
public class PaymentMenu extends BankMenu {

    private final String targetNumber;
    private final String label;
    private final boolean fixed;
    private double amount;
    private PaymentSource source;

    public PaymentMenu(FSMPBank plugin, String targetNumber, String label, double amount, boolean fixed) {
        super(plugin);
        this.targetNumber = targetNumber;
        this.label = label;
        this.amount = amount;
        this.fixed = fixed;
    }

    @Override
    protected void build(Player player) {
        Lang lang = plugin.lang();
        this.inventory = Bukkit.createInventory(this, 27, Lang.color("&2Оплата"));
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        if (source == null) {
            Account personal = plugin.bank().getPersonalAccount(player.getUniqueId());
            if (personal != null) {
                source = new PaymentSource(personal, null);
            }
        }

        Account target = plugin.bank().getByNumber(targetNumber);
        String targetName = target != null && target.isCorporate() && !target.getName().isEmpty()
                ? target.getName() : ("счёт " + targetNumber);

        inventory.setItem(13, Items.of(Material.GOLD_INGOT, "&6К оплате",
                "&7Получатель: &e" + targetName,
                "&7Счёт: &e" + targetNumber,
                label == null || label.isEmpty() ? "&8—" : "&7Назначение: &f" + label,
                "",
                "&7Сумма: &a" + lang.money(amount)));

        // Источник оплаты (клик — сменить, если есть карты)
        List<PaymentSource> sources = plugin.bank().getPaymentSources(player.getUniqueId());
        String sourceLabel = source == null ? "нет счёта"
                : (source.isCard() ? "карта " + BankManager.formatCard(source.getCard().getNumber()) : "личный счёт");
        inventory.setItem(4, Items.of(
                source != null && source.isCard() ? Material.PAPER : lang.getCurrencyMaterial(),
                "&bИсточник: &e" + sourceLabel,
                sources.size() > 1 ? "&7Нажми, чтобы сменить источник" : "&8Другого источника нет"));

        if (!fixed) {
            inventory.setItem(10, Items.of(Material.RED_STAINED_GLASS_PANE, "&c-100"));
            inventory.setItem(11, Items.of(Material.RED_STAINED_GLASS_PANE, "&c-10"));
            inventory.setItem(12, Items.of(Material.RED_STAINED_GLASS_PANE, "&c-1"));
            inventory.setItem(14, Items.of(Material.LIME_STAINED_GLASS_PANE, "&a+1"));
            inventory.setItem(15, Items.of(Material.LIME_STAINED_GLASS_PANE, "&a+10"));
            inventory.setItem(16, Items.of(Material.LIME_STAINED_GLASS_PANE, "&a+100"));
        }

        inventory.setItem(21, Items.of(Material.EMERALD_BLOCK, "&aОплатить",
                "&7Сумма: &a" + lang.money(amount)));
        inventory.setItem(23, Items.of(Material.BARRIER, "&cОтмена"));
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        switch (slot) {
            case 4 -> changeSource(player);
            case 10 -> change(player, -100);
            case 11 -> change(player, -10);
            case 12 -> change(player, -1);
            case 14 -> change(player, 1);
            case 15 -> change(player, 10);
            case 16 -> change(player, 100);
            case 23 -> player.closeInventory();
            case 21 -> confirm(player);
            default -> {
            }
        }
    }

    private void changeSource(Player player) {
        List<PaymentSource> sources = plugin.bank().getPaymentSources(player.getUniqueId());
        if (sources.size() <= 1) {
            return;
        }
        new SourceMenu(plugin, "&2Чем оплатить", sources,
                picked -> {
                    this.source = picked;
                    build(player);
                    player.openInventory(inventory);
                },
                () -> {
                    build(player);
                    player.openInventory(inventory);
                }
        ).open(player);
    }

    private void change(Player player, double delta) {
        if (fixed) {
            return;
        }
        amount = Math.max(1, Math.round((amount + delta) * 100.0) / 100.0);
        build(player);
        player.openInventory(inventory);
    }

    private void confirm(Player player) {
        Lang lang = plugin.lang();
        Account target = plugin.bank().getByNumber(targetNumber);
        if (target == null) {
            player.closeInventory();
            player.sendMessage(lang.msg("target-not-found"));
            return;
        }
        if (source == null) {
            player.closeInventory();
            player.sendMessage(lang.msg("no-account"));
            return;
        }
        BankManager.Result result = plugin.bank().spend(source.getAccount(), source.getCard(),
                target, amount, label == null ? "" : label);

        player.closeInventory();
        switch (result) {
            case OK -> {
                player.sendMessage(lang.msg("transfer-sent",
                        "amount", lang.money(amount), "target", targetNumber));
                Player owner = Bukkit.getPlayer(target.getOwner());
                if (owner != null && owner.isOnline() && !owner.equals(player)) {
                    owner.sendMessage(lang.msg("transfer-received",
                            "amount", lang.money(amount), "from", source.getAccount().getNumber()));
                }
            }
            case NOT_ENOUGH -> player.sendMessage(lang.msg("not-enough-money"));
            case CARD_FROZEN -> player.sendMessage(lang.msg("card-frozen"));
            case CARD_LIMIT -> player.sendMessage(lang.msg("card-over-limit"));
            case SAME_ACCOUNT -> player.sendMessage(Lang.color("&cНельзя платить самому себе."));
            default -> player.sendMessage(lang.msg("invalid-amount"));
        }
    }
}
