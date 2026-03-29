package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.ItadoriShinjukuEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.BFMasteryProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.BlackFlashNerfedProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.BlackFlashedProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.ItadoriClan2Procedure;
import net.mcreator.jujutsucraft.entity.*;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.RangeAttackProcedure;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RangeAttackProcedure.class, priority = -10000)
public abstract class RangeAttackProcedureMixin {

    /**
     * @author Satushi
     * @reason Refactored for v43 to maintain JJKUR's custom Black Flash chances and Mastery system.
     * FIXED: Removed lambda usage to ensure return values correctly affect the base mod logic.
     */
    @ModifyConstant(method = "execute", constant = @Constant(doubleValue = 0.998), remap = false)
    private static double modifyBlackFlashConstant(double constant, LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return constant;

        boolean gojoSatoruTarget = false;
        boolean sukunaTarget = false;

        // 1. Entity Type Check (Itadori Special)
        if (entity instanceof ItadoriShinjukuEntity || entity instanceof ItadoriYujiShinjukuEntity) {
            float healthRatio = ((LivingEntity) entity).getHealth() / ((LivingEntity) entity).getMaxHealth();
            if (healthRatio <= 0.25) { // 1/4 HP
                if (Math.random() < 0.05) return 0.001; // 1/20
            } else if (healthRatio <= 0.33) { // 1/3 HP
                if (Math.random() < 0.04) return 0.001; // 1/25
            } else {
                if (Math.random() < 0.01) return 0.001; // 1/100
            }
        }

        // 2. Target Identification
        LivingEntity target = null;
        if (entity instanceof Mob _mob) {
            target = _mob.getTarget();
        }

        if (target != null) {
            if (target instanceof GojoSatoruEntity || target instanceof GojoSatoruSchoolDaysEntity || 
                target.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables()).PlayerCurseTechnique == 2) {
                gojoSatoruTarget = true;
            }
            if (target instanceof SukunaFushiguroEntity || target.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) {
                sukunaTarget = true;
            }
        }

        // 3. Boss and Special Characters Probabilities
        if (entity instanceof SukunaPerfectEntity || entity instanceof GojoSatoruEntity || 
            entity instanceof OkkotsuYutaCullingGameEntity || entity instanceof NanamiKentoEntity || 
            entity instanceof GojoSatoruSchoolDaysEntity || entity instanceof SukunaEntity || 
            entity instanceof SukunaFushiguroEntity) {
            
            float healthRatio = ((LivingEntity) entity).getHealth() / ((LivingEntity) entity).getMaxHealth();
            
            if (healthRatio <= 0.5) {
                if (entity instanceof SukunaPerfectEntity) {
                    if (Math.random() < 0.05) return 0.001;
                } else if (entity instanceof SukunaFushiguroEntity _fukuna && _fukuna.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode)) {
                    if (Math.random() < 0.05) return 0.001;
                } else if (entity instanceof SukunaFushiguroEntity) {
                    if (target != null) {
                        if (gojoSatoruTarget) return constant; // Against Gojo, use normal
                        if (Math.random() < 0.02) return 0.001;
                    }
                } else if (entity instanceof GojoSatoruEntity) {
                    if (sukunaTarget) return constant;
                    if (Math.random() < 0.01) return 0.001;
                } else if (entity instanceof OkkotsuYutaCullingGameEntity) {
                    if (Math.random() < 0.0033) return 0.001;
                } else {
                    if (Math.random() < 0.02) return 0.001;
                }
            } else {
                if (entity instanceof GojoSatoruEntity) {
                    if (target != null && !sukunaTarget && Math.random() < 0.005) return 0.001;
                } else if (entity instanceof SukunaFushiguroEntity) {
                    if (target != null && !gojoSatoruTarget && Math.random() < 0.0066) return 0.001;
                } else if (entity instanceof NanamiKentoEntity) {
                    if (Math.random() < 0.01) return 0.001;
                } else if (entity instanceof OkkotsuYutaCullingGameEntity) {
                    if (Math.random() < 0.0025) return 0.001;
                } else if (Math.random() < 0.0066) {
                    return 0.001;
                }
            }
        }

        // 4. Custom Addon BFChance Variable - FIXED RETURN
        ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (typeKey != null && !typeKey.toString().startsWith("jujutsucraft")) {
            JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftaddonModVariables.PlayerVariables());
            
            if (Math.random() < (addonVars.BFChance / 1000.0)) {
                boolean reworked = world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_BLACK_FLASH_REWORKED);
                boolean fatigue = (entity instanceof LivingEntity _liv) && _liv.hasEffect(JujutsucraftaddonModMobEffects.FATIGUE_BLACK_FLASH.get());
                
                if (reworked) {
                    if (!fatigue && entity.getPersistentData().getDouble("cnt_bf") >= 50.0) {
                        return 0.001;
                    }
                } else if (!fatigue) {
                    return 0.001;
                }
            }
        }

        return constant;
    }

    @Inject(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;", ordinal = 4), remap = false)
    private static void injectBlackFlashProcedures(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity == null) return;

        ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (typeKey == null) return;

        if (typeKey.toString().startsWith("jujutsucraft")) {
            BlackFlashedProcedure.execute(world, x, y, z, entity);
            return;
        }

        JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .orElse(new JujutsucraftaddonModVariables.PlayerVariables());
        
        boolean reworked = world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_BLACK_FLASH_REWORKED);
        boolean hasFatigue = ((LivingEntity)entity).hasEffect(JujutsucraftaddonModMobEffects.FATIGUE_BLACK_FLASH.get());
        boolean isTimerReady = addonVars.Timer1 == 1;

        if (reworked) {
            if (!hasFatigue && entity.getPersistentData().getDouble("cnt_bf") >= 50.0 && isTimerReady) {
                executeBFSystems(world, x, y, z, entity, addonVars);
            }
        } else {
            if (!hasFatigue && isTimerReady) {
                executeBFSystems(world, x, y, z, entity, addonVars);
            }
            entity.getPersistentData().putDouble("cnt_bf", 0);
        }
    }

    private static void executeBFSystems(LevelAccessor world, double x, double y, double z, Entity entity, JujutsucraftaddonModVariables.PlayerVariables addonVars) {
        BlackFlashedProcedure.execute(world, x, y, z, entity);
        BFMasteryProcedure.execute(world, entity);
        entity.getPersistentData().putDouble("cnt_bf", 0);
        ItadoriClan2Procedure.execute(world, entity);
        BlackFlashNerfedProcedure.execute(world, entity);

        if (addonVars.ImpactFramesVariable == 0) {
            if (!((LivingEntity)entity).hasEffect(JujutsucraftaddonModMobEffects.OUT_LINER.get())) {
                if (!entity.level().isClientSide()) {
                    ((LivingEntity)entity).addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.OUT_LINER.get(), Mth.nextInt(RandomSource.create(), 10, 15), 1, false, false));
                }
            }
        }
    }
}
