package com.monst.bankingplugin.events.control;

import com.monst.bankingplugin.BankingPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a player reloads the shops
 */
public class ReloadEvent extends ControlEvent implements Cancellable {

    private boolean cancelled;

    public ReloadEvent(BankingPlugin plugin, CommandSender sender) {
        super(plugin, sender);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }
}