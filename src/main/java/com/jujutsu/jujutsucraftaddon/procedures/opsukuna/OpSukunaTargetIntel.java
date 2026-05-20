package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

final class OpSukunaTargetIntel {
    private OpSukunaTargetIntel() {
    }

    static LivingEntity chooseFocusTarget(LevelAccessor world, LivingEntity sukuna, LivingEntity current, OpSukunaBrainMemory memory) {
        String sukunaFightId = sukuna.getPersistentData().getString("JJKU_BENCHMARK_FIGHT_ID");
        boolean inBenchmark = !sukunaFightId.isEmpty();
        if (inBenchmark && current != null && !sameBenchmarkFight(sukunaFightId, current)) {
            current = null;
        }
        if (inBenchmark && (current == null || !current.isAlive() || !OpSukunaEngine.isValidEnemy(world, sukuna, current))) {
            LivingEntity benchmarkTarget = findBenchmarkTarget(world, sukuna, sukunaFightId);
            if (benchmarkTarget != null) {
                OpSukunaEngine.switchTarget(sukuna, benchmarkTarget, memory);
                return benchmarkTarget;
            }
        }

        OpSukunaDomainFieldScan domainField = OpSukunaDomainFieldScan.scan(world, sukuna);
        if ((domainField.singleDominantSureHit || domainField.dominantCasting && domainField.dominantVoidLike)
                && domainField.dominantOwner != null && domainField.dominantOwner.isAlive()
                && (!inBenchmark || sameBenchmarkFight(sukunaFightId, domainField.dominantOwner))) {
            if (current == null || !OpSukunaEngine.targetKey(current).equals(OpSukunaEngine.targetKey(domainField.dominantOwner))) {
                OpSukunaEngine.switchTarget(sukuna, domainField.dominantOwner, memory);
                return domainField.dominantOwner;
            }
            memory.lastPrimaryTarget = OpSukunaEngine.targetKey(current);
            return current;
        }

        OpSukunaEngine.ThreatScan scan = OpSukunaEngine.ThreatScan.scan(world, sukuna, current, memory);
        LivingEntity best = scan.bestTarget;
        if (inBenchmark && best != null && !sameBenchmarkFight(sukunaFightId, best)) {
            best = null;
        }
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

    private static boolean sameBenchmarkFight(String sukunaFightId, LivingEntity candidate) {
        if (sukunaFightId == null || sukunaFightId.isEmpty() || candidate == null) {
            return true;
        }
        String otherFightId = candidate.getPersistentData().getString("JJKU_BENCHMARK_FIGHT_ID");
        if (otherFightId.isEmpty()) {
            return false;
        }
        return sukunaFightId.equals(otherFightId);
    }

    private static LivingEntity findBenchmarkTarget(LevelAccessor world, LivingEntity sukuna, String fightId) {
        if (!(world instanceof Level level) || fightId == null || fightId.isEmpty()) {
            return null;
        }
        AABB searchBox = sukuna.getBoundingBox().inflate(128.0);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : level.getEntities((Entity) null, searchBox, entity -> entity instanceof LivingEntity)) {
            if (!(entity instanceof LivingEntity candidate) || candidate == sukuna || !candidate.isAlive() || candidate.isRemoved()) {
                continue;
            }
            if (!(candidate instanceof Mob) || !sameBenchmarkFight(fightId, candidate)
                    || !OpSukunaEngine.isValidEnemy(world, sukuna, candidate)) {
                continue;
            }
            double distance = candidate.distanceToSqr(sukuna);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }
}
