package com.robvanblerk.tieredpower.block.entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.menu.SecurityTerminalMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Locks a storage network: only the owner (whoever placed it) and the players they add may use it. Server operators
 * can always get in.
 */
public class SecurityTerminalBlockEntity extends BlockEntity implements MenuProvider {
	private @Nullable UUID owner;
	private String ownerName = "";
	private final Map<UUID, String> members = new LinkedHashMap<>();

	public SecurityTerminalBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SECURITY_TERMINAL.get(), pos, state);
	}

	public void setOwner(Player player) {
		owner = player.getUUID();
		ownerName = player.getGameProfile().getName();
		setChanged();
	}

	public @Nullable UUID getOwner() { return owner; }
	public String getOwnerName() { return ownerName; }
	public Map<UUID, String> getMembers() { return members; }

	public boolean isOwner(Player player) {
		return owner == null || owner.equals(player.getUUID()) || player.hasPermissions(2);
	}

	public boolean isAllowed(Player player) {
		return isOwner(player) || members.containsKey(player.getUUID());
	}

	public void addMember(UUID id, String name) {
		if (id.equals(owner)) return;
		members.put(id, name);
		setChanged();
	}

	public void removeMember(UUID id) {
		members.remove(id);
		setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (owner != null) tag.putUUID("Owner", owner);
		tag.putString("OwnerName", ownerName);
		ListTag list = new ListTag();
		for (Map.Entry<UUID, String> e : members.entrySet()) {
			CompoundTag t = new CompoundTag();
			t.putUUID("Id", e.getKey());
			t.putString("Name", e.getValue());
			list.add(t);
		}
		tag.put("Members", list);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
		ownerName = tag.getString("OwnerName");
		members.clear();
		ListTag list = tag.getList("Members", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) members.put(list.getCompound(i).getUUID("Id"), list.getCompound(i).getString("Name"));
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.security_terminal");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new SecurityTerminalMenu(id, inv, worldPosition);
	}
}
