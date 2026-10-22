package com.pjminecraft.luodiguigen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从一格原木认出整棵树。
 *
 * <p>树干按 26 邻接连通（金合欢的枝是斜着接上的）。贴着树叶的原木算树冠；
 * 树冠高度上、水平距离 {@link #CANOPY_RADIUS} 以内的原木也算（横枝）；
 * 再从贴着树叶的原木沿同一列往下收到树桩。旁边的木屋因此不会被算进这棵树。
 * 另一棵树的原木撑得住的树叶不会算作“这棵树自己的树叶”。
 */
public final class TreeGraph {
    /** 原版树叶在距离原木 7 格时凋零，6 格及以内留着。这里连最外一圈一起收走。 */
    public static final int LEAF_REACH = 7;
    public static final int SUPPORT_REACH = 6;
    /** 横枝离开树冠的最大水平距离。再远就当是建筑。 */
    public static final int CANOPY_RADIUS = 8;
    /** 连成一片的同种原木超过这个数，就当是建筑，整棵不处理。 */
    public static final int MAX_TRUNKS = 512;
    public static final int MAX_LEAVES = 4096;
    private static final long MAX_SUPPORT_VOLUME = 120_000L;

    private TreeGraph() {
    }

    public record Pos(int x, int y, int z) {
    }

    /** 调用方提供方块种类。测试用内存网格，服务器上再接到世界。 */
    public interface Blocks {
        boolean isTrunk(Pos pos);

        /** 任何能撑住树叶的原木，包括别的树种和去皮原木。 */
        boolean isLog(Pos pos);

        /** 这棵树自己的、会凋零的树叶。玩家放置的树叶（persistent）不算。 */
        boolean isCanopyLeaf(Pos pos);

        /** 任何树叶，支撑距离要能穿过去。 */
        boolean isAnyLeaf(Pos pos);
    }

    public static final class Snapshot {
        private final Set<Pos> trunks;
        private final Set<Pos> leaves;
        private final Pos plantAt;
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;

        private Snapshot(Set<Pos> trunks, Set<Pos> leaves, Pos plantAt, int[] bounds) {
            this.trunks = Collections.unmodifiableSet(new HashSet<>(trunks));
            this.leaves = Collections.unmodifiableSet(new HashSet<>(leaves));
            this.plantAt = plantAt;
            this.minX = bounds[0];
            this.minY = bounds[1];
            this.minZ = bounds[2];
            this.maxX = bounds[3];
            this.maxY = bounds[4];
            this.maxZ = bounds[5];
        }

        public Set<Pos> trunks() {
            return trunks;
        }

        public Set<Pos> leaves() {
            return leaves;
        }

        /** 这棵树被认出来时，最底部原木所在的那一格。 */
        public Pos plantAt() {
            return plantAt;
        }
    }

    /**
     * @param minimumLeaves 至少要有这么多片“只有这棵树撑着”的自然树叶，才当作树
     * @return 认不出来时返回 null（建筑、树叶太少、或大得不像一棵树）
     */
    public static Snapshot capture(Blocks blocks, Pos origin, int minimumLeaves) {
        if (!blocks.isTrunk(origin)) {
            return null;
        }
        Set<Pos> connected = floodTrunks(blocks, origin);
        if (connected == null) {
            return null;
        }
        Set<Pos> trunks = selectTrunks(blocks, connected);
        if (trunks.isEmpty() || !trunks.contains(origin)) {
            return null;
        }
        Set<Pos> leaves = floodLeaves(blocks, trunks);
        if (leaves == null) {
            return null;
        }
        int[] bounds = bounds(trunks, leaves);
        Set<Pos> supported = supportedByOtherLogs(blocks, trunks, leaves, bounds);
        int exclusive = 0;
        for (Pos leaf : leaves) {
            if (!supported.contains(leaf)) {
                exclusive++;
            }
        }
        if (exclusive < minimumLeaves) {
            return null;
        }
        return new Snapshot(trunks, leaves, plantAt(trunks), bounds);
    }

    /** 这些树叶现在仍被“别的原木”撑着，不该枯萎。 */
    public static Set<Pos> supportedLeaves(Blocks blocks, Snapshot snapshot) {
        int[] bounds = new int[] {
                snapshot.minX, snapshot.minY, snapshot.minZ,
                snapshot.maxX, snapshot.maxY, snapshot.maxZ
        };
        return supportedByOtherLogs(blocks, snapshot.trunks, snapshot.leaves, bounds);
    }

    private static Set<Pos> floodTrunks(Blocks blocks, Pos origin) {
        Set<Pos> trunks = new HashSet<>();
        ArrayDeque<Pos> queue = new ArrayDeque<>();
        trunks.add(origin);
        queue.add(origin);
        while (!queue.isEmpty()) {
            Pos pos = queue.removeFirst();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Pos next = new Pos(pos.x + dx, pos.y + dy, pos.z + dz);
                        if (!trunks.add(next)) {
                            continue;
                        }
                        if (!blocks.isTrunk(next)) {
                            trunks.remove(next);
                            continue;
                        }
                        if (trunks.size() > MAX_TRUNKS) {
                            return null;
                        }
                        queue.add(next);
                    }
                }
            }
        }
        return trunks;
    }

    private static Set<Pos> selectTrunks(Blocks blocks, Set<Pos> connected) {
        Set<Pos> crown = new HashSet<>();
        for (Pos log : connected) {
            if (touchesCanopy(blocks, log)) {
                crown.add(log);
            }
        }
        if (crown.isEmpty()) {
            return Set.of();
        }
        int minCrownY = Integer.MAX_VALUE;
        for (Pos log : crown) {
            minCrownY = Math.min(minCrownY, log.y);
        }
        Set<Pos> tree = new HashSet<>(crown);
        for (Pos log : connected) {
            if (log.y < minCrownY) {
                continue;
            }
            if (nearCrown(log, crown)) {
                tree.add(log);
            }
        }
        ArrayDeque<Pos> down = new ArrayDeque<>(crown);
        Set<Pos> walked = new HashSet<>();
        while (!down.isEmpty()) {
            Pos log = down.removeFirst();
            Pos below = new Pos(log.x, log.y - 1, log.z);
            // 下一格可能已经因为贴着树冠被收进来了，仍然要继续往下走到树桩。
            if (!connected.contains(below) || !walked.add(below)) {
                continue;
            }
            tree.add(below);
            down.add(below);
        }
        return tree;
    }

    private static boolean touchesCanopy(Blocks blocks, Pos log) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    if (blocks.isCanopyLeaf(new Pos(log.x + dx, log.y + dy, log.z + dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean nearCrown(Pos log, Set<Pos> crown) {
        for (Pos center : crown) {
            int dx = Math.abs(log.x - center.x);
            int dz = Math.abs(log.z - center.z);
            if (Math.max(dx, dz) <= CANOPY_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static Set<Pos> floodLeaves(Blocks blocks, Set<Pos> trunks) {
        Set<Pos> seen = new HashSet<>(trunks);
        Set<Pos> leaves = new HashSet<>();
        ArrayDeque<Pos> queue = new ArrayDeque<>(trunks);
        Map<Pos, Integer> distance = new HashMap<>();
        for (Pos trunk : trunks) {
            distance.put(trunk, 0);
        }
        int visited = 0;
        while (!queue.isEmpty()) {
            if (++visited > MAX_LEAVES * 2) {
                return null;
            }
            Pos pos = queue.removeFirst();
            int current = distance.get(pos);
            if (current >= LEAF_REACH) {
                continue;
            }
            for (Pos next : leafNeighbors(pos)) {
                if (!seen.add(next) || !blocks.isCanopyLeaf(next)) {
                    continue;
                }
                distance.put(next, current + 1);
                leaves.add(next);
                if (leaves.size() > MAX_LEAVES) {
                    return null;
                }
                queue.add(next);
            }
        }
        return leaves;
    }

    /**
     * 树叶连成一片时，按离哪边的原木更近来分。
     * 另一棵树的原木不比这棵更远，这片叶子就留给它，不跟着枯萎。
     */
    private static Set<Pos> supportedByOtherLogs(Blocks blocks, Set<Pos> ownTrunks, Set<Pos> leaves, int[] bounds) {
        if (leaves.isEmpty()) {
            return Set.of();
        }
        Map<Pos, Integer> ownDistance = new HashMap<>();
        ArrayDeque<Pos> queue = new ArrayDeque<>();
        for (Pos trunk : ownTrunks) {
            ownDistance.put(trunk, 0);
            queue.add(trunk);
        }
        Set<Pos> foreignLogs = new HashSet<>();
        while (!queue.isEmpty()) {
            Pos pos = queue.removeFirst();
            int current = ownDistance.get(pos);
            for (Pos next : leafNeighbors(pos)) {
                if (blocks.isLog(next) && !ownTrunks.contains(next)) {
                    foreignLogs.add(next);
                }
                if (current >= LEAF_REACH || ownDistance.containsKey(next) || !leaves.contains(next)) {
                    continue;
                }
                ownDistance.put(next, current + 1);
                queue.add(next);
            }
        }
        if (foreignLogs.isEmpty()) {
            return Set.of();
        }
        Map<Pos, Integer> foreignDistance = new HashMap<>();
        queue.clear();
        for (Pos log : foreignLogs) {
            foreignDistance.put(log, 0);
            queue.add(log);
        }
        Set<Pos> supported = new HashSet<>();
        while (!queue.isEmpty()) {
            Pos pos = queue.removeFirst();
            int current = foreignDistance.get(pos);
            if (current >= LEAF_REACH) {
                continue;
            }
            for (Pos next : leafNeighbors(pos)) {
                if (foreignDistance.containsKey(next) || !blocks.isAnyLeaf(next)) {
                    continue;
                }
                int nextDistance = current + 1;
                foreignDistance.put(next, nextDistance);
                queue.add(next);
                Integer own = ownDistance.get(next);
                if (leaves.contains(next) && own != null && nextDistance <= own) {
                    supported.add(next);
                }
            }
        }
        return supported;
    }

    /** 同一高度上离树冠中心最近的那格；并列时取 x 更小、再取 z 更小的。 */
    static Pos plantAt(Set<Pos> trunks) {
        int minY = Integer.MAX_VALUE;
        for (Pos pos : trunks) {
            minY = Math.min(minY, pos.y);
        }
        long sumX = 0;
        long sumZ = 0;
        int count = 0;
        for (Pos pos : trunks) {
            if (pos.y != minY) {
                continue;
            }
            sumX += pos.x;
            sumZ += pos.z;
            count++;
        }
        Pos best = null;
        long bestDistance = 0;
        for (Pos pos : trunks) {
            if (pos.y != minY) {
                continue;
            }
            long dx = (long) pos.x * count - sumX;
            long dz = (long) pos.z * count - sumZ;
            long distance = dx * dx + dz * dz;
            if (best == null || distance < bestDistance
                    || (distance == bestDistance && (pos.x < best.x || (pos.x == best.x && pos.z < best.z)))) {
                best = pos;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static int[] bounds(Set<Pos> trunks, Set<Pos> leaves) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (Set<Pos> group : List.of(trunks, leaves)) {
            for (Pos pos : group) {
                minX = Math.min(minX, pos.x);
                minY = Math.min(minY, pos.y);
                minZ = Math.min(minZ, pos.z);
                maxX = Math.max(maxX, pos.x);
                maxY = Math.max(maxY, pos.y);
                maxZ = Math.max(maxZ, pos.z);
            }
        }
        return new int[] {minX, minY, minZ, maxX, maxY, maxZ};
    }

    /** 树叶斜着挨在一起也算连着，森林里两顶树冠经常只在角上碰上。 */
    private static List<Pos> leafNeighbors(Pos pos) {
        List<Pos> neighbors = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    neighbors.add(new Pos(pos.x + dx, pos.y + dy, pos.z + dz));
                }
            }
        }
        return neighbors;
    }

    private static List<Pos> faceNeighbors(Pos pos) {
        return List.of(
                new Pos(pos.x + 1, pos.y, pos.z),
                new Pos(pos.x - 1, pos.y, pos.z),
                new Pos(pos.x, pos.y + 1, pos.z),
                new Pos(pos.x, pos.y - 1, pos.z),
                new Pos(pos.x, pos.y, pos.z + 1),
                new Pos(pos.x, pos.y, pos.z - 1));
    }
}
