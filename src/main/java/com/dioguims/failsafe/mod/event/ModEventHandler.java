package com.dioguims.failsafe.mod.event;

import com.dioguims.failsafe.mod.FailsafeMod;
import com.dioguims.failsafe.mod.enchantment.ModEnchantments;
import net.minecraft.tags.FluidTags;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.network.chat.Component;

@EventBusSubscriber(modid = FailsafeMod.MOD_ID)
public class ModEventHandler {

    private static final int DAMAGE_MARGIN = 1;

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (!isAtDurabilityLimit(stack)) return;

        event.getToolTip().add(Component.empty());

        event.getToolTip().add(
            Component.translatable("tooltip.failsafe.disabled")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
        );

        event.getToolTip().add(
            Component.translatable("tooltip.failsafe.repair_needed")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
        );
    }

    private static boolean isAtDurabilityLimit(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isDamageableItem()) return false;
        if (!hasFailsafe(stack)) return false;

        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) return false;

        int damageBorder = maxDamage - DAMAGE_MARGIN;
        
        return stack.getDamageValue() >= damageBorder;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().getCommandSenderWorld().isClientSide()) return;
        if (!(event.getContainer().getSource().getEntity() instanceof LivingEntity attacker)) return;

        InteractionHand hand = attacker.getUsedItemHand();
        if (hand == null) return;

        ItemStack stack = attacker.getItemInHand(hand);
        
        if (shouldStopUsage(stack)) {
            event.getContainer().setNewDamage(0);
            
            attacker.releaseUsingItem();
            
            attacker.setItemInHand(hand, stack);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer().level().isClientSide()) return;

        InteractionHand hand = event.getPlayer().getUsedItemHand();
        if (hand == null) hand = InteractionHand.MAIN_HAND;

        ItemStack stack = event.getPlayer().getItemInHand(hand);

        if (shouldStopUsage(stack)) {
            event.setCanceled(true);
            
            event.getPlayer().releaseUsingItem();
            
            event.getPlayer().setItemInHand(hand, stack);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (shouldStopUsage(event.getItemStack())) {
            event.setCanceled(true);
            event.getEntity().releaseUsingItem();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(UseItemOnBlockEvent event) {
        if (shouldStopUsage(event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        
        if (shouldStopUsage(stack)) {
            event.setCanceled(true);
            player.releaseUsingItem();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (shouldStopUsage(event.getItemStack())) {
            event.setCanceled(true);
            event.getEntity().releaseUsingItem();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (shouldStopUsage(event.getItemStack())) {
            event.setCanceled(true);
            event.getEntity().releaseUsingItem();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        checkAndFix(player.getMainHandItem());
        checkAndFix(player.getOffhandItem());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor()) {
                checkAndFix(player.getItemBySlot(slot));
            }
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            checkAndFix(player.getInventory().getItem(i));
        }
    }

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!shouldStopUsage(stack)) return;

        event.removeAllModifiersFor(Attributes.ARMOR);
        event.removeAllModifiersFor(Attributes.ARMOR_TOUGHNESS);
        event.removeAllModifiersFor(Attributes.KNOCKBACK_RESISTANCE);
        event.removeAllModifiersFor(Attributes.ATTACK_DAMAGE);
        event.removeAllModifiersFor(Attributes.ATTACK_SPEED);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();

        if (!shouldStopUsage(stack)) return;

        float speed = 1.0f;

        if (player.hasEffect(MobEffects.DIG_SPEED)) {
            int amp = player.getEffect(MobEffects.DIG_SPEED).getAmplifier();
            speed *= 1.0f + (amp + 1) * 0.2f;
        }
        if (player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            float[] fatigue = {0.3f, 0.09f, 0.0027f, 8.1E-4f};
            int amp = Math.min(player.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier(), 3);
            speed *= fatigue[amp];
        }

        if (player.isEyeInFluid(FluidTags.WATER)) speed *= 0.2f;
        if (!player.onGround()) speed *= 0.2f;

        event.setNewSpeed(speed);
    }

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();

        if (!shouldStopUsage(stack)) return;

        BlockState state = event.getTargetBlock();
        if (state.requiresCorrectToolForDrops()) {
            event.setCanHarvest(false);
        }
    }

    private static boolean shouldStopUsage(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isDamageableItem()) return false;
        if (!hasFailsafe(stack)) return false;

        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) return false;

        int damageBorder = maxDamage - DAMAGE_MARGIN;
        
        if (stack.getDamageValue() >= damageBorder) {
            stack.setDamageValue(damageBorder);
            return true;
        }

        return false;
    }

    private static void checkAndFix(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return;
        if (!hasFailsafe(stack)) return;

        int maxDamage = stack.getMaxDamage();
        int damageBorder = maxDamage - DAMAGE_MARGIN;

        if (stack.getDamageValue() >= damageBorder) {
            stack.setDamageValue(damageBorder);
        }
    }

    private static boolean hasFailsafe(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getAllEnchantments(null)
                .keySet()
                .stream()
                .anyMatch(holder -> holder.is(ModEnchantments.FAILSAFE));
    }
}