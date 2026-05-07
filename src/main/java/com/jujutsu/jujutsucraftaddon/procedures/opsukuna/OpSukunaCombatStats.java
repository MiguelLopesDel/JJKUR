package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

class OpSukunaCombatStats {
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
    double evasionTicks;
    double rangePressureTicks;
    double trapTicks;
    double actionSuccessTicks;
    double actionFailureTicks;
    final Map<Long, Double> skills = new HashMap<>();
    final Map<String, Double> actionSuccess = new HashMap<>();
    final Map<String, Double> actionFailure = new HashMap<>();

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
        tag.putDouble("evasionTicks", evasionTicks);
        tag.putDouble("rangePressureTicks", rangePressureTicks);
        tag.putDouble("trapTicks", trapTicks);
        tag.putDouble("actionSuccessTicks", actionSuccessTicks);
        tag.putDouble("actionFailureTicks", actionFailureTicks);
        CompoundTag skillTag = new CompoundTag();
        for (Map.Entry<Long, Double> entry : skills.entrySet()) {
            skillTag.putDouble(Long.toString(entry.getKey()), entry.getValue());
        }
        tag.put("skills", skillTag);
        tag.put("actionSuccess", OpSukunaBrainMemory.saveDoubleMap(actionSuccess));
        tag.put("actionFailure", OpSukunaBrainMemory.saveDoubleMap(actionFailure));
        return tag;
    }

    static OpSukunaCombatStats load(CompoundTag tag) {
        OpSukunaCombatStats stats = new OpSukunaCombatStats();
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
        stats.evasionTicks = tag.getDouble("evasionTicks");
        stats.rangePressureTicks = tag.getDouble("rangePressureTicks");
        stats.trapTicks = tag.getDouble("trapTicks");
        stats.actionSuccessTicks = tag.getDouble("actionSuccessTicks");
        stats.actionFailureTicks = tag.getDouble("actionFailureTicks");
        CompoundTag skillTag = tag.getCompound("skills");
        for (String key : skillTag.getAllKeys()) {
            try {
                stats.skills.put(Long.parseLong(key), skillTag.getDouble(key));
            } catch (NumberFormatException ignored) {
            }
        }
        OpSukunaBrainMemory.loadDoubleMap(tag.getCompound("actionSuccess"), stats.actionSuccess);
        OpSukunaBrainMemory.loadDoubleMap(tag.getCompound("actionFailure"), stats.actionFailure);
        return stats;
    }

    private static double sampleValue(double old, double value, double alpha) {
        return old == 0.0 ? value : old + (value - old) * alpha;
    }
}
