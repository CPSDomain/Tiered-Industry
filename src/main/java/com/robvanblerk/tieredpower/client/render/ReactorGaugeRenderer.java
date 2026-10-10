package com.robvanblerk.tieredpower.client.render;

import java.util.List;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import com.robvanblerk.tieredpower.block.entity.ReactorGaugeBlockEntity;
import com.robvanblerk.tieredpower.energy.PowerInfo;

public class ReactorGaugeRenderer extends ScreenTextRenderer<ReactorGaugeBlockEntity> {
	public ReactorGaugeRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected List<Line> lines(ReactorGaugeBlockEntity be) {
		if (be.getStatus() == ReactorGaugeBlockEntity.STATUS_NONE) return List.of(new Line("", 0), new Line("No reactor", 0xFF5555));
		int h = be.getHeatPercent();
		int heatColour = h >= 90 ? 0xFF4444 : h >= 60 ? 0xFFAA33 : 0x55FF55;
		String status = switch (be.getStatus()) {
			case ReactorGaugeBlockEntity.STATUS_SCRAM -> "SCRAM";
			case ReactorGaugeBlockEntity.STATUS_RUNNING -> "Running";
			default -> "Idle";
		};
		boolean fusion = be.getPlasma() > 0;
		String extras = fusion ? "Plasma " + com.robvanblerk.tieredpower.block.entity.PlasmaTiers.NAMES[be.getPlasma() - 1]
				: (be.hasReflector() ? "Refl " : "") + (be.isCryo() ? "N2" : "");
		return List.of(
				new Line((fusion ? "Plasma " : "Heat ") + h + "%", fusion ? 0x55CCFF : heatColour),
				new Line(PowerInfo.shortFe(be.getGenerating()) + " FE/t", 0xFFFF55),
				new Line(status, be.getStatus() == ReactorGaugeBlockEntity.STATUS_SCRAM ? 0xFF4444 : 0x55AAFF),
				new Line(extras.isBlank() ? "-" : extras.trim(), 0xAAAAFF));
	}
}
