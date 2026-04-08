package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.ModernJujutsuDustFix;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.slyrienmodern.com.util.SmoothChasingValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.slyrienmodern.com.client.hud.TempStatusHudRenderer", remap = false)
public class ModernJujutsuDustFixMixin {

    @Redirect(
            method = "renderContent",
            at = @At(value = "INVOKE", target = "Lnet/slyrienmodern/com/util/SmoothChasingValue;target(F)Lnet/slyrienmodern/com/util/SmoothChasingValue;"),
            remap = false
    )
    private static SmoothChasingValue redirect(SmoothChasingValue instance, float target, GuiGraphics gui, Entity player, float delta, boolean isEditing) {
        return ModernJujutsuDustFix.redirectDustTarget(instance, target, gui, player, delta, isEditing);
    }
}
