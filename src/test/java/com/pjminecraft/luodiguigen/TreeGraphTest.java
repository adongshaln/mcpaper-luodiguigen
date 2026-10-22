package com.pjminecraft.luodiguigen;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeGraphTest {
    @Test
    void airIsNotATree() {
        assertNull(TreeGraph.capture(new Grid(), new TreeGraph.Pos(0, 0, 0), 1));
    }

    @Test
    void singleLogWithEnoughLeavesIsATree() {
        Grid grid = canopy(new Grid().trunk(0, 0, 0), 0, 0);
        TreeGraph.Snapshot snapshot = TreeGraph.capture(grid, new TreeGraph.Pos(0, 0, 0), 6);
        assertNotNull(snapshot);
        assertEquals(new TreeGraph.Pos(0, 0, 0), snapshot.plantAt());
        assertEquals(Set.of(new TreeGraph.Pos(0, 0, 0)), snapshot.trunks());
    }

    @Test
    void tooFewLeavesIsNotATree() {
        Grid grid = new Grid().trunk(0, 0, 0).leaf(1, 0, 0).leaf(-1, 0, 0).leaf(0, 1, 0);
        assertNull(TreeGraph.capture(grid, new TreeGraph.Pos(0, 0, 0), 4));
    }

    @Test
    void playerPlacedLeavesDoNotCount() {
        Grid grid = new Grid().trunk(0, 0, 0);
        for (int i = 0; i < 8; i++) {
            grid.persistent(i + 1, 0, 0);
        }
        assertNull(TreeGraph.capture(grid, new TreeGraph.Pos(0, 0, 0), 4));
    }

    @Test
    void stumpIsRememberedAndCabinBesideItIsNot() {
        Grid grid = new Grid();
        for (int y = 0; y <= 6; y++) {
            grid.trunk(0, y, 0);
        }
        grid.trunk(1, 0, 0).trunk(2, 0, 0).trunk(3, 0, 0);
        canopy(grid, 0, 6);
        TreeGraph.Snapshot snapshot = TreeGraph.capture(grid, new TreeGraph.Pos(0, 2, 0), 6);
        assertNotNull(snapshot);
        assertEquals(new TreeGraph.Pos(0, 0, 0), snapshot.plantAt());
        assertEquals(column(0, 0, 6), snapshot.trunks());
        assertFalse(snapshot.trunks().contains(new TreeGraph.Pos(1, 0, 0)));
        assertNull(TreeGraph.capture(grid, new TreeGraph.Pos(3, 0, 0), 6));
    }

    @Test
    void diagonalBranchStaysOneTree() {
        Grid grid = new Grid();
        for (int y = 0; y <= 4; y++) {
            grid.trunk(0, y, 0);
        }
        grid.trunk(1, 5, 0);
        grid.leaf(2, 5, 0).leaf(1, 6, 0).leaf(1, 5, 1).leaf(1, 5, -1).leaf(0, 6, 0).leaf(1, 4, 0);
        TreeGraph.Snapshot snapshot = TreeGraph.capture(grid, new TreeGraph.Pos(0, 1, 0), 6);
        assertNotNull(snapshot);
        assertTrue(snapshot.trunks().contains(new TreeGraph.Pos(1, 5, 0)));
        assertEquals(new TreeGraph.Pos(0, 0, 0), snapshot.plantAt());
        assertFalse(snapshot.trunks().contains(new TreeGraph.Pos(2, 0, 0)));
    }

    @Test
    void logsTwoBlocksApartAreNotTheSameTree() {
        Grid grid = canopy(new Grid().trunk(0, 0, 0).trunk(3, 0, 0), 0, 0);
        TreeGraph.Snapshot snapshot = TreeGraph.capture(grid, new TreeGraph.Pos(0, 0, 0), 6);
        assertNotNull(snapshot);
        assertFalse(snapshot.trunks().contains(new TreeGraph.Pos(3, 0, 0)));
    }

    @Test
    void branchInsideTheCrownIsIncludedAndALongBeamIsNot() {
        Grid grid = new Grid();
        for (int y = 0; y <= 6; y++) {
            grid.trunk(0, y, 0);
        }
        for (int x = 1; x <= 12; x++) {
            grid.trunk(x, 6, 0);
        }
        grid.leaf(-1, 6, 0).leaf(0, 7, 0).leaf(0, 6, 1).leaf(0, 6, -1).leaf(-1, 7, 0).leaf(0, 5, 1);
        TreeGraph.Snapshot snapshot = TreeGraph.capture(grid, new TreeGraph.Pos(0, 1, 0), 6);
        assertNotNull(snapshot);
        assertTrue(snapshot.trunks().contains(new TreeGraph.Pos(8, 6, 0)));
        assertFalse(snapshot.trunks().contains(new TreeGraph.Pos(12, 6, 0)));
        assertEquals(new TreeGraph.Pos(0, 0, 0), snapshot.plantAt());
    }

    @Test
    void twoByTwoStumpPlantsTheSouthwestCorner() {
        Grid grid = new Grid();
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                for (int y = 0; y <= 4; y++) {
                    grid.trunk(x, y, z);
                }
            }
        }
        grid.trunk(2, 0, 0);
        grid.leaf(0, 5, 0).leaf(1, 5, 0).leaf(0, 5, 1).leaf(1, 5, 1).leaf(-1, 4, 0).leaf(2, 5, 0);
        TreeGraph.Snapshot snapshot = TreeGraph.capture(grid, new TreeGraph.Pos(1, 2, 1), 6);
        assertNotNull(snapshot);
        assertEquals(new TreeGraph.Pos(0, 0, 0), snapshot.plantAt());
        assertFalse(snapshot.trunks().contains(new TreeGraph.Pos(2, 0, 0)));
        assertEquals(20, snapshot.trunks().size());
    }

    @Test
    void leafHeldByAnotherLogIsSpared() {
        Grid grid = new Grid().trunk(0, 0, 0).foreign(2, 0, 0);
        grid.leaf(1, 0, 0).leaf(0, 1, 0).leaf(-1, 0, 0).leaf(0, 0, 1).leaf(0, 0, -1).leaf(0, -1, 0);
        TreeGraph.Snapshot snapshot = TreeGraph.capture(grid, new TreeGraph.Pos(0, 0, 0), 4);
        assertNotNull(snapshot);
        Set<TreeGraph.Pos> spared = TreeGraph.supportedLeaves(grid, snapshot);
        assertTrue(spared.contains(new TreeGraph.Pos(1, 0, 0)));
        assertFalse(spared.contains(new TreeGraph.Pos(-1, 0, 0)));
    }

    @Test
    void leavesOnlyHeldByAnotherTreeDoNotMakeALogIntoATree() {
        Grid grid = new Grid().trunk(0, 0, 0).foreign(2, 0, 0);
        grid.leaf(1, 0, 0).leaf(0, 1, 0).leaf(-1, 0, 0).leaf(0, 0, 1);
        assertNull(TreeGraph.capture(grid, new TreeGraph.Pos(0, 0, 0), 4));
    }

    @Test
    void seventhLeafIsIncludedAndTheEighthIsNot() {
        Grid grid = new Grid().trunk(0, 0, 0);
        for (int x = 1; x <= 8; x++) {
            grid.leaf(x, 0, 0);
        }
        TreeGraph.Snapshot snapshot = TreeGraph.capture(grid, new TreeGraph.Pos(0, 0, 0), 6);
        assertNotNull(snapshot);
        assertTrue(snapshot.leaves().contains(new TreeGraph.Pos(7, 0, 0)));
        assertFalse(snapshot.leaves().contains(new TreeGraph.Pos(8, 0, 0)));
    }

    @Test
    void joinedCanopiesWitherOnlyLeavesCloserToTheFelledTree() {
        Grid grid = new Grid().trunk(0, 0, 0).trunk(6, 0, 0);
        for (int x = -2; x <= 8; x++) {
            grid.leaf(x, 1, 0);
        }
        grid.leaf(-1, 0, 0).leaf(0, 0, 1).leaf(0, 0, -1).leaf(-2, 0, 0).leaf(0, 2, 0).leaf(-1, 1, 1);
        TreeGraph.Snapshot snapshot = TreeGraph.capture(grid, new TreeGraph.Pos(0, 0, 0), 6);
        assertNotNull(snapshot);
        Set<TreeGraph.Pos> spared = TreeGraph.supportedLeaves(grid, snapshot);
        assertTrue(spared.contains(new TreeGraph.Pos(5, 1, 0)));
        assertFalse(spared.contains(new TreeGraph.Pos(-1, 0, 0)));
        assertFalse(snapshot.trunks().contains(new TreeGraph.Pos(6, 0, 0)));
    }

    private static Grid canopy(Grid grid, int x, int y) {
        return grid.leaf(x + 1, y, 0).leaf(x - 1, y, 0).leaf(x, y + 1, 0)
                .leaf(x, y, 1).leaf(x, y, -1).leaf(x + 1, y + 1, 0);
    }

    private static Set<TreeGraph.Pos> column(int x, int from, int to) {
        Set<TreeGraph.Pos> logs = new HashSet<>();
        for (int y = from; y <= to; y++) {
            logs.add(new TreeGraph.Pos(x, y, 0));
        }
        return logs;
    }
}
