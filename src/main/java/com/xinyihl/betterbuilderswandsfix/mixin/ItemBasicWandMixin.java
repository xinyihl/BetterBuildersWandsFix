package com.xinyihl.betterbuilderswandsfix.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.xinyihl.betterbuilderswandsfix.common.Configurations;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import portablejim.bbw.core.items.ItemBasicWand;

@Mixin(value = ItemBasicWand.class, remap = false)
public abstract class ItemBasicWandMixin {

    @Inject(
            method = "onItemUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;setTagInfo(Ljava/lang/String;Lnet/minecraft/nbt/NBTBase;)V"
            ),
            cancellable = true
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
            cir.setReturnValue(EnumActionResult.SUCCESS);
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
}
