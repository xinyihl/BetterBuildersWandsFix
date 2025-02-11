package com.xinyihl.betterbuilderswandsfix.mixin;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import org.spongepowered.asm.mixin.*;
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

import static net.minecraft.world.chunk.Chunk.NULL_BLOCK_STORAGE;

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
        if (!heldItem.isEmpty()) {
            heldItem.setCount(1);
            IBlockState heldBlockState = BasicPlayerShim.getBlock(heldItem).getStateForPlacement(world.getWorld(), blockPos.toBlockPos(), player.getPlayer().getHorizontalFacing(), hitX, hitY, hitZ, heldItem.getMetadata(), player.getPlayer(), EnumHand.MAIN_HAND);
            if (player.countItems(heldItem) > 0 && heldBlockState.getBlock() != Blocks.AIR) {
                cir.setReturnValue(new ReplacementTriplet(heldBlockState, heldItem, heldBlockState));
                return;
            }
            cir.setReturnValue(null);
        }
    }

    @Unique
    private boolean betterBuildersWandsFix$setBlockWithoutLighting(World world, BlockPos pos, IBlockState newState) {
        Chunk chunk = world.getChunk(pos);
        int sectionIndex = pos.getY() >> 4;
        ExtendedBlockStorage[] storageArray = chunk.getBlockStorageArray();
        ExtendedBlockStorage storage = storageArray[sectionIndex];

        if (storage == NULL_BLOCK_STORAGE)
        {
            storage = new ExtendedBlockStorage(pos.getY() >> 4 << 4, chunk.getWorld().provider.hasSkyLight());
            chunk.getBlockStorageArray()[sectionIndex] = storage;
        }

        int x = pos.getX() & 15;
        int y = pos.getY() & 15;
        int z = pos.getZ() & 15;

        IBlockState oldState = storage.get(x, y, z);
        storage.set(x, y, z, newState);
        chunk.markDirty();
        world.notifyBlockUpdate(pos, oldState, newState, 2);
        return true;
    }

    /**
     * @author xinyihl
     * @reason 修复手杖放方块缺少nbt
     */
    @SuppressWarnings("deprecation")
    @Overwrite
    public ArrayList<Point3d> placeBlocks(ItemStack wandItem, LinkedList<Point3d> blockPosList, IBlockState targetBlock, ItemStack sourceItems, EnumFacing side, float hitX, float hitY, float hitZ) {
        EntityPlayer entityPlayer = player.getPlayer();
        World worldObj = world.getWorld();
        ArrayList<Point3d> placedBlocks = new ArrayList<>();
        EnumHand hand = player.getPlayer().getHeldItemMainhand().getItem() instanceof ItemBasicWand ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
        for (Point3d blockPos : blockPosList) {
            BlockPos bp = blockPos.toBlockPos();
            BlockSnapshot snapshot = new BlockSnapshot(worldObj, bp, targetBlock);
            BlockEvent.PlaceEvent placeEvent = new BlockEvent.PlaceEvent(snapshot, targetBlock, entityPlayer, hand);
            MinecraftForge.EVENT_BUS.post(placeEvent);
            if (placeEvent.isCanceled()) continue;
            ItemStack itemFromInventory = player.useItem(sourceItems);
            if (itemFromInventory != null && itemFromInventory.getItem() instanceof ItemBlock) {
                ItemBlock itemBlock = ((ItemBlock) itemFromInventory.getItem());
                boolean isPlace;
                if (itemFromInventory.hasTagCompound()) {
                    isPlace = itemBlock.getBlock().canPlaceBlockAt(worldObj, bp) && itemBlock.placeBlockAt(itemFromInventory, entityPlayer, worldObj, bp, EnumFacing.DOWN, hitX, hitY, hitZ, targetBlock);
                } else {
                      isPlace = betterBuildersWandsFix$setBlockWithoutLighting(worldObj, bp, targetBlock);
                    //isPlace = world.setBlock(blockPos, targetBlock);
                }
                if(isPlace){
                    world.playPlaceAtBlock(blockPos, targetBlock.getBlock());
                    placedBlocks.add(blockPos);
                    if (!player.isCreative()) wand.placeBlock(wandItem, entityPlayer);
                } else {
                    itemFromInventory.setCount(1);
                    EntityItem entityItem = new EntityItem(worldObj, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, itemFromInventory);
                    world.getWorld().spawnEntity(entityItem);
                }
            }
        }
        return placedBlocks;
    }
}