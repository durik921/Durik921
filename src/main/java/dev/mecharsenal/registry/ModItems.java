package dev.mecharsenal.registry;

import dev.mecharsenal.MechArsenal;
import dev.mecharsenal.item.AutocannonItem;
import dev.mecharsenal.item.AutocannonMountItem;
import dev.mecharsenal.item.StationaryCannonItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MechArsenal.MOD_ID);

    public static final DeferredItem<Item> AUTOCANNON = ITEMS.register(
            "autocannon",
            () -> new AutocannonItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> AUTOCANNON_ROUND = ITEMS.register(
            "autocannon_round",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<Item> HEAVY_SHELL = ITEMS.register(
            "heavy_shell",
            () -> new Item(new Item.Properties())
    );
    public static final DeferredItem<BlockItem> AUTOCANNON_MOUNT = ITEMS.register(
            "autocannon_mount",
            () -> new AutocannonMountItem(
                    ModBlocks.AUTOCANNON_MOUNT.get(),
                    new Item.Properties()
            )
    );
    public static final DeferredItem<BlockItem> AMMO_MAGAZINE = ITEMS.register(
            "ammo_magazine",
            () -> new BlockItem(ModBlocks.AMMO_MAGAZINE.get(), new Item.Properties())
    );
    public static final DeferredItem<BlockItem> STATIONARY_CANNON = ITEMS.register(
            "stationary_cannon",
            () -> new StationaryCannonItem(ModBlocks.STATIONARY_CANNON.get(), new Item.Properties())
    );

    private ModItems() {
    }
}
