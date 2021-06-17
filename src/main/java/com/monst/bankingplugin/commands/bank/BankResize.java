package com.monst.bankingplugin.commands.bank;

import com.monst.bankingplugin.banking.bank.Bank;
import com.monst.bankingplugin.banking.bank.BankField;
import com.monst.bankingplugin.config.Config;
import com.monst.bankingplugin.events.bank.BankResizeEvent;
import com.monst.bankingplugin.external.VisualizationManager;
import com.monst.bankingplugin.external.WorldEditReader;
import com.monst.bankingplugin.lang.LangUtils;
import com.monst.bankingplugin.lang.Message;
import com.monst.bankingplugin.lang.Placeholder;
import com.monst.bankingplugin.lang.Replacement;
import com.monst.bankingplugin.geo.selections.Selection;
import com.monst.bankingplugin.utils.Callback;
import com.monst.bankingplugin.utils.Permissions;
import com.monst.bankingplugin.utils.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BankResize extends BankCommand.SubCommand {

    BankResize() {
        super("resize", true);
    }

    @Override
    protected String getPermission() {
        return Permissions.BANK_CREATE;
    }

    @Override
    protected Message getUsageMessage() {
        return Message.COMMAND_USAGE_BANK_RESIZE;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        Player p = ((Player) sender);
        PLUGIN.debug(p.getName() + " wants to resize a bank");

        if (!p.hasPermission(Permissions.BANK_RESIZE)) {
            PLUGIN.debug(p.getName() + " does not have permission to resize a bank");
            p.sendMessage(LangUtils.getMessage(Message.NO_PERMISSION_BANK_RESIZE));
            return true;
        }

        Bank bank;
        Selection selection;

        if (args.length == 1)
            return false;

        else if (args.length == 2) {
            if (Config.enableWorldEditIntegration.get() && PLUGIN.hasWorldEdit()) {
                selection = WorldEditReader.getSelection(PLUGIN, p);
                if (selection == null) {
                    PLUGIN.debug(p.getName() + " tried to resize a bank with no WorldEdit selection");
                    p.sendMessage(LangUtils.getMessage(Message.BANK_SELECT_REGION));
                    return true;
                }
            } else {
                PLUGIN.debug("WorldEdit is not enabled");
                p.sendMessage(LangUtils.getMessage(Message.WORLDEDIT_NOT_ENABLED));
                return true;
            }
        } else {
            try {
                selection = parseCoordinates(args, p.getLocation().getBlock());
            } catch (NumberFormatException e) {
                PLUGIN.debug("Could not parse coordinates in command args: \"" + Arrays.toString(args) + "\"");
                p.sendMessage(LangUtils.getMessage(Message.BANK_COORDINATE_PARSE_ERROR));
                return false;
            }
        }

        if (selection == null)
            return false;

        bank = bankRepo.getByIdentifier(args[1]);
        if (bank == null) {
            PLUGIN.debugf("Couldn't find bank with name or ID %s", args[1]);
            p.sendMessage(LangUtils.getMessage(Message.BANK_NOT_FOUND, new Replacement(Placeholder.STRING, args[1])));
            return true;
        }
        if (bank.isPlayerBank() && !bank.isOwner(p) && !p.hasPermission(Permissions.BANK_RESIZE_OTHER)) {
            PLUGIN.debug(p.getName() + " does not have permission to resize another player's bank");
            p.sendMessage(LangUtils.getMessage(Message.NO_PERMISSION_BANK_RESIZE_OTHER));
            return true;
        }
        if (bank.isAdminBank() && !p.hasPermission(Permissions.BANK_RESIZE_ADMIN)) {
            PLUGIN.debug(p.getName() + " does not have permission to resize an admin bank");
            p.sendMessage(LangUtils.getMessage(Message.NO_PERMISSION_BANK_RESIZE_ADMIN));
            return true;
        }
        if (Config.disabledWorlds.get().contains(selection.getWorld().getName())) {
            PLUGIN.debug("BankingPlugin is disabled in world " + selection.getWorld().getName());
            p.sendMessage(LangUtils.getMessage(Message.WORLD_DISABLED));
            return true;
        }
        long volume = selection.getVolume();
        long volumeLimit = getVolumeLimit(p);
        if (bank.isPlayerBank() && volumeLimit >= 0 && volume > volumeLimit) {
            PLUGIN.debug("Bank is too large (" + volume + " blocks, limit: " + volumeLimit + ")");
            p.sendMessage(LangUtils.getMessage(Message.BANK_SELECTION_TOO_LARGE,
                    new Replacement(Placeholder.BANK_SIZE, volume),
                    new Replacement(Placeholder.MAXIMUM, volumeLimit),
                    new Replacement(Placeholder.DIFFERENCE, () -> volume - volumeLimit)
            ));
            return true;
        }
        if (bank.isPlayerBank() && volume < Config.minimumBankVolume.get()) {
            PLUGIN.debug("Bank is too small (" + volume + " blocks, minimum: " + Config.minimumBankVolume.get() + ")");
            p.sendMessage(LangUtils.getMessage(Message.BANK_SELECTION_TOO_SMALL,
                    new Replacement(Placeholder.BANK_SIZE, volume),
                    new Replacement(Placeholder.MINIMUM, Config.minimumBankVolume::get),
                    new Replacement(Placeholder.DIFFERENCE, () -> Config.minimumBankVolume.get() - volume)
            ));
            return true;
        }
        Set<Selection> overlappingSelections = bankRepo.getOverlappingSelections(selection);
        overlappingSelections.remove(bank.getSelection());
        if (!overlappingSelections.isEmpty()) {
            PLUGIN.debug("New selection is overlaps with an existing bank selection");
            p.sendMessage(LangUtils.getMessage(Message.BANK_SELECTION_OVERLAPS_EXISTING,
                    new Replacement(Placeholder.NUMBER_OF_BANKS, overlappingSelections::size)
            ));
            if (PLUGIN.hasGriefPrevention() && Config.enableGriefPreventionIntegration.get())
                VisualizationManager.visualizeOverlap(p, overlappingSelections);
            return true;
        }
        long cutAccounts = bank.getAccounts(account -> !selection.contains(account.getChestLocation())).size();
        if (cutAccounts > 0) {
            PLUGIN.debug("New selection does not contain all accounts");
            p.sendMessage(LangUtils.getMessage(Message.BANK_SELECTION_CUTS_ACCOUNTS, new Replacement(Placeholder.NUMBER_OF_ACCOUNTS, cutAccounts)));
            return true;
        }

        BankResizeEvent event = new BankResizeEvent(p, bank, selection);
        event.fire();
        if (event.isCancelled()) {
            PLUGIN.debug("Bank resize event cancelled");
            return true;
        }

        bankRepo.remove(bank, false);
        bank.setSelection(selection);
        bankRepo.update(bank, Callback.of(
                result -> {
                    PLUGIN.debug(p.getName() + " has resized bank \"" + bank.getName() + "\" (#" + bank.getID() + ")");
                    p.sendMessage(LangUtils.getMessage(Message.BANK_RESIZED, new Replacement(Placeholder.BANK_SIZE, volume)));
                },
                error -> p.sendMessage(LangUtils.getMessage(Message.ERROR_OCCURRED,
                        new Replacement(Placeholder.ERROR, error::getLocalizedMessage)
                ))
        ), BankField.SELECTION);
        return true;
    }

    @Override
    protected List<String> getTabCompletions(CommandSender sender, String[] args) {
        Player p = ((Player) sender);
        if (args.length == 2) {
            return bankRepo.getAll().stream()
                    .map(Bank::getName)
                    .filter(name -> Utils.startsWithIgnoreCase(name, args[1]))
                    .sorted()
                    .collect(Collectors.toList());
        } else if (args.length > 2) {
            String coord = getCoordLookingAt(p, args.length);
            if (coord.startsWith(args[args.length - 1]))
                return Collections.singletonList("" + coord);
        }
        return Collections.emptyList();
    }

}
