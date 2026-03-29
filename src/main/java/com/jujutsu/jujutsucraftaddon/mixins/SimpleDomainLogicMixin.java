package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.YamatoToolInHandTickProcedure;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.ActiveTickConditionProcedure;
import net.mcreator.jujutsucraft.procedures.LogicSimpleDomainProcedure;
import net.mcreator.jujutsucraft.procedures.SimpleDomainEffectStartedappliedProcedure;
import net.mcreator.jujutsucraft.procedures.SimpleDomainOnEffectActiveTickProcedure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SimpleDomainOnEffectActiveTickProcedure.class, priority = -10000)
public abstract class SimpleDomainLogicMixin {

    /**
     * @author Satushi / Audit Correction
     * @reason Refactored for v43 with optimized entity loop, new technique IDs, and JJKUR's Yamato support.
     * FIXED: Restored Curse Power drain for non-Six Eyes users and fixed SimpleDomainLevel bypass logic.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())) {
            if (!LogicSimpleDomainProcedure.execute()) return;

            MobEffectInstance effect = _liv.getEffect((MobEffect) JujutsucraftModMobEffects.SIMPLE_DOMAIN.get());
            int duration = effect.getDuration();
            int amplifier = effect.getAmplifier();

            if (amplifier <= 0) return;

            JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftModVariables.PlayerVariables());
            JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftaddonModVariables.PlayerVariables());

            // 1. Periodic Effect Trigger & Energy Drain (Every 5 ticks)
            if (duration % 5 == 1) {
                // CORRECTED: Reintroduced energy drain for non-Six Eyes players
                if (entity instanceof Player) {
                    if (!_liv.hasEffect((MobEffect) JujutsucraftModMobEffects.SIX_EYES.get())) {
                        baseVars.PlayerCursePowerChange -= 1.0;
                        baseVars.syncPlayerVariables(entity);
                    }
                }
                SimpleDomainEffectStartedappliedProcedure.execute(world, x, y, z, entity);
            }

            // 2. Addon Specific: Yamato Support
            if (baseVars.PlayerCurseTechnique2 == 31.0 && "New Shadow Style: Defensive".equals(addonVars.Mode)) {
                YamatoToolInHandTickProcedure.execute(world, x, y, z, entity);
            }

            // 3. Condition Check (Simple Domain Level System)
            // CORRECTED: Condition check only applies if Level <= 1.0 (Addon Advantage)
            if (addonVars.SimpleDomainLevel <= 1.0) {
                if (!ActiveTickConditionProcedure.execute(entity)) {
                    _liv.removeEffect((MobEffect) JujutsucraftModMobEffects.SIMPLE_DOMAIN.get());
                    return;
                }
            }

            // 4. Particle Rendering (v43 Particle Parity)
            if (duration % 2 == 1) {
                double num1 = baseVars.PlayerCurseTechnique;
                double num2 = baseVars.PlayerCurseTechnique2;
                
                boolean showParticles = entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:can_use_hollow_wicker_basket"))) || 
                                       _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get()) ||
                                       (entity instanceof Player && (num1 == 1.0 || num2 == 1.0 || num1 == 7.0 || num2 == 7.0 || num1 == 12.0 || num2 == 12.0 || num1 == 24.0 || num2 == 24.0 || num1 == 44.0 || num2 == 44.0 || num1 == 45.0 || num2 == 45.0));

                if (showParticles) {
                    double yaw = Math.toRadians(entity.getYRot() + 90.0F);
                    double pitch = Math.toRadians(entity.getXRot());
                    double offset = -1.0 - entity.getBbWidth();
                    double px = entity.getX() + Math.cos(yaw) * Math.cos(pitch) * offset;
                    double py = entity.getY() + entity.getBbHeight() * 0.5;
                    double pz = entity.getZ() + Math.sin(yaw) * Math.cos(pitch) * offset;
                    
                    if (world instanceof ServerLevel _level) {
                        _level.sendParticles(JujutsucraftModParticleTypes.PARTICLE_HOLLOW_WICKER_BASKET.get(), px, py, pz, 0, 0.0, 0.0, 0.0, 0.0);
                    }
                }
            }

            // 5. Influence Radius Logic (v43 Optimized Loop)
            double radius = entity.getBbWidth() + 0.025 * duration;
            radius = Math.min(radius, entity.getPersistentData().getDouble("skill") == 3105.0 ? 16.0 : 4.0);
            
            Vec3 center = new Vec3(x, y, z);
            for (Entity target : world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(radius), e -> true)) {
                if (entity != target && (entity.getY() + entity.getBbHeight() >= target.getY()) && (entity.getY() <= target.getY() + target.getBbHeight())) {
                    if (target instanceof LivingEntity _target && !_target.level().isClientSide()) {
                        _target.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.SIMPLE_DOMAIN.get(), 5, 0, true, true));
                    }
                    break;
                }
            }
        }
    }
}
