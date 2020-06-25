package com.monst.bankingplugin.utils;

public class BlockVector2D {

	private int x;
	private int z;

	public BlockVector2D(int x, int z) {
		this.x = x;
		this.z = z;
	}

	public int getBlockX() {
		return x;
	}

	public int getBlockZ() {
		return z;
	}

}