package com.pjminecraft.luodiguigen;

import java.util.HashMap;
import java.util.Map;

/** 测试用的一小块世界。没放方块的地方是空气。 */
final class Grid implements TreeGraph.Blocks {
    enum Kind {
        TRUNK,
        LEAF,
        PERSISTENT,
        FOREIGN
    }

    private final Map<TreeGraph.Pos, Kind> cells = new HashMap<>();

    Grid trunk(int x, int y, int z) {
        return put(x, y, z, Kind.TRUNK);
    }

    Grid leaf(int x, int y, int z) {
        return put(x, y, z, Kind.LEAF);
    }

    Grid persistent(int x, int y, int z) {
        return put(x, y, z, Kind.PERSISTENT);
    }

    Grid foreign(int x, int y, int z) {
        return put(x, y, z, Kind.FOREIGN);
    }

    Grid clear(int x, int y, int z) {
        cells.remove(new TreeGraph.Pos(x, y, z));
        return this;
    }

    private Grid put(int x, int y, int z, Kind kind) {
        cells.put(new TreeGraph.Pos(x, y, z), kind);
        return this;
    }

    private Kind kind(TreeGraph.Pos pos) {
        return cells.get(pos);
    }

    @Override
    public boolean isTrunk(TreeGraph.Pos pos) {
        return kind(pos) == Kind.TRUNK;
    }

    @Override
    public boolean isLog(TreeGraph.Pos pos) {
        Kind kind = kind(pos);
        return kind == Kind.TRUNK || kind == Kind.FOREIGN;
    }

    @Override
    public boolean isCanopyLeaf(TreeGraph.Pos pos) {
        return kind(pos) == Kind.LEAF;
    }

    @Override
    public boolean isAnyLeaf(TreeGraph.Pos pos) {
        Kind kind = kind(pos);
        return kind == Kind.LEAF || kind == Kind.PERSISTENT;
    }
}
