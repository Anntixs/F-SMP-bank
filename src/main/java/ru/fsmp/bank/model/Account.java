package ru.fsmp.bank.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Банковский счёт. Может быть личным (PERSONAL) или корпоративным (CORPORATE, ИП).
 */
public class Account {

    public enum Type {
        PERSONAL,
        CORPORATE
    }

    private final String number;
    private final Type type;
    private UUID owner;
    private String name;            // отображаемое имя (для ИП)
    private double balance;
    private final List<String> cards = new ArrayList<>();      // номера карт (только для личного счёта)
    private final Set<UUID> members = new LinkedHashSet<>();    // совладельцы (для ИП)
    private final List<String> history = new ArrayList<>();     // журнал операций

    public Account(String number, Type type, UUID owner, String name, double balance) {
        this.number = number;
        this.type = type;
        this.owner = owner;
        this.name = name;
        this.balance = balance;
    }

    public String getNumber() {
        return number;
    }

    public Type getType() {
        return type;
    }

    public boolean isCorporate() {
        return type == Type.CORPORATE;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = round(balance);
    }

    public void deposit(double amount) {
        this.balance = round(this.balance + amount);
    }

    public void withdraw(double amount) {
        this.balance = round(this.balance - amount);
    }

    public boolean has(double amount) {
        return this.balance + 1.0E-9 >= amount;
    }

    public List<String> getCards() {
        return cards;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    /** Есть ли у игрока доступ к счёту (владелец или совладелец). */
    public boolean canAccess(UUID uuid) {
        return uuid != null && (uuid.equals(owner) || members.contains(uuid));
    }

    public List<String> getHistory() {
        return history;
    }

    public void addHistory(String line, int max) {
        history.add(line);
        while (history.size() > max) {
            history.remove(0);
        }
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
