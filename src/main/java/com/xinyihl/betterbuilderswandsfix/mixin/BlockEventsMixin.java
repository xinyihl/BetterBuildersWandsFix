package com.xinyihl.betterbuilderswandsfix.mixin;

import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import portablejim.bbw.core.BlockEvents;

@Mixin(value = BlockEvents.class, remap = false)
public abstract class BlockEventsMixin {

    @Shadow
    public abstract void blockHighlightEvent(DrawBlockHighlightEvent event);

    @Unique
    @SubscribeEvent(receiveCanceled = true)
    public void betterBuildersWandsFix$blockHighlightEvent(DrawBlockHighlightEvent event) {
        if (event.isCanceled()) {
            blockHighlightEvent(event);
        }
    }
}
