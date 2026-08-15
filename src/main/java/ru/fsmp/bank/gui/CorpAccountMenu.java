package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.manager.BankManager;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.util.Items;
import ru.fsmp.bank.util.Lang;

import java.util.List;

/**
 * Операции по конкретному ИП: внести/снять (между личным и корпоративным счётом),
 * управление сотрудниками (для владельца).
 */
public class CorpAccountMenu extends BankMenu {

    private final String number;

    public CorpAccountMenu(FSMPBank plugin, String number) {
        super(plugin);
        this.number = number;
    }

    @Override
    protected void build(Player player) {
        Lang lang = plugin.lang();
        this.inventory = Bukkit.createInventory(this, 27, Lang.color("&aИП"));
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        Account corp = plugin.bank().getByNumber(number);
        if (corp == null) {
            inventory.setItem(13, Items.of(Material.BARRIER, "&cСчёт не найден"));
            inventory.setItem(22, Items.of(Material.ARROW, "&7Назад"));
            return;
        }
        boolean owner = player.getUniqueId().equals(corp.getOwner());

        inventory.setItem(4, Items.of(Material.EMERALD_BLOCK, "&a" + corp.getName(),
                "&7Номер счёта: &e" + corp.getNumber(),
                "&7Баланс: &a" + lang.money(corp.getBalance()),
                "&7Сотрудников: &e" + corp.getMembers().size()));

        inventory.setItem(10, Items.of(Material.CHEST, "&aВнести на ИП",
                "&7Перевести со своего счёта на ИП."));
        inventory.setItem(12, Items.of(Material.HOPPER, "&cСнять с ИП",
                "&7Перевести с ИП на свой счёт."));
        inventory.setItem(14, Items.of(lang.getCurrencyMaterial(), "&6Как пополнять",
                "&7По номеру счёта: &e" + corp.getNumber(),
                "&7Переводом или платёжной табличкой [Bank]."));

        if (owner) {
            inventory.setItem(16, Items.of(Material.PLAYER_HEAD, "&bСотрудники",
                    "&7Добавить или убрать сотрудника.",
                    "&7Сейчас: &e" + corp.getMembers().size(),
                    "",
                    "&eНажми, чтобы выбрать игрока"));
        }

        inventory.setItem(22, Items.of(Material.ARROW, "&7Назад"));
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        Account corp = plugin.bank().getByNumber(number);
        if (corp == null || slot == 22) {
            new CorpMenu(plugin).open(player);
            return;
        }
        switch (slot) {
            case 10 -> move(player, corp, true);
            case 12 -> move(player, corp, false);
            case 16 -> {
                if (player.getUniqueId().equals(corp.getOwner())) {
                    openMembers(player);
                }
            }
            default -> {
            }
        }
    }

    private void move(Player player, Account corp, boolean deposit) {
        Lang lang = plugin.lang();
        Account personal = plugin.bank().getPersonalAccount(player.getUniqueId());
        if (personal == null) {
            player.sendMessage(lang.msg("no-account"));
            return;
        }
        int max = deposit ? (int) Math.floor(personal.getBalance()) : (int) Math.floor(corp.getBalance());
        if (max <= 0) {
            player.sendMessage(lang.msg("not-enough-money"));
            return;
        }
        String title = deposit ? "&aВнести на ИП" : "&cСнять с ИП";
        new AmountMenu(plugin, title, lang.getCurrencyMaterial(), max, 1, max,
                List.of(Lang.color("&7ИП: &e" + corp.getName())),
                amount -> {
                    Account from = deposit ? personal : corp;
                    Account to = deposit ? corp : personal;
                    BankManager.Result result = plugin.bank().transfer(from, to, amount,
                            deposit ? "взнос в ИП" : "снятие с ИП");
                    if (result == BankManager.Result.OK) {
                        player.sendMessage(deposit
                                ? Lang.color("&aВнесено &e" + lang.money(amount) + " &aна ИП.")
                                : Lang.color("&aСнято &e" + lang.money(amount) + " &aс ИП."));
                    } else {
                        player.sendMessage(lang.msg("not-enough-money"));
                    }
                    new CorpAccountMenu(plugin, number).open(player);
                },
                () -> new CorpAccountMenu(plugin, number).open(player)
        ).open(player);
    }

    private void openMembers(Player owner) {
        new PlayerPickerMenu(plugin, "&bСотрудники ИП",
                target -> {
                    Account corp = plugin.bank().getByNumber(number);
                    if (corp == null) {
                        return;
                    }
                    if (target.getUniqueId().equals(corp.getOwner())) {
                        owner.sendMessage(Lang.color("&cВладелец и так имеет полный доступ."));
                    } else if (corp.getMembers().remove(target.getUniqueId())) {
                        owner.sendMessage(Lang.color("&aИгрок &e" + safe(target) + " &aубран из ИП."));
                    } else {
                        corp.getMembers().add(target.getUniqueId());
                        owner.sendMessage(Lang.color("&aИгрок &e" + safe(target) + " &aдобавлен в ИП."));
                    }
                    plugin.bank().save();
                    openMembers(owner);
                },
                () -> new CorpAccountMenu(plugin, number).open(owner),
                null, null, null
        ).open(owner);
    }

    private String safe(OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }
}
