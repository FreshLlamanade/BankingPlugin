package com.monst.bankingplugin.gui;

import com.monst.bankingplugin.banking.Account;
import com.monst.bankingplugin.sql.logging.AccountTransaction;
import com.monst.bankingplugin.utils.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.slot.SlotSettings;
import org.ipvp.canvas.template.StaticItemTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class AccountTransactionGUI extends HistoryGUI<AccountTransaction> {

    private static final int SWITCH_VIEW_SLOT = 31;

    private static final Predicate<AccountTransaction> POSITIVE_AMOUNT = t -> t.getAmount().signum() >= 0;
    private static final List<MenuItemFilter<? super AccountTransaction>> FILTERS = Arrays.asList(
            MenuItemFilter.of("Deposits", POSITIVE_AMOUNT),
            MenuItemFilter.of("Withdrawals", POSITIVE_AMOUNT.negate())
    );

    private static final Comparator<AccountTransaction> BY_AMOUNT = Comparator.comparing(t -> t.getAmount().abs());
    private static final Comparator<AccountTransaction> BY_EXECUTOR = Comparator.comparing(AccountTransaction::getExecutorName);
    private static final List<MenuItemSorter<? super AccountTransaction>> SORTERS = Arrays.asList(
            MenuItemSorter.of("Largest Amount", BY_AMOUNT.reversed()),
            MenuItemSorter.of("Smallest Amount", BY_AMOUNT),
            MenuItemSorter.of("Player Name A-Z", BY_EXECUTOR),
            MenuItemSorter.of("Player Name Z-A", BY_EXECUTOR.reversed())
    );

    private final SlotSettings switchViewSlot;

    public AccountTransactionGUI(Account account) {
        super(callback -> DATABASE.getTransactionsAtAccount(account, callback), FILTERS, SORTERS);
        this.switchViewSlot = createSwitchViewSlot(account);
    }

    @Override
    String getTitle() {
        return "Account Transaction Log";
    }

    @Override
    SlotSettings createSlotSettings(AccountTransaction transaction) {
        Material material = transaction.getAmount().signum() >= 0 ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        ItemStack item = createSlotItem(material, "Transaction #" + transaction.getID(), Arrays.asList(
                transaction.getTimeFormatted(),
                "Player: " + transaction.getExecutorName(),
                "Amount: " + Utils.formatAndColorize(transaction.getAmount())
        ));
        return SlotSettings.builder().itemTemplate(new StaticItemTemplate(item)).build();
    }

    @Override
    void modify(Menu page) {
        page.getSlot(SWITCH_VIEW_SLOT).setSettings(switchViewSlot);
    }

    private SlotSettings createSwitchViewSlot(Account account) {
        ItemStack item = createSlotItem(
                Material.BOOK,
                "Account Interest Log",
                Collections.singletonList("Click to view the interest log.")
        );
        return SlotSettings.builder()
                .itemTemplate(new StaticItemTemplate(item))
                .clickHandler((player, info) -> new AccountInterestGUI(account).setParentGUI(parentGUI).open(player))
                .build();
    }

    @Override
    GUIType getType() {
        return GUIType.ACCOUNT_TRANSACTION_LOG;
    }

}
