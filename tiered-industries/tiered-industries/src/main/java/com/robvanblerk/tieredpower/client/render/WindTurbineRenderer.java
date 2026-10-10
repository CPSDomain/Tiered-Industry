package com.robvanblerk.tieredpower.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.block.entity.WindTurbineBlockEntity;

/** Draws a three-blade rotor (about 3 blocks across) on the turbine's front face; it spins while the turbine is making power. */
public class WindTurbineRenderer implements BlockEntityRenderer<WindTurbineBlockEntity> {
	private static final ResourceLocation BLADE = ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "textures/entity/wind_blade.png");
	private static final float LENGTH = 1.45f, ROOT = 0.12f, TIP = 0.07f;

	public WindTurbineRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(WindTurbineBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(MachineBlock.FACING)) return;
		Direction facing = state.getValue(MachineBlock.FACING);
		boolean spinning = state.getValue(MachineBlock.LIT);
		long time = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
		float angle = spinning ? (time + partialTick) * 6f % 360f : 20f;

		pose.pushPose();
		pose.translate(0.5, 0.5, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180));  // model faces north; turn to the block's facing
		pose.translate(0, 0, -0.56);                                    // just in front of the front face
		pose.mulPose(Axis.ZP.rotationDegrees(angle));
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(BLADE));
		for (int i = 0; i < 3; i++) {
			pose.pushPose();
			pose.mulPose(Axis.ZP.rotationDegrees(i * 120f));
			blade(vc, pose, light);
			pose.popPose();
		}
		// Hub
		quad(vc, pose, -0.14f, -0.14f, 0.14f, 0.14f, -0.01f, 0f, 0f, 0.25f, 0.25f, light);
		pose.popPose();
	}

	/** One blade: a tapered strip from the hub outwards, slightly twisted so it catches the light. */
	private void blade(VertexConsumer vc, PoseStack pose, int light) {
		Matrix4f m = pose.last().pose();
		Matrix3f n = pose.last().normal();
		vertex(vc, m, n, -ROOT, 0.1f, 0.00f, 0f, 1f, light);
		vertex(vc, m, n, ROOT, 0.1f, -0.02f, 1f, 1f, light);
		vertex(vc, m, n, TIP, LENGTH, -0.05f, 1f, 0f, light);
		vertex(vc, m, n, -TIP, LENGTH, 0.00f, 0f, 0f, light);
	}

	private void quad(VertexConsumer vc, PoseStack pose, float x0, float y0, float x1, float y1, float z, float u0, float v0, float u1, float v1, int light) {
		Matrix4f m = pose.last().pose();
		Matrix3f n = pose.last().normal();
		vertex(vc, m, n, x0, y0, z, u0, v1, light);
		vertex(vc, m, n, x1, y0, z, u1, v1, light);
		vertex(vc, m, n, x1, y1, z, u1, v0, light);
		vertex(vc, m, n, x0, y1, z, u0, v0, light);
	}

	private void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, int light) {
		vc.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, -1).endVertex();
	}

	@Override
	public boolean shouldRenderOffScreen(WindTurbineBlockEntity be) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
