package ru.fsmp.bank.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.model.Account;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Центральный менеджер: хранит счета, карты и банкиров, содержит всю бизнес-логику
 * и отвечает за сохранение/загрузку данных.
 */
public class BankManager {

    private static final SimpleDateFormat TS = new SimpleDateFormat("dd.MM HH:mm");

    private final FSMPBank plugin;

    private final Map<String, Account> accountsByNumber = new HashMap<>();
    private final Map<UUID, String> personalByOwner = new HashMap<>();
    private final Map<String, String> cardIndex = new HashMap<>();   // номер карты -> номер счёта
    private final Set<UUID> bankers = new HashSet<>();

    private File dataFile;
    private int maxCards;
    private int maxCorpPerPlayer;
    private int historySize;
    private double transferFee;
    private double startingBalance;
    private double corpOpenCost;

    public BankManager(FSMPBank plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    //  Загрузка настроек и данных
    // ------------------------------------------------------------------

    public void reloadSettings() {
        FileConfiguration cfg = plugin.getConfig();
        this.maxCards = cfg.getInt("account.max-cards", 3);
        this.historySize = cfg.getInt("account.history-size", 30);
        this.transferFee = cfg.getDouble("account.transfer-fee", 0.0);
        this.startingBalance = cfg.getDouble("account.starting-balance", 0.0);
        this.maxCorpPerPlayer = cfg.getInt("corporate.max-per-player", 3);
        this.corpOpenCost = cfg.getDouble("corporate.open-cost", 0.0);
    }

    public void load() {
        reloadSettings();
        accountsByNumber.clear();
        personalByOwner.clear();
        cardIndex.clear();
        bankers.clear();

        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        // Банкиры
        for (String s : data.getStringList("bankers")) {
            try {
                bankers.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Счета
        ConfigurationSection accounts = data.getConfigurationSection("accounts");
        if (accounts != null) {
            for (String number : accounts.getKeys(false)) {
                ConfigurationSection sec = accounts.getConfigurationSection(number);
                if (sec == null) {
                    continue;
                }
                try {
                    Account.Type type = Account.Type.valueOf(sec.getString("type", "PERSONAL"));
                    UUID owner = UUID.fromString(sec.getString("owner"));
                    String name = sec.getString("name", "");
                    double balance = sec.getDouble("balance", 0.0);
                    Account account = new Account(number, type, owner, name, balance);

                    account.getCards().addAll(sec.getStringList("cards"));
                    for (String m : sec.getStringList("members")) {
                        try {
                            account.getMembers().add(UUID.fromString(m));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    account.getHistory().addAll(sec.getStringList("history"));

                    register(account);
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Не удалось загрузить счёт " + number, ex);
                }
            }
        }
        plugin.getLogger().info("Загружено счетов: " + accountsByNumber.size()
                + ", банкиров: " + bankers.size());
    }

    private void register(Account account) {
        accountsByNumber.put(account.getNumber(), account);
        if (account.getType() == Account.Type.PERSONAL) {
            personalByOwner.put(account.getOwner(), account.getNumber());
        }
        for (String card : account.getCards()) {
            cardIndex.put(card, account.getNumber());
        }
    }

    public void save() {
        if (dataFile == null) {
            dataFile = new File(plugin.getDataFolder(), "data.yml");
        }
        YamlConfiguration data = new YamlConfiguration();

        List<String> bankerList = new ArrayList<>();
        for (UUID uuid : bankers) {
            bankerList.add(uuid.toString());
        }
        data.set("bankers", bankerList);

        for (Account account : accountsByNumber.values()) {
            String path = "accounts." + account.getNumber();
            data.set(path + ".type", account.getType().name());
            data.set(path + ".owner", account.getOwner().toString());
            data.set(path + ".name", account.getName());
            data.set(path + ".balance", account.getBalance());
            data.set(path + ".cards", new ArrayList<>(account.getCards()));
            List<String> members = new ArrayList<>();
            for (UUID m : account.getMembers()) {
                members.add(m.toString());
            }
            data.set(path + ".members", members);
            data.set(path + ".history", new ArrayList<>(account.getHistory()));
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            data.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить данные банка", ex);
        }
    }

    // ------------------------------------------------------------------
    //  Личные счета
    // ------------------------------------------------------------------

    public boolean hasPersonalAccount(UUID uuid) {
        return personalByOwner.containsKey(uuid);
    }

    public Account getPersonalAccount(UUID uuid) {
        String number = personalByOwner.get(uuid);
        return number == null ? null : accountsByNumber.get(number);
    }

    public Account openPersonalAccount(UUID uuid) {
        if (hasPersonalAccount(uuid)) {
            return getPersonalAccount(uuid);
        }
        String number = generateNumber(8);
        Account account = new Account(number, Account.Type.PERSONAL, uuid, "", startingBalance);
        register(account);
        log(account, "Счёт открыт");
        save();
        return account;
    }

    // ------------------------------------------------------------------
    //  Карты
    // ------------------------------------------------------------------

    public int getMaxCards() {
        return maxCards;
    }

    /** Выпускает новую карту к личному счёту. null — если достигнут лимит. */
    public String issueCard(Account account) {
        if (account.getCards().size() >= maxCards) {
            return null;
        }
        String card = generateCardNumber();
        account.getCards().add(card);
        cardIndex.put(card, account.getNumber());
        log(account, "Выпущена карта " + card);
        save();
        return card;
    }

    public boolean removeCard(Account account, String cardNumber) {
        if (account.getCards().remove(cardNumber)) {
            cardIndex.remove(cardNumber);
            log(account, "Карта " + cardNumber + " удалена");
            save();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    //  Корпоративные счета (ИП)
    // ------------------------------------------------------------------

    public int getMaxCorpPerPlayer() {
        return maxCorpPerPlayer;
    }

    public double getCorpOpenCost() {
        return corpOpenCost;
    }

    public long countCorporate(UUID owner) {
        return accountsByNumber.values().stream()
                .filter(a -> a.getType() == Account.Type.CORPORATE && owner.equals(a.getOwner()))
                .count();
    }

    public List<Account> getCorporateAccounts(UUID player) {
        List<Account> result = new ArrayList<>();
        for (Account a : accountsByNumber.values()) {
            if (a.getType() == Account.Type.CORPORATE && a.canAccess(player)) {
                result.add(a);
            }
        }
        return result;
    }

    public Account openCorporateAccount(UUID owner, String name) {
        String number = generateNumber(10);
        Account account = new Account(number, Account.Type.CORPORATE, owner, name, 0.0);
        register(account);
        log(account, "Корпоративный счёт открыт");
        save();
        return account;
    }

    // ------------------------------------------------------------------
    //  Поиск
    // ------------------------------------------------------------------

    public Account getByNumber(String number) {
        return accountsByNumber.get(number);
    }

    public Account getByCard(String cardNumber) {
        String number = cardIndex.get(cardNumber);
        return number == null ? null : accountsByNumber.get(number);
    }

    /**
     * Универсальный поиск счёта по строке: номер счёта, номер карты или имя игрока.
     */
    public Optional<Account> resolveTarget(String raw) {
        String cleaned = raw.replace(" ", "").replace("-", "");
        if (accountsByNumber.containsKey(cleaned)) {
            return Optional.of(accountsByNumber.get(cleaned));
        }
        if (cardIndex.containsKey(cleaned)) {
            return Optional.of(accountsByNumber.get(cardIndex.get(cleaned)));
        }
        // По имени игрока -> его личный счёт
        org.bukkit.OfflinePlayer offline = org.bukkit.Bukkit.getOfflinePlayerIfCached(raw);
        if (offline == null) {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer byName = org.bukkit.Bukkit.getOfflinePlayer(raw);
            offline = byName;
        }
        if (offline != null && hasPersonalAccount(offline.getUniqueId())) {
            return Optional.of(getPersonalAccount(offline.getUniqueId()));
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    //  Денежные операции
    // ------------------------------------------------------------------

    public double getTransferFee() {
        return transferFee;
    }

    public enum Result {
        OK,
        NOT_ENOUGH,
        INVALID_AMOUNT,
        SAME_ACCOUNT
    }

    /** Перевод между счетами с учётом комиссии. */
    public Result transfer(Account from, Account to, double amount, String note) {
        if (amount <= 0) {
            return Result.INVALID_AMOUNT;
        }
        if (from.getNumber().equals(to.getNumber())) {
            return Result.SAME_ACCOUNT;
        }
        double fee = Math.round(amount * transferFee * 100.0) / 100.0;
        double total = amount + fee;
        if (!from.has(total)) {
            return Result.NOT_ENOUGH;
        }
        from.withdraw(total);
        to.deposit(amount);
        String feePart = fee > 0 ? " (комиссия " + fee + ")" : "";
        log(from, "→ " + to.getNumber() + " -" + amount + feePart + (note != null ? " " + note : ""));
        log(to, "← " + from.getNumber() + " +" + amount + (note != null ? " " + note : ""));
        save();
        return Result.OK;
    }

    /** Пополнение счёта (банкир/админ/касса). */
    public Result deposit(Account account, double amount, String note) {
        if (amount <= 0) {
            return Result.INVALID_AMOUNT;
        }
        account.deposit(amount);
        log(account, "Пополнение +" + amount + (note != null ? " " + note : ""));
        save();
        return Result.OK;
    }

    /** Снятие со счёта (банкир/админ/касса). */
    public Result withdraw(Account account, double amount, String note) {
        if (amount <= 0) {
            return Result.INVALID_AMOUNT;
        }
        if (!account.has(amount)) {
            return Result.NOT_ENOUGH;
        }
        account.withdraw(amount);
        log(account, "Снятие -" + amount + (note != null ? " " + note : ""));
        save();
        return Result.OK;
    }

    public void setBalance(Account account, double amount) {
        account.setBalance(amount);
        log(account, "Баланс установлен: " + amount);
        save();
    }

    // ------------------------------------------------------------------
    //  Банкиры
    // ------------------------------------------------------------------

    public boolean isBanker(UUID uuid) {
        return bankers.contains(uuid);
    }

    public boolean addBanker(UUID uuid) {
        boolean added = bankers.add(uuid);
        if (added) {
            save();
        }
        return added;
    }

    public boolean removeBanker(UUID uuid) {
        boolean removed = bankers.remove(uuid);
        if (removed) {
            save();
        }
        return removed;
    }

    public Set<UUID> getBankers() {
        return bankers;
    }

    // ------------------------------------------------------------------
    //  Вспомогательное
    // ------------------------------------------------------------------

    private void log(Account account, String text) {
        account.addHistory(TS.format(new Date()) + " " + text, historySize);
    }

    private String generateNumber(int digits) {
        String number;
        do {
            StringBuilder sb = new StringBuilder();
            // Первая цифра не ноль
            sb.append(ThreadLocalRandom.current().nextInt(1, 10));
            for (int i = 1; i < digits; i++) {
                sb.append(ThreadLocalRandom.current().nextInt(0, 10));
            }
            number = sb.toString();
        } while (accountsByNumber.containsKey(number) || cardIndex.containsKey(number));
        return number;
    }

    private String generateCardNumber() {
        String number;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append(ThreadLocalRandom.current().nextInt(0, 10));
            }
            number = sb.toString();
        } while (cardIndex.containsKey(number) || accountsByNumber.containsKey(number));
        return number;
    }

    /** Форматирует номер карты группами по 4 для показа. */
    public static String formatCard(String card) {
        if (card == null || card.length() != 16) {
            return card;
        }
        return card.substring(0, 4) + " " + card.substring(4, 8) + " "
                + card.substring(8, 12) + " " + card.substring(12, 16);
    }
}
