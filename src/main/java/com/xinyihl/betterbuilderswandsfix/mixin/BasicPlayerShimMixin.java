package com.xinyihl.betterbuilderswandsfix.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import portablejim.bbw.BetterBuildersWandsMod;
import portablejim.bbw.api.IContainerHandlerSpecial;
import portablejim.bbw.containers.ContainerManager;
import portablejim.bbw.shims.BasicPlayerShim;

import java.util.Map;

@Mixin(value = BasicPlayerShim.class, remap = false)
public abstract class BasicPlayerShimMixin {

    @Shadow
    private EntityPlayer player;

    /**
     * @author xin_yi_hl
     * @reason 消耗副手物品
     */
    @Overwrite
    public int countItems(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty() || player.inventory == null) {
            return 0;
        }
        int total = 0;
        ContainerManager containerManager = BetterBuildersWandsMod.instance.containerManager;
        Map<IContainerHandlerSpecial, Object> containerState = containerManager.initCount(player);

        // 计算背包内匹配物品数量
        for (ItemStack inventoryStack : player.inventory.mainInventory) {
            if (inventoryStack.isEmpty()) continue;
            if (itemStack.isItemEqual(inventoryStack) && ItemStack.areItemStackTagsEqual(itemStack, inventoryStack)) {
                total += Math.max(0, inventoryStack.getCount());
            } else {
                int amount = containerManager.countItems(containerState, player, itemStack, inventoryStack);
                if (amount == Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
                total += amount;
            }
        }

        // 计算副手内匹配物品数量
        ItemStack offhandStack = player.getHeldItemOffhand();
        if (!offhandStack.isEmpty() && itemStack.isItemEqual(offhandStack) && ItemStack.areItemStackTagsEqual(itemStack, offhandStack)) {
            total += Math.max(0, offhandStack.getCount());
        }

        total += containerManager.finalCount(containerState);
        return itemStack.getCount() > 0 ? total / itemStack.getCount() : 0;
    }

    /**
     * @author xin_yi_hl
     * @reason 消耗副手物品
     */
    @Overwrite
    public ItemStack useItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty() || player.inventory == null) {
            return null;
        }

        ContainerManager containerManager = BetterBuildersWandsMod.instance.containerManager;
        Map<IContainerHandlerSpecial, Object> containerState = containerManager.initUse(player);
        int toUse = itemStack.getCount();

        // 优先消耗主背包物品 & 背包内容器中的物品（反向遍历）
        for (int i = player.inventory.mainInventory.size() - 1; i >= 0; i--) {
            ItemStack inventoryStack = player.inventory.mainInventory.get(i);
            if (inventoryStack.isEmpty()) continue;
            if (itemStack.isItemEqual(inventoryStack) && ItemStack.areItemStackTagsEqual(itemStack, inventoryStack)) {
                ItemStack inventoryStackBackUp = inventoryStack.copy();
                int consumeAmount = Math.min(inventoryStack.getCount(), toUse);
                inventoryStack.shrink(consumeAmount);
                toUse -= consumeAmount;

                if (inventoryStack.isEmpty()) {
                    player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
                }
                player.inventoryContainer.detectAndSendChanges();

                if (toUse <= 0) {
                    return inventoryStackBackUp;
                }
            } else {
                if (toUse > 0) {
                    toUse = containerManager.useItems(containerState, player, itemStack, inventoryStack, toUse);
                    toUse = containerManager.finalUse(containerState, toUse);
                }
                if (toUse <= 0) {
                    return itemStack;
                }
            }

        }

        // 消耗副手物品（当主背包不足时）
        if (toUse > 0) {
            ItemStack offhandStack = player.getHeldItemOffhand();
            if (!offhandStack.isEmpty() && itemStack.isItemEqual(offhandStack) && ItemStack.areItemStackTagsEqual(itemStack, offhandStack)) {
                ItemStack inventoryStackBackUp = offhandStack.copy();
                int consumeAmount = Math.min(offhandStack.getCount(), toUse);
                offhandStack.shrink(consumeAmount);
                toUse -= consumeAmount;

                if (offhandStack.isEmpty()) {
                    player.inventory.offHandInventory.set(0, ItemStack.EMPTY);
                }
                player.inventoryContainer.detectAndSendChanges();

                if (toUse <= 0) {
                    return inventoryStackBackUp;
                }
            }
        }
        return null;
    }

}
