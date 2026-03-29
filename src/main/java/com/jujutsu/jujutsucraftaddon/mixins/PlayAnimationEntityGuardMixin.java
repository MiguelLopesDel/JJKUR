package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.GetEntityAnimationProcedure;
import net.mcreator.jujutsucraft.procedures.LogicSwordProcedure;
import net.mcreator.jujutsucraft.procedures.PlayAnimationEntityGuardProcedure;
import net.mcreator.jujutsucraft.procedures.PlayAnimationProcedure;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
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

@Mixin(value = PlayAnimationEntityGuardProcedure.class, priority = -10000)
public abstract class PlayAnimationEntityGuardMixin {

    /**
     * @author Satushi
     * @reason Refactored to match v43 blocking animations and side-aware logic, respecting entity types
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, Entity entity, Entity entityiterator, CallbackInfo ci) {
        ci.cancel();
        if (entity == null || entityiterator == null) return;

        if (entityiterator instanceof LivingEntity _liv && _liv.isAlive()) {
            // 1. Check if technique is active (prevents block animation)
            if (_liv.hasEffect((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) {
                return;
            }

            // 2. Ensure no other skill is running
            if (_liv.getPersistentData().getDouble("skill") == 0.0) {
                boolean sword = LogicSwordProcedure.execute(_liv);

                // v43 Neutralization Check
                if (sword && entity.getPersistentData().getDouble("skill") != 0.0 && !entity.getPersistentData().getBoolean("attack")) {
                    int neutralizationAmp = _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get()) ? 
                                            _liv.getEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get()).getAmplifier() : 0;
                    if (neutralizationAmp > 10) {
                        sword = false;
                    }
                }

                // 3. Handle Swing (Applies to all living entities)
                _liv.swing(InteractionHand.MAIN_HAND, true);

                // 4. Animation Logic (Restricted to Player or GeoEntity to avoid overhead/bugs)
                if (_liv instanceof Player || _liv instanceof GeoEntity) {
                    double animationValue = 0.0;
                    if (sword) {
                        if (_liv instanceof GeoEntity) {
                            String side = GetEntityAnimationProcedure.execute(_liv);
                            if (side.contains("_right")) {
                                animationValue = 4.0;
                            } else if (side.contains("_left")) {
                                animationValue = 1.0;
                            } else {
                                animationValue = (double) Mth.nextInt(RandomSource.create(), 1, 6);
                            }
                        } else {
                            animationValue = (double) Mth.nextInt(RandomSource.create(), 1, 6);
                        }

                        // Set Animation Attributes for Sword Block
                        setAttributeValue(_liv, JujutsucraftModAttributes.ANIMATION_1.get(), 100.0);
                        setAttributeValue(_liv, JujutsucraftModAttributes.ANIMATION_2.get(), animationValue);
                    } else {
                        // Set Animation Attributes for Arm Guard
                        setAttributeValue(_liv, JujutsucraftModAttributes.ANIMATION_1.get(), -9.0);
                        setAttributeValue(_liv, JujutsucraftModAttributes.ANIMATION_2.get(), 0.0);
                    }

                    PlayAnimationProcedure.execute(world, _liv);
                }
            }
        }
    }

    private static void setAttributeValue(LivingEntity entity, Attribute attribute, double value) {
        if (entity.getAttributes().hasAttribute(attribute)) {
            entity.getAttribute(attribute).setBaseValue(value);
        }
    }
}
