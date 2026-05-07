package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import java.util.ArrayList;
import java.util.List;

final class OpSukunaDomainIntel {
    private OpSukunaDomainIntel() {
    }

    static List<OpSukunaAction> domainResponseActions(OpSukunaSnapshot s) {
        List<OpSukunaAction> actions = new ArrayList<>();
        OpSukunaDomainTactics.DomainDeliberation d = s.domainDeliberation;
        actions.add(OpSukunaAction.worldCut("WORLD_CUT_KILL_OR_BREAK",
                OpSukunaEngine.scoreWorldCut(s) + d.bonus(OpSukunaDomainOption.WORLD_CUT_KILL_OR_BREAK)));
        actions.add(OpSukunaAction.move("ESCAPE_BEFORE_CLOSE", OpSukunaMovement.DOMAIN_ESCAPE,
                OpSukunaEngine.scoreDomainEscape(s) + d.bonus(OpSukunaDomainOption.ESCAPE_BEFORE_CLOSE)));
        boolean muryoCommit = "GOJO_MURYO".equals(s.domainProfile.name) || "GOJO_MURYO_GLOBAL".equals(s.domainProfile.name)
                || s.domainField.dominantVoidLike;
        double counterReadyPenalty = muryoCommit && OpSukunaEngine.canSelectDomain(s) && !s.selfSimpleDomain ? 0.85 : 0.0;
        double noEscapeVoidSimpleBonus = s.voidExposure >= 0.9 && s.canUseSimpleDomain && (s.pathBlocked || !d.escapeFeasible)
                ? 1.6 - counterReadyPenalty : 0.0;
        actions.add(OpSukunaAction.simpleDomain(
                OpSukunaEngine.scoreSimpleDomain(s) + d.bonus(OpSukunaDomainOption.SIMPLE_DOMAIN_HOLD) + noEscapeVoidSimpleBonus));
        actions.add(OpSukunaAction.simpleDomainPlusDa(
                OpSukunaEngine.scoreSimpleDomain(s) + OpSukunaEngine.scoreDomainAmplification(s) * 0.45
                        + d.bonus(OpSukunaDomainOption.SIMPLE_DOMAIN_PLUS_DA)));
        actions.add(OpSukunaAction.domain(
                OpSukunaEngine.scoreDomain(s) + (s.catastrophicDomain ? 1.2 : 0.8) + (s.domainDeathSpiral ? 1.0 : 0.0)
                        + (muryoCommit && !d.escapeFeasible ? 1.15 : 0.0)
                        + d.bonus(OpSukunaDomainOption.COUNTER_DOMAIN)));
        actions.add(OpSukunaAction.tenShadowsDomain(
                OpSukunaEngine.scoreTenShadowsDomain(s) + 0.6 + (s.catastrophicDomain ? 0.35 : 0.0)
                        + d.bonus(OpSukunaDomainOption.COUNTER_DOMAIN) * 0.75));
        actions.add(OpSukunaAction.domainAmplification(
                OpSukunaEngine.scoreDomainAmplification(s) + (s.voidExposure > 0.45 ? 0.75 : 0.35)));
        actions.add(OpSukunaAction.mahoraga(
                OpSukunaEngine.scoreMahoraga(s) + (s.catastrophicDomain ? 0.45 : 0.0)));
        if (OpSukunaEngine.muryoSureHitEndgame(s)) {
            actions.add(OpSukunaAction.rct("PRE_HIT_RCT",
                    OpSukunaEngine.scoreRct(s) + 1.05));
        } else {
            actions.add(OpSukunaAction.rct("TANK_AND_RECOVER",
                    OpSukunaEngine.scoreRct(s) + (s.healthLosingRace ? 0.35 : 0.0)
                            + d.bonus(OpSukunaDomainOption.TANK_AND_RECOVER)));
            actions.add(OpSukunaAction.guardTiming(OpSukunaEngine.scoreGuardTiming(s) + 0.35));
            actions.add(OpSukunaAction.backstep("DOMAIN_BACKSTEP", OpSukunaEngine.scoreEvasiveBackstep(s) + 0.25));
            actions.add(OpSukunaAction.move("MaintainRange", OpSukunaMovement.MAINTAIN_RANGE,
                    OpSukunaEngine.scoreMaintainRange(s) + (s.pathBlocked ? -0.35 : 0.25)));
        }
        return actions;
    }
}
