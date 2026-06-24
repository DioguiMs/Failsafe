package com.dioguims.failsafe.mod.mixin;

import com.dioguims.failsafe.mod.enchantment.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;


import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract int getDamageValue();
    @Shadow public abstract int getMaxDamage();
    @Shadow public abstract boolean isDamageableItem();
    @Shadow public abstract void setDamageValue(int damage);

    @Inject(
        method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private <T extends LivingEntity> void failsafe$hurtAndBreak(
            int amount,
            ServerLevel level,
            T entity,
            Consumer<Item> onBreak,
            CallbackInfo ci) {

        ItemStack self = (ItemStack)(Object)this;

        if (!this.isDamageableItem()) return;

        var lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        boolean hasFailsafe = self.getAllEnchantments(lookup)
                .keySet()
                .stream()
                .anyMatch(h -> h.is(ModEnchantments.FAILSAFE));

        if (!hasFailsafe) return;

        if (this.getDamageValue() + amount >= this.getMaxDamage()) {
            this.setDamageValue(this.getMaxDamage() - 1);
            ci.cancel();
            System.out.println("[FAILSAFE] Quebra prevenida em: " + self.getDescriptionId());
        }
    }
}
