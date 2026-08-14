package ru.fsmp.bank.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Локализация, цвета и форматирование сумм.
 */
public class Lang {

    private FileConfiguration config;
    private String prefix;
    private String currencySymbol;
    private String currencyName;

    private static final DecimalFormat MONEY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator('.');
        MONEY_FORMAT = new DecimalFormat("#,##0.##", symbols);
    }

    public Lang(FileConfiguration config) {
        reload(config);
    }

    public void reload(FileConfiguration config) {
        this.config = config;
        this.prefix = color(config.getString("messages.prefix", "&8[&6Банк&8] &7"));
        this.currencySymbol = config.getString("currency.symbol", "⛃");
        this.currencyName = config.getString("currency.name", "коинов");
    }

    /** Возвращает готовое сообщение по ключу с подстановками (пары ключ/значение). */
    public String msg(String key, String... replacements) {
        String raw = config.getString("messages." + key, "&c[missing: " + key + "]");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return prefix + color(raw);
    }

    /** То же, но без префикса (для строк меню, заголовков и т.п.). */
    public String raw(String key, String... replacements) {
        String value = config.getString("messages." + key, "&c[missing: " + key + "]");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            value = value.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return color(value);
    }

    public String money(double amount) {
        return MONEY_FORMAT.format(amount) + " " + currencySymbol;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
