package dev.mecharsenal.network;

import dev.mecharsenal.MechArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.function.Consumer;

public record PilotStatusPayload(int autocannons, int heavyCannons, int autocannonRounds, int heavyShells)
        implements CustomPacketPayload {
    public static final Type<PilotStatusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MechArsenal.MOD_ID, "pilot_status")
    );
    public static final StreamCodec<ByteBuf, PilotStatusPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeInt(payload.autocannons());
                buffer.writeInt(payload.heavyCannons());
                buffer.writeInt(payload.autocannonRounds());
                buffer.writeInt(payload.heavyShells());
            },
            buffer -> new PilotStatusPayload(
                    buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()
            )
    );
    private static volatile Consumer<PilotStatusPayload> clientHandler;

    public static void setClientHandler(Consumer<PilotStatusPayload> handler) {
        clientHandler = handler;
    }

    public static void handle(PilotStatusPayload payload, IPayloadContext context) {
        Consumer<PilotStatusPayload> handler = clientHandler;
        if (handler == null) {
            throw new IllegalStateException("Pilot HUD payload handler was not registered on the client");
        }
        context.enqueueWork(() -> handler.accept(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
