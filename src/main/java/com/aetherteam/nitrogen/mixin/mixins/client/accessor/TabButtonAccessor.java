package com.aetherteam.nitrogen.mixin.mixins.client.accessor;

import net.minecraft.client.gui.components.TabButton;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TabButton.class)
public interface TabButtonAccessor {
    @Mutable
    @Accessor("TEXTURE_LOCATION")
    static void nitrogen$setTextureLocation(ResourceLocation location) {
        throw new AssertionError();
    }

    @Mutable
    @Accessor("TEXTURE_LOCATION")
    static ResourceLocation nitrogen$getTextureLocation() {
        throw new AssertionError();
    }
}
