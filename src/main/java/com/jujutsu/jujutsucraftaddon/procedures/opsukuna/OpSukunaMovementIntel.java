package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import java.util.List;

final class OpSukunaMovementIntel {
    private OpSukunaMovementIntel() {
    }

    static void addBaselineMovementActions(OpSukunaSnapshot s, List<OpSukunaAction> actions) {
        actions.add(OpSukunaAction.move("ProjectileDodge", OpSukunaMovement.PROJECTILE_DODGE, OpSukunaEngine.scoreProjectileDodge(s)));
        actions.add(OpSukunaAction.move("PURPLE_EVADE", OpSukunaMovement.PURPLE_EVADE, OpSukunaEngine.scorePurpleEvade(s)));
        actions.add(OpSukunaAction.move("DomainEscape", OpSukunaMovement.DOMAIN_ESCAPE, OpSukunaEngine.scoreDomainEscape(s)));
        actions.add(OpSukunaAction.move("PressureChase", OpSukunaMovement.PRESSURE_CHASE, OpSukunaEngine.scorePressureChase(s)));
        actions.add(OpSukunaAction.move("MaintainRange", OpSukunaMovement.MAINTAIN_RANGE, OpSukunaEngine.scoreMaintainRange(s)));
        actions.add(OpSukunaAction.move("CutOffEscape", OpSukunaMovement.CUT_OFF_ESCAPE, OpSukunaEngine.scoreCutOffEscape(s)));
        actions.add(OpSukunaAction.move("StrafePressure", OpSukunaMovement.STRAFE_PRESSURE, OpSukunaEngine.scoreStrafePressure(s)));
        actions.add(OpSukunaAction.move("BaitWhiff", OpSukunaMovement.BAIT_WHIFF, OpSukunaEngine.scoreBaitWhiff(s)));
    }
}
