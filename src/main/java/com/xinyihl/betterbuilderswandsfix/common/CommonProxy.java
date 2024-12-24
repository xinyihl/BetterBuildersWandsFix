package com.xinyihl.betterbuilderswandsfix.common;

import com.xinyihl.betterbuilderswandsfix.common.network.PacketWandOops;
import net.minecraftforge.fml.relauncher.Side;
import portablejim.bbw.BetterBuildersWandsMod;

public class CommonProxy {
    public void RegisterEvents() {
        BetterBuildersWandsMod.instance.networkWrapper.registerMessage(PacketWandOops.Handler.class, PacketWandOops.class, 0, Side.SERVER);
    }
}
