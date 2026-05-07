package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

final class OpSukunaState {
    final OpSukunaSnapshot snapshot;
    final boolean domainThreat;
    final boolean movementImpaired;
    final boolean antiDomainGapImminent;
    final String targetKey;
    final String techniqueKey;

    private OpSukunaState(OpSukunaSnapshot snapshot) {
        this.snapshot = snapshot;
        this.domainThreat = snapshot.targetDomain || snapshot.targetCastingDomain
                || snapshot.domainField.singleDominantSureHit
                || snapshot.domainAssessment.targetDomainThreat > 0.55;
        this.movementImpaired = snapshot.pathBlocked || snapshot.stuckLevel > 0.62 || snapshot.selfInsideSolid;
        this.antiDomainGapImminent = snapshot.antiDomainGapImminent;
        this.targetKey = OpSukunaEngine.targetKey(snapshot.target);
        this.techniqueKey = snapshot.read.techniqueKey();
    }

    static OpSukunaState from(OpSukunaSnapshot snapshot) {
        return new OpSukunaState(snapshot);
    }
}
