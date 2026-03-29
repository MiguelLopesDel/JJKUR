package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.PartialRikaEntity;
import com.jujutsu.jujutsucraftaddon.procedures.DeerBuffedProcedure;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.AIActiveProcedure;
import net.mcreator.jujutsucraft.procedures.AIRoundDeerProcedure;
import net.mcreator.jujutsucraft.procedures.FollowEntityProcedure;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AIRoundDeerProcedure.class, priority = -10000)
public abstract class RoundDeerAIMixin {

    /**
     * @author Satushi
     * @reason Refactored for clean code and compatibility with Partial Rika and Deer Buffs.
     */
    @Inject(method = "execute", at = @At("HEAD"), remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        if (entity.isAlive() && entity instanceof LivingEntity _liv) {
            // 1. AI Base Procedures
            AIActiveProcedure.execute(world, x, y, z, entity);
            if (!(entity instanceof PartialRikaEntity)) {
                FollowEntityProcedure.execute(world, entity);
            }

            // 2. Strength and Resistance Buffs
            double strengthBase = entity.getPersistentData().getDouble("Strength");
            double num1 = 4.0 + Math.round(strengthBase * 0.5);
            
            double attackDamage = _liv.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? 
                                 _liv.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() : 0.0;
            double num2 = Math.round(Math.floor(Math.min((num1 + attackDamage * 3.0) / 4.0, 3.0)));

            if (!_liv.hasEffect(MobEffects.DAMAGE_BOOST) && !_liv.level().isClientSide()) {
                _liv.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, (int) num1, false, false));
            }

            int currentResistance = _liv.hasEffect(MobEffects.DAMAGE_RESISTANCE) ? 
                                   _liv.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() : 0;
            
            if (currentResistance < (int) num2 && !_liv.level().isClientSide()) {
                _liv.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, (int) num2, false, false));
            }

            // 3. Target Management
            LivingEntity target = (entity instanceof Mob _mob) ? _mob.getTarget() : null;
            if (target != null) {
                entity.getPersistentData().putDouble("cnt_x", entity.getPersistentData().getDouble("cnt_x") + 1.0);
                
                if (!target.getPersistentData().getBoolean("CursedSpirit")) {
                    if (!_liv.level().isClientSide()) {
                        _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 9, false, false));
                    }
                } else {
                    double friendNum = entity.getPersistentData().getDouble("friend_num");
                    if (friendNum != 0.0) {
                        for (Entity player : world.players()) {
                            if (player.getPersistentData().getDouble("friend_num") == friendNum) {
                                if (player.isShiftKeyDown() && !_liv.level().isClientSide()) {
                                    _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 9, false, false));
                                }
                                break;
                            }
                        }
                    }
                }
            } else {
                entity.getPersistentData().putDouble("cnt_x", 0.0);
            }

            // 4. RCT Maintenance
            if (_liv.hasEffect((MobEffect) JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get())) {
                double baseCE = entity.getPersistentData().getDouble("BaseCursePower");
                entity.getPersistentData().putDouble("BaseCursePower", Math.max(baseCE - 1.0, 0.0));
            }

            // 5. JJKUR Exclusive Logic
            DeerBuffedProcedure.execute(world, x, y, z, entity);
        }
    }
}
