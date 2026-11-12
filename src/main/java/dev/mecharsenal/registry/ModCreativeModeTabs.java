package dev.mecharsenal.registry;

import dev.mecharsenal.MechArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MechArsenal.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MECH_ARSENAL = TABS.register(
            "mech_arsenal",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mech_arsenal"))
                    .icon(() -> ModItems.AUTOCANNON.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.AUTOCANNON);
                        output.accept(ModItems.AUTOCANNON_ROUND);
                        output.accept(ModItems.HEAVY_SHELL);
                        output.accept(ModItems.AUTOCANNON_MOUNT);
                        output.accept(ModItems.AMMO_MAGAZINE);
                        output.accept(ModItems.STATIONARY_CANNON);
                    })
                    .build()
    );

    private ModCreativeModeTabs() {
    }
}
