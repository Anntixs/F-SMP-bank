package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.manager.BankManager;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.model.Card;
import ru.fsmp.bank.util.Items;
import ru.fsmp.bank.util.Lang;

import java.util.List;

/**
 * Управление конкретной картой: заморозка, суточный лимит, выдача держателю,
 * возврат себе, удаление. Доступно владельцу счёта, к которому привязана карта.
 */
public class CardDetailMenu extends BankMenu {

    private final String cardNumber;

    public CardDetailMenu(FSMPBank plugin, String cardNumber) {
        super(plugin);
        this.cardNumber = cardNumber;
    }

    @Override
    protected void build(Player player) {
        Lang lang = plugin.lang();
        this.inventory = Bukkit.createInventory(this, 27, Lang.color("&bКарта"));
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        Card card = plugin.bank().getCard(cardNumber);
        if (card == null) {
            inventory.setItem(13, Items.of(Material.BARRIER, "&cКарта не найдена"));
            inventory.setItem(22, Items.of(Material.ARROW, "&7Назад"));
            return;
        }

        inventory.setItem(4, Items.of(Material.PAPER, "&e" + BankManager.formatCard(cardNumber),
                "&7Счёт: &e" + card.getAccountNumber(),
                "&7Держатель: &e" + holderName(card),
                card.getDailyLimit() <= 0 ? "&7Лимит: &aбез лимита"
                        : "&7Лимит: &e" + lang.money(card.getDailyLimit()),
                "&7Потрачено сегодня: &e" + lang.money(card.getSpentToday()),
                card.isFrozen() ? "&cСтатус: заморожена" : "&aСтатус: активна"));

        inventory.setItem(10, card.isFrozen()
                ? Items.of(Material.LIME_DYE, "&aРазморозить карту", "&7Разрешить оплату этой картой.")
                : Items.of(Material.RED_STAINED_GLASS_PANE, "&cЗаморозить карту", "&7Запретить оплату этой картой."));

        inventory.setItem(11, Items.of(Material.LAPIS_LAZULI, "&bЗадать суточный лимит",
                "&7Ограничить траты по карте за день."));
        inventory.setItem(12, Items.of(Material.BARRIER, "&7Убрать лимит",
                "&7Сделать карту без ограничения."));

        inventory.setItem(14, Items.of(Material.PLAYER_HEAD, "&6Выдать игроку",
                "&7Передать карту другому игроку.",
                "&7Он сможет платить с этого счёта",
                "&7в пределах лимита."));
        inventory.setItem(15, Items.of(Material.NAME_TAG, "&aВернуть себе",
                "&7Сделать держателем снова себя."));

        inventory.setItem(16, Items.of(Material.RED_STAINED_GLASS_PANE, "&cУдалить карту",
                "&7Карта перестанет работать."));

        inventory.setItem(22, Items.of(Material.ARROW, "&7Назад"));
    }

    private String holderName(Card card) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(card.getHolder());
        return op.getName() != null ? op.getName() : card.getHolder().toString();
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        Lang lang = plugin.lang();
        Card card = plugin.bank().getCard(cardNumber);
        if (card == null || slot == 22) {
            new CardMenu(plugin).open(player);
            return;
        }
        Account account = plugin.bank().getByNumber(card.getAccountNumber());
        if (account == null || !account.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(Lang.color("&cТолько владелец счёта управляет этой картой."));
            return;
        }

        switch (slot) {
            case 10 -> {
                plugin.bank().setCardFrozen(card, !card.isFrozen());
                player.sendMessage(Lang.color(card.isFrozen()
                        ? "&cКарта заморожена." : "&aКарта разморожена."));
                reopen(player);
            }
            case 11 -> new AmountMenu(plugin, "&bСуточный лимит карты", Material.LAPIS_LAZULI,
                    Math.max(1, (int) Math.floor(card.getDailyLimit())), 1, 0,
                    List.of(Lang.color("&7Максимум трат по карте за день")),
                    amount -> {
                        plugin.bank().setCardLimit(card, amount);
                        player.sendMessage(Lang.color("&aЛимит карты: &e" + lang.money(amount) + " &aв день."));
                        new CardDetailMenu(plugin, cardNumber).open(player);
                    },
                    () -> new CardDetailMenu(plugin, cardNumber).open(player)).open(player);
            case 12 -> {
                plugin.bank().setCardLimit(card, 0);
                player.sendMessage(Lang.color("&aЛимит снят — карта без ограничения."));
                reopen(player);
            }
            case 14 -> new PlayerPickerMenu(plugin, "&6Кому выдать карту",
                    target -> {
                        plugin.bank().setCardHolder(card, target.getUniqueId());
                        player.sendMessage(Lang.color("&aКарта выдана игроку &e"
                                + (target.getName() != null ? target.getName() : target.getUniqueId())));
                        Player online = Bukkit.getPlayer(target.getUniqueId());
                        if (online != null) {
                            online.sendMessage(Lang.color("&aТебе выдали карту &e"
                                    + BankManager.formatCard(cardNumber) + "&a. Плати ей в разделе «Перевод»."));
                        }
                        new CardDetailMenu(plugin, cardNumber).open(player);
                    },
                    () -> new CardDetailMenu(plugin, cardNumber).open(player),
                    null, null, null).open(player);
            case 15 -> {
                plugin.bank().setCardHolder(card, player.getUniqueId());
                player.sendMessage(Lang.color("&aТы снова держатель этой карты."));
                reopen(player);
            }
            case 16 -> {
                plugin.bank().removeCard(account, cardNumber);
                player.sendMessage(lang.msg("card-removed", "number", BankManager.formatCard(cardNumber)));
                new CardMenu(plugin).open(player);
            }
            default -> {
            }
        }
    }

    private void reopen(Player player) {
        build(player);
        player.openInventory(inventory);
    }
}
