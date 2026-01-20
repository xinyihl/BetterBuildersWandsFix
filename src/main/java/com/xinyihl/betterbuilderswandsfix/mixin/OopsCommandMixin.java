package com.xinyihl.betterbuilderswandsfix.mixin;

import com.xinyihl.betterbuilderswandsfix.common.Utils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import portablejim.bbw.core.OopsCommand;

@Mixin(value = OopsCommand.class, remap = false)
public abstract class OopsCommandMixin {
    @Inject(
            method = "execute",
            at = @At("HEAD"),
            remap = true,
            cancellable = true
    )
    public void injected(MinecraftServer server, ICommandSender sender, String[] arguments, CallbackInfo ci) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            throw new WrongUsageException("bbw.chat.error.bot");
        } else {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            Utils.undoOperation(player);
        }
        ci.cancel();
    }
}
