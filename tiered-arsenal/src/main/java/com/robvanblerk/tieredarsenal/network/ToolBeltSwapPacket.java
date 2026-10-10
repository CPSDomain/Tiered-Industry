package com.robvanblerk.tieredarsenal.network;

import com.robvanblerk.tieredarsenal.item.ToolBeltItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sent when the player presses the Tool Belt key. No data: the server does the swap. */
public class ToolBeltSwapPacket {
    public static void encode(ToolBeltSwapPacket msg, FriendlyByteBuf buf) {
    }

    public static ToolBeltSwapPacket decode(FriendlyByteBuf buf) {
        return new ToolBeltSwapPacket();
    }

    public static void handle(ToolBeltSwapPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            ToolBeltItem.swap(player);
        }
        ctx.get().setPacketHandled(true);
    }
}
