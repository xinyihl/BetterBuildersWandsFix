package com.xinyihl.betterbuilderswandsfix.mixin;

import com.xinyihl.betterbuilderswandsfix.common.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import portablejim.bbw.basics.Point3d;
import portablejim.bbw.core.BlockEvents;
import portablejim.bbw.core.items.IWandItem;
import portablejim.bbw.shims.BasicPlayerShim;

import java.util.List;

@Mixin(value = BlockEvents.class, remap = false)
public abstract class BlockEventsMixin {

    @Inject(
            method = "blockHighlightEvent",
            at = @At("HEAD"),
            cancellable = true
    )
    public void blockHighlightEvent(DrawBlockHighlightEvent event, CallbackInfo ci) {
        ci.cancel();
    }

    @Unique
    @SubscribeEvent(receiveCanceled = true)
    public void betterBuildersWandsFix$blockHighlightEvent(DrawBlockHighlightEvent event) {
        ItemStack wandStack = BasicPlayerShim.getHeldWandIfAny(event.getPlayer());
        if (wandStack == null || wandStack.isEmpty() || !(wandStack.getItem() instanceof IWandItem)) {
            return;
        }

        //渲染撤回选择框
        if (event.getPlayer().isSneaking()) {
            List<Point3d> positions = Utils.getLastUndoCandidatePositions(wandStack, event.getPlayer().getEntityWorld());
            BlockPos looked = Utils.findLookedAtLastUndoCandidate(event.getPlayer(), wandStack, event.getPlayer().getEntityWorld(), event.getPartialTicks());
            if (looked != null) {
                Utils.renderSelectionBox(positions, event.getPlayer(), true, event.getPartialTicks(), 1.0F, 1.0F, 0.0F, 0.6F);
                return;
            }
        }

        RayTraceResult target = event.getTarget();
        if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }

        //渲染放置方块选择框
        if (!Utils.isBreakModeActive(wandStack)) {
            List<Point3d> positions = Utils.getWandBlockPositionList(event.getPlayer(), wandStack, event.getTarget().getBlockPos(), event.getTarget().sideHit, event.getTarget().hitVec);
            Utils.renderSelectionBox(positions, event.getPlayer(), true, event.getPartialTicks(), 1.0F, 1.0F, 1.0F, 0.4F);
            return;
        }

        //渲染破坏模式选择框
        List<Point3d> positions = Utils.getWandBreakPositionList(event.getPlayer(), wandStack, target.getBlockPos(), target.sideHit);
        Utils.renderSelectionBox(positions, event.getPlayer(), false, event.getPartialTicks(), 1.0F, 0.0F, 0.0F, 0.4F);
    }
}
