package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.util.OpSukunaBrainTelemetry;
import net.mcreator.jujutsucraft.entity.SukunaPerfectEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.AntiInfinityProcedure;
import net.mcreator.jujutsucraft.procedures.GetDistanceProcedure;
import net.mcreator.jujutsucraft.procedures.GetReachProcedure;
import net.mcreator.jujutsucraft.procedures.InsideSolidCalculateProcedure;
import net.mcreator.jujutsucraft.procedures.LogicCooldownCombatProcedure;
import net.mcreator.jujutsucraft.procedures.LogicStartPassiveProcedure;
import net.mcreator.jujutsucraft.procedures.LogicStartProcedure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

import static com.jujutsu.jujutsucraftaddon.procedures.opsukuna.OpSukunaEngineCore.*;

class OpSukunaSnapshot {
    final LevelAccessor world;
    final double x;
    final double y;
    final double z;
    final LivingEntity sukuna;
    final LivingEntity target;
    final CompoundTag nbt;
    final OpSukunaBrainMemory memory;
    final OpSukunaCombatStats targetStats;
    final OpSukunaCombatStats typeStats;
    final OpSukunaCombatStats techniqueStats;
    final OpSukunaCombatStats archetypeStats;
    final OpSukunaPlayerRead read;
    final double tick;
    final double distance;
    final double predictedDistance;
    final double selfHealth;
    final double targetHealth;
    final double selfHealthRatio;
    final double targetHealthRatio;
    final double hitboxDistance;
    final double centerDistance;
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
    final double purpleRisk;
    final double postHealWindow;
    final double idealDistance;
    final double infinitySignal;
    final double immediateThreat;
    final double whiffOpportunity;
    final double hitConfidence;
    final double groupPressure;
    final double groupActivePressure;
    final double groupApexPressure;
    final Vec3 projectileDodgeVector;
    final Vec3 purpleDodgeVector;
    final Vec3 groupEscapeVector;
    final Vec3 domainEscapeVector;
    final OpSukunaDomainFieldScan domainField;
    final OpSukunaDomainTactics.DomainAssessment domainAssessment;
    final OpSukunaDomainTactics.DomainProfile domainProfile;
    final OpSukunaDomainTactics.DomainDeliberation domainDeliberation;
    final AdaptationView adaptation;
    final OpSukunaEngineCore.CombatRelation targetCombatRelation;
    final PowerProfile targetPowerProfile;
    final KillEstimate targetKillEstimate;
    final double rctLimit;
    final double rctStrain;
    final double rctLevel;
    final double brainDamageLevel;
    final double incomingKillRisk;
    final double survivalUrgency;
    final double blackFlashRecoveryValue;
    final double openShotQuality;
    final double stuckLevel;
    final double domainConfidence;
    final double selfReach;
    final double targetReach;
    final double worldCutCooldownRemaining;
    final boolean selfDomain;
    final boolean targetDomain;
    final boolean targetCooldown;
    final boolean targetUnstable;
    final boolean combatCooldown;
    final boolean passiveReady;
    final boolean normalReady;
    final boolean domainReady;
    final boolean domainBlockedByCooldown;
    final boolean domainRecentlyOpened;
    final boolean infinity;
    final boolean gojoTarget;
    final boolean targetCastingDomain;
    final boolean purpleThreat;
    final boolean purpleWindup;
    final boolean purpleProjectile;
    final boolean purpleLineOfFire;
    final boolean worldCutCapable;
    final boolean worldCutReady;
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
    final boolean targetPerfectAntiDomain;
    final boolean targetEscaping;
    final boolean targetSkillStartup;
    final boolean targetWhiffed;
    final boolean targetOverextended;
    final boolean selfRct;
    final boolean selfSimpleDomain;
    final boolean selfAntiHeal;
    final boolean itadoriModulo;
    final boolean yujiCounterStance;
    final boolean trivialTarget;
    final boolean isFushiguro;
    final boolean isPerfectMode;
    final boolean isMeguna;
    final boolean canTransformHeian;
    final boolean canUseTenShadows;
    final boolean canUseAgito;
    final boolean canUseMahoraga;
    final boolean canUseTenShadowsDomain;
    final boolean canUseRct;
    final boolean canUseSimpleDomain;
    final boolean canUseBurnoutRct;
    final boolean canUseDomainAmplification;
    final int simpleDomainDuration;
    final int simpleDomainCooldown;
    final boolean rctFatigued;
    final boolean survivalMode;
    final boolean mahoragaWheel;
    final boolean mahoragaExist;
    final boolean lethalForecast;
    final boolean killConfirm;
    final boolean clearShot;
    final boolean pathBlocked;
    final boolean selfInsideSolid;
    final boolean antiInfinityBypass;
    final boolean canBypassInfinityNow;
    final boolean yujiBurstDuel;
    final boolean enemyDomainTrap;
    final boolean catastrophicDomain;
    final boolean domainEscapeWindow;
    final boolean domainDeathSpiral;
    final boolean simpleDomainExpiresSoon;
    final boolean antiDomainGapImminent;
    final boolean blackFlashChain;
    final boolean healthLosingRace;
    final double domainCastRequestedSkill;
    final boolean domainCastPending;
    final boolean domainCastConfirmed;
    final boolean fugaActive;
    final double fugaCharge;
    final double fugaChargeMax;
    final double fugaChargeRate;
    final double fugaHoldTicks;
    final int fugaDuration;
    final int strafeSide;
    final String archetype;
    final String sukunaForm;
    final String domainBlockReason;
    final String domainCastFailedReason;
    final String worldCutBlockReason;
    final String heianEmergencyReason;
    final String fugaReleaseReason;
    final String infinityBypassMode;
    final String meleeUnlockReason;
    final OpSukunaDomainIntent domainIntent;
    boolean actionStarted = true;
    String blockReason = "ready";
    String fallbackAction = "";
    double voidExposure;

    static OpSukunaSnapshot capture(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, LivingEntity target, CompoundTag nbt,
                            OpSukunaBrainMemory memory) {
        OpSukunaCombatStats targetStats = memory.target(targetKey(target));
        OpSukunaCombatStats typeStats = memory.type(typeKey(target));
        OpSukunaPlayerRead read = OpSukunaPlayerRead.read(target);
        String preliminary = classify(target, read, targetStats, typeStats, null);
        OpSukunaCombatStats archetypeStats = memory.archetype(preliminary);
        String archetype = classify(target, read, targetStats, typeStats, archetypeStats);
        archetypeStats = memory.archetype(archetype);
        String techniqueKey = read.techniqueKey();
        OpSukunaCombatStats techniqueStats = memory.technique(techniqueKey);
        return new OpSukunaSnapshot(world, x, y, z, sukuna, target, nbt, memory, targetStats, typeStats, techniqueStats, archetypeStats, read, archetype);
    }

    private OpSukunaSnapshot(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, LivingEntity target, CompoundTag nbt,
                     OpSukunaBrainMemory memory, OpSukunaCombatStats targetStats, OpSukunaCombatStats typeStats, OpSukunaCombatStats techniqueStats,
                     OpSukunaCombatStats archetypeStats, OpSukunaPlayerRead read, String archetype) {
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
        this.hitboxDistance = hitboxDistance(sukuna, target);
        this.centerDistance = sukuna.position().add(0.0, sukuna.getBbHeight() * 0.5, 0.0)
                .distanceTo(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
        this.predictedDistance = predictedDistance(sukuna, target);
        this.stuckLevel = memory.updateStuck(sukuna, sukuna.tickCount);
        this.selfReach = Math.max(1.5, GetReachProcedure.execute(sukuna));
        this.targetReach = Math.max(1.5, GetReachProcedure.execute(target));
        this.selfInsideSolid = InsideSolidCalculateProcedure.execute(world, sukuna.getX(), sukuna.getY(), sukuna.getZ(), 1.0, sukuna.getBbWidth() + 1.0);
        this.antiInfinityBypass = AntiInfinityProcedure.execute(sukuna);
        this.selfHealth = sukuna.getHealth();
        this.targetHealth = target.getHealth();
        this.selfHealthRatio = selfHealth / Math.max(1.0, sukuna.getMaxHealth());
        this.targetHealthRatio = targetHealth / Math.max(1.0, target.getMaxHealth());
        this.damageTaken = Math.max(0.0, memory.lastHealth - selfHealth);
        double observedDamageDealt = Math.max(0.0, targetStats.lastHealth - targetHealth);
        double actualDamageDealt = OpSukunaBrainTelemetry.recentActualDamageDealt(sukuna, target, 4);
        this.damageDealt = actualDamageDealt >= 0.0 ? actualDamageDealt : observedDamageDealt;
        this.targetHeal = Math.max(0.0, targetHealth - targetStats.lastHealth);
        this.recentDamageRatio = damageTaken / Math.max(1.0, sukuna.getMaxHealth());
        this.damageTakenBurst = Mth.clamp((damageTaken + targetStats.damageTakenAvg) / Math.max(1.0, sukuna.getMaxHealth() * 0.12), 0.0, 1.0);
        this.targetSkill = target.getPersistentData().getDouble("skill");
        this.selfDomain = sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
        DomainCastStatus domainCastStatus = memory.updateSukunaDomainCast(sukuna, nbt, tick, selfDomain);
        this.domainCastRequestedSkill = domainCastStatus.requestedSkill();
        this.domainCastPending = domainCastStatus.pending();
        this.domainCastConfirmed = domainCastStatus.confirmed();
        this.domainCastFailedReason = domainCastStatus.failedReason();
        this.fugaActive = sukuna.hasEffect(JujutsucraftaddonModMobEffects.FUGA.get()) || Math.round(nbt.getDouble("skill")) == (long) OPEN;
        this.fugaCharge = nbt.getDouble("cnt6");
        this.fugaChargeMax = selfHealthRatio <= 0.34 ? 30.0 : 5.0;
        this.fugaDuration = effectDuration(sukuna, JujutsucraftaddonModMobEffects.FUGA.get());
        this.fugaChargeRate = memory.updateFugaCharge(fugaCharge, tick, fugaActive);
        this.fugaHoldTicks = memory.fugaStartTick < 0.0 || !fugaActive ? 0.0 : Math.max(0.0, tick - memory.fugaStartTick);
        this.fugaReleaseReason = memory.fugaLastDecision;
        this.targetDomain = target.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
        this.targetCooldown = target.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get()) || target.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get());
        this.targetUnstable = target.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get());
        this.combatCooldown = LogicCooldownCombatProcedure.execute(sukuna);
        this.passiveReady = LogicStartPassiveProcedure.execute(sukuna);
        this.normalReady = LogicStartProcedure.execute(sukuna);
        this.domainReady = readDomainReady(world, x, y, z, sukuna);
        this.domainRecentlyOpened = tick - memory.lastSukunaDomainTick < 420.0;
        this.domainBlockedByCooldown = domainRecentlyOpened || sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get())
                || sukuna.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get());
        this.domainBlockReason = domainBlockReason(world, x, y, z, sukuna, target, memory, tick, domainReady, domainRecentlyOpened);
        this.gojoTarget = isGojoTarget(target, read);
        this.infinity = target.hasEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.INFINITY.get());
        this.infinitySignal = (infinity && !antiInfinityBypass ? 1.0 : infinity ? 0.25 : 0.0) + (gojoTarget ? 0.35 : 0.0);
        double targetDomainNumber = domainNumber(target);
        this.targetCastingDomain = isDomainCastSignal(target, targetSkill, targetDomainNumber, read);
        this.domainField = OpSukunaDomainFieldScan.scan(world, sukuna);
        this.worldCutCapable = hasWorldCut(sukuna);
        this.worldCutCooldownRemaining = sukuna instanceof SukunaPerfectEntity ? Math.max(0.0, PERFECT_WORLD_CUT_COOLDOWN - (tick - memory.lastWorldCutTick)) : 0.0;
        this.worldCutReady = worldCutCapable && worldCutCooldownRemaining <= 0.0;
        this.worldCutBlockReason = !worldCutCapable ? "not_capable" : !worldCutReady ? "perfect_cooldown" : "ready";
        this.brainDamaged = effectAmplifier(sukuna, JujutsucraftModMobEffects.BRAIN_DAMAGE.get()) >= 2;
        this.isFushiguro = isFushiguroBody(sukuna);
        this.isPerfectMode = isPerfectMode(sukuna);
        this.isMeguna = isMegunaBody(sukuna);
        this.canTransformHeian = OpSukunaEngineCore.canTransformHeian(sukuna);
        this.sukunaForm = OpSukunaEngineCore.sukunaForm(sukuna);
        this.mahoragaWheel = sukuna.getItemBySlot(EquipmentSlot.HEAD).getItem() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get();
        this.mahoragaExist = nbt.getDouble("TenShadowsTechnique14") == -1.0;
        this.canUseTenShadows = isMeguna && nbt.getBoolean("flag_start");
        this.canUseAgito = canUseTenShadows && nbt.getDouble("TenShadowsTechnique13") >= 0.0;
        this.canUseMahoraga = canUseTenShadows && nbt.getDouble("TenShadowsTechnique14") >= 0.0;
        this.canUseTenShadowsDomain = canUseTenShadows && (nbt.getDouble("TenShadowsTechnique14") >= 0.0 || mahoragaExist);
        this.itadoriModulo = isItadoriModulo(target);
        int roundedTargetSkill = (int) Math.round(this.targetSkill);
        this.yujiCounterStance = this.itadoriModulo
                && (roundedTargetSkill == 2105 || roundedTargetSkill == 2106 || roundedTargetSkill == 2108 || roundedTargetSkill == 2118);
        this.targetHwb = target.hasEffect(JujutsucraftaddonModMobEffects.HWB.get());
        this.targetSimpleDomain = read.simpleDomain || target.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.SIMPLE_DOMAIN_MAX.get());
        this.selfRct = sukuna.hasEffect(JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get());
        this.selfSimpleDomain = sukuna.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) || sukuna.hasEffect(JujutsucraftaddonModMobEffects.SIMPLE_DOMAIN_MAX.get()) || sukuna.hasEffect(JujutsucraftaddonModMobEffects.HWB.get());
        this.simpleDomainDuration = simpleDomainDuration(sukuna);
        this.simpleDomainCooldown = effectDuration(sukuna, JujutsucraftModMobEffects.COOLDOWN_TIME_SIMPLE_DOMAIN.get());
        this.simpleDomainExpiresSoon = selfSimpleDomain && simpleDomainDuration <= 70;
        this.canUseRct = OpSukunaEngineCore.canUseRct(sukuna);
        this.canUseSimpleDomain = OpSukunaEngineCore.canUseSimpleDomain(sukuna);
        this.canUseBurnoutRct = OpSukunaEngineCore.canUseBurnoutRct(sukuna);
        this.canUseDomainAmplification = OpSukunaEngineCore.canUseDomainAmplification(sukuna);
        this.targetDodge = target.hasEffect(JujutsucraftaddonModMobEffects.DODGE.get());
        this.targetCounter = target.hasEffect(JujutsucraftaddonModMobEffects.COUNTER.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.COUNTER_CD.get());
        this.targetGuard = target.hasEffect(JujutsucraftModMobEffects.GUARD.get()) || target.getPersistentData().getBoolean("guard");
        this.targetRegen = target.hasEffect(MobEffects.REGENERATION) || read.rct > 0.0;
        this.targetAntiHeal = target.hasEffect(JujutsucraftaddonModMobEffects.ANTI_HEAL.get());
        this.selfAntiHeal = sukuna.hasEffect(JujutsucraftaddonModMobEffects.ANTI_HEAL.get()) || sukuna.hasEffect(JujutsucraftaddonModMobEffects.RCT_CUT.get());
        this.targetAttacking = target.getPersistentData().getBoolean("attack") || target.getPersistentData().getDouble("Damage") != 0.0 || targetSkill != 0.0;
        this.targetNeutralization = target.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get());
        this.targetDomainAmplification = target.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get());
        this.targetPerfectAntiDomain = isSatushi(target) || hasEntityTypeTag(target, "jujutsucraft:can_use_simple_domain")
                && hasEntityTypeTag(target, "jujutsucraft:can_use_hollow_wicker_basket")
                && hasEntityTypeTag(target, "jujutsucraft:can_use_falling_blossom_emotion");
        this.infinityBypassMode = infinityBypassMode(sukuna, target, this);
        this.canBypassInfinityNow = !"NONE".equals(infinityBypassMode);
        this.meleeUnlockReason = canBypassInfinityNow && (gojoTarget || infinity) ? infinityBypassMode : "";
        int selfNeutralizationAmp = effectAmplifier(sukuna, JujutsucraftModMobEffects.NEUTRALIZATION.get());
        boolean gojoDomainSignal = targetDomainNumber == 2.0
                || targetSkill == 220.0
                || ((int) Math.round(Math.abs(targetSkill))) % 100 == 20
                || gojoTarget;
        this.enemyDomainTrap = !selfDomain && (targetDomain && distance < 42.0 || domainField.singleDominantSureHit && domainField.sukunaInsideDominant);
        this.catastrophicDomain = !domainField.clashSuppressed && (targetDomain || targetCastingDomain || enemyDomainTrap || domainField.singleDominantSureHit)
                && (gojoDomainSignal || domainField.dominantVoidLike
                || targetSkill == -999.0
                || nbt.getDouble("skill") == -999.0
                || selfNeutralizationAmp == 12 || selfNeutralizationAmp == 2 || selfNeutralizationAmp >= 37
                || sukuna.hasEffect(JujutsucraftModMobEffects.BRAIN_DAMAGE.get())
                || hasExtremeVoidDebuff(sukuna));
        Vec3 domainCenter = domainField.dominantOwner == null ? target.position() : domainField.dominantCenter;
        Vec3 awayFromDomain = horizontal(sukuna.position().subtract(domainCenter));
        this.domainEscapeVector = awayFromDomain.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : awayFromDomain;
        OpSukunaHazardIntel.DangerScan danger = OpSukunaHazardIntel.scanDanger(world, sukuna);
        OpSukunaHazardIntel.PurpleThreat purple = OpSukunaHazardIntel.scanPurpleThreat(world, sukuna, target, targetSkill, read);
        ThreatScan threatScan = ThreatScan.scan(world, sukuna, target, memory);
        ThreatCandidate targetCandidate = threatScan.candidateOf(target);
        this.targetCombatRelation = targetCandidate == null ? readCombatRelation(world, sukuna, target, read, memory) : targetCandidate.relation;
        this.yujiBurstDuel = itadoriModulo && distance < 20.0 && targetCombatRelation != OpSukunaEngineCore.CombatRelation.PASSIVE_VALID;
        this.targetPowerProfile = targetCandidate == null ? readPowerProfile(sukuna, target, read, targetStats, typeStats) : targetCandidate.powerProfile;
        this.targetKillEstimate = targetCandidate == null ? estimateKill(sukuna, target, targetPowerProfile, targetStats, read) : targetCandidate.killEstimate;
        this.dangerArea = danger.areaRisk;
        this.incomingProjectileRisk = danger.incomingProjectileRisk;
        this.projectileDodgeVector = danger.dodgeVector;
        this.purpleRisk = purple.risk;
        this.purpleThreat = purple.threat;
        this.purpleWindup = purple.windup;
        this.purpleProjectile = purple.projectile;
        this.purpleLineOfFire = purple.lineOfFire;
        this.purpleDodgeVector = purple.dodgeVector;
        this.groupPressure = threatScan.groupPressure;
        this.groupActivePressure = threatScan.groupActivePressure;
        this.groupApexPressure = threatScan.groupApexPressure;
        this.groupEscapeVector = threatScan.escapeVector;
        this.postHealWindow = targetHeal > target.getMaxHealth() * 0.03 || target.hasEffect(MobEffects.REGENERATION) ? 1.0 : 0.0;
        this.targetSkillDanger = scoreTargetSkill(targetSkill, targetStats);
        this.targetSkillStartup = targetSkill != 0.0 && (memory.lastTargetSkill == 0.0 || memory.lastTargetSkill != targetSkill || tick - memory.lastTargetSkillTick <= 8.0);
        this.targetWhiffed = targetSkill == 0.0 && memory.lastTargetSkill != 0.0 && tick - memory.lastTargetSkillTick <= 24.0 && damageTaken <= sukuna.getMaxHealth() * 0.015;
        this.targetOverextended = predictedDistance < distance - 0.85 || target.getDeltaMovement().dot(sukuna.position().subtract(target.position())) > 0.035;
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
        this.adaptation = AdaptationView.create(this);
        this.powerScore = estimatePower(this);
        this.targetThreat = estimateThreat(this);
        this.trivialTarget = isTrivialTarget(this);
        this.pressure = Mth.clamp(powerScore * 0.45 + targetSkillDanger * 0.25 + (1.0 - selfHealthRatio) * 0.3 + dangerArea * 0.35, 0.0, 1.0);
        this.whiffOpportunity = Mth.clamp((targetWhiffed ? 0.85 : 0.0) + (targetOverextended ? 0.45 : 0.0)
                + (targetCooldown ? 0.35 : 0.0) + (targetUnstable ? 0.35 : 0.0), 0.0, 1.0);
        this.opportunity = Mth.clamp((targetCooldown ? 0.35 : 0.0) + (targetUnstable ? 0.35 : 0.0) + (1.0 - targetHealthRatio) * 0.3
                + postHealWindow * 0.25 + whiffOpportunity * 0.45 + (target.hasEffect(JujutsucraftaddonModMobEffects.FATIGUE.get()) ? 0.2 : 0.0), 0.0, 1.0);
        this.idealDistance = adaptation.preferredRange > 1.0
                ? Mth.clamp(adaptation.preferredRange, 8.0, 30.0)
                : idealDistance(archetype, infinitySignal, tankScore, healScore);
        this.targetDomainBreak = target.hasEffect(JujutsucraftaddonModMobEffects.DOMAIN_BREAK.get());
        this.targetEscaping = target.getDeltaMovement().dot(sukuna.position().subtract(target.position())) < -0.03 || predictedDistance > distance + 1.5;
        double threatReach = Math.max(targetReach + 2.5, itadoriModulo ? 13.0 : 8.0);
        this.immediateThreat = Mth.clamp(incomingProjectileRisk * 0.9 + dangerArea * 0.45 + targetSkillDanger * (targetSkillStartup ? 1.15 : 0.55)
                + meleeThreat * close(predictedDistance, threatReach) + damageTakenBurst * 0.5, 0.0, 1.6);
        this.hitConfidence = Mth.clamp(band(predictedDistance, 3.0, worldCutCapable ? 54.0 : 38.0) + whiffOpportunity * 0.35
                + (targetGuard || targetDodge || targetCounter ? -0.25 : 0.0) + (targetEscaping ? -0.15 : 0.0), 0.0, 1.0);
        this.rctLimit = rctLimit(sukuna);
        this.rctStrain = Mth.clamp(nbt.getDouble("cnt_reverse_lim") / Math.max(1.0, rctLimit), 0.0, 1.2);
        this.rctLevel = rctLevel(sukuna);
        this.brainDamageLevel = brainDamageLevel(sukuna);
        this.incomingKillRisk = Mth.clamp(immediateThreat * 0.45 + targetSkillDanger * 0.5 + damageTakenBurst * 0.75
                + groupActivePressure * 0.35 + groupApexPressure * 0.45 + (itadoriModulo ? 0.35 : 0.0), 0.0, 1.6);
        this.rctFatigued = rctStrain > 0.68 || brainDamageLevel >= 4.0 || selfAntiHeal
                || (rctLevel <= 1.0 && selfHealthRatio < 0.55 && damageTakenBurst > 0.2);
        double projectedRct = (selfRct || canUseRct && !selfAntiHeal) ? sukuna.getMaxHealth() * (rctFatigued ? 0.012 : 0.022) : 0.0;
        this.healthLosingRace = memory.recentDamageWindow > Math.max(projectedRct + memory.recentHealWindow, sukuna.getMaxHealth() * 0.055)
                || damageTaken > Math.max(projectedRct * 1.5, sukuna.getMaxHealth() * 0.08)
                || memory.burstChainTicks >= 4.0 && memory.recentDamageWindow > memory.recentHealWindow + sukuna.getMaxHealth() * 0.035;
        this.blackFlashChain = memory.blackFlashChainTicks >= 2.0
                || (damageTakenBurst > 0.48 && distance < 9.0 && ("BLACK_FLASH_USER".equals(archetype) || itadoriModulo
                || target.hasEffect(JujutsucraftaddonModMobEffects.BLACK_FLASH_CUT.get())
                || target.hasEffect(JujutsucraftaddonModMobEffects.KOKUSEN_EFFECT.get())));
        this.survivalUrgency = Mth.clamp((1.0 - selfHealthRatio) * 0.55 + incomingKillRisk * 0.55 + rctStrain * 0.35
                + (rctFatigued ? 0.35 : 0.0) + (targetPowerProfile.high ? 0.12 : 0.0)
                + (healthLosingRace ? 0.22 : 0.0) + (blackFlashChain ? 0.18 : 0.0), 0.0, 1.6);
        this.blackFlashRecoveryValue = Mth.clamp(rctStrain * 0.65 + (brainDamageLevel >= 3.0 ? 0.25 : 0.0)
                + close(predictedDistance, 6.5) * 0.35 + whiffOpportunity * 0.35 + (targetCooldown || targetUnstable ? 0.25 : 0.0), 0.0, 1.35);
        this.clearShot = hasClearShot(world, sukuna, target, Math.max(1.5, target.getBbWidth() * 0.85));
        this.pathBlocked = selfInsideSolid || pathBlocked(world, sukuna, target.position());
        this.antiDomainGapImminent = domainField.singleDominantSureHit && (simpleDomainExpiresSoon || !selfSimpleDomain && simpleDomainCooldown > 0);
        this.domainEscapeWindow = (targetCastingDomain && !targetDomain || domainField.singleDominantSureHit)
                && !read.barrierlessDomain && !domainField.dominantBarrierless && !selfDomain && hasDomainEscapeCandidate(this)
                && distance < (catastrophicDomain ? 38.0 : 30.0);
        this.openShotQuality = clearShot
                ? Mth.clamp(1.0 - close(distance, 5.0) * 0.25 + (targetOverextended ? 0.12 : 0.0), 0.0, 1.0)
                : 0.0;
        this.survivalMode = rctFatigued && (targetThreat > 0.45 || incomingKillRisk > 0.55 || groupActivePressure > 0.35);
        this.domainDeathSpiral = catastrophicDomain && enemyDomainTrap
                && (antiDomainGapImminent || healthLosingRace || selfHealthRatio < 0.42 || brainDamageLevel >= 2.0 || memory.domainTrapTicks > 35.0);
        this.voidExposure = Mth.clamp((catastrophicDomain ? 0.55 : 0.0)
                + (enemyDomainTrap ? 0.35 : 0.0)
                + (targetNeutralization ? 0.25 : 0.0)
                + (hasExtremeVoidDebuff(sukuna) ? 0.45 : 0.0)
                + ((gojoTarget && (targetDomain || targetCastingDomain)) || domainField.dominantVoidLike ? 0.35 : 0.0)
                - (selfDomain || selfSimpleDomain ? 0.45 : 0.0), 0.0, 1.6);
        this.lethalForecast = selfHealthRatio < 0.22
                || (selfHealthRatio < 0.42 && (damageTakenBurst > 0.55 || targetSkillDanger > 0.75 || targetDomain))
                || damageTaken >= selfHealth * 0.55
                || (rctFatigued && selfHealthRatio < 0.48 && incomingKillRisk > 0.78)
                || (selfHealthRatio < 0.62 && incomingKillRisk > 1.18)
                || (domainDeathSpiral)
                || (blackFlashChain && healthLosingRace && selfHealthRatio < 0.62);
        this.killConfirm = targetHealthRatio < 0.34
                || (targetHealth <= 32.0 && (targetCooldown || targetUnstable || postHealWindow > 0.0))
                || (targetAntiHeal && targetHealthRatio < 0.52);
        int baseStrafe = ((sukuna.getUUID().getLeastSignificantBits() ^ target.getUUID().getMostSignificantBits()) & 1L) == 0L ? 1 : -1;
        this.strafeSide = memory.strafeSide == 0 ? baseStrafe : memory.strafeSide;
        this.domainAssessment = OpSukunaDomainTactics.DomainTactics.assess(this);
        this.domainProfile = OpSukunaDomainTactics.DomainProfile.create(this);
        this.domainDeliberation = OpSukunaDomainTactics.DomainDeliberation.choose(this);
        this.domainIntent = domainAssessment.intent;
        this.domainConfidence = domainAssessment.confidence;
        this.heianEmergencyReason = heianReason(this);
    }

    double killPressure(double expectedDamageRatio) {
        return targetHealthRatio <= expectedDamageRatio ? 1.0 : Mth.clamp((expectedDamageRatio * 1.5 - targetHealthRatio) / Math.max(0.01, expectedDamageRatio * 1.5), 0.0, 1.0);
    }

    double expensiveWastePenalty(double minimumThreat) {
        if (targetThreat >= minimumThreat || targetDomain || targetCastingDomain || domainField.singleDominantSureHit || infinitySignal > 0.0) return 0.0;
        return Mth.clamp((minimumThreat - targetThreat) * 2.2, 0.0, 1.3);
    }
}
