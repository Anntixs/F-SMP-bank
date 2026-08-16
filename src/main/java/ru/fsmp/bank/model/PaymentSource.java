package ru.fsmp.bank.model;

/**
 * Источник оплаты: либо напрямую личный счёт (card == null),
 * либо карта, привязанная к чужому/своему счёту (с лимитом и заморозкой).
 */
public class PaymentSource {

    private final Account account;
    private final Card card;

    public PaymentSource(Account account, Card card) {
        this.account = account;
        this.card = card;
    }

    public Account getAccount() {
        return account;
    }

    public Card getCard() {
        return card;
    }

    public boolean isCard() {
        return card != null;
    }
}
