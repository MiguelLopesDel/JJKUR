package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

final class OpSukunaActionCatalog {
    private OpSukunaActionCatalog() {
    }

    static List<OpSukunaAction> baselineActions(OpSukunaSnapshot s) {
        List<OpSukunaAction> actions = new ArrayList<>();
        actions.add(OpSukunaAction.backstep(OpSukunaEngineCore.scoreBackstep(s)));
        actions.add(OpSukunaAction.guard(OpSukunaEngineCore.scoreGuard(s)));
        actions.add(OpSukunaAction.guardTiming(OpSukunaEngineCore.scoreGuardTiming(s)));
        actions.add(OpSukunaAction.backstep("EvasiveBackstep", OpSukunaEngineCore.scoreEvasiveBackstep(s)));
        actions.add(OpSukunaAction.rct("RCT", OpSukunaEngineCore.scoreRct(s)));
        actions.add(OpSukunaAction.burnoutRct(OpSukunaEngineCore.scoreBurnoutRct(s)));
        actions.add(OpSukunaAction.simpleDomain(OpSukunaEngineCore.scoreSimpleDomain(s)));
        actions.add(OpSukunaAction.domainAmplification(OpSukunaEngineCore.scoreDomainAmplification(s)));
        actions.add(OpSukunaAction.calculate("Chase", OpSukunaEngineCore.scoreChase(s)));
        actions.add(OpSukunaAction.calculate("CalculateAttack", OpSukunaEngineCore.scoreBasic(s)));
        actions.add(OpSukunaAction.calculate("BLACK_FLASH_RECOVERY", OpSukunaEngineCore.scoreBlackFlashRecovery(s)));
        OpSukunaMovementIntel.addBaselineMovementActions(s, actions);
        actions.addAll(offensiveActions(s, ""));
        actions.add(OpSukunaAction.summon(OpSukunaEngineCore.scoreSummon(s)));
        actions.add(OpSukunaAction.agito(OpSukunaEngineCore.scoreAgito(s)));
        actions.add(OpSukunaAction.mahoraga(OpSukunaEngineCore.scoreMahoraga(s)));
        actions.add(OpSukunaAction.tenShadowsDomain(OpSukunaEngineCore.scoreTenShadowsDomain(s)));
        actions.add(OpSukunaAction.heianReset(OpSukunaEngineCore.scoreHeianReset(s)));
        return actions;
    }

    static List<OpSukunaAction> criticalDomainReserveActions(OpSukunaSnapshot s) {
        List<OpSukunaAction> actions = new ArrayList<>();
        actions.add(OpSukunaAction.domain(OpSukunaEngineCore.scoreDomain(s) + 2.4));
        actions.add(OpSukunaAction.worldCut("DOMAIN_RESERVE_WORLD_CUT", OpSukunaEngineCore.scoreWorldCut(s) + 1.6));
        actions.add(OpSukunaAction.simpleDomain(OpSukunaEngineCore.scoreSimpleDomain(s) + 1.2));
        actions.add(OpSukunaAction.simpleDomainPlusDa(OpSukunaEngineCore.scoreSimpleDomain(s)
                + OpSukunaEngineCore.scoreDomainAmplification(s) * 0.45 + 1.0));
        actions.add(OpSukunaAction.domainAmplification(OpSukunaEngineCore.scoreDomainAmplification(s) + 1.15));
        actions.add(OpSukunaAction.tenShadowsDomain(OpSukunaEngineCore.scoreTenShadowsDomain(s) + 1.1));
        actions.add(OpSukunaAction.mahoraga(OpSukunaEngineCore.scoreMahoraga(s) + 0.85));
        actions.add(OpSukunaAction.rct("PRE_HIT_RCT", OpSukunaEngineCore.scoreRct(s)
                + (OpSukunaEngineCore.hasPrimaryAntiDomainReserve(s) ? -0.35 : 1.25)));
        actions.add(OpSukunaAction.burnoutRct(OpSukunaEngineCore.scoreBurnoutRct(s) + 0.35));
        return actions;
    }

    static List<OpSukunaAction> offensiveActions(OpSukunaSnapshot s, String priorityName) {
        List<OpSukunaAction> actions = new ArrayList<>();
        String prefix = priorityName == null || priorityName.isEmpty() ? "" : priorityName + "_";
        if (s.normalReady) {
            if (!s.selfDomain) {
                actions.add(OpSukunaAction.skill(prefix + "Dismantle", OpSukunaEngineCore.FAST_DISMANTLE, 50.0, false,
                        OpSukunaEngineCore.scoreDismantle(s)));
            }
            actions.add(OpSukunaAction.skill(prefix + "Cleave", OpSukunaEngineCore.CLEAVE, 100.0, false,
                    OpSukunaEngineCore.scoreCleave(s)));
            actions.add(OpSukunaAction.skill(prefix + "Open", OpSukunaEngineCore.OPEN, 250.0, false,
                    OpSukunaEngineCore.scoreOpen(s)));
            if (s.worldCutReady) {
                actions.add(OpSukunaAction.worldCut(prefix + "WorldCut", OpSukunaEngineCore.scoreWorldCut(s)));
            }
            if (s.domainReady && OpSukunaEngineCore.canSelectDomain(s)) {
                actions.add(OpSukunaAction.domain(OpSukunaEngineCore.scoreDomain(s)));
            }
        }
        if (s.passiveReady && !s.combatCooldown && !s.infinity) {
            actions.add(OpSukunaAction.skill(prefix + "Passive111", OpSukunaEngineCore.PASSIVE_FAST, 50.0, true,
                    OpSukunaEngineCore.scorePassive111(s)));
            actions.add(OpSukunaAction.skill(prefix + "Passive112", OpSukunaEngineCore.PASSIVE_CLOSE, 50.0, true,
                    OpSukunaEngineCore.scorePassive112(s)));
            actions.add(OpSukunaAction.skill(prefix + "Passive113", OpSukunaEngineCore.PASSIVE_ANTI_RANGE, 50.0, true,
                    OpSukunaEngineCore.scorePassive113(s)));
        }
        if (actions.isEmpty()) {
            if (OpSukunaEngineCore.canConfirmRawMelee(s) || OpSukunaEngineCore.canProbeRawMelee(s)) {
                actions.add(OpSukunaAction.calculate(prefix + "CalculateAttack", OpSukunaEngineCore.scoreBasic(s)
                        + (OpSukunaEngineCore.canProbeRawMelee(s) ? 0.32 : 0.0)));
            } else {
                double loopPenalty = s.itadoriModulo ? Math.min(1.15, s.memory.noImpactActionStreak * 0.22 + s.memory.rangeActionStreak * 0.12) : 0.0;
                actions.add(OpSukunaAction.move(prefix + "PressureReposition", OpSukunaMovement.PRESSURE_CHASE,
                        OpSukunaEngineCore.scorePressureChase(s) - 0.35 - loopPenalty));
            }
        }
        return actions;
    }

    static List<OpSukunaAction> noImpactBreakerActions(OpSukunaSnapshot s, String priorityName) {
        List<OpSukunaAction> actions = new ArrayList<>();
        String prefix = priorityName == null || priorityName.isEmpty() ? "" : priorityName + "_";
        double urgency = Mth.clamp(s.memory.noImpactActionStreak / 6.0, 0.0, 1.2);
        boolean contact = s.hitboxDistance <= Math.max(2.75, s.selfReach + 0.75);
        if (contact) {
            actions.add(OpSukunaAction.calculate(prefix + "MeleeConfirm", OpSukunaEngineCore.scoreBasic(s) + 0.65 + urgency));
            actions.add(OpSukunaAction.skill(prefix + "CleaveConfirm", OpSukunaEngineCore.CLEAVE, 100.0, false,
                    OpSukunaEngineCore.scoreCleave(s) + 0.75 + urgency));
        }
        if (s.selfDomain) {
            actions.add(OpSukunaAction.skill(prefix + "ShrineCleave", OpSukunaEngineCore.CLEAVE, 100.0, false,
                    OpSukunaEngineCore.scoreCleave(s) + 0.9 + urgency));
        } else if (s.normalReady && (!s.yujiCounterStance || s.distance > 7.0 || s.memory.noImpactActionStreak >= 4)) {
            actions.add(OpSukunaAction.skill(prefix + "DismantleConfirm", OpSukunaEngineCore.FAST_DISMANTLE, 50.0, false,
                    OpSukunaEngineCore.scoreDismantle(s) + 0.8 + urgency * 0.65));
            if (s.clearShot && s.distance >= 9.0 && s.memory.noImpactActionStreak >= 4) {
                actions.add(OpSukunaAction.skill(prefix + "OpenBreak", OpSukunaEngineCore.OPEN, 250.0, false,
                        OpSukunaEngineCore.scoreOpen(s) + 0.45 + urgency * 0.35));
            }
        }
        if (s.worldCutReady) {
            actions.add(OpSukunaAction.worldCut(prefix + "WorldCutConfirm", OpSukunaEngineCore.scoreWorldCut(s) + 0.85 + urgency * 0.55));
        }
        if (actions.isEmpty()) {
            actions.add(OpSukunaAction.calculate(prefix + "CalculateConfirm", OpSukunaEngineCore.scoreBasic(s) + urgency));
        }
        return actions;
    }

    static List<OpSukunaAction> openingEngageActions(OpSukunaSnapshot s) {
        List<OpSukunaAction> actions = new ArrayList<>();
        double openingLoopPenalty = s.memory.rangeActionStreak >= 2 || s.memory.noImpactActionStreak >= 2 ? 1.15 : 0.0;
        double openingConfirmBoost = s.memory.noImpactActionStreak >= 2 ? 0.55 : 0.0;
        boolean allowOpeningChase = s.itadoriModulo
                ? (s.memory.noImpactActionStreak == 0 && s.distance > 8.0) || s.distance > 24.0
                : s.memory.noImpactActionStreak < 3 || s.distance > 14.0;
        if (allowOpeningChase) {
            actions.add(OpSukunaAction.move("OPENING_PRESSURE_CHASE", OpSukunaMovement.PRESSURE_CHASE,
                    OpSukunaEngineCore.scorePressureChase(s) + 0.55 - openingLoopPenalty));
        }
        actions.add(OpSukunaAction.move("OPENING_CUT_OFF", OpSukunaMovement.CUT_OFF_ESCAPE,
                OpSukunaEngineCore.scoreCutOffEscape(s) + 0.55));
        if (OpSukunaEngineCore.canConfirmRawMelee(s) || OpSukunaEngineCore.canProbeRawMelee(s)) {
            actions.add(OpSukunaAction.calculate("OPENING_FORCE_CONTACT", OpSukunaEngineCore.scoreBasic(s) + 0.75 + openingConfirmBoost));
        }
        double dismantleCounterRisk = s.yujiCounterStance ? -3.0 : 0.0;
        actions.add(OpSukunaAction.skill("OPENING_DISMANTLE", OpSukunaEngineCore.FAST_DISMANTLE, 50.0, false,
                OpSukunaEngineCore.scoreDismantle(s) + 0.45 + openingConfirmBoost + dismantleCounterRisk));
        actions.add(OpSukunaAction.skill("OPENING_CLEAVE", OpSukunaEngineCore.CLEAVE, 100.0, false,
                OpSukunaEngineCore.scoreCleave(s) + 0.65 + openingConfirmBoost));
        actions.add(OpSukunaAction.skill("OPENING_OPEN", OpSukunaEngineCore.OPEN, 250.0, false,
                OpSukunaEngineCore.scoreOpen(s) + 0.15));
        actions.add(OpSukunaAction.guardTiming(OpSukunaEngineCore.scoreGuardTiming(s) + 0.25));
        return actions;
    }

    static List<OpSukunaAction> shrineOffenseActions(OpSukunaSnapshot s, String priorityName) {
        List<OpSukunaAction> actions = new ArrayList<>();
        String prefix = priorityName == null || priorityName.isEmpty() ? "" : priorityName + "_";
        actions.add(OpSukunaAction.skill(prefix + "Open", OpSukunaEngineCore.OPEN, 250.0, false,
                OpSukunaEngineCore.scoreOpen(s) + OpSukunaEngineCore.shrineOpenPriority(s)));
        actions.add(OpSukunaAction.skill(prefix + "Cleave", OpSukunaEngineCore.CLEAVE, 100.0, false,
                OpSukunaEngineCore.scoreCleave(s) + 0.85));
        if (s.worldCutReady) {
            actions.add(OpSukunaAction.worldCut(prefix + "WorldCut", OpSukunaEngineCore.scoreWorldCut(s) + 0.35));
        }
        actions.add(OpSukunaAction.calculate(prefix + "Pressure", OpSukunaEngineCore.scoreBasic(s) + 0.25));
        return actions;
    }
}
