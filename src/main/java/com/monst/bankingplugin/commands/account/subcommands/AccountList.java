package com.monst.bankingplugin.commands.account.subcommands;

import com.monst.bankingplugin.banking.account.Account;
import com.monst.bankingplugin.gui.AccountListGui;
import com.monst.bankingplugin.utils.Messages;
import com.monst.bankingplugin.utils.Permissions;
import com.monst.bankingplugin.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class AccountList extends AccountSubCommand {

    public AccountList() {
        super("list", false);
    }

    @Override
    public String getHelpMessage(CommandSender sender) {
        return Messages.COMMAND_USAGE_ACCOUNT_LIST;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.debug(sender.getName() + " wants to list accounts");
        List<Account> accounts;
        if (!sender.hasPermission(Permissions.ACCOUNT_LIST_OTHER)) {
            if (!(sender instanceof Player)) {
                plugin.debug("Only players can list their own accounts");
                sender.sendMessage(Messages.PLAYER_COMMAND_ONLY);
                return true;
            }
            plugin.debug(sender.getName() + " has listed their own accounts");
            accounts = accountUtils.getAccountsCopy().stream()
                    .filter(a -> a.isOwner((Player) sender))
                    .sorted(Comparator.comparing(Account::getBalance).reversed())
                    .collect(Collectors.toList());
        } else if (args.length == 1) {
            plugin.debug(sender.getName() + " has listed all accounts");
            accounts = accountUtils.getAccountsCopy().stream()
                    .sorted(Comparator.comparing(Account::getBalance).reversed())
                    .collect(Collectors.toList());
        } else {
            List<OfflinePlayer> players = Arrays.stream(args)
                    .map(Utils::getPlayer)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            plugin.debug(sender.getName() + " has listed the accounts of " + players);
            accounts = accountUtils.getAccountsCopy().stream()
                    .filter(a -> players.contains(a.getOwner()))
                    .sorted(Comparator.<Account, Integer>comparing(a -> players.indexOf(a.getOwner()))
                            .thenComparing(Account::getBalance, BigDecimal::compareTo).reversed())
                    .collect(Collectors.toList());
        }

        if (accounts.isEmpty()) {
            sender.sendMessage(Messages.NO_ACCOUNTS_TO_LIST);
            return true;
        }

        if (sender instanceof Player)
            new AccountListGui(accounts).open(((Player) sender));
        else {
            int i = 0;
            for (Account account : accounts)
                sender.sendMessage(ChatColor.AQUA + "" + ++i + ". " + account.getColorizedName());
        }
        return true;
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ACCOUNT_LIST_OTHER))
            return Collections.emptyList();
        List<String> argList = Arrays.asList(args);
        return Utils.filter(Utils.getOnlinePlayerNames(plugin), name -> !argList.contains(name));
    }

}
