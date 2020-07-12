package com.monst.bankingplugin.listeners;

import java.math.BigDecimal;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.scheduler.BukkitRunnable;

import com.monst.bankingplugin.Account;
import com.monst.bankingplugin.BankingPlugin;
import com.monst.bankingplugin.config.Config;
import com.monst.bankingplugin.utils.AccountUtils;
import com.monst.bankingplugin.utils.Messages;
import com.monst.bankingplugin.utils.Utils;

public class AccountBalanceListener implements Listener {
	
	private BankingPlugin plugin;
	private AccountUtils accountUtils;

	public AccountBalanceListener(BankingPlugin plugin) {
		this.plugin = plugin;
		this.accountUtils = plugin.getAccountUtils();
	}

	@EventHandler
	public void onAccountInventoryClose(InventoryCloseEvent e) {
		
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!(e.getPlayer() instanceof Player) || e.getInventory() == null)
					return;
				if (!e.getInventory().getType().equals(InventoryType.CHEST))
					return;

				Location loc = e.getInventory().getLocation();

				if (accountUtils.isAccount(loc)) {
					Player executor = (Player) e.getPlayer();
					Account account = accountUtils.getAccount(loc);
					
					plugin.debug(executor.getName() + " has closed an account chest (" + account.getID() + ")");
					
					BigDecimal valueOnClose = accountUtils.appraiseAccountContents(account);

					BigDecimal difference = valueOnClose.subtract(account.getBalance());
					if (difference.signum() == 0)
						return;

					account.setBalance(valueOnClose);

					plugin.debug("Account #" + account.getID() + " has been updated with a new balance ("
							+ Utils.formatNumber(valueOnClose) + ")");

					if (difference.signum() == 1)
						executor.sendMessage(String.format(Messages.ACCOUNT_DEPOSIT, Utils.formatNumber(difference),
								(account.isOwner(executor)) ? "your" : account.getOwner().getName()));
					else
						executor.sendMessage(String.format(Messages.ACCOUNT_WITHDRAWAL, Utils.formatNumber(difference.abs()),
								(account.isOwner(executor)) ? "your" : account.getOwner().getName()));
					executor.sendMessage(String.format(Messages.ACCOUNT_NEW_BALANCE, Utils.formatNumber(valueOnClose)));

					if (difference.signum() == -1 && valueOnClose.compareTo(account.getPrevBalance()) == -1) {
						int multiplier = account.getStatus().getMultiplierStage();
						if (multiplier != account.getStatus().processWithdrawal())
							executor.sendMessage(ChatColor.GOLD + "Your multiplier has decreased to " + ChatColor.GREEN
									+ account.getStatus().getRealMultiplier() + ChatColor.GOLD + "!");
					}

					plugin.getDatabase().addAccount(account, null);

					if (account.getOwner().isOnline())
						plugin.getDatabase().logLogout(account.getOwner().getPlayer(), null);

					if (Config.enableTransactionLog) {
						TransactionType type = difference.signum() == 1 ? TransactionType.DEPOSIT : TransactionType.WITHDRAWAL;
						plugin.getDatabase().logTransaction(executor, account, difference.abs(), type, null);
						plugin.debug("Logging transaction to database...");
					}
				}
			}
		}.runTaskAsynchronously(plugin);
	}

	public enum TransactionType {
		DEPOSIT, WITHDRAWAL
	}

}

