package com.xinyihl.betterbuilderswandsfix.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.xinyihl.betterbuilderswandsfix.common.Configurations;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import portablejim.bbw.core.items.ItemBasicWand;
import com.xinyihl.betterbuilderswandsfix.common.Utils;
import portablejim.bbw.basics.Point3d;

import java.util.LinkedList;
import java.util.List;

@Mixin(value = ItemBasicWand.class, remap = false)
public abstract class ItemBasicWandMixin {

    @Inject(
            method = "onItemUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;setTagInfo(Ljava/lang/String;Lnet/minecraft/nbt/NBTBase;)V"
            )
    )
    private void afterSaveLastPlaced(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<EnumActionResult> cir, @Local(name = "bbwCompound") NBTTagCompound bbwCompound) {
        if (world.isRemote || bbwCompound == null) {
            return;
        }
        if (bbwCompound.hasKey("lastPlaced", 11)) {
            int[] lastPlaced = bbwCompound.getIntArray("lastPlaced");
            this.betterBuildersWandsFix$addToUndoHistory(bbwCompound, world, lastPlaced);
            bbwCompound.removeTag("lastPlaced");
            bbwCompound.removeTag("lastBlock");
            bbwCompound.removeTag("lastItemBlock");
            bbwCompound.removeTag("lastBlockMeta");
            bbwCompound.removeTag("lastPerBlock");
        }
    }

    @Unique
    private void betterBuildersWandsFix$addToUndoHistory(NBTTagCompound bbwTag, World world, int[] positions) {
        if (positions == null || positions.length == 0) {
            return;
        }
        NBTTagList historyList;
        if (bbwTag.hasKey("undoHistory", 9)) {
            historyList = bbwTag.getTagList("undoHistory", 10);
        } else {
            historyList = new NBTTagList();
            bbwTag.setTag("undoHistory", historyList);
        }
        NBTTagCompound historyEntry = new NBTTagCompound();
        historyEntry.setIntArray("positions", positions);
        historyEntry.setInteger("dimension", world.provider.getDimension());
        historyEntry.setLong("timestamp", System.currentTimeMillis());
        if (positions.length >= 3) {
            BlockPos firstPos = new BlockPos(positions[0], positions[1], positions[2]);
            IBlockState blockState = world.getBlockState(firstPos);
            historyEntry.setString("blockState", blockState.toString());
        }
        historyList.appendTag(historyEntry);
        int maxHistory = Configurations.maxUndoHistory;
        while (historyList.tagCount() > maxHistory) {
            historyList.removeTag(0);
        }
    }

    @Inject(
            method = "onItemUse",
            at = @At("HEAD"),
            cancellable = true
    )
    private void betterBuildersWandsFix$handleBreakMode(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<EnumActionResult> cir) {
        if (world.isRemote || !Configurations.breakModeEnabled) {
            return;
        }
        ItemStack wandStack = player.getHeldItem(hand);
        if (wandStack.isEmpty() || !Utils.isBreakModeUnlocked(wandStack)) {
            return;
        }

        if (player.isSneaking()) {
            boolean next = !Utils.isBreakModeActive(wandStack);
            Utils.setBreakModeActive(wandStack, next);
            cir.setReturnValue(EnumActionResult.SUCCESS);
            return;
        }

        if (!Utils.isBreakModeActive(wandStack)) {
            return;
        }

        IBlockState targetState = world.getBlockState(pos);
        if (targetState.getBlock().isAir(targetState, world, pos)) {
            cir.setReturnValue(EnumActionResult.PASS);
            return;
        }

        int maxBlocks = Utils.getWandMaxBlocks(wandStack);
        LinkedList<Point3d> positions = Utils.getWandBreakPositionList(player, wandStack, pos, side, maxBlocks);
        if (positions == null || positions.isEmpty()) {
            cir.setReturnValue(EnumActionResult.PASS);
            return;
        }

        LinkedList<Point3d> breakablePositions = new LinkedList<>();
        for (Point3d point : positions) {
            BlockPos breakPos = new BlockPos(point.x, point.y, point.z);
            IBlockState state = world.getBlockState(breakPos);
            if (state.getBlockHardness(world, breakPos) < 0.0f) {
                continue;
            }
            if (state.getBlock().isAir(state, world, breakPos)) {
                continue;
            }
            if (state.getBlock().hasTileEntity(state) || world.getTileEntity(breakPos) != null) {
                continue;
            }
            breakablePositions.add(point);
        }

        if (breakablePositions.isEmpty()) {
            cir.setReturnValue(EnumActionResult.PASS);
            return;
        }

        Utils.recordBreakHistory(wandStack, world, breakablePositions);

        for (Point3d point : breakablePositions) {
            BlockPos breakPos = new BlockPos(point.x, point.y, point.z);
            world.setBlockToAir(breakPos);
        }

        cir.setReturnValue(EnumActionResult.SUCCESS);
    }

    @Inject(
            method = "addInformation",
            at = @At("TAIL")
    )
    private void betterBuildersWandsFix$addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn, CallbackInfo ci) {
        if (!Configurations.breakModeEnabled) {
            return;
        }
        if (!Utils.isBreakModeUnlocked(stack)) {
            tooltip.add(I18n.format("bbw.tooltip.break_mode.locked"));
            return;
        }
        boolean active = Utils.isBreakModeActive(stack);
        tooltip.add(I18n.format("bbw.tooltip.break_mode", I18n.format(active ? "bbw.tooltip.break_mode.on" : "bbw.tooltip.break_mode.off")));
        tooltip.add(I18n.format("bbw.tooltip.break_mode.toggle"));
    }
}
