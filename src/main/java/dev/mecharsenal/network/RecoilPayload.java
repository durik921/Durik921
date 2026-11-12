package dev.mecharsenal.network;

import dev.mecharsenal.MechArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.function.Consumer;

public record RecoilPayload(float strength) implements CustomPacketPayload {
    public static final Type<RecoilPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MechArsenal.MOD_ID, "recoil")
    );
    public static final StreamCodec<ByteBuf, RecoilPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeFloat(payload.strength()),
            buffer -> new RecoilPayload(buffer.readFloat())
    );
    private static volatile Consumer<RecoilPayload> clientHandler;

    public static void setClientHandler(Consumer<RecoilPayload> handler) {
        clientHandler = handler;
    }

    public static void handle(RecoilPayload payload, IPayloadContext context) {
        Consumer<RecoilPayload> handler = clientHandler;
        if (handler == null) {
            throw new IllegalStateException("Recoil payload handler was not registered on the client");
        }
        context.enqueueWork(() -> handler.accept(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
