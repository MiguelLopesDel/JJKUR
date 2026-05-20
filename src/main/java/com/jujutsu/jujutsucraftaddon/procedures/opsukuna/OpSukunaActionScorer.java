package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import java.util.List;

final class OpSukunaActionScorer {
    private OpSukunaActionScorer() {
    }

    static OpSukunaAction chooseBestAction(OpSukunaSnapshot snapshot) {
        OpSukunaState state = OpSukunaState.from(snapshot);
        if (state.domainThreat && state.antiDomainGapImminent) {
            OpSukunaAction antiDomain = bestOf(snapshot, OpSukunaDomainIntel.domainResponseActions(snapshot));
            if (antiDomain.score > 0.55) {
                return antiDomain;
            }
        }
        return OpSukunaDecisionPipeline.chooseBestAction(snapshot);
    }

    static OpSukunaAction bestOf(OpSukunaSnapshot s, List<OpSukunaAction> actions) {
        OpSukunaAction best = actions.get(0);
        double bestScore = OpSukunaScorePolicy.adjustedScore(s, best);
        for (OpSukunaAction action : actions) {
            double adjusted = OpSukunaScorePolicy.adjustedScore(s, action);
            if (adjusted > bestScore) {
                best = action;
                bestScore = adjusted;
            } else if (s.isMeguna && adjusted >= bestScore - 0.16 && OpSukunaScorePolicy.shouldPreferDiverseTie(s, best, action)) {
                best = action;
                bestScore = adjusted;
            }
        }
        return best;
    }
}
