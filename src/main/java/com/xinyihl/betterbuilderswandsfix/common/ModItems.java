package com.xinyihl.betterbuilderswandsfix.common;

import com.xinyihl.betterbuilderswandsfix.Tags;
import com.xinyihl.betterbuilderswandsfix.common.item.ItemBreakCore;
import com.xinyihl.betterbuilderswandsfix.common.recipe.RecipeUnlockBreakMode;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class ModItems {
    public static final Item BREAK_CORE = new ItemBreakCore();

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(BREAK_CORE);
    }

    @SubscribeEvent
    public static void registerModel(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(BREAK_CORE, 0, new ModelResourceLocation(Objects.requireNonNull(BREAK_CORE.getRegistryName()), "inventory"));
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new RecipeUnlockBreakMode().setRegistryName(Tags.MOD_ID, "unlock_break_mode"));
    }
}
