package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.CalculateAttackProcedure;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CalculateAttackProcedure.class, priority = -10000)
public abstract class CalculateAttackMixin {

    @Inject(method = "execute", at = @At("HEAD"), remap = false, cancellable = true)
    private static void cancelIfQuake(LevelAccessor world, Entity entity, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity && livingEntity.hasEffect(JujutsucraftaddonModMobEffects.QUAKE.get())) {
            livingEntity.getPersistentData().putDouble("skill", 0.0);
            ci.cancel();
        }
    }

    @ModifyConstant(method = "execute", constant = @Constant(intValue = 32), remap = false)
    private static int buffAILoopLimit(int originalLimit) { //checkar depois
        return 255;
    }

    @Inject(method = "execute", at = @At("TAIL"), remap = false)
    private static void halveCooldown(LevelAccessor world, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isAlive()) return;

        if (livingEntity.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get())) {

            boolean alreadyHalved = livingEntity.getPersistentData().getBoolean("cooldown_halved");

            if (!alreadyHalved) {
                MobEffectInstance effect = livingEntity.getEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get());

                if (effect != null && effect.getDuration() > 2) {
                    int originalAmp = effect.getAmplifier();
//                    int halvedDuration = Math.max(1, effect.getDuration() / 2); depois testa

                    livingEntity.addEffect(new MobEffectInstance(
                            JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(),
                            effect.getDuration() / 2,
                            originalAmp,
                            false,
                            false
                    ));

                    livingEntity.getPersistentData().putBoolean("cooldown_halved", true);
                }
            }
        } else {
            livingEntity.getPersistentData().putBoolean("cooldown_halved", false);
        }
    }
}