package com.monst.bankingplugin.selections;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CuboidSelectionTest {

    @Test
    void getMinimumPoint() {
        CuboidSelection sel = CuboidSelection.of(
                null, new BlockVector3D(0, 9, 5), new BlockVector3D(4, 0, 0));
        assertEquals(new BlockVector3D(0, 0, 0), sel.getMinimumPoint());
    }

    @Test
    void getMaximumPoint() {
        CuboidSelection sel = CuboidSelection.of(
                null, new BlockVector3D(0, 9, 5), new BlockVector3D(4, 0, 0));
        assertEquals(new BlockVector3D(4, 9, 5), sel.getMaximumPoint());
    }

    @Test
    void getCenterPoint() {
        CuboidSelection sel = CuboidSelection.of(
                null, new BlockVector3D(0, 9, 5), new BlockVector3D(4, 0, 0));
        assertEquals(new BlockVector3D(2, 4, 2), sel.getCenterPoint());
    }

    @Test
    void getVolume() {
        CuboidSelection sel = CuboidSelection.of(
                null, new BlockVector3D(0, 9, 5), new BlockVector3D(4, 0, 0));
        assertEquals(300, sel.getVolume());
    }

    @Test
    void overlaps() {
        CuboidSelection sel = CuboidSelection.of(
                null, new BlockVector3D(1, 0, 1), new BlockVector3D(5, 9, 5));
        Polygonal2DSelection polySel = Polygonal2DSelectionTest.newSel(0, 2,
                5, 2,
                5, -6);
        assertTrue(sel.overlaps(polySel));
    }
}