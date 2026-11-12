package dev.mecharsenal.registry;

import dev.mecharsenal.MechArsenal;
import dev.mecharsenal.entity.HeavyShellProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MechArsenal.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<HeavyShellProjectile>> HEAVY_SHELL =
            ENTITY_TYPES.register("heavy_shell", () -> EntityType.Builder
                    .<HeavyShellProjectile>of(HeavyShellProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("heavy_shell"));

    private ModEntities() {
    }
}
