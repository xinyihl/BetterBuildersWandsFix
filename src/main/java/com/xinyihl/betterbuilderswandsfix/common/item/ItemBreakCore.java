package com.xinyihl.betterbuilderswandsfix.common.item;

import com.xinyihl.betterbuilderswandsfix.Tags;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class ItemBreakCore extends Item {
    public ItemBreakCore() {
        setRegistryName(Tags.MOD_ID, "break_core");
        setTranslationKey(Tags.MOD_ID + ".break_core");
        setCreativeTab(CreativeTabs.MISC);
    }
}
