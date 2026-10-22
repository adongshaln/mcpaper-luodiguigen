package com.pjminecraft.luodiguigen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeTrackerTest {
    @Test
    void bottomLogBrokenFirstStillPlantsThere() {
        Grid grid = new Grid();
        for (int y = 0; y <= 4; y++) {
            grid.trunk(0, y, 0);
        }
        grid.leaf(1, 4, 0).leaf(-1, 4, 0).leaf(0, 5, 0).leaf(0, 4, 1).leaf(0, 4, -1).leaf(1, 5, 0);
        TreeTracker tracker = new TreeTracker();

        TreeTracker.Outcome first = tracker.breakLog(grid, new TreeGraph.Pos(0, 0, 0), 6);
        grid.clear(0, 0, 0);
        assertTrue(first.created());
        assertFalse(first.felled());
        assertEquals(new TreeGraph.Pos(0, 0, 0), first.snapshot().plantAt());

        for (int y = 1; y <= 3; y++) {
            TreeTracker.Outcome mid = tracker.breakLog(grid, new TreeGraph.Pos(0, y, 0), 6);
            grid.clear(0, y, 0);
            assertFalse(mid.felled());
        }

        TreeTracker.Outcome last = tracker.breakLog(grid, new TreeGraph.Pos(0, 4, 0), 6);
        assertTrue(last.felled());
        assertFalse(last.created());
        assertEquals(new TreeGraph.Pos(0, 0, 0), last.snapshot().plantAt());
    }

    @Test
    void oneLogTreeFallsImmediately() {
        Grid grid = new Grid().trunk(2, 3, 2);
        grid.leaf(3, 3, 2).leaf(1, 3, 2).leaf(2, 4, 2).leaf(2, 3, 3).leaf(2, 3, 1).leaf(3, 4, 2);
        TreeTracker.Outcome outcome = new TreeTracker().breakLog(grid, new TreeGraph.Pos(2, 3, 2), 6);
        assertTrue(outcome.created());
        assertTrue(outcome.felled());
        assertEquals(new TreeGraph.Pos(2, 3, 2), outcome.snapshot().plantAt());
    }

    @Test
    void buildingLogIsIgnored() {
        Grid grid = new Grid().trunk(0, 0, 0).trunk(1, 0, 0).trunk(2, 0, 0);
        assertNull(new TreeTracker().breakLog(grid, new TreeGraph.Pos(1, 0, 0), 6).snapshot());
    }

    @Test
    void explodedRemainderIsForgottenWithoutPlanting() {
        Grid grid = new Grid();
        for (int y = 0; y <= 4; y++) {
            grid.trunk(0, y, 0);
        }
        grid.leaf(1, 4, 0).leaf(-1, 4, 0).leaf(0, 5, 0).leaf(0, 4, 1).leaf(0, 4, -1).leaf(1, 5, 0);
        TreeTracker tracker = new TreeTracker();
        TreeTracker.Outcome first = tracker.breakLog(grid, new TreeGraph.Pos(0, 0, 0), 6);
        assertNotNull(first.snapshot());
        grid.clear(0, 0, 0);
        for (int y = 1; y <= 4; y++) {
            grid.clear(0, y, 0);
        }
        var dropped = tracker.prune(new TreeTracker.Presence() {
            @Override
            public boolean loaded(TreeGraph.Pos pos) {
                return true;
            }

            @Override
            public boolean stillTrunk(TreeGraph.Snapshot snapshot, TreeGraph.Pos pos) {
                return false;
            }
        });
        assertEquals(1, dropped.size());
        assertEquals(new TreeGraph.Pos(0, 0, 0), dropped.getFirst().plantAt());
        assertNull(tracker.breakLog(grid, new TreeGraph.Pos(0, 4, 0), 6).snapshot());
    }
}
