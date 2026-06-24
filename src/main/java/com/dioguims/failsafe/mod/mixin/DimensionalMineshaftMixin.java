package com.dioguims.failsafe.mod.mixin;

import com.dioguims.failsafe.mod.enchantment.ModEnchantments;
import com.klikli_dev.occultism.common.blockentity.DimensionalMineshaftBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DimensionalMineshaftBlockEntity.class, remap = false)
public abstract class DimensionalMineshaftMixin {

    @Shadow
    public DimensionalMineshaftBlockEntity.MineshaftInventory inputHandler;

    @Shadow
    public int miningTime;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void failsafe$stopMiningWhenExhausted(CallbackInfo ci) {
        DimensionalMineshaftBlockEntity self = (DimensionalMineshaftBlockEntity) (Object) this;

        if (self.getLevel() == null || self.getLevel().isClientSide()) return;

        ItemStack input = this.inputHandler.getStackInSlot(0);
        
        if (input.isEmpty() || !input.isDamageableItem()) return;

        var failsafe = self.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.FAILSAFE)
                .orElse(null);

        if (failsafe == null) return;

        int remaining = input.getMaxDamage() - input.getDamageValue();

        if (remaining < 2 && input.getEnchantmentLevel(failsafe) > 0) {
            this.miningTime = 0;
            ci.cancel();
        }
    }
}