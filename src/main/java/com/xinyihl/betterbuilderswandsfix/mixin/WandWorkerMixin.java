package com.xinyihl.betterbuilderswandsfix.mixin;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import portablejim.bbw.basics.Point3d;
import portablejim.bbw.basics.ReplacementTriplet;
import portablejim.bbw.core.WandWorker;
import portablejim.bbw.core.items.ItemBasicWand;
import portablejim.bbw.core.wands.IWand;
import portablejim.bbw.shims.BasicPlayerShim;
import portablejim.bbw.shims.IPlayerShim;
import portablejim.bbw.shims.IWorldShim;

import java.util.ArrayList;
import java.util.LinkedList;

@Mixin(value = WandWorker.class, remap = false)
public abstract class WandWorkerMixin {

    @Final
    @Shadow
    private IPlayerShim player;
    @Final
    @Shadow
    private IWorldShim world;

    @Final
    @Shadow
    private IWand wand;

    @Inject(
            method = "getProperItemStack",
            at = @At(
                    value = "INVOKE",
                    target = "Lportablejim/bbw/core/conversion/CustomMappingManager;getMappings(Lnet/minecraft/block/Block;I)Ljava/util/ArrayList;"
            ),
            cancellable = true
    )
    public void injected(IWorldShim world, IPlayerShim player, Point3d blockPos, float hitX, float hitY, float hitZ, CallbackInfoReturnable<ReplacementTriplet> cir) {
        ItemStack heldItem = player.getPlayer().getHeldItem(EnumHand.OFF_HAND).copy();
        heldItem.setCount(1);
        IBlockState heldBlockState = BasicPlayerShim.getBlock(heldItem).getStateForPlacement(world.getWorld(), blockPos.toBlockPos(), player.getPlayer().getHorizontalFacing(), hitX, hitY, hitZ, heldItem.getMetadata(), player.getPlayer(), EnumHand.MAIN_HAND);
        if (!heldItem.isEmpty()) {
            if (player.countItems(heldItem) > 0 && heldBlockState.getBlock() != Blocks.AIR) {
                cir.setReturnValue(new ReplacementTriplet(heldBlockState, heldItem, heldBlockState));
                return;
            }
            cir.setReturnValue(null);
        }
    }

    /**
     * @author xinyihl
     * @reason 修复手杖放方块缺少nbt
     */
    @SuppressWarnings("deprecation")
    @Overwrite
    public ArrayList<Point3d> placeBlocks(ItemStack wandItem, LinkedList<Point3d> blockPosList, IBlockState targetBlock, ItemStack sourceItems, EnumFacing side, float hitX, float hitY, float hitZ) {
        ArrayList<Point3d> placedBlocks = new ArrayList<>();
        EnumHand hand = player.getPlayer().getHeldItemMainhand().getItem() instanceof ItemBasicWand ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
        for (Point3d blockPos : blockPosList) {
            BlockSnapshot snapshot = new BlockSnapshot(world.getWorld(), blockPos.toBlockPos(), targetBlock);
            BlockEvent.PlaceEvent placeEvent = new BlockEvent.PlaceEvent(snapshot, targetBlock, player.getPlayer(), hand);
            MinecraftForge.EVENT_BUS.post(placeEvent);
            if (!placeEvent.isCanceled()) {
                ItemStack itemFromInventory = player.useItem(sourceItems);
                if (itemFromInventory != null && itemFromInventory.getItem() instanceof ItemBlock) {
                    ItemBlock itemBlock = ((ItemBlock) itemFromInventory.getItem());
                    if (itemBlock.getBlock().canPlaceBlockAt(world.getWorld(), blockPos.toBlockPos()) && itemBlock.placeBlockAt(itemFromInventory, player.getPlayer(), world.getWorld(), blockPos.toBlockPos(), EnumFacing.DOWN, hitX, hitY, hitZ, targetBlock)){
                        world.playPlaceAtBlock(blockPos, targetBlock.getBlock());
                        placedBlocks.add(blockPos);
                        if (!player.isCreative()) wand.placeBlock(wandItem, player.getPlayer());
                    }else {
                        itemFromInventory.setCount(itemFromInventory.getCount() + 1);
                    }
                }
            }
        }
        return placedBlocks;
    }
}