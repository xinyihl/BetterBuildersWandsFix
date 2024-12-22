package com.xinyihl.betterbuilderswandsfix.mixin;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import portablejim.bbw.shims.BasicPlayerShim;

@Mixin(value = BasicPlayerShim.class, remap = false)
public abstract class BasicPlayerShimMixin {

    @Redirect(
            method = {"countItems", "useItem"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;isItemEqual(Lnet/minecraft/item/ItemStack;)Z"
            )
    )
    public boolean injected(ItemStack instance, ItemStack itemStack) {
        return instance.isItemEqual(itemStack) && ItemStack.areItemStackTagsEqual(instance, itemStack);
    }

}
