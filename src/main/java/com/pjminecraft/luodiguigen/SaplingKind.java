package com.pjminecraft.luodiguigen;

/**
 * 原木被砍光之后种回去的那一株。橡树若树冠主要是杜鹃，则种杜鹃而不是橡树苗。
 */
public enum SaplingKind {
    OAK,
    SPRUCE,
    BIRCH,
    JUNGLE,
    ACACIA,
    DARK_OAK,
    MANGROVE,
    CHERRY,
    PALE_OAK,
    POPLAR,
    AZALEA,
    FLOWERING_AZALEA;

    public static SaplingKind of(WoodType wood) {
        return switch (wood) {
            case OAK -> OAK;
            case SPRUCE -> SPRUCE;
            case BIRCH -> BIRCH;
            case JUNGLE -> JUNGLE;
            case ACACIA -> ACACIA;
            case DARK_OAK -> DARK_OAK;
            case MANGROVE -> MANGROVE;
            case CHERRY -> CHERRY;
            case PALE_OAK -> PALE_OAK;
            case POPLAR -> POPLAR;
        };
    }

    /**
     * @param oakLeaves 橡树叶数量；杜鹃树的树干也是橡木原木，要和橡树叶分开数
     */
    public static SaplingKind choose(WoodType wood, int oakLeaves, int azaleaLeaves, int floweringAzaleaLeaves) {
        if (wood != WoodType.OAK) {
            return of(wood);
        }
        int azalea = azaleaLeaves + floweringAzaleaLeaves;
        if (azalea > oakLeaves) {
            return floweringAzaleaLeaves >= azaleaLeaves ? FLOWERING_AZALEA : AZALEA;
        }
        return OAK;
    }
}
