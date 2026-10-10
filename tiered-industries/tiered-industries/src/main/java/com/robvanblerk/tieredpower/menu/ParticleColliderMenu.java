package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.block.entity.ParticleColliderBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class ParticleColliderMenu extends MachineMenu {
	// 0-1 energy, 2 deuterium, 3 tritium, 4 antimatter, 5 FE per op
	public static final int DATA_COUNT = 6;
	private final Container container;

	public ParticleColliderMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT));
	}

	public ParticleColliderMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.PARTICLE_COLLIDER.get(), id, data, 2);
		this.container = container;
		addUpgradeSlots(container, 0, 5);
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return ParticleColliderBlockEntity.CAPACITY; }
	public int getDeuterium() { return data.get(2); }
	public int getTritium() { return data.get(3); }
	public int getAntimatter() { return data.get(4); }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
