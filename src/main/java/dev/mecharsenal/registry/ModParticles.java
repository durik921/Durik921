package dev.mecharsenal.registry;

import dev.mecharsenal.MechArsenal;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, MechArsenal.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUZZLE_FLASH =
            PARTICLE_TYPES.register("muzzle_flash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CANNON_SPARK =
            PARTICLE_TYPES.register("cannon_spark", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> IMPACT_BURST =
            PARTICLE_TYPES.register("impact_burst", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TRACER =
            PARTICLE_TYPES.register("tracer", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
