package ru.fsmp.bank.listener;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import ru.fsmp.bank.FSMPBank;
import ru.fsmp.bank.gui.PaymentMenu;
import ru.fsmp.bank.model.Account;
import ru.fsmp.bank.util.Lang;

/**
 * Платёжные таблички.
 *
 * Формат:
 *   строка 1: [Bank]            (заголовок из конфига)
 *   строка 2: номер счёта       (личный или корпоративный — куда идут деньги)
 *   строка 3: сумма             (необязательно; пусто = покупатель выбирает сам)
 *   строка 4: назначение        (необязательно, любой текст)
 */
public class SignListener implements Listener {

    private final FSMPBank plugin;

    public SignListener(FSMPBank plugin) {
        this.plugin = plugin;
    }

    private String header() {
        return plugin.getConfig().getString("sign.header", "[Bank]");
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String header = header();
        String line0 = event.getLine(0);
        if (line0 == null || !ChatColor.stripColor(line0).trim().equalsIgnoreCase(header)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("fsmpbank.sign.create")) {
            player.sendMessage(plugin.lang().msg("no-permission"));
            event.setCancelled(true);
            return;
        }

        String rawNumber = event.getLine(1) == null ? "" : event.getLine(1).replace(" ", "").trim();
        Account target = plugin.bank().getByNumber(rawNumber);
        if (target == null) {
            player.sendMessage(plugin.lang().msg("target-not-found"));
            player.sendMessage(Lang.color("&7Во 2-й строке укажи существующий номер счёта."));
            event.setCancelled(true);
            return;
        }

        String amountLine = event.getLine(2) == null ? "" : event.getLine(2).trim();
        double amount = 0.0;
        if (!amountLine.isEmpty()) {
            Double parsed = parseAmount(amountLine);
            if (parsed == null || parsed <= 0) {
                player.sendMessage(plugin.lang().msg("invalid-amount"));
                event.setCancelled(true);
                return;
            }
            amount = parsed;
        }

        // Оформляем табличку
        event.setLine(0, Lang.color("&1&l" + header));
        event.setLine(1, Lang.color("&0" + rawNumber));
        event.setLine(2, amount > 0 ? Lang.color("&2" + plugin.lang().money(amount)) : Lang.color("&8Сумма: любая"));
        String label = event.getLine(3) == null ? "" : event.getLine(3).trim();
        event.setLine(3, Lang.color(label.isEmpty() ? "&8ПКМ — оплатить" : "&0" + label));

        String targetName = target.isCorporate() && !target.getName().isEmpty()
                ? target.getName() : ("счёт " + rawNumber);
        player.sendMessage(Lang.color("&aПлатёжная табличка создана. Получатель: &e" + targetName));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Только правый клик открывает оплату. Левый не трогаем — иначе табличку
        // нельзя было бы разбить в выживании.
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) {
            return;
        }
        String line0 = sign.getLine(0);
        if (line0 == null || !ChatColor.stripColor(line0).trim().equalsIgnoreCase(header())) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!player.hasPermission("fsmpbank.use")) {
            player.sendMessage(plugin.lang().msg("no-permission"));
            return;
        }

        String rawNumber = ChatColor.stripColor(sign.getLine(1)).replace(" ", "").trim();
        Account target = plugin.bank().getByNumber(rawNumber);
        if (target == null) {
            player.sendMessage(plugin.lang().msg("target-not-found"));
            return;
        }

        if (!plugin.bank().hasPersonalAccount(player.getUniqueId())) {
            player.sendMessage(plugin.lang().msg("no-account"));
            return;
        }

        String amountLine = ChatColor.stripColor(sign.getLine(2)).trim();
        Double parsed = parseAmount(amountLine.replace(plugin.lang().getCurrencySymbol(), "").trim());
        boolean fixed = parsed != null && parsed > 0;
        double amount = fixed ? parsed : 1.0;

        String label = ChatColor.stripColor(sign.getLine(3)).trim();

        new PaymentMenu(plugin, rawNumber, label, amount, fixed).open(player);
    }

    private Double parseAmount(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(text.replace(",", ".").replace(" ", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
