package com.robvanblerk.tieredarsenal.network;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private ModNetwork() {}

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TieredArsenal.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.messageBuilder(ToolBeltSwapPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToolBeltSwapPacket::encode)
                .decoder(ToolBeltSwapPacket::decode)
                .consumerMainThread(ToolBeltSwapPacket::handle)
                .add();
    }
}
