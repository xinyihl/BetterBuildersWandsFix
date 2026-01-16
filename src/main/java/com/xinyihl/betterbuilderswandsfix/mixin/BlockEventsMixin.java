package com.xinyihl.betterbuilderswandsfix.mixin;

import com.xinyihl.betterbuilderswandsfix.common.Utils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import portablejim.bbw.core.BlockEvents;
import portablejim.bbw.basics.Point3d;
import portablejim.bbw.core.items.IWandItem;
import portablejim.bbw.shims.BasicPlayerShim;

import java.util.LinkedList;

import org.lwjgl.opengl.GL11;

@Mixin(value = BlockEvents.class, remap = false)
public abstract class BlockEventsMixin {

    @Shadow
    public abstract void blockHighlightEvent(DrawBlockHighlightEvent event);

    @Unique
    @SubscribeEvent(receiveCanceled = true)
    public void betterBuildersWandsFix$blockHighlightEvent(DrawBlockHighlightEvent event) {
        ItemStack wandStack = BasicPlayerShim.getHeldWandIfAny(event.getPlayer());
        if (wandStack == null || wandStack.isEmpty() || !(wandStack.getItem() instanceof IWandItem)) {
            return;
        }

        if (!Utils.isBreakModeActive(wandStack)) {
            if (event.isCanceled()) {
                blockHighlightEvent(event);
            }
            return;
        }

        RayTraceResult target = event.getTarget();
        if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }

        int maxBlocks = Utils.getWandMaxBlocks(wandStack);
        LinkedList<Point3d> positions = Utils.getWandBreakPositionList(event.getPlayer(), wandStack, target.getBlockPos(), target.sideHit, maxBlocks);
        if (positions.isEmpty()) {
            return;
        }

        event.setCanceled(true);

        double partialTicks = event.getPartialTicks();
        double dx = event.getPlayer().lastTickPosX + (event.getPlayer().posX - event.getPlayer().lastTickPosX) * partialTicks;
        double dy = event.getPlayer().lastTickPosY + (event.getPlayer().posY - event.getPlayer().lastTickPosY) * partialTicks;
        double dz = event.getPlayer().lastTickPosZ + (event.getPlayer().posZ - event.getPlayer().lastTickPosZ) * partialTicks;

        GlStateManager.disableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GL11.glLineWidth(2.5F);

        for (Point3d point : positions) {
            BlockPos pos = new BlockPos(point.x, point.y, point.z);
            IBlockState state = event.getPlayer().getEntityWorld().getBlockState(pos);
            if (state.getBlock().isAir(state, event.getPlayer().getEntityWorld(), pos)) {
                continue;
            }
            AxisAlignedBB bb = state.getSelectedBoundingBox(event.getPlayer().getEntityWorld(), pos).grow(0.002D).offset(-dx, -dy, -dz);
            RenderGlobal.drawSelectionBoundingBox(bb, 1.0F, 0.0F, 0.0F, 0.4F);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
    }
}
