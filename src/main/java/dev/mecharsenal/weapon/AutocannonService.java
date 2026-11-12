package dev.mecharsenal.weapon;

import dev.letitwalk.content.PilotControlsBlockEntity;
import dev.letitwalk.content.PilotStation;
import dev.letitwalk.content.RemoteControllerItem;
import dev.letitwalk.walker.WalkerTracker;
import dev.mecharsenal.block.StationaryCannonBlock;
import dev.mecharsenal.block.entity.AmmoMagazineBlockEntity;
import dev.mecharsenal.block.entity.AutocannonMountBlockEntity;
import dev.mecharsenal.entity.HeavyShellProjectile;
import dev.mecharsenal.network.PilotStatusPayload;
import dev.mecharsenal.network.RecoilPayload;
import dev.mecharsenal.registry.ModBlocks;
import dev.mecharsenal.registry.ModItems;
import dev.mecharsenal.registry.ModParticles;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AutocannonService {
    private static final int SHOT_INTERVAL_TICKS = 4;
    private static final int RANGE_BLOCKS = 48;
    private static final float DAMAGE = 5.0F;
    private static final int MOUNT_SEARCH_RADIUS = 16;
    private static final int MOUNT_RESCAN_TICKS = 20;
    private static final int WARNING_INTERVAL_TICKS = 40;
    private static final Map<UUID, Integer> NEXT_SHOT_TICK = new HashMap<>();
    private static final Map<UUID, Integer> NEXT_WARNING_TICK = new HashMap<>();
    private static final Map<UUID, Integer> NEXT_STATUS_TICK = new HashMap<>();
    private static final Map<UUID, CachedMounts> MOUNT_CACHE = new HashMap<>();
    private static final SoundEvent CANNON_SOUND = BuiltInRegistries.SOUND_EVENT.getOptional(
            ResourceLocation.fromNamespaceAndPath("createbigcannons", "fire_big_cannon")
    ).orElse(SoundEvents.GENERIC_EXPLODE.value());

    private AutocannonService() {
    }

    public static void handleInput(ServerPlayer player, BlockPos pilotControls, boolean firing) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        PilotWalker pilotWalker = findPilotedWalker(player, pilotControls);
        if (pilotWalker != null) {
            sendPilotStatus(player, pilotWalker);
            fireMountedCannons(player, level, pilotWalker, firing);
            return;
        }

        if (!firing) {
            return;
        }
        if (isHoldingAutocannon(player)) {
            fireHandheld(player, level);
        } else {
            warn(player, "message.mech_arsenal.no_walker");
        }
    }

    public static void fireStationary(ServerPlayer player, ServerLevel level, BlockPos cannonPos) {
        if (player.tickCount < NEXT_SHOT_TICK.getOrDefault(player.getUUID(), 0)) {
            return;
        }
        if (!level.getBlockState(cannonPos).is(ModBlocks.STATIONARY_CANNON.get())) {
            return;
        }
        if (!consumeRound(player, null, cannonPos, level, ModItems.HEAVY_SHELL.get())) {
            warn(player, "message.mech_arsenal.no_heavy_shells");
            return;
        }

        BlockState state = level.getBlockState(cannonPos);
        var facing = state.getValue(StationaryCannonBlock.FACING);
        Vec3 direction = Vec3.atLowerCornerOf(facing.getNormal()).normalize();
        fireHeavyShell(player, level, StationaryCannonBlock.muzzlePosition(cannonPos, facing), direction);
        NEXT_SHOT_TICK.put(player.getUUID(), player.tickCount + SHOT_INTERVAL_TICKS);
        NEXT_WARNING_TICK.remove(player.getUUID());
    }

    public static void clear(ServerPlayer player) {
        NEXT_SHOT_TICK.remove(player.getUUID());
        NEXT_WARNING_TICK.remove(player.getUUID());
        NEXT_STATUS_TICK.remove(player.getUUID());
    }

    public static void clearAll() {
        NEXT_SHOT_TICK.clear();
        NEXT_WARNING_TICK.clear();
        NEXT_STATUS_TICK.clear();
        MOUNT_CACHE.clear();
    }

    private static void fireMountedCannons(
            ServerPlayer player,
            ServerLevel level,
            PilotWalker pilotWalker,
            boolean firing
    ) {
        List<BlockPos> mounts = findMountedCannons(player, pilotWalker);
        if (mounts.isEmpty()) {
            if (firing) {
                warn(player, "message.mech_arsenal.no_mount");
            }
            return;
        }

        Vec3 targetWorld = player.getEyePosition().add(player.getLookAngle().scale(RANGE_BLOCKS));
        var pose = pilotWalker.subLevel().logicalPose();
        Vec3 targetLocal = pose.transformPositionInverse(targetWorld);
        List<MountedShot> shots = new ArrayList<>();

        for (BlockPos pos : mounts) {
            LevelChunk chunk = getChunkAt(pilotWalker.subLevel(), new ChunkPos(pos));
            if (chunk == null) {
                continue;
            }

            BlockState state = chunk.getBlockState(pos);
            Vec3 muzzleLocal;
            Vec3 localDirection;
            if (state.is(ModBlocks.AUTOCANNON_MOUNT.get())
                    && chunk.getBlockEntity(pos) instanceof AutocannonMountBlockEntity cannon) {
                muzzleLocal = autocannonMuzzlePosition(pos, cannon);
                for (int i = 0; i < 2; i++) {
                    Vec3 targetDirection = targetLocal.subtract(muzzleLocal).normalize();
                    cannon.setAim(
                            (float) Math.atan2(-targetDirection.x, -targetDirection.z),
                            (float) Math.atan2(targetDirection.y, Math.sqrt(
                                    targetDirection.x * targetDirection.x + targetDirection.z * targetDirection.z
                            ))
                    );
                    muzzleLocal = autocannonMuzzlePosition(pos, cannon);
                }
                localDirection = cannon.getAimDirection();
            } else if (state.is(ModBlocks.STATIONARY_CANNON.get())) {
                localDirection = Vec3.atLowerCornerOf(state.getValue(StationaryCannonBlock.FACING).getNormal())
                        .normalize();
                muzzleLocal = StationaryCannonBlock.muzzlePosition(pos, state.getValue(StationaryCannonBlock.FACING));
            } else {
                continue;
            }

            Vec3 worldMuzzle = pose.transformPosition(muzzleLocal);
            Vec3 worldDirection = pose.transformNormal(localDirection).normalize();
            shots.add(new MountedShot(worldMuzzle, worldDirection, state.is(ModBlocks.STATIONARY_CANNON.get())));
        }

        if (shots.isEmpty()) {
            if (firing) {
                warn(player, "message.mech_arsenal.no_mount");
            }
            return;
        }

        if (!firing) {
            return;
        }

        if (player.tickCount < NEXT_SHOT_TICK.getOrDefault(player.getUUID(), 0)) {
            return;
        }

        boolean fired = false;
        for (MountedShot shot : shots) {
            Item ammunition = shot.heavy() ? ModItems.HEAVY_SHELL.get() : ModItems.AUTOCANNON_ROUND.get();
            if (!consumeRound(player, pilotWalker, null, null, ammunition)) {
                continue;
            }
            if (shot.heavy()) {
                fireHeavyShell(player, level, shot.muzzle(), shot.direction());
            } else {
                fireShot(player, level, shot.muzzle(), shot.direction());
            }
            fired = true;
        }

        if (fired) {
            NEXT_SHOT_TICK.put(player.getUUID(), player.tickCount + SHOT_INTERVAL_TICKS);
            NEXT_WARNING_TICK.remove(player.getUUID());
        } else {
            warn(player, "message.mech_arsenal.no_mounted_ammo");
        }
    }

    private static void fireHandheld(ServerPlayer player, ServerLevel level) {
        if (player.tickCount < NEXT_SHOT_TICK.getOrDefault(player.getUUID(), 0)) {
            return;
        }
        if (!consumeRound(player, null, null, null, ModItems.AUTOCANNON_ROUND.get())) {
            warn(player, "message.mech_arsenal.no_rounds");
            return;
        }

        Vec3 direction = player.getLookAngle()
                .add(
                        player.getRandom().triangle(0.0, 0.003),
                        player.getRandom().triangle(0.0, 0.003),
                        player.getRandom().triangle(0.0, 0.003)
                )
                .normalize();
        fireShot(player, level, player.getEyePosition(), direction);
        NEXT_SHOT_TICK.put(player.getUUID(), player.tickCount + SHOT_INTERVAL_TICKS);
        NEXT_WARNING_TICK.remove(player.getUUID());
    }

    private static List<BlockPos> findMountedCannons(ServerPlayer player, PilotWalker pilotWalker) {
        SubLevel walkerLevel = pilotWalker.subLevel();
        BlockPos corePos = pilotWalker.corePos();
        UUID subLevelId = walkerLevel.getUniqueId();
        long gameTime = player.serverLevel().getGameTime();
        CachedMounts cached = MOUNT_CACHE.get(subLevelId);
        if (cached != null && gameTime < cached.rescanAt()) {
            return cached.positions().stream()
                    .filter(pos -> isMountedCannon(walkerLevel, pos))
                    .toList();
        }

        BlockPos min = corePos.offset(-MOUNT_SEARCH_RADIUS, -MOUNT_SEARCH_RADIUS, -MOUNT_SEARCH_RADIUS);
        BlockPos max = corePos.offset(MOUNT_SEARCH_RADIUS, MOUNT_SEARCH_RADIUS, MOUNT_SEARCH_RADIUS);
        List<BlockPos> found = new ArrayList<>();
        int minChunkX = new ChunkPos(min).x;
        int maxChunkX = new ChunkPos(max).x;
        int minChunkZ = new ChunkPos(min).z;
        int maxChunkZ = new ChunkPos(max).z;
        double searchDistance = (double) MOUNT_SEARCH_RADIUS * MOUNT_SEARCH_RADIUS;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = getChunkAt(walkerLevel, new ChunkPos(chunkX, chunkZ));
                if (chunk == null) {
                    continue;
                }
                int startX = Math.max(min.getX(), chunkX << 4);
                int endX = Math.min(max.getX(), (chunkX << 4) + 15);
                int startZ = Math.max(min.getZ(), chunkZ << 4);
                int endZ = Math.min(max.getZ(), (chunkZ << 4) + 15);
                for (int x = startX; x <= endX; x++) {
                    for (int y = min.getY(); y <= max.getY(); y++) {
                        for (int z = startZ; z <= endZ; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (pos.getCenter().distanceToSqr(corePos.getCenter()) <= searchDistance
                                    && isCannonBlock(chunk.getBlockState(pos))) {
                                found.add(pos);
                            }
                        }
                    }
                }
            }
        }

        found.sort(Comparator.comparingLong(BlockPos::asLong));
        List<BlockPos> result = List.copyOf(found);
        MOUNT_CACHE.put(subLevelId, new CachedMounts(result, gameTime + MOUNT_RESCAN_TICKS));
        return result;
    }

    private static boolean isMountedCannon(SubLevel subLevel, BlockPos pos) {
        LevelChunk chunk = getChunkAt(subLevel, new ChunkPos(pos));
        return chunk != null && isCannonBlock(chunk.getBlockState(pos));
    }

    private static boolean isCannonBlock(BlockState state) {
        return state.is(ModBlocks.AUTOCANNON_MOUNT.get())
                || state.is(ModBlocks.STATIONARY_CANNON.get());
    }

    private static LevelChunk getChunkAt(SubLevel subLevel, ChunkPos worldChunkPos) {
        return subLevel.getPlot().getChunk(subLevel.getPlot().toLocal(worldChunkPos));
    }

    private static PilotWalker findPilotedWalker(ServerPlayer player, BlockPos pilotControls) {
        ServerLevel level = player.serverLevel();
        if (pilotControls != null
                && Sable.HELPER.distanceSquaredWithSubLevels(
                        level,
                        player.position(),
                        pilotControls.getCenter()
                ) <= 12.0 * 12.0) {
            if (level.getBlockEntity(pilotControls) instanceof PilotControlsBlockEntity controls
                    && controls.linkedWalker != null) {
                PilotWalker linkedWalker = walkerFromId(player, controls.linkedWalker);
                if (linkedWalker != null) {
                    return linkedWalker;
                }
            }

            PilotWalker controlSubLevel = walkerInSubLevel(
                    level,
                    Sable.HELPER.getContaining(level, pilotControls)
            );
            if (controlSubLevel != null) {
                return controlSubLevel;
            }
        }

        UUID remoteWalker = findRemoteWalker(player);
        if (remoteWalker != null) {
            PilotWalker linkedWalker = walkerFromId(player, remoteWalker);
            if (linkedWalker != null) {
                return linkedWalker;
            }
        }

        return walkerInSubLevel(level, Sable.HELPER.getContaining(player));
    }

    private static PilotWalker walkerFromId(ServerPlayer player, UUID walkerId) {
        WalkerTracker.Located located = WalkerTracker.find(walkerId);
        if (located == null) {
            return null;
        }
        ServerLevel rootLevel = player.server.getLevel(located.level());
        if (rootLevel == null) {
            return null;
        }
        SubLevel subLevel = Sable.HELPER.getContaining(rootLevel, located.pos());
        return walkerInSubLevel(rootLevel, subLevel);
    }

    private static PilotWalker walkerInSubLevel(ServerLevel rootLevel, SubLevel subLevel) {
        if (subLevel == null) {
            return null;
        }
        BlockPos corePos = WalkerTracker.coreOf(subLevel);
        if (corePos == null || rootLevel.getBlockEntity(corePos) == null) {
            return null;
        }
        return new PilotWalker(subLevel, corePos);
    }

    private static UUID findRemoteWalker(ServerPlayer player) {
        for (ItemStack stack : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (stack.getItem() instanceof RemoteControllerItem) {
                PilotStation station = RemoteControllerItem.station(
                        stack,
                        new int[RemoteControllerItem.SLOTS]
                );
                if (station != null && station.linkedWalker() != null) {
                    return station.linkedWalker();
                }
            }
        }
        return null;
    }

    private static boolean isHoldingAutocannon(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.AUTOCANNON)
                || player.getOffhandItem().is(ModItems.AUTOCANNON);
    }

    private static boolean consumeRound(
            ServerPlayer player,
            PilotWalker pilotWalker,
            BlockPos localMagazineCenter,
            ServerLevel rootLevel,
            Item ammunition
    ) {
        if (pilotWalker != null && consumeMagazineRound(pilotWalker, ammunition)) {
            return true;
        }
        if (localMagazineCenter != null && rootLevel != null
                && consumeNearbyMagazineRound(rootLevel, localMagazineCenter, ammunition)) {
            return true;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ammunition)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static boolean consumeMagazineRound(PilotWalker pilotWalker, Item ammunition) {
        SubLevel subLevel = pilotWalker.subLevel();
        BlockPos corePos = pilotWalker.corePos();
        BlockPos min = corePos.offset(-MOUNT_SEARCH_RADIUS, -MOUNT_SEARCH_RADIUS, -MOUNT_SEARCH_RADIUS);
        BlockPos max = corePos.offset(MOUNT_SEARCH_RADIUS, MOUNT_SEARCH_RADIUS, MOUNT_SEARCH_RADIUS);
        int minChunkX = new ChunkPos(min).x;
        int maxChunkX = new ChunkPos(max).x;
        int minChunkZ = new ChunkPos(min).z;
        int maxChunkZ = new ChunkPos(max).z;
        double searchDistance = (double) MOUNT_SEARCH_RADIUS * MOUNT_SEARCH_RADIUS;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = getChunkAt(subLevel, new ChunkPos(chunkX, chunkZ));
                if (chunk == null) {
                    continue;
                }
                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    if (pos.getCenter().distanceToSqr(corePos.getCenter()) <= searchDistance
                            && chunk.getBlockEntity(pos) instanceof AmmoMagazineBlockEntity magazine
                            && magazine.consumeRound(ammunition)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean consumeNearbyMagazineRound(ServerLevel level, BlockPos cannonPos, Item ammunition) {
        BlockPos min = cannonPos.offset(-2, -2, -2);
        BlockPos max = cannonPos.offset(2, 2, 2);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockEntity(pos) instanceof AmmoMagazineBlockEntity magazine
                    && magazine.consumeRound(ammunition)) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 autocannonMuzzlePosition(BlockPos pos, AutocannonMountBlockEntity cannon) {
        double yaw = cannon.getAimYaw();
        double pitch = cannon.getAimPitch();
        double sinPitch = Math.sin(pitch);
        double cosPitch = Math.cos(pitch);
        double forward = 4.0 + sinPitch + 31.0 * cosPitch;
        double height = 22.0 - cosPitch + 31.0 * sinPitch;
        return new Vec3(
                pos.getX() + 0.5 - Math.sin(yaw) * forward / 16.0,
                pos.getY() + height / 16.0,
                pos.getZ() + 0.5 - Math.cos(yaw) * forward / 16.0
        );
    }

    private static void fireShot(ServerPlayer shooter, ServerLevel level, Vec3 start, Vec3 direction) {
        Vec3 end = start.add(direction.scale(RANGE_BLOCKS));
        HitResult blockHit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                shooter
        ));
        Vec3 impact = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        Candidate candidate = findTarget(level, shooter, start, impact);
        if (candidate != null && candidate.entity() instanceof LivingEntity livingTarget) {
            livingTarget.hurt(level.damageSources().playerAttack(shooter), DAMAGE);
            impact = candidate.hit().orElse(impact);
        }

        spawnTracer(level, start, impact);
        level.sendParticles(ModParticles.MUZZLE_FLASH.get(), start.x, start.y, start.z, 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.CANNON_SPARK.get(), start.x, start.y, start.z, 7, 0.07, 0.07, 0.07, 0.12);
        PacketDistributor.sendToPlayer(shooter, new RecoilPayload(0.18F));
        level.playSound(
                null,
                start.x,
                start.y,
                start.z,
                CANNON_SOUND,
                SoundSource.PLAYERS,
                2.0F,
                0.62F + shooter.getRandom().nextFloat() * 0.08F
        );
    }

    private static void fireHeavyShell(ServerPlayer shooter, ServerLevel level, Vec3 muzzle, Vec3 direction) {
        HeavyShellProjectile.launch(level, shooter, muzzle, direction);
        level.sendParticles(ModParticles.MUZZLE_FLASH.get(), muzzle.x, muzzle.y, muzzle.z, 2, 0.02, 0.02, 0.02, 0.01);
        level.sendParticles(ModParticles.CANNON_SPARK.get(), muzzle.x, muzzle.y, muzzle.z, 20, 0.12, 0.12, 0.12, 0.18);
        level.playSound(null, muzzle.x, muzzle.y, muzzle.z, CANNON_SOUND, SoundSource.PLAYERS,
                3.0F, 0.58F + shooter.getRandom().nextFloat() * 0.04F);
        PacketDistributor.sendToPlayer(shooter, new RecoilPayload(2.2F));
    }

    private static void sendPilotStatus(ServerPlayer player, PilotWalker pilotWalker) {
        UUID playerId = player.getUUID();
        if (player.tickCount < NEXT_STATUS_TICK.getOrDefault(playerId, 0)) {
            return;
        }
        int autocannons = 0;
        int heavyCannons = 0;
        for (BlockPos pos : findMountedCannons(player, pilotWalker)) {
            LevelChunk chunk = getChunkAt(pilotWalker.subLevel(), new ChunkPos(pos));
            if (chunk == null) {
                continue;
            }
            if (chunk.getBlockState(pos).is(ModBlocks.STATIONARY_CANNON.get())) {
                heavyCannons++;
            } else {
                autocannons++;
            }
        }
        int rounds = countAmmunition(pilotWalker, ModItems.AUTOCANNON_ROUND.get());
        int shells = countAmmunition(pilotWalker, ModItems.HEAVY_SHELL.get());
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.AUTOCANNON_ROUND)) {
                rounds += stack.getCount();
            } else if (stack.is(ModItems.HEAVY_SHELL)) {
                shells += stack.getCount();
            }
        }
        PacketDistributor.sendToPlayer(player, new PilotStatusPayload(autocannons, heavyCannons, rounds, shells));
        NEXT_STATUS_TICK.put(playerId, player.tickCount + 10);
    }

    private static int countAmmunition(PilotWalker pilotWalker, Item ammunition) {
        SubLevel subLevel = pilotWalker.subLevel();
        BlockPos corePos = pilotWalker.corePos();
        BlockPos min = corePos.offset(-MOUNT_SEARCH_RADIUS, -MOUNT_SEARCH_RADIUS, -MOUNT_SEARCH_RADIUS);
        BlockPos max = corePos.offset(MOUNT_SEARCH_RADIUS, MOUNT_SEARCH_RADIUS, MOUNT_SEARCH_RADIUS);
        int count = 0;
        for (int chunkX = new ChunkPos(min).x; chunkX <= new ChunkPos(max).x; chunkX++) {
            for (int chunkZ = new ChunkPos(min).z; chunkZ <= new ChunkPos(max).z; chunkZ++) {
                LevelChunk chunk = getChunkAt(subLevel, new ChunkPos(chunkX, chunkZ));
                if (chunk == null) {
                    continue;
                }
                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    if (pos.getCenter().distanceToSqr(corePos.getCenter()) > (double) MOUNT_SEARCH_RADIUS
                            * MOUNT_SEARCH_RADIUS
                            || !(chunk.getBlockEntity(pos) instanceof AmmoMagazineBlockEntity magazine)) {
                        continue;
                    }
                    for (int slot = 0; slot < magazine.getContainerSize(); slot++) {
                        ItemStack stack = magazine.getItem(slot);
                        if (stack.is(ammunition)) {
                            count += stack.getCount();
                        }
                    }
                }
            }
        }
        return count;
    }

    private static Candidate findTarget(ServerLevel level, ServerPlayer shooter, Vec3 start, Vec3 end) {
        AABB searchBounds = new AABB(start, end).inflate(1.0);
        return level.getEntities(
                        shooter,
                        searchBounds,
                        entity -> entity instanceof LivingEntity && entity.isPickable() && !entity.isSpectator()
                )
                .stream()
                .map(entity -> new Candidate(entity, entity.getBoundingBox().inflate(0.3).clip(start, end)))
                .filter(candidate -> candidate.hit().isPresent())
                .min((left, right) -> Double.compare(
                        start.distanceToSqr(left.hit().orElseThrow()),
                        start.distanceToSqr(right.hit().orElseThrow())
                ))
                .orElse(null);
    }

    private static void spawnTracer(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 path = end.subtract(start);
        int points = Math.max(1, (int) (path.length() / 2.0));
        for (int point = 1; point <= points; point++) {
            Vec3 position = start.add(path.scale((double) point / points));
            level.sendParticles(ModParticles.TRACER.get(), position.x, position.y, position.z, 1, 0, 0, 0, 0);
        }
    }

    private static void warn(ServerPlayer player, String messageKey) {
        UUID playerId = player.getUUID();
        if (player.tickCount < NEXT_WARNING_TICK.getOrDefault(playerId, 0)) {
            return;
        }
        player.displayClientMessage(Component.translatable(messageKey), true);
        NEXT_WARNING_TICK.put(playerId, player.tickCount + WARNING_INTERVAL_TICKS);
    }

    private record Candidate(Entity entity, java.util.Optional<Vec3> hit) {
    }

    private record CachedMounts(List<BlockPos> positions, long rescanAt) {
    }

    private record MountedShot(Vec3 muzzle, Vec3 direction, boolean heavy) {
    }

    private record PilotWalker(SubLevel subLevel, BlockPos corePos) {
    }
}
