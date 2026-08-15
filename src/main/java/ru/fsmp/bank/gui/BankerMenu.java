package ru.fsmp.bank.gui;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.util.Lang;

import java.util.Optional;

/**
 * UI-поток банкира: выбрать игрока (или счёт по номеру), затем через окно
 * пополнить/снять средства. Доступен только тем, кому выдана роль банкира.
 */
public final class BankerMenu {

    private BankerMenu() {
    }

    public static void open(FSMPBank plugin, Player banker) {
        new PlayerPickerMenu(plugin, "&6Банкомат: выберите игрока",
                target -> onPick(plugin, banker, target),
                () -> new MainMenu(plugin).open(banker),
                "&eПо номеру счёта", "&7Ввести номер в чат",
                () -> promptAccount(plugin, banker)
        ).open(banker);
    }

    private static void onPick(FSMPBank plugin, Player banker, OfflinePlayer target) {
        Account account = plugin.bank().getPersonalAccount(target.getUniqueId());
        if (account == null) {
            banker.sendMessage(Lang.color("&cУ игрока &e" + target.getName() + " &cнет счёта."));
            open(plugin, banker);
            return;
        }
        new BankerActionMenu(plugin, account.getNumber(), target.getName()).open(banker);
    }

    private static void promptAccount(FSMPBank plugin, Player banker) {
        banker.closeInventory();
        banker.sendMessage(Lang.color("&eВведи номер счёта в чат &7(или «отмена»)&e:"));
        plugin.prompts().await(banker.getUniqueId(), input -> {
            Optional<Account> target = plugin.bank().resolveTarget(input);
            if (target.isEmpty()) {
                banker.sendMessage(plugin.lang().msg("target-not-found"));
                open(plugin, banker);
                return;
            }
            String label = target.get().isCorporate() && !target.get().getName().isEmpty()
                    ? target.get().getName() : ("счёт " + target.get().getNumber());
            new BankerActionMenu(plugin, target.get().getNumber(), label).open(banker);
        });
    }
}
