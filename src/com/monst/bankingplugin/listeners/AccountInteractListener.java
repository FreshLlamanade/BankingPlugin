package com.monst.bankingplugin.listeners;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.monst.bankingplugin.Account;
import com.monst.bankingplugin.Bank;
import com.monst.bankingplugin.BankingPlugin;
import com.monst.bankingplugin.config.Config;
import com.monst.bankingplugin.events.account.AccountCreateEvent;
import com.monst.bankingplugin.events.account.AccountInfoEvent;
import com.monst.bankingplugin.events.account.AccountRemoveEvent;
import com.monst.bankingplugin.utils.AccountUtils;
import com.monst.bankingplugin.utils.BankUtils;
import com.monst.bankingplugin.utils.Callback;
import com.monst.bankingplugin.utils.ClickType;
import com.monst.bankingplugin.utils.ClickType.InfoClickType;
import com.monst.bankingplugin.utils.ClickType.SetClickType;
import com.monst.bankingplugin.utils.ItemUtils;
import com.monst.bankingplugin.utils.Messages;
import com.monst.bankingplugin.utils.Permissions;
import com.monst.bankingplugin.utils.Utils;

import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.EconomyResponse;

public class AccountInteractListener implements Listener {
	
	private static Map<UUID, Set<Integer>> unconfirmed = new HashMap<>();

	private BankingPlugin plugin;
	private AccountUtils accountUtils;
	private BankUtils bankUtils;

	public AccountInteractListener(BankingPlugin plugin) {
		this.plugin = plugin;
		this.accountUtils = plugin.getAccountUtils();
		this.bankUtils = plugin.getBankUtils();
	}

	/**
	 * Checks every inventory interact event for an account create attempt, and
	 * handles the creation.
	 * 
	 * @param PlayerInteractEvent
	 */
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGH)
	public void onAccountInteract(PlayerInteractEvent e) {
		
		Player p = e.getPlayer();
		Block b = e.getClickedBlock();
		if (b.getType() == Material.AIR)
			return;
		Account account = accountUtils.getAccount(b.getLocation());
		ClickType clickType = ClickType.getPlayerClickType(p);

		if (!(b.getType() == Material.CHEST || b.getType() == Material.TRAPPED_CHEST))
			return;

		if (clickType != null) {

			if (clickType.getClickType() != ClickType.EnumClickType.CREATE && account == null)
				return;
			if (!(e.getAction() == Action.RIGHT_CLICK_BLOCK))
				return;

			switch (clickType.getClickType()) {

			case CREATE:

				if (e.isCancelled() && !p.hasPermission(Permissions.ACCOUNT_CREATE_PROTECTED)) {
					p.sendMessage(Messages.NO_PERMISSION_ACCOUNT_CREATE_PROTECTED);
					plugin.debug(p.getName() + " does not have permission to create an account on the selected chest.");
				} else
					tryCreate(p, b);
				ClickType.removePlayerClickType(p);
				e.setCancelled(true);
				break;

			case REMOVE:

				if (confirmRemove(p, account))
					tryRemove(p, account);
				e.setCancelled(true);
				break;

			case INFO:

				boolean verbose = ((InfoClickType) clickType).isVerbose();
				info(p, account, verbose);
				ClickType.removePlayerClickType(p);
				e.setCancelled(true);
				break;

			case SET:

				String[] args = ((SetClickType) clickType).getArgs();
				set(p, account, args);
				ClickType.removePlayerClickType(p);
				e.setCancelled(true);
				break;
			}

		} else {

			if (account == null)
				return;
			if (!(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK))
				return;
			if (p.isSneaking() && Utils.hasAxeInHand(p) && e.getAction() == Action.LEFT_CLICK_BLOCK)
				return;

			// Handles account info requests using config info item
			ItemStack infoItem = Config.accountInfoItem;

			if (infoItem != null) {
				ItemStack item = Utils.getItemInMainHand(p);
				if (item != null && infoItem.getType() == item.getType()) {
					e.setCancelled(true);
					info(p, account, p.hasPermission(Permissions.ACCOUNT_OTHER_INFO));
					return;
				}
				item = Utils.getItemInOffHand(p);
				if (item != null && infoItem.getType() == item.getType()) {
					e.setCancelled(true);
					info(p, account, p.hasPermission(Permissions.ACCOUNT_OTHER_INFO));
					return;
				}
			}

			if (e.getAction() == Action.RIGHT_CLICK_BLOCK && !p.isSneaking()) {
				e.setCancelled(true); // peek method handles the chest opening instead
				tryPeek(p, account, true);
			}
		}
	}

	/**
	 * Prevents unauthorized players from editing other players' accounts
	 * 
	 * @param InventoryClickEvent e
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent e) {

		Inventory chestInv = e.getInventory();

		if (!(chestInv.getHolder() instanceof Chest || chestInv.getHolder() instanceof DoubleChest)) {
			return;
		}

		Location loc = null;
		if (chestInv.getHolder() instanceof Chest) {
			loc = ((Chest) chestInv.getHolder()).getLocation();
		} else if (chestInv.getHolder() instanceof DoubleChest) {
			loc = ((DoubleChest) chestInv.getHolder()).getLocation();
		}

		final Account account = plugin.getAccountUtils().getAccount(loc);
		if (account == null)
			return;
		OfflinePlayer owner = account.getOwner();
		if (owner.getUniqueId().equals(e.getWhoClicked().getUniqueId()))
			return;
		if (e.getWhoClicked() instanceof Player) {
			Player executor = (Player) e.getWhoClicked();
			if (!executor.hasPermission(Permissions.ACCOUNT_OTHER_EDIT)) {
				plugin.debug(
						executor.getName() + " does not have permission to edit " + owner.getName() + "'s account");
				executor.sendMessage(Messages.NO_PERMISSION_ACCOUNT_OTHER_EDIT);
				e.setCancelled(true);
			}

		}
	}

	/**
	 * Create a new account
	 *
	 * @param executor Player, who executed the command, will receive the message
	 *                 and become the owner of the account
	 * @param location Where the account will be located
	 */
	private void tryCreate(final Player p, final Block b) {

		if (accountUtils.isAccount(b.getLocation())) {
			p.sendMessage(Messages.CHEST_ALREADY_ACCOUNT);
			plugin.debug("Chest is already an account.");
			return;
		}
		if (!ItemUtils.isTransparent(b.getRelative(BlockFace.UP))) {
			p.sendMessage(Messages.CHEST_BLOCKED);
			plugin.debug("Chest is blocked.");
			return;
		}
		if (!bankUtils.isBank(b.getLocation())) {
			p.sendMessage(Messages.CHEST_NOT_IN_BANK);
			plugin.debug("Chest is not in a bank.");
			plugin.debug(p.getName() + " is creating new account...");
			return;
		}
		if (!p.hasPermission(Permissions.ACCOUNT_CREATE)) {
			p.sendMessage(Messages.NO_PERMISSION_ACCOUNT_CREATE);
			plugin.debug(p.getName() + " is not permitted to create the account");
			return;
		}

		Location location = b.getLocation();

		double creationPrice = Config.creationPriceAccount;
		Bank bank = bankUtils.getBank(location);
		Account account = new Account(plugin, p, bank, location);

		AccountCreateEvent event = new AccountCreateEvent(p, account, creationPrice);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled() && !p.hasPermission(Permissions.ACCOUNT_CREATE_PROTECTED)) {
			plugin.debug("No permission to create account on a protected chest.");
			p.sendMessage(Messages.NO_PERMISSION_ACCOUNT_CREATE_PROTECTED);
			return;
		}

		if (creationPrice > 0) {
			if (plugin.getEconomy().getBalance(p) < creationPrice) {
				p.sendMessage(Messages.ACCOUNT_CREATE_INSUFFICIENT_FUNDS);
				return;
			}
			OfflinePlayer player = p.getPlayer();
			EconomyResponse r = plugin.getEconomy().withdrawPlayer(player, location.getWorld().getName(), creationPrice);
			if (!r.transactionSuccess()) {
				plugin.debug("Economy transaction failed: " + r.errorMessage);
				p.sendMessage(Messages.ERROR_OCCURRED);
				return;
			} else
				p.sendMessage(Messages.getWithValue(Messages.ACCOUNT_CREATE_FEE_PAID,
						BigDecimal.valueOf(r.amount).setScale(2, RoundingMode.HALF_EVEN)));
		}

		if (account.create(true)) {
			plugin.debug("Account created");
			accountUtils.addAccount(account, true, new Callback<Integer>(plugin) {
				@Override
				public void onResult(Integer result) {
					account.setDefaultNickname();
				}
			});
			bank.addAccount(account);
			p.sendMessage(Messages.ACCOUNT_CREATED);
		}

	}
	
	private boolean confirmRemove(Player executor, Account account) {
		if (!account.isOwner(executor)
				&& !executor.hasPermission(Permissions.ACCOUNT_OTHER_REMOVE)) {
			executor.sendMessage(Messages.NO_PERMISSION_ACCOUNT_OTHER_REMOVE);
			return !unconfirmed.containsKey(executor.getUniqueId());
		}
		
		boolean confirmed = unconfirmed.containsKey(executor.getUniqueId()) 
				&& unconfirmed.get(executor.getUniqueId()).contains(account.getID());
		
		if (!confirmed && (Config.confirmOnRemove || account.getBalance().signum() == 1)) {
			plugin.debug("Needs confirmation");

			if (account.getBalance().signum() == 1) {
				executor.sendMessage(Messages.ACCOUNT_BALANCE_NOT_ZERO);
			}
	        executor.sendMessage(Messages.CLICK_TO_CONFIRM);
			Set<Integer> ids = unconfirmed.containsKey(executor.getUniqueId())
					? unconfirmed.get(executor.getUniqueId())
					: new HashSet<>();
	        ids.add(account.getID());
	        unconfirmed.put(executor.getUniqueId(), ids);
			return false;
		} else {
			Set<Integer> ids = unconfirmed.containsKey(executor.getUniqueId()) ? unconfirmed.get(executor.getUniqueId())
					: new HashSet<>();
			ids.remove(account.getID());
			if (ids.isEmpty()) {
				unconfirmed.remove(executor.getUniqueId());
				ClickType.removePlayerClickType(executor);
			} else
				unconfirmed.put(executor.getUniqueId(), ids);
			return true;
		}
	}

	/**
	 * Remove a account
	 * 
	 * @param executor Player, who executed the command and will receive the message
	 * @param account  Account to be removed
	 */
	private void tryRemove(Player executor, Account account) {
		plugin.debug(executor.getName() + String.format(" is removing %s account (#", 
				account.isOwner(executor) ? "their" 
				: account.getOwner().getName() + "'s") + account.getID() + ")");
		AccountRemoveEvent event = new AccountRemoveEvent(executor, account);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			plugin.debug("Remove event cancelled (#" + account.getID() + ")");
			return;
		}

		double creationPrice = Config.creationPriceAccount;
		if (creationPrice > 0 && Config.reimburseAccountCreation
				&& account.isOwner(executor)) {
			OfflinePlayer owner = executor.getPlayer();
			EconomyResponse r = plugin.getEconomy().depositPlayer(owner, account.getLocation().getWorld().getName(),
					creationPrice);

			if (!r.transactionSuccess()) {
				plugin.debug("Economy transaction failed: " + r.errorMessage);
				executor.sendMessage(Messages.ERROR_OCCURRED);
			} else {
				executor.sendMessage(Messages.getWithValue(Messages.PLAYER_REIMBURSED,
						BigDecimal.valueOf(r.amount).setScale(2, RoundingMode.HALF_EVEN)).toString());
				executor.sendMessage(Messages.ACCOUNT_REMOVED);
			}
		} else {
			executor.sendMessage(Messages.ACCOUNT_REMOVED);
		}

		accountUtils.removeAccount(account, true);
		plugin.debug("Removed account (#" + account.getID() + ")");
	}

	/**
	 * Look into an account
	 * 
	 * @param executor Player, who executed the command and will receive the message
	 * @param account  Account to be opened
	 * @param message  Whether the player should receive the
	 *                 {@link Message#ACCOUNT_OPENED} message
	 */
	private void tryPeek(Player executor, Account account, boolean message) {
		boolean executorIsOwner = account.isOwner(executor);
		if (!executorIsOwner && !executor.hasPermission(Permissions.ACCOUNT_OTHER_VIEW)) {
			executor.sendMessage(Messages.NO_PERMISSION_ACCOUNT_OTHER_VIEW);
			plugin.debug(executor.getName() + " does not have permission to open " + account.getOwner().getName()
					+ "'s account chest.");
			return;
		}

		if (executorIsOwner)
			plugin.debug(executor.getName() + " is opening their account (#" + account.getID() + ")");
		else
			plugin.debug(executor.getName() + " is opening " + account.getOwner().getName() + "'s account (#"
					+ account.getID() + ")");

		executor.openInventory(account.getInventoryHolder().getInventory());

		if (message && !executorIsOwner)
			executor.sendMessage(Messages.getWithValue(Messages.ACCOUNT_OPENED, account.getOwner().getName()));
	}

	/**
	 *
	 * @param executor Player, who executed the command and will retrieve the
	 *                 information
	 * @param account  Account from which the information will be retrieved
	 */
	private void info(Player executor, Account account, boolean verbose) {
		boolean executorIsOwner = account.isOwner(executor);
		if (!executorIsOwner && !executor.hasPermission(Permissions.ACCOUNT_OTHER_INFO)) {
			executor.sendMessage(Messages.NO_PERMISSION_ACCOUNT_OTHER_INFO);
			return;
		}

		if (executorIsOwner)
			plugin.debug(executor.getName() + " is retrieving their account info (#" + account.getID() + ")");
		else
			plugin.debug(executor.getName() + " is retrieving " + account.getOwner().getName() + "'s account info (#"
					+ account.getID() + ")");

		AccountInfoEvent event = new AccountInfoEvent(executor, account, verbose);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			plugin.debug("Info event cancelled (#" + account.getID() + ")");
			return;
		}

		executor.sendMessage(" ");
		if (verbose) 
			executor.sendMessage(account.toStringVerbose());
		else
			executor.sendMessage(account.toString());
		executor.sendMessage(" ");
	}

	private void set(Player executor, Account account, String[] args) {
		if (args[0].equalsIgnoreCase("nickname")) {
			if (account.isOwner(executor)) {
				if (args[1].equals("")) {
					plugin.debug(executor.getName() + " has reset their account nickname (#" + account.getID() + ")");
					account.setDefaultNickname();
				} else {
					plugin.debug(executor.getName() + " has set their account nickname to \"" + args[1] + "\" (#" + account.getID() + ")");
					account.setNickname(args[1]);
				}
				executor.sendMessage(Messages.NICKNAME_SET);
			} else {
				if (executor.hasPermission(Permissions.ACCOUNT_OTHER_SET_NICKNAME)) {
					if (args[1].equals("")) {
						args[1] = ChatColor.DARK_GREEN + account.getOwner().getName() + "'s Account " + ChatColor.GRAY + "(#" + account.getID() + ")";
					}
					plugin.debug(executor.getName() + " has set " + account.getOwner().getName()
							+ "'s account nickname to \"" + args[1] + "\" (#" + account.getID() + ")");
					account.setNickname(args[1]);
					executor.sendMessage(Messages.NICKNAME_SET);
				} else {
					plugin.debug(executor.getName() + " does not have permission to change another player's account nickname");
					executor.sendMessage(Messages.NO_PERMISSION_ACCOUNT_SET_NICKNAME);
				}
			}
		} else if (args[0].equalsIgnoreCase("multiplier")) {
			if (account.isOwner(executor)) {
				if (args[1].equals("")) {
					int stage = account.getStatus().setMultiplierStage(Integer.parseInt(args[2]));
					plugin.debug(executor.getName() + " has set their account multiplier stage to " + stage + " (#" + account.getID() + ")");
					executor.sendMessage(Messages.getWithValue(Messages.MULTIPLIER_SET, account.getStatus().getRealMultiplier()));
				} else if (args[1].equals("+")) {
					int stage = account.getStatus().setMultiplierStageRelative(Integer.parseInt(args[2]));
					plugin.debug(executor.getName() + " has set their account multiplier stage to " + stage + " (#" + account.getID() + ")");
					executor.sendMessage(Messages.getWithValue(Messages.MULTIPLIER_SET, account.getStatus().getRealMultiplier()));
				} else if (args[1].equals("-")) {
					int stage = account.getStatus().setMultiplierStageRelative(Integer.parseInt(args[2]) * -1);
					plugin.debug(executor.getName() + " has set their account multiplier stage to " + stage + " (#" + account.getID() + ")");
					executor.sendMessage(Messages.getWithValue(Messages.MULTIPLIER_SET, account.getStatus().getRealMultiplier()));
				}
			} else {
				if (executor.hasPermission(Permissions.ACCOUNT_OTHER_SET_MULTIPLIER)) {
					if (args[1].equals("")) {
						account.getStatus().setMultiplierStage(Integer.parseInt(args[2]));
						plugin.debug(executor.getName() + " has set " + account.getOwner().getName()
								+ "'s account multiplier stage to " + args[2] + " (#" + account.getID() + ")");
						executor.sendMessage(Messages.getWithValue(Messages.MULTIPLIER_SET, account.getStatus().getRealMultiplier()));
					} else if (args[1].equals("+")) {
						int stage = account.getStatus().setMultiplierStageRelative(Integer.parseInt(args[2]));
						plugin.debug(executor.getName() + " has set " + account.getOwner().getName()
								+ "'s account multiplier stage to " + stage + " (#" + account.getID() + ")");
						executor.sendMessage(Messages.getWithValue(Messages.MULTIPLIER_SET, account.getStatus().getRealMultiplier()));
					} else if (args[1].equals("-")) {
						int stage = account.getStatus().setMultiplierStageRelative(Integer.parseInt(args[2]) * -1);
						plugin.debug(executor.getName() + " has set " + account.getOwner().getName()
								+ "'s account multiplier stage to " + stage + " (#" + account.getID() + ")");
						executor.sendMessage(Messages.getWithValue(Messages.MULTIPLIER_SET, account.getStatus().getRealMultiplier()));
					}
				} else {
					plugin.debug(executor.getName()
							+ " does not have permission to change another player's account multiplier");
					executor.sendMessage(Messages.NO_PERMISSION_ACCOUNT_SET_MULTIPLIER);
				}
			}
		} else if (args[0].equalsIgnoreCase("interest-delay")) {
			if (account.isOwner(executor)) {
				int delay = account.getStatus().setInterestDelay(Integer.parseInt(args[1]));
				plugin.debug(executor.getName() + " has set their account interest delay to " + delay + "(#" + account.getID() + ")");
				executor.sendMessage(Messages.INTEREST_DELAY_SET);
			} else {
				if (executor.hasPermission(Permissions.ACCOUNT_OTHER_SET_INTEREST_DELAY)) {
					int delay = account.getStatus().setInterestDelay(Integer.parseInt(args[1]));
					plugin.debug(executor.getName() + " has set " + account.getOwner().getName() + "'s account interest delay to " + delay + "(#" + account.getID() + ")");
					executor.sendMessage(Messages.INTEREST_DELAY_SET);
				} else
					executor.sendMessage(Messages.NO_PERMISSION_ACCOUNT_OTHER_SET_INTEREST_DELAY);
			}
		}
	}

	public static void clearUnconfirmed(OfflinePlayer p) {
		unconfirmed.remove(p.getUniqueId());
	}
}
