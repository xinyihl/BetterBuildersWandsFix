package com.xinyihl.betterbuilderswandsfix.common;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import portablejim.bbw.basics.Point3d;
import portablejim.bbw.basics.EnumLock;
import portablejim.bbw.basics.EnumFluidLock;
import portablejim.bbw.core.items.IWandItem;
import portablejim.bbw.core.wands.IWand;
import portablejim.bbw.core.WandWorker;
import portablejim.bbw.shims.BasicPlayerShim;
import portablejim.bbw.shims.BasicWorldShim;
import portablejim.bbw.shims.CreativePlayerShim;
import portablejim.bbw.shims.IPlayerShim;
import portablejim.bbw.shims.IWorldShim;

import java.util.ArrayList;
import java.util.LinkedList;

public class Utils {

    public static void undoPlaceBlocks(EntityPlayerMP player) {
        ItemStack currentItemstack = BasicPlayerShim.getHeldWandIfAny(player);
        if (currentItemstack != null && currentItemstack.getItem() instanceof IWandItem) {
            NBTTagCompound tagComponent = currentItemstack.getTagCompound();
            if (tagComponent != null && tagComponent.hasKey("bbw", 10)) {
                NBTTagCompound bbwCompound = tagComponent.getCompoundTag("bbw");
                long breakTs = getLastHistoryTimestamp(bbwCompound, "breakHistory");
                long undoTs = getLastHistoryTimestamp(bbwCompound, "undoHistory");
                if (breakTs > 0 || undoTs > 0) {
                    if (breakTs >= undoTs) {
                        if (undoBreakBlocks(player, bbwCompound)) {
                            return;
                        }
                    }
                    if (undoPlacedBlocks(player, bbwCompound)) {
                        return;
                    }
                }

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
                        player.sendStatusMessage(new TextComponentTranslation("bbw.chat.undo"), true);
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
                    player.sendStatusMessage(new TextComponentTranslation("bbw.chat.undo"), true);
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

    private static long getLastHistoryTimestamp(NBTTagCompound bbwCompound, String key) {
        if (!bbwCompound.hasKey(key, 9)) {
            return -1L;
        }
        NBTTagList list = bbwCompound.getTagList(key, 10);
        if (list.tagCount() == 0) {
            return -1L;
        }
        NBTTagCompound entry = list.getCompoundTagAt(list.tagCount() - 1);
        if (entry.hasKey("timestamp")) {
            return entry.getLong("timestamp");
        }
        return -1L;
    }

    private static boolean undoPlacedBlocks(EntityPlayerMP player, NBTTagCompound bbwCompound) {
        if (!bbwCompound.hasKey("undoHistory", 9)) {
            return false;
        }
        NBTTagList historyList = bbwCompound.getTagList("undoHistory", 10);
        if (historyList.tagCount() == 0) {
            return false;
        }
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
        player.sendStatusMessage(new TextComponentTranslation("bbw.chat.undo"), true);
        return true;
    }

    private static boolean undoBreakBlocks(EntityPlayerMP player, NBTTagCompound bbwCompound) {
        if (!bbwCompound.hasKey("breakHistory", 9)) {
            return false;
        }
        NBTTagList historyList = bbwCompound.getTagList("breakHistory", 10);
        if (historyList.tagCount() == 0) {
            return false;
        }
        int lastIndex = historyList.tagCount() - 1;
        NBTTagCompound entry = historyList.getCompoundTagAt(lastIndex);
        if (entry.hasKey("dimension") && entry.getInteger("dimension") != player.dimension) {
            return false;
        }
        NBTTagList blocks = entry.getTagList("blocks", 10);
        for (int i = 0; i < blocks.tagCount(); i++) {
            NBTTagCompound blockEntry = blocks.getCompoundTagAt(i);
            int x = blockEntry.getInteger("x");
            int y = blockEntry.getInteger("y");
            int z = blockEntry.getInteger("z");
            String blockName = blockEntry.getString("block");
            int meta = blockEntry.getInteger("meta");
            Block block = Block.getBlockFromName(blockName);
            if (block == null) {
                continue;
            }
            BlockPos pos = new BlockPos(x, y, z);
            IBlockState state = block.getStateFromMeta(meta);
            player.getEntityWorld().setBlockState(pos, state, 3);
        }
        historyList.removeTag(lastIndex);
        if (historyList.tagCount() == 0) {
            bbwCompound.removeTag("breakHistory");
        }
        player.sendStatusMessage(new TextComponentTranslation("bbw.chat.undo"), true);
        return true;
    }

    private static ArrayList<Point3d> unpackNbt(int[] placedBlocks) {
        ArrayList<Point3d> output = new ArrayList<>();
        int countPoints = placedBlocks.length / 3;
        for (int i = 0; i < countPoints * 3; i += 3) {
            output.add(new Point3d(placedBlocks[i], placedBlocks[i + 1], placedBlocks[i + 2]));
        }
        return output;
    }

    public static boolean isBreakModeUnlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("bbw", 10)) {
            return false;
        }
        return tag.getCompoundTag("bbw").getBoolean("breakModeUnlocked");
    }

    public static boolean isBreakModeActive(ItemStack stack) {
        if (!isBreakModeUnlocked(stack)) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag.getCompoundTag("bbw").getBoolean("breakModeActive");
    }

    public static void setBreakModeActive(ItemStack stack, boolean active) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        NBTTagCompound bbw = tag.getCompoundTag("bbw");
        if (!tag.hasKey("bbw", 10)) {
            tag.setTag("bbw", bbw);
        }
        bbw.setBoolean("breakModeActive", active);
    }

    public static void recordBreakHistory(ItemStack wandStack, World world, LinkedList<Point3d> positions) {
        if (wandStack == null || wandStack.isEmpty() || positions == null || positions.isEmpty()) {
            return;
        }
        NBTTagCompound tag = wandStack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            wandStack.setTagCompound(tag);
        }
        NBTTagCompound bbw = tag.getCompoundTag("bbw");
        if (!tag.hasKey("bbw", 10)) {
            tag.setTag("bbw", bbw);
        }

        NBTTagList blockList = new NBTTagList();
        for (Point3d point : positions) {
            BlockPos pos = new BlockPos(point.x, point.y, point.z);
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock().isAir(state, world, pos)) {
                continue;
            }
            NBTTagCompound blockEntry = new NBTTagCompound();
            blockEntry.setInteger("x", pos.getX());
            blockEntry.setInteger("y", pos.getY());
            blockEntry.setInteger("z", pos.getZ());
            blockEntry.setString("block", Block.REGISTRY.getNameForObject(state.getBlock()).toString());
            blockEntry.setInteger("meta", state.getBlock().getMetaFromState(state));
            blockList.appendTag(blockEntry);
        }
        if (blockList.tagCount() == 0) {
            return;
        }

        NBTTagCompound entry = new NBTTagCompound();
        entry.setInteger("dimension", world.provider.getDimension());
        entry.setLong("timestamp", System.currentTimeMillis());
        entry.setTag("blocks", blockList);

        NBTTagList historyList;
        if (bbw.hasKey("breakHistory", 9)) {
            historyList = bbw.getTagList("breakHistory", 10);
        } else {
            historyList = new NBTTagList();
            bbw.setTag("breakHistory", historyList);
        }
        historyList.appendTag(entry);

        int maxHistory = Configurations.maxUndoHistory;
        while (historyList.tagCount() > maxHistory) {
            historyList.removeTag(0);
        }
    }

    public static int getWandMaxBlocks(ItemStack wandStack) {
        if (wandStack == null || wandStack.isEmpty() || !(wandStack.getItem() instanceof IWandItem)) {
            return 0;
        }
        IWandItem wandItem = (IWandItem) wandStack.getItem();
        IWand wand = wandItem.getWand();
        return Math.max(0, wand.getMaxBlocks(wandStack));
    }

    public static LinkedList<Point3d> getWandBlockPositionList(EntityPlayer player, ItemStack wandStack, BlockPos pos, EnumFacing side, int maxBlocks) {
        if (player == null || wandStack == null || wandStack.isEmpty() || !(wandStack.getItem() instanceof IWandItem)) {
            return new LinkedList<>();
        }

        IWandItem wandItem = (IWandItem) wandStack.getItem();
        IWand wand = wandItem.getWand();
        IPlayerShim playerShim = wandItem.getPlayerShim(player);
        if (player.capabilities.isCreativeMode) {
            playerShim = new CreativePlayerShim(player);
        }
        IWorldShim worldShim = new BasicWorldShim(player.getEntityWorld());
        WandWorker worker = new WandWorker(wand, playerShim, worldShim);

        EnumLock mode = wandItem.getMode(wandStack);
        EnumLock faceLock = wandItem.getFaceLock(wandStack);
        EnumFluidLock fluidMode = wandItem.getFluidMode(wandStack);

        Point3d origin = new Point3d(pos.getX(), pos.getY(), pos.getZ());
        return worker.getBlockPositionList(origin, side, maxBlocks, mode, faceLock, fluidMode);
    }

    public static LinkedList<Point3d> getWandBreakPositionList(EntityPlayer player, ItemStack wandStack, BlockPos pos, EnumFacing side, int maxBlocks) {
        LinkedList<Point3d> placementList = getWandBlockPositionList(player, wandStack, pos, side, maxBlocks);
        if (placementList.isEmpty()) {
            return placementList;
        }
        LinkedList<Point3d> breakList = new LinkedList<>();
        EnumFacing opposite = side.getOpposite();
        int dx = opposite.getXOffset();
        int dy = opposite.getYOffset();
        int dz = opposite.getZOffset();
        for (Point3d point : placementList) {
            breakList.add(new Point3d(point.x + dx, point.y + dy, point.z + dz));
        }
        return breakList;
    }
}
