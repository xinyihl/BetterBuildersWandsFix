package com.xinyihl.betterbuilderswandsfix.common;

import com.xinyihl.betterbuilderswandsfix.Tags;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
@Config(modid = Tags.MOD_ID, name = Tags.MOD_NAME)
public class Configurations {
    @Config.Comment("Bypass light updates when placing non-NBT blocks. \n放置无NBT方块时绕过光照更新。")
    public static boolean setBlockStateFastEnable = false;
    
    @Config.Comment("Maximum number of undo history entries. \n最大撤回历史记录数量。")
    @Config.RangeInt(min = 1, max = 10)
    public static int maxUndoHistory = 3;

    @Config.Comment("Enable break mode. \n启用破坏模式。")
    public static boolean breakModeEnabled = true;

    @SubscribeEvent
    public static void onConfigChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(Tags.MOD_ID)) {
            ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
        }
    }
}
