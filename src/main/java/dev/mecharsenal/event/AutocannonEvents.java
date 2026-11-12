package dev.mecharsenal.event;

import dev.mecharsenal.MechArsenal;
import dev.mecharsenal.weapon.AutocannonService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = MechArsenal.MOD_ID)
public final class AutocannonEvents {
    private AutocannonEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AutocannonService.clear(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        AutocannonService.clearAll();
    }
}
