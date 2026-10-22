package com.pjminecraft.luodiguigen;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.MangrovePropagule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 玩家砍断原木时认出整棵树；最后一刀的下一刻让树叶枯萎并种回树苗。 */
final class FellListener implements Listener {
    private final LuoDiGuiGenPlugin plugin;
    private final Map<UUID, TreeTracker> trackers = new HashMap<>();
    private final Map<TreeGraph.Snapshot, SaplingKind> kinds = new IdentityHashMap<>();
    private final Map<TreeGraph.Snapshot, WoodType> woods = new IdentityHashMap<>();

    FellListener(LuoDiGuiGenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Settings settings = plugin.settings();
        if (!settings.enabled() || !settings.allows(event.getBlock().getWorld())) {
            return;
        }
        Block block = event.getBlock();
        WoodType wood = Woods.trunk(block.getType());
        if (wood == null) {
            return;
        }
        World world = block.getWorld();
        TreeGraph.Pos pos = new TreeGraph.Pos(block.getX(), block.getY(), block.getZ());
        TreeTracker.Outcome outcome = tracker(world).breakLog(new LiveBlocks(world, wood), pos, settings.minimumLeaves());
        TreeGraph.Snapshot snapshot = outcome.snapshot();
        if (snapshot == null) {
            return;
        }
        if (outcome.created()) {
            kinds.put(snapshot, saplingKind(world, wood, snapshot));
            woods.put(snapshot, wood);
        }
        if (!outcome.felled()) {
            return;
        }
        SaplingKind kind = kinds.remove(snapshot);
        woods.remove(snapshot);
        if (kind == null) {
            kind = SaplingKind.of(wood);
        }
        UUID worldId = world.getUID();
        SaplingKind sapling = kind;
        // 这一刀的方块要等事件结束才变成空气，树苗得下一刻再种。
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!plugin.isEnabled()) {
                return;
            }
            World live = plugin.getServer().getWorld(worldId);
            if (live == null) {
                return;
            }
            finish(live, wood, snapshot, sapling);
        });
    }

    void prune() {
        List<World> worlds = List.copyOf(plugin.getServer().getWorlds());
        for (World world : worlds) {
            TreeTracker tracker = trackers.get(world.getUID());
            if (tracker == null) {
                continue;
            }
            List<TreeGraph.Snapshot> dropped = tracker.prune(new TreeTracker.Presence() {
                @Override
                public boolean loaded(TreeGraph.Pos pos) {
                    return world.isChunkLoaded(pos.x() >> 4, pos.z() >> 4);
                }

                @Override
                public boolean stillTrunk(TreeGraph.Snapshot snapshot, TreeGraph.Pos pos) {
                    WoodType expected = woods.get(snapshot);
                    if (expected == null) {
                        return false;
                    }
                    return Woods.trunk(world.getBlockAt(pos.x(), pos.y(), pos.z()).getType()) == expected;
                }
            });
            for (TreeGraph.Snapshot snapshot : dropped) {
                kinds.remove(snapshot);
                woods.remove(snapshot);
            }
        }
    }

    private void finish(World world, WoodType wood, TreeGraph.Snapshot snapshot, SaplingKind kind) {
        Settings settings = plugin.settings();
        Set<TreeGraph.Pos> spared = TreeGraph.supportedLeaves(new LiveBlocks(world, wood), snapshot);
        boolean sounded = false;
        for (TreeGraph.Pos pos : snapshot.leaves()) {
            if (spared.contains(pos)) {
                continue;
            }
            if (!world.isChunkLoaded(pos.x() >> 4, pos.z() >> 4)) {
                continue;
            }
            Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
            if (!Woods.canopy(wood, block.getType())) {
                continue;
            }
            BlockData data = block.getBlockData();
            if (data instanceof Leaves leaves && leaves.isPersistent()) {
                continue;
            }
            boolean waterlogged = data instanceof Waterlogged logged && logged.isWaterlogged();
            block.setType(waterlogged ? Material.WATER : Material.AIR, false);
            if (settings.effects()) {
                world.spawnParticle(Particle.BLOCK, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5, 4, 0.25, 0.25, 0.25, 0.0, data);
                sounded = true;
            }
        }
        TreeGraph.Pos at = snapshot.plantAt();
        if (settings.effects() && sounded) {
            world.playSound(new Location(world, at.x() + 0.5, at.y() + 0.5, at.z() + 0.5), witherSound(kind), 0.7f, 0.85f);
        }
        plant(world, at, kind);
    }

    private void plant(World world, TreeGraph.Pos at, SaplingKind kind) {
        if (!world.isChunkLoaded(at.x() >> 4, at.z() >> 4)) {
            return;
        }
        Block block = world.getBlockAt(at.x(), at.y(), at.z());
        Material current = block.getType();
        boolean water = current == Material.WATER;
        if (water && kind != SaplingKind.MANGROVE) {
            occupied(world, at);
            return;
        }
        if (!water && !current.isAir() && !Tag.REPLACEABLE.isTagged(current)) {
            occupied(world, at);
            return;
        }
        block.setType(Woods.sapling(kind), false);
        if (block.getBlockData() instanceof MangrovePropagule propagule) {
            propagule.setHanging(false);
            propagule.setAge(0);
            propagule.setWaterlogged(water);
            block.setBlockData(propagule, false);
        }
    }

    private void occupied(World world, TreeGraph.Pos at) {
        plugin.getLogger().warning("落地归根没能在 " + world.getName() + " "
                + at.x() + " " + at.y() + " " + at.z() + " 种下树苗，那一格被占住了");
    }

    private SaplingKind saplingKind(World world, WoodType wood, TreeGraph.Snapshot snapshot) {
        Set<TreeGraph.Pos> spared = TreeGraph.supportedLeaves(new LiveBlocks(world, wood), snapshot);
        int oak = 0;
        int azalea = 0;
        int flowering = 0;
        for (TreeGraph.Pos pos : snapshot.leaves()) {
            if (spared.contains(pos)) {
                continue;
            }
            switch (world.getBlockAt(pos.x(), pos.y(), pos.z()).getType()) {
                case OAK_LEAVES -> oak++;
                case AZALEA_LEAVES -> azalea++;
                case FLOWERING_AZALEA_LEAVES -> flowering++;
                default -> {
                }
            }
        }
        return SaplingKind.choose(wood, oak, azalea, flowering);
    }

    private TreeTracker tracker(World world) {
        return trackers.computeIfAbsent(world.getUID(), id -> new TreeTracker());
    }

    private static Sound witherSound(SaplingKind kind) {
        return switch (kind) {
            case CHERRY -> Sound.BLOCK_CHERRY_LEAVES_BREAK;
            case AZALEA, FLOWERING_AZALEA -> Sound.BLOCK_AZALEA_LEAVES_BREAK;
            case POPLAR -> Sound.BLOCK_POPLAR_LEAVES_BREAK;
            default -> Sound.BLOCK_GRASS_BREAK;
        };
    }
}
