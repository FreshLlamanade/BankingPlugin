package com.monst.bankingplugin.geo.locations;

import com.monst.bankingplugin.exceptions.ChestNotFoundException;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.InventoryHolder;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Iterator;

public class SingleChestLocation extends ChestLocation {

    public SingleChestLocation(@Nonnull Block block) {
        super(block);
    }

    @Override
    public Block getMinimumBlock() {
        return b1;
    }

    @Override
    public Block getMaximumBlock() {
        return b1;
    }

    @Override
    public InventoryHolder findInventoryHolder() throws ChestNotFoundException {
        InventoryHolder ih = getInventoryHolderAt(b1);
        if (ih != null)
            return ih;
        throw new ChestNotFoundException(this);
    }

    @Override
    public Iterator<Block> iterator() {
        return Collections.singleton(b1).iterator();
    }

    @Override
    public Location getTeleportLocation() {
        return b1.getLocation().add(0.5, 1, 0.5);
    }

    @Override
    public byte getSize() {
        return 1;
    }

    public DoubleChestLocation extend(BlockFace direction) {
        return new DoubleChestLocation(b1, direction);
    }

}
