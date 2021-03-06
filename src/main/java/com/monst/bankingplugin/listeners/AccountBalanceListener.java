package com.monst.bankingplugin.listeners;

import com.monst.bankingplugin.BankingPlugin;
import com.monst.bankingplugin.banking.Account;
import com.monst.bankingplugin.banking.AccountField;
import com.monst.bankingplugin.events.account.AccountContractEvent;
import com.monst.bankingplugin.events.account.AccountTransactionEvent;
import com.monst.bankingplugin.lang.LangUtils;
import com.monst.bankingplugin.lang.Message;
import com.monst.bankingplugin.lang.Placeholder;
import com.monst.bankingplugin.lang.Replacement;
import com.monst.bankingplugin.sql.logging.AccountTransaction;
import com.monst.bankingplugin.utils.Utils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.math.BigDecimal;

/**
 * Continuously updates account balances.
 * @see Account#calculateBalance()
 */
public class AccountBalanceListener extends BankingPluginListener {

	public AccountBalanceListener(BankingPlugin plugin) {
		super(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onAccountContract(AccountContractEvent e) {
		evaluateAccountTransaction(e.getPlayer(), e.getAccount());
	}

	@EventHandler
	public void onAccountInventoryClose(InventoryCloseEvent e) {
		if (!(e.getPlayer() instanceof Player))
			return;
		Player executor = (Player) e.getPlayer();

		Location loc = e.getInventory().getLocation();
		if (loc == null)
			return;
		Account account = accountRepo.getAt(loc.getBlock());
		if (account == null)
			return;

		evaluateAccountTransaction(executor, account);
	}

	private void evaluateAccountTransaction(Player executor, Account account) {

		BigDecimal appraisal = account.calculateBalance();
		BigDecimal balance = account.getBalance();
		BigDecimal difference = appraisal.subtract(balance);

		if (difference.signum() == 0)
			return;

		plugin.debugf("Appraised balance of account #d: %s, difference to previous: %s", account.getID(),
				Utils.format(appraisal), Utils.format(difference));

		executor.sendMessage(LangUtils.getMessage(difference.signum() > 0 ? Message.ACCOUNT_DEPOSIT : Message.ACCOUNT_WITHDRAWAL,
				new Replacement(Placeholder.AMOUNT, difference::abs),
				new Replacement(Placeholder.ACCOUNT_BALANCE, appraisal)
		));

		if (difference.signum() < 0 && appraisal.compareTo(account.getPrevBalance()) < 0)
			if (account.getMultiplierStage() != account.processWithdrawal())
				executor.sendMessage(LangUtils.getMessage(Message.MULTIPLIER_DECREASED,
						new Replacement(Placeholder.NUMBER, account::getRealMultiplier)
				));

		account.setBalance(appraisal);
		accountRepo.update(account, AccountField.BALANCE);

		plugin.debugf("Account #%d has been updated with a new balance of %s", account.getID(), Utils.format(appraisal));
		new AccountTransactionEvent(executor, account, difference, appraisal).fire();

		if (account.getOwner().isOnline())
			plugin.getDatabase().logLastSeen(account.getOwner().getPlayer());

		plugin.getDatabase().logAccountTransaction(new AccountTransaction(
				account.getID(), account.getBank().getID(), executor.getUniqueId(), executor.getName(),
				account.getBalance(), balance, difference, System.currentTimeMillis()
		));
	}

}

