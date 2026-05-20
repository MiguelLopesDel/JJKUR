package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import java.util.List;

/**
 * Hard-priority tactical layer that runs before the general score pool.
 * This keeps fight-critical policy out of loose numeric weights.
 */
final class OpSukunaTacticalDirector {
    private OpSukunaTacticalDirector() {
    }

    static OpSukunaAction choosePriorityAction(OpSukunaSnapshot s) {
        OpSukunaAction domain = antiDomain(s);
        if (domain != null) {
            return domain;
        }
        OpSukunaAction lethal = lethalResponse(s);
        if (lethal != null) {
            return lethal;
        }
        OpSukunaAction matchup = matchupPlan(s);
        if (matchup != null) {
            return matchup;
        }
        return tempoPlan(s);
    }

    private static OpSukunaAction antiDomain(OpSukunaSnapshot s) {
        if (OpSukunaEngineCore.shouldFirstInstantDomainEscape(s)) {
            OpSukunaAction escape = OpSukunaAction.move("ESCAPE_BEFORE_CLOSE", OpSukunaMovement.DOMAIN_ESCAPE,
                    OpSukunaEngineCore.scoreDomainEscape(s) + 2.2);
            if (escape.canUse(s) && OpSukunaEngineCore.adjustedScore(s, escape) > -0.35) {
                return escape;
            }
        }
        if (OpSukunaEngineCore.shouldForceCounterDomainNow(s)) {
            OpSukunaAction domain = OpSukunaAction.domain(OpSukunaEngineCore.scoreDomain(s) + 3.4);
            if (domain.canUse(s)) {
                return domain;
            }
        }
        if (OpSukunaEngineCore.dominantDomainReserveCritical(s)) {
            OpSukunaAction crisis = OpSukunaActionScorer.bestOf(s, OpSukunaActionCatalog.criticalDomainReserveActions(s));
            if (OpSukunaEngineCore.adjustedScore(s, crisis) > -999.0) {
                return crisis;
            }
        }
        if (OpSukunaEngineCore.hasConcreteDomainSignal(s)
                && (s.voidExposure >= 0.9 || s.catastrophicDomain || s.domainField.dominantVoidLike || !s.domainDeliberation.escapeFeasible)) {
            OpSukunaAction antiDomain = OpSukunaActionScorer.bestOf(s, OpSukunaDomainIntel.domainResponseActions(s));
            if (antiDomain.score > 0.45) {
                return antiDomain;
            }
        }
        return null;
    }

    private static OpSukunaAction lethalResponse(OpSukunaSnapshot s) {
        if (s.purpleThreat) {
            OpSukunaAction purple = OpSukunaEngineCore.bestOf(s, List.of(
                    OpSukunaAction.move("PURPLE_EVADE", OpSukunaMovement.PURPLE_EVADE, OpSukunaEngineCore.scorePurpleEvade(s)),
                    OpSukunaAction.worldCut("PURPLE_INTERRUPT", OpSukunaEngineCore.scoreWorldCut(s) + (s.purpleWindup ? 0.8 : 0.15)),
                    OpSukunaAction.domainAmplification(OpSukunaEngineCore.scoreDomainAmplification(s) + 0.35),
                    OpSukunaAction.backstep("PURPLE_BACKSTEP", OpSukunaEngineCore.scoreEvasiveBackstep(s) + s.purpleRisk * 1.2)));
            if (purple.score > 0.55) {
                return purple;
            }
        }
        if (s.lethalForecast || s.survivalMode) {
            OpSukunaAction survival = OpSukunaEngineCore.bestOf(s, List.of(
                    OpSukunaAction.domain(OpSukunaEngineCore.scoreDomain(s) + (s.domainDeathSpiral ? 1.0 : 0.2)),
                    OpSukunaAction.simpleDomain(OpSukunaEngineCore.scoreSimpleDomain(s) + (s.simpleDomainExpiresSoon || s.domainDeathSpiral ? 0.7 : 0.35)),
                    OpSukunaAction.domainAmplification(OpSukunaEngineCore.scoreDomainAmplification(s) + (s.voidExposure > 0.55 ? 0.65 : 0.25)),
                    OpSukunaAction.rct("RCT", OpSukunaEngineCore.scoreRct(s) + 0.55),
                    OpSukunaAction.backstep("SURVIVAL_RESET", OpSukunaEngineCore.scoreBackstep(s) + 1.1),
                    OpSukunaAction.guard("PANIC_SURVIVAL", OpSukunaEngineCore.scoreGuard(s) + 0.8),
                    OpSukunaAction.guardTiming(OpSukunaEngineCore.scoreGuardTiming(s) + 0.85),
                    OpSukunaAction.calculate("BLACK_FLASH_RECOVERY", OpSukunaEngineCore.scoreBlackFlashRecovery(s)),
                    OpSukunaAction.burnoutRct(OpSukunaEngineCore.scoreBurnoutRct(s) + 0.5),
                    OpSukunaAction.worldCut("SURVIVAL_CUT", OpSukunaEngineCore.scoreWorldCut(s) + (s.domainDeathSpiral || s.blackFlashChain ? 0.65 : 0.0)),
                    OpSukunaAction.mahoraga(OpSukunaEngineCore.scoreMahoraga(s) + 0.7),
                    OpSukunaAction.heianReset(OpSukunaEngineCore.scoreHeianReset(s))));
            if (survival.score > 0.65) {
                return survival;
            }
        }
        return null;
    }

    private static OpSukunaAction matchupPlan(OpSukunaSnapshot s) {
        if (s.yujiBurstDuel && s.nbt.getDouble("cnt_target") > 80.0
                && (s.distance < 12.0 || s.immediateThreat > 0.6 || s.blackFlashChain)) {
            OpSukunaAction burstDuel = OpSukunaEngineCore.bestOf(s, List.of(
                    OpSukunaAction.guardTiming(OpSukunaEngineCore.scoreGuardTiming(s) + 0.55),
                    OpSukunaAction.backstep("YUJI_LATERAL_RESET", OpSukunaEngineCore.scoreEvasiveBackstep(s) + 0.35),
                    OpSukunaAction.domainAmplification(OpSukunaEngineCore.scoreDomainAmplification(s) + 0.28),
                    OpSukunaAction.rct("YUJI_RCT_WINDOW", OpSukunaEngineCore.scoreRct(s) + (s.selfHealthRatio < 0.62 ? 0.35 : -0.15)),
                    OpSukunaAction.worldCut("YUJI_PUNISH_WorldCut", OpSukunaEngineCore.scoreWorldCut(s) + 0.45),
                    OpSukunaAction.skill("YUJI_PUNISH_Cleave", OpSukunaEngineCore.CLEAVE, 100.0, false, OpSukunaEngineCore.scoreCleave(s) + 0.35),
                    OpSukunaAction.skill("YUJI_PUNISH_Open", OpSukunaEngineCore.OPEN, 250.0, false, OpSukunaEngineCore.scoreOpen(s) + 0.2),
                    OpSukunaAction.calculate("YUJI_COUNTER_PRESSURE", OpSukunaEngineCore.scoreBasic(s) + 0.28 - (s.memory.noImpactActionStreak > 4 ? 0.45 : 0.0))));
            if (burstDuel.score > 0.5) {
                return burstDuel;
            }
        }
        return null;
    }

    private static OpSukunaAction tempoPlan(OpSukunaSnapshot s) {
        if (OpSukunaEngineCore.shouldBreakNoImpactLoop(s)) {
            OpSukunaAction breaker = OpSukunaEngineCore.bestOf(s, OpSukunaActionCatalog.noImpactBreakerActions(s, "LOOP_BREAK"));
            if (breaker.score > 0.35) {
                return breaker;
            }
        }
        if (OpSukunaEngineCore.shouldForceOpeningEngage(s)) {
            OpSukunaAction opening = OpSukunaEngineCore.bestOf(s, OpSukunaActionCatalog.openingEngageActions(s));
            if (opening.score > 0.35) {
                return opening;
            }
        }
        if (OpSukunaEngineCore.shouldForceOffensiveTempo(s)) {
            OpSukunaAction pressure = OpSukunaEngineCore.bestOf(s, OpSukunaActionCatalog.offensiveActions(s, "TEMPO"));
            if (pressure.score > 0.42) {
                return pressure;
            }
        }
        if (OpSukunaEngineCore.shouldForcePressureChase(s)) {
            OpSukunaAction chase = OpSukunaAction.move("PRESSURE_CHASE", OpSukunaMovement.PRESSURE_CHASE,
                    OpSukunaEngineCore.scorePressureChase(s) + 0.45);
            if (OpSukunaEngineCore.adjustedScore(s, chase) > 0.35) {
                return chase;
            }
        }
        return null;
    }
}
