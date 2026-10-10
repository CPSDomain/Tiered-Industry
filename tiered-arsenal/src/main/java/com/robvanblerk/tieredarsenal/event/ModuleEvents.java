package com.robvanblerk.tieredarsenal.event;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.config.ArsenalConfig;
import com.robvanblerk.tieredarsenal.item.SpearItem;
import com.robvanblerk.tieredarsenal.registry.ModEnchantments;
import com.robvanblerk.tieredarsenal.module.GearModule;
import com.robvanblerk.tieredarsenal.module.ModuleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** What each fitted module does. Auto-Smelt lives in loot/AutoSmeltModifier.java. */
@Mod.EventBusSubscriber(modid = TieredArsenal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModuleEvents {
    private ModuleEvents() {}

    public static final int FIRE_SECONDS = 4;
    public static final float THORNS_PER_PIECE = 0.10F;
    public static final int VEIN_MINER_MAX = 32;
    private static final UUID REACH_ENCHANT_ID = UUID.fromString("9b0b7a52-63a1-4f5e-a7d1-3f6c2e9d8b44");

    /** Attribute modules: Vitality, Bulwark, Step Assist, Swiftness (armour) and Knockback (weapons). */
    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();

        // Reach enchantment on spears: +0.5 blocks per level
        if (event.getSlotType() == EquipmentSlot.MAINHAND && stack.getItem() instanceof SpearItem) {
            int reach = ModEnchantments.level(ModEnchantments.REACH, stack);
            if (reach > 0) {
                event.addModifier(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(REACH_ENCHANT_ID,
                        "Reach enchantment", 0.5 * reach, AttributeModifier.Operation.ADDITION));
            }
        }

        List<GearModule> modules = ModuleHelper.getModules(stack);
        if (modules.isEmpty()) return;

        EquipmentSlot home = stack.getItem() instanceof ArmorItem armour
                ? armour.getEquipmentSlot() : EquipmentSlot.MAINHAND;
        if (event.getSlotType() != home) return;

        for (GearModule m : modules) {
            switch (m) {
                case VITALITY -> add(event, m, home, Attributes.MAX_HEALTH, 4.0);
                case BULWARK -> add(event, m, home, Attributes.KNOCKBACK_RESISTANCE, 0.1);
                case STEP_ASSIST -> add(event, m, home, ForgeMod.STEP_HEIGHT_ADDITION.get(), 0.6);
                case SWIFTNESS -> add(event, m, home, Attributes.MOVEMENT_SPEED, 0.01); // base is 0.1, so +10%
                case KNOCKBACK -> add(event, m, home, Attributes.ATTACK_KNOCKBACK, 1.0);
                case CELESTIAL -> {
                    if (stack.getItem() instanceof ArmorItem) {
                        add(event, m, home, Attributes.ARMOR, 1.0);
                        add(event, m, home, Attributes.ARMOR_TOUGHNESS, 1.0);
                    } else {
                        add(event, m, home, Attributes.ATTACK_DAMAGE, 2.0);
                    }
                }
                default -> { }
            }
        }
    }

    private static void add(ItemAttributeModifierEvent event, GearModule module, EquipmentSlot slot,
                            Attribute attribute, double amount) {
        UUID id = UUID.nameUUIDFromBytes((TieredArsenal.MOD_ID + ":" + module.id() + ":" + slot.getName())
                .getBytes(StandardCharsets.UTF_8));
        event.addModifier(attribute, new AttributeModifier(id, "Tiered Arsenal " + module.id(), amount,
                AttributeModifier.Operation.ADDITION));
    }

    /** Thorns on the victim's armour; Lifesteal and Fire on the attacker's weapon. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();

        // Thorns: melee attackers take 10% of the damage back for each armour piece with the module
        if (!event.getSource().is(DamageTypes.THORNS)
                && event.getSource().getDirectEntity() instanceof LivingEntity hitter && hitter != victim) {
            int pieces = 0;
            for (ItemStack armour : victim.getArmorSlots()) {
                if (ModuleHelper.has(armour, GearModule.THORNS)) pieces++;
            }
            if (pieces > 0) {
                hitter.hurt(victim.damageSources().thorns(victim), event.getAmount() * THORNS_PER_PIECE * pieces);
            }
        }

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        List<GearModule> modules = ModuleHelper.getModules(attacker.getMainHandItem());
        if (modules.isEmpty()) return;

        if (modules.contains(GearModule.LIFESTEAL)) {
            attacker.heal(event.getAmount() * ArsenalConfig.LIFESTEAL.get().floatValue());
        }
        if (modules.contains(GearModule.FIRE) && !victim.fireImmune()) {
            victim.setSecondsOnFire(FIRE_SECONDS);
        }
    }

    @SubscribeEvent
    public static void onLooting(LootingLevelEvent event) {
        if (event.getDamageSource() != null
                && event.getDamageSource().getEntity() instanceof LivingEntity killer
                && ModuleHelper.has(killer.getMainHandItem(), GearModule.LOOTING)) {
            event.setLootingLevel(event.getLootingLevel() + 1);
        }
    }

    /** Aqua Lung (helmet, under water) and Regeneration (chestplate, while hurt). */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide() || player.tickCount % 20 != 0) return;
        if (player.isEyeInFluid(FluidTags.WATER)
                && ModuleHelper.has(player.getItemBySlot(EquipmentSlot.HEAD), GearModule.AQUA_LUNG)) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false, true));
        }
        if (player.getHealth() < player.getMaxHealth()
                && ModuleHelper.has(player.getItemBySlot(EquipmentSlot.CHEST), GearModule.REGENERATION)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
        }
    }

    // ---- Vein Miner ----

    private static boolean veinMining;

    /** Sneak while breaking an ore or log with a Vein Miner tool to break the connected blocks too. */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (veinMining || !(event.getPlayer() instanceof ServerPlayer player) || !player.isShiftKeyDown()) return;
        ItemStack tool = player.getMainHandItem();
        if (!ModuleHelper.has(tool, GearModule.VEIN_MINER)) return;
        BlockState state = event.getState();
        if (!state.is(Tags.Blocks.ORES) && !state.is(BlockTags.LOGS)) return;
        if (!(event.getLevel() instanceof Level level)) return;

        List<BlockPos> vein = findVein(level, event.getPos(), state.getBlock());
        veinMining = true;
        try {
            for (BlockPos pos : vein) {
                if (player.getMainHandItem() != tool || tool.isEmpty()) break; // tool broke or was swapped
                player.gameMode.destroyBlock(pos);
            }
        } finally {
            veinMining = false;
        }
    }

    /** Connected blocks of the same type (touching on any side or corner), not including the start. */
    private static List<BlockPos> findVein(Level level, BlockPos start, Block block) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && found.size() < VEIN_MINER_MAX) {
            BlockPos pos = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (found.size() >= VEIN_MINER_MAX) break;
                BlockPos n = next.immutable();
                if (seen.add(n) && level.getBlockState(n).is(block)) {
                    found.add(n);
                    queue.add(n);
                }
            }
        }
        return found;
    }
}
