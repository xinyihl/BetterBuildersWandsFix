package com.xinyihl.betterbuilderswandsfix.common;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import portablejim.bbw.basics.Point3d;
import portablejim.bbw.core.items.IWandItem;
import portablejim.bbw.shims.BasicPlayerShim;

import java.util.ArrayList;

public class Utils {

    public static void undoPlaceBlocks(EntityPlayerMP player) {
        ItemStack currentItemstack = BasicPlayerShim.getHeldWandIfAny(player);
        if (currentItemstack != null && currentItemstack.getItem() instanceof IWandItem) {
            NBTTagCompound tagComponent = currentItemstack.getTagCompound();
            if (tagComponent != null && tagComponent.hasKey("bbw", 10)) {
                NBTTagCompound bbwCompound = tagComponent.getCompoundTag("bbw");
                if (bbwCompound.hasKey("undoHistory", 9)) {
                    net.minecraft.nbt.NBTTagList historyList = bbwCompound.getTagList("undoHistory", 10);
                    if (historyList.tagCount() > 0) {
                        int lastIndex = historyList.tagCount() - 1;
                        NBTTagCompound historyEntry = historyList.getCompoundTagAt(lastIndex);
                        ArrayList<Point3d> pointList = unpackNbt(historyEntry.getIntArray("positions"));
                        String blockStateString = historyEntry.getString("blockState");
                        for (Point3d point : pointList) {
                            IBlockState pointState = player.getEntityWorld().getBlockState(new BlockPos(point.x, point.y, point.z));
                            String pointStateString = pointState.toString();
                            if (pointStateString != null && pointStateString.equals(blockStateString)) {
                                BlockPos blockPos = new BlockPos(point.x, point.y, point.z);
                                IBlockState iBlockState = player.getEntityWorld().getBlockState(blockPos);
                                ItemStack item = iBlockState.getBlock().getPickBlock(iBlockState, new RayTraceResult(player), player.getEntityWorld(), blockPos, player);
                                boolean isSetAir = player.getEntityWorld().setBlockToAir(blockPos);
                                if (isSetAir && !player.isCreative()) player.getServerWorld().spawnEntity(new EntityItem(player.getEntityWorld(), player.posX, player.posY, player.posZ, item));
                            }
                        }
                        historyList.removeTag(lastIndex);
                        if (historyList.tagCount() == 0) {
                            bbwCompound.removeTag("undoHistory");
                        }
                    } else {
                        player.sendMessage(new TextComponentTranslation("bbw.chat.error.noundo"));
                    }
                }
                else if (bbwCompound.hasKey("lastPlaced", 11)) {
                    ArrayList<Point3d> pointList = unpackNbt(bbwCompound.getIntArray("lastPlaced"));
                    for (Point3d point : pointList) {
                        IBlockState pointState = player.getEntityWorld().getBlockState(new BlockPos(point.x, point.y, point.z));
                        String pointStateString = pointState.toString();
                        if (pointStateString != null && bbwCompound.hasKey("lastBlock") && pointStateString.equals(bbwCompound.getString("lastBlock"))) {
                            BlockPos blockPos = new BlockPos(point.x, point.y, point.z);
                            IBlockState iBlockState = player.getEntityWorld().getBlockState(blockPos);
                            ItemStack item = iBlockState.getBlock().getPickBlock(iBlockState, new RayTraceResult(player), player.getEntityWorld(), blockPos, player);
                            boolean isSetAir = player.getEntityWorld().setBlockToAir(blockPos);
                            if (isSetAir && !player.isCreative()) player.getServerWorld().spawnEntity(new EntityItem(player.getEntityWorld(), player.posX, player.posY, player.posZ, item));
                        }
                    }
                    bbwCompound.removeTag("lastPlaced");
                    bbwCompound.removeTag("lastBlock");
                    bbwCompound.removeTag("lastItemBlock");
                    bbwCompound.removeTag("lastBlockMeta");
                    bbwCompound.removeTag("lastPerBlock");
                } else {
                    player.sendMessage(new TextComponentTranslation("bbw.chat.error.noundo"));
                }
            } else {
                player.sendMessage(new TextComponentTranslation("bbw.chat.error.noundo"));
            }
        } else {
            player.sendMessage(new TextComponentTranslation("bbw.chat.error.nowand"));
        }
    }

    private static ArrayList<Point3d> unpackNbt(int[] placedBlocks) {
        ArrayList<Point3d> output = new ArrayList<>();
        int countPoints = placedBlocks.length / 3;
        for (int i = 0; i < countPoints * 3; i += 3) {
            output.add(new Point3d(placedBlocks[i], placedBlocks[i + 1], placedBlocks[i + 2]));
        }
        return output;
    }
}
