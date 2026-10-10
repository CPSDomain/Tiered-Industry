package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import com.robvanblerk.tieredpower.block.entity.ElectricPumpBlockEntity;
import com.robvanblerk.tieredpower.menu.ElectricPumpMenu;

public class ElectricPumpScreen extends MachineScreen<ElectricPumpMenu> {
	private static final int TANK_X = 136, TANK_Y = 18;

	public ElectricPumpScreen(ElectricPumpMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected String tierEffect(int tier) {
		int t = Math.min(tier, ElectricPumpBlockEntity.TIER_RANGE.length - 1);
		return com.robvanblerk.tieredpower.block.entity.MachineBlockEntity.TIER_LANES[t] + " buckets/cycle";
	}

	static int fluidColour(Fluid fluid) {
		if (fluid == Fluids.EMPTY) return 0xFF3F76E4;
		if (fluid.isSame(Fluids.WATER)) return 0xFF3F76E4;
		if (fluid.isSame(Fluids.LAVA)) return 0xFFE5641A;
		int tint = IClientFluidTypeExtensions.of(fluid).getTintColor(new FluidStack(fluid, 1000));
		return 0xFF000000 | (tint & 0xFFFFFF);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		Fluid fluid = menu.getFluid();
		drawTank(g, x + TANK_X, y + TANK_Y, (float) menu.getFluidAmount() / menu.getTankCapacity(), fluidColour(fluid));
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
		String status = menu.noFluidBelow() ? "No fluid below" : menu.getFluidAmount() >= menu.getTankCapacity() - 999 ? "Tank full" : "Pumping";
		text(g, status, x + 32, y + 22);
		text(g, String.format("%,d mB", menu.getFluidAmount()), x + 32, y + 36);
		text(g, statusText(), x + 32, y + 60);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(TANK_X, TANK_Y, 14, 52, mouseX, mouseY)) {
			Fluid fluid = menu.getFluid();
			Component name = fluid == Fluids.EMPTY ? Component.literal("Empty") : new FluidStack(fluid, 1).getDisplayName();
			g.renderTooltip(font, Component.literal(String.format("%,d / %,d mB ", menu.getFluidAmount(), menu.getTankCapacity())).append(name), mouseX, mouseY);
		}
	}
}
