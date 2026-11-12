package dev.mecharsenal;

import dev.mecharsenal.network.FireAutocannonPayload;
import dev.mecharsenal.network.PilotStatusPayload;
import dev.mecharsenal.network.RecoilPayload;
import dev.mecharsenal.registry.ModBlockEntities;
import dev.mecharsenal.registry.ModBlocks;
import dev.mecharsenal.registry.ModCreativeModeTabs;
import dev.mecharsenal.registry.ModEntities;
import dev.mecharsenal.registry.ModItems;
import dev.mecharsenal.registry.ModParticles;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(MechArsenal.MOD_ID)
public final class MechArsenal {
    public static final String MOD_ID = "mech_arsenal";

    public MechArsenal(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeModeTabs.TABS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                FireAutocannonPayload.TYPE,
                FireAutocannonPayload.STREAM_CODEC,
                FireAutocannonPayload::handle
        );
        event.registrar("1").playToClient(
                PilotStatusPayload.TYPE,
                PilotStatusPayload.STREAM_CODEC,
                PilotStatusPayload::handle
        );
        event.registrar("1").playToClient(
                RecoilPayload.TYPE,
                RecoilPayload.STREAM_CODEC,
                RecoilPayload::handle
        );
    }
}
