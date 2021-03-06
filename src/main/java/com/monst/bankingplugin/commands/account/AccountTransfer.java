package com.monst.bankingplugin.commands.account;

import com.monst.bankingplugin.banking.Account;
import com.monst.bankingplugin.banking.AccountField;
import com.monst.bankingplugin.config.Config;
import com.monst.bankingplugin.events.account.AccountTransferCommandEvent;
import com.monst.bankingplugin.events.account.AccountTransferEvent;
import com.monst.bankingplugin.lang.*;
import com.monst.bankingplugin.utils.ClickType;
import com.monst.bankingplugin.utils.Permissions;
import com.monst.bankingplugin.utils.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AccountTransfer extends AccountCommand.SubCommand implements ConfirmableAccountAction {

    private static AccountTransfer instance;

    public static AccountTransfer getInstance() {
        return instance;
    }

    AccountTransfer() {
        super("transfer", true);
        instance = this;
    }

    @Override
    protected String getPermission() {
        return Permissions.ACCOUNT_TRANSFER;
    }

    @Override
    protected Message getUsageMessage() {
        return Message.COMMAND_USAGE_ACCOUNT_TRANSFER;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        Player p = ((Player) sender);
        PLUGIN.debug(p.getName() + " wants to transfer ownership of an account");

        if (!p.hasPermission(Permissions.ACCOUNT_TRANSFER)) {
            PLUGIN.debug(p.getName() + " does not have permission to transfer ownership of an account");
            p.sendMessage(LangUtils.getMessage(Message.NO_PERMISSION_ACCOUNT_TRANSFER));
            return true;
        }

        if (args.length < 2)
            return false;

        OfflinePlayer newOwner = Utils.getPlayer(args[1]);
        if (newOwner == null) {
            p.sendMessage(LangUtils.getMessage(Message.PLAYER_NOT_FOUND, new Replacement(Placeholder.INPUT, args[1])));
            return true;
        }

        AccountTransferCommandEvent event = new AccountTransferCommandEvent(p, args);
        event.fire();
        if (event.isCancelled()) {
            PLUGIN.debug("Account transfer command event cancelled");
            return true;
        }

        p.sendMessage(LangUtils.getMessage(Message.CLICK_ACCOUNT_TRANSFER, new Replacement(Placeholder.PLAYER, newOwner::getName)));
        ClickType.setTransferClickType(p, newOwner);
        PLUGIN.debug(p.getName() + " is transferring ownership of an account to " + newOwner.getName());
        return true;
    }

    @Override
    protected List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length != 1)
            return Collections.emptyList();

        List<String> returnCompletions = Utils.getOnlinePlayerNames();
        if (!sender.hasPermission(Permissions.ACCOUNT_TRANSFER_OTHER))
            returnCompletions.remove(sender.getName());
        return Utils.filter(returnCompletions, string -> Utils.startsWithIgnoreCase(string, args[0]));
    }

    public void transfer(Player p, Account account, OfflinePlayer newOwner) {
        PLUGIN.debug(p.getName() + " is transferring account #" + account.getID() + " to the ownership of " + newOwner.getName());

        if (!account.isOwner(p) && !p.hasPermission(Permissions.ACCOUNT_TRANSFER_OTHER)) {
            PLUGIN.debug(p.getName() + " does not have permission to transfer the account.");
            if (account.isTrusted(p))
                p.sendMessage(LangUtils.getMessage(Message.MUST_BE_OWNER));
            else
                p.sendMessage(LangUtils.getMessage(Message.NO_PERMISSION_ACCOUNT_TRANSFER_OTHER));
            ClickType.removeClickType(p);
            return;
        }

        if (account.isOwner(newOwner)) {
            PLUGIN.debug(p.getName() + " is already owner of account");
            p.sendMessage(LangUtils.getMessage(Message.ALREADY_OWNER, new Replacement(Placeholder.PLAYER, newOwner::getName)));
            ClickType.removeClickType(p);
            return;
        }

        if (Config.confirmOnTransfer.get() && !isConfirmed(p, account.getID())) {
            PLUGIN.debug("Needs confirmation");
            p.sendMessage(LangUtils.getMessage(Message.ACCOUNT_CONFIRM_TRANSFER,
                    new Replacement(Placeholder.PLAYER, newOwner::getName)
            ));
            return;
        }

        AccountTransferEvent event = new AccountTransferEvent(p, account, newOwner);
        event.fire();
        if (event.isCancelled()) {
            PLUGIN.debug("Account transfer event cancelled");
            return;
        }

        boolean hasCustomName = account.hasCustomName();

        MailingRoom mailingRoom = new MailingRoom(LangUtils.getMessage(Message.ACCOUNT_TRANSFERRED,
                new Replacement(Placeholder.PLAYER, newOwner::getName)
        ));
        mailingRoom.addRecipient(p);
        mailingRoom.send();
        mailingRoom.newMessage(LangUtils.getMessage(Message.ACCOUNT_TRANSFERRED_TO_YOU,
                new Replacement(Placeholder.PLAYER, p::getName)
        ));
        mailingRoom.addOfflineRecipient(newOwner);
        mailingRoom.removeRecipient(p);
        mailingRoom.send();

        account.setOwner(newOwner);
        if (!hasCustomName)
            account.resetName();
        PLUGIN.getAccountRepository().update(account, AccountField.OWNER);
        ClickType.removeClickType(p);
    }

}
