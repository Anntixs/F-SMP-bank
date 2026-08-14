package ru.fsmp.bank.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import ru.fsmp.bank.gui.BankMenu;

/**
 * Направляет клики по меню банка в соответствующий обработчик и запрещает
 * вытаскивать предметы из служебных окон.
 */
public class GUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof BankMenu menu)) {
            return;
        }
        // Любое взаимодействие с окном банка запрещаем (это чистое меню)
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        // Реагируем только на клики по верхнему (служебному) инвентарю
        if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
            return;
        }
        menu.handleClick(player, event.getRawSlot(), event.getClick());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof BankMenu) {
            event.setCancelled(true);
        }
    }
}
