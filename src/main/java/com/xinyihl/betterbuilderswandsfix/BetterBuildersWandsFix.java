package com.xinyihl.betterbuilderswandsfix;

import com.xinyihl.betterbuilderswandsfix.common.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, dependencies = "required-after:betterbuilderswands@[0.13.2,);required-after:mixinbooter@[8.0,)")
public class BetterBuildersWandsFix {
    @SidedProxy(clientSide = "com.xinyihl.betterbuilderswandsfix.common.ClientProxy", serverSide = "com.xinyihl.betterbuilderswandsfix.common.CommonProxy")
    public static CommonProxy PROXY;
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        PROXY.RegisterEvents();
    }
}
