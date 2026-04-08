package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.EffectCharactorProcedure", remap = false)
public class HWBProtectionMixin {

    @Inject(method = "execute", at = @At("HEAD"), remap = false, cancellable = true)
    private static void onEffectApplied(LevelAccessor world, Entity entity, Entity entityiterator, CallbackInfo ci) {
        if (entityiterator != null) {
            entityiterator.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
                if (vars.hwb_active) {
                    ci.cancel();
                }
            });
        }
    }
}
