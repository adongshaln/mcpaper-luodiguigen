package com.pjminecraft.luodiguigen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 记下还没砍完的树。主线程使用。
 * 第一刀记下整棵树和最底部的原木；最后一刀才算砍光。
 */
public final class TreeTracker {
    private final Map<TreeGraph.Pos, Set<TreeGraph.Pos>> byLog = new HashMap<>();
    private final Map<Set<TreeGraph.Pos>, TreeGraph.Snapshot> byRemaining = new IdentityHashMap<>();

    public static final class Outcome {
        private final TreeGraph.Snapshot snapshot;
        private final boolean created;
        private final boolean felled;

        private Outcome(TreeGraph.Snapshot snapshot, boolean created, boolean felled) {
            this.snapshot = snapshot;
            this.created = created;
            this.felled = felled;
        }

        static final Outcome NONE = new Outcome(null, false, false);

        public TreeGraph.Snapshot snapshot() {
            return snapshot;
        }

        public boolean created() {
            return created;
        }

        public boolean felled() {
            return felled;
        }
    }

    /** 某一格上记下的树还在不在。区块没加载时应当当作还在，避免把树忘掉。 */
    public interface Presence {
        boolean loaded(TreeGraph.Pos pos);

        boolean stillTrunk(TreeGraph.Snapshot snapshot, TreeGraph.Pos pos);
    }

    public Outcome breakLog(TreeGraph.Blocks blocks, TreeGraph.Pos pos, int minimumLeaves) {
        Set<TreeGraph.Pos> remaining = byLog.remove(pos);
        if (remaining != null) {
            return remove(remaining, pos, false);
        }
        TreeGraph.Snapshot snapshot = TreeGraph.capture(blocks, pos, minimumLeaves);
        if (snapshot == null) {
            return Outcome.NONE;
        }
        for (TreeGraph.Pos log : snapshot.trunks()) {
            Set<TreeGraph.Pos> existing = byLog.get(log);
            if (existing == null) {
                continue;
            }
            return remove(existing, pos, false);
        }
        Set<TreeGraph.Pos> left = new HashSet<>(snapshot.trunks());
        left.remove(pos);
        if (left.isEmpty()) {
            return new Outcome(snapshot, true, true);
        }
        byRemaining.put(left, snapshot);
        for (TreeGraph.Pos log : left) {
            byLog.put(log, left);
        }
        return new Outcome(snapshot, true, false);
    }

    /**
     * 原木已经不在了（爆炸、活塞）就从记录里拿掉。
     * 这样空掉的树不种树苗：不是玩家把最后一刀砍完的。
     */
    public List<TreeGraph.Snapshot> prune(Presence presence) {
        Iterator<Map.Entry<TreeGraph.Pos, Set<TreeGraph.Pos>>> logs = byLog.entrySet().iterator();
        while (logs.hasNext()) {
            Map.Entry<TreeGraph.Pos, Set<TreeGraph.Pos>> entry = logs.next();
            TreeGraph.Pos pos = entry.getKey();
            if (!presence.loaded(pos)) {
                continue;
            }
            TreeGraph.Snapshot snapshot = byRemaining.get(entry.getValue());
            if (snapshot != null && presence.stillTrunk(snapshot, pos)) {
                continue;
            }
            entry.getValue().remove(pos);
            logs.remove();
        }
        List<TreeGraph.Snapshot> dropped = new ArrayList<>();
        Iterator<Map.Entry<Set<TreeGraph.Pos>, TreeGraph.Snapshot>> trees = byRemaining.entrySet().iterator();
        while (trees.hasNext()) {
            Map.Entry<Set<TreeGraph.Pos>, TreeGraph.Snapshot> entry = trees.next();
            if (!entry.getKey().isEmpty()) {
                continue;
            }
            dropped.add(entry.getValue());
            trees.remove();
        }
        return dropped;
    }

    public void clear() {
        byLog.clear();
        byRemaining.clear();
    }

    private Outcome remove(Set<TreeGraph.Pos> remaining, TreeGraph.Pos pos, boolean created) {
        remaining.remove(pos);
        TreeGraph.Snapshot snapshot = byRemaining.get(remaining);
        if (!remaining.isEmpty()) {
            return new Outcome(snapshot, created, false);
        }
        byRemaining.remove(remaining);
        return new Outcome(snapshot, created, true);
    }
}
