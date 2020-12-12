package com.monst.bankingplugin.gui;

import com.monst.bankingplugin.banking.bank.Bank;
import com.monst.bankingplugin.utils.Observable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.paginate.PaginatedMenuBuilder;
import org.ipvp.canvas.slot.Slot;
import org.ipvp.canvas.slot.SlotSettings;
import org.ipvp.canvas.template.ItemStackTemplate;
import org.ipvp.canvas.template.StaticItemTemplate;
import org.ipvp.canvas.type.ChestMenu;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

public class BankListGUI extends MultiPageGUI<Collection<Bank>> {

    public BankListGUI(Supplier<? extends Collection<Bank>> banks) {
        super(banks, 18, 26);
    }

    @Override
    Menu.Builder<?> getPageTemplate() {
        return ChestMenu.builder(3).title("Bank List").redraw(true);
    }

    @Override
    void addItems(PaginatedMenuBuilder builder) {
        for (Bank bank : guiSubjects.get()) {
            ItemStack item = bank.isPlayerBank() ?
                    createSlotItem(bank.getOwner(), bank.getColorizedName(), Collections.singletonList("Owner: " + bank.getOwnerDisplayName())) :
                    createSlotItem(Material.PLAYER_HEAD, bank.getColorizedName(), Collections.singletonList("Owner: " + bank.getOwnerDisplayName()));
            ItemStackTemplate template = new StaticItemTemplate(item);
            Slot.ClickHandler clickHandler = (player, info) -> new BankGUI(bank).setParentGUI(this).open(player);
            builder.addItem(SlotSettings.builder().itemTemplate(template).clickHandler(clickHandler).build());
        }
    }

    @Override
    Observable getSubject() {
        return plugin.getBankRepository();
    }

    @Override
    GuiType getType() {
        return GuiType.BANK_LIST;
    }
}