package com.pjminecraft.luodiguigen;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;

/** 把一棵树的判定接到某个世界、某个树种上。 */
final class LiveBlocks implements TreeGraph.Blocks {
    private final World world;
    private final WoodType wood;

    LiveBlocks(World world, WoodType wood) {
        this.world = world;
        this.wood = wood;
    }

    @Override
    public boolean isTrunk(TreeGraph.Pos pos) {
        return Woods.trunk(block(pos).getType()) == wood;
    }

    @Override
    public boolean isLog(TreeGraph.Pos pos) {
        return Woods.isLog(block(pos).getType());
    }

    @Override
    public boolean isCanopyLeaf(TreeGraph.Pos pos) {
        Block block = block(pos);
        if (!Woods.canopy(wood, block.getType())) {
            return false;
        }
        BlockData data = block.getBlockData();
        return !(data instanceof Leaves leaves && leaves.isPersistent());
    }

    @Override
    public boolean isAnyLeaf(TreeGraph.Pos pos) {
        var type = block(pos).getType();
        return org.bukkit.Tag.LEAVES.isTagged(type) || Woods.isKnownLeaf(type);
    }

    private Block block(TreeGraph.Pos pos) {
        return world.getBlockAt(pos.x(), pos.y(), pos.z());
    }
}
