package com.robvanblerk.tieredpower.block.entity;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.robvanblerk.tieredpower.menu.WorkerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModMenus;

/**
 * Looks after animals on a 9x9 area in front of it: breeds pairs with feed from its inputs (keeping the herd to 24),
 * shears sheep, milks cows into empty buckets and picks up eggs and feathers. 200 FE per job.
 */
public class AnimalRanchBlockEntity extends WorkerBlockEntity {
	public static final int RADIUS = 4, MAX_ANIMALS = 24;

	public AnimalRanchBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.ANIMAL_RANCH.get(), pos, state); }
	@Override protected int costPerAction() { return 200; }
	@Override protected int interval() { return 20; }
	@Override public boolean isValidInput(ItemStack stack) { return true; } // feed of any kind, and empty buckets
	@Override protected MenuType<WorkerMenu> menuType() { return ModMenus.ANIMAL_RANCH.get(); }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.animal_ranch"); }

	@Override
	protected boolean work(ServerLevel level, BlockPos pos, Direction facing) {
		BlockPos c = pos.relative(facing, RADIUS + 1);
		AABB area = new AABB(c).inflate(RADIUS, 2, RADIUS);
		boolean did = false;
		// Eggs and feathers lying about.
		for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, area, ie -> ie.getItem().is(Items.EGG) || ie.getItem().is(Items.FEATHER))) {
			ItemStack left = addOutput(e.getItem());
			if (left.isEmpty()) e.discard(); else e.setItem(left);
			did = true;
		}
		List<Animal> animals = level.getEntitiesOfClass(Animal.class, area, Animal::isAlive);
		var player = fakePlayer(level);
		// Shear one sheep.
		for (Animal a : animals) {
			if (a instanceof Sheep sheep && sheep.readyForShearing()) {
				for (ItemStack d : sheep.onSheared(player, new ItemStack(Items.SHEARS), level, sheep.blockPosition(), 0)) outputOrDrop(level, sheep.blockPosition(), d);
				did = true;
				break;
			}
		}
		// Milk one cow into an empty bucket.
		int bucket = findInput(s -> s.is(Items.BUCKET));
		if (bucket >= 0 && animals.stream().anyMatch(a -> a instanceof Cow && !a.isBaby())) {
			items.get(bucket).shrink(1);
			outputOrDrop(level, c, new ItemStack(Items.MILK_BUCKET));
			did = true;
		}
		// Breed one pair, while the herd is small enough.
		if (animals.size() < MAX_ANIMALS) {
			for (Animal a : animals) {
				if (a.isBaby() || !a.canFallInLove()) continue;
				int food = findInput(a::isFood);
				if (food < 0 || items.get(food).getCount() < 2) continue;
				Animal mate = null;
				for (Animal b : animals) if (b != a && b.getType() == a.getType() && !b.isBaby() && b.canFallInLove()) { mate = b; break; }
				if (mate == null) continue;
				a.setInLove(player);
				mate.setInLove(player);
				items.get(food).shrink(2);
				did = true;
				break;
			}
		}
		if (did) setChanged();
		return did;
	}
}
