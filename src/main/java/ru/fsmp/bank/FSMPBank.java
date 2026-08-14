package ru.fsmp.bank;

import org.bukkit.plugin.java.JavaPlugin;
import ru.fsmp.bank.command.BankAdminCommand;
import ru.fsmp.bank.command.BankCommand;
import ru.fsmp.bank.listener.GUIListener;
import ru.fsmp.bank.listener.SignListener;
import ru.fsmp.bank.manager.BankManager;
import ru.fsmp.bank.util.Lang;

import java.util.Objects;

/**
 * Банковская система F-SMP.
 *
 * Возможности:
 *  - личный счёт и до трёх карт;
 *  - переводы и оплата по номеру счёта/карты или имени игрока;
 *  - корпоративные счета (ИП): пополнение по номеру и платёжные таблички-терминалы;
 *  - роль банкира — «банкомат» для пополнения/снятия наличных;
 *  - администрирование: назначение банкиров, ручное управление счетами.
 */
public final class FSMPBank extends JavaPlugin {

    private BankManager bank;
    private Lang lang;
    private int autosaveTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.lang = new Lang(getConfig());
        this.bank = new BankManager(this);
        this.bank.load();

        BankCommand bankCommand = new BankCommand(this);
        Objects.requireNonNull(getCommand("bank")).setExecutor(bankCommand);
        Objects.requireNonNull(getCommand("bank")).setTabCompleter(bankCommand);

        BankAdminCommand adminCommand = new BankAdminCommand(this);
        Objects.requireNonNull(getCommand("bankadmin")).setExecutor(adminCommand);
        Objects.requireNonNull(getCommand("bankadmin")).setTabCompleter(adminCommand);

        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new SignListener(this), this);

        // Автосохранение каждые 5 минут (6000 тиков)
        this.autosaveTaskId = getServer().getScheduler().runTaskTimer(this,
                () -> bank.save(), 6000L, 6000L).getTaskId();

        getLogger().info("FSMPBank включён.");
    }

    @Override
    public void onDisable() {
        if (autosaveTaskId != -1) {
            getServer().getScheduler().cancelTask(autosaveTaskId);
        }
        if (bank != null) {
            bank.save();
        }
        getLogger().info("FSMPBank выключен, данные сохранены.");
    }

    /** Перезагрузить конфигурацию и настройки. */
    public void reloadAll() {
        reloadConfig();
        lang.reload(getConfig());
        bank.reloadSettings();
    }

    public BankManager bank() {
        return bank;
    }

    public Lang lang() {
        return lang;
    }
}
