package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.procedures.PlayAnimationEntityProcedure;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayAnimationEntityProcedure.class, priority = -1000)
public abstract class PlayAnimationEntity {

    /**
     * @author Satushi
     * @reason Redirects animation handling to Addon's procedure to include custom animations
     */
    @Inject(method = "onEntityAttacked", at = @At("HEAD"), cancellable = true, remap = false)
    private static void execute(LivingAttackEvent event, CallbackInfo ci) {
        if (event != null && event.getEntity() != null) {
            ci.cancel();
            // Delegate to Addon Procedure
            com.jujutsu.jujutsucraftaddon.procedures.PlayAnimationEntity.execute(
                event, 
                event.getEntity().level(), 
                event.getSource(), 
                event.getEntity()
            );
        }
    }
}
