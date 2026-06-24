package com.dioguims.failsafe.mod.tags;

import com.dioguims.failsafe.mod.FailsafeMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {

    // Tag para todos os itens que aceitam o encantamento.
    // Populada via JSON em data/failsafe/tags/item/has_durability.json
    public static final TagKey<Item> HAS_DURABILITY =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath(FailsafeMod.MOD_ID, "has_durability"));

    public static void init() {
        // Chamado no construtor do mod apenas para garantir carregamento da classe.
    }
}
