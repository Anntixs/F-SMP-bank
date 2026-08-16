package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.manager.BankManager;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.model.Card;
import ru.fsmp.bank.util.Items;
import ru.fsmp.bank.util.Lang;

import java.util.ArrayList;
import java.util.List;

/**
 * Карты игрока: свои выпущенные (управление) и выданные ему (для оплаты).
 */
public class CardMenu extends BankMenu {

    private static final int[] OWN_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    private List<String> ownCards;

    public CardMenu(FSMPBank plugin) {
        super(plugin);
    }

    @Override
    protected void build(Player player) {
        Lang lang = plugin.lang();
        this.inventory = Bukkit.createInventory(this, 36, Lang.color("&bМои карты"));
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        Account account = plugin.bank().getPersonalAccount(player.getUniqueId());
        ownCards = new ArrayList<>(account.getCards());

        int max = plugin.bank().getMaxCards();
        for (int i = 0; i < OWN_SLOTS.length; i++) {
            int slot = OWN_SLOTS[i];
            if (i >= max) {
                break;
            }
            if (i < ownCards.size()) {
                Card card = plugin.bank().getCard(ownCards.get(i));
                inventory.setItem(slot, cardItem(card, i + 1));
            } else {
                inventory.setItem(slot, Items.of(Material.LIME_DYE, "&aВыпустить карту",
                        "&7Свободный слот",
                        "",
                        "&eНажми, чтобы выпустить"));
            }
        }

        // Выданные мне карты (можно платить с чужого счёта)
        List<Card> held = new ArrayList<>();
        for (Card c : plugin.bank().getCardsHeldBy(player.getUniqueId())) {
            Account acc = plugin.bank().getByNumber(c.getAccountNumber());
            if (acc != null && !acc.getOwner().equals(player.getUniqueId())) {
                held.add(c);
            }
        }
        if (!held.isEmpty()) {
            inventory.setItem(18, Items.of(Material.NAME_TAG, "&6Выданные мне карты",
                    "&7Ими можно платить в разделе «Перевод»."));
            int slot = 19;
            for (Card c : held) {
                if (slot > 25) {
                    break;
                }
                Account acc = plugin.bank().getByNumber(c.getAccountNumber());
                inventory.setItem(slot, Items.of(Material.PAPER, "&e" + BankManager.formatCard(c.getNumber()),
                        "&7Счёт: &e" + c.getAccountNumber(),
                        "&7Владелец счёта: &e" + ownerName(acc),
                        limitLine(c),
                        c.isFrozen() ? "&cЗаморожена" : "&aАктивна"));
                slot++;
            }
        }

        inventory.setItem(31, Items.of(Material.ARROW, "&7Назад"));
    }

    private org.bukkit.inventory.ItemStack cardItem(Card card, int index) {
        List<String> lore = new ArrayList<>();
        lore.add(Lang.color("&7" + BankManager.formatCard(card.getNumber())));
        lore.add(Lang.color("&7Держатель: &e" + holderName(card)));
        lore.add(Lang.color(limitLine(card)));
        lore.add(Lang.color(card.isFrozen() ? "&cЗаморожена" : "&aАктивна"));
        lore.add("");
        lore.add(Lang.color("&eНажми — управление картой"));
        return Items.of(card.isFrozen() ? Material.RED_STAINED_GLASS_PANE : Material.PAPER,
                "&bКарта №" + index, lore);
    }

    private String limitLine(Card card) {
        if (card.getDailyLimit() <= 0) {
            return "&7Лимит: &aбез лимита";
        }
        return "&7Лимит: &e" + plugin.lang().money(card.getDailyLimit())
                + " &7(потрачено сегодня: &e" + plugin.lang().money(card.getSpentToday()) + "&7)";
    }

    private String holderName(Card card) {
        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(card.getHolder());
        return op.getName() != null ? op.getName() : card.getHolder().toString();
    }

    private String ownerName(Account account) {
        if (account == null) {
            return "?";
        }
        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(account.getOwner());
        return op.getName() != null ? op.getName() : account.getOwner().toString();
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        Lang lang = plugin.lang();
        Account account = plugin.bank().getPersonalAccount(player.getUniqueId());

        if (slot == 31) {
            new MainMenu(plugin).open(player);
            return;
        }

        int max = plugin.bank().getMaxCards();
        for (int i = 0; i < OWN_SLOTS.length && i < max; i++) {
            if (OWN_SLOTS[i] != slot) {
                continue;
            }
            if (i < ownCards.size()) {
                new CardDetailMenu(plugin, ownCards.get(i)).open(player);
            } else {
                String created = plugin.bank().issueCard(account);
                if (created == null) {
                    player.sendMessage(lang.msg("card-limit", "max", String.valueOf(max)));
                } else {
                    player.sendMessage(lang.msg("card-created", "number", BankManager.formatCard(created)));
                }
                build(player);
                player.openInventory(inventory);
            }
            return;
        }
    }
}
