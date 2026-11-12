package dev.mecharsenal.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.letitwalk.content.PilotControlsBlock;
import dev.letitwalk.content.RemoteControllerItem;
import dev.mecharsenal.MechArsenal;
import dev.mecharsenal.client.AutocannonMountRenderer;
import dev.mecharsenal.registry.ModBlockEntities;
import dev.mecharsenal.registry.ModEntities;
import dev.mecharsenal.registry.ModParticles;
import dev.mecharsenal.network.PilotStatusPayload;
import dev.mecharsenal.network.RecoilPayload;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MechArsenal.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    public static final KeyMapping FIRE_AUTOCANNON = new KeyMapping(
            "key.mech_arsenal.fire_autocannon",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.mech_arsenal"
    );
    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
            "key.mech_arsenal.toggle_hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.mech_arsenal"
    );

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(FIRE_AUTOCANNON);
        event.register(TOGGLE_HUD);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.AUTOCANNON_MOUNT.get(),
                AutocannonMountRenderer::new
        );
        event.registerEntityRenderer(ModEntities.HEAVY_SHELL.get(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.MUZZLE_FLASH.get(),
                sprites -> new CannonParticle.Provider(sprites, CannonParticle.Style.MUZZLE));
        event.registerSpriteSet(ModParticles.CANNON_SPARK.get(),
                sprites -> new CannonParticle.Provider(sprites, CannonParticle.Style.SPARK));
        event.registerSpriteSet(ModParticles.IMPACT_BURST.get(),
                sprites -> new CannonParticle.Provider(sprites, CannonParticle.Style.IMPACT));
        event.registerSpriteSet(ModParticles.TRACER.get(),
                sprites -> new CannonParticle.Provider(sprites, CannonParticle.Style.TRACER));
    }

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PilotStatusPayload.setClientHandler(PilotHudOverlay::updateStatus);
        RecoilPayload.setClientHandler(payload -> PilotHudOverlay.applyRecoil(payload.strength()));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Consumer<BlockPos> pilotStart = Objects.requireNonNull(
                    PilotControlsBlock.clientStartPiloting,
                    "Let it Walk pilot controls callback was not initialized"
            );
            PilotControlsBlock.clientStartPiloting = controls -> {
                ClientEvents.setPilotControls(controls);
                pilotStart.accept(controls);
            };

            Consumer<InteractionHand> remoteStart = Objects.requireNonNull(
                    RemoteControllerItem.clientStartPiloting,
                    "Let it Walk remote pilot callback was not initialized"
            );
            RemoteControllerItem.clientStartPiloting = hand -> {
                ClientEvents.clearPilotControls();
                remoteStart.accept(hand);
            };

            BiConsumer<BlockPos, String> panelStart = Objects.requireNonNull(
                    RemoteControllerItem.clientStartPanel,
                    "Let it Walk panel pilot callback was not initialized"
            );
            RemoteControllerItem.clientStartPanel = (controls, module) -> {
                ClientEvents.setPilotControls(controls);
                panelStart.accept(controls, module);
            };
        });
    }
}
