package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.PlayAnimationProcedure;
import net.mcreator.jujutsucraft.procedures.StartGuardProcedure;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoEntity;

@Mixin(value = StartGuardProcedure.class, priority = -10000)
public abstract class GuardEffectStartedappliedProcedureMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        if (entity instanceof LivingEntity _liv && !_liv.hasEffect(JujutsucraftaddonModMobEffects.SOKA_MONA.get())) {
            if (entity instanceof Player _player) {
                if (_player.isCreative() || _player.isSpectator()) return;
            }

            if (_liv.hasEffect((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) {
                return;
            }

            if (entity.getPersistentData().getDouble("skill") == 0.0) {
                if (_liv.hasEffect((MobEffect) JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get()) && entity.getPersistentData().getBoolean("PRESS_M")) {
                    return;
                }

                if (_liv.hasEffect((MobEffect) JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get())) {
                    return;
                }

                double animation_num = 0.0;
                double tick = 0.0;

                if (!_liv.hasEffect((MobEffect) JujutsucraftModMobEffects.DAMAGE_EFFECT.get()) && !_liv.hasEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_GUARD.get())) {
                    if (_liv.hasEffect((MobEffect) JujutsucraftModMobEffects.PRAYER_SONG.get())) {
                        tick = 15.0;
                        animation_num = Math.round(-15.0 + Math.ceil(Math.random() * 4.0));
                    } else {
                        tick = 10.0;
                    }

                    if (animation_num != 0.0) {
                        if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                            _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(animation_num);
                        }
                        if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get())) {
                            _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get()).setBaseValue(0.0);
                        }

                        PlayAnimationProcedure.execute(world, entity);
                    }

                    if (!_liv.level().isClientSide()) {
                        _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.GUARD.get(), (int) tick, 1, false, false));
                    }
                }

                if (!_liv.level().isClientSide()) {
                    _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_GUARD.get(), 20, 0, false, false));
                    _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.GUARD.get(), 10, 0, false, false));
                }
            }
        }
    }
}
