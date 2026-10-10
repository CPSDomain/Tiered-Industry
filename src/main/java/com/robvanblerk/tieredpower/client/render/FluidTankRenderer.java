package com.robvanblerk.tieredpower.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import com.robvanblerk.tieredpower.block.entity.FluidTankBlockEntity;

/** Draws the tank's fluid as a box inside the glass, as high as the tank is full. */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity> {
	private static final float MIN = 2.05f / 16f, MAX = 13.95f / 16f, BOTTOM = 1.05f / 16f, TOP = 14.95f / 16f;

	public FluidTankRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(FluidTankBlockEntity tank, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		FluidStack fluid = tank.getFluid();
		if (fluid.isEmpty() || tank.getCapacity() <= 0) return;

		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
		int color = ext.getTintColor(fluid);
		float r = ((color >> 16) & 0xFF) / 255f, g = ((color >> 8) & 0xFF) / 255f, b = (color & 0xFF) / 255f;
		float a = Math.max(0.6f, ((color >>> 24) & 0xFF) / 255f);
		int fluidLight = fluid.getFluid().getFluidType().getLightLevel() > 0 ? 0xF000F0 : light; // lava glows

		float fill = Math.min(1f, (float) fluid.getAmount() / tank.getCapacity());
		float top = BOTTOM + (TOP - BOTTOM) * fill;

		// entityTranslucent draws both sides of each face, so the fluid is visible from any angle.
		VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
		Matrix4f m = pose.last().pose();
		float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
		float vTop = v0 + (v1 - v0) * (1 - fill * (TOP - BOTTOM));

		// top
		quad(vc, m, MIN, top, MIN, MIN, top, MAX, MAX, top, MAX, MAX, top, MIN, u0, v0, u1, v1, r, g, b, a, fluidLight, 0, 1, 0);
		// sides
		quad(vc, m, MIN, BOTTOM, MIN, MIN, top, MIN, MAX, top, MIN, MAX, BOTTOM, MIN, u0, vTop, u1, v1, r, g, b, a, fluidLight, 0, 0, -1);
		quad(vc, m, MAX, BOTTOM, MAX, MAX, top, MAX, MIN, top, MAX, MIN, BOTTOM, MAX, u0, vTop, u1, v1, r, g, b, a, fluidLight, 0, 0, 1);
		quad(vc, m, MIN, BOTTOM, MAX, MIN, top, MAX, MIN, top, MIN, MIN, BOTTOM, MIN, u0, vTop, u1, v1, r, g, b, a, fluidLight, -1, 0, 0);
		quad(vc, m, MAX, BOTTOM, MIN, MAX, top, MIN, MAX, top, MAX, MAX, BOTTOM, MAX, u0, vTop, u1, v1, r, g, b, a, fluidLight, 1, 0, 0);
	}

	private static void quad(VertexConsumer vc, Matrix4f m,
			float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4,
			float u0, float v0, float u1, float v1, float r, float g, float b, float a, int light, float nx, float ny, float nz) {
		vertex(vc, m, x1, y1, z1, u0, v1, r, g, b, a, light, nx, ny, nz);
		vertex(vc, m, x2, y2, z2, u0, v0, r, g, b, a, light, nx, ny, nz);
		vertex(vc, m, x3, y3, z3, u1, v0, r, g, b, a, light, nx, ny, nz);
		vertex(vc, m, x4, y4, z4, u1, v1, r, g, b, a, light, nx, ny, nz);
	}

	private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v,
			float r, float g, float b, float a, int light, float nx, float ny, float nz) {
		vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, ny, nz).endVertex();
	}
}
