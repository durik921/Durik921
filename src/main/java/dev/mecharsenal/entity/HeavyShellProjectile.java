package dev.mecharsenal.entity;

import dev.mecharsenal.registry.ModEntities;
import dev.mecharsenal.registry.ModItems;
import dev.mecharsenal.registry.ModParticles;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class HeavyShellProjectile extends ThrowableItemProjectile {
    private static final float IMPACT_DAMAGE = 32.0F;
    private static final double BLAST_RADIUS = 2.5;

    public HeavyShellProjectile(EntityType<? extends HeavyShellProjectile> type, Level level) {
        super(type, level);
    }

    public static HeavyShellProjectile launch(ServerLevel level, Entity owner, Vec3 start, Vec3 direction) {
        HeavyShellProjectile shell = new HeavyShellProjectile(ModEntities.HEAVY_SHELL.get(), level);
        shell.setOwner(owner);
        shell.setPos(start.x, start.y, start.z);
        shell.shoot(direction.x, direction.y, direction.z, 3.2F, 0.0F);
        level.addFreshEntity(shell);
        return shell;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.HEAVY_SHELL.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.035;
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 100) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 impact = result.getLocation();
            Entity owner = getOwner();
            DamageSource damageSource = owner instanceof LivingEntity livingOwner
                    ? serverLevel.damageSources().mobProjectile(this, livingOwner)
                    : serverLevel.damageSources().generic();
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(impact, impact).inflate(BLAST_RADIUS),
                    target -> target.isAlive() && target != owner
            );
            for (LivingEntity target : targets) {
                double distance = Math.sqrt(target.position().distanceToSqr(impact));
                float damage = IMPACT_DAMAGE * (float) Math.max(0.25, 1.0 - distance / (BLAST_RADIUS + 1.0));
                target.hurt(damageSource, damage);
            }
            serverLevel.sendParticles(ModParticles.IMPACT_BURST.get(), impact.x, impact.y, impact.z, 2, 0, 0, 0, 0);
            serverLevel.sendParticles(ModParticles.CANNON_SPARK.get(),
                    impact.x, impact.y, impact.z, 28, 0.55, 0.45, 0.55, 0.25);
            serverLevel.playSound(null, impact.x, impact.y, impact.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 3.0F, 0.75F);
        }
        super.onHit(result);
        discard();
    }
}
