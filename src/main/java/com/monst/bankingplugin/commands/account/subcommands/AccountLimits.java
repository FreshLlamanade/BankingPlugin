package com.monst.bankingplugin.commands.account.subcommands;

import com.monst.bankingplugin.utils.Messages;
import com.monst.bankingplugin.utils.Permissions;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AccountLimits extends AccountSubCommand {

    public AccountLimits() {
        super("limits", true);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player p = ((Player) sender);
        int used = accountUtils.getNumberOfAccounts(p);
        Object limit = accountUtils.getAccountLimit(p) < 0 ? "∞" : accountUtils.getAccountLimit(p);
        plugin.debug(p.getName() + " is viewing their account limits: " + used + " / " + limit);
        p.sendMessage(String.format(Messages.ACCOUNT_LIMIT, used, limit));
        return true;
    }

    @Override
    public String getHelpMessage(CommandSender sender) {
        return hasPermission(sender, Permissions.ACCOUNT_CREATE) ? Messages.COMMAND_USAGE_ACCOUNT_LIMITS : "";
    }

}
