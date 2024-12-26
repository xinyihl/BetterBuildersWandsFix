package com.xinyihl.betterbuilderswandsfix;

import com.xinyihl.betterbuilderswandsfix.common.CommonProxy;
import com.xinyihl.betterbuilderswandsfix.common.network.PacketWandOops;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, dependencies = "required-after:betterbuilderswands@[0.13.2,);required-after:mixinbooter@[8.0,)")
public class BetterBuildersWandsFix {
    public SimpleNetworkWrapper networkWrapper;
    @Mod.Instance
    public static BetterBuildersWandsFix instance;
    @SidedProxy(clientSide = "com.xinyihl.betterbuilderswandsfix.common.ClientProxy", serverSide = "com.xinyihl.betterbuilderswandsfix.common.CommonProxy")
    public static CommonProxy proxy;
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        this.networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("bbwandsfix");
        this.networkWrapper.registerMessage(PacketWandOops.Handler.class, PacketWandOops.class, 0, Side.SERVER);
    }
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }
}
