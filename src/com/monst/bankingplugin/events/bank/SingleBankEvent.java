package com.monst.bankingplugin.events.bank;

import org.bukkit.command.CommandSender;

import com.monst.bankingplugin.Bank;

public abstract class SingleBankEvent extends BankEvent {

	private final Bank bank;
	
	public SingleBankEvent(CommandSender sender, Bank bank) {
		super(sender);
		this.bank = bank;
	}
	
	public Bank getBank() {
		return bank;
	}
	
}