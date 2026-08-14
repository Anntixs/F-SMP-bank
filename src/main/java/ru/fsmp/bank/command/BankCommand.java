package ru.fsmp.bank.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.gui.MainMenu;
import ru.fsmp.bank.manager.BankManager;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.util.Lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Игровая команда /bank.
 */
public class BankCommand implements CommandExecutor, TabCompleter {

    private final FSMPBank plugin;

    public BankCommand(FSMPBank plugin) {
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang().msg("players-only"));
            return true;
        }
        if (!player.hasPermission("fsmpbank.use")) {
            player.sendMessage(lang().msg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            if (!bank().hasPersonalAccount(player.getUniqueId())) {
                player.sendMessage(lang().msg("no-account"));
                return true;
            }
            new MainMenu(plugin).open(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help", "?" -> sendHelp(player);
            case "open", "открыть" -> openAccount(player);
            case "balance", "bal", "баланс" -> balance(player);
            case "card", "карта", "карты" -> card(player, args);
            case "pay", "transfer", "перевод", "оплатить" -> pay(player, args);
            case "corp", "ип", "корп" -> corp(player, args);
            case "deposit", "пополнить" -> tellerDeposit(player, args);
            case "withdraw", "снять" -> tellerWithdraw(player, args);
            case "history", "история" -> history(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    // ------------------------------------------------------------------

    private void sendHelp(Player p) {
        p.sendMessage(Lang.color("&8&m----------&r &6Банк F-SMP &8&m----------"));
        p.sendMessage(Lang.color("&e/bank &7— открыть меню банка"));
        p.sendMessage(Lang.color("&e/bank open &7— открыть счёт"));
        p.sendMessage(Lang.color("&e/bank balance &7— посмотреть баланс"));
        p.sendMessage(Lang.color("&e/bank card [new|remove <номер>] &7— карты"));
        p.sendMessage(Lang.color("&e/bank pay <игрок|счёт|карта> <сумма> &7— перевод/оплата"));
        p.sendMessage(Lang.color("&e/bank corp create <название> &7— открыть ИП"));
        p.sendMessage(Lang.color("&e/bank corp deposit <счёт> <сумма> &7— пополнить ИП"));
        p.sendMessage(Lang.color("&e/bank corp withdraw <счёт> <сумма> &7— снять с ИП"));
        p.sendMessage(Lang.color("&e/bank corp addmember <счёт> <игрок> &7— добавить сотрудника"));
        p.sendMessage(Lang.color("&e/bank history &7— последние операции"));
        if (p.hasPermission("fsmpbank.banker")) {
            p.sendMessage(Lang.color("&6[Банкир] &e/bank deposit <игрок> <сумма> &7— выдать наличные на счёт"));
            p.sendMessage(Lang.color("&6[Банкир] &e/bank withdraw <игрок> <сумма> &7— принять наличные со счёта"));
        }
        p.sendMessage(Lang.color("&7Табличка-терминал: &f[Bank]&7 / номер счёта / сумма / назначение"));
    }

    private void openAccount(Player p) {
        if (bank().hasPersonalAccount(p.getUniqueId())) {
            Account acc = bank().getPersonalAccount(p.getUniqueId());
            p.sendMessage(lang().msg("account-exists", "number", acc.getNumber()));
            return;
        }
        Account acc = bank().openPersonalAccount(p.getUniqueId());
        p.sendMessage(lang().msg("account-created", "number", acc.getNumber()));
    }

    private Account requireAccount(Player p) {
        if (!bank().hasPersonalAccount(p.getUniqueId())) {
            p.sendMessage(lang().msg("no-account"));
            return null;
        }
        return bank().getPersonalAccount(p.getUniqueId());
    }

    private void balance(Player p) {
        Account acc = requireAccount(p);
        if (acc == null) {
            return;
        }
        p.sendMessage(lang().msg("balance", "number", acc.getNumber(),
                "balance", lang().money(acc.getBalance())));
        for (Account corp : bank().getCorporateAccounts(p.getUniqueId())) {
            p.sendMessage(Lang.color("&a" + corp.getName() + " &7(" + corp.getNumber() + "): &a"
                    + lang().money(corp.getBalance())));
        }
    }

    private void card(Player p, String[] args) {
        Account acc = requireAccount(p);
        if (acc == null) {
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("new")) {
            String card = bank().issueCard(acc);
            if (card == null) {
                p.sendMessage(lang().msg("card-limit", "max", String.valueOf(bank().getMaxCards())));
            } else {
                p.sendMessage(lang().msg("card-created", "number", BankManager.formatCard(card)));
            }
            return;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("remove")) {
            String number = args[2].replace(" ", "").replace("-", "");
            if (bank().removeCard(acc, number)) {
                p.sendMessage(lang().msg("card-removed", "number", BankManager.formatCard(number)));
            } else {
                p.sendMessage(Lang.color("&cКарта не найдена среди твоих."));
            }
            return;
        }
        // список карт
        p.sendMessage(Lang.color("&6Твои карты (&e" + acc.getCards().size() + "/"
                + bank().getMaxCards() + "&6):"));
        if (acc.getCards().isEmpty()) {
            p.sendMessage(Lang.color("&7Карт нет. Выпусти: &e/bank card new"));
        }
        for (String c : acc.getCards()) {
            p.sendMessage(Lang.color("&7• &e" + BankManager.formatCard(c)));
        }
    }

    private void pay(Player p, String[] args) {
        Account from = requireAccount(p);
        if (from == null) {
            return;
        }
        if (args.length < 3) {
            p.sendMessage(Lang.color("&cИспользование: /bank pay <игрок|счёт|карта> <сумма>"));
            return;
        }
        Double amount = parseAmount(args[2]);
        if (amount == null || amount <= 0) {
            p.sendMessage(lang().msg("invalid-amount"));
            return;
        }
        Optional<Account> targetOpt = bank().resolveTarget(args[1]);
        if (targetOpt.isEmpty()) {
            p.sendMessage(lang().msg("target-not-found"));
            return;
        }
        Account target = targetOpt.get();
        BankManager.Result result = bank().transfer(from, target, amount, "перевод");
        switch (result) {
            case OK -> {
                p.sendMessage(lang().msg("transfer-sent",
                        "amount", lang().money(amount), "target", target.getNumber()));
                Player receiver = Bukkit.getPlayer(target.getOwner());
                if (receiver != null && receiver.isOnline() && !receiver.equals(p)) {
                    receiver.sendMessage(lang().msg("transfer-received",
                            "amount", lang().money(amount), "from", from.getNumber()));
                }
            }
            case NOT_ENOUGH -> p.sendMessage(lang().msg("not-enough-money"));
            case INVALID_AMOUNT -> p.sendMessage(lang().msg("invalid-amount"));
            case SAME_ACCOUNT -> p.sendMessage(Lang.color("&cНельзя переводить самому себе."));
        }
    }

    // ------------------------------------------------------------------
    //  Корпоративные счета
    // ------------------------------------------------------------------

    private void corp(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(Lang.color("&cИспользование: /bank corp <create|list|info|deposit|withdraw|addmember|removemember>"));
            return;
        }
        String action = args[1].toLowerCase();
        switch (action) {
            case "create", "открыть" -> corpCreate(p, args);
            case "list", "список" -> corpList(p);
            case "info", "инфо" -> corpInfo(p, args);
            case "deposit", "пополнить" -> corpMove(p, args, true);
            case "withdraw", "снять" -> corpMove(p, args, false);
            case "addmember", "add" -> corpMember(p, args, true);
            case "removemember", "remove" -> corpMember(p, args, false);
            default -> p.sendMessage(Lang.color("&cНеизвестное действие для ИП."));
        }
    }

    private void corpCreate(Player p, String[] args) {
        if (!p.hasPermission("fsmpbank.corp.create")) {
            p.sendMessage(lang().msg("no-permission"));
            return;
        }
        if (args.length < 3) {
            p.sendMessage(Lang.color("&cУкажи название: /bank corp create <название>"));
            return;
        }
        if (bank().countCorporate(p.getUniqueId()) >= bank().getMaxCorpPerPlayer()) {
            p.sendMessage(lang().msg("corp-limit", "max", String.valueOf(bank().getMaxCorpPerPlayer())));
            return;
        }
        double cost = bank().getCorpOpenCost();
        Account personal = requireAccount(p);
        if (personal == null) {
            return;
        }
        if (cost > 0 && !personal.has(cost)) {
            p.sendMessage(lang().msg("not-enough-money"));
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (name.length() > 32) {
            name = name.substring(0, 32);
        }
        if (cost > 0) {
            personal.withdraw(cost);
        }
        Account corp = bank().openCorporateAccount(p.getUniqueId(), name);
        bank().save();
        p.sendMessage(lang().msg("corp-created", "name", name, "number", corp.getNumber()));
        p.sendMessage(Lang.color("&7Скидывать деньги на ИП: &e/bank pay " + corp.getNumber() + " <сумма>"));
    }

    private void corpList(Player p) {
        List<Account> list = bank().getCorporateAccounts(p.getUniqueId());
        if (list.isEmpty()) {
            p.sendMessage(Lang.color("&7У тебя нет корпоративных счетов. Открой: &e/bank corp create <название>"));
            return;
        }
        p.sendMessage(Lang.color("&6Твои ИП:"));
        for (Account a : list) {
            boolean owner = p.getUniqueId().equals(a.getOwner());
            p.sendMessage(Lang.color("&a" + a.getName() + " &7(" + a.getNumber() + ") &7баланс &a"
                    + lang().money(a.getBalance()) + " &8" + (owner ? "владелец" : "сотрудник")));
        }
    }

    private Account requireCorp(Player p, String number) {
        Account acc = bank().getByNumber(number);
        if (acc == null || !acc.isCorporate()) {
            p.sendMessage(lang().msg("target-not-found"));
            return null;
        }
        if (!acc.canAccess(p.getUniqueId())) {
            p.sendMessage(Lang.color("&cУ тебя нет доступа к этому ИП."));
            return null;
        }
        return acc;
    }

    private void corpInfo(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(Lang.color("&cУкажи номер счёта: /bank corp info <счёт>"));
            return;
        }
        Account acc = requireCorp(p, args[2]);
        if (acc == null) {
            return;
        }
        p.sendMessage(Lang.color("&6ИП &a" + acc.getName()));
        p.sendMessage(Lang.color("&7Номер счёта: &e" + acc.getNumber()));
        p.sendMessage(Lang.color("&7Баланс: &a" + lang().money(acc.getBalance())));
        OfflinePlayer owner = Bukkit.getOfflinePlayer(acc.getOwner());
        p.sendMessage(Lang.color("&7Владелец: &e" + (owner.getName() != null ? owner.getName() : acc.getOwner())));
        p.sendMessage(Lang.color("&7Сотрудников: &e" + acc.getMembers().size()));
    }

    private void corpMove(Player p, String[] args, boolean deposit) {
        if (args.length < 4) {
            p.sendMessage(Lang.color("&cИспользование: /bank corp "
                    + (deposit ? "deposit" : "withdraw") + " <счёт> <сумма>"));
            return;
        }
        Account corp = requireCorp(p, args[2]);
        if (corp == null) {
            return;
        }
        Account personal = requireAccount(p);
        if (personal == null) {
            return;
        }
        Double amount = parseAmount(args[3]);
        if (amount == null || amount <= 0) {
            p.sendMessage(lang().msg("invalid-amount"));
            return;
        }
        Account from = deposit ? personal : corp;
        Account to = deposit ? corp : personal;
        BankManager.Result result = bank().transfer(from, to, amount,
                deposit ? "взнос в ИП" : "снятие с ИП");
        switch (result) {
            case OK -> p.sendMessage(deposit
                    ? Lang.color("&aВнесено &e" + lang().money(amount) + " &aна ИП &6" + corp.getName())
                    : Lang.color("&aСнято &e" + lang().money(amount) + " &aс ИП &6" + corp.getName()));
            case NOT_ENOUGH -> p.sendMessage(lang().msg("not-enough-money"));
            default -> p.sendMessage(lang().msg("invalid-amount"));
        }
    }

    private void corpMember(Player p, String[] args, boolean add) {
        if (args.length < 4) {
            p.sendMessage(Lang.color("&cИспользование: /bank corp "
                    + (add ? "addmember" : "removemember") + " <счёт> <игрок>"));
            return;
        }
        Account corp = requireCorp(p, args[2]);
        if (corp == null) {
            return;
        }
        if (!p.getUniqueId().equals(corp.getOwner())) {
            p.sendMessage(Lang.color("&cТолько владелец ИП может управлять сотрудниками."));
            return;
        }
        OfflinePlayer targetPlayer = resolveOffline(args[3]);
        if (targetPlayer == null) {
            p.sendMessage(lang().msg("target-not-found"));
            return;
        }
        if (add) {
            corp.getMembers().add(targetPlayer.getUniqueId());
            p.sendMessage(Lang.color("&aИгрок &e" + args[3] + " &aдобавлен в ИП &6" + corp.getName()));
        } else {
            corp.getMembers().remove(targetPlayer.getUniqueId());
            p.sendMessage(Lang.color("&aИгрок &e" + args[3] + " &aубран из ИП &6" + corp.getName()));
        }
        bank().save();
    }

    // ------------------------------------------------------------------
    //  Банкир (банкомат)
    // ------------------------------------------------------------------

    private void tellerDeposit(Player p, String[] args) {
        if (!p.hasPermission("fsmpbank.banker") && !p.hasPermission("fsmpbank.admin")) {
            p.sendMessage(lang().msg("no-permission"));
            return;
        }
        if (args.length < 3) {
            p.sendMessage(Lang.color("&cИспользование: /bank deposit <игрок> <сумма>"));
            return;
        }
        Account target = resolveTargetAccount(p, args[1]);
        if (target == null) {
            return;
        }
        Double amount = parseAmount(args[2]);
        if (amount == null || amount <= 0) {
            p.sendMessage(lang().msg("invalid-amount"));
            return;
        }
        bank().deposit(target, amount, "касса " + p.getName());
        p.sendMessage(lang().msg("deposit-done", "amount", lang().money(amount), "player", args[1]));
        notify(target, lang().msg("transfer-received", "amount", lang().money(amount), "from", "касса"));
    }

    private void tellerWithdraw(Player p, String[] args) {
        if (!p.hasPermission("fsmpbank.banker") && !p.hasPermission("fsmpbank.admin")) {
            p.sendMessage(lang().msg("no-permission"));
            return;
        }
        if (args.length < 3) {
            p.sendMessage(Lang.color("&cИспользование: /bank withdraw <игрок> <сумма>"));
            return;
        }
        Account target = resolveTargetAccount(p, args[1]);
        if (target == null) {
            return;
        }
        Double amount = parseAmount(args[2]);
        if (amount == null || amount <= 0) {
            p.sendMessage(lang().msg("invalid-amount"));
            return;
        }
        BankManager.Result result = bank().withdraw(target, amount, "касса " + p.getName());
        if (result == BankManager.Result.NOT_ENOUGH) {
            p.sendMessage(lang().msg("not-enough-money"));
            return;
        }
        p.sendMessage(lang().msg("withdraw-done", "amount", lang().money(amount), "player", args[1]));
    }

    private Account resolveTargetAccount(Player p, String raw) {
        // Сначала пробуем номер счёта/карты, затем имя игрока (его личный счёт)
        Optional<Account> byRef = bank().resolveTarget(raw);
        if (byRef.isPresent()) {
            return byRef.get();
        }
        p.sendMessage(lang().msg("target-not-found"));
        return null;
    }

    private void history(Player p, String[] args) {
        Account acc;
        if (args.length >= 2) {
            acc = bank().getByNumber(args[1].replace(" ", ""));
            if (acc == null || !acc.canAccess(p.getUniqueId())) {
                p.sendMessage(lang().msg("target-not-found"));
                return;
            }
        } else {
            acc = requireAccount(p);
            if (acc == null) {
                return;
            }
        }
        p.sendMessage(Lang.color("&6История счёта &e" + acc.getNumber() + "&6:"));
        List<String> h = acc.getHistory();
        if (h.isEmpty()) {
            p.sendMessage(Lang.color("&7Операций пока нет."));
            return;
        }
        int from = Math.max(0, h.size() - 10);
        for (int i = from; i < h.size(); i++) {
            p.sendMessage(Lang.color("&7" + h.get(i)));
        }
    }

    // ------------------------------------------------------------------

    private void notify(Account account, String message) {
        Player owner = Bukkit.getPlayer(account.getOwner());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(message);
        }
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

    // ------------------------------------------------------------------
    //  Автодополнение
    // ------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("open", "balance", "card", "pay", "corp", "history", "help")) {
                if (s.startsWith(args[0].toLowerCase())) {
                    result.add(s);
                }
            }
            if (sender.hasPermission("fsmpbank.banker")) {
                for (String s : Arrays.asList("deposit", "withdraw")) {
                    if (s.startsWith(args[0].toLowerCase())) {
                        result.add(s);
                    }
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("card")) {
            result.addAll(Arrays.asList("new", "remove", "list"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("corp")) {
            result.addAll(Arrays.asList("create", "list", "info", "deposit", "withdraw", "addmember", "removemember"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("pay")
                || args[0].equalsIgnoreCase("deposit") || args[0].equalsIgnoreCase("withdraw"))) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                result.add(online.getName());
            }
        }
        return result;
    }
}
