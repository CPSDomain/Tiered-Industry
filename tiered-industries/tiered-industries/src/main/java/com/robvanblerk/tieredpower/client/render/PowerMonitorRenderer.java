package com.robvanblerk.tieredpower.client.render;

import java.util.List;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import com.robvanblerk.tieredpower.block.entity.PowerMonitorBlockEntity;
import com.robvanblerk.tieredpower.energy.CableNetwork;
import com.robvanblerk.tieredpower.energy.PowerInfo;

public class PowerMonitorRenderer extends ScreenTextRenderer<PowerMonitorBlockEntity> {
	public PowerMonitorRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected List<Line> lines(PowerMonitorBlockEntity be) {
		if (!be.isConnected()) return List.of(new Line("", 0), new Line("No cable", 0xFF5555));
		CableNetwork.Stats s = be.getStats();
		String stored = s.capacity() <= 0 ? "No battery" : "Bat " + Math.round(100.0 * s.stored() / s.capacity()) + "%";
		return List.of(
				new Line("In " + PowerInfo.shortFe(s.in()), 0x55FF55),
				new Line("Use " + PowerInfo.shortFe(s.toMachines()), 0xFFFF55),
				new Line("Chg " + PowerInfo.shortFe(s.toStorage()), 0x55FFFF),
				new Line(stored, 0x55AAFF));
	}
}
