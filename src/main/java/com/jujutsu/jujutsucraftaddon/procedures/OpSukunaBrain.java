package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.util.DomainMasterySystem;
import com.jujutsu.jujutsucraftaddon.util.OpSukunaBrainMemoryHolder;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.entity.SukunaFushiguroEntity;
import net.mcreator.jujutsucraft.entity.SukunaPerfectEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.CalculateAttackProcedure;
import net.mcreator.jujutsucraft.procedures.GetDistanceProcedure;
import net.mcreator.jujutsucraft.procedures.LogicConfilmDomainProcedure;
import net.mcreator.jujutsucraft.procedures.LogicCooldownCombatProcedure;
import net.mcreator.jujutsucraft.procedures.LogicStartPassiveProcedure;
import net.mcreator.jujutsucraft.procedures.LogicStartProcedure;
import net.mcreator.jujutsucraft.procedures.ResetCounterProcedure;
import net.mcreator.jujutsucraft.procedures.ReturnShadowProcedure;
import net.mcreator.jujutsucraft.procedures.StartGuardProcedure;
import net.mcreator.jujutsucraft.procedures.WhenBackStepProcedure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OpSukunaBrain {
    private static final String SAVE_KEY = "JJKUR_OP_AI_RAM";
    private static final double FAST_DISMANTLE = 105.0;
    private static final double CLEAVE = 106.0;
    private static final double OPEN = 107.0;
    private static final double DOMAIN = 20.0;
    private static final double TEN_SHADOWS_UTILITY = 612.0;
    private static final double AGITO = 617.0;
    private static final double MAHORAGA = 618.0;
    private static final double TEN_SHADOWS_DOMAIN = 620.0;
    private static final double PASSIVE_FAST = 111.0;
    private static final double PASSIVE_CLOSE = 112.0;
    private static final double PASSIVE_ANTI_RANGE = 113.0;

    private OpSukunaBrain() {
    }

    public static boolean tryExecute(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, LivingEntity target, CompoundTag nbt) {
        if (!isSupportedSukuna(sukuna)) {
            return false;
        }

        BrainMemory memory = memory(sukuna);
        if (target == null || !target.isAlive() || nbt.getDouble("cnt_target") <= 6.0) {
            memory.lastHealth = sukuna.getHealth();
            return false;
        }

        Snapshot s = Snapshot.capture(world, x, y, z, sukuna, target, nbt, memory);
        memory.observe(s);

        if (nbt.getDouble("skill") != 0.0) {
            return true;
        }

        ResetCounterProcedure.execute(sukuna);
        if (s.selfDomain && s.distance < 48.0 && !sukuna.level().isClientSide()) {
            sukuna.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20, 0, false, false));
        }

        Action best = chooseBestAction(s);
        best.execute(world, x, y, z, sukuna, nbt, s);
        memory.lastAction = best.name;
        memory.lastActionTarget = targetKey(target);
        return true;
    }

    public static CompoundTag save(LivingEntity sukuna) {
        return memory(sukuna).save();
    }

    public static void load(LivingEntity sukuna, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return;
        }
        if (sukuna instanceof OpSukunaBrainMemoryHolder holder) {
            holder.jjkur$setOpSukunaBrainMemory(BrainMemory.load(tag));
        }
    }

    public static String saveKey() {
        return SAVE_KEY;
    }

    private static BrainMemory memory(LivingEntity sukuna) {
        if (sukuna instanceof OpSukunaBrainMemoryHolder holder) {
            return holder.jjkur$getOpSukunaBrainMemory();
        }
        return new BrainMemory();
    }

    private static boolean isSupportedSukuna(LivingEntity entity) {
        return entity instanceof SukunaPerfectEntity
                || (entity instanceof SukunaFushiguroEntity sf && sf.getEntityData().get(SukunaFushiguroEntity.DATA_world_cut) );
    }

    private static boolean hasWorldCut(LivingEntity entity) {
        return entity instanceof SukunaPerfectEntity
                || (entity instanceof SukunaFushiguroEntity sf && sf.getEntityData().get(SukunaFushiguroEntity.DATA_world_cut));
    }

    private static Action chooseBestAction(Snapshot s) {
        if (s.lethalForecast) {
            Action survival = bestOf(s, List.of(
                    Action.backstep("SURVIVAL_RESET", scoreBackstep(s) + 1.1),
                    Action.guard("PANIC_SURVIVAL", scoreGuard(s) + 0.8),
                    Action.hwb(scoreHwb(s) + 0.55),
                    Action.mahoraga(scoreMahoraga(s) + 0.7),
                    Action.heianReset(scoreHeianReset(s))));
            if (survival.score > 0.65) return survival;
        }

        if (s.targetDomain || s.targetCastingDomain || s.domainAssessment.targetDomainThreat > 0.55) {
            Action antiDomain = bestOf(s, List.of(
                    Action.hwb(scoreHwb(s) + 1.0),
                    Action.domain(scoreDomain(s) + 0.6),
                    Action.tenShadowsDomain(scoreTenShadowsDomain(s) + 0.6),
                    Action.worldCut("INTERRUPT", scoreWorldCut(s) + 0.35),
                    Action.move("MaintainRange", Movement.MAINTAIN_RANGE, scoreMaintainRange(s) + 0.25)));
            if (antiDomain.score > 0.55) return antiDomain;
        }

        if (s.infinitySignal > 0.0) {
            Action antiInfinity = bestOf(s, List.of(
                    Action.domainAmplification(scoreDomainAmplification(s) + 0.8),
                    Action.domain(scoreDomain(s) + 0.35),
                    Action.worldCut("ANTI_INFINITY", scoreWorldCut(s) + 0.55),
                    Action.mahoraga(scoreMahoraga(s) + 0.25),
                    Action.move("MaintainRange", Movement.MAINTAIN_RANGE, scoreMaintainRange(s))));
            if (antiInfinity.score > 0.45) return antiInfinity;
        }

        if (s.targetCooldown || s.targetUnstable || s.targetAttacking) {
            Action punish = bestOf(s, offensiveActions(s, "PUNISH"));
            if (punish.score > 0.72) return punish;
        }

        if (s.killConfirm) {
            Action finisher = bestOf(s, offensiveActions(s, "KILL_CONFIRM"));
            if (finisher.score > 0.65) return finisher;
        }

        if (s.trivialTarget) {
            return bestOf(s, List.of(
                    Action.skill("RESOURCE_EFFICIENT", FAST_DISMANTLE, 50.0, false, scoreDismantle(s) + 0.45),
                    Action.skill("TrivialCleave", CLEAVE, 100.0, false, scoreCleave(s) + 0.25),
                    Action.calculate("TRIVIAL_CLEANUP", scoreBasic(s) + 0.4)));
        }

        List<Action> actions = baselineActions(s);
        Action best = actions.get(0);
        double bestScore = best.score + s.memory.actionBias(best.name);
        for (Action action : actions) {
            double adjusted = action.score + s.memory.actionBias(action.name);
            if (adjusted > bestScore) {
                best = action;
                bestScore = adjusted;
            }
        }
        return best;
    }

    private static List<Action> baselineActions(Snapshot s) {
        List<Action> actions = new ArrayList<>();
        actions.add(Action.backstep(scoreBackstep(s)));
        actions.add(Action.guard(scoreGuard(s)));
        actions.add(Action.domainAmplification(scoreDomainAmplification(s)));
        actions.add(Action.calculate("Chase", scoreChase(s)));
        actions.add(Action.calculate("CalculateAttack", scoreBasic(s)));
        actions.add(Action.hwb(scoreHwb(s)));
        actions.add(Action.move("ProjectileDodge", Movement.PROJECTILE_DODGE, scoreProjectileDodge(s)));
        actions.add(Action.move("MaintainRange", Movement.MAINTAIN_RANGE, scoreMaintainRange(s)));
        actions.add(Action.move("CutOffEscape", Movement.CUT_OFF_ESCAPE, scoreCutOffEscape(s)));
        actions.add(Action.move("StrafePressure", Movement.STRAFE_PRESSURE, scoreStrafePressure(s)));
        actions.addAll(offensiveActions(s, ""));
        actions.add(Action.summon(scoreSummon(s)));
        actions.add(Action.agito(scoreAgito(s)));
        actions.add(Action.mahoraga(scoreMahoraga(s)));
        actions.add(Action.tenShadowsDomain(scoreTenShadowsDomain(s)));
        actions.add(Action.heianReset(scoreHeianReset(s)));
        return actions;
    }

    private static List<Action> offensiveActions(Snapshot s, String priorityName) {
        List<Action> actions = new ArrayList<>();
        String prefix = priorityName == null || priorityName.isEmpty() ? "" : priorityName + "_";
        if (s.normalReady) {
            actions.add(Action.skill(prefix + "Dismantle", FAST_DISMANTLE, 50.0, false, scoreDismantle(s)));
            actions.add(Action.skill(prefix + "Cleave", CLEAVE, 100.0, false, scoreCleave(s)));
            actions.add(Action.skill(prefix + "Open", OPEN, 250.0, false, scoreOpen(s)));
            if (s.worldCutCapable) {
                actions.add(Action.worldCut(prefix + "WorldCut", scoreWorldCut(s)));
            }
            if (s.domainReady && !s.selfDomain && !s.brainDamaged) {
                actions.add(Action.domain(scoreDomain(s)));
            }
        }
        if (s.passiveReady && !s.combatCooldown && !s.infinity) {
            actions.add(Action.skill(prefix + "Passive111", PASSIVE_FAST, 50.0, true, scorePassive111(s)));
            actions.add(Action.skill(prefix + "Passive112", PASSIVE_CLOSE, 50.0, true, scorePassive112(s)));
            actions.add(Action.skill(prefix + "Passive113", PASSIVE_ANTI_RANGE, 50.0, true, scorePassive113(s)));
        }
        if (actions.isEmpty()) {
            actions.add(Action.calculate(prefix + "CalculateAttack", scoreBasic(s)));
        }
        return actions;
    }

    private static Action bestOf(Snapshot s, List<Action> actions) {
        Action best = actions.get(0);
        double bestScore = best.score + s.memory.actionBias(best.name);
        for (Action action : actions) {
            double adjusted = action.score + s.memory.actionBias(action.name);
            if (adjusted > bestScore) {
                best = action;
                bestScore = adjusted;
            }
        }
        return best;
    }

    private static double scoreHwb(Snapshot s) {
        if (s.sukuna.hasEffect(JujutsucraftaddonModMobEffects.HWB.get()) || s.selfDomain) return -1.0;
        return s.domainAssessment.hwbScore;
    }

    private static double scoreGuard(Snapshot s) {
        if (s.sukuna.hasEffect(JujutsucraftModMobEffects.GUARD.get()) || s.sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_GUARD.get())) return -1.0;
        double recency = s.tick - s.memory.lastGuardTick < 14.0 ? -1.2 : 0.0;
        double comboRisk = s.meleeThreat * close(s.distance, s.itadoriModulo ? 10.0 : 7.0) + s.targetSkillDanger * 0.45 + s.damageTakenBurst * 1.8;
        double domainPenalty = s.targetDomain || s.targetCastingDomain ? -0.35 : 0.0;
        return recency - 0.05 + comboRisk + domainPenalty;
    }

    private static double scoreDomainAmplification(Snapshot s) {
        if (s.sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get()) || s.selfDomain) return -1.0;
        double recency = s.tick - s.memory.lastDomainAmplificationTick < 45.0 ? -0.6 : 0.0;
        double infinityValue = s.infinitySignal * (s.distance < 12.0 ? 1.7 : 1.1);
        double domainValue = (s.targetNeutralization || s.targetDomainAmplification ? 0.35 : 0.0);
        double burstValue = s.itadoriModulo && s.distance < 10.0 ? 0.75 : 0.0;
        return recency - 0.05 + infinityValue + domainValue + burstValue + s.targetSkillDanger * 0.2;
    }

    private static double scoreBackstep(Snapshot s) {
        double recency = s.tick - s.memory.lastBackstepTick < 8.0 ? -2.0 : 0.0;
        double burstRange = s.itadoriModulo ? close(s.distance, 11.0) * 1.35 : close(s.distance, 7.5) * 0.8;
        double danger = s.dangerArea * 1.15 + s.targetSkillDanger * 0.8 + s.meleeThreat * burstRange + s.damageTakenBurst * 2.0;
        double health = (1.0 - s.selfHealthRatio) * s.recentDamageRatio * 6.0;
        double punishLoss = s.targetCooldown || s.targetUnstable ? -0.4 : 0.0;
        return recency + danger + health + punishLoss;
    }

    private static double scoreProjectileDodge(Snapshot s) {
        double recency = s.tick - s.memory.lastProjectileDodgeTick < 10.0 ? -1.2 : 0.0;
        return recency + s.incomingProjectileRisk * 3.2 + s.targetSkillDanger * 0.25;
    }

    private static double scoreMaintainRange(Snapshot s) {
        double error = Math.abs(s.predictedDistance - s.idealDistance) / Math.max(8.0, s.idealDistance);
        double unsafeClose = s.meleeThreat * close(s.distance, s.itadoriModulo ? 12.0 : 6.0) * (s.itadoriModulo ? 1.25 : 0.7);
        return 0.16 + Mth.clamp(error, 0.0, 0.9) + unsafeClose + s.dangerArea * 0.15;
    }

    private static double scoreCutOffEscape(Snapshot s) {
        return 0.12 + (s.targetEscaping ? 0.75 : 0.0) + s.targetFleeBias * 0.55 + (s.targetHealthRatio < 0.35 ? 0.25 : 0.0);
    }

    private static double scoreStrafePressure(Snapshot s) {
        double range = band(s.distance, 5.0, 24.0);
        double cooldown = s.normalReady ? -0.18 : 0.18;
        return 0.18 + range * 0.32 + s.rangeThreat * 0.22 + s.meleeThreat * 0.16 + cooldown;
    }

    private static double scoreChase(Snapshot s) {
        double ideal = Math.abs(s.distance - s.idealDistance) / 32.0;
        return 0.22 + Mth.clamp(ideal, 0.0, 0.8) + s.targetFleeBias * 0.35 - s.dangerArea * 0.35;
    }

    private static double scoreBasic(Snapshot s) {
        if (s.infinitySignal > 0.0) return -0.55;
        return 0.18 + (s.distance > 45.0 ? 0.35 : 0.0) + s.dangerArea * 0.2 + (s.trivialTarget ? 1.0 : 0.0);
    }

    private static double scoreDismantle(Snapshot s) {
        if (s.infinitySignal > 0.0) return -0.75;
        double range = band(s.distance, 5.0, 48.0);
        double domainBonus = s.selfDomain ? 0.45 : 0.0;
        return 0.42 + range * 0.75 + s.opportunity * 0.25 + s.domainAssessment.punishScore * 0.15 + s.killPressure(0.20) + domainBonus - s.expensiveWastePenalty(0.20);
    }

    private static double scoreCleave(Snapshot s) {
        if (s.infinitySignal > 0.0) return -0.85;
        double domainBonus = s.selfDomain ? 0.45 : 0.0;
        return 0.36 + close(s.distance, 7.0) * 1.0 + s.meleeThreat * 0.35 + s.opportunity * 0.45 + s.killPressure(0.28) + domainBonus - s.dodgeCounterRisk * 0.35
                + (s.trivialTarget ? 0.45 : 0.0);
    }

    private static double scoreOpen(Snapshot s) {
        if (s.infinitySignal > 0.0 && !s.selfDomain) return -0.45;
        if (s.trivialTarget) return -1.25;
        double range = band(s.distance, 8.0, 40.0);
        double domainBonus = s.selfDomain ? 0.55 : 0.0;
        return 0.34 + range * 0.8 + s.healScore * 0.45 + s.tankScore * 0.35 + s.postHealWindow * 0.4
                + s.domainAssessment.punishScore * 0.2 + s.killPressure(0.35) + domainBonus - s.dangerArea * 0.25
                + (s.itadoriModulo && (s.targetCooldown || s.targetUnstable || s.distance >= 10.0) ? 0.85 : 0.0)
                - s.expensiveWastePenalty(0.45);
    }

    private static double scoreWorldCut(Snapshot s) {
        if (!s.worldCutCapable) return -2.0;
        if (s.trivialTarget) return -2.0;
        double range = band(s.distance, 4.0, 58.0);
        double gojoBonus = (s.read.primaryTechnique == TechniqueIDs.GOJO || s.read.secondaryTechnique == TechniqueIDs.GOJO) ? 0.65 : 0.0;
        double castBonus = s.targetCastingDomain ? 0.55 : 0.0;
        double burstPunish = s.itadoriModulo && (s.targetCooldown || s.targetUnstable || s.distance >= 9.0) ? 0.95 : 0.0;
        return 0.30 + range * 0.65 + s.infinitySignal * 1.9 + gojoBonus + castBonus + s.tankScore * 0.45 + s.targetDomainCounterBias * 0.25
                + s.domainAssessment.punishScore * 0.35 + s.killPressure(0.55) + s.opportunity * 0.45 + burstPunish - close(s.distance, 3.5) * 0.4
                - s.expensiveWastePenalty(0.65);
    }

    private static double scoreDomain(Snapshot s) {
        if (s.trivialTarget) return -1.5;
        if (s.targetThreat < 0.45 && !s.targetDomain && !s.targetCastingDomain && s.infinitySignal <= 0.0) return -0.8;
        return s.domainAssessment.castScore;
    }

    private static double scoreSummon(Snapshot s) {
        if (!s.canUseTenShadows || s.trivialTarget || !s.normalReady || s.selfDomain) return -2.0;
        return -0.05 + s.rangeThreat * 0.45 + s.healScore * 0.35 + s.pressure * 0.35
                + (s.targetEscaping ? 0.25 : 0.0) + (s.targetThreat > 0.55 ? 0.25 : 0.0)
                - s.expensiveWastePenalty(0.42);
    }

    private static double scoreAgito(Snapshot s) {
        if (!s.canUseAgito || !s.normalReady || s.trivialTarget || s.selfDomain) return -2.0;
        return 0.05 + s.healScore * 0.75 + s.rangeThreat * 0.45 + s.pressure * 0.45 + s.postHealWindow * 0.35
                + (s.targetThreat > 0.65 ? 0.35 : 0.0) + s.killPressure(0.25) * 0.25
                - s.expensiveWastePenalty(0.58);
    }

    private static double scoreMahoraga(Snapshot s) {
        if (!s.canUseMahoraga || !s.normalReady || s.trivialTarget || s.mahoragaExist) return -2.0;
        double adaptation = (s.infinitySignal > 0.0 ? 0.9 : 0.0) + (s.mahoragaWheel ? 0.25 : 0.0);
        return -0.15 + adaptation + s.domainAssessment.targetDomainThreat * 0.8 + s.damageTakenBurst * 0.85
                + (s.lethalForecast ? 0.95 : 0.0) + (s.targetThreat > 0.9 ? 0.55 : 0.0)
                + (s.selfHealthRatio < 0.38 ? 0.45 : 0.0) - s.expensiveWastePenalty(0.82);
    }

    private static double scoreTenShadowsDomain(Snapshot s) {
        if (!s.canUseTenShadowsDomain || !s.normalReady || s.trivialTarget || s.selfDomain || s.brainDamaged) return -2.0;
        double betterThanShrine = s.isMeguna && (s.mahoragaExist || s.domainAssessment.counterRisk > 0.45 || s.infinitySignal <= 0.0) ? 0.35 : -0.05;
        return betterThanShrine + s.domainAssessment.castScore + s.healScore * 0.25 + s.tankScore * 0.25
                + (s.targetDomain || s.targetCastingDomain ? 0.55 : 0.0) - s.expensiveWastePenalty(0.65);
    }

    private static double scoreHeianReset(Snapshot s) {
        if (!s.isMeguna || s.isPerfectMode || s.trivialTarget) return -2.0;
        boolean failedPlan = s.memory.megunaHighThreatTicks > 220.0 && (s.targetHealthRatio > 0.55 || s.selfHealthRatio < 0.55);
        return -0.5 + (s.lethalForecast ? 1.65 : 0.0) + (failedPlan ? 0.75 : 0.0)
                + (s.selfHealthRatio < 0.28 ? 0.55 : 0.0) + s.damageTakenBurst * 0.5;
    }

    private static double scorePassive111(Snapshot s) {
        return 0.2 + band(s.distance, 2.0, 16.0) * 0.45 + s.targetSkillDanger * 0.25;
    }

    private static double scorePassive112(Snapshot s) {
        return 0.28 + close(s.distance, 8.0) * 0.55 + s.meleeThreat * 0.35;
    }

    private static double scorePassive113(Snapshot s) {
        return 0.22 + band(s.distance, 10.0, 48.0) * 0.45 + s.rangeThreat * 0.45;
    }

    private static double close(double distance, double max) {
        return Mth.clamp((max - distance) / max, 0.0, 1.0);
    }

    private static double band(double distance, double min, double max) {
        if (distance < min) {
            return Mth.clamp(distance / Math.max(1.0, min), 0.0, 1.0);
        }
        if (distance > max) {
            return Mth.clamp(1.0 - (distance - max) / Math.max(1.0, max), 0.0, 1.0);
        }
        return 1.0;
    }

    private static void useDomain(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, CompoundTag nbt, Snapshot s) {
        nbt.putDouble("skill", 1.0);
        ReturnShadowProcedure.execute(world, x, y, z, sukuna);
        setSkill(sukuna, nbt, DOMAIN, 20.0, false);
        s.memory.lastSukunaDomainTick = s.tick;
    }

    private static void backstep(LevelAccessor world, LivingEntity sukuna, Snapshot s) {
        CompoundTag nbt = sukuna.getPersistentData();
        nbt.putBoolean("PRESS_S", true);
        WhenBackStepProcedure.execute(world, sukuna);
        nbt.putBoolean("PRESS_S", false);
        s.memory.lastBackstepTick = s.tick;
    }

    private static void guard(LevelAccessor world, LivingEntity sukuna, Snapshot s) {
        StartGuardProcedure.execute(world, sukuna);
        s.memory.lastGuardTick = s.tick;
    }

    private static void domainAmplification(LivingEntity sukuna, Snapshot s) {
        if (!sukuna.level().isClientSide()) {
            sukuna.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get(), 30, 254, false, false));
        }
        s.memory.lastDomainAmplificationTick = s.tick;
    }

    private static void moveTactically(LivingEntity sukuna, Snapshot s, Movement movement) {
        Vec3 self = sukuna.position();
        Vec3 target = s.target.position();
        Vec3 toward = horizontal(target.subtract(self));
        if (toward.lengthSqr() < 1.0E-4) {
            toward = new Vec3(1.0, 0.0, 0.0);
        }
        Vec3 away = toward.scale(-1.0);
        Vec3 lateral = new Vec3(-toward.z, 0.0, toward.x).normalize().scale(s.strafeSide);
        Vec3 destination;
        double speed = 1.25;

        if (movement == Movement.PROJECTILE_DODGE) {
            destination = self.add(s.projectileDodgeVector.scale(7.0)).add(toward.scale(1.5));
            speed = 1.45;
            s.memory.lastProjectileDodgeTick = s.tick;
        } else if (movement == Movement.MAINTAIN_RANGE) {
            double error = s.distance - s.idealDistance;
            Vec3 adjust = error < 0.0 ? away.scale(Math.min(8.0, -error + 2.0)) : toward.scale(Math.min(8.0, error + 2.0));
            destination = self.add(adjust).add(lateral.scale(2.5));
        } else if (movement == Movement.CUT_OFF_ESCAPE) {
            Vec3 predicted = s.target.position().add(s.target.getDeltaMovement().scale(14.0));
            destination = predicted.add(lateral.scale(-2.0));
            speed = 1.35;
        } else {
            double inward = s.distance > s.idealDistance + 3.0 ? 2.5 : 0.6;
            destination = self.add(lateral.scale(6.0)).add(toward.scale(inward));
        }

        moveTo(sukuna, destination, speed);
        s.memory.lastMoveTick = s.tick;
    }

    private static Vec3 horizontal(Vec3 vector) {
        Vec3 flat = new Vec3(vector.x, 0.0, vector.z);
        return flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
    }

    private static void moveTo(LivingEntity sukuna, Vec3 destination, double speed) {
        Vec3 self = sukuna.position();
        if (sukuna instanceof Mob mob) {
            mob.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
        }
        Vec3 impulse = destination.subtract(self);
        if (impulse.lengthSqr() > 1.0E-4) {
            Vec3 flat = horizontal(impulse).scale(0.22);
            sukuna.setDeltaMovement(sukuna.getDeltaMovement().add(flat.x, 0.0, flat.z));
        }
    }

    private static void setSkill(LivingEntity sukuna, CompoundTag nbt, double skill, double tick, boolean combatOnly) {
        nbt.putDouble("cnt_x", 0.0);
        nbt.putDouble("skill", skill);
        if (!sukuna.level().isClientSide()) {
            MobEffect cooldown = combatOnly ? JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get() : JujutsucraftModMobEffects.COOLDOWN_TIME.get();
            int duration = combatOnly ? (int) tick : Math.max(1, (int) tick / 2);
            sukuna.addEffect(new MobEffectInstance(cooldown, duration, 0, false, false));
            sukuna.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
        }
    }

    private static void setWorldCut(LivingEntity sukuna, CompoundTag nbt) {
        if (!hasWorldCut(sukuna)) {
            setSkill(sukuna, nbt, FAST_DISMANTLE, 50.0, false);
            return;
        }
        nbt.putDouble("cnt_x", 0.0);
        nbt.putDouble("cnt6", Math.max(nbt.getDouble("cnt6"), 5.0));
        nbt.putDouble("cnt7", 2.0);
        nbt.putBoolean("flag_dismantle", true);
        setSkill(sukuna, nbt, FAST_DISMANTLE, 100.0, false);
    }

    private static void equipMahoragaWheel(LivingEntity sukuna, Snapshot s) {
        if (!s.canUseMahoraga || s.mahoragaWheel || s.trivialTarget) {
            return;
        }
        if (s.infinitySignal > 0.0 || s.targetThreat > 0.72 || s.lethalForecast || s.damageTakenBurst > 0.45) {
            sukuna.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()));
        }
    }

    private static String targetKey(LivingEntity target) {
        return target.getStringUUID();
    }

    private static String typeKey(LivingEntity target) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        return id == null ? target.getClass().getName() : id.toString();
    }

    private enum Movement {
        NONE,
        PROJECTILE_DODGE,
        MAINTAIN_RANGE,
        CUT_OFF_ESCAPE,
        STRAFE_PRESSURE
    }

    private enum ActionKind {
        MELEE,
        NORMAL_SLASH,
        WORLD_CUT,
        DOMAIN,
        TEN_SHADOWS,
        TRANSFORM,
        DOMAIN_AMPLIFICATION,
        MICRO_DEFENSE,
        RANGE_CONTROL
    }

    private static class Action {
        final String name;
        final ActionKind kind;
        final double skill;
        final double cooldownTicks;
        final boolean combatOnly;
        final boolean backstep;
        final boolean domain;
        final boolean hwb;
        final boolean guard;
        final boolean domainAmplification;
        final boolean worldCut;
        final boolean tenShadows;
        final boolean heianReset;
        final Movement movement;
        final double score;

        private Action(String name, ActionKind kind, double skill, double cooldownTicks, boolean combatOnly, boolean backstep, boolean domain, boolean hwb,
                       boolean guard, boolean domainAmplification, boolean worldCut, boolean tenShadows, boolean heianReset, Movement movement, double score) {
            this.name = name;
            this.kind = kind;
            this.skill = skill;
            this.cooldownTicks = cooldownTicks;
            this.combatOnly = combatOnly;
            this.backstep = backstep;
            this.domain = domain;
            this.hwb = hwb;
            this.guard = guard;
            this.domainAmplification = domainAmplification;
            this.worldCut = worldCut;
            this.tenShadows = tenShadows;
            this.heianReset = heianReset;
            this.movement = movement;
            this.score = score;
        }

        static Action skill(String name, double skill, double cooldownTicks, boolean combatOnly, double score) {
            ActionKind kind = skill == CLEAVE ? ActionKind.MELEE : ActionKind.NORMAL_SLASH;
            return new Action(name, kind, skill, cooldownTicks, combatOnly, false, false, false, false, false, false, false, false, Movement.NONE, score);
        }

        static Action worldCut(double score) {
            return worldCut("WorldCut", score);
        }

        static Action worldCut(String name, double score) {
            return new Action(name, ActionKind.WORLD_CUT, FAST_DISMANTLE, 100.0, false, false, false, false, false, false, true, false, false, Movement.NONE, score);
        }

        static Action backstep(double score) {
            return backstep("Backstep", score);
        }

        static Action backstep(String name, double score) {
            return new Action(name, ActionKind.MICRO_DEFENSE, 0.0, 0.0, false, true, false, false, false, false, false, false, false, Movement.NONE, score);
        }

        static Action guard(double score) {
            return guard("Guard", score);
        }

        static Action guard(String name, double score) {
            return new Action(name, ActionKind.MICRO_DEFENSE, 0.0, 0.0, false, false, false, false, true, false, false, false, false, Movement.NONE, score);
        }

        static Action domainAmplification(double score) {
            return new Action("DomainAmplification", ActionKind.DOMAIN_AMPLIFICATION, 0.0, 0.0, false, false, false, false, false, true, false, false, false, Movement.NONE, score);
        }

        static Action domain(double score) {
            return new Action("Domain", ActionKind.DOMAIN, DOMAIN, 20.0, false, false, true, false, false, false, false, false, false, Movement.NONE, score);
        }

        static Action hwb(double score) {
            return new Action("HWB", ActionKind.MICRO_DEFENSE, 0.0, 0.0, false, false, false, true, false, false, false, false, false, Movement.NONE, score);
        }

        static Action move(String name, Movement movement, double score) {
            return new Action(name, ActionKind.RANGE_CONTROL, 0.0, 0.0, false, false, false, false, false, false, false, false, false, movement, score);
        }

        static Action calculate(String name, double score) {
            return new Action(name, ActionKind.MELEE, 0.0, 0.0, false, false, false, false, false, false, false, false, false, Movement.NONE, score);
        }

        static Action summon(double score) {
            return new Action("SUMMON", ActionKind.TEN_SHADOWS, TEN_SHADOWS_UTILITY, 90.0, false, false, false, false, false, false, false, true, false, Movement.NONE, score);
        }

        static Action agito(double score) {
            return new Action("AGITO", ActionKind.TEN_SHADOWS, AGITO, 140.0, false, false, false, false, false, false, false, true, false, Movement.NONE, score);
        }

        static Action mahoraga(double score) {
            return new Action("MAHORAGA", ActionKind.TEN_SHADOWS, MAHORAGA, 180.0, false, false, false, false, false, false, false, true, false, Movement.NONE, score);
        }

        static Action tenShadowsDomain(double score) {
            return new Action("TEN_SHADOWS_DOMAIN", ActionKind.TEN_SHADOWS, TEN_SHADOWS_DOMAIN, 20.0, false, false, false, false, false, false, false, true, false, Movement.NONE, score);
        }

        static Action heianReset(double score) {
            return new Action("SURVIVAL_RESET_HEIAN", ActionKind.TRANSFORM, 0.0, 0.0, false, false, false, false, false, false, false, false, true, Movement.NONE, score);
        }

        void execute(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, CompoundTag nbt, Snapshot s) {
            if (hwb) {
                sukuna.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.HWB.get(), 100, 0, false, false));
            } else if (guard) {
                OpSukunaBrain.guard(world, sukuna, s);
            } else if (domainAmplification) {
                OpSukunaBrain.domainAmplification(sukuna, s);
            } else if (backstep) {
                OpSukunaBrain.backstep(world, sukuna, s);
            } else if (domain) {
                OpSukunaBrain.useDomain(world, x, y, z, sukuna, nbt, s);
            } else if (worldCut) {
                OpSukunaBrain.setWorldCut(sukuna, nbt);
            } else if (heianReset) {
                JJKURSukunaAIBuff.forceHeianTransformation(world, sukuna, false);
            } else if (tenShadows) {
                OpSukunaBrain.equipMahoragaWheel(sukuna, s);
                OpSukunaBrain.setSkill(sukuna, nbt, skill, cooldownTicks, combatOnly);
            } else if (skill != 0.0) {
                OpSukunaBrain.setSkill(sukuna, nbt, skill, cooldownTicks, combatOnly);
            } else if (movement != Movement.NONE) {
                OpSukunaBrain.moveTactically(sukuna, s, movement);
            } else {
                CalculateAttackProcedure.execute(world, sukuna);
            }
        }
    }

    private static class Snapshot {
        final LevelAccessor world;
        final double x;
        final double y;
        final double z;
        final LivingEntity sukuna;
        final LivingEntity target;
        final CompoundTag nbt;
        final BrainMemory memory;
        final CombatStats targetStats;
        final CombatStats typeStats;
        final CombatStats techniqueStats;
        final CombatStats archetypeStats;
        final PlayerRead read;
        final double tick;
        final double distance;
        final double predictedDistance;
        final double selfHealth;
        final double targetHealth;
        final double selfHealthRatio;
        final double targetHealthRatio;
        final double damageTaken;
        final double damageDealt;
        final double targetHeal;
        final double recentDamageRatio;
        final double damageTakenBurst;
        final double targetSkill;
        final double targetSkillDanger;
        final double meleeThreat;
        final double rangeThreat;
        final double tankScore;
        final double healScore;
        final double powerScore;
        final double targetThreat;
        final double pressure;
        final double opportunity;
        final double domainCounterRisk;
        final double targetDomainCounterBias;
        final double dodgeCounterRisk;
        final double targetFleeBias;
        final double dangerArea;
        final double incomingProjectileRisk;
        final double postHealWindow;
        final double idealDistance;
        final double infinitySignal;
        final Vec3 projectileDodgeVector;
        final DomainAssessment domainAssessment;
        final boolean selfDomain;
        final boolean targetDomain;
        final boolean targetCooldown;
        final boolean targetUnstable;
        final boolean combatCooldown;
        final boolean passiveReady;
        final boolean normalReady;
        final boolean domainReady;
        final boolean infinity;
        final boolean targetCastingDomain;
        final boolean worldCutCapable;
        final boolean brainDamaged;
        final boolean targetHwb;
        final boolean targetSimpleDomain;
        final boolean targetDomainBreak;
        final boolean targetDodge;
        final boolean targetCounter;
        final boolean targetGuard;
        final boolean targetRegen;
        final boolean targetAntiHeal;
        final boolean targetAttacking;
        final boolean targetNeutralization;
        final boolean targetDomainAmplification;
        final boolean targetEscaping;
        final boolean itadoriModulo;
        final boolean trivialTarget;
        final boolean isFushiguro;
        final boolean isPerfectMode;
        final boolean isMeguna;
        final boolean canUseTenShadows;
        final boolean canUseAgito;
        final boolean canUseMahoraga;
        final boolean canUseTenShadowsDomain;
        final boolean mahoragaWheel;
        final boolean mahoragaExist;
        final boolean lethalForecast;
        final boolean killConfirm;
        final int strafeSide;
        final String archetype;

        static Snapshot capture(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, LivingEntity target, CompoundTag nbt,
                                BrainMemory memory) {
            CombatStats targetStats = memory.target(targetKey(target));
            CombatStats typeStats = memory.type(typeKey(target));
            PlayerRead read = PlayerRead.read(target);
            String preliminary = classify(target, read, targetStats, typeStats, null);
            CombatStats archetypeStats = memory.archetype(preliminary);
            String archetype = classify(target, read, targetStats, typeStats, archetypeStats);
            archetypeStats = memory.archetype(archetype);
            String techniqueKey = read.techniqueKey();
            CombatStats techniqueStats = memory.technique(techniqueKey);
            return new Snapshot(world, x, y, z, sukuna, target, nbt, memory, targetStats, typeStats, techniqueStats, archetypeStats, read, archetype);
        }

        private Snapshot(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, LivingEntity target, CompoundTag nbt,
                         BrainMemory memory, CombatStats targetStats, CombatStats typeStats, CombatStats techniqueStats,
                         CombatStats archetypeStats, PlayerRead read, String archetype) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.sukuna = sukuna;
            this.target = target;
            this.nbt = nbt;
            this.memory = memory;
            this.targetStats = targetStats;
            this.typeStats = typeStats;
            this.techniqueStats = techniqueStats;
            this.archetypeStats = archetypeStats;
            this.read = read;
            this.archetype = archetype;
            this.tick = sukuna.tickCount;
            this.distance = GetDistanceProcedure.execute(sukuna);
            this.predictedDistance = predictedDistance(sukuna, target);
            this.selfHealth = sukuna.getHealth();
            this.targetHealth = target.getHealth();
            this.selfHealthRatio = selfHealth / Math.max(1.0, sukuna.getMaxHealth());
            this.targetHealthRatio = targetHealth / Math.max(1.0, target.getMaxHealth());
            this.damageTaken = Math.max(0.0, memory.lastHealth - selfHealth);
            this.damageDealt = Math.max(0.0, targetStats.lastHealth - targetHealth);
            this.targetHeal = Math.max(0.0, targetHealth - targetStats.lastHealth);
            this.recentDamageRatio = damageTaken / Math.max(1.0, sukuna.getMaxHealth());
            this.damageTakenBurst = Mth.clamp((damageTaken + targetStats.damageTakenAvg) / Math.max(1.0, sukuna.getMaxHealth() * 0.12), 0.0, 1.0);
            this.targetSkill = target.getPersistentData().getDouble("skill");
            this.selfDomain = sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
            this.targetDomain = target.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
            this.targetCooldown = target.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get()) || target.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get());
            this.targetUnstable = target.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get());
            this.combatCooldown = LogicCooldownCombatProcedure.execute(sukuna);
            this.passiveReady = LogicStartPassiveProcedure.execute(sukuna);
            this.normalReady = LogicStartProcedure.execute(sukuna);
            this.domainReady = LogicConfilmDomainProcedure.execute(world, x, y, z, sukuna);
            this.infinity = target.hasEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.INFINITY.get());
            this.infinitySignal = (infinity ? 1.0 : 0.0) + (read.primaryTechnique == TechniqueIDs.GOJO || read.secondaryTechnique == TechniqueIDs.GOJO ? 0.35 : 0.0);
            this.targetCastingDomain = targetSkill == 20.0 || (read.primaryTechnique == TechniqueIDs.GOJO && target.getPersistentData().getDouble("cnt1") > 0.0 && target.getPersistentData().getDouble("cnt1") < 40.0);
            this.worldCutCapable = hasWorldCut(sukuna);
            this.brainDamaged = sukuna.hasEffect(JujutsucraftModMobEffects.BRAIN_DAMAGE.get());
            this.isFushiguro = sukuna instanceof SukunaFushiguroEntity;
            this.isPerfectMode = sukuna instanceof SukunaFushiguroEntity sf && sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode);
            this.isMeguna = isFushiguro && !isPerfectMode;
            this.mahoragaWheel = sukuna.getItemBySlot(EquipmentSlot.HEAD).getItem() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get();
            this.mahoragaExist = nbt.getDouble("TenShadowsTechnique14") == -1.0;
            this.canUseTenShadows = isMeguna && nbt.getBoolean("flag_start");
            this.canUseAgito = canUseTenShadows && nbt.getDouble("TenShadowsTechnique13") >= 0.0;
            this.canUseMahoraga = canUseTenShadows && nbt.getDouble("TenShadowsTechnique14") >= 0.0;
            this.canUseTenShadowsDomain = canUseTenShadows && (nbt.getDouble("TenShadowsTechnique14") >= 0.0 || mahoragaExist);
            this.itadoriModulo = isItadoriModulo(target);
            this.targetHwb = target.hasEffect(JujutsucraftaddonModMobEffects.HWB.get());
            this.targetSimpleDomain = read.simpleDomain || target.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.SIMPLE_DOMAIN_MAX.get());
            this.targetDodge = target.hasEffect(JujutsucraftaddonModMobEffects.DODGE.get());
            this.targetCounter = target.hasEffect(JujutsucraftaddonModMobEffects.COUNTER.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.COUNTER_CD.get());
            this.targetGuard = target.hasEffect(JujutsucraftModMobEffects.GUARD.get()) || target.getPersistentData().getBoolean("guard");
            this.targetRegen = target.hasEffect(MobEffects.REGENERATION) || read.rct > 0.0;
            this.targetAntiHeal = target.hasEffect(JujutsucraftaddonModMobEffects.ANTI_HEAL.get());
            this.targetAttacking = target.getPersistentData().getBoolean("attack") || target.getPersistentData().getDouble("Damage") != 0.0 || targetSkill != 0.0;
            this.targetNeutralization = target.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get());
            this.targetDomainAmplification = target.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get());
            DangerScan danger = scanDanger(world, sukuna);
            this.dangerArea = danger.areaRisk;
            this.incomingProjectileRisk = danger.incomingProjectileRisk;
            this.projectileDodgeVector = danger.dodgeVector;
            this.postHealWindow = targetHeal > target.getMaxHealth() * 0.03 || target.hasEffect(MobEffects.REGENERATION) ? 1.0 : 0.0;
            this.targetSkillDanger = scoreTargetSkill(targetSkill, targetStats);
            this.healScore = Mth.clamp(targetStats.targetHealAvg / Math.max(1.0, target.getMaxHealth() * 0.06) + read.healBias * 0.4 + postHealWindow * 0.25, 0.0, 1.0);
            double damageEfficiency = Mth.clamp(targetStats.damageDealtAvg / Math.max(1.0, target.getMaxHealth() * 0.06), 0.0, 1.0);
            this.tankScore = Mth.clamp((target.getMaxHealth() / Math.max(1.0, sukuna.getMaxHealth()) - 0.65) + (1.0 - damageEfficiency) * 0.55 + read.tankBias * 0.35, 0.0, 1.0);
            this.meleeThreat = Mth.clamp(targetStats.meleeRatio() * 0.55 + typeStats.meleeRatio() * 0.2 + archetypeStats.meleeRatio() * 0.2
                    + targetStats.damageTakenAvg / Math.max(1.0, sukuna.getMaxHealth() * 0.08) + read.aggression * 0.25, 0.0, 1.0);
            this.rangeThreat = Mth.clamp(targetStats.rangedRatio() * 0.45 + typeStats.rangedRatio() * 0.25 + archetypeStats.rangedRatio() * 0.25 + read.rangedBias * 0.45, 0.0, 1.0);
            this.domainCounterRisk = Mth.clamp(read.domainBias * 0.35 + read.hwbBias * 0.2 + targetStats.counterDomainAfterSukuna / 4.0 + typeStats.counterDomainAfterSukuna / 7.0
                    + archetypeStats.counterDomainAfterSukuna / 7.0, 0.0, 1.0);
            this.targetDomainCounterBias = Mth.clamp(targetStats.domainTicks / Math.max(1.0, targetStats.seenTicks) + read.domainBias, 0.0, 1.0);
            this.dodgeCounterRisk = Mth.clamp((targetDodge ? 0.55 : 0.0) + (targetCounter ? 0.55 : 0.0), 0.0, 1.0);
            this.targetFleeBias = Mth.clamp(targetStats.fleeTicks / Math.max(1.0, targetStats.seenTicks) + read.fleeBias * 0.45, 0.0, 1.0);
            this.powerScore = estimatePower(this);
            this.targetThreat = estimateThreat(this);
            this.trivialTarget = isTrivialTarget(this);
            this.pressure = Mth.clamp(powerScore * 0.45 + targetSkillDanger * 0.25 + (1.0 - selfHealthRatio) * 0.3 + dangerArea * 0.35, 0.0, 1.0);
            this.opportunity = Mth.clamp((targetCooldown ? 0.35 : 0.0) + (targetUnstable ? 0.35 : 0.0) + (1.0 - targetHealthRatio) * 0.3
                    + postHealWindow * 0.25 + (target.hasEffect(JujutsucraftaddonModMobEffects.FATIGUE.get()) ? 0.2 : 0.0), 0.0, 1.0);
            this.idealDistance = idealDistance(archetype, infinitySignal, tankScore, healScore);
            this.targetDomainBreak = target.hasEffect(JujutsucraftaddonModMobEffects.DOMAIN_BREAK.get());
            this.targetEscaping = target.getDeltaMovement().dot(sukuna.position().subtract(target.position())) < -0.03 || predictedDistance > distance + 1.5;
            this.lethalForecast = selfHealthRatio < 0.22
                    || (selfHealthRatio < 0.42 && (damageTakenBurst > 0.55 || targetSkillDanger > 0.75 || targetDomain))
                    || damageTaken >= selfHealth * 0.55;
            this.killConfirm = targetHealthRatio < 0.34
                    || (targetHealth <= 32.0 && (targetCooldown || targetUnstable || postHealWindow > 0.0))
                    || (targetAntiHeal && targetHealthRatio < 0.52);
            this.strafeSide = ((sukuna.getUUID().getLeastSignificantBits() ^ target.getUUID().getMostSignificantBits()) & 1L) == 0L ? 1 : -1;
            this.domainAssessment = DomainTactics.assess(this);
        }

        double killPressure(double expectedDamageRatio) {
            return targetHealthRatio <= expectedDamageRatio ? 1.0 : Mth.clamp((expectedDamageRatio * 1.5 - targetHealthRatio) / Math.max(0.01, expectedDamageRatio * 1.5), 0.0, 1.0);
        }

        double expensiveWastePenalty(double minimumThreat) {
            if (targetThreat >= minimumThreat || targetDomain || targetCastingDomain || infinitySignal > 0.0) return 0.0;
            return Mth.clamp((minimumThreat - targetThreat) * 2.2, 0.0, 1.3);
        }
    }

    private static String classify(LivingEntity target, PlayerRead read, CombatStats targetStats, CombatStats typeStats, CombatStats archetypeStats) {
        if (isItadoriModulo(target)) return "MELEE_BURST_TANK";
        if (target.hasEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.INFINITY.get())
                || read.primaryTechnique == TechniqueIDs.GOJO || read.secondaryTechnique == TechniqueIDs.GOJO) return "INFINITY_USER";
        if (target.hasEffect(JujutsucraftaddonModMobEffects.BLACK_FLASH_CUT.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.FATIGUE_BLACK_FLASH.get())
                || read.primaryTechnique == TechniqueIDs.ITADORI || read.secondaryTechnique == TechniqueIDs.ITADORI) return "BLACK_FLASH_USER";
        if (read.domainBias > 0.55 || targetStats.counterDomainAfterSukuna > 2.0 || typeStats.counterDomainAfterSukuna > 4.0) return "DOMAIN_COUNTER";
        if (read.healBias > 0.5 || targetStats.targetHealAvg > target.getMaxHealth() * 0.025) return "HEALER";
        if (read.tankBias > 0.55 || target.getMaxHealth() > 80.0) return "TANK";
        if (read.rangedBias > 0.55 || targetStats.rangedRatio() > 0.45 || typeStats.rangedRatio() > 0.55) return "RANGED_PRESSURE";
        if (read.aggression > 0.55 || targetStats.meleeRatio() > 0.45) return "MELEE_BURST";
        return "UNKNOWN_OP";
    }

    private static double predictedDistance(LivingEntity sukuna, LivingEntity target) {
        Vec3 predicted = target.position().add(target.getDeltaMovement().scale(8.0));
        return predicted.distanceTo(sukuna.position());
    }

    private static double idealDistance(String archetype, double infinitySignal, double tankScore, double healScore) {
        if (infinitySignal > 0.1) return 18.0;
        if ("MELEE_BURST_TANK".equals(archetype)) return 22.0;
        if ("MELEE_BURST".equals(archetype) || "BLACK_FLASH_USER".equals(archetype)) return 12.0;
        if ("RANGED_PRESSURE".equals(archetype)) return 16.0;
        if (tankScore > 0.65 || healScore > 0.55) return 24.0;
        return 14.0;
    }

    private static boolean isItadoriModulo(LivingEntity target) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        String name = id == null ? target.getType().toString().toLowerCase() : id.toString().toLowerCase();
        return name.equals("jjsk:itadori_yuji_modulo")
                || name.equals("jujutsucrafts:itadori_yuji_modulo")
                || name.equals("jujutsucraftaddon:itadori_yuji_modulo")
                || name.contains("itadori_yuji_modulo");
    }

    private static DangerScan scanDanger(LevelAccessor world, LivingEntity sukuna) {
        if (!(world instanceof Level level)) {
            return DangerScan.NONE;
        }
        AABB box = sukuna.getBoundingBox().inflate(9.0);
        double areaRisk = 0.0;
        double incomingRisk = 0.0;
        Vec3 dodgeVector = Vec3.ZERO;
        Vec3 sukunaCenter = sukuna.position().add(0.0, sukuna.getBbHeight() * 0.5, 0.0);
        for (Entity entity : level.getEntities(sukuna, box, entity -> entity instanceof Projectile)) {
            if (entity instanceof Projectile projectile && projectile.getOwner() == sukuna) {
                continue;
            }
            double dist = Math.max(1.0, entity.distanceTo(sukuna));
            areaRisk += 1.0 / dist;
            Vec3 velocity = entity.getDeltaMovement();
            if (velocity.lengthSqr() > 1.0E-4) {
                Vec3 toSukuna = sukunaCenter.subtract(entity.position());
                Vec3 travel = velocity.normalize();
                double approach = travel.dot(toSukuna.normalize());
                double missDistance = toSukuna.subtract(travel.scale(toSukuna.dot(travel))).length();
                if (approach > 0.55 && missDistance < 3.5) {
                    double risk = approach * (3.5 - missDistance) / 3.5 * Mth.clamp(9.0 / dist, 0.0, 1.0);
                    incomingRisk += risk;
                    Vec3 lateral = new Vec3(-travel.z, 0.0, travel.x);
                    if (lateral.lengthSqr() > 1.0E-4) {
                        dodgeVector = dodgeVector.add(lateral.normalize().scale(risk));
                    }
                }
            }
        }
        if (dodgeVector.lengthSqr() < 1.0E-4) {
            dodgeVector = new Vec3(1.0, 0.0, 0.0);
        } else {
            dodgeVector = dodgeVector.normalize();
        }
        return new DangerScan(Mth.clamp(areaRisk, 0.0, 1.0), Mth.clamp(incomingRisk, 0.0, 1.0), dodgeVector);
    }

    private static class DangerScan {
        static final DangerScan NONE = new DangerScan(0.0, 0.0, new Vec3(1.0, 0.0, 0.0));
        final double areaRisk;
        final double incomingProjectileRisk;
        final Vec3 dodgeVector;

        DangerScan(double areaRisk, double incomingProjectileRisk, Vec3 dodgeVector) {
            this.areaRisk = areaRisk;
            this.incomingProjectileRisk = incomingProjectileRisk;
            this.dodgeVector = dodgeVector;
        }
    }

    private static double scoreTargetSkill(double skill, CombatStats stats) {
        if (skill <= 0.0) return stats.damageTakenAvg > stats.damageDealtAvg ? 0.25 : 0.0;
        double repeat = stats.skillUseRatio(skill);
        double base = skill >= 20.0 ? 0.32 : 0.12;
        if (skill >= 100.0) base += 0.22;
        if (skill >= 1000.0) base += 0.22;
        return Mth.clamp(base + repeat * 0.42 + stats.damageTakenPeak * 0.01, 0.0, 1.0);
    }

    private static double estimatePower(Snapshot s) {
        double hpPower = s.target.getMaxHealth() / Math.max(1.0, s.sukuna.getMaxHealth());
        double damagePower = (s.targetStats.damageTakenAvg + s.damageTaken) / Math.max(1.0, s.sukuna.getMaxHealth() * 0.08);
        double survivalPower = s.targetHealthRatio > 0.72 && s.targetStats.seenTicks > 200.0 ? 0.18 : 0.0;
        double domainPower = s.targetDomain || s.targetStats.domainTicks > 80.0 ? 0.25 : 0.0;
        return Mth.clamp(hpPower * 0.22 + damagePower * 0.45 + s.healScore * 0.15 + survivalPower + domainPower + s.read.outputLevel * 0.04, 0.0, 1.0);
    }

    private static double estimateThreat(Snapshot s) {
        double specialDefense = (s.infinitySignal > 0.0 ? 0.8 : 0.0)
                + (s.targetDomain || s.targetCastingDomain ? 0.55 : 0.0)
                + (s.targetSimpleDomain || s.targetHwb || s.targetNeutralization || s.targetDomainAmplification ? 0.25 : 0.0);
        double specialBody = (s.itadoriModulo ? 0.9 : 0.0)
                + (s.target instanceof Player ? 0.2 : 0.0)
                + (s.read.ultimate ? 0.2 : 0.0);
        return Mth.clamp(s.powerScore + s.targetSkillDanger * 0.35 + s.tankScore * 0.25 + s.healScore * 0.2
                + s.meleeThreat * 0.2 + s.rangeThreat * 0.2 + s.damageTakenBurst * 0.35 + specialDefense + specialBody, 0.0, 1.5);
    }

    private static boolean isTrivialTarget(Snapshot s) {
        if (s.target instanceof Player || s.itadoriModulo || s.infinitySignal > 0.0 || s.targetDomain || s.targetCastingDomain) return false;
        if (s.targetSimpleDomain || s.targetHwb || s.targetNeutralization || s.targetDomainAmplification || s.targetRegen) return false;
        if (s.target.getMaxHealth() > 40.0 || s.targetThreat >= 0.32) return false;
        return s.damageTakenBurst < 0.12 && s.targetSkillDanger < 0.25;
    }

    private static class DomainTactics {
        static DomainAssessment assess(Snapshot s) {
            double simpleRisk = (s.targetSimpleDomain ? 0.42 : 0.0) + Mth.clamp(s.read.simpleDomainLevel / 8.0, 0.0, 0.35);
            double hwbRisk = (s.targetHwb || s.read.hwbActive ? 0.55 : 0.0) + s.read.hwbBias * 0.35;
            double breakRisk = s.targetDomainBreak ? 0.7 : 0.0;
            double amplificationRisk = s.targetDomainAmplification || s.targetNeutralization ? 0.25 : 0.0;
            double masteryRisk = Mth.clamp((s.read.domainMastery - 1.0) / 2.0, 0.0, 0.6);
            double barrierlessRisk = s.read.barrierlessDomain ? 0.35 : 0.0;
            double infusedRisk = s.read.infusedDomain ? 0.25 : 0.0;
            double learnedCounter = s.domainCounterRisk;
            double counterRisk = Mth.clamp(simpleRisk + hwbRisk + breakRisk + amplificationRisk + masteryRisk + barrierlessRisk + infusedRisk + learnedCounter * 0.65, 0.0, 1.0);

            double targetDomainThreat = Mth.clamp((s.targetCastingDomain ? 1.0 : 0.0) + (s.targetDomain ? 0.75 : 0.0) + s.read.domainBias * 0.55
                    + (s.read.ultimate ? 0.25 : 0.0) + (s.read.curseEnergy > 450.0 ? 0.25 : 0.0), 0.0, 1.0);
            double domainValue = Mth.clamp(s.pressure * 0.65 + s.tankScore * 0.35 + s.healScore * 0.35 + s.infinitySignal * 0.45
                    + s.killPressure(0.45) * 0.8 + (s.selfHealthRatio < 0.45 ? 0.25 : 0.0), 0.0, 1.5);
            double emergency = s.targetCastingDomain ? 3.0 : (s.targetDomain ? 1.0 : 0.0);
            double castScore = 0.2 + domainValue + emergency + (1.0 - counterRisk) * 0.7 - counterRisk * 1.15;

            double hwbScore = -0.2 + targetDomainThreat * 2.4 + (s.targetCastingDomain ? 1.4 : 0.0) - (s.selfHealthRatio > 0.75 && !s.targetDomain ? 0.35 : 0.0);
            double punishScore = Mth.clamp(counterRisk * 0.45 + targetDomainThreat * 0.35 + (s.targetCooldown ? 0.25 : 0.0), 0.0, 1.0);
            return new DomainAssessment(counterRisk, targetDomainThreat, castScore, hwbScore, punishScore);
        }
    }

    private static class DomainAssessment {
        final double counterRisk;
        final double targetDomainThreat;
        final double castScore;
        final double hwbScore;
        final double punishScore;

        DomainAssessment(double counterRisk, double targetDomainThreat, double castScore, double hwbScore, double punishScore) {
            this.counterRisk = counterRisk;
            this.targetDomainThreat = targetDomainThreat;
            this.castScore = castScore;
            this.hwbScore = hwbScore;
            this.punishScore = punishScore;
        }
    }

    private static class PlayerRead {
        double primaryTechnique;
        double secondaryTechnique;
        double curseEnergy;
        double outputLevel;
        double moveset;
        double rct;
        double domainBias;
        double healBias;
        double aggression;
        double fleeBias;
        double rangedBias;
        double tankBias;
        double hwbBias;
        double techniqueMastery;
        double simpleDomainLevel;
        double domainMastery = 1.0;
        double bfChance;
        double ceShield;
        boolean barrierlessDomain;
        boolean infusedDomain;
        boolean simpleDomain;
        boolean hwbActive;
        boolean worldSlash;
        boolean ultimate;
        boolean itadoriAwakening;
        boolean mahoragaReady;

        static PlayerRead read(LivingEntity target) {
            PlayerRead read = new PlayerRead();
            read.primaryTechnique = target.getPersistentData().getDouble("PlayerCurseTechnique");
            read.secondaryTechnique = target.getPersistentData().getDouble("PlayerCurseTechnique2");
            read.outputLevel = 1.0;
            if (target instanceof Player) {
                target.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
                    read.primaryTechnique = vars.PlayerCurseTechnique;
                    read.secondaryTechnique = vars.PlayerCurseTechnique2;
                    read.curseEnergy = noisy(vars.PlayerCursePower, target, 0);
                    read.domainBias = Math.max(read.domainBias, vars.PlayerCursePower > 450.0 ? 0.45 : 0.15);
                });
                target.getCapability(com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
                    read.outputLevel = noisy(vars.OutputLevel, target, 1);
                    read.moveset = noisy(vars.Moveset, target, 2);
                    read.rct = noisy(vars.RCTMastery + vars.RCTRegen + (vars.RCTOutputActive ? 1.0 : 0.0), target, 3);
                    read.techniqueMastery = noisy(vars.TechniqueMastery, target, 4);
                    read.simpleDomainLevel = noisy(vars.SimpleDomainLevel, target, 5);
                    read.bfChance = noisy(vars.BFChance + vars.blackflashmastery, target, 6);
                    read.ceShield = noisy(vars.CEShield, target, 7);
                    read.barrierlessDomain = vars.BarrierlessDomain;
                    read.infusedDomain = vars.InfusedDomain;
                    read.simpleDomain = vars.SimpleDomain;
                    read.hwbActive = vars.hwb_active;
                    read.worldSlash = vars.WorldSlash;
                    read.ultimate = vars.Ultimate;
                    read.itadoriAwakening = vars.ItadoriAwakening > 0.0;
                    read.mahoragaReady = vars.Mahoraga > 0.0 || vars.MahoragaCanAdapt > 0.0;
                    read.healBias += Mth.clamp(read.rct / 6.0, 0.0, 0.45);
                    read.hwbBias += vars.hwb_active ? 0.6 : 0.0;
                    read.tankBias += Mth.clamp((vars.HPCap + vars.HealthAttribute + vars.CEShield) / 120.0, 0.0, 0.35);
                });
                read.domainMastery = noisy(DomainMasterySystem.getFinalMultiplier(target), target, 8);
            }
            read.applyTechniqueBias(target);
            read.applyEffectBias(target);
            return read;
        }

        String techniqueKey() {
            return Math.round(primaryTechnique) + ":" + Math.round(secondaryTechnique);
        }

        private void applyTechniqueBias(LivingEntity target) {
            if (primaryTechnique == TechniqueIDs.GOJO || secondaryTechnique == TechniqueIDs.GOJO) {
                rangedBias += 0.55;
                domainBias += 0.35;
            }
            if (primaryTechnique == TechniqueIDs.ITADORI || secondaryTechnique == TechniqueIDs.ITADORI || looksLikeItadoriModulo(target)) {
                aggression += 0.65;
                tankBias += 0.35;
            }
            if (primaryTechnique == TechniqueIDs.HAKARI || secondaryTechnique == TechniqueIDs.HAKARI || primaryTechnique == TechniqueIDs.OKKOTSU || secondaryTechnique == TechniqueIDs.OKKOTSU) {
                healBias += 0.35;
                domainBias += 0.25;
            }
            if (primaryTechnique == TechniqueIDs.JOGO || secondaryTechnique == TechniqueIDs.JOGO || primaryTechnique == TechniqueIDs.URAUME || secondaryTechnique == TechniqueIDs.URAUME) {
                rangedBias += 0.45;
            }
            if (barrierlessDomain || infusedDomain || ultimate) {
                domainBias += 0.35;
            }
            if (worldSlash) {
                rangedBias += 0.25;
            }
            if (itadoriAwakening || bfChance > 2.0) {
                aggression += 0.25;
            }
            if (mahoragaReady) {
                tankBias += 0.25;
                domainBias += 0.15;
            }
            aggression = Mth.clamp(aggression, 0.0, 1.0);
            rangedBias = Mth.clamp(rangedBias, 0.0, 1.0);
            healBias = Mth.clamp(healBias, 0.0, 1.0);
            domainBias = Mth.clamp(domainBias, 0.0, 1.0);
            tankBias = Mth.clamp(tankBias, 0.0, 1.0);
        }

        private void applyEffectBias(LivingEntity target) {
            if (target.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) domainBias += 0.55;
            if (target.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.SIMPLE_DOMAIN_MAX.get())) hwbBias += 0.25;
            if (target.hasEffect(JujutsucraftaddonModMobEffects.HWB.get())) hwbBias += 0.6;
            if (target.hasEffect(JujutsucraftaddonModMobEffects.DOMAIN_BREAK.get())) domainBias += 0.35;
            if (target.hasEffect(MobEffects.REGENERATION) || target.hasEffect(JujutsucraftaddonModMobEffects.ANTI_HEAL.get())) healBias += 0.35;
            if (target.hasEffect(JujutsucraftaddonModMobEffects.WORLD_CUT.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.WORLD_SLASH_EFFECT.get())) rangedBias += 0.35;
            if (target.hasEffect(JujutsucraftaddonModMobEffects.BLACK_FLASH_CUT.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.KOKUSEN_EFFECT.get())) aggression += 0.4;
            if (target.hasEffect(JujutsucraftaddonModMobEffects.FATIGUE.get())) aggression -= 0.15;
            if (target.hasEffect(JujutsucraftaddonModMobEffects.DODGE.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.COUNTER.get())) fleeBias += 0.25;
            aggression = Mth.clamp(aggression, 0.0, 1.0);
            fleeBias = Mth.clamp(fleeBias, 0.0, 1.0);
            rangedBias = Mth.clamp(rangedBias, 0.0, 1.0);
            healBias = Mth.clamp(healBias, 0.0, 1.0);
            domainBias = Mth.clamp(domainBias, 0.0, 1.0);
            hwbBias = Mth.clamp(hwbBias, 0.0, 1.0);
        }

        private static double noisy(double value, LivingEntity target, int salt) {
            double seed = Math.abs((target.getUUID().getLeastSignificantBits() + salt * 734287L) % 1000L) / 1000.0;
            double error = (seed - 0.5) * 0.30;
            return value * (1.0 + error);
        }

        private static boolean looksLikeItadoriModulo(LivingEntity target) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
            String name = id == null ? target.getType().toString().toLowerCase() : id.toString().toLowerCase();
            if (name.contains("itadori") || name.contains("yuji")) return true;
            double skill = target.getPersistentData().getDouble("skill");
            return target.getMaxHealth() >= 60.0 && (target.hasEffect(JujutsucraftaddonModMobEffects.IMBUED_FISTS.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.BLACK_FLASH_CUT.get()) || skill == 21.0);
        }
    }

    public static class BrainMemory {
        final Map<String, CombatStats> targets = new HashMap<>();
        final Map<String, CombatStats> types = new HashMap<>();
        final Map<String, CombatStats> techniques = new HashMap<>();
        final Map<String, CombatStats> archetypes = new HashMap<>();
        final Map<String, Double> actionValues = new HashMap<>();
        double lastHealth;
        double lastBackstepTick = -1000.0;
        double lastProjectileDodgeTick = -1000.0;
        double lastMoveTick = -1000.0;
        double lastGuardTick = -1000.0;
        double lastDomainAmplificationTick = -1000.0;
        double lastSukunaDomainTick = -1000.0;
        double megunaHighThreatTicks;
        String lastAction = "None";
        String lastActionTarget = "";

        CombatStats target(String key) {
            return targets.computeIfAbsent(key, unused -> new CombatStats());
        }

        CombatStats type(String key) {
            return types.computeIfAbsent(key, unused -> new CombatStats());
        }

        CombatStats technique(String key) {
            return techniques.computeIfAbsent(key, unused -> new CombatStats());
        }

        CombatStats archetype(String key) {
            return archetypes.computeIfAbsent(key, unused -> new CombatStats());
        }

        void observe(Snapshot s) {
            observeActionValue(s);
            observeStats(s.targetStats, s, 1.0);
            observeStats(s.typeStats, s, 0.35);
            observeStats(s.techniqueStats, s, 0.35);
            observeStats(s.archetypeStats, s, 0.35);
            lastHealth = s.selfHealth;
            megunaHighThreatTicks = s.isMeguna && s.targetThreat > 0.62 && !s.trivialTarget ? megunaHighThreatTicks + 1.0 : Math.max(0.0, megunaHighThreatTicks - 2.0);
            if (s.targetDomain && s.tick - lastSukunaDomainTick < 600.0) {
                s.targetStats.counterDomainAfterSukuna += 1.0;
                s.typeStats.counterDomainAfterSukuna += 0.35;
                s.techniqueStats.counterDomainAfterSukuna += 0.35;
                s.archetypeStats.counterDomainAfterSukuna += 0.35;
            }
        }

        double actionBias(String action) {
            return Mth.clamp(actionValues.getOrDefault(action, 0.0), -0.35, 0.35);
        }

        private void observeActionValue(Snapshot s) {
            if ("None".equals(lastAction) || !lastActionTarget.equals(targetKey(s.target))) {
                return;
            }
            double reward = s.damageDealt / Math.max(1.0, s.target.getMaxHealth()) * 3.0
                    - s.damageTaken / Math.max(1.0, s.sukuna.getMaxHealth()) * 2.6
                    - s.targetHeal / Math.max(1.0, s.target.getMaxHealth()) * 1.2
                    + (s.targetCooldown || s.targetUnstable ? 0.08 : 0.0)
                    + (s.targetEscaping && ("CutOffEscape".equals(lastAction) || "StrafePressure".equals(lastAction)) ? 0.08 : 0.0);
            if (s.trivialTarget && isExpensiveAction(lastAction)) {
                reward -= 0.45;
            }
            if ((s.targetCooldown || s.targetUnstable) && lastAction.startsWith("PUNISH")) {
                reward += 0.12;
            }
            reward = Mth.clamp(reward, -0.5, 0.5);
            double old = actionValues.getOrDefault(lastAction, 0.0);
            actionValues.put(lastAction, old + (reward - old) * 0.08);
        }

        private static boolean isExpensiveAction(String action) {
            return action.contains("Open") || action.contains("WorldCut") || action.contains("Domain")
                    || "MAHORAGA".equals(action) || "AGITO".equals(action) || "SUMMON".equals(action);
        }

        private void observeStats(CombatStats stats, Snapshot s, double weight) {
            stats.sample("damageTakenAvg", s.damageTaken, 0.18 * weight);
            stats.sample("damageDealtAvg", s.damageDealt, 0.18 * weight);
            stats.sample("targetHealAvg", s.targetHeal, 0.16 * weight);
            stats.sample("distanceAvg", s.distance, 0.08 * weight);
            stats.damageTakenPeak = Math.max(stats.damageTakenPeak, s.damageTaken);
            stats.targetHealPeak = Math.max(stats.targetHealPeak, s.targetHeal);
            stats.seenTicks += weight;
            stats.lastHealth = s.targetHealth;
            if (s.distance < 8.0 || s.predictedDistance < 8.0) stats.meleeTicks += weight;
            if (s.distance > 24.0 || s.predictedDistance > 24.0) stats.rangedTicks += weight;
            if (s.target.position().distanceTo(s.sukuna.position()) > s.distance + 0.4) stats.fleeTicks += weight;
            if (s.targetDomain) stats.domainTicks += weight;
            if (s.targetSkill > 0.0) stats.addSkillUse(s.targetSkill, weight);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("lastHealth", lastHealth);
            tag.putDouble("lastBackstepTick", lastBackstepTick);
            tag.putDouble("lastProjectileDodgeTick", lastProjectileDodgeTick);
            tag.putDouble("lastMoveTick", lastMoveTick);
            tag.putDouble("lastGuardTick", lastGuardTick);
            tag.putDouble("lastDomainAmplificationTick", lastDomainAmplificationTick);
            tag.putDouble("lastSukunaDomainTick", lastSukunaDomainTick);
            tag.putDouble("megunaHighThreatTicks", megunaHighThreatTicks);
            tag.putString("lastAction", lastAction);
            tag.putString("lastActionTarget", lastActionTarget);
            tag.put("targets", saveMap(targets));
            tag.put("types", saveMap(types));
            tag.put("techniques", saveMap(techniques));
            tag.put("archetypes", saveMap(archetypes));
            tag.put("actionValues", saveDoubleMap(actionValues));
            return tag;
        }

        public static BrainMemory load(CompoundTag tag) {
            BrainMemory memory = new BrainMemory();
            memory.lastHealth = tag.getDouble("lastHealth");
            memory.lastBackstepTick = tag.getDouble("lastBackstepTick");
            memory.lastProjectileDodgeTick = tag.getDouble("lastProjectileDodgeTick");
            memory.lastMoveTick = tag.getDouble("lastMoveTick");
            memory.lastGuardTick = tag.getDouble("lastGuardTick");
            memory.lastDomainAmplificationTick = tag.getDouble("lastDomainAmplificationTick");
            memory.lastSukunaDomainTick = tag.getDouble("lastSukunaDomainTick");
            memory.megunaHighThreatTicks = tag.getDouble("megunaHighThreatTicks");
            memory.lastAction = tag.getString("lastAction");
            memory.lastActionTarget = tag.getString("lastActionTarget");
            loadMap(tag.getCompound("targets"), memory.targets);
            loadMap(tag.getCompound("types"), memory.types);
            loadMap(tag.getCompound("techniques"), memory.techniques);
            loadMap(tag.getCompound("archetypes"), memory.archetypes);
            loadDoubleMap(tag.getCompound("actionValues"), memory.actionValues);
            return memory;
        }

        private static CompoundTag saveMap(Map<String, CombatStats> map) {
            CompoundTag tag = new CompoundTag();
            for (Map.Entry<String, CombatStats> entry : map.entrySet()) {
                tag.put(entry.getKey(), entry.getValue().save());
            }
            return tag;
        }

        private static void loadMap(CompoundTag tag, Map<String, CombatStats> map) {
            for (String key : tag.getAllKeys()) {
                map.put(key, CombatStats.load(tag.getCompound(key)));
            }
        }

        private static CompoundTag saveDoubleMap(Map<String, Double> map) {
            CompoundTag tag = new CompoundTag();
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                tag.putDouble(entry.getKey(), entry.getValue());
            }
            return tag;
        }

        private static void loadDoubleMap(CompoundTag tag, Map<String, Double> map) {
            for (String key : tag.getAllKeys()) {
                map.put(key, tag.getDouble(key));
            }
        }
    }

    private static class CombatStats {
        double lastHealth;
        double damageTakenAvg;
        double damageDealtAvg;
        double targetHealAvg;
        double distanceAvg;
        double damageTakenPeak;
        double targetHealPeak;
        double seenTicks;
        double meleeTicks;
        double rangedTicks;
        double fleeTicks;
        double domainTicks;
        double skillUses;
        double counterDomainAfterSukuna;
        final Map<Long, Double> skills = new HashMap<>();

        void sample(String key, double value, double alpha) {
            if (!Double.isFinite(value)) return;
            if ("damageTakenAvg".equals(key)) damageTakenAvg = sampleValue(damageTakenAvg, value, alpha);
            else if ("damageDealtAvg".equals(key)) damageDealtAvg = sampleValue(damageDealtAvg, value, alpha);
            else if ("targetHealAvg".equals(key)) targetHealAvg = sampleValue(targetHealAvg, value, alpha);
            else if ("distanceAvg".equals(key)) distanceAvg = sampleValue(distanceAvg, value, alpha);
        }

        double meleeRatio() {
            return meleeTicks / Math.max(1.0, seenTicks);
        }

        double rangedRatio() {
            return rangedTicks / Math.max(1.0, seenTicks);
        }

        void addSkillUse(double skill, double amount) {
            skillUses += amount;
            long key = Math.round(skill);
            skills.put(key, skills.getOrDefault(key, 0.0) + amount);
        }

        double skillUseRatio(double skill) {
            return skills.getOrDefault(Math.round(skill), 0.0) / Math.max(1.0, skillUses);
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("lastHealth", lastHealth);
            tag.putDouble("damageTakenAvg", damageTakenAvg);
            tag.putDouble("damageDealtAvg", damageDealtAvg);
            tag.putDouble("targetHealAvg", targetHealAvg);
            tag.putDouble("distanceAvg", distanceAvg);
            tag.putDouble("damageTakenPeak", damageTakenPeak);
            tag.putDouble("targetHealPeak", targetHealPeak);
            tag.putDouble("seenTicks", seenTicks);
            tag.putDouble("meleeTicks", meleeTicks);
            tag.putDouble("rangedTicks", rangedTicks);
            tag.putDouble("fleeTicks", fleeTicks);
            tag.putDouble("domainTicks", domainTicks);
            tag.putDouble("skillUses", skillUses);
            tag.putDouble("counterDomainAfterSukuna", counterDomainAfterSukuna);
            CompoundTag skillTag = new CompoundTag();
            for (Map.Entry<Long, Double> entry : skills.entrySet()) {
                skillTag.putDouble(Long.toString(entry.getKey()), entry.getValue());
            }
            tag.put("skills", skillTag);
            return tag;
        }

        static CombatStats load(CompoundTag tag) {
            CombatStats stats = new CombatStats();
            stats.lastHealth = tag.getDouble("lastHealth");
            stats.damageTakenAvg = tag.getDouble("damageTakenAvg");
            stats.damageDealtAvg = tag.getDouble("damageDealtAvg");
            stats.targetHealAvg = tag.getDouble("targetHealAvg");
            stats.distanceAvg = tag.getDouble("distanceAvg");
            stats.damageTakenPeak = tag.getDouble("damageTakenPeak");
            stats.targetHealPeak = tag.getDouble("targetHealPeak");
            stats.seenTicks = tag.getDouble("seenTicks");
            stats.meleeTicks = tag.getDouble("meleeTicks");
            stats.rangedTicks = tag.getDouble("rangedTicks");
            stats.fleeTicks = tag.getDouble("fleeTicks");
            stats.domainTicks = tag.getDouble("domainTicks");
            stats.skillUses = tag.getDouble("skillUses");
            stats.counterDomainAfterSukuna = tag.getDouble("counterDomainAfterSukuna");
            CompoundTag skillTag = tag.getCompound("skills");
            for (String key : skillTag.getAllKeys()) {
                try {
                    stats.skills.put(Long.parseLong(key), skillTag.getDouble(key));
                } catch (NumberFormatException ignored) {
                }
            }
            return stats;
        }

        private static double sampleValue(double old, double value, double alpha) {
            return old == 0.0 ? value : old + (value - old) * alpha;
        }
    }
}
