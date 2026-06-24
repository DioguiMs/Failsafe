package com.dioguims.failsafe.mod;

import com.dioguims.failsafe.mod.tags.ModTags;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(FailsafeMod.MOD_ID)
public class FailsafeMod {

    public static final String MOD_ID = "failsafe";

    public FailsafeMod(IEventBus modEventBus) {
        ModTags.init();
    }
}
