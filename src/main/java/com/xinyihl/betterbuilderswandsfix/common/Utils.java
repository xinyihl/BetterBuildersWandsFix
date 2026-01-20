package com.xinyihl.betterbuilderswandsfix.common;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import portablejim.bbw.basics.EnumFluidLock;
import portablejim.bbw.basics.EnumLock;
import portablejim.bbw.basics.Point3d;
import portablejim.bbw.basics.ReplacementTriplet;
import portablejim.bbw.core.WandWorker;
import portablejim.bbw.core.items.IWandItem;
import portablejim.bbw.core.wands.IWand;
import portablejim.bbw.shims.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Utils {

    public static void undoOperation(EntityPlayerMP player) {
        ItemStack currentItemstack = BasicPlayerShim.getHeldWandIfAny(player);
        if (currentItemstack != null && currentItemstack.getItem() instanceof IWandItem) {
            NBTTagCompound tagComponent = currentItemstack.getTagCompound();
            if (tagComponent != null && tagComponent.hasKey("bbw", 10)) {
                NBTTagCompound bbwCompound = tagComponent.getCompoundTag("bbw");
                long breakTs = getLastTimestamp(bbwCompound, "breakHistory");
                long undoTs = getLastTimestamp(bbwCompound, "undoHistory");
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
                player.sendMessage(new TextComponentTranslation("bbw.chat.error.noundo"));
            } else {
                player.sendMessage(new TextComponentTranslation("bbw.chat.error.noundo"));
            }
        } else {
            player.sendMessage(new TextComponentTranslation("bbw.chat.error.nowand"));
        }
    }

    private static long getLastTimestamp(NBTTagCompound bbwCompound, String key) {
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
                if (isSetAir && !player.isCreative())
                    player.getServerWorld().spawnEntity(new EntityItem(player.getEntityWorld(), player.posX, player.posY, player.posZ, item));
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

    private static NBTTagCompound getBbwCompound(ItemStack wandStack) {
        if (wandStack == null || wandStack.isEmpty()) {
            return null;
        }
        NBTTagCompound tag = wandStack.getTagCompound();
        if (tag == null || !tag.hasKey("bbw", 10)) {
            return null;
        }
        return tag.getCompoundTag("bbw");
    }

    private static long getEntryTimestamp(NBTTagCompound entry) {
        if (entry == null) {
            return -1L;
        }
        if (entry.hasKey("timestamp")) {
            return entry.getLong("timestamp");
        }
        return -1L;
    }

    private static NBTTagCompound getLastHistoryEntryForDimension(NBTTagCompound bbwCompound, String key, int dimension) {
        if (bbwCompound == null || !bbwCompound.hasKey(key, 9)) {
            return null;
        }
        NBTTagList list = bbwCompound.getTagList(key, 10);
        if (list.tagCount() == 0) {
            return null;
        }
        NBTTagCompound entry = list.getCompoundTagAt(list.tagCount() - 1);
        if (entry.hasKey("dimension") && entry.getInteger("dimension") != dimension) {
            return null;
        }
        return entry;
    }

    private static ArrayList<Point3d> unpackUndoHistoryPositions(World world, NBTTagCompound undoEntry) {
        ArrayList<Point3d> out = new ArrayList<>();
        if (world == null || undoEntry == null) {
            return out;
        }
        int[] packed = undoEntry.getIntArray("positions");
        ArrayList<Point3d> points = unpackNbt(packed);
        String blockStateString = undoEntry.getString("blockState");
        for (Point3d point : points) {
            BlockPos pos = new BlockPos(point.x, point.y, point.z);
            IBlockState current = world.getBlockState(pos);
            String currentString = current.toString();
            if (currentString != null && currentString.equals(blockStateString)) {
                out.add(point);
            }
        }
        return out;
    }

    private static ArrayList<Point3d> unpackBreakHistoryPositions(NBTTagCompound breakEntry) {
        ArrayList<Point3d> out = new ArrayList<>();
        if (breakEntry == null || !breakEntry.hasKey("blocks", 9)) {
            return out;
        }
        NBTTagList blocks = breakEntry.getTagList("blocks", 10);
        for (int i = 0; i < blocks.tagCount(); i++) {
            NBTTagCompound blockEntry = blocks.getCompoundTagAt(i);
            int x = blockEntry.getInteger("x");
            int y = blockEntry.getInteger("y");
            int z = blockEntry.getInteger("z");
            out.add(new Point3d(x, y, z));
        }
        return out;
    }

    public static List<Point3d> getLastUndoCandidatePositions(ItemStack wandStack, World world) {
        ArrayList<Point3d> empty = new ArrayList<>();
        if (world == null) {
            return empty;
        }
        NBTTagCompound bbw = getBbwCompound(wandStack);
        if (bbw == null) {
            return empty;
        }

        int dim = world.provider.getDimension();
        NBTTagCompound undoEntry = getLastHistoryEntryForDimension(bbw, "undoHistory", dim);
        NBTTagCompound breakEntry = getLastHistoryEntryForDimension(bbw, "breakHistory", dim);
        long undoTs = getEntryTimestamp(undoEntry);
        long breakTs = getEntryTimestamp(breakEntry);

        if (undoTs < 0 && breakTs < 0) {
            return empty;
        }

        if (breakTs >= undoTs) {
            return unpackBreakHistoryPositions(breakEntry);
        }
        return unpackUndoHistoryPositions(world, undoEntry);
    }

    public static BlockPos findLookedAtLastUndoCandidate(EntityPlayer player, ItemStack wandStack, World world, float partialTicks) {
        if (player == null || world == null) {
            return null;
        }
        List<Point3d> candidates = getLastUndoCandidatePositions(wandStack, world);
        if (candidates.isEmpty()) {
            return null;
        }

        double reach = player.capabilities != null && player.capabilities.isCreativeMode ? 6.0D : 5.0D;
        Vec3d start = player.getPositionEyes(partialTicks);
        Vec3d look = player.getLook(partialTicks);
        Vec3d end = start.add(look.x * reach, look.y * reach, look.z * reach);

        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Point3d pos : candidates) {
            BlockPos blockPos = pos.toBlockPos();
            AxisAlignedBB bb = new AxisAlignedBB(blockPos);
            RayTraceResult intercept = bb.calculateIntercept(start, end);
            if (intercept == null || intercept.hitVec == null) {
                continue;
            }
            double distSq = intercept.hitVec.squareDistanceTo(start);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = blockPos;
            }
        }
        return best;
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
        if (tag != null) {
            return tag.getCompoundTag("bbw").getBoolean("breakModeActive");
        }
        return false;
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

    public static LinkedList<Point3d> getWandBlockPositionList(EntityPlayer player, ItemStack wandStack, BlockPos pos, EnumFacing side) {
        return getWandBlockPositionList(player, wandStack, pos, side, null);
    }

    public static LinkedList<Point3d> getWandBlockPositionList(EntityPlayer player, ItemStack wandStack, BlockPos pos, EnumFacing side, Vec3d hitVec) {
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

        if (hitVec == null) {
            return worker.getBlockPositionList(origin, side, wand.getMaxBlocks(wandStack), mode, faceLock, fluidMode);
        }

        ReplacementTriplet triplet = worker.getProperItemStack(worldShim, playerShim, origin, (float) hitVec.x, (float) hitVec.y, (float) hitVec.z);
        if (triplet != null && triplet.items != null && triplet.items.getItem() instanceof ItemBlock) {
            ItemStack sourceItems = triplet.items;
            int maxBlocks = Math.min(wand.getMaxBlocks(wandStack), playerShim.countItems(sourceItems));
            return worker.getBlockPositionList(origin, side, maxBlocks, mode, faceLock, fluidMode);
        }
        return new LinkedList<>();
    }

    public static LinkedList<Point3d> getWandBreakPositionList(EntityPlayer player, ItemStack wandStack, BlockPos pos, EnumFacing side) {
        LinkedList<Point3d> placementList = getWandBlockPositionList(player, wandStack, pos, side);
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

    public static void renderSelectionBox(List<Point3d> positions, EntityPlayer player, boolean renderAir, double partialTicks, float red, float green, float blue, float alpha) {
        if (positions.isEmpty()) return;
        double dx = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double dy = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double dz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        GlStateManager.disableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GL11.glLineWidth(2.5F);

        for (Point3d point : positions) {
            BlockPos pos = new BlockPos(point.x, point.y, point.z);
            IBlockState state = player.getEntityWorld().getBlockState(pos);
            AxisAlignedBB bb;
            if (state.getBlock().isAir(state, player.getEntityWorld(), pos)) {
                if (!renderAir) continue;
                bb = new AxisAlignedBB(pos).grow(0.002D).offset(-dx, -dy, -dz);
            } else {
                bb = state.getSelectedBoundingBox(player.getEntityWorld(), pos).grow(0.002D).offset(-dx, -dy, -dz);
            }
            RenderGlobal.drawSelectionBoundingBox(bb, red, green, blue, alpha);
        }

        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
    }
}
