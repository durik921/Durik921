package dev.mecharsenal.registry;

import dev.mecharsenal.MechArsenal;
import dev.mecharsenal.block.AmmoMagazineBlock;
import dev.mecharsenal.block.AutocannonMountBlock;
import dev.mecharsenal.block.StationaryCannonBlock;
import dev.mecharsenal.block.StationaryCannonPartBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MechArsenal.MOD_ID);

    public static final DeferredBlock<Block> AUTOCANNON_MOUNT = BLOCKS.registerBlock(
            "autocannon_mount",
            AutocannonMountBlock::new,
            BlockBehaviour.Properties.of().strength(3.0F).noOcclusion()
    );
    public static final DeferredBlock<Block> AMMO_MAGAZINE = BLOCKS.registerBlock(
            "ammo_magazine",
            AmmoMagazineBlock::new,
            BlockBehaviour.Properties.of().strength(4.0F, 6.0F).noOcclusion()
    );
    public static final DeferredBlock<Block> STATIONARY_CANNON = BLOCKS.registerBlock(
            "stationary_cannon",
            StationaryCannonBlock::new,
            BlockBehaviour.Properties.of().strength(4.0F, 6.0F).noOcclusion()
    );
    public static final DeferredBlock<Block> STATIONARY_CANNON_PART = BLOCKS.registerBlock(
            "stationary_cannon_part",
            StationaryCannonPartBlock::new,
            BlockBehaviour.Properties.of().strength(4.0F, 6.0F).noLootTable().noOcclusion()
    );

    private ModBlocks() {
    }
}
