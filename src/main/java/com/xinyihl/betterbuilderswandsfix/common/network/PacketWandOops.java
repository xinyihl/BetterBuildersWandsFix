package com.xinyihl.betterbuilderswandsfix.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import portablejim.bbw.basics.Point3d;
import portablejim.bbw.core.items.IWandItem;
import portablejim.bbw.shims.BasicPlayerShim;

import java.util.ArrayList;

public class PacketWandOops implements IMessage {
    @Override
    public void fromBytes(ByteBuf buf) {

    }

    @Override
    public void toBytes(ByteBuf buf) {

    }

    public static class Handler implements IMessageHandler<PacketWandOops, IMessage> {
        @Override
        public IMessage onMessage(PacketWandOops packetWandOops, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(()->{
                ItemStack currentItemstack = BasicPlayerShim.getHeldWandIfAny(player);
                if (currentItemstack != null && currentItemstack.getItem() instanceof IWandItem) {
                    NBTTagCompound tagComponent = currentItemstack.getTagCompound();
                    if (tagComponent != null && tagComponent.hasKey("bbw", 10) && tagComponent.getCompoundTag("bbw").hasKey("lastPlaced", 11)) {
                        NBTTagCompound bbwCompound = tagComponent.getCompoundTag("bbw");
                        ArrayList<Point3d> pointList = this.unpackNbt(bbwCompound.getIntArray("lastPlaced"));
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
                        // throw new WrongUsageException("bbw.chat.error.noundo", new Object[0]);
                    }
                } else {
                    player.sendMessage(new TextComponentTranslation("bbw.chat.error.nowand"));
                    //throw new WrongUsageException("bbw.chat.error.nowand", new Object[0]);
                }
            });
            return null;
        }

        protected ArrayList<Point3d> unpackNbt(int[] placedBlocks) {
            ArrayList<Point3d> output = new ArrayList<>();
            int countPoints = placedBlocks.length / 3;

            for (int i = 0; i < countPoints * 3; i += 3) {
                output.add(new Point3d(placedBlocks[i], placedBlocks[i + 1], placedBlocks[i + 2]));
            }

            return output;
        }
    }

}
