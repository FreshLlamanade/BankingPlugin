package com.monst.bankingplugin.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.earth2me.essentials.Essentials;
import com.monst.bankingplugin.Account;
import com.monst.bankingplugin.Bank;
import com.monst.bankingplugin.BankingPlugin;
import com.monst.bankingplugin.config.Config;
import com.monst.bankingplugin.events.account.AccountRemoveAllEvent;

import net.milkbowl.vault.economy.EconomyResponse;

public class AccountUtils {

	private final BankingPlugin plugin;

	private final Map<Location, Account> accountLocationMap = new ConcurrentHashMap<>();

    public AccountUtils(BankingPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Get the account at a given location
     *
     * @param location Location of the account
     * @return Account at the given location or <b>null</b> if no account is found there
     */
	public Account getAccount(Location location) { // XXX
		if (location == null)
			return null;
		return accountLocationMap.get(Utils.blockifyLocation(location));
    }

    /**
     * Checks whether there is a account at a given location
     * @param location Location to check
     * @return Whether there is a account at the given location
     */
    public boolean isAccount(Location location) {
        return getAccount(location) != null;
    }

    /**
     * Get all accounts
     * Do not use for removing while iterating!
     *
     * @see #getAccountsCopy()
     * @return Read-only collection of all accounts, may contain duplicates
     */
    public Collection<Account> getAccounts() {
		return accountLocationMap.values().stream().distinct().collect(Collectors.toSet());
    }

    /**
     * Get all accounts
     * Same as {@link #getAccounts()} but this is safe to remove while iterating
     *
     * @see #getAccounts()
     * @return Copy of collection of all accounts, may contain duplicates
     */
    public Collection<Account> getAccountsCopy() {
		return Collections.unmodifiableCollection(getAccounts());
    }

	public Collection<Account> getPlayerAccountsCopy(OfflinePlayer owner) {
		return getAccounts().stream().filter(account -> account.isOwner(owner))
				.collect(Collectors.toList());
    }

	public Collection<Account> getBankAccountsCopy(Bank bank) {
		return getAccounts().stream().filter(account -> account.getBank().equals(bank)).collect(Collectors.toList());
	}

    /**
     * Add a account
     * @param account Account to add
     * @param addToDatabase Whether the account should also be added to the database
     * @param callback Callback that - if succeeded - returns the ID the account had or was given (as {@code int})
     */
    public void addAccount(Account account, boolean addToDatabase, Callback<Integer> callback) {
        InventoryHolder ih = account.getInventoryHolder();
        plugin.debug("Adding account... (#" + account.getID() + ")");

		if (!account.getBank().getAccounts().contains(account))
			account.getBank().addAccount(account);
		else
			plugin.debug("Bank already contained account #" + account.getID());

        if (ih instanceof DoubleChest) {
			DoubleChest dc = (DoubleChest) ih;
			Chest l = (Chest) dc.getLeftSide();
			Chest r = (Chest) dc.getRightSide();

			plugin.debug("Added account as double chest. (#" + account.getID() + ")");

			accountLocationMap.put(r.getLocation(), account);
			accountLocationMap.put(l.getLocation(), account);
        } else {
            plugin.debug("Added account as single chest. (#" + account.getID() + ")");

            accountLocationMap.put(account.getLocation(), account);
        }

        if (addToDatabase) {
			plugin.getDatabase().addAccount(account, callback);
        } else {
			if (callback != null)
				callback.callSyncResult(account.getID());
        }
    }

    /**
     * Add a account
     * @param account Account to add
     * @param addToDatabase Whether the account should also be added to the database
     */
    public void addAccount(Account account, boolean addToDatabase) {
        addAccount(account, addToDatabase, null);
    }

    /** Remove a account. May not work properly if double chest doesn't exist!
     * @param account Account to remove
     * @param removeFromDatabase Whether the account should also be removed from the database
     * @param callback Callback that - if succeeded - returns null
     */
    public void removeAccount(Account account, boolean removeFromDatabase, Callback<Void> callback) {
        plugin.debug("Removing account (#" + account.getID() + ")");

		account.clearNickname();
		account.getBank().removeAccount(account);

        InventoryHolder ih = account.getInventoryHolder();

        if (ih instanceof DoubleChest) {
            DoubleChest dc = (DoubleChest) ih;
			Chest r = (Chest) dc.getRightSide();
			Chest l = (Chest) dc.getLeftSide();

			accountLocationMap.remove(r.getLocation());
			accountLocationMap.remove(l.getLocation());
        } else {
            accountLocationMap.remove(account.getLocation());
        }

        if (removeFromDatabase) {
			plugin.getDatabase().removeAccount(account, callback);
        } else {
            if (callback != null) callback.callSyncResult(null);
        }
    }

    /**
     * Remove an account. May not work properly if double chest doesn't exist!
     * @param account Account to remove
     * @param removeFromDatabase Whether the account should also be removed from the database
     * @see AccountUtils#removeAccountById(int, boolean)
     */
    public void removeAccount(Account account, boolean removeFromDatabase) {
        removeAccount(account, removeFromDatabase, null);
    }

    /**
     * Get the account limits of a player
     * @param player Player, whose account limits should be returned
     * @return The account limits of the given player
     */
    public int getAccountLimit(Player player) {
        int limit = 0;
        boolean useDefault = true;

        for (PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
			if (permInfo.getPermission().startsWith("bankingplugin.account.limit.")
					&& player.hasPermission(permInfo.getPermission())) {
                if (permInfo.getPermission().equalsIgnoreCase(Permissions.ACCOUNT_NO_LIMIT)) {
                    limit = -1;
                    useDefault = false;
                    break;
                } else {
					String[] spl = permInfo.getPermission().split("bankingplugin.account.limit.");

                    if (spl.length > 1) {
                        try {
                            int newLimit = Integer.valueOf(spl[1]);
                            if (newLimit < 0) {
                                limit = -1;
                                break;
                            }
                            limit = Math.max(limit, newLimit);
                            useDefault = false;
						} catch (NumberFormatException e) {
						}
                    }
                }
            }
        }
		if (limit < -1)
			limit = -1;
        return (useDefault ? Config.defaultAccountLimit : limit);
    }

    /**
	 * Get the number of accounts owned by a certain player
	 * 
	 * @param player Player whose accounts should be counted
	 * @return The number of accounts owned by the player
	 */
	public int getNumberOfAccounts(OfflinePlayer owner) {
		return (int) Math.round(getPlayerAccountsCopy(owner).stream()
				.mapToDouble(account -> account.getChestSize() == 1 ? 1.0 : 0.5).sum());
    }

	@SuppressWarnings("deprecation")
	public List<Account> listAccounts(CommandSender sender, String request, String[] args) {
		BankUtils bankUtils = plugin.getBankUtils();
		if (sender instanceof Player) {
			Player p = (Player) sender;
			switch (request) {
			case "":
				return new ArrayList<>(getPlayerAccountsCopy(p));
			case "-d":
				return new ArrayList<>(getPlayerAccountsCopy(p));
			case "-a":
				return new ArrayList<>(getAccountsCopy());
			case "-a -d":
				return new ArrayList<>(getAccountsCopy());
			case "name":
				OfflinePlayer owner1 = Bukkit.getOfflinePlayer(args[1]);
				return new ArrayList<>(getPlayerAccountsCopy(owner1));
			case "name -d":
				OfflinePlayer owner2 = Bukkit.getOfflinePlayer(args[1]);
				return new ArrayList<>(getPlayerAccountsCopy(owner2));
			case "bank":
				Bank bankA = bankUtils.lookupBank(args[2]);
				return getPlayerAccountsCopy(p).stream().filter(account -> account.getBank().equals(bankA))
						.collect(Collectors.toList());
			case "-a bank":
				Bank bankB = bankUtils.lookupBank(args[2]);
				return getAccountsCopy().stream().filter(account -> account.getBank().equals(bankB))
						.collect(Collectors.toList());
			default:
				return new ArrayList<>();
			}
		} else {
			switch (request) {
			case "-a":
				return new ArrayList<>(getAccountsCopy());
			case "-a -d":
				return new ArrayList<>(getAccountsCopy());
			case "name":
				OfflinePlayer owner = Bukkit.getOfflinePlayer(args[1]);
				return new ArrayList<>(getPlayerAccountsCopy(owner));
			case "name -d":
				OfflinePlayer owner2 = Bukkit.getOfflinePlayer(args[1]);
				return new ArrayList<>(getPlayerAccountsCopy(owner2));
			case "-a bank":
				Bank bankB = bankUtils.lookupBank(args[2]);
				return getAccountsCopy().stream().filter(account -> account.getBank().equals(bankB))
						.collect(Collectors.toList());
			default:
				return new LinkedList<>();
			}
		}
	}

	public int removeAccount(Collection<Account> accounts, boolean removeFromDatabase) {
		int removed = accounts.size();

		AccountRemoveAllEvent event = new AccountRemoveAllEvent(accounts);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			plugin.debug("Removeall event cancelled");
			return 0;
		}
		accounts.forEach(account -> removeAccount(account, removeFromDatabase));

		return removed;
	}

	public BigDecimal appraiseAccountContents(Account account) {

		BigDecimal sum = BigDecimal.ZERO;
		sum.setScale(2, RoundingMode.HALF_EVEN);
		for (ItemStack items : account.getInventoryHolder().getInventory().getContents()) {
			if (items == null)
				continue;
			if (Config.blacklist.contains(items.getType().toString()))
				continue;
			BigDecimal itemValue = BigDecimal.ZERO;
			if (items.getItemMeta() instanceof BlockStateMeta) {
				BlockStateMeta im = (BlockStateMeta) items.getItemMeta();
                if (im.getBlockState() instanceof ShulkerBox) {
                	ShulkerBox shulkerBox = (ShulkerBox) im.getBlockState();
                	for (ItemStack innerItems : shulkerBox.getInventory().getContents()) {
                		if (innerItems == null)
                			continue;
                		if (Config.blacklist.contains(innerItems.getType().toString()))
							continue;
						BigDecimal innerItemValue = getWorth(innerItems);
						if (innerItemValue.signum() == 1)
            				innerItemValue = innerItemValue.multiply(BigDecimal.valueOf(innerItems.getAmount()));
            			else {
							plugin.debug("An item without value (" + items.getType().toString()
									+ ") was placed into account (#" + account.getID() + ")");
            				continue;
            			}
						itemValue = itemValue.add(innerItemValue);
                	}
                }
			}
			itemValue = itemValue.add(getWorth(items));
			if (itemValue.signum() == 1)
				itemValue = itemValue.multiply(BigDecimal.valueOf(items.getAmount()));
			else {
				plugin.debug("An item without value (" + items.getType().toString() + ") was placed into account (#" + account.getID() + ")");
				continue;
			}
			sum = sum.add(itemValue);
		}
		return sum.setScale(2, RoundingMode.HALF_EVEN);
	}

	private BigDecimal getWorth(ItemStack items) {
		Essentials essentials = plugin.getEssentials();
		return Optional.ofNullable(essentials.getWorth().getPrice(essentials, items)).orElse(BigDecimal.ZERO);
	}

	public boolean payInsurance(Account account, BigDecimal loss) {
		long insurance = Config.insureAccountsUpTo;
		if (insurance == 0)
			return false;
		EconomyResponse response;
		if (insurance < 0)
			response = plugin.getEconomy().depositPlayer(account.getOwner(), loss.doubleValue());
		else {
			double payoutAmount = loss.doubleValue() > insurance ? insurance : loss.doubleValue();
			response = plugin.getEconomy().depositPlayer(account.getOwner(), payoutAmount);
		}
		return response.transactionSuccess();
	}
}
