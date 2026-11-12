package dev.mecharsenal.network;

import dev.mecharsenal.MechArsenal;
import dev.mecharsenal.weapon.AutocannonService;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FireAutocannonPayload(BlockPos pilotControls, boolean firing) implements CustomPacketPayload {
    public static final Type<FireAutocannonPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MechArsenal.MOD_ID, "fire_autocannon")
    );
    public static final StreamCodec<ByteBuf, FireAutocannonPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBoolean(payload.pilotControls() != null);
                if (payload.pilotControls() != null) {
                    BlockPos.STREAM_CODEC.encode(buffer, payload.pilotControls());
                }
                buffer.writeBoolean(payload.firing());
            },
            buffer -> new FireAutocannonPayload(
                    buffer.readBoolean() ? BlockPos.STREAM_CODEC.decode(buffer) : null,
                    buffer.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FireAutocannonPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                AutocannonService.handleInput(player, payload.pilotControls(), payload.firing());
            }
        });
    }
}
