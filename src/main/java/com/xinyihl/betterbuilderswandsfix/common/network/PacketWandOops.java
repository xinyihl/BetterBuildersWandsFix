package com.xinyihl.betterbuilderswandsfix.common.network;

import com.xinyihl.betterbuilderswandsfix.common.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

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
            player.getServerWorld().addScheduledTask(()-> Utils.undoPlaceBlocks(player));
            return null;
        }
    }
}
