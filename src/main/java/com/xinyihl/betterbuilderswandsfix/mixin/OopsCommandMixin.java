package com.xinyihl.betterbuilderswandsfix.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import portablejim.bbw.core.OopsCommand;

@Mixin(value = OopsCommand.class, remap = false)
public abstract class OopsCommandMixin {
    @Redirect(
            method = "execute",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockToAir(Lnet/minecraft/util/math/BlockPos;)Z"
            ),
            remap = true
    )
    public boolean injected(World instance, BlockPos blockPos, @Local(name = "player") EntityPlayerMP player){
        IBlockState iBlockState = player.getEntityWorld().getBlockState(blockPos);
        ItemStack item = iBlockState.getBlock().getPickBlock(iBlockState, new RayTraceResult(player), player.getEntityWorld(), blockPos, player);
        boolean isSetAir = instance.setBlockToAir(blockPos);
        if (isSetAir && !player.isCreative()) player.getServerWorld().spawnEntity(new EntityItem(player.getEntityWorld(), player.posX, player.posY, player.posZ, item));
        return isSetAir;
    }

    @Redirect(
            method = "execute",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NBTTagCompound;hasKey(Ljava/lang/String;I)Z",
                    ordinal = 2
            ),
            remap = true
    )
    public boolean injected(NBTTagCompound instance, String s, int i){
        instance.removeTag("lastPlaced");
        instance.removeTag("lastBlock");
        instance.removeTag("lastItemBlock");
        instance.removeTag("lastBlockMeta");
        instance.removeTag("lastPerBlock");
        return false;
    }
}
