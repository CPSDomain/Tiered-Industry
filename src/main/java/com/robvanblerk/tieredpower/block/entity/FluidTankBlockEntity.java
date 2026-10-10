package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.robvanblerk.tieredpower.block.FluidTankBlock;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** A single-fluid tank. Sends its contents to nearby players (throttled) so the fluid level renders inside the glass. */
public class FluidTankBlockEntity extends BlockEntity {
	private final FluidTank tank;
	private LazyOptional<IFluidHandler> cap;
	private boolean needsSync;
	private int lastSignal = -1;

	public FluidTankBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FLUID_TANK.get(), pos, state);
		int capacity = state.getBlock() instanceof FluidTankBlock block ? block.getCapacity() : 16_000;
		this.tank = new FluidTank(capacity) {
			@Override
			protected void onContentsChanged() {
				setChanged();
				needsSync = true;
			}
		};
		this.cap = LazyOptional.of(() -> tank);
	}

	public FluidStack getFluid() {
		return tank.getFluid();
	}

	public int getCapacity() {
		return tank.getCapacity();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FluidTankBlockEntity be) {
		if (be.needsSync && level.getGameTime() % 5 == 0) {
			be.needsSync = false;
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
			int amount = be.tank.getFluidAmount();
			int signal = amount <= 0 ? 0 : 1 + (int) ((long) amount * 14 / be.tank.getCapacity());
			if (signal != be.lastSignal) {
				be.lastSignal = signal;
				level.updateNeighbourForOutputSignal(pos, state.getBlock());
			}
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
		if (capability == ForgeCapabilities.FLUID_HANDLER) return cap.cast();
		return super.getCapability(capability, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		cap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		cap = LazyOptional.of(() -> tank);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (!tank.isEmpty()) tag.put("fluid", tank.getFluid().writeToNBT(new CompoundTag()));
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		tank.setFluid(tag.contains("fluid") ? FluidStack.loadFluidStackFromNBT(tag.getCompound("fluid")) : FluidStack.EMPTY);
	}

	// Client sync so the renderer can draw the fluid.
	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = new CompoundTag();
		saveAdditional(tag);
		return tag;
	}

	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
