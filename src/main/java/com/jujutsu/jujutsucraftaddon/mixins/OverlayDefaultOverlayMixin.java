package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.JJKUROverlayDefaultOverlay;
import net.mcreator.jujutsucraft.client.screens.OverlayDefaultOverlay;
import net.minecraftforge.client.event.RenderGuiEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OverlayDefaultOverlay.class, priority = -1000)
public abstract class OverlayDefaultOverlayMixin {

    @Inject(method = "eventHandler", at = @At("HEAD"), cancellable = true, remap = false)
    private static void eventHandler(RenderGuiEvent.Pre event, CallbackInfo ci) {
        JJKUROverlayDefaultOverlay.onExecute(event, ci);
    }
}
