package dev.mecharsenal.client;

import dev.letitwalk.client.PilotInput;
import dev.mecharsenal.MechArsenal;
import dev.mecharsenal.network.FireAutocannonPayload;
import dev.mecharsenal.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = MechArsenal.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static int ticksUntilNextUpdate;
    private static BlockPos pilotControls;

    private ClientEvents() {
    }

    static void setPilotControls(BlockPos controls) {
        pilotControls = controls.immutable();
    }

    static void clearPilotControls() {
        pilotControls = null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        boolean piloting = PilotInput.isActive();
        PilotHudOverlay.setPilotActive(piloting);
        if (ClientModEvents.TOGGLE_HUD.consumeClick() && piloting) {
            PilotHudOverlay.toggleHud();
        }
        boolean firing = ClientModEvents.FIRE_AUTOCANNON.isDown();
        boolean holdingAutocannon = net.minecraft.client.Minecraft.getInstance().player != null
                && (net.minecraft.client.Minecraft.getInstance().player.getMainHandItem().is(ModItems.AUTOCANNON)
                || net.minecraft.client.Minecraft.getInstance().player.getOffhandItem().is(ModItems.AUTOCANNON));
        if (!piloting) {
            clearPilotControls();
        }
        if (!piloting && !(holdingAutocannon && firing)) {
            ticksUntilNextUpdate = 0;
            return;
        }

        if (ticksUntilNextUpdate > 0) {
            ticksUntilNextUpdate--;
            return;
        }

        PacketDistributor.sendToServer(new FireAutocannonPayload(
                piloting ? pilotControls : null,
                firing
        ));
        ticksUntilNextUpdate = piloting ? 0 : 2;
    }
}
