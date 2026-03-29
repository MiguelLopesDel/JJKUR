package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.procedures.JackpotEffectStartedappliedProcedure;
import net.mcreator.jujutsucraft.procedures.JackpotOnEffectActiveTickProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = JackpotOnEffectActiveTickProcedure.class, priority = -10000)
public abstract class JackpotEffectActiveTickMixin {

    @Inject(method = "execute", at = @At("HEAD"), remap = false, cancellable = true)
    private static void execute(LevelAccessor world, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        if (entity.isAlive() && entity instanceof LivingEntity _liv) {
            double level = _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.JACKPOT.get())
                    ? _liv.getEffect((MobEffect) JujutsucraftModMobEffects.JACKPOT.get()).getAmplifier()
                    : 0.0;

            int zoneAmp = _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.ZONE.get())
                    ? _liv.getEffect((MobEffect) JujutsucraftModMobEffects.ZONE.get()).getAmplifier()
                    : 0;

            // Addon Logic: Keep triggering started applied procedure if zone is low
            if ((double) zoneAmp < 4.0 + level) {
                JackpotEffectStartedappliedProcedure.execute(world, entity);
            }

            entity.getPersistentData().putBoolean("PRESS_M", false);
            
            if (!_liv.level().isClientSide()) {
                // Addon Logic: Keep RCT level at 9 (instead of base 5.0 + level)
                _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get(), 2, 9, false, false));
                // v43 Logic: Add Speed
                _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 2, false, false));
            }

            if (world instanceof ServerLevel _level) {
                _level.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_CURSE_POWER_GREEN.get(), entity.getX(), entity.getY() + entity.getBbHeight() * 0.25, entity.getZ(), 2, entity.getBbWidth() * 0.25, entity.getBbHeight() * 0.25, entity.getBbWidth() * 0.25, 0.5);
            }

            if (Math.random() < 0.2 && world instanceof ServerLevel _level) {
                _level.sendParticles(ParticleTypes.NOTE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.75, entity.getZ(), 1, entity.getBbWidth() * 2.0, entity.getBbHeight() * 1.0, entity.getBbWidth() * 2.0, 0.5);
            }

            // v43 Logic: Explosion particles and sound when sprinting
            if (entity.isSprinting() && entity.onGround() && !entity.isShiftKeyDown() && !_liv.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                if (world instanceof ServerLevel _level) {
                    _level.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY(), entity.getZ(), 1, entity.getBbWidth() * 0.1, 0.0, entity.getBbWidth() * 0.1, 0.5);
                    _level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY(), entity.getZ(), 1, entity.getBbWidth() * 0.1, 0.0, entity.getBbWidth() * 0.1, 0.25);
                }

                if (world instanceof Level _level) {
                    SoundEvent explodeSound = (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode"));
                    if (!_level.isClientSide()) {
                        _level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), explodeSound, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    } else {
                        _level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), explodeSound, SoundSource.NEUTRAL, 1.0F, 1.0F, false);
                    }
                }
            }
        }
    }
}
