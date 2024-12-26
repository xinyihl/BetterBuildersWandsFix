package com.xinyihl.betterbuilderswandsfix.common.client;

import com.xinyihl.betterbuilderswandsfix.BetterBuildersWandsFix;
import com.xinyihl.betterbuilderswandsfix.common.network.PacketWandOops;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import portablejim.bbw.core.items.IWandItem;
import portablejim.bbw.shims.BasicPlayerShim;

public class KeyEvents {

    public KeyBinding keyBinding = new KeyBinding("bbw.key.oops", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, Keyboard.KEY_Z, "bbw.key.category");

    public KeyEvents() {
        ClientRegistry.registerKeyBinding(this.keyBinding);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void KeyEvent(InputEvent event) {
        if (keyBinding.isPressed()) {
            ItemStack currentItemstack = BasicPlayerShim.getHeldWandIfAny(Minecraft.getMinecraft().player);
            if(currentItemstack != null && currentItemstack.getItem() instanceof IWandItem){
                PacketWandOops packet = new PacketWandOops();
                BetterBuildersWandsFix.instance.networkWrapper.sendToServer(packet);
            }
        }
    }
}
