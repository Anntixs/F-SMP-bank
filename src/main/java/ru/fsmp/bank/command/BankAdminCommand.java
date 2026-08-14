package ru.fsmp.bank.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.manager.BankManager;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.util.Lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Административная команда /bankadmin — назначение банкиров, ручное управление счетами.
 */
public class BankAdminCommand implements CommandExecutor, TabCompleter {

    private final FSMPBank plugin;

    public BankAdminCommand(FSMPBank plugin) {
        this.plugin = plugin;
    }

    private Lang lang() {
        return plugin.lang();
    }

    private BankManager bank() {
        return plugin.bank();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fsmpbank.admin")) {
            sender.sendMessage(lang().msg("no-permission"));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "addbanker" -> addBanker(sender, args);
            case "removebanker" -> removeBanker(sender, args);
            case "listbankers" -> listBankers(sender);
            case "setbalance", "set" -> setBalance(sender, args);
            case "info" -> info(sender, args);
            case "reload" -> {
                plugin.reloadAll();
                sender.sendMessage(lang().msg("reload-done"));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(Lang.color("&8&m----------&r &6Банк — админ &8&m----------"));
        s.sendMessage(Lang.color("&e/bankadmin addbanker <игрок> &7— назначить банкира"));
        s.sendMessage(Lang.color("&e/bankadmin removebanker <игрок> &7— снять банкира"));
        s.sendMessage(Lang.color("&e/bankadmin listbankers &7— список банкиров"));
        s.sendMessage(Lang.color("&e/bankadmin setbalance <игрок|счёт> <сумма> &7— задать баланс"));
        s.sendMessage(Lang.color("&e/bankadmin info <игрок|счёт> &7— информация о счёте"));
        s.sendMessage(Lang.color("&e/bankadmin reload &7— перезагрузить конфиг"));
    }

    private void addBanker(CommandSender s, String[] args) {
        if (args.length < 2) {
            s.sendMessage(Lang.color("&cИспользование: /bankadmin addbanker <игрок>"));
            return;
        }
        OfflinePlayer target = resolveOffline(args[1]);
        if (target == null) {
            s.sendMessage(lang().msg("target-not-found"));
            return;
        }
        if (bank().addBanker(target.getUniqueId())) {
            s.sendMessage(lang().msg("banker-added", "player", args[1]));
            Player online = Bukkit.getPlayer(target.getUniqueId());
            if (online != null) {
                online.sendMessage(Lang.color("&aТебя назначили банкиром F-SMP! Теперь тебе доступны "
                        + "&e/bank deposit&a и &e/bank withdraw&a."));
            }
        } else {
            s.sendMessage(Lang.color("&eЭтот игрок уже банкир."));
        }
    }

    private void removeBanker(CommandSender s, String[] args) {
        if (args.length < 2) {
            s.sendMessage(Lang.color("&cИспользование: /bankadmin removebanker <игрок>"));
            return;
        }
        OfflinePlayer target = resolveOffline(args[1]);
        if (target == null) {
            s.sendMessage(lang().msg("target-not-found"));
            return;
        }
        if (bank().removeBanker(target.getUniqueId())) {
            s.sendMessage(lang().msg("banker-removed", "player", args[1]));
        } else {
            s.sendMessage(Lang.color("&eЭтот игрок не был банкиром."));
        }
    }

    private void listBankers(CommandSender s) {
        s.sendMessage(Lang.color("&6Банкиры:"));
        if (bank().getBankers().isEmpty()) {
            s.sendMessage(Lang.color("&7Банкиров пока нет."));
            return;
        }
        for (UUID uuid : bank().getBankers()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            s.sendMessage(Lang.color("&7• &e" + (op.getName() != null ? op.getName() : uuid.toString())));
        }
    }

    private void setBalance(CommandSender s, String[] args) {
        if (args.length < 3) {
            s.sendMessage(Lang.color("&cИспользование: /bankadmin setbalance <игрок|счёт> <сумма>"));
            return;
        }
        Account acc = resolve(args[1]);
        if (acc == null) {
            s.sendMessage(lang().msg("target-not-found"));
            return;
        }
        Double amount = parseAmount(args[2]);
        if (amount == null || amount < 0) {
            s.sendMessage(lang().msg("invalid-amount"));
            return;
        }
        bank().setBalance(acc, amount);
        s.sendMessage(Lang.color("&aБаланс счёта &e" + acc.getNumber() + " &aустановлен: &e"
                + lang().money(amount)));
    }

    private void info(CommandSender s, String[] args) {
        if (args.length < 2) {
            s.sendMessage(Lang.color("&cИспользование: /bankadmin info <игрок|счёт>"));
            return;
        }
        Account acc = resolve(args[1]);
        if (acc == null) {
            s.sendMessage(lang().msg("target-not-found"));
            return;
        }
        s.sendMessage(Lang.color("&6Счёт &e" + acc.getNumber() + " &7(" + acc.getType() + ")"));
        if (acc.isCorporate()) {
            s.sendMessage(Lang.color("&7Название: &a" + acc.getName()));
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(acc.getOwner());
        s.sendMessage(Lang.color("&7Владелец: &e" + (owner.getName() != null ? owner.getName() : acc.getOwner())));
        s.sendMessage(Lang.color("&7Баланс: &a" + lang().money(acc.getBalance())));
        s.sendMessage(Lang.color("&7Карт: &e" + acc.getCards().size()));
        s.sendMessage(Lang.color("&7Сотрудников: &e" + acc.getMembers().size()));
    }

    private Account resolve(String raw) {
        Optional<Account> opt = bank().resolveTarget(raw);
        return opt.orElse(null);
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer resolveOffline(String name) {
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null) {
            return cached;
        }
        return Bukkit.getOfflinePlayer(name);
    }

    private Double parseAmount(String text) {
        try {
            double value = Double.parseDouble(text.replace(",", ".").replace(" ", ""));
            return Math.round(value * 100.0) / 100.0;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("addbanker", "removebanker", "listbankers",
                    "setbalance", "info", "reload")) {
                if (s.startsWith(args[0].toLowerCase())) {
                    result.add(s);
                }
            }
        } else if (args.length == 2) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                result.add(online.getName());
            }
        }
        return result;
    }
}
