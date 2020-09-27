package com.monst.bankingplugin.selections;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collection;
import java.util.Set;

public abstract class Selection {

	private final World world;

	Selection(World world) {
		this.world = world;
	}

	public enum SelectionType {
		CUBOID, POLYGONAL
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
	 * @return A coordinate string.
	 */
	public abstract String getCoordinates();

	/**
	 * Gets the number of blocks in this selection.
	 * 
	 * @return number of blocks
	 */
	public abstract long getVolume();
	
	/**
	 * Checks whether or not this selection overlaps with another one.
	 * @param sel The other selection
	 * @return Yes or no
	 */
	public abstract boolean overlaps(Selection sel);

	/**
	 * Returns true based on whether this selection contains the {@link Location},
	 *
	 * @param loc The location that may or may not be contained by this selection
	 * @return Whether or not the location is contained
	 */
	public abstract boolean contains(Location loc);

	public abstract boolean contains(BlockVector3D bv);

	public abstract boolean contains(BlockVector2D bv);

	/**
	 * Gets a {@link Set} with a {@link BlockVector2D} for every block in this selection,
	 * disregarding the y-coordinate.
	 *
	 * @return a set with every {@link BlockVector2D} in this selection
	 */
	public abstract Set<BlockVector2D> getBlocks();
	
	/**
	 * Get all vertices of the selection.
	 * 
	 * @return a Collection<Location> representing all vertices.
	 */
	public abstract Collection<BlockVector3D> getVertices();

	/**
	 * Returns the type of selection.
	 * 
	 * @return SelectionType.CUBOID or SelectionType.POLYGONAL
	 */
	public abstract SelectionType getType();

}
