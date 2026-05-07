package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

final class OpSukunaTargetIntel {
    private OpSukunaTargetIntel() {
    }

    static LivingEntity chooseFocusTarget(LevelAccessor world, LivingEntity sukuna, LivingEntity current, OpSukunaBrainMemory memory) {
        OpSukunaDomainFieldScan domainField = OpSukunaDomainFieldScan.scan(world, sukuna);
        if ((domainField.singleDominantSureHit || domainField.dominantCasting && domainField.dominantVoidLike)
                && domainField.dominantOwner != null && domainField.dominantOwner.isAlive()) {
            if (current == null || !OpSukunaEngine.targetKey(current).equals(OpSukunaEngine.targetKey(domainField.dominantOwner))) {
                OpSukunaEngine.switchTarget(sukuna, domainField.dominantOwner, memory);
                return domainField.dominantOwner;
            }
            memory.lastPrimaryTarget = OpSukunaEngine.targetKey(current);
            return current;
        }

        OpSukunaEngine.ThreatScan scan = OpSukunaEngine.ThreatScan.scan(world, sukuna, current, memory);
        LivingEntity best = scan.bestTarget;
        if (best == null) {
            return current;
        }
        if (current == null || !current.isAlive() || !OpSukunaEngine.isValidEnemy(world, sukuna, current)) {
            OpSukunaEngine.switchTarget(sukuna, best, memory);
            return best;
        }

        String currentKey = OpSukunaEngine.targetKey(current);
        String bestKey = OpSukunaEngine.targetKey(best);
        if (currentKey.equals(bestKey)) {
            memory.lastPrimaryTarget = currentKey;
            return current;
        }

        double tick = sukuna.tickCount;
        double currentScore = scan.scoreOf(current);
        double bestScore = scan.scoreOf(best);
        boolean emergencySwitch = scan.bestImmediatePriority || bestScore >= currentScore + 1.15;
        boolean cooldownReady = tick - memory.lastTargetSwitchTick >= 35.0;
        boolean currentStillGood = currentScore >= 0.95 && current.distanceTo(sukuna) < 48.0 && !scan.currentOutclassed;

        if (emergencySwitch || (cooldownReady && !currentStillGood && bestScore >= currentScore + 0.35)) {
            OpSukunaEngine.switchTarget(sukuna, best, memory);
            return best;
        }
        memory.lastPrimaryTarget = currentKey;
        return current;
    }
}
