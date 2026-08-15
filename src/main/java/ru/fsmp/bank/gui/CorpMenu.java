package ru.fsmp.bank.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.util.Items;
import ru.fsmp.bank.util.Lang;

import java.util.List;

/**
 * Список корпоративных счетов (ИП) игрока. Открытие — через ввод названия в чат.
 */
public class CorpMenu extends BankMenu {

    private List<Account> shown;

    public CorpMenu(FSMPBank plugin) {
        super(plugin);
    }

    @Override
    protected void build(Player player) {
        this.inventory = Bukkit.createInventory(this, 27, Lang.color("&aКорпоративные счета (ИП)"));
        Lang lang = plugin.lang();

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, Items.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        shown = plugin.bank().getCorporateAccounts(player.getUniqueId());
        int slot = 10;
        for (Account account : shown) {
            if (slot > 16) {
                break;
            }
            boolean owner = player.getUniqueId().equals(account.getOwner());
            inventory.setItem(slot, Items.of(Material.EMERALD_BLOCK, "&a" + account.getName(),
                    "&7Номер счёта: &e" + account.getNumber(),
                    "&7Баланс: &a" + lang.money(account.getBalance()),
                    "&7Роль: " + (owner ? "&6владелец" : "&bсотрудник"),
                    "&7Сотрудников: &e" + account.getMembers().size(),
                    "",
                    "&eНажми, чтобы открыть"));
            slot++;
        }

        inventory.setItem(22, Items.of(Material.WRITABLE_BOOK, "&2Открыть новый ИП",
                "&7Стоимость открытия: &e" + lang.money(plugin.bank().getCorpOpenCost()),
                "",
                "&eНажми и введи название в чат"));
        inventory.setItem(26, Items.of(Material.ARROW, "&7Назад"));
    }

    @Override
    public void handleClick(Player player, int slot, ClickType click) {
        if (slot == 26) {
            new MainMenu(plugin).open(player);
            return;
        }
        if (slot == 22) {
            promptCreate(player);
            return;
        }
        if (shown != null && slot >= 10 && slot <= 16) {
            int index = slot - 10;
            if (index < shown.size()) {
                new CorpAccountMenu(plugin, shown.get(index).getNumber()).open(player);
            }
        }
    }

    private void promptCreate(Player player) {
        Lang lang = plugin.lang();
        if (!player.hasPermission("fsmpbank.corp.create")) {
            player.sendMessage(lang.msg("no-permission"));
            return;
        }
        if (plugin.bank().countCorporate(player.getUniqueId()) >= plugin.bank().getMaxCorpPerPlayer()) {
            player.sendMessage(lang.msg("corp-limit", "max", String.valueOf(plugin.bank().getMaxCorpPerPlayer())));
            return;
        }
        player.closeInventory();
        player.sendMessage(Lang.color("&eВведи название ИП в чат &7(или «отмена»)&e:"));
        plugin.prompts().await(player.getUniqueId(), name -> {
            Account personal = plugin.bank().getPersonalAccount(player.getUniqueId());
            if (personal == null) {
                player.sendMessage(lang.msg("no-account"));
                return;
            }
            double cost = plugin.bank().getCorpOpenCost();
            if (cost > 0 && !personal.has(cost)) {
                player.sendMessage(lang.msg("not-enough-money"));
                return;
            }
            String trimmed = name.length() > 32 ? name.substring(0, 32) : name;
            if (cost > 0) {
                personal.withdraw(cost);
            }
            Account corp = plugin.bank().openCorporateAccount(player.getUniqueId(), trimmed);
            plugin.bank().save();
            player.sendMessage(lang.msg("corp-created", "name", trimmed, "number", corp.getNumber()));
            new CorpMenu(plugin).open(player);
        });
    }
}
