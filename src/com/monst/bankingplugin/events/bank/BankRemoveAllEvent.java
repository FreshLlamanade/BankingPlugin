package com.monst.bankingplugin.events.bank;

import java.util.Collection;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.monst.bankingplugin.Bank;

public class BankRemoveAllEvent extends Event implements Cancellable {

	private HandlerList handlers;

	private boolean cancelled;
	private CommandSender sender;
	private Collection<Bank> banks;

	public BankRemoveAllEvent(CommandSender sender, Collection<Bank> banks) {
		this.sender = sender;
		this.banks = banks;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public CommandSender getSender() {
		return sender;
	}

	public Collection<Bank> getBanks() {
		return banks;
	}
}