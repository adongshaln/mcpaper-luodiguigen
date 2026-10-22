package com.pjminecraft.luodiguigen;

import org.bukkit.Material;
import org.bukkit.Tag;

/** 方块到树种的对应。树苗材料只在真正要种下去的时候用。 */
final class Woods {
    private Woods() {
    }

    static WoodType trunk(Material type) {
        return switch (type) {
            case OAK_LOG -> WoodType.OAK;
            case SPRUCE_LOG -> WoodType.SPRUCE;
            case BIRCH_LOG -> WoodType.BIRCH;
            case JUNGLE_LOG -> WoodType.JUNGLE;
            case ACACIA_LOG -> WoodType.ACACIA;
            case DARK_OAK_LOG -> WoodType.DARK_OAK;
            case MANGROVE_LOG -> WoodType.MANGROVE;
            case CHERRY_LOG -> WoodType.CHERRY;
            case PALE_OAK_LOG -> WoodType.PALE_OAK;
            case POPLAR_LOG -> WoodType.POPLAR;
            default -> null;
        };
    }

    static boolean canopy(WoodType wood, Material type) {
        return switch (wood) {
            case OAK -> type == Material.OAK_LEAVES
                    || type == Material.AZALEA_LEAVES
                    || type == Material.FLOWERING_AZALEA_LEAVES;
            case SPRUCE -> type == Material.SPRUCE_LEAVES;
            case BIRCH -> type == Material.BIRCH_LEAVES;
            case JUNGLE -> type == Material.JUNGLE_LEAVES;
            case ACACIA -> type == Material.ACACIA_LEAVES;
            case DARK_OAK -> type == Material.DARK_OAK_LEAVES;
            case MANGROVE -> type == Material.MANGROVE_LEAVES;
            case CHERRY -> type == Material.CHERRY_LEAVES;
            case PALE_OAK -> type == Material.PALE_OAK_LEAVES;
            case POPLAR -> type == Material.ORANGE_POPLAR_LEAVES
                    || type == Material.RED_POPLAR_LEAVES
                    || type == Material.YELLOW_POPLAR_LEAVES;
        };
    }

    static boolean isKnownLeaf(Material type) {
        return switch (type) {
            case OAK_LEAVES, SPRUCE_LEAVES, BIRCH_LEAVES, JUNGLE_LEAVES, ACACIA_LEAVES, DARK_OAK_LEAVES,
                    MANGROVE_LEAVES, CHERRY_LEAVES, PALE_OAK_LEAVES, AZALEA_LEAVES, FLOWERING_AZALEA_LEAVES,
                    ORANGE_POPLAR_LEAVES, RED_POPLAR_LEAVES, YELLOW_POPLAR_LEAVES -> true;
            default -> false;
        };
    }

    static boolean isLog(Material type) {
        return Tag.LOGS.isTagged(type) || trunk(type) != null;
    }

    static Material sapling(SaplingKind kind) {
        return switch (kind) {
            case OAK -> Material.OAK_SAPLING;
            case SPRUCE -> Material.SPRUCE_SAPLING;
            case BIRCH -> Material.BIRCH_SAPLING;
            case JUNGLE -> Material.JUNGLE_SAPLING;
            case ACACIA -> Material.ACACIA_SAPLING;
            case DARK_OAK -> Material.DARK_OAK_SAPLING;
            case MANGROVE -> Material.MANGROVE_PROPAGULE;
            case CHERRY -> Material.CHERRY_SAPLING;
            case PALE_OAK -> Material.PALE_OAK_SAPLING;
            case POPLAR -> Material.POPLAR_SAPLING;
            case AZALEA -> Material.AZALEA;
            case FLOWERING_AZALEA -> Material.FLOWERING_AZALEA;
        };
    }
}
