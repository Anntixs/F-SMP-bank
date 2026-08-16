package ru.fsmp.bank.model;

import java.util.UUID;

/**
 * Банковская карта — инструмент траты средств со счёта {@link #accountNumber}.
 * Держатель ({@link #holder}) может платить картой в пределах суточного лимита,
 * пока карта не заморожена. Владелец счёта управляет картой (лимит, заморозка,
 * выдача другому игроку, удаление).
 */
public class Card {

    private final String number;
    private final String accountNumber;
    private UUID holder;
    private double dailyLimit;   // 0 = без лимита
    private double spentToday;
    private long day;           // «эпоха дней» последней активности (для сброса лимита)
    private boolean frozen;

    public Card(String number, String accountNumber, UUID holder) {
        this.number = number;
        this.accountNumber = accountNumber;
        this.holder = holder;
        this.dailyLimit = 0;
        this.spentToday = 0;
        this.day = today();
        this.frozen = false;
    }

    private static long today() {
        return System.currentTimeMillis() / 86_400_000L;
    }

    /** Сбрасывает суточную трату, если наступил новый день. */
    public void resetIfNewDay() {
        long now = today();
        if (day != now) {
            day = now;
            spentToday = 0;
        }
    }

    public String getNumber() {
        return number;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public UUID getHolder() {
        return holder;
    }

    public void setHolder(UUID holder) {
        this.holder = holder;
    }

    public double getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(double dailyLimit) {
        this.dailyLimit = Math.max(0, dailyLimit);
    }

    public double getSpentToday() {
        resetIfNewDay();
        return spentToday;
    }

    public void setSpentToday(double spentToday) {
        this.spentToday = spentToday;
    }

    public long getDay() {
        return day;
    }

    public void setDay(long day) {
        this.day = day;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    /** Уложится ли трата в суточный лимит. */
    public boolean withinLimit(double amount) {
        resetIfNewDay();
        return dailyLimit <= 0 || spentToday + amount <= dailyLimit + 1.0E-9;
    }

    /** Сколько ещё можно потратить сегодня; -1 — без ограничения. */
    public double remainingToday() {
        resetIfNewDay();
        if (dailyLimit <= 0) {
            return -1;
        }
        return Math.max(0, dailyLimit - spentToday);
    }

    public void addSpent(double amount) {
        resetIfNewDay();
        spentToday += amount;
    }
}
