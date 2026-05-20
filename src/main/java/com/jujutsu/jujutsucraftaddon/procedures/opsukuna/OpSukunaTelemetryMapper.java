package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import com.jujutsu.jujutsucraftaddon.util.OpSukunaBrainTelemetry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

final class OpSukunaTelemetryMapper {
    private OpSukunaTelemetryMapper() {
    }

static void recordDecision(LevelAccessor world, LivingEntity sukuna, LivingEntity target, OpSukunaSnapshot s, OpSukunaAction best, String topScores) {
    if (!OpSukunaBrainTelemetry.enabled(world)) {
        return;
    }
    OpSukunaBrainTelemetry.Decision decision = new OpSukunaBrainTelemetry.Decision();
    decision.action = best.name;
    decision.kind = best.kind.name();
    decision.aiMode = aiMode(s, best.name, best.kind.name());
    decision.topScores = topScores;
    decision.score = best.score;
    decision.adjustedScore = OpSukunaEngineCore.adjustedScore(s, best);
    decision.skill = best.skill;
    decision.selfHealth = s.selfHealth;
    decision.targetHealth = s.targetHealth;
    decision.selfHealthRatio = s.selfHealthRatio;
    decision.targetHealthRatio = s.targetHealthRatio;
    decision.distance = s.distance;
    decision.hitboxDistance = s.hitboxDistance;
    decision.centerDistance = s.centerDistance;
    decision.predictedDistance = s.predictedDistance;
    decision.damageDealt = s.damageDealt;
    decision.damageTaken = s.damageTaken;
    decision.targetSkill = s.targetSkill;
    decision.archetype = s.archetype;
    decision.techniqueKey = s.read.techniqueKey();
    decision.primaryTechnique = s.read.primaryTechnique;
    decision.secondaryTechnique = s.read.secondaryTechnique;
    decision.targetThreat = s.targetThreat;
    decision.targetPower = s.targetPowerProfile.score;
    decision.meleeThreat = s.meleeThreat;
    decision.rangeThreat = s.rangeThreat;
    decision.pressure = s.pressure;
    decision.opportunity = s.opportunity;
    decision.immediateThreat = s.immediateThreat;
    decision.survivalUrgency = s.survivalUrgency;
    decision.incomingKillRisk = s.incomingKillRisk;
    decision.hitConfidence = s.hitConfidence;
    decision.whiffOpportunity = s.whiffOpportunity;
    decision.stuckLevel = s.stuckLevel;
    decision.groupPressure = s.groupPressure;
    decision.groupActivePressure = s.groupActivePressure;
    decision.groupApexPressure = s.groupApexPressure;
    decision.rctStrain = s.rctStrain;
    decision.brainDamage = s.brainDamageLevel;
    decision.idealDistance = s.idealDistance;
    decision.gojo = s.gojoTarget;
    decision.targetDomain = s.targetDomain;
    decision.targetCastingDomain = s.targetCastingDomain;
    decision.targetDomainSignalReason = s.targetDomainSignalReason;
    decision.targetCooldown = s.targetCooldown;
    decision.targetUnstable = s.targetUnstable;
    decision.targetDodge = s.targetDodge;
    decision.targetCounter = s.targetCounter;
    decision.targetGuard = s.targetGuard;
    decision.targetEscaping = s.targetEscaping;
    decision.targetWhiffed = s.targetWhiffed;
    decision.targetOverextended = s.targetOverextended;
    decision.clearShot = s.clearShot;
    decision.pathBlocked = s.pathBlocked;
    decision.normalReady = s.normalReady;
    decision.passiveReady = s.passiveReady;
    decision.domainReady = s.domainReady;
    decision.domainBlockedByCooldown = s.domainBlockedByCooldown;
    decision.domainBlockReason = s.domainBlockReason;
    decision.domainRecentlyOpened = s.domainRecentlyOpened;
    decision.worldCutCapable = s.worldCutCapable;
    decision.worldCutReady = s.worldCutReady;
    decision.worldCutCooldown = s.worldCutCooldownRemaining;
    decision.worldCutBlockReason = s.worldCutBlockReason;
    decision.trivialTarget = s.trivialTarget;
    decision.sukunaForm = s.sukunaForm;
    decision.canTransformHeian = s.canTransformHeian;
    decision.heianEmergencyReason = s.heianEmergencyReason;
    decision.tenShadowsAvailable = s.canUseTenShadows;
    decision.mahoragaAvailable = s.canUseMahoraga || s.mahoragaExist;
    decision.enemyDomainTrap = s.enemyDomainTrap;
    decision.domainEscapeWindow = s.domainEscapeWindow;
    decision.domainResponseTicks = s.memory.firstDomainThreatTick < 0.0 ? -1.0 : s.tick - s.memory.firstDomainThreatTick;
    decision.activeEnemyDomains = s.domainField.activeEnemyDomains;
    decision.activeVoidDomains = s.domainField.activeVoidDomains;
    decision.domainClashSuppressed = s.domainField.clashSuppressed;
    decision.singleDominantSureHit = s.domainField.singleDominantSureHit;
    decision.simpleDomainDuration = s.simpleDomainDuration;
    decision.simpleDomainCooldown = s.simpleDomainCooldown;
    decision.antiDomainGapImminent = s.antiDomainGapImminent;
    decision.dominantDomainOwner = s.domainField.dominantOwnerKey;
    decision.dominantDomainProfile = s.domainField.dominantProfile;
    decision.purpleThreat = s.purpleThreat;
    decision.purpleRisk = s.purpleRisk;
    decision.purpleEvade = "PURPLE_EVADE".equals(best.name) || "PURPLE_BACKSTEP".equals(best.name);
    decision.purpleHit = s.purpleThreat && s.damageTaken > sukuna.getMaxHealth() * 0.08;
    decision.purpleAvoided = s.memory.pendingPurpleResponseTick >= 0.0 && s.tick - s.memory.pendingPurpleResponseTick <= 45.0 && s.damageTaken <= sukuna.getMaxHealth() * 0.02;
    decision.stuck = s.stuckLevel > 0.62;
    decision.infinityWaste = s.infinitySignal > 0.0 && !s.selfDomain && !best.worldCut && best.skill != 0.0
            && best.kind != OpSukunaActionKind.DOMAIN_AMPLIFICATION && best.kind != OpSukunaActionKind.TEN_SHADOWS && best.kind != OpSukunaActionKind.DOMAIN;
    decision.adaptationConfidence = s.adaptation.confidence;
    decision.trapPressure = s.adaptation.trapMastery;
    decision.evasionPressure = s.adaptation.antiEvasionNeed;
    decision.rangePressure = s.adaptation.rangeKiteNeed;
    decision.domainPlan = s.domainAssessment.plan;
    decision.domainClearValue = s.domainAssessment.clearValue;
    decision.domainSustainCost = s.domainAssessment.sustainCost;
    decision.domainRecoveryRisk = s.domainAssessment.recoveryRisk;
    decision.domainCounterRisk = s.domainAssessment.counterRisk;
    decision.domainCastScore = s.domainAssessment.castScore;
    decision.domainHwbScore = s.domainAssessment.hwbScore;
    decision.domainPunishScore = s.domainAssessment.punishScore;
    decision.domainScore = s.domainAssessment.domainScore;
    decision.domainDeliberation = s.domainDeliberation.reason;
    decision.domainPhase = OpSukunaEngineCore.domainPhase(s);
    decision.domainCommitDeadline = OpSukunaEngineCore.domainCommitDeadline(s);
    decision.domainGateReason = s.domainBlockReason;
    decision.selfDomain = s.selfDomain;
    decision.domainCastRequestedSkill = best.kind == OpSukunaActionKind.DOMAIN ? best.skill : s.domainCastRequestedSkill;
    decision.domainCastPending = best.kind == OpSukunaActionKind.DOMAIN || s.domainCastPending;
    decision.domainCastConfirmed = s.domainCastConfirmed;
    decision.domainCastFailedReason = s.domainCastFailedReason;
    decision.domainProfile = s.domainProfile.name;
    decision.chosenDomainOption = s.domainDeliberation.option.name();
    decision.escapeFeasible = s.domainDeliberation.escapeFeasible;
    decision.worldCutDomainValue = s.domainDeliberation.worldCutValue;
    decision.counterDomainValue = s.domainDeliberation.counterDomainValue;
    decision.tankDomainValue = s.domainDeliberation.tankValue;
    decision.infinityBypassMode = s.infinityBypassMode;
    decision.meleeUnlockReason = s.meleeUnlockReason;
    decision.preferredRange = s.adaptation.preferredRange;
    decision.safePunishWindow = s.adaptation.safePunishWindow;
    decision.learnedBurstRisk = s.adaptation.learnedBurstRisk;
    decision.rangeActionStreak = s.memory.rangeActionStreak;
    decision.noImpactActionStreak = s.memory.noImpactActionStreak;
    decision.yujiBurstDuel = s.yujiBurstDuel;
    decision.actionStarted = s.actionStarted;
    decision.blockReason = s.blockReason;
    decision.fallbackAction = s.fallbackAction;
    decision.voidExposure = s.voidExposure;
    decision.simpleDomainReady = s.canUseSimpleDomain;
    decision.simpleDomainKeyBlocked = s.nbt.getBoolean("JJKUR_OP_AI_SIMPLE_DOMAIN_KEY_BLOCKED");
    decision.simpleDomainKeySource = s.nbt.getString("JJKUR_OP_AI_SIMPLE_DOMAIN_KEY_SOURCE");
    decision.domainAmplificationReady = s.canUseDomainAmplification;
    decision.rctReady = s.canUseRct;
    decision.shrineMode = s.selfDomain;
    decision.fugaActive = s.fugaActive;
    decision.fugaCharge = s.fugaCharge;
    decision.fugaChargeMax = s.fugaChargeMax;
    decision.fugaChargeRate = s.fugaChargeRate;
    decision.fugaHoldTicks = s.fugaHoldTicks;
    decision.fugaReleaseReason = s.fugaReleaseReason;
    OpSukunaBrainTelemetry.recordDecision(world, sukuna, target, decision);
    s.nbt.putBoolean("JJKUR_OP_AI_SIMPLE_DOMAIN_KEY_BLOCKED", false);
    s.nbt.putString("JJKUR_OP_AI_SIMPLE_DOMAIN_KEY_SOURCE", "");
}

static void recordExecutionState(LevelAccessor world, LivingEntity sukuna, LivingEntity target, OpSukunaSnapshot s, String reason) {
    if (!OpSukunaBrainTelemetry.enabled(world) || !OpSukunaBrainTelemetry.hasBenchmarkContext(sukuna)) {
        return;
    }
    if (sukuna.tickCount % 5 != 0) {
        return;
    }
    OpSukunaBrainTelemetry.Decision decision = new OpSukunaBrainTelemetry.Decision();
    double activeSkill = s.nbt.getDouble("skill");
    String last = s.memory.lastAction == null || s.memory.lastAction.isEmpty() ? "unknown" : s.memory.lastAction;
    decision.action = "EXECUTING_SKILL_" + (long) activeSkill + "_AFTER_" + last;
    decision.kind = "EXECUTING";
    decision.aiMode = aiMode(s, decision.action, decision.kind);
    decision.topScores = "reason=" + reason + ",active_skill=" + activeSkill + ",last_action=" + last
            + ",attack=" + s.nbt.getBoolean("attack") + ",damage_nbt=" + s.nbt.getDouble("Damage");
    decision.skill = activeSkill;
    decision.selfHealth = s.selfHealth;
    decision.targetHealth = s.targetHealth;
    decision.selfHealthRatio = s.selfHealthRatio;
    decision.targetHealthRatio = s.targetHealthRatio;
    decision.distance = s.distance;
    decision.hitboxDistance = s.hitboxDistance;
    decision.centerDistance = s.centerDistance;
    decision.predictedDistance = s.predictedDistance;
    decision.damageDealt = s.damageDealt;
    decision.damageTaken = s.damageTaken;
    decision.targetSkill = s.targetSkill;
    decision.archetype = s.archetype;
    decision.techniqueKey = s.read.techniqueKey();
    decision.primaryTechnique = s.read.primaryTechnique;
    decision.secondaryTechnique = s.read.secondaryTechnique;
    decision.targetThreat = s.targetThreat;
    decision.targetPower = s.targetPowerProfile.score;
    decision.meleeThreat = s.meleeThreat;
    decision.rangeThreat = s.rangeThreat;
    decision.pressure = s.pressure;
    decision.opportunity = s.opportunity;
    decision.immediateThreat = s.immediateThreat;
    decision.survivalUrgency = s.survivalUrgency;
    decision.incomingKillRisk = s.incomingKillRisk;
    decision.hitConfidence = s.hitConfidence;
    decision.whiffOpportunity = s.whiffOpportunity;
    decision.stuckLevel = s.stuckLevel;
    decision.groupPressure = s.groupPressure;
    decision.groupActivePressure = s.groupActivePressure;
    decision.groupApexPressure = s.groupApexPressure;
    decision.rctStrain = s.rctStrain;
    decision.brainDamage = s.brainDamageLevel;
    decision.idealDistance = s.idealDistance;
    decision.gojo = s.gojoTarget;
    decision.targetDomain = s.targetDomain;
    decision.targetCastingDomain = s.targetCastingDomain;
    decision.targetDomainSignalReason = s.targetDomainSignalReason;
    decision.targetCooldown = s.targetCooldown;
    decision.targetUnstable = s.targetUnstable;
    decision.targetDodge = s.targetDodge;
    decision.targetCounter = s.targetCounter;
    decision.targetGuard = s.targetGuard;
    decision.targetEscaping = s.targetEscaping;
    decision.targetWhiffed = s.targetWhiffed;
    decision.targetOverextended = s.targetOverextended;
    decision.clearShot = s.clearShot;
    decision.pathBlocked = s.pathBlocked;
    decision.normalReady = s.normalReady;
    decision.passiveReady = s.passiveReady;
    decision.domainReady = s.domainReady;
    decision.domainBlockedByCooldown = s.domainBlockedByCooldown;
    decision.domainBlockReason = s.domainBlockReason;
    decision.domainRecentlyOpened = s.domainRecentlyOpened;
    decision.selfDomain = s.selfDomain;
    decision.domainCastRequestedSkill = s.domainCastRequestedSkill;
    decision.domainCastPending = s.domainCastPending;
    decision.domainCastConfirmed = s.domainCastConfirmed;
    decision.domainCastFailedReason = s.domainCastFailedReason;
    decision.worldCutCapable = s.worldCutCapable;
    decision.worldCutReady = s.worldCutReady;
    decision.worldCutCooldown = s.worldCutCooldownRemaining;
    decision.worldCutBlockReason = s.worldCutBlockReason;
    decision.trivialTarget = s.trivialTarget;
    decision.sukunaForm = s.sukunaForm;
    decision.canTransformHeian = s.canTransformHeian;
    decision.heianEmergencyReason = s.heianEmergencyReason;
    decision.tenShadowsAvailable = s.canUseTenShadows;
    decision.mahoragaAvailable = s.canUseMahoraga || s.mahoragaExist;
    decision.enemyDomainTrap = s.enemyDomainTrap;
    decision.domainEscapeWindow = s.domainEscapeWindow;
    decision.domainResponseTicks = s.memory.firstDomainThreatTick < 0.0 ? -1.0 : s.tick - s.memory.firstDomainThreatTick;
    decision.activeEnemyDomains = s.domainField.activeEnemyDomains;
    decision.activeVoidDomains = s.domainField.activeVoidDomains;
    decision.domainClashSuppressed = s.domainField.clashSuppressed;
    decision.singleDominantSureHit = s.domainField.singleDominantSureHit;
    decision.simpleDomainDuration = s.simpleDomainDuration;
    decision.simpleDomainCooldown = s.simpleDomainCooldown;
    decision.antiDomainGapImminent = s.antiDomainGapImminent;
    decision.dominantDomainOwner = s.domainField.dominantOwnerKey;
    decision.dominantDomainProfile = s.domainField.dominantProfile;
    decision.purpleThreat = s.purpleThreat;
    decision.purpleRisk = s.purpleRisk;
    decision.stuck = s.stuckLevel > 0.62;
    decision.adaptationConfidence = s.adaptation.confidence;
    decision.trapPressure = s.adaptation.trapMastery;
    decision.evasionPressure = s.adaptation.antiEvasionNeed;
    decision.rangePressure = s.adaptation.rangeKiteNeed;
    decision.domainPlan = s.domainAssessment.plan;
    decision.domainClearValue = s.domainAssessment.clearValue;
    decision.domainSustainCost = s.domainAssessment.sustainCost;
    decision.domainRecoveryRisk = s.domainAssessment.recoveryRisk;
    decision.domainCounterRisk = s.domainAssessment.counterRisk;
    decision.domainCastScore = s.domainAssessment.castScore;
    decision.domainHwbScore = s.domainAssessment.hwbScore;
    decision.domainPunishScore = s.domainAssessment.punishScore;
    decision.domainScore = s.domainAssessment.domainScore;
    decision.domainDeliberation = s.domainDeliberation.reason;
    decision.domainPhase = OpSukunaEngineCore.domainPhase(s);
    decision.domainCommitDeadline = OpSukunaEngineCore.domainCommitDeadline(s);
    decision.domainGateReason = s.domainBlockReason;
    decision.domainProfile = s.domainProfile.name;
    decision.chosenDomainOption = s.domainDeliberation.option.name();
    decision.escapeFeasible = s.domainDeliberation.escapeFeasible;
    decision.worldCutDomainValue = s.domainDeliberation.worldCutValue;
    decision.counterDomainValue = s.domainDeliberation.counterDomainValue;
    decision.tankDomainValue = s.domainDeliberation.tankValue;
    decision.infinityBypassMode = s.infinityBypassMode;
    decision.meleeUnlockReason = s.meleeUnlockReason;
    decision.preferredRange = s.adaptation.preferredRange;
    decision.safePunishWindow = s.adaptation.safePunishWindow;
    decision.learnedBurstRisk = s.adaptation.learnedBurstRisk;
    decision.rangeActionStreak = s.memory.rangeActionStreak;
    decision.noImpactActionStreak = s.memory.noImpactActionStreak;
    decision.yujiBurstDuel = s.yujiBurstDuel;
    decision.actionStarted = true;
    decision.blockReason = "executing_active_skill";
    decision.fallbackAction = "";
    decision.voidExposure = s.voidExposure;
    decision.simpleDomainReady = s.canUseSimpleDomain;
    decision.simpleDomainKeyBlocked = s.nbt.getBoolean("JJKUR_OP_AI_SIMPLE_DOMAIN_KEY_BLOCKED");
    decision.simpleDomainKeySource = s.nbt.getString("JJKUR_OP_AI_SIMPLE_DOMAIN_KEY_SOURCE");
    decision.domainAmplificationReady = s.canUseDomainAmplification;
    decision.rctReady = s.canUseRct;
    decision.shrineMode = s.selfDomain;
    decision.fugaActive = s.fugaActive;
    decision.fugaCharge = s.fugaCharge;
    decision.fugaChargeMax = s.fugaChargeMax;
    decision.fugaChargeRate = s.fugaChargeRate;
    decision.fugaHoldTicks = s.fugaHoldTicks;
    decision.fugaReleaseReason = s.fugaReleaseReason;
    OpSukunaBrainTelemetry.recordDecision(world, sukuna, target, decision);
}

private static String aiMode(OpSukunaSnapshot s, String action, String kind) {
    String name = action == null ? "" : action;
    String actionKind = kind == null ? "" : kind;
    if (s.enemyDomainTrap || s.targetCastingDomain || s.domainField.activeEnemyDomains > 0.0 || s.voidExposure > 0.0) {
        return "ANTI_DOMAIN";
    }
    if (s.survivalUrgency > 0.62 || s.incomingKillRisk > 0.55 || name.contains("RCT") || name.contains("GUARD")) {
        return "SURVIVAL";
    }
    if ("EXECUTING".equals(actionKind)) {
        return "EXECUTING";
    }
    if (name.startsWith("OPENING_")) {
        return "OPENING";
    }
    if (name.contains("CHASE") || name.contains("Reposition") || name.contains("Strafe") || name.contains("Move")) {
        return "CONTACT";
    }
    if ("MELEE".equals(actionKind) || "TECHNIQUE".equals(actionKind) || name.contains("Cleave") || name.contains("Dismantle")
            || name.contains("Fuga") || name.contains("WorldCut") || name.contains("Calculate")) {
        return "BURST";
    }
    if ("DOMAIN".equals(actionKind)) {
        return "DOMAIN";
    }
    return "NEUTRAL";
}

}
