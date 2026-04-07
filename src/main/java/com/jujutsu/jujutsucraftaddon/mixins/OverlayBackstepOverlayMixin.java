package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.JJKUROverlayBackstepOverlay;
import net.mcreator.jujutsucraft.client.screens.OverlayBackstepOverlay;
import net.minecraftforge.client.event.RenderGuiEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OverlayBackstepOverlay.class, priority = -1000)
public abstract class OverlayBackstepOverlayMixin {

    @Inject(method = "eventHandler", at = @At("HEAD"), cancellable = true, remap = false)
    private static void eventHandler(RenderGuiEvent.Pre event, CallbackInfo ci) {
        JJKUROverlayBackstepOverlay.execute(event, ci);
    }
}
