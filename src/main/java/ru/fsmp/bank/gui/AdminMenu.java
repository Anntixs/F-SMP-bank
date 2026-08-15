package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.util.Items;
import ru.fsmp.bank.util.Lang;

import java.util.List;

/**
 * Административное меню банка (только для fsmpbank.admin):
 * назначение банкиров и установка баланса — всё через UI.
 */
public class AdminMenu extends BankMenu {

    public AdminMenu(FSMPBank plugin) {
        super(plugin);
    }

    @Override
    protected void build(Player player) {
        this.inventory = Bukkit.createInventory(this, 27, Lang.color("&4Банк — администрирование"));
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }
        inventory.setItem(11, Items.of(Material.GOLD_BLOCK, "&6Банкиры",
                "&7Назначить или снять роль банкира.",
                "&7Сейчас банкиров: &e" + plugin.bank().getBankers().size(),
                "",
                "&eНажми, чтобы выбрать игрока"));
        inventory.setItem(15, Items.of(Material.DIAMOND_ORE, "&bЗадать баланс",
                "&7Вручную установить баланс счёта игрока.",
                "",
                "&eНажми, чтобы выбрать игрока"));
        inventory.setItem(22, Items.of(Material.ARROW, "&7Назад"));
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        switch (slot) {
            case 11 -> openBankerPicker(player);
            case 15 -> openBalancePicker(player);
            case 22 -> new MainMenu(plugin).open(player);
            default -> {
            }
        }
    }

    private void openBankerPicker(Player admin) {
        new PlayerPickerMenu(plugin, "&6Назначить/снять банкира",
                target -> {
                    boolean nowBanker;
                    if (plugin.bank().isBanker(target.getUniqueId())) {
                        plugin.bank().removeBanker(target.getUniqueId());
                        nowBanker = false;
                    } else {
                        plugin.bank().addBanker(target.getUniqueId());
                        nowBanker = true;
                    }
                    admin.sendMessage(nowBanker
                            ? plugin.lang().msg("banker-added", "player", safeName(target))
                            : plugin.lang().msg("banker-removed", "player", safeName(target)));
                    Player online = Bukkit.getPlayer(target.getUniqueId());
                    if (online != null && nowBanker) {
                        online.sendMessage(Lang.color("&aТебе выдали роль банкира! Открой &e/bank &aи нажми «Банкир»."));
                    }
                    openBankerPicker(admin);
                },
                () -> new AdminMenu(plugin).open(admin),
                null, null, null
        ).open(admin);
    }

    private void openBalancePicker(Player admin) {
        new PlayerPickerMenu(plugin, "&bЗадать баланс: игрок",
                target -> {
                    Account account = plugin.bank().getPersonalAccount(target.getUniqueId());
                    if (account == null) {
                        admin.sendMessage(Lang.color("&cУ игрока нет счёта."));
                        openBalancePicker(admin);
                        return;
                    }
                    Lang lang = plugin.lang();
                    new AmountMenu(plugin, "&bБаланс: " + safeName(target), lang.getCurrencyMaterial(),
                            Math.max(1, (int) Math.floor(account.getBalance())), 1, 0,
                            List.of(Lang.color("&7Новый баланс счёта &e" + account.getNumber())),
                            amount -> {
                                plugin.bank().setBalance(account, amount);
                                admin.sendMessage(Lang.color("&aБаланс счёта &e" + account.getNumber()
                                        + " &aустановлен: &e" + lang.money(amount)));
                                new AdminMenu(plugin).open(admin);
                            },
                            () -> openBalancePicker(admin)
                    ).open(admin);
                },
                () -> new AdminMenu(plugin).open(admin),
                null, null, null
        ).open(admin);
    }

    private String safeName(OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }
}
