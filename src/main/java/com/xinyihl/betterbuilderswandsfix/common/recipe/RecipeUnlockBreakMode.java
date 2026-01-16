package com.xinyihl.betterbuilderswandsfix.common.recipe;

import com.xinyihl.betterbuilderswandsfix.common.ModItems;
import com.xinyihl.betterbuilderswandsfix.common.Utils;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.IForgeRegistryEntry;
import portablejim.bbw.core.items.IWandItem;

public class RecipeUnlockBreakMode extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        boolean foundWand = false;
        boolean foundCore = false;
        boolean isUnlocked = false;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof IWandItem) {
                if (foundWand) {
                    return false;
                }
                foundWand = true;
                isUnlocked = Utils.isBreakModeUnlocked(stack);
            } else if (stack.getItem() == ModItems.BREAK_CORE) {
                if (foundCore) {
                    return false;
                }
                foundCore = true;
            } else {
                return false;
            }
        }
        return foundWand && foundCore && !isUnlocked;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack wandStack = ItemStack.EMPTY;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IWandItem) {
                wandStack = stack;
                break;
            }
        }
        if (wandStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = wandStack.copy();
        result.setCount(1);

        NBTTagCompound tag = result.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            result.setTagCompound(tag);
        }
        NBTTagCompound bbw = tag.getCompoundTag("bbw");
        if (!tag.hasKey("bbw", 10)) {
            tag.setTag("bbw", bbw);
        }
        bbw.setBoolean("breakModeUnlocked", true);

        return result;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                remaining.set(i, ForgeHooks.getContainerItem(stack));
            }
        }
        return remaining;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }
}
