package com.monst.bankingplugin.gui;

import com.monst.bankingplugin.BankingPlugin;
import com.monst.bankingplugin.utils.Ownable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.slot.ClickOptions;
import org.ipvp.canvas.slot.Slot.ClickHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

abstract class Gui<T extends Ownable> {

	T guiSubject;
	Menu gui;
	Gui prevGui;
	boolean highClearance;

	static final Material GENERAL_INFO_BLOCK = Material.PLAYER_HEAD;
	static final Material MULTIPLIER_INFO_BLOCK = Material.NETHER_STAR;

	public Gui(T t) {
		this.guiSubject = t;
	}

	public void open(Player player) {
		gui = getMenu();
		if (prevGui != null) {
			gui.setCloseHandler((player1, menu1) -> {
				new BukkitRunnable() {
					@Override
					public void run() {
						prevGui.open(player);
					}
				}.runTaskLater(BankingPlugin.getInstance(), 1);
			});
		}
		getClearance(player);
		for (int i = 0; i < gui.getDimensions().getArea(); i++) {
			gui.getSlot(i).setItem(createSlotItem(i));
			gui.getSlot(i).setClickHandler(createClickHandler(i));
			gui.getSlot(i).setClickOptions(ClickOptions.DENY_ALL);
		}
		gui.open(player);
	}

	public Gui<T> setPrevGui(@Nullable Gui prevGui) {
		this.prevGui = prevGui;
		return this;
	}

	abstract Menu getMenu();

	abstract void getClearance(Player player);

	abstract ItemStack createSlotItem(int i);

	abstract ClickHandler createClickHandler(int i);

	static ItemStack createSlotItem(Material material, String displayName, List<String> lore) {
		ItemStack item = new ItemStack(material);
		ItemMeta itemMeta = item.getItemMeta();
		assert itemMeta != null;
		itemMeta.setDisplayName(ChatColor.GRAY + displayName);
		itemMeta.setLore(lore.stream().map(s -> ChatColor.GRAY + s).collect(Collectors.toList()));
		item.setItemMeta(itemMeta);
		return item;
	}
}