package com.monst.bankingplugin.gui;

import com.monst.bankingplugin.banking.account.Account;
import com.monst.bankingplugin.utils.Observable;
import org.bukkit.inventory.ItemStack;
import org.ipvp.canvas.paginate.PaginatedMenuBuilder;
import org.ipvp.canvas.slot.Slot;
import org.ipvp.canvas.slot.SlotSettings;
import org.ipvp.canvas.template.ItemStackTemplate;
import org.ipvp.canvas.template.StaticItemTemplate;

import java.util.*;
import java.util.function.Supplier;

public class AccountListGUI extends MultiPageGUI<Account> {

    private static final Comparator<Account> BY_BALANCE = Comparator.comparing(Account::getBalance);
    private static final Comparator<Account> BY_OWNER_NAME = Comparator.comparing(account -> account.getOwner().getName());
    private static final Comparator<Account> BY_BANK_NAME = Comparator.comparing(account -> account.getBank().getName());

    static final List<MenuItemSorter<Account>> SORTERS = Arrays.asList(
            MenuItemSorter.of("Balance Ascending", BY_BALANCE),
            MenuItemSorter.of("Balance Descending", BY_BALANCE.reversed()),
            MenuItemSorter.of("Owner Name Ascending", BY_OWNER_NAME),
            MenuItemSorter.of("Owner Name Descending", BY_OWNER_NAME.reversed()),
            MenuItemSorter.of("Bank Name Ascending", BY_BANK_NAME),
            MenuItemSorter.of("Bank Name Descending", BY_BANK_NAME.reversed())
    );
    static final List<MenuItemFilter<Account>> FILTERS = Arrays.asList(
            MenuItemFilter.of("Single Chest Accounts", account -> account.getSize() == 1),
            MenuItemFilter.of("Double Chest Accounts", account -> account.getSize() == 2)
    );

    public AccountListGUI(Supplier<Set<? extends Account>> source) {
        super(source, FILTERS, SORTERS, 18, 26);
    }

    @Override
    String getTitle() {
        return "Account List";
    }

    @Override
    void addItems(PaginatedMenuBuilder builder) {
        for (Account account : getMenuItems()) {
            ItemStack item = createSlotItem(account.getOwner(), account.getChestName(),
                    Collections.singletonList("Owner: " + account.getOwnerDisplayName()));
            ItemStackTemplate template = new StaticItemTemplate(item);
            Slot.ClickHandler clickHandler = (player, info) -> new AccountGUI(account).setParentGUI(this).open(player);
            builder.addItem(SlotSettings.builder().itemTemplate(template).clickHandler(clickHandler).build());
        }
    }

    @Override
    Observable getSubject() {
        return plugin.getAccountRepository();
    }

    @Override
    GUIType getType() {
        return GUIType.ACCOUNT_LIST;
    }

}
