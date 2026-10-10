package com.robvanblerk.tieredarsenal.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.network.ModNetwork;
import com.robvanblerk.tieredarsenal.network.ToolBeltSwapPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** The Tool Belt key (G by default, changeable in Controls under "Tiered Arsenal"). */
public final class KeyBindings {
    private KeyBindings() {}

    public static final KeyMapping TOOL_BELT = new KeyMapping("key." + TieredArsenal.MOD_ID + ".tool_belt",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G,
            "key.categories." + TieredArsenal.MOD_ID);

    @Mod.EventBusSubscriber(modid = TieredArsenal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        private Register() {}

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(TOOL_BELT);
        }
    }

    @Mod.EventBusSubscriber(modid = TieredArsenal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Ticks {
        private Ticks() {}

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().player == null) return;
            while (TOOL_BELT.consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new ToolBeltSwapPacket());
            }
        }
    }
}
