package ru.fsmp.bank.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import ru.fsmp.bank.FSMPBank;

/**
 * Базовое меню банка. Использует InventoryHolder, чтобы кликами занимался
 * единый слушатель, безопасно определяя тип открытого окна.
 */
public abstract class BankMenu implements InventoryHolder {

    protected final FSMPBank plugin;
    protected Inventory inventory;

    protected BankMenu(FSMPBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Открыть меню игроку. */
    public void open(Player player) {
        build(player);
        player.openInventory(inventory);
    }

    /** Собрать содержимое инвентаря. */
    protected abstract void build(Player player);

    /** Обработать клик по слоту. */
    public abstract void handleClick(Player player, int slot, ClickType click);
}
