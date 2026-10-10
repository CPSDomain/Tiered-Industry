package com.robvanblerk.tieredpower.stargate;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import com.robvanblerk.tieredpower.TieredPower;

/**
 * Draws a formed Stargate the way it looks on TV: a thick round ring with a recessed band of glyphs that spins while
 * dialling, nine chevrons that lock one by one, and a rippling round event horizon when the gate is open.
 */
public class StargateRenderer implements BlockEntityRenderer<StargateRingBlockEntity> {
	private static final int SEGS = 36;
	private static final float R_OUT = 3.4f, R_GLYPH_OUT = 3.05f, R_GLYPH_IN = 2.75f, R_IN = 2.45f;
	private static final float DEPTH = 0.35f, GLYPH_DEPTH = 0.25f;

	private boolean axisX;
	private Matrix4f m;
	private Matrix3f n;
	private int light;

	public StargateRenderer(BlockEntityRendererProvider.Context context) {}

	private static TextureAtlasSprite sprite(String name) {
		return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "block/" + name));
	}

	/** One vertex in the ring's own frame: u across, v up (from the centre), w through the gate. */
	private void vtx(VertexConsumer vc, float u, float v, float w, float tu, float tv, int argb, int lightLevel, float nu, float nv, float nw) {
		float x = axisX ? u : w, z = axisX ? w : u, nx = axisX ? nu : nw, nz = axisX ? nw : nu;
		vc.vertex(m, 0.5f + x, 3.5f + v, 0.5f + z).color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
				.uv(tu, tv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightLevel).normal(n, nx, nv, nz).endVertex();
	}

	private static float cos(double deg) { return (float) Math.cos(Math.toRadians(deg)); }
	private static float sin(double deg) { return (float) Math.sin(Math.toRadians(deg)); }

	/** A flat ring between two radii, facing +w or -w, its texture repeating every 4 segments (one column of 4 px each). */
	private void band(VertexConsumer vc, TextureAtlasSprite s, float r0, float r1, float w, float spin, int argb) {
		float face = Math.signum(w);
		for (int i = 0; i < SEGS; i++) {
			double a0 = spin + i * 360.0 / SEGS, a1 = spin + (i + 1) * 360.0 / SEGS;
			float u0 = s.getU((i % 4) * 4), u1 = s.getU((i % 4) * 4 + 4), v0 = s.getV(0), v1 = s.getV(16);
			vtx(vc, r0 * cos(a0), r0 * sin(a0), w, u0, v1, argb, light, 0, 0, face);
			vtx(vc, r1 * cos(a0), r1 * sin(a0), w, u0, v0, argb, light, 0, 0, face);
			vtx(vc, r1 * cos(a1), r1 * sin(a1), w, u1, v0, argb, light, 0, 0, face);
			vtx(vc, r0 * cos(a1), r0 * sin(a1), w, u1, v1, argb, light, 0, 0, face);
		}
	}

	/** The curved side of a ring at radius r, from w0 to w1 (outward when 'out'). */
	private void rim(VertexConsumer vc, TextureAtlasSprite s, float r, float w0, float w1, boolean out, int argb) {
		for (int i = 0; i < SEGS; i++) {
			double a0 = i * 360.0 / SEGS, a1 = (i + 1) * 360.0 / SEGS, mid = (a0 + a1) / 2;
			float nu = cos(mid) * (out ? 1 : -1), nv = sin(mid) * (out ? 1 : -1);
			float u0 = s.getU((i % 4) * 4), u1 = s.getU((i % 4) * 4 + 4), v0 = s.getV(4), v1 = s.getV(12);
			vtx(vc, r * cos(a0), r * sin(a0), w0, u0, v0, argb, light, nu, nv, 0);
			vtx(vc, r * cos(a0), r * sin(a0), w1, u0, v1, argb, light, nu, nv, 0);
			vtx(vc, r * cos(a1), r * sin(a1), w1, u1, v1, argb, light, nu, nv, 0);
			vtx(vc, r * cos(a1), r * sin(a1), w0, u1, v0, argb, light, nu, nv, 0);
		}
	}

	/** A chevron at angle 'deg' (front and back): a wedge sitting on the outer edge. */
	private void chevron(VertexConsumer vc, TextureAtlasSprite s, double deg, boolean lit) {
		int argb = lit ? 0xFFFFFFFF : 0xFF7A4030;
		int l = lit ? LightTexture.FULL_BRIGHT : light;
		float r0 = 2.95f, r1 = 3.65f;
		for (float w : new float[] {DEPTH + 0.06f, -DEPTH - 0.06f}) {
			float face = Math.signum(w);
			float u0 = s.getU(0), u1 = s.getU(16), v0 = s.getV(0), v1 = s.getV(16);
			vtx(vc, r0 * cos(deg - 3), r0 * sin(deg - 3), w, u0, v1, argb, l, 0, 0, face);
			vtx(vc, r1 * cos(deg - 7), r1 * sin(deg - 7), w, u0, v0, argb, l, 0, 0, face);
			vtx(vc, r1 * cos(deg + 7), r1 * sin(deg + 7), w, u1, v0, argb, l, 0, 0, face);
			vtx(vc, r0 * cos(deg + 3), r0 * sin(deg + 3), w, u1, v1, argb, l, 0, 0, face);
		}
		// the top of the chevron, standing proud of the outer rim
		float u0 = s.getU(0), u1 = s.getU(16), v0 = s.getV(0), v1 = s.getV(4);
		float nu = cos(deg), nv = sin(deg);
		vtx(vc, r1 * cos(deg - 7), r1 * sin(deg - 7), -DEPTH - 0.06f, u0, v0, argb, l, nu, nv, 0);
		vtx(vc, r1 * cos(deg - 7), r1 * sin(deg - 7), DEPTH + 0.06f, u0, v1, argb, l, nu, nv, 0);
		vtx(vc, r1 * cos(deg + 7), r1 * sin(deg + 7), DEPTH + 0.06f, u1, v1, argb, l, nu, nv, 0);
		vtx(vc, r1 * cos(deg + 7), r1 * sin(deg + 7), -DEPTH - 0.06f, u1, v0, argb, l, nu, nv, 0);
	}

	/** The rippling round surface, both sides. */
	private void horizon(VertexConsumer vc, TextureAtlasSprite s, int age) {
		int alpha = age < 10 ? 255 : 215;
		int argb = (alpha << 24) | 0xFFFFFF;
		float bulge = 0f;
		for (int side = -1; side <= 1; side += 2) {
			for (int i = 0; i < SEGS; i++) {
				double a0 = i * 360.0 / SEGS, a1 = (i + 1) * 360.0 / SEGS;
				float x0 = R_IN * cos(a0), y0 = R_IN * sin(a0), x1 = R_IN * cos(a1), y1 = R_IN * sin(a1);
				float tu = s.getU(8), tv = s.getV(8);
				float u0 = s.getU(8 + x0 / R_IN * 8), v0 = s.getV(8 - y0 / R_IN * 8), u1 = s.getU(8 + x1 / R_IN * 8), v1 = s.getV(8 - y1 / R_IN * 8);
				float wc = side * (0.02f + bulge);
				vtx(vc, 0, 0, wc, tu, tv, argb, LightTexture.FULL_BRIGHT, 0, 0, side);
				vtx(vc, x0, y0, side * 0.02f, u0, v0, argb, LightTexture.FULL_BRIGHT, 0, 0, side);
				vtx(vc, x1, y1, side * 0.02f, u1, v1, argb, LightTexture.FULL_BRIGHT, 0, 0, side);
				vtx(vc, x1, y1, side * 0.02f, u1, v1, argb, LightTexture.FULL_BRIGHT, 0, 0, side);
			}
		}
	}

	/**
	 * The "kawoosh": the unstable vortex that bursts out of the gate as it opens, reaches several blocks out, and
	 * collapses back into the event horizon - a rippling, bulb-shaped surface of revolution.
	 */
	private void kawoosh(VertexConsumer vc, TextureAtlasSprite s, float age, int side) {
		float len = StargateRingBlockEntity.kawooshLength(age);
		if (len <= 0.05f) return;
		final int RINGS = 14;
		int argb = 0xF0FFFFFF;
		float[][] u = new float[RINGS + 1][SEGS + 1], v = new float[RINGS + 1][SEGS + 1], w = new float[RINGS + 1][SEGS + 1];
		for (int k = 0; k <= RINGS; k++) {
			float f = k / (float) RINGS;
			// a bulb: full width at the gate, swelling a little, then rounding off to a point
			float prof = (float) Math.sqrt(Math.max(0, 1 - f * f)) * (1 + 0.18f * (float) Math.sin(f * Math.PI));
			for (int i = 0; i <= SEGS; i++) {
				double a = i * 360.0 / SEGS;
				float wob = 1 + 0.09f * (float) Math.sin(Math.toRadians(a) * 5 + age * 0.9 + f * 7) + 0.05f * (float) Math.sin(Math.toRadians(a) * 3 - age * 1.3);
				float r = R_IN * 0.97f * prof * (k == 0 ? 1 : wob);
				u[k][i] = r * cos(a);
				v[k][i] = r * sin(a);
				w[k][i] = side * (0.03f + f * len);
			}
		}
		for (int k = 0; k < RINGS; k++) {
			float t0 = s.getV(k * 16f / RINGS), t1 = s.getV((k + 1) * 16f / RINGS);
			for (int i = 0; i < SEGS; i++) {
				float s0 = s.getU((i % 9) * 16f / 9), s1 = s.getU((i % 9 + 1) * 16f / 9);
				double mid = (i + 0.5) * 360.0 / SEGS;
				float nu = cos(mid), nv = sin(mid);
				vtx(vc, u[k][i], v[k][i], w[k][i], s0, t0, argb, LightTexture.FULL_BRIGHT, nu, nv, 0);
				vtx(vc, u[k + 1][i], v[k + 1][i], w[k + 1][i], s0, t1, argb, LightTexture.FULL_BRIGHT, nu, nv, 0);
				vtx(vc, u[k + 1][i + 1], v[k + 1][i + 1], w[k + 1][i + 1], s1, t1, argb, LightTexture.FULL_BRIGHT, nu, nv, 0);
				vtx(vc, u[k][i + 1], v[k][i + 1], w[k][i + 1], s1, t0, argb, LightTexture.FULL_BRIGHT, nu, nv, 0);
			}
		}
	}

	@Override
	public void render(StargateRingBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight, int overlay) {
		if (be.getLevel() == null) return;
		var state = be.getBlockState();
		if (!state.hasProperty(StargateFrameBlock.AXIS) || !state.getValue(StargateFrameBlock.MASTER) || !state.getValue(StargateFrameBlock.FORMED)) return;
		axisX = state.getValue(StargateFrameBlock.AXIS) == Direction.Axis.X;
		m = pose.last().pose();
		n = pose.last().normal();
		light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above(3));

		TextureAtlasSprite stone = sprite("stargate_ring"), glyphs = sprite("stargate_glyphs"), chev = sprite("stargate_chevron");
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
		int white = 0xFFFFFFFF;
		// outer band
		band(vc, stone, R_GLYPH_OUT, R_OUT, DEPTH, 0, white);
		band(vc, stone, R_GLYPH_OUT, R_OUT, -DEPTH, 0, white);
		rim(vc, stone, R_OUT, -DEPTH, DEPTH, true, white);
		rim(vc, stone, R_GLYPH_OUT, GLYPH_DEPTH, DEPTH, false, 0xFFB0B0B0);
		rim(vc, stone, R_GLYPH_OUT, -DEPTH, -GLYPH_DEPTH, false, 0xFFB0B0B0);
		// inner band
		band(vc, stone, R_IN, R_GLYPH_IN, DEPTH, 0, white);
		band(vc, stone, R_IN, R_GLYPH_IN, -DEPTH, 0, white);
		rim(vc, stone, R_IN, -DEPTH, DEPTH, false, 0xFFC8C8C8);
		rim(vc, stone, R_GLYPH_IN, GLYPH_DEPTH, DEPTH, true, 0xFFB0B0B0);
		rim(vc, stone, R_GLYPH_IN, -DEPTH, -GLYPH_DEPTH, true, 0xFFB0B0B0);
		// the glyph band, recessed, spinning while dialling
		float spin = be.clientSpin + (be.getDialing() > 0 ? partialTick * 6f : 0);
		band(vc, glyphs, R_GLYPH_IN, R_GLYPH_OUT, GLYPH_DEPTH, spin, white);
		band(vc, glyphs, R_GLYPH_IN, R_GLYPH_OUT, -GLYPH_DEPTH, -spin, white);
		// chevrons: top first, then round; they lock in as dialling goes on
		int lit = be.getDialing() > 0 ? (int) Math.ceil((StargateRingBlockEntity.DIAL_TICKS - be.getDialing()) * 9.0 / StargateRingBlockEntity.DIAL_TICKS)
				: be.getOpen() > 0 ? 9 : 0;
		for (int k = 0; k < 9; k++) chevron(vc, chev, 90 - k * 40, k < lit);
		// the event horizon
		if (be.getDialing() == 0 && be.getOpen() > 0) {
			VertexConsumer tv = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
			TextureAtlasSprite eh = sprite("event_horizon");
			horizon(tv, eh, be.clientOpenAge);
			kawoosh(tv, eh, be.clientOpenAge + partialTick, be.getSide());
		}
	}

	@Override
	public boolean shouldRenderOffScreen(StargateRingBlockEntity be) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
