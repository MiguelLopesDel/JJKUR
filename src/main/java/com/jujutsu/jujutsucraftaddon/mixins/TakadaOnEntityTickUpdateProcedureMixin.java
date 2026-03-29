package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.entity.JudgemanEntity;
import net.mcreator.jujutsucraft.entity.TakadaEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.GetEntityFromUUIDProcedure;
import net.mcreator.jujutsucraft.procedures.LogicOwnerExistProcedure;
import net.mcreator.jujutsucraft.procedures.PlayAnimationEntity2Procedure;
import net.mcreator.jujutsucraft.procedures.TakadaOnEntityTickUpdateProcedure;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = TakadaOnEntityTickUpdateProcedure.class, priority = -10000)
public abstract class TakadaOnEntityTickUpdateProcedureMixin {

    /**
     * @author Satushi / Audit Correction
     * @reason Refactored for v43. Restored Domain protection, skill survival checks, and animation resets.
     * FIXED: Restored burst particles in the unowned persistence block for 1:1 visual parity.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        if (LogicOwnerExistProcedure.execute(world, entity)) {
            Entity owner = GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().getString("OWNER_UUID"));
            
            if (owner instanceof LivingEntity _livOwner) {
                // 1. Generic Domain Expansion protection
                if (_livOwner.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                    if (entity instanceof JudgemanEntity && owner.getPersistentData().getDouble("skill") == 2719.0 
                        && owner.getPersistentData().getDouble("cnt3") >= 20.0 && owner.getPersistentData().getDouble("cnt1") > 0.0) {
                        
                        ((JudgemanEntity) entity).setAnimation("empty");
                        String anim = (owner.getPersistentData().getDouble("cnt2") >= 1.0) ? "judgement" : "judgement_light";
                        PlayAnimationEntity2Procedure.execute(entity, anim);

                        if (world instanceof ServerLevel _level) {
                            _level.sendParticles(ParticleTypes.SQUID_INK, entity.getX(), entity.getY(), entity.getZ(), 15, 2.0, 0.5, 2.0, 0.5);
                        }
                    }
                    return; 
                }

                // 2. Ambient Particles (v43 formula)
                if (world instanceof ServerLevel _level) {
                    int particleCount = (int) (10.0F + entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight() * 1.0F);
                    if (entity instanceof TakadaEntity) {
                        _level.sendParticles(ParticleTypes.END_ROD, x, y + entity.getBbHeight() * 0.5, z, particleCount, entity.getBbWidth() * 0.25, entity.getBbHeight() * 0.25, entity.getBbWidth() * 0.25, 0.0);
                    } else if (entity instanceof JudgemanEntity) {
                        _level.sendParticles(ParticleTypes.SQUID_INK, x, y + entity.getBbHeight() * 0.5, z, particleCount, entity.getBbWidth() * 0.25, entity.getBbHeight() * 0.25, entity.getBbWidth() * 0.25, 0.0);
                    }
                }

                // 3. Survival Logic (ShikigamiLevel Support)
                boolean shouldDiscard = true;
                if (entity.getPersistentData().getBoolean("ShikigamiLevel")) {
                    shouldDiscard = false;
                    if (entity instanceof JudgemanEntity) {
                        ((JudgemanEntity) entity).setAnimation("empty");
                        PlayAnimationEntity2Procedure.execute(entity, "judgement");
                        // RESTORED: Burst particles for ShikigamiLevel within owner block
                        if (world instanceof ServerLevel _level) {
                            _level.sendParticles(ParticleTypes.SQUID_INK, entity.getX(), entity.getY(), entity.getZ(), 15, 2.0, 0.5, 2.0, 0.5);
                        }
                    }
                }
                
                if (owner.getPersistentData().getDouble("skill") != 0.0) {
                    shouldDiscard = false;
                }

                if (shouldDiscard && !entity.level().isClientSide()) {
                    entity.discard();
                }
            } else if (!entity.level().isClientSide()) {
                entity.discard();
            }
        } 
        // 4. Persistence Feature (Unowned ShikigamiLevel - FIXED)
        else if (!entity.level().isClientSide()) {
            if (entity.getPersistentData().getBoolean("ShikigamiLevel")) {
                if (entity instanceof JudgemanEntity) {
                    ((JudgemanEntity) entity).setAnimation("empty");
                    PlayAnimationEntity2Procedure.execute(entity, "judgement");
                    
                    // RESTORED: Burst particles for unowned persistent Judgeman
                    if (world instanceof ServerLevel _level) {
                        _level.sendParticles(ParticleTypes.SQUID_INK, entity.getX(), entity.getY(), entity.getZ(), 15, 2.0, 0.5, 2.0, 0.5);
                    }

                    // Area Debuff Logic
                    final Vec3 center = new Vec3(x, y, z);
                    List<Entity> targets = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(30.0), e -> true);
                    for (Entity target : targets) {
                        if (!target.getStringUUID().equals(entity.getPersistentData().getString("OWNER_UUID"))) {
                            if (target instanceof LivingEntity _livTarget && !_livTarget.level().isClientSide()) {
                                _livTarget.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.UNSTABLE.get(), 100, 1, false, false));
                            }
                        }
                    }
                }
            } else {
                entity.discard();
            }
        }
    }
}
