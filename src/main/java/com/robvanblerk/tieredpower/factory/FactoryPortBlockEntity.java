package com.robvanblerk.tieredpower.factory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Factory Port: part of a Digital Factory; pipes, cables and hoppers on it reach the controller's slots, water and power. */
public class FactoryPortBlockEntity extends BlockEntity {
	private @Nullable BlockPos controller;

	public FactoryPortBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FACTORY_PORT.get(), pos, state);
	}

	public void link(BlockPos controllerPos) {
		if (!controllerPos.equals(controller)) {
			controller = controllerPos.immutable();
			invalidateCaps();
			reviveCaps();
		}
	}

	private @Nullable DigitalFactoryBlockEntity factory() {
		if (controller == null || level == null || !level.isLoaded(controller)) return null;
		return level.getBlockEntity(controller) instanceof DigitalFactoryBlockEntity f && f.owns(worldPosition) ? f : null;
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		DigitalFactoryBlockEntity f = factory();
		if (f != null) {
			if (cap == ForgeCapabilities.ITEM_HANDLER) return LazyOptional.of(f::automationHandler).cast();
			if (cap == ForgeCapabilities.FLUID_HANDLER || cap == ForgeCapabilities.ENERGY) return f.getCapability(cap, null);
		}
		return super.getCapability(cap, side);
	}
}
