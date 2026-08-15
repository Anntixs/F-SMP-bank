package ru.fsmp.bank.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Ожидание ввода из чата (для полей, которые нельзя выбрать кликом:
 * номер счёта, название ИП). Запускается из UI, результат приходит в обработчик.
 */
public class PromptManager {

    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public void await(UUID player, Consumer<String> handler) {
        pending.put(player, handler);
    }

    public boolean has(UUID player) {
        return pending.containsKey(player);
    }

    public Consumer<String> take(UUID player) {
        return pending.remove(player);
    }

    public void cancel(UUID player) {
        pending.remove(player);
    }
}
