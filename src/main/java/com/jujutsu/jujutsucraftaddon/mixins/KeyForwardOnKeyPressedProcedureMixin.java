package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.KeyForwardOnKeyPressedProcedure;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyForwardOnKeyPressedProcedure.class, priority = -10000)
public abstract class KeyForwardOnKeyPressedProcedureMixin {

    /**
     * @author Satushi
     * @reason Gives Dashing To Players when Pressing Forward
     */
    @Inject(method = "execute", at = @At("TAIL"), remap = false)
    private static void execute(Entity entity, CallbackInfo ci) {
        if (entity == null) return;

        JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .orElse(new JujutsucraftaddonModVariables.PlayerVariables());

        if (addonVars.Dash) {
            double dashCount = entity.getPersistentData().getDouble("dash") + 1;
            entity.getPersistentData().putDouble("dash", dashCount);

            if (dashCount == 2 && entity instanceof LivingEntity _liv) {
                // Check for restrictions
                boolean hasRestriction = _liv.hasEffect(JujutsucraftaddonModMobEffects.DASH_COOLDOWN.get())
                        || _liv.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get())
                        || _liv.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                        || _liv.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get());

                if (!hasRestriction) {
                    JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                            .orElse(new JujutsucraftModVariables.PlayerVariables());

                    int duration = (baseVars.PlayerCurseTechnique2 == -1) ? 60 : 1;

                    if (!_liv.level().isClientSide()) {
                        _liv.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.DASH.get(), duration, 1, false, false));
                    }
                }
            }
        }
    }
}
