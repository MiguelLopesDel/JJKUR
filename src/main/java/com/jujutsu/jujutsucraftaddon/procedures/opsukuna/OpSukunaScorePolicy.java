package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

final class OpSukunaScorePolicy {
    private OpSukunaScorePolicy() {
    }

    static double adjustedScore(OpSukunaSnapshot s, OpSukunaAction action) {
        if (!action.canUse(s)) {
            return -999.0;
        }
        double adjusted = action.score + s.memory.actionBias(action.name) + s.adaptation.actionBias(action.name);
        if (action.kind == OpSukunaActionKind.RANGE_CONTROL) {
            adjusted -= Math.min(0.95, s.memory.rangeActionStreak * 0.18 + s.memory.noImpactActionStreak * 0.12);
            if (s.memory.noImpactActionStreak >= 5 && action.movement == OpSukunaMovement.PRESSURE_CHASE && s.distance <= 18.0) {
                adjusted -= 1.15;
            }
            if (action.movement == OpSukunaMovement.PRESSURE_CHASE
                    && s.memory.rangeActionStreak >= 3
                    && s.damageDealt <= s.target.getMaxHealth() * 0.02) {
                adjusted -= 0.75;
            }
            if (OpSukunaEngineCore.shouldForceOffensiveTempo(s) && action.movement != OpSukunaMovement.PRESSURE_CHASE) {
                adjusted -= 0.55;
            }
            if (OpSukunaEngineCore.shouldForceOpeningEngage(s)
                    && action.movement != OpSukunaMovement.PRESSURE_CHASE
                    && action.movement != OpSukunaMovement.CUT_OFF_ESCAPE) {
                adjusted -= 0.75;
            }
            if (action.movement == OpSukunaMovement.DOMAIN_ESCAPE && OpSukunaEngineCore.shouldAbortDomainEscape(s)) {
                adjusted -= 3.2;
            }
            if ((s.targetDomain || s.targetCastingDomain || s.domainField.singleDominantSureHit || s.voidExposure > 0.45)
                    && (s.pathBlocked || !s.domainDeliberation.escapeFeasible)) {
                adjusted -= action.movement == OpSukunaMovement.DOMAIN_ESCAPE && !s.pathBlocked ? 0.25 : 1.15;
            }
            if (s.yujiBurstDuel && (action.movement == OpSukunaMovement.CUT_OFF_ESCAPE || action.name.equals("Chase"))) {
                adjusted -= 0.75;
            }
        }
        if (OpSukunaEngineCore.isRawMeleeAction(action) && !OpSukunaEngineCore.canConfirmRawMelee(s)) {
            adjusted -= s.itadoriModulo && OpSukunaEngineCore.canProbeRawMelee(s) ? 0.45 : (s.itadoriModulo ? 1.65 : 0.95);
            if (s.memory.noImpactActionStreak >= 3) {
                adjusted -= 0.55;
            }
        }
        if (s.itadoriModulo && action.movement == OpSukunaMovement.PRESSURE_CHASE
                && (action.name.contains("PressureReposition") || action.name.contains("PRESSURE_CHASE") || action.name.contains("OPENING_PRESSURE_CHASE"))
                && s.memory.noImpactActionStreak >= 3 && s.distance <= 18.0) {
            adjusted -= 1.25;
        }
        if (s.itadoriModulo && action.movement == OpSukunaMovement.PRESSURE_CHASE
                && action.name.contains("OPENING_PRESSURE_CHASE")
                && s.memory.noImpactActionStreak >= 1 && s.distance <= 24.0) {
            adjusted -= 1.1;
        }
        if (s.itadoriModulo && action.movement == OpSukunaMovement.PRESSURE_CHASE
                && s.memory.noImpactActionStreak >= 2
                && s.damageDealt <= s.target.getMaxHealth() * 0.02) {
            adjusted -= 0.85;
        }
        if ((action.kind == OpSukunaActionKind.MELEE || action.kind == OpSukunaActionKind.RANGE_CONTROL)
                && s.domainField.singleDominantSureHit
                && s.voidExposure >= 0.9
                && s.simpleDomainCooldown > 0
                && !s.domainDeliberation.escapeFeasible
                && !s.selfDomain) {
            adjusted -= 1.65;
        }
        if (OpSukunaEngineCore.dominantDomainReserveCritical(s)) {
            if (action.kind == OpSukunaActionKind.RANGE_CONTROL) {
                if (action.movement == OpSukunaMovement.DOMAIN_ESCAPE && s.domainDeliberation.escapeFeasible && !s.pathBlocked) {
                    adjusted -= 0.45;
                } else {
                    adjusted -= 4.0;
                }
            }
            if (action.backstep || "PURPLE_BACKSTEP".equals(action.name) || "PURPLE_EVADE".equals(action.name)
                    || action.name.contains("MaintainRange")) {
                adjusted -= 4.0;
            }
            if (action.recoveryWindow || action.name.contains("RCT")) {
                adjusted -= s.selfHealthRatio < 0.28 ? 1.1 : 2.2;
            }
            if ("BLACK_FLASH_RECOVERY".equals(action.name)
                    || action.kind == OpSukunaActionKind.MELEE
                    || action.kind == OpSukunaActionKind.NORMAL_SLASH) {
                adjusted -= 2.8;
            }
        }
        if (OpSukunaEngineCore.muryoSureHitEndgame(s)) {
            if (action.kind == OpSukunaActionKind.RANGE_CONTROL || action.backstep || action.guard || action.guardTiming
                    || "PURPLE_BACKSTEP".equals(action.name) || "PURPLE_EVADE".equals(action.name)
                    || "TANK_AND_RECOVER".equals(action.name)) {
                adjusted -= 4.5;
            }
            if (action.recoveryWindow) {
                adjusted += OpSukunaEngineCore.hasPrimaryAntiDomainReserve(s) ? -0.9 : 1.45;
            }
            if (action.domainAmplification && !OpSukunaEngineCore.hasPrimaryAntiDomainReserve(s)) {
                adjusted += 0.9;
            }
        }
        if (action.kind == OpSukunaActionKind.DOMAIN && !OpSukunaEngineCore.canSelectDomain(s)) {
            adjusted -= 2.0;
        }
        if (action.kind == OpSukunaActionKind.WORLD_CUT && !s.worldCutReady) {
            adjusted -= 2.0;
        }
        if (OpSukunaEngineCore.shouldForceOpeningEngage(s) && action.kind == OpSukunaActionKind.MICRO_DEFENSE
                && !action.guardTiming && !action.domainAmplification && !action.recoveryWindow) {
            adjusted -= 0.55;
        }
        if (s.yujiCounterStance) {
            boolean longCommitSkill = action.skill == OpSukunaEngineCore.OPEN || action.skill == OpSukunaEngineCore.DOMAIN
                    || action.skill == OpSukunaEngineCore.AGITO || action.skill == OpSukunaEngineCore.MAHORAGA
                    || action.skill == OpSukunaEngineCore.TEN_SHADOWS_DOMAIN;
            if (longCommitSkill) {
                adjusted -= 0.85;
            } else if (action.kind == OpSukunaActionKind.NORMAL_SLASH || action.kind == OpSukunaActionKind.MELEE) {
                adjusted -= 0.6;
            }
            if (action.kind == OpSukunaActionKind.WORLD_CUT) {
                adjusted += 0.8;
            }
            if (action.backstep && !"PURPLE_BACKSTEP".equals(action.name)) {
                adjusted += 0.6;
            }
            if (action.guardTiming) {
                adjusted += 0.45;
            }
        }
        return adjusted;
    }

    static boolean shouldPreferDiverseTie(OpSukunaSnapshot s, OpSukunaAction best, OpSukunaAction candidate) {
        if (candidate.name.equals(s.memory.lastAction) || candidate.score < 0.35) {
            return false;
        }
        if (best.name.equals(s.memory.lastAction) && s.memory.lastActionStreak >= 2) {
            return true;
        }
        int bucket = Math.floorMod(candidate.name.hashCode() ^ OpSukunaEngineCore.targetKey(s.target).hashCode() ^ ((int) s.tick / 20), 5);
        return bucket == 0 && candidate.kind != OpSukunaActionKind.DOMAIN && !candidate.name.equals(best.name);
    }
}
