package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class OpSukunaBrainMemory {
    final Map<String, OpSukunaCombatStats> targets = new HashMap<>();
    final Map<String, OpSukunaCombatStats> types = new HashMap<>();
    final Map<String, OpSukunaCombatStats> techniques = new HashMap<>();
    final Map<String, OpSukunaCombatStats> archetypes = new HashMap<>();
    final Map<String, Double> actionValues = new HashMap<>();
    double lastHealth;
    double lastBackstepTick = -1000.0;
    double lastProjectileDodgeTick = -1000.0;
    double lastMoveTick = -1000.0;
    double lastGuardTick = -1000.0;
    double lastGuardTimingTick = -1000.0;
    double lastEvasiveBackstepTick = -1000.0;
    double lastRecoveryWindowTick = -1000.0;
    double lastSimpleDomainTick = -1000.0;
    double lastBurnoutRctTick = -1000.0;
    double lastDomainAmplificationTick = -1000.0;
    double lastSukunaDomainTick = -1000.0;
    double pendingSukunaDomainCastTick = -1000.0;
    double pendingSukunaDomainCastSkill = 0.0;
    double lastWorldCutTick = -1000.0;
    double lastPurpleEvadeTick = -1000.0;
    double pendingPurpleResponseTick = -1000.0;
    double fugaStartTick = -1.0;
    double lastFugaCharge = 0.0;
    double lastFugaChargeTick = -1.0;
    double fugaChargeRate = 0.0;
    double firstDomainThreatTick = -1.0;
    double lastTargetSwitchTick = -1000.0;
    double lastTargetSkill;
    double lastTargetSkillTick = -1000.0;
    double lastTargetDistance;
    double lastPosX = Double.NaN;
    double lastPosY = Double.NaN;
    double lastPosZ = Double.NaN;
    double stuckTicks;
    double lastDestinationX = Double.NaN;
    double lastDestinationY = Double.NaN;
    double lastDestinationZ = Double.NaN;
    double lastEscapeTick = -1000.0;
    int strafeSide;
    double megunaHighThreatTicks;
    double groupPressure;
    double recentDamageWindow;
    double recentHealWindow;
    double burstChainTicks;
    double domainTrapTicks;
    double blackFlashChainTicks;
    int rangeActionStreak;
    int noImpactActionStreak;
    boolean heianResetUsed;
    String fugaLastDecision = "";
    String lastAction = "None";
    String lastActionTarget = "";
    String lastPrimaryTarget = "";
    int lastActionStreak;

    void requestSukunaDomainCast(double tick, double skill) {
        pendingSukunaDomainCastTick = tick;
        pendingSukunaDomainCastSkill = skill;
    }

    OpSukunaEngine.DomainCastStatus updateSukunaDomainCast(LivingEntity sukuna, CompoundTag nbt, double tick, boolean selfDomain) {
        boolean hasPending = pendingSukunaDomainCastTick > -999.0;
        if (!hasPending) {
            return new OpSukunaEngine.DomainCastStatus(0.0, false, selfDomain, "");
        }
        double requestedSkill = pendingSukunaDomainCastSkill;
        if (selfDomain) {
            lastSukunaDomainTick = tick;
            pendingSukunaDomainCastTick = -1000.0;
            pendingSukunaDomainCastSkill = 0.0;
            return new OpSukunaEngine.DomainCastStatus(requestedSkill, false, true, "");
        }
        double currentSkill = nbt.getDouble("skill");
        boolean stillCasting = currentSkill == requestedSkill && sukuna.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get());
        if (tick - pendingSukunaDomainCastTick <= 40.0 && stillCasting) {
            return new OpSukunaEngine.DomainCastStatus(requestedSkill, true, false, "");
        }
        String failedReason = OpSukunaEngineCore.domainCastFailureReason(sukuna, nbt);
        pendingSukunaDomainCastTick = -1000.0;
        pendingSukunaDomainCastSkill = 0.0;
        return new OpSukunaEngine.DomainCastStatus(requestedSkill, false, false, failedReason);
    }

    OpSukunaCombatStats target(String key) {
        return targets.computeIfAbsent(key, unused -> new OpSukunaCombatStats());
    }

    OpSukunaCombatStats type(String key) {
        return types.computeIfAbsent(key, unused -> new OpSukunaCombatStats());
    }

    OpSukunaCombatStats technique(String key) {
        return techniques.computeIfAbsent(key, unused -> new OpSukunaCombatStats());
    }

    OpSukunaCombatStats archetype(String key) {
        return archetypes.computeIfAbsent(key, unused -> new OpSukunaCombatStats());
    }

    double updateFugaCharge(double charge, double tick, boolean active) {
        if (!active) {
            fugaStartTick = -1.0;
            lastFugaCharge = charge;
            lastFugaChargeTick = tick;
            fugaChargeRate = 0.0;
            fugaLastDecision = "";
            return 0.0;
        }
        if (fugaStartTick < 0.0) {
            fugaStartTick = tick;
        }
        if (lastFugaChargeTick >= 0.0 && tick > lastFugaChargeTick) {
            double instant = Math.max(0.0, charge - lastFugaCharge) / Math.max(1.0, tick - lastFugaChargeTick);
            fugaChargeRate = fugaChargeRate <= 0.0 ? instant : fugaChargeRate * 0.72 + instant * 0.28;
        }
        lastFugaCharge = charge;
        lastFugaChargeTick = tick;
        return fugaChargeRate;
    }

    void observe(OpSukunaSnapshot s) {
        observeActionValue(s);
        observeStats(s.targetStats, s, 1.0);
        observeStats(s.typeStats, s, 0.35);
        observeStats(s.techniqueStats, s, 0.35);
        observeStats(s.archetypeStats, s, 0.35);
        recentDamageWindow = recentDamageWindow * 0.72 + s.damageTaken;
        recentHealWindow = recentHealWindow * 0.72 + (Math.max(0.0, s.selfHealth - lastHealth));
        boolean burst = s.damageTakenBurst > 0.42 || s.damageTaken > s.sukuna.getMaxHealth() * 0.055;
        burstChainTicks = burst ? Math.min(40.0, burstChainTicks + 1.0) : Math.max(0.0, burstChainTicks - 1.6);
        boolean blackFlashSignal = burst && s.distance < 9.5 && ("BLACK_FLASH_USER".equals(s.archetype) || s.itadoriModulo
                || s.target.hasEffect(JujutsucraftaddonModMobEffects.BLACK_FLASH_CUT.get())
                || s.target.hasEffect(JujutsucraftaddonModMobEffects.KOKUSEN_EFFECT.get()));
        blackFlashChainTicks = blackFlashSignal ? Math.min(30.0, blackFlashChainTicks + 1.0) : Math.max(0.0, blackFlashChainTicks - 1.0);
        domainTrapTicks = s.enemyDomainTrap ? Math.min(120.0, domainTrapTicks + 1.0) : Math.max(0.0, domainTrapTicks - 2.0);
        if (OpSukunaEngineCore.hasConcreteDomainSignal(s)) {
            if (firstDomainThreatTick < 0.0) {
                firstDomainThreatTick = s.tick;
            }
        } else {
            firstDomainThreatTick = -1.0;
        }
        if (s.purpleThreat && pendingPurpleResponseTick < 0.0) {
            pendingPurpleResponseTick = s.tick;
        } else if (!s.purpleThreat && pendingPurpleResponseTick >= 0.0 && s.tick - pendingPurpleResponseTick > 45.0) {
            pendingPurpleResponseTick = -1000.0;
        }
        lastHealth = s.selfHealth;
        groupPressure = s.groupPressure;
        megunaHighThreatTicks = s.isMeguna && s.targetThreat > 0.62 && !s.trivialTarget ? megunaHighThreatTicks + 1.0 : Math.max(0.0, megunaHighThreatTicks - 2.0);
        if (s.targetDomain && s.tick - lastSukunaDomainTick < 600.0) {
            s.targetStats.counterDomainAfterSukuna += 1.0;
            s.typeStats.counterDomainAfterSukuna += 0.35;
            s.techniqueStats.counterDomainAfterSukuna += 0.35;
            s.archetypeStats.counterDomainAfterSukuna += 0.35;
        }
        if (s.targetSkill != 0.0) {
            if (lastTargetSkill != s.targetSkill) {
                lastTargetSkillTick = s.tick;
            }
            lastTargetSkill = s.targetSkill;
        } else if (lastTargetSkill != 0.0 && s.tick - lastTargetSkillTick > 24.0) {
            lastTargetSkill = 0.0;
        }
        lastTargetDistance = s.distance;
    }

    double updateStuck(LivingEntity sukuna, double tick) {
        Vec3 pos = sukuna.position();
        if (!Double.isFinite(lastPosX) || !Double.isFinite(lastPosY) || !Double.isFinite(lastPosZ)) {
            lastPosX = pos.x;
            lastPosY = pos.y;
            lastPosZ = pos.z;
            return 0.0;
        }
        double dx = pos.x - lastPosX;
        double dz = pos.z - lastPosZ;
        double movedSqr = dx * dx + dz * dz;
        boolean recentlyTriedMove = tick - lastMoveTick <= 18.0;
        boolean hasDestination = Double.isFinite(lastDestinationX) && new Vec3(lastDestinationX, lastDestinationY, lastDestinationZ).distanceToSqr(pos) > 2.25;
        boolean blockedMotion = sukuna.horizontalCollision || sukuna.getDeltaMovement().horizontalDistanceSqr() < 0.0025;
        if ((recentlyTriedMove || hasDestination) && movedSqr < 0.018 && blockedMotion) {
            stuckTicks = Math.min(28.0, stuckTicks + 1.0);
        } else {
            stuckTicks = Math.max(0.0, stuckTicks - 2.0);
        }
        lastPosX = pos.x;
        lastPosY = pos.y;
        lastPosZ = pos.z;
        return Mth.clamp(stuckTicks / 12.0, 0.0, 1.0);
    }

    void rememberDestination(Vec3 destination) {
        lastDestinationX = destination.x;
        lastDestinationY = destination.y;
        lastDestinationZ = destination.z;
    }

    double actionBias(String action) {
        double repeatPenalty = action.equals(lastAction) ? Math.min(0.55, lastActionStreak * 0.09) : 0.0;
        return Mth.clamp(actionValues.getOrDefault(action, 0.0) - repeatPenalty, -0.55, 0.35);
    }

    void rememberAction(String action) {
        lastActionStreak = action.equals(lastAction) ? Math.min(20, lastActionStreak + 1) : 1;
        rangeActionStreak = isRangeControlAction(action) ? Math.min(20, rangeActionStreak + 1) : 0;
    }

    private void observeActionValue(OpSukunaSnapshot s) {
        if ("None".equals(lastAction) || !lastActionTarget.equals(OpSukunaEngineCore.targetKey(s.target))) {
            return;
        }
        boolean lowImpact = s.damageDealt <= s.target.getMaxHealth() * 0.01 && s.damageTakenBurst > 0.18;
        noImpactActionStreak = lowImpact ? Math.min(20, noImpactActionStreak + 1) : Math.max(0, noImpactActionStreak - 1);
        double reward = s.damageDealt / Math.max(1.0, s.target.getMaxHealth()) * 3.0
                - s.damageTaken / Math.max(1.0, s.sukuna.getMaxHealth()) * 2.6
                - s.targetHeal / Math.max(1.0, s.target.getMaxHealth()) * 1.2
                + (s.targetCooldown || s.targetUnstable ? 0.08 : 0.0)
                + (s.targetEscaping && ("CutOffEscape".equals(lastAction) || "StrafePressure".equals(lastAction)) ? 0.08 : 0.0);
        if (isRangeControlAction(lastAction) && s.damageDealt <= s.target.getMaxHealth() * 0.01 && s.damageTakenBurst > 0.12) {
            reward -= 0.18 + Math.min(0.22, rangeActionStreak * 0.035);
        }
        if ((lastAction.contains("WorldCut") || lastAction.contains("Open")) && s.damageDealt <= s.target.getMaxHealth() * 0.015 && !s.targetCooldown && !s.targetUnstable) {
            reward -= 0.16;
        }
        if (s.yujiBurstDuel && (lastAction.contains("Guard") || lastAction.contains("Backstep") || lastAction.contains("Cleave") || lastAction.contains("Open"))
                && s.damageTakenBurst < 0.18) {
            reward += 0.1;
        }
        if (s.trivialTarget && isExpensiveAction(lastAction)) {
            reward -= 0.45;
        }
        if ((s.targetCooldown || s.targetUnstable) && lastAction.startsWith("PUNISH")) {
            reward += 0.12;
        }
        reward = Mth.clamp(reward, -0.5, 0.5);
        double old = actionValues.getOrDefault(lastAction, 0.0);
        actionValues.put(lastAction, old + (reward - old) * 0.08);
        observeActionOutcome(s.targetStats, lastAction, reward, 1.0);
        observeActionOutcome(s.typeStats, lastAction, reward, 0.35);
        observeActionOutcome(s.techniqueStats, lastAction, reward, 0.55);
        observeActionOutcome(s.archetypeStats, lastAction, reward, 0.25);
    }

    private static boolean isRangeControlAction(String action) {
        return "MaintainRange".equals(action) || "CutOffEscape".equals(action) || "StrafePressure".equals(action)
                || "BaitWhiff".equals(action) || "ProjectileDodge".equals(action) || "DomainEscape".equals(action)
                || "PURPLE_EVADE".equals(action);
    }

    private static boolean isExpensiveAction(String action) {
        return action.contains("Open") || action.contains("WorldCut") || action.contains("Domain")
                || "MAHORAGA".equals(action) || "AGITO".equals(action) || "SUMMON".equals(action);
    }

    private void observeStats(OpSukunaCombatStats stats, OpSukunaSnapshot s, double weight) {
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
        if ((s.targetDodge || s.targetCounter || s.targetGuard) && s.damageDealt <= s.target.getMaxHealth() * 0.012) stats.evasionTicks += weight;
        if ((s.rangeThreat > 0.45 || s.targetEscaping || s.distance > 22.0) && s.damageTaken > 0.0) stats.rangePressureTicks += weight;
        if (s.stuckLevel > 0.55 || s.pathBlocked || s.enemyDomainTrap) stats.trapTicks += weight;
        if (s.targetSkill > 0.0) stats.addSkillUse(s.targetSkill, weight);
    }

    private void observeActionOutcome(OpSukunaCombatStats stats, String action, double reward, double weight) {
        if (reward > 0.04) {
            stats.actionSuccessTicks += weight;
            stats.actionSuccess.put(action, stats.actionSuccess.getOrDefault(action, 0.0) + weight * Mth.clamp(reward * 2.0, 0.1, 1.0));
        } else if (reward < -0.04) {
            stats.actionFailureTicks += weight;
            stats.actionFailure.put(action, stats.actionFailure.getOrDefault(action, 0.0) + weight * Mth.clamp(-reward * 2.0, 0.1, 1.0));
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("lastHealth", lastHealth);
        tag.putDouble("lastBackstepTick", lastBackstepTick);
        tag.putDouble("lastProjectileDodgeTick", lastProjectileDodgeTick);
        tag.putDouble("lastMoveTick", lastMoveTick);
        tag.putDouble("lastGuardTick", lastGuardTick);
        tag.putDouble("lastGuardTimingTick", lastGuardTimingTick);
        tag.putDouble("lastEvasiveBackstepTick", lastEvasiveBackstepTick);
        tag.putDouble("lastRecoveryWindowTick", lastRecoveryWindowTick);
        tag.putDouble("lastSimpleDomainTick", lastSimpleDomainTick);
        tag.putDouble("lastBurnoutRctTick", lastBurnoutRctTick);
        tag.putDouble("lastDomainAmplificationTick", lastDomainAmplificationTick);
        tag.putDouble("lastSukunaDomainTick", lastSukunaDomainTick);
        tag.putDouble("lastWorldCutTick", lastWorldCutTick);
        tag.putDouble("lastPurpleEvadeTick", lastPurpleEvadeTick);
        tag.putDouble("pendingPurpleResponseTick", pendingPurpleResponseTick);
        tag.putDouble("firstDomainThreatTick", firstDomainThreatTick);
        tag.putDouble("lastTargetSwitchTick", lastTargetSwitchTick);
        tag.putDouble("lastTargetSkill", lastTargetSkill);
        tag.putDouble("lastTargetSkillTick", lastTargetSkillTick);
        tag.putDouble("lastTargetDistance", lastTargetDistance);
        if (Double.isFinite(lastPosX) && Double.isFinite(lastPosY) && Double.isFinite(lastPosZ)) {
            tag.putDouble("lastPosX", lastPosX);
            tag.putDouble("lastPosY", lastPosY);
            tag.putDouble("lastPosZ", lastPosZ);
        }
        tag.putDouble("stuckTicks", stuckTicks);
        if (Double.isFinite(lastDestinationX) && Double.isFinite(lastDestinationY) && Double.isFinite(lastDestinationZ)) {
            tag.putDouble("lastDestinationX", lastDestinationX);
            tag.putDouble("lastDestinationY", lastDestinationY);
            tag.putDouble("lastDestinationZ", lastDestinationZ);
        }
        tag.putDouble("lastEscapeTick", lastEscapeTick);
        tag.putInt("strafeSide", strafeSide);
        tag.putDouble("megunaHighThreatTicks", megunaHighThreatTicks);
        tag.putDouble("groupPressure", groupPressure);
        tag.putDouble("recentDamageWindow", recentDamageWindow);
        tag.putDouble("recentHealWindow", recentHealWindow);
        tag.putDouble("burstChainTicks", burstChainTicks);
        tag.putDouble("domainTrapTicks", domainTrapTicks);
        tag.putDouble("blackFlashChainTicks", blackFlashChainTicks);
        tag.putInt("rangeActionStreak", rangeActionStreak);
        tag.putInt("noImpactActionStreak", noImpactActionStreak);
        tag.putBoolean("heianResetUsed", heianResetUsed);
        tag.putString("lastAction", lastAction);
        tag.putString("lastActionTarget", lastActionTarget);
        tag.putString("lastPrimaryTarget", lastPrimaryTarget);
        tag.putInt("lastActionStreak", lastActionStreak);
        tag.put("targets", saveMap(targets));
        tag.put("types", saveMap(types));
        tag.put("techniques", saveMap(techniques));
        tag.put("archetypes", saveMap(archetypes));
        tag.put("actionValues", saveDoubleMap(actionValues));
        return tag;
    }

    public static OpSukunaBrainMemory load(CompoundTag tag) {
        OpSukunaBrainMemory memory = new OpSukunaBrainMemory();
        memory.lastHealth = tag.getDouble("lastHealth");
        memory.lastBackstepTick = tag.getDouble("lastBackstepTick");
        memory.lastProjectileDodgeTick = tag.getDouble("lastProjectileDodgeTick");
        memory.lastMoveTick = tag.getDouble("lastMoveTick");
        memory.lastGuardTick = tag.getDouble("lastGuardTick");
        memory.lastGuardTimingTick = tag.contains("lastGuardTimingTick") ? tag.getDouble("lastGuardTimingTick") : -1000.0;
        memory.lastEvasiveBackstepTick = tag.contains("lastEvasiveBackstepTick") ? tag.getDouble("lastEvasiveBackstepTick") : -1000.0;
        memory.lastRecoveryWindowTick = tag.contains("lastRecoveryWindowTick") ? tag.getDouble("lastRecoveryWindowTick") : -1000.0;
        memory.lastSimpleDomainTick = tag.contains("lastSimpleDomainTick") ? tag.getDouble("lastSimpleDomainTick") : -1000.0;
        memory.lastBurnoutRctTick = tag.contains("lastBurnoutRctTick") ? tag.getDouble("lastBurnoutRctTick") : -1000.0;
        memory.lastDomainAmplificationTick = tag.getDouble("lastDomainAmplificationTick");
        memory.lastSukunaDomainTick = tag.getDouble("lastSukunaDomainTick");
        memory.lastWorldCutTick = tag.contains("lastWorldCutTick") ? tag.getDouble("lastWorldCutTick") : -1000.0;
        memory.lastPurpleEvadeTick = tag.contains("lastPurpleEvadeTick") ? tag.getDouble("lastPurpleEvadeTick") : -1000.0;
        memory.pendingPurpleResponseTick = tag.contains("pendingPurpleResponseTick") ? tag.getDouble("pendingPurpleResponseTick") : -1000.0;
        memory.firstDomainThreatTick = tag.contains("firstDomainThreatTick") ? tag.getDouble("firstDomainThreatTick") : -1.0;
        memory.lastTargetSwitchTick = tag.contains("lastTargetSwitchTick") ? tag.getDouble("lastTargetSwitchTick") : -1000.0;
        memory.lastTargetSkill = tag.getDouble("lastTargetSkill");
        memory.lastTargetSkillTick = tag.getDouble("lastTargetSkillTick");
        memory.lastTargetDistance = tag.getDouble("lastTargetDistance");
        memory.lastPosX = tag.contains("lastPosX") ? tag.getDouble("lastPosX") : Double.NaN;
        memory.lastPosY = tag.contains("lastPosY") ? tag.getDouble("lastPosY") : Double.NaN;
        memory.lastPosZ = tag.contains("lastPosZ") ? tag.getDouble("lastPosZ") : Double.NaN;
        memory.stuckTicks = tag.getDouble("stuckTicks");
        memory.lastDestinationX = tag.contains("lastDestinationX") ? tag.getDouble("lastDestinationX") : Double.NaN;
        memory.lastDestinationY = tag.contains("lastDestinationY") ? tag.getDouble("lastDestinationY") : Double.NaN;
        memory.lastDestinationZ = tag.contains("lastDestinationZ") ? tag.getDouble("lastDestinationZ") : Double.NaN;
        memory.lastEscapeTick = tag.contains("lastEscapeTick") ? tag.getDouble("lastEscapeTick") : -1000.0;
        memory.strafeSide = tag.getInt("strafeSide");
        memory.megunaHighThreatTicks = tag.getDouble("megunaHighThreatTicks");
        memory.groupPressure = tag.getDouble("groupPressure");
        memory.recentDamageWindow = tag.getDouble("recentDamageWindow");
        memory.recentHealWindow = tag.getDouble("recentHealWindow");
        memory.burstChainTicks = tag.getDouble("burstChainTicks");
        memory.domainTrapTicks = tag.getDouble("domainTrapTicks");
        memory.blackFlashChainTicks = tag.getDouble("blackFlashChainTicks");
        memory.rangeActionStreak = tag.getInt("rangeActionStreak");
        memory.noImpactActionStreak = tag.getInt("noImpactActionStreak");
        memory.heianResetUsed = tag.getBoolean("heianResetUsed");
        memory.lastAction = tag.getString("lastAction");
        memory.lastActionTarget = tag.getString("lastActionTarget");
        memory.lastPrimaryTarget = tag.getString("lastPrimaryTarget");
        memory.lastActionStreak = tag.getInt("lastActionStreak");
        loadMap(tag.getCompound("targets"), memory.targets);
        loadMap(tag.getCompound("types"), memory.types);
        loadMap(tag.getCompound("techniques"), memory.techniques);
        loadMap(tag.getCompound("archetypes"), memory.archetypes);
        loadDoubleMap(tag.getCompound("actionValues"), memory.actionValues);
        return memory;
    }

    private static CompoundTag saveMap(Map<String, OpSukunaCombatStats> map) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, OpSukunaCombatStats> entry : map.entrySet()) {
            tag.put(entry.getKey(), entry.getValue().save());
        }
        return tag;
    }

    private static void loadMap(CompoundTag tag, Map<String, OpSukunaCombatStats> map) {
        for (String key : tag.getAllKeys()) {
            map.put(key, OpSukunaCombatStats.load(tag.getCompound(key)));
        }
    }

    static CompoundTag saveDoubleMap(Map<String, Double> map) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            tag.putDouble(entry.getKey(), entry.getValue());
        }
        return tag;
    }

    static void loadDoubleMap(CompoundTag tag, Map<String, Double> map) {
        for (String key : tag.getAllKeys()) {
            map.put(key, tag.getDouble(key));
        }
    }
}
