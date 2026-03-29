package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.LogicAttackProcedure;
import net.mcreator.jujutsucraft.procedures.PlayAnimationProcedure;
import net.mcreator.jujutsucraft.procedures.PrayerSongOnEffectActiveTickProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PrayerSongOnEffectActiveTickProcedure.class, priority = -10000)
public abstract class PrayerSongOnEffectActiveTickProcedureMixin {

    /**
     * @author Satushi
     * @reason Refactored for v43 with optimized entity search while preserving JJKUR's progressive weakness and debuffs.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        if (entity.isAlive() && entity instanceof LivingEntity _liv) {
            MobEffect prayerSong = (MobEffect) JujutsucraftModMobEffects.PRAYER_SONG.get();
            if (!_liv.hasEffect(prayerSong)) return;

            int duration = _liv.getEffect(prayerSong).getDuration();
            int tick = duration;

            // 1. Maintain Zone Effect
            if (!_liv.hasEffect((MobEffect) JujutsucraftModMobEffects.ZONE.get()) && !_liv.level().isClientSide()) {
                _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.ZONE.get(), 5, 0, false, false));
            }

            // 2. Sound and Animation Logic (On Ground)
            if (entity.onGround()) {
                boolean isUsingTech = _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get());
                boolean isGuarding = _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.GUARD.get());

                if (!isUsingTech && !isGuarding) {
                    // Periodic Sound (3 ticks)
                    if (tick % 3 == 0 && world instanceof Level _level) {
                        SoundEvent glassSound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.glass.fall"));
                        if (!_level.isClientSide()) {
                            _level.playSound(null, BlockPos.containing(x, y, z), glassSound, SoundSource.NEUTRAL, 2.0F, 2.0F);
                        } else {
                            _level.playLocalSound(x, y, z, glassSound, SoundSource.NEUTRAL, 2.0F, 2.0F, false);
                        }
                    }

                    // Periodic Animation (5 ticks)
                    if (tick % 5 == 0) {
                        if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                            _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(-15.0);
                        }
                        PlayAnimationProcedure.execute(world, entity);
                    }
                }
            }

            // 3. Entity Debuff Logic (v43 Optimized Loop)
            if (tick % 5 == 0) {
                Vec3 center = new Vec3(x, y, z);
                AABB range = new AABB(center, center).inflate(12.0);
                
                for (Entity target : world.getEntitiesOfClass(Entity.class, range, e -> true)) {
                    if (entity != target && target instanceof LivingEntity _target && LogicAttackProcedure.execute(world, entity, target)) {
                        if (!_target.level().isClientSide()) {
                            // Progressive Weakness (JJKUR Feature)
                            MobEffectInstance currentWeakness = _target.getEffect(MobEffects.WEAKNESS);
                            int nextAmplifier = 0;
                            if (currentWeakness != null && currentWeakness.getAmplifier() < 50) {
                                nextAmplifier = currentWeakness.getAmplifier() + 1;
                            }

                            if (Math.random() < 0.025) { // 1/40
                                _target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, nextAmplifier));
                            }

                            // JJKUR Additional Debuffs
                            _target.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME.get(), 30, 0));
                            _target.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.UNSTABLE.get(), 30, 0));

                            // Concentration Break (1/200)
                            if (Math.random() < 0.005) {
                                _target.removeEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get());
                            }
                        }
                    }
                }
            }

            // 4. Cancellation Logic
            boolean isUnstable = _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.UNSTABLE.get());
            boolean isAmpActive = _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get());

            if (isUnstable || isAmpActive) {
                _liv.removeEffect(prayerSong);
            }
        } else if (entity instanceof LivingEntity _liv) {
            _liv.removeEffect((MobEffect) JujutsucraftModMobEffects.PRAYER_SONG.get());
        }
    }
}
