package com.xinyihl.betterbuilderswandsfix.common.client;

import com.xinyihl.betterbuilderswandsfix.common.network.PacketWandOops;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import portablejim.bbw.BetterBuildersWandsMod;

public class KeyEvents {

    public KeyBinding keyBinding = new KeyBinding("bbw.key.oops", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, Keyboard.KEY_Z, "bbw.key.category");

    public KeyEvents() {
        ClientRegistry.registerKeyBinding(this.keyBinding);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void KeyEvent(InputEvent event) {
        if (keyBinding.isPressed()) {
            PacketWandOops packet = new PacketWandOops();
            BetterBuildersWandsMod.instance.networkWrapper.sendToServer(packet);
        }
    }
}
