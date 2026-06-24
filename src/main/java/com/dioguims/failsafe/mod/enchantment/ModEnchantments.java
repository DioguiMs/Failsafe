package com.dioguims.failsafe.mod.enchantment;

import com.dioguims.failsafe.mod.FailsafeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {

    public static final ResourceKey<Enchantment> FAILSAFE = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(FailsafeMod.MOD_ID, "failsafe")
    );
}
