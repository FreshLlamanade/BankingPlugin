package com.monst.bankingplugin.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.OfflinePlayer;

public abstract class Ownable {

	protected OfflinePlayer owner;
	protected Set<OfflinePlayer> coowners = new HashSet<>();

	public boolean isOwner(OfflinePlayer player) {
		if (owner == null)
			return false;
		return owner.getUniqueId().equals(player.getUniqueId());
	}

	public String getOwnerDisplayName() {
		if (owner == null)
			return "";
		return owner.isOnline() ? owner.getPlayer().getDisplayName() : owner.getName();
	}

	public OfflinePlayer getOwner() {
		return owner;
	}

	public boolean isTrustedPlayerOnline() {
		if (owner == null)
			return false;
		return owner.isOnline() || (coowners != null ? coowners.stream().anyMatch(p -> p.isOnline()) : false);
	}

	public boolean isTrusted(OfflinePlayer p) {
		return p != null ? isOwner(p) || isCoowner(p) : false;
	}

	public boolean isCoowner(OfflinePlayer p) {
		return p != null && coowners != null ? coowners.contains(p) : false;
	}

	public void trustPlayer(OfflinePlayer p) {
		if (coowners != null)
			coowners.add(p);
	}

	public void untrustPlayer(OfflinePlayer p) {
		if (coowners != null)
			coowners.remove(p);
	}

	public Set<OfflinePlayer> getTrustedPlayersCopy() {
		if (owner == null || coowners == null)
			return null;
		Set<OfflinePlayer> trustedPlayers = new HashSet<>();
		trustedPlayers.add(owner);
		trustedPlayers.addAll(coowners);
		return Collections.unmodifiableSet(trustedPlayers);
	}

	public Set<OfflinePlayer> getCoowners() {
		return Collections.unmodifiableSet(coowners);
	}

}
