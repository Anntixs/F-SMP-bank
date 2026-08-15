package ru.fsmp.bank.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.util.Lang;

import java.util.function.Consumer;

/**
 * Ловит ответ игрока в чате, когда UI ожидает ввод (номер счёта, название ИП).
 */
public class ChatListener implements Listener {

    private final FSMPBank plugin;

    public ChatListener(FSMPBank plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.prompts().has(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        Consumer<String> handler = plugin.prompts().take(player.getUniqueId());
        if (handler == null) {
            return;
        }
        // Чат-событие асинхронное — работу с миром/инвентарём делаем в основном потоке
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (message.equalsIgnoreCase("отмена") || message.equalsIgnoreCase("cancel")) {
                player.sendMessage(Lang.color("&7Отменено."));
                return;
            }
            handler.accept(message.trim());
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.prompts().cancel(event.getPlayer().getUniqueId());
    }
}
