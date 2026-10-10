package com.robvanblerk.tieredpower.client.render;

import java.util.List;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import com.robvanblerk.tieredpower.block.entity.EnergyMeterBlockEntity;
import com.robvanblerk.tieredpower.energy.PowerInfo;

public class EnergyMeterRenderer extends ScreenTextRenderer<EnergyMeterBlockEntity> {
	public EnergyMeterRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected List<Line> lines(EnergyMeterBlockEntity be) {
		if (be.isBlocked()) return List.of(new Line("", 0), new Line("OFF", 0xFF5555), new Line("redstone", 0xAA4444));
		return List.of(
				new Line(PowerInfo.shortFe(be.getFlow()), 0x55FF55),
				new Line("FE/t", 0x33AA33),
				new Line("Total", 0x55AAFF),
				new Line(PowerInfo.shortFe(be.getTotal()), 0x55FFFF));
	}
}
