package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.item.StorageDiskItem;
import com.robvanblerk.tieredpower.menu.DriveBayMenu;

public class DriveBayScreen extends MachineScreen<DriveBayMenu> {
	public DriveBayScreen(DriveBayMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		long used = 0, cap = 0, fUsed = 0, fCap = 0;
		int disks = 0, fluidDisks = 0;
		for (int i = 0; i < 8; i++) {
			ItemStack s = menu.slots.get(i).getItem();
			if (s.getItem() instanceof StorageDiskItem d) {
				used += StorageDiskItem.used(s);
				cap += d.getCapacity();
				disks++;
			} else if (s.getItem() instanceof com.robvanblerk.tieredpower.item.FluidDiskItem f) {
				fUsed += StorageDiskItem.used(s);
				fCap += f.getCapacityMb();
				fluidDisks++;
			}
		}
		if (disks + fluidDisks == 0) text(g, "Put Storage Disks here", x + 53, y + 66);
		if (disks > 0) text(g, com.robvanblerk.tieredpower.energy.PowerInfo.shortFe(used) + " / " + com.robvanblerk.tieredpower.energy.PowerInfo.shortFe(cap) + " items", x + 53, y + 64);
		if (fluidDisks > 0) text(g, com.robvanblerk.tieredpower.energy.PowerInfo.shortFe(fUsed / 1000) + " / " + com.robvanblerk.tieredpower.energy.PowerInfo.shortFe(fCap / 1000) + " buckets",
				x + 53, y + (disks > 0 ? 73 : 64));
	}
}
