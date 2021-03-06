package com.monst.bankingplugin.gui;

import com.monst.bankingplugin.banking.Account;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.slot.Slot;
import org.ipvp.canvas.type.ChestMenu;

public class AccountContentsGUI extends SinglePageGUI<Account> {

    // boolean canEdit; TODO: Implement remote editing

    public AccountContentsGUI(Account account) {
        super(account);
    }

    @Override
    Menu createMenu() {
        return ChestMenu.builder(guiSubject.getSize() * 3).title(guiSubject.getChestName()).redraw(true).build();
    }

//    @Override
//    void evaluateClearance(Player player) {
//        canEdit = player.hasPermission(Permissions.ACCOUNT_EDIT_OTHER);
//    }

    @Override
    ItemStack createSlotItem(int slot) {
        return guiSubject.getInventoryHolder(false).getInventory().getItem(slot);
    }

    @Override
    Slot.ClickHandler createClickHandler(int slot) {
        ItemStack item = guiSubject.getInventoryHolder(false).getInventory().getItem(slot);
        if (item != null && item.getItemMeta() instanceof BlockStateMeta) {
            BlockStateMeta im = (BlockStateMeta) item.getItemMeta();
            if (im.getBlockState() instanceof ShulkerBox) {
                ShulkerBox shulkerBox = (ShulkerBox) im.getBlockState();
                return (player, info) -> new ShulkerContentsGUI(guiSubject, shulkerBox).setParentGUI(this).open(player);
            }
        }
        return null;
        /*if (canEdit)
            gui.getSlot(i).setClickOptions(ClickOptions.ALLOW_ALL);
        return (player, info) -> {
            ItemStack item = gui.getSlot(i).getItem(player);
            guiSubject.getInventoryHolder().getInventory().setItem(i, item);
            if (guiSubject.isDoubleChest()) {
                DoubleChest dc = (DoubleChest) guiSubject.getInventoryHolder();
                ((Chest) dc.getRightSide()).update();
                ((Chest) dc.getLeftSide()).update();
            } else {
                Chest chest = (Chest) guiSubject.getInventoryHolder();
                chest.update();
            }
        };*/
    }

    @Override
    GUIType getType() {
        return GUIType.ACCOUNT_CONTENTS;
    }

    static class ShulkerContentsGUI extends AccountContentsGUI {

        private final ShulkerBox shulkerBox;

        public ShulkerContentsGUI(Account account, ShulkerBox shulkerBox) {
            super(account);
            this.shulkerBox = shulkerBox;
        }

        @Override
        Menu createMenu() {
            return ChestMenu.builder(3).title("Shulker Box").redraw(true).build();
//                    shulkerBox.getCustomName() != null ?
//                            shulkerBox.getCustomName() : // TODO: Figure out why always null
//                            WordUtils.capitalizeFully(shulkerBox.getColor().toString())
//                       FIXME: shulkerBox.getColor() throws NullPointerException when Shulker Box default color

        }

        @Override
        ItemStack createSlotItem(int slot) {
            return shulkerBox.getInventory().getItem(slot);
        }

        @Override
        Slot.ClickHandler createClickHandler(int slot) {
            return null;
        }

        @Override
        GUIType getType() {
            return GUIType.ACCOUNT_SHULKER_CONTENTS;
        }
    }
}
