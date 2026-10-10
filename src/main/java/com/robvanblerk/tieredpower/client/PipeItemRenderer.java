package com.robvanblerk.tieredpower.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.robvanblerk.tieredpower.TieredPower;

/** Draws items gliding along Item Pipes. Purely visual - the server has already delivered them. */
@Mod.EventBusSubscriber(modid = TieredPower.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PipeItemRenderer {
	private static final int MAX_ACTIVE = 256;

	private record Travel(ItemStack stack, List<Vec3> points, double blocksPerTick, long startTick) {
		double length() {
			double total = 0;
			for (int i = 1; i < points.size(); i++) total += points.get(i).distanceTo(points.get(i - 1));
			return total;
		}
	}

	private static final List<Travel> ACTIVE = new ArrayList<>();

	public static void add(ItemStack stack, List<BlockPos> path, int blocksPerSecond) {
		var level = Minecraft.getInstance().level;
		if (level == null || ACTIVE.size() >= MAX_ACTIVE) return;
		// Points: from the face of the sending block, through each pipe centre, to the face of the receiving block.
		List<Vec3> points = new ArrayList<>();
		for (int i = 1; i < path.size() - 1; i++) points.add(Vec3.atCenterOf(path.get(i)));
		Vec3 first = Vec3.atCenterOf(path.get(1)), last = Vec3.atCenterOf(path.get(path.size() - 2));
		points.add(0, first.add(Vec3.atCenterOf(path.get(0)).subtract(first).scale(0.5)));
		points.add(last.add(Vec3.atCenterOf(path.get(path.size() - 1)).subtract(last).scale(0.5)));
		ACTIVE.add(new Travel(stack, points, blocksPerSecond / 20.0, level.getGameTime()));
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		var level = Minecraft.getInstance().level;
		if (level == null) {
			ACTIVE.clear();
			return;
		}
		long now = level.getGameTime();
		ACTIVE.removeIf(t -> (now - t.startTick()) * t.blocksPerTick() > t.length() + 0.01);
	}

	@SubscribeEvent
	public static void onRender(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || ACTIVE.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Camera camera = event.getCamera();
		Vec3 cam = camera.getPosition();
		PoseStack pose = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		double time = mc.level.getGameTime() + event.getPartialTick();

		for (Iterator<Travel> it = ACTIVE.iterator(); it.hasNext(); ) {
			Travel t = it.next();
			double distance = (time - t.startTick()) * t.blocksPerTick();
			Vec3 at = pointAlong(t.points(), distance);
			if (at == null) continue;
			pose.pushPose();
			pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
			pose.mulPose(Axis.YP.rotationDegrees((float) (time * 4 % 360)));
			pose.scale(0.25f, 0.25f, 0.25f); // fits inside the 6-pixel tube
			int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(at));
			mc.getItemRenderer().renderStatic(t.stack(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
			pose.popPose();
		}
		buffers.endBatch();
	}

	private static Vec3 pointAlong(List<Vec3> points, double distance) {
		if (distance < 0) return points.get(0);
		for (int i = 1; i < points.size(); i++) {
			Vec3 a = points.get(i - 1), b = points.get(i);
			double seg = a.distanceTo(b);
			if (distance <= seg) return seg == 0 ? b : a.add(b.subtract(a).scale(distance / seg));
			distance -= seg;
		}
		return null;
	}

	private PipeItemRenderer() {}
}
