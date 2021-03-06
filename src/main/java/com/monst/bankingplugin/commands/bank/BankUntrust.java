package com.monst.bankingplugin.commands.bank;

import com.monst.bankingplugin.banking.Bank;
import com.monst.bankingplugin.lang.LangUtils;
import com.monst.bankingplugin.lang.Message;
import com.monst.bankingplugin.lang.Placeholder;
import com.monst.bankingplugin.lang.Replacement;
import com.monst.bankingplugin.utils.Permissions;
import com.monst.bankingplugin.utils.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BankUntrust extends BankCommand.SubCommand {

    BankUntrust() {
        super("untrust", false);
    }

    @Override
    protected String getPermission() {
        return Permissions.BANK_TRUST;
    }

    @Override
    protected Message getUsageMessage() {
        return Message.COMMAND_USAGE_BANK_UNTRUST;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3)
            return false;

        PLUGIN.debug(sender.getName() + " wants to untrust a player from a bank");

        if (!sender.hasPermission(Permissions.BANK_TRUST)) {
            sender.sendMessage(LangUtils.getMessage(Message.NO_PERMISSION_BANK_UNTRUST));
            return true;
        }
        Bank bank = PLUGIN.getBankRepository().getByIdentifier(args[1]);
        if (bank == null) {
            PLUGIN.debugf("Couldn't find bank with name or ID %s", args[1]);
            sender.sendMessage(LangUtils.getMessage(Message.BANK_NOT_FOUND, new Replacement(Placeholder.INPUT, args[1])));
            return true;
        }
        OfflinePlayer playerToUntrust = Utils.getPlayer(args[2]);
        if (playerToUntrust == null) {
            sender.sendMessage(LangUtils.getMessage(Message.PLAYER_NOT_FOUND, new Replacement(Placeholder.INPUT, args[1])));
            return true;
        }

        if (bank.isPlayerBank() && !((sender instanceof Player && bank.isOwner((Player) sender))
                || sender.hasPermission(Permissions.BANK_TRUST_OTHER))) {
            if (sender instanceof Player && bank.isTrusted(((Player) sender))) {
                PLUGIN.debugf("%s does not have permission to untrust a player from bank %s as a co-owner",
                        sender.getName(), bank.getName());
                sender.sendMessage(LangUtils.getMessage(Message.MUST_BE_OWNER));
                return true;
            }
            PLUGIN.debugf("%s does not have permission to untrust a player from bank %s", sender.getName(), bank.getName());
            sender.sendMessage(LangUtils.getMessage(Message.NO_PERMISSION_BANK_UNTRUST_OTHER));
            return true;
        }

        if (bank.isAdminBank() && !sender.hasPermission(Permissions.BANK_TRUST_ADMIN)) {
            PLUGIN.debugf("%s does not have permission to untrust a player from admin bank %s", sender.getName(), bank.getName());
            sender.sendMessage(LangUtils.getMessage(Message.NO_PERMISSION_BANK_UNTRUST_ADMIN));
            return true;
        }

        boolean isSelf = sender instanceof Player && Utils.samePlayer(playerToUntrust, ((Player) sender));
        if (!bank.isCoOwner(playerToUntrust)) {
            PLUGIN.debugf("%s was not co-owner at bank %s (#%d)", playerToUntrust.getName(), bank.getName(), bank.getID());
            sender.sendMessage(LangUtils.getMessage(Message.NOT_A_COOWNER, new Replacement(Placeholder.PLAYER, playerToUntrust::getName)));
            return true;
        }

        PLUGIN.debugf("%s has untrusted %s from bank %s (#%d)",
                sender.getName(), playerToUntrust.getName(), bank.getName(), bank.getID());
        sender.sendMessage(LangUtils.getMessage(Message.REMOVED_COOWNER,
                new Replacement(Placeholder.PLAYER, playerToUntrust::getName)
        ));
        bank.untrustPlayer(playerToUntrust);
        PLUGIN.getDatabase().removeCoOwner(bank, playerToUntrust, null);
        return true;
    }

    @Override
    protected List<String> getTabCompletions(CommandSender sender, String[] args) {
        Player p = ((Player) sender);
        if (args.length == 1) {
            return bankRepo.getAll().stream()
                    .filter(bank -> bank.isOwner(p)
                            || (bank.isPlayerBank() && p.hasPermission(Permissions.BANK_TRUST_OTHER))
                            || (bank.isAdminBank() && p.hasPermission(Permissions.BANK_TRUST_ADMIN)))
                    .map(Bank::getName)
                    .filter(name -> Utils.startsWithIgnoreCase(name, args[0]))
                    .sorted()
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            Bank bank = PLUGIN.getBankRepository().getByIdentifier(args[1]);
            if (bank == null)
                return Collections.emptyList();
            List<String> coowners = bank.getCoOwners().stream().map(OfflinePlayer::getName).collect(Collectors.toList());
            return Utils.filter(coowners, name -> Utils.startsWithIgnoreCase(name, args[1]));
        }
        return Collections.emptyList();
    }

}
