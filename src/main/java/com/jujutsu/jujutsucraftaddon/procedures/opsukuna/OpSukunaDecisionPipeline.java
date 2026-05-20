package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import java.util.ArrayList;
import java.util.List;

final class OpSukunaDecisionPipeline {
    private OpSukunaDecisionPipeline() {
    }

    static OpSukunaAction chooseBestAction(OpSukunaSnapshot s) {
        OpSukunaAction directed = OpSukunaTacticalDirector.choosePriorityAction(s);
        if (directed != null) {
            return directed;
        }

        OpSukunaAction specialist = chooseSpecialistAction(s);
        if (specialist != null) {
            return specialist;
        }

        OpSukunaAction loopBreak = chooseSoftLoopBreak(s);
        if (loopBreak != null) {
            return loopBreak;
        }

        if (s.trivialTarget) {
            return OpSukunaEngineCore.bestOf(s, List.of(
                    OpSukunaAction.skill("RESOURCE_EFFICIENT", OpSukunaEngineCore.FAST_DISMANTLE, 50.0, false,
                            OpSukunaEngineCore.scoreDismantle(s) + 0.45),
                    OpSukunaAction.skill("TrivialCleave", OpSukunaEngineCore.CLEAVE, 100.0, false,
                            OpSukunaEngineCore.scoreCleave(s) + 0.25),
                    OpSukunaAction.calculate("TRIVIAL_CLEANUP", OpSukunaEngineCore.scoreBasic(s) + 0.4)));
        }

        return OpSukunaActionScorer.bestOf(s, OpSukunaActionCatalog.baselineActions(s));
    }

    private static OpSukunaAction chooseSpecialistAction(OpSukunaSnapshot s) {
        if (shouldForceBenchmarkFinish(s)) {
            OpSukunaAction finisher = OpSukunaEngineCore.bestOf(s, List.of(
                    OpSukunaAction.worldCut("BENCH_FINISH_WorldCut", OpSukunaEngineCore.scoreWorldCut(s) + 1.15),
                    OpSukunaAction.skill("BENCH_FINISH_Dismantle", OpSukunaEngineCore.FAST_DISMANTLE, 50.0, false,
                            OpSukunaEngineCore.scoreDismantle(s) + 0.95),
                    OpSukunaAction.skill("BENCH_FINISH_Cleave", OpSukunaEngineCore.CLEAVE, 100.0, false,
                            OpSukunaEngineCore.scoreCleave(s) + 0.8),
                    OpSukunaAction.skill("BENCH_FINISH_Open", OpSukunaEngineCore.OPEN, 250.0, false,
                            OpSukunaEngineCore.scoreOpen(s) + 0.45),
                    OpSukunaAction.calculate("BENCH_FINISH_CONTACT", OpSukunaEngineCore.scoreBasic(s) + 0.65)));
            if (finisher.score > 0.35) {
                return finisher;
            }
        }

        if (OpSukunaEngineCore.hasConcreteDomainSignal(s)) {
            OpSukunaAction antiDomain = OpSukunaActionScorer.bestOf(s, OpSukunaDomainIntel.domainResponseActions(s));
            if (antiDomain.score > 0.55) {
                return antiDomain;
            }
        }

        if (s.infinitySignal > 0.0) {
            OpSukunaAction antiInfinity = OpSukunaEngineCore.bestOf(s, List.of(
                    OpSukunaAction.calculate("INFINITY_MELEE_UNLOCK", OpSukunaEngineCore.scoreBasic(s)
                            + (s.canBypassInfinityNow && s.hitboxDistance <= s.selfReach + 2.0 ? 0.95 : -0.4)),
                    OpSukunaAction.skill("INFINITY_Cleave", OpSukunaEngineCore.CLEAVE, 100.0, false,
                            OpSukunaEngineCore.scoreCleave(s) + (s.canBypassInfinityNow ? 0.75 : -0.35)),
                    OpSukunaAction.domainAmplification(OpSukunaEngineCore.scoreDomainAmplification(s) + 0.8),
                    OpSukunaAction.domain(OpSukunaEngineCore.scoreDomain(s) + 0.35),
                    OpSukunaAction.worldCut("ANTI_INFINITY", OpSukunaEngineCore.scoreWorldCut(s) + 0.55),
                    OpSukunaAction.mahoraga(OpSukunaEngineCore.scoreMahoraga(s) + 0.25),
                    OpSukunaAction.move("MaintainRange", OpSukunaMovement.MAINTAIN_RANGE,
                            OpSukunaEngineCore.scoreMaintainRange(s) + (s.pathBlocked ? -0.35 : 0.0))));
            if (antiInfinity.score > 0.45) {
                return antiInfinity;
            }
        }

        if (s.targetCooldown || s.targetUnstable || s.targetAttacking) {
            OpSukunaAction punish = OpSukunaEngineCore.bestOf(s, OpSukunaActionCatalog.offensiveActions(s, "PUNISH"));
            if (punish.score > 0.72) {
                return punish;
            }
        }

        if (s.selfDomain && s.normalReady) {
            OpSukunaAction shrine = OpSukunaEngineCore.bestOf(s, OpSukunaActionCatalog.shrineOffenseActions(s, "SHRINE"));
            if (shrine.score > 0.35) {
                return shrine;
            }
        }

        if (s.killConfirm) {
            OpSukunaAction finisher = OpSukunaEngineCore.bestOf(s, OpSukunaActionCatalog.offensiveActions(s, "KILL_CONFIRM"));
            if (finisher.score > 0.65) {
                return finisher;
            }
        }
        return null;
    }

    private static boolean shouldForceBenchmarkFinish(OpSukunaSnapshot s) {
        if (!s.itadoriModulo || s.lethalForecast || s.survivalMode || OpSukunaEngineCore.hasConcreteDomainSignal(s)) {
            return false;
        }
        boolean lowTarget = s.targetHealthRatio <= 0.20;
        boolean timedOutPressure = s.nbt.getDouble("cnt_target") > 800.0 && s.targetHealthRatio <= 0.35;
        boolean chaseLoop = s.memory.noImpactActionStreak >= 3 && s.distance <= 18.0 && s.targetHealthRatio <= 0.42;
        return lowTarget || timedOutPressure || chaseLoop;
    }

    private static OpSukunaAction chooseSoftLoopBreak(OpSukunaSnapshot s) {
        if (s.memory.rangeActionStreak < 3 && s.memory.noImpactActionStreak < 3) {
            return null;
        }
        double breakBoost = s.memory.noImpactActionStreak >= 6 ? 0.55 : 0.35;
        List<OpSukunaAction> breakers = new ArrayList<>(List.of(
                OpSukunaAction.guardTiming(OpSukunaEngineCore.scoreGuardTiming(s) + 0.25),
                OpSukunaAction.domainAmplification(OpSukunaEngineCore.scoreDomainAmplification(s) + 0.2),
                OpSukunaAction.calculate("FORCE_CONTACT", OpSukunaEngineCore.scoreBasic(s) + breakBoost),
                OpSukunaAction.skill("FORCE_Cleave", OpSukunaEngineCore.CLEAVE, 100.0, false,
                        OpSukunaEngineCore.scoreCleave(s) + breakBoost * 0.9),
                OpSukunaAction.worldCut("FORCE_WorldCut", OpSukunaEngineCore.scoreWorldCut(s) + 0.45)));
        if (s.memory.noImpactActionStreak < 5 || s.distance > 16.0) {
            breakers.add(OpSukunaAction.move("FORCE_CHASE", OpSukunaMovement.PRESSURE_CHASE,
                    OpSukunaEngineCore.scorePressureChase(s) + breakBoost * 0.35));
        }
        breakers.addAll(OpSukunaActionCatalog.noImpactBreakerActions(s, "FORCE"));
        OpSukunaAction realAction = OpSukunaEngineCore.bestOf(s, breakers);
        return realAction.score > 0.3 ? realAction : null;
    }
}
