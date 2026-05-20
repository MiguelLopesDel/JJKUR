package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.procedures.JJKURSukunaAIBuff;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.CalculateAttackProcedure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

final class OpSukunaAction {
    final String name;
    final OpSukunaActionKind kind;
    final double skill;
    final double cooldownTicks;
    final boolean combatOnly;
    final boolean backstep;
    final boolean domain;
    final boolean hwb;
    final boolean guard;
    final boolean guardTiming;
    final boolean recoveryWindow;
    final boolean domainAmplification;
    final boolean worldCut;
    final boolean simpleDomainPlusDa;
    final boolean tenShadows;
    final boolean heianReset;
    final OpSukunaMovement movement;
    final double score;

    private OpSukunaAction(String name, OpSukunaActionKind kind, double skill, double cooldownTicks, boolean combatOnly, boolean backstep, boolean domain, boolean hwb,
                   boolean guard, boolean guardTiming, boolean recoveryWindow, boolean domainAmplification, boolean worldCut,
                   boolean simpleDomainPlusDa, boolean tenShadows, boolean heianReset, OpSukunaMovement movement, double score) {
        this.name = name;
        this.kind = kind;
        this.skill = skill;
        this.cooldownTicks = cooldownTicks;
        this.combatOnly = combatOnly;
        this.backstep = backstep;
        this.domain = domain;
        this.hwb = hwb;
        this.guard = guard;
        this.guardTiming = guardTiming;
        this.recoveryWindow = recoveryWindow;
        this.domainAmplification = domainAmplification;
        this.worldCut = worldCut;
        this.simpleDomainPlusDa = simpleDomainPlusDa;
        this.tenShadows = tenShadows;
        this.heianReset = heianReset;
        this.movement = movement;
        this.score = score;
    }

    static OpSukunaAction skill(String name, double skill, double cooldownTicks, boolean combatOnly, double score) {
        OpSukunaActionKind kind = skill == OpSukunaEngineCore.CLEAVE ? OpSukunaActionKind.MELEE : OpSukunaActionKind.NORMAL_SLASH;
        return create(name, kind, skill, cooldownTicks, combatOnly, score);
    }

    static OpSukunaAction worldCut(double score) {
        return worldCut("WorldCut", score);
    }

    static OpSukunaAction worldCut(String name, double score) {
        return create(name, OpSukunaActionKind.WORLD_CUT, OpSukunaEngineCore.FAST_DISMANTLE, 100.0, false, false, false, false, false, false, false, false, true, false, false, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction backstep(double score) {
        return backstep("Backstep", score);
    }

    static OpSukunaAction backstep(String name, double score) {
        return create(name, OpSukunaActionKind.MICRO_DEFENSE, 0.0, 0.0, false, true, false, false, false, false, false, false, false, false, false, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction guard(double score) {
        return guard("Guard", score);
    }

    static OpSukunaAction guard(String name, double score) {
        return create(name, OpSukunaActionKind.MICRO_DEFENSE, 0.0, 0.0, false, false, false, false, true, false, false, false, false, false, false, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction guardTiming(double score) {
        return create("GUARD_TIMING", OpSukunaActionKind.MICRO_DEFENSE, 0.0, 0.0, false, false, false, false, false, true, false, false, false, false, false, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction rct(String name, double score) {
        return create(name, OpSukunaActionKind.MICRO_DEFENSE, 0.0, 0.0, false, false, false, false, false, false, true, false, false, false, false, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction burnoutRct(double score) {
        return create("BURNOUT_RCT", OpSukunaActionKind.MICRO_DEFENSE, 0.0, 0.0, false, false, false, false, false, false, false, true, false, false, false, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction domainAmplification(double score) {
        return create("DomainAmplification", OpSukunaActionKind.DOMAIN_AMPLIFICATION, 0.0, 0.0, false, false, false, false, false, false, false, true, false, false, false, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction domain(double score) {
        return create("Domain", OpSukunaActionKind.DOMAIN, OpSukunaEngineCore.DOMAIN, 20.0, false, false, true, false, false, false, false, false, false, false, false, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction simpleDomain(double score) {
        return create("SIMPLE_DOMAIN", OpSukunaActionKind.MICRO_DEFENSE, 0.0, 0.0, false, false, false, true, false, false, false, false, false, false, false, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction simpleDomainPlusDa(double score) {
        return create("SIMPLE_DOMAIN_PLUS_DA", OpSukunaActionKind.MICRO_DEFENSE, 0.0, 0.0, false, false, false, false, false, false, false, false, false, true, false, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction move(String name, OpSukunaMovement movement, double score) {
        return create(name, OpSukunaActionKind.RANGE_CONTROL, 0.0, 0.0, false, false, false, false, false, false, false, false, false, false, false, false, movement, score);
    }

    static OpSukunaAction calculate(String name, double score) {
        return create(name, OpSukunaActionKind.MELEE, 0.0, 0.0, false, false, false, false, false, false, false, false, false, false, false, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction summon(double score) {
        return create("SUMMON", OpSukunaActionKind.TEN_SHADOWS, OpSukunaEngineCore.TEN_SHADOWS_UTILITY, 90.0, false, false, false, false, false, false, false, false, false, false, true, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction agito(double score) {
        return create("AGITO", OpSukunaActionKind.TEN_SHADOWS, OpSukunaEngineCore.AGITO, 140.0, false, false, false, false, false, false, false, false, false, false, true, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction mahoraga(double score) {
        return create("MAHORAGA", OpSukunaActionKind.TEN_SHADOWS, OpSukunaEngineCore.MAHORAGA, 180.0, false, false, false, false, false, false, false, false, false, false, true, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction tenShadowsDomain(double score) {
        return create("TEN_SHADOWS_DOMAIN", OpSukunaActionKind.TEN_SHADOWS, OpSukunaEngineCore.TEN_SHADOWS_DOMAIN, 20.0, false, false, false, false, false, false, false, false, false, false, true, false, OpSukunaMovement.NONE, score);
    }

    static OpSukunaAction heianReset(double score) {
        return create("SURVIVAL_RESET_HEIAN", OpSukunaActionKind.TRANSFORM, 0.0, 0.0, false, false, false, false, false, false, false, false, false, false, false, true, OpSukunaMovement.NONE, score);
    }

    private static OpSukunaAction create(String name, OpSukunaActionKind kind, double skill, double cooldownTicks, boolean combatOnly, double score) {
        return create(name, kind, skill, cooldownTicks, combatOnly, false, false, false, false, false, false, false, false, false, false, false, OpSukunaMovement.NONE, score);
    }

    private static OpSukunaAction create(String name, OpSukunaActionKind kind, double skill, double cooldownTicks, boolean combatOnly, boolean backstep, boolean domain, boolean hwb,
                                 boolean guard, boolean guardTiming, boolean recoveryWindow, boolean domainAmplification, boolean worldCut,
                                 boolean simpleDomainPlusDa, boolean tenShadows, boolean heianReset, OpSukunaMovement movement, double score) {
        return new OpSukunaAction(name, kind, skill, cooldownTicks, combatOnly, backstep, domain, hwb, guard, guardTiming, recoveryWindow,
                domainAmplification, worldCut, simpleDomainPlusDa, tenShadows, heianReset, movement, score);
    }

    boolean canUse(OpSukunaSnapshot s) {
        if (score <= -900.0) return false;
        if (hwb) return s.canUseSimpleDomain && !s.selfSimpleDomain && !s.selfDomain && OpSukunaEngineCore.shouldSpendSimpleDomain(s);
        if (simpleDomainPlusDa) return s.canUseSimpleDomain && s.canUseDomainAmplification && !s.selfSimpleDomain && !s.selfDomain
                && !s.sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get()) && OpSukunaEngineCore.shouldSpendSimpleDomain(s);
        if (recoveryWindow) {
            // Emergency: bypass canUseRct when CURSED_TECHNIQUE is the only blocker and Sukuna is near-death
            if (!s.selfRct && !s.selfAntiHeal && s.selfHealthRatio < 0.38 && s.itadoriModulo) return true;
            return s.canUseRct && !s.selfRct && !s.selfAntiHeal;
        }
        if ("BURNOUT_RCT".equals(name)) return s.canUseBurnoutRct && !s.nbt.getBoolean("PRESS_BURNOUT");
        if (domainAmplification) return s.canUseDomainAmplification && !s.sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get());
        if (domain) return OpSukunaEngineCore.canSelectDomain(s);
        if (worldCut) return s.worldCutReady;
        if (tenShadows) {
            if ("AGITO".equals(name)) return s.canUseAgito && s.normalReady && !s.selfDomain;
            if ("MAHORAGA".equals(name)) return s.canUseMahoraga && s.normalReady && !s.mahoragaExist;
            if ("TEN_SHADOWS_DOMAIN".equals(name)) return s.canUseTenShadowsDomain && s.normalReady && !s.selfDomain && !s.brainDamaged;
            return s.canUseTenShadows && s.normalReady && !s.selfDomain;
        }
        if (heianReset) return s.isMeguna && s.canTransformHeian && !s.memory.heianResetUsed;
        if (skill != 0.0) return combatOnly ? s.passiveReady && !s.combatCooldown : s.normalReady;
        return true;
    }

    String blockReason(OpSukunaSnapshot s) {
        if (hwb) {
            if (s.selfSimpleDomain) return "simple_domain_already_active";
            if (s.selfDomain) return "inside_own_domain";
            if (s.sukuna.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) return "cursed_technique_active";
            if (s.sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_SIMPLE_DOMAIN.get())) return "simple_domain_cooldown";
            if (!OpSukunaEngineCore.hasEntityTypeTag(s.sukuna, "jujutsucraft:can_use_simple_domain")) return "missing_simple_domain_tag";
        } else if (recoveryWindow) {
            if (s.selfRct) return "rct_already_active";
            if (s.selfAntiHeal) return "anti_heal";
            if (s.sukuna.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) return "cursed_technique_active";
            if (!s.canUseRct) return "rct_not_ready";
        } else if ("BURNOUT_RCT".equals(name)) {
            if (!(s.sukuna instanceof Player)) return "burnout_rct_player_only";
            if (s.nbt.getBoolean("PRESS_BURNOUT")) return "burnout_already_pressed";
            if (!s.canUseBurnoutRct) return "burnout_rct_not_ready";
        } else if (domainAmplification) {
            if (s.sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get())) return "domain_amplification_active";
            if (s.selfDomain) return "inside_own_domain";
            if (!OpSukunaEngineCore.hasEntityTypeTag(s.sukuna, "jujutsucraft:can_use_domain_amplification")) return "missing_domain_amplification_tag";
        } else if (domain) {
            if (!s.domainReady) return s.domainBlockReason == null || s.domainBlockReason.isEmpty() ? "domain_not_ready" : s.domainBlockReason;
            if (s.selfDomain) return "domain_already_active";
            if (s.brainDamaged) return "brain_damage";
            if (s.domainBlockedByCooldown) return s.domainBlockReason;
        } else if (worldCut && !s.worldCutReady) {
            return s.worldCutBlockReason;
        } else if (skill != 0.0 && !canUse(s)) {
            return combatOnly ? "passive_not_ready" : "normal_not_ready";
        }
        return canUse(s) ? "ready" : "not_ready";
    }

    boolean execute(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, CompoundTag nbt, OpSukunaSnapshot s) {
        if (!canUse(s)) {
            return false;
        }
        double beforeSkill = nbt.getDouble("skill");
        boolean beforeSimple = OpSukunaEngineCore.hasSimpleDomainDefense(sukuna);
        boolean beforeDa = sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get());
        boolean beforeRct = sukuna.hasEffect(JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get()) || nbt.getBoolean("PRESS_M");
        if (hwb) {
            OpSukunaEngineCore.useSimpleDomain(world, x, y, z, sukuna, s);
        } else if (simpleDomainPlusDa) {
            OpSukunaEngineCore.useSimpleDomain(world, x, y, z, sukuna, s);
            OpSukunaEngineCore.domainAmplification(sukuna, s);
        } else if (guard) {
            OpSukunaEngineCore.guard(world, sukuna, s);
        } else if (guardTiming) {
            OpSukunaEngineCore.guardTiming(world, sukuna, s);
        } else if (recoveryWindow) {
            OpSukunaEngineCore.useRct(sukuna, s);
        } else if (domainAmplification) {
            if ("BURNOUT_RCT".equals(name)) {
                OpSukunaEngineCore.useBurnoutRct(sukuna, s);
            } else {
                OpSukunaEngineCore.domainAmplification(sukuna, s);
            }
        } else if (backstep) {
            OpSukunaEngineCore.backstep(world, sukuna, s);
            if (name.contains("Backstep")) {
                s.memory.lastEvasiveBackstepTick = s.tick;
            }
        } else if (domain) {
            OpSukunaEngineCore.useDomain(world, x, y, z, sukuna, nbt, s);
        } else if (worldCut) {
            OpSukunaEngineCore.setWorldCut(sukuna, nbt, s);
        } else if (heianReset) {
            JJKURSukunaAIBuff.forceHeianTransformation(world, sukuna, false);
            s.memory.heianResetUsed = true;
        } else if (tenShadows) {
            OpSukunaEngineCore.equipMahoragaWheel(sukuna, s);
            OpSukunaEngineCore.setSkill(sukuna, nbt, skill, cooldownTicks, combatOnly);
        } else if (skill != 0.0) {
            OpSukunaEngineCore.tryPreloadRctCombo(sukuna, s);
            OpSukunaEngineCore.setSkill(sukuna, nbt, skill, cooldownTicks, combatOnly);
            if (skill == OpSukunaEngineCore.OPEN) {
                nbt.putBoolean("PRESS_Z", s.selfDomain);
                s.memory.fugaStartTick = s.tick;
                s.memory.fugaLastDecision = s.selfDomain ? "start_hold" : "start_release";
            }
        } else if (movement != OpSukunaMovement.NONE) {
            OpSukunaMovementController.moveTactically(sukuna, s, movement);
        } else {
            CalculateAttackProcedure.execute(world, sukuna);
        }
        return didStart(sukuna, nbt, beforeSkill, beforeSimple, beforeDa, beforeRct);
    }

    private boolean didStart(LivingEntity sukuna, CompoundTag nbt, double beforeSkill, boolean beforeSimple, boolean beforeDa, boolean beforeRct) {
        if (hwb) return !beforeSimple && OpSukunaEngineCore.hasSimpleDomainDefense(sukuna);
        if (simpleDomainPlusDa) return !beforeSimple && OpSukunaEngineCore.hasSimpleDomainDefense(sukuna)
                && !beforeDa && sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get());
        if (recoveryWindow) return !beforeRct && (sukuna.hasEffect(JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get()) || nbt.getBoolean("PRESS_M"));
        if ("BURNOUT_RCT".equals(name)) return nbt.getBoolean("PRESS_BURNOUT");
        if (domainAmplification) return !beforeDa && sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get());
        if (domain || worldCut || tenShadows || skill != 0.0) return nbt.getDouble("skill") != 0.0 && nbt.getDouble("skill") != beforeSkill;
        return true;
    }
}
