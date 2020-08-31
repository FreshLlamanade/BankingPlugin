package com.monst.bankingplugin.events.account;

import com.monst.bankingplugin.banking.account.Account;
import com.monst.bankingplugin.utils.ClickType;
import org.bukkit.entity.Player;

public class AccountConfigureEvent extends SingleAccountEvent {

    private final ClickType.SetClickType.SetField field;
    private final String newValue;

    public AccountConfigureEvent(Player player, Account account, ClickType.SetClickType.SetField field, String newValue) {
        super(player, account);
        this.field = field;
        this.newValue = newValue;
    }

    public ClickType.SetClickType.SetField getField() {
        return field;
    }

    public String getNewValue() {
        return newValue;
    }
}
