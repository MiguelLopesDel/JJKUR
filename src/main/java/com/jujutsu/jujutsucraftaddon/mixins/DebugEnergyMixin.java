package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.procedures.InfinityActiveTickProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InfinityActiveTickProcedure.class, remap = false)
public class DebugEnergyMixin {
    @Inject(method = "execute", at = @At("HEAD"))
    private static void onInfinityTick(LevelAccessor world, Entity entity, CallbackInfo ci) {
        if (entity instanceof LivingEntity living && !world.isClientSide()) {
            int duration = living.hasEffect((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get()) 
                           ? living.getEffect((MobEffect)JujutsucraftModMobEffects.INFINITY_EFFECT.get()).getDuration() 
                           : -1;
            
            if (duration % 10 == 5) {
                System.out.println("[JJKUR DEBUG] DRENO DISPARADO! Duração: " + duration + " | Tick: " + entity.tickCount);
            }
        }
    }
}
