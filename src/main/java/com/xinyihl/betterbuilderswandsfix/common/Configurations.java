package com.xinyihl.betterbuilderswandsfix.common;

import com.xinyihl.betterbuilderswandsfix.Tags;
import net.minecraftforge.common.config.Config;

@Config(modid = Tags.MOD_ID, name = Tags.MOD_NAME)
public class Configurations {
    @Config.Comment("Bypass light updates when placing non-NBT blocks. \n放置无NBT方块时绕过光照更新。")
    public static boolean setBlockStateFastEnable = false;
    
    @Config.Comment("Maximum number of undo history entries. \n最大撤回历史记录数量。")
    @Config.RangeInt(min = 1, max = 10)
    public static int maxUndoHistory = 3;
}
