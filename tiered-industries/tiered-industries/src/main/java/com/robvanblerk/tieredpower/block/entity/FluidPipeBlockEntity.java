package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.robvanblerk.tieredpower.block.FluidPipeBlock;
import com.robvanblerk.tieredpower.energy.FluidPipeNetwork;
import com.robvanblerk.tieredpower.energy.PipeTier;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** One pipe segment: a small buffer (its tier rate in mB). The FluidPipeNetwork does the moving. */
public class FluidPipeBlockEntity extends BlockEntity {
	private final PipeTier tier;
	private final PipeTier.Kind kind;
	public final FluidTank tank;
	private LazyOptional<IFluidHandler> cap;
	@Nullable
	private FluidPipeNetwork network;

	public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FLUID_PIPE.get(), pos, state);
		FluidPipeBlock block = state.getBlock() instanceof FluidPipeBlock b ? b : null;
		this.tier = block != null ? block.getTier() : PipeTier.BASIC;
		this.kind = block != null ? block.getKind() : PipeTier.Kind.LIQUID;
		this.tank = new FluidTank(tier.getRate(), this::accepts) {
			@Override
			protected void onContentsChanged() {
				setChanged();
			}
		};
		this.cap = LazyOptional.of(() -> tank);
	}

	/** Only the right kind of fluid, and only the fluid the network is already carrying. */
	private boolean accepts(FluidStack stack) {
		if (!kind.accepts(stack)) return false;
		FluidStack carrying = network != null ? network.getCarrying() : FluidStack.EMPTY;
		return carrying.isEmpty() || carrying.isFluidEqual(stack);
	}

	public PipeTier getTier() {
		return tier;
	}

	public PipeTier.Kind getKind() {
		return kind;
	}

	public @Nullable FluidPipeNetwork getNetwork() {
		return network;
	}

	public void setNetwork(@Nullable FluidPipeNetwork network) {
		this.network = network;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity be) {
		if (be.network == null || !be.network.isValid()) FluidPipeNetwork.build(level, be);
		if (be.network != null) be.network.tick(level);
		if (level.getGameTime() % 20 == 0) {
			BlockState updated = FluidPipeBlock.withConnections(state, level, pos);
			if (updated != state) level.setBlock(pos, updated, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
		}
	}

	private void invalidateNeighbours() {
		if (level == null) return;
		if (network != null) network.invalidate();
		for (Direction dir : Direction.values()) {
			BlockPos next = worldPosition.relative(dir);
			if (!level.isLoaded(next)) continue;
			if (level.getBlockEntity(next) instanceof FluidPipeBlockEntity other && other.network != null) other.network.invalidate();
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide()) invalidateNeighbours();
	}

	@Override
	public void setRemoved() {
		if (level != null && !level.isClientSide()) invalidateNeighbours();
		super.setRemoved();
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

	// ---- Fluid Filters on connections ----
	private final java.util.Map<net.minecraft.core.Direction, net.minecraft.world.item.ItemStack> filters = new java.util.EnumMap<>(net.minecraft.core.Direction.class);

	public net.minecraft.world.item.ItemStack getFilter(net.minecraft.core.Direction side) {
		return filters.getOrDefault(side, net.minecraft.world.item.ItemStack.EMPTY);
	}

	public void setFilter(net.minecraft.core.Direction side, net.minecraft.world.item.ItemStack filter) {
		if (filter.isEmpty()) filters.remove(side); else filters.put(side, filter);
		setChanged();
	}

	public void dropFilters(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
		for (var f : filters.values()) net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, f);
		filters.clear();
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (!tank.isEmpty()) tag.put("fluid", tank.getFluid().writeToNBT(new CompoundTag()));
		CompoundTag f = new CompoundTag();
		filters.forEach((dir, stack) -> f.put(dir.getName(), stack.save(new CompoundTag())));
		if (!f.isEmpty()) tag.put("filters", f);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		tank.setFluid(tag.contains("fluid") ? FluidStack.loadFluidStackFromNBT(tag.getCompound("fluid")) : FluidStack.EMPTY);
		filters.clear();
		CompoundTag f = tag.getCompound("filters");
		for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values())
			if (f.contains(d.getName())) filters.put(d, net.minecraft.world.item.ItemStack.of(f.getCompound(d.getName())));
	}
}
