package com.monst.bankingplugin.geo.selections;

import com.monst.bankingplugin.geo.BlockVector2D;
import com.monst.bankingplugin.geo.BlockVector3D;
import com.monst.bankingplugin.geo.locations.ChestLocation;
import com.monst.bankingplugin.utils.Utils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class Selection {

	private final World world;

	Selection(World world) {
		this.world = world;
	}

	/**
	 * @return the point on the bounding box of this {@link Selection} with the lowest x, y, and z values.
	 */
    public BlockVector3D getMinimumPoint() {
		return new BlockVector3D(getMinX(), getMinY(), getMinZ());
	}

	/**
	 * @return the point on the bounding box of this {@link Selection} with the highest x, y, and z values.
	 */
	public BlockVector3D getMaximumPoint() {
		return new BlockVector3D(getMaxX(), getMaxY(), getMaxZ());
	}

	/**
	 * Gets the center point of this selection.
	 *
	 * @return the center point
	 */
	public abstract BlockVector3D getCenterPoint();

	public Location getTeleportLocation() {
		return Utils.getSafeLocation(getCenterPoint().toLocation(world));
	}

	/**
	 * @return the minimum x-coordinate of this {@link Selection}
	 */
	public abstract int getMinX();

	/**
	 * @return the maximum x-coordinate of this {@link Selection}
	 */
	public abstract int getMaxX();

	/**
	 * @return the minimum y-coordinate of this {@link Selection}
	 */
	public abstract int getMinY();

	/**
	 * @return the maximum y-coordinate of this {@link Selection}
	 */
	public abstract int getMaxY();

	/**
	 * @return the minimum z-coordinate of this {@link Selection}
	 */
	public abstract int getMinZ();

	/**
	 * @return the maximum z-coordinate of this {@link Selection}
	 */
	public abstract int getMaxZ();

	/**
	 * @return the world this {@link Selection} is in
	 */
	public World getWorld() {
		return world;
	}

	/**
	 * Gets a {@link String} that illustrates the location of this selection.
	 *
	 * @return a coordinate string
	 */
	public abstract String getCoordinates();

	/**
	 * Gets the number of blocks in this selection.
	 *
	 * @return the number of blocks
	 */
	public abstract long getVolume();

	/**
	 * @param sel The other selection
	 * @return whether or not this selection overlaps with another one
	 */
	public abstract boolean overlaps(Selection sel);

	/**
	 * @param sel The other selection
	 * @return whether this selection *cannot* overlap with the other selection
	 */
	public final boolean isDisjunct(Selection sel) {
		return getMinX() > sel.getMaxX() || getMaxX() < sel.getMinX() ||
				getMinY() > sel.getMaxY() || getMaxY() < sel.getMinY() ||
				getMinZ() > sel.getMaxZ() || getMaxZ() < sel.getMinZ();
	}

	public boolean contains(ChestLocation chest) {
		if (!Objects.equals(getWorld(), chest.getWorld()))
			return false;
		for (Block chestSide : chest)
			if (!contains(BlockVector3D.fromBlock(chestSide)))
				return false;
		return true;
	}

	/**
	 * Returns true based on whether this selection contains the {@link Location},
	 *
	 * @param block The block that may or may not be contained by this selection
	 * @return Whether or not the location is contained
	 */
	public abstract boolean contains(Block block);

	public abstract boolean contains(BlockVector3D bv);

	public abstract boolean contains(BlockVector2D bv);

	/**
	 * Gets a {@link Set<BlockVector2D>} containing a horizontal cross-section
	 * of this selection with no y-coordinate.
	 *
	 * @return a set with every {@link BlockVector2D} in this selection
	 */
	public abstract Set<BlockVector2D> getFootprint();

	public abstract List<BlockVector2D> getVertices();

	/**
	 * Get all corners of this selection.
	 *
	 * @return a Collection<Location> representing all vertices.
	 */
	public abstract Collection<BlockVector3D> getCorners();

	public boolean isCuboid() {
		return false;
	}

	public boolean isPolygonal() {
		return false;
	}

}
