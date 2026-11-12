package dev.mecharsenal.registry;

import dev.mecharsenal.MechArsenal;
import dev.mecharsenal.block.entity.AutocannonMountBlockEntity;
import dev.mecharsenal.block.entity.AmmoMagazineBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MechArsenal.MOD_ID);

    public static final Supplier<BlockEntityType<AmmoMagazineBlockEntity>> AMMO_MAGAZINE =
            BLOCK_ENTITY_TYPES.register("ammo_magazine", () -> BlockEntityType.Builder.of(
                    AmmoMagazineBlockEntity::new,
                    ModBlocks.AMMO_MAGAZINE.get()
            ).build(null));
    public static final Supplier<BlockEntityType<AutocannonMountBlockEntity>> AUTOCANNON_MOUNT =
            BLOCK_ENTITY_TYPES.register("autocannon_mount", () -> BlockEntityType.Builder.of(
                    AutocannonMountBlockEntity::new,
                    ModBlocks.AUTOCANNON_MOUNT.get()
            ).build(null));

    private ModBlockEntities() {
    }
}
