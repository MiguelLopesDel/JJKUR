package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.util.Mth;

import static com.jujutsu.jujutsucraftaddon.procedures.opsukuna.OpSukunaEngineCore.*;

final class OpSukunaDomainTactics {
    private OpSukunaDomainTactics() {
    }

    static class DomainTactics {
        static DomainAssessment assess(OpSukunaSnapshot s) {
            double simpleRisk = (s.targetSimpleDomain ? 0.42 : 0.0) + Mth.clamp(s.read.simpleDomainLevel / 8.0, 0.0, 0.35);
            double hwbRisk = (s.targetHwb || s.read.hwbActive ? 0.55 : 0.0) + s.read.hwbBias * 0.35;
            double breakRisk = s.targetDomainBreak ? 0.7 : 0.0;
            double amplificationRisk = s.targetDomainAmplification || s.targetNeutralization ? 0.25 : 0.0;
            double masteryRisk = Mth.clamp((s.read.domainMastery - 1.0) / 2.0, 0.0, 0.6);
            double perfectAntiDomain = s.targetPerfectAntiDomain ? 0.85 : 0.0;
            double barrierlessRisk = s.read.barrierlessDomain ? 0.35 : 0.0;
            double infusedRisk = s.read.infusedDomain ? 0.25 : 0.0;
            double learnedCounter = s.domainCounterRisk;
            double counterRisk = Mth.clamp(simpleRisk + hwbRisk + breakRisk + amplificationRisk + masteryRisk + perfectAntiDomain + barrierlessRisk + infusedRisk + learnedCounter * 0.65, 0.0, 1.0);
            if (s.domainDeathSpiral) {
                counterRisk = Math.min(counterRisk, 0.42);
            }

            double globalThreat = s.domainField.singleDominantSureHit ? s.domainField.dominantThreat + (s.domainField.dominantVoidLike ? 0.28 : 0.0)
                    : s.domainField.dominantCasting ? 0.65 : 0.0;
            double targetDomainThreat = Mth.clamp((s.targetCastingDomain ? 1.0 : 0.0) + (s.targetDomain ? 0.75 : 0.0) + globalThreat + s.read.domainBias * 0.55
                    + (s.read.ultimate ? 0.25 : 0.0) + (s.read.curseEnergy > 450.0 ? 0.25 : 0.0)
                    + (s.catastrophicDomain ? 0.35 : 0.0), 0.0, 1.0);
            if (s.domainField.clashSuppressed && !s.catastrophicDomain) {
                targetDomainThreat *= 0.35;
            }
            double killValue = Mth.clamp(s.targetKillEstimate.resourceValue * 0.45 + s.killPressure(0.45) * 0.85
                    + (s.targetCooldown || s.targetUnstable || s.targetWhiffed ? 0.18 : 0.0), 0.0, 1.0);
            double checkmate = Mth.clamp(killValue + s.tankScore * 0.28 + s.healScore * 0.25 + s.pressure * 0.25 - counterRisk * 0.75
                    - (s.targetPerfectAntiDomain ? 0.45 : 0.0), 0.0, 1.0);
            double simpleStillEnough = s.canUseSimpleDomain && s.selfHealthRatio > 0.42 && !s.lethalForecast && !s.domainDeathSpiral ? 0.25 : 0.0;
            double counterDomain = Mth.clamp(targetDomainThreat * 0.95 + (s.targetCastingDomain ? 0.45 : 0.0)
                    + (s.catastrophicDomain ? 0.25 : 0.0) + (s.healthLosingRace ? 0.18 : 0.0) - simpleStillEnough, 0.0, 1.0);
            if (s.domainField.clashSuppressed && !s.antiDomainGapImminent) {
                counterDomain *= 0.35;
            }
            double antiInfinity = Mth.clamp(s.infinitySignal * 0.75 - (s.worldCutCapable && s.hitConfidence > 0.45 ? 0.25 : 0.0)
                    - (s.sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get()) ? 0.3 : 0.0) - counterRisk * 0.4, 0.0, 1.0);
            double crowdReset = Mth.clamp(s.groupActivePressure * 0.75 + s.groupApexPressure * 0.9 + s.groupPressure * 0.25
                    - counterRisk * 0.35, 0.0, 1.0);
            double alternateDefense = (s.canUseSimpleDomain ? targetDomainThreat * 0.6 : 0.0)
                    + close(s.distance, s.itadoriModulo ? 10.0 : 7.0) * 0.25
                    + (s.targetSkillStartup ? 0.25 : 0.0);
            double survivalStall = Mth.clamp(s.survivalUrgency * 0.65 + (s.lethalForecast ? 0.35 : 0.0)
                    + (s.domainDeathSpiral ? 0.42 : 0.0) + (s.blackFlashChain ? 0.18 : 0.0)
                    - alternateDefense * 0.18 - counterRisk * 0.28, 0.0, 1.0);

            OpSukunaDomainIntent intent = OpSukunaDomainIntent.NONE;
            double confidence = 0.0;
            if (counterDomain >= confidence) {
                intent = OpSukunaDomainIntent.COUNTER_DOMAIN;
                confidence = counterDomain;
            }
            if (checkmate > confidence) {
                intent = OpSukunaDomainIntent.CHECKMATE;
                confidence = checkmate;
            }
            if (antiInfinity > confidence) {
                intent = OpSukunaDomainIntent.ANTI_INFINITY;
                confidence = antiInfinity;
            }
            if (crowdReset > confidence) {
                intent = OpSukunaDomainIntent.CROWD_RESET;
                confidence = crowdReset;
            }
            if (survivalStall > confidence) {
                intent = OpSukunaDomainIntent.SURVIVAL_STALL;
                confidence = survivalStall;
            }
            if (confidence < 0.42) {
                intent = OpSukunaDomainIntent.NONE;
            }

            double clearValue = Mth.clamp(killValue * 0.45 + checkmate * 0.38 + crowdReset * 0.5
                    + s.groupApexPressure * 0.35 + (s.targetPowerProfile.apex ? 0.25 : 0.0), 0.0, 1.35);
            double sustainCost = Mth.clamp((1.0 - s.selfHealthRatio) * 0.55 + s.rctStrain * 0.45 + s.brainDamageLevel / 8.0
                    + s.damageTakenBurst * 0.45 + (s.rctFatigued ? 0.3 : 0.0), 0.0, 1.35);
            double breakRecovery = Mth.clamp((s.canUseBurnoutRct ? 0.36 : 0.0) + (s.canUseRct && !s.selfAntiHeal ? 0.24 : 0.0)
                    + (s.canUseSimpleDomain ? 0.16 : 0.0) + (s.canUseMahoraga || s.mahoragaExist ? 0.22 : 0.0)
                    + (s.selfHealthRatio > 0.55 ? 0.18 : 0.0), 0.0, 1.0);
            double recoveryRisk = Mth.clamp((1.0 - breakRecovery) * 0.75 + s.incomingKillRisk * 0.35
                    + (s.selfAntiHeal ? 0.2 : 0.0) + (s.lethalForecast ? 0.18 : 0.0), 0.0, 1.35);
            double domainScore = Mth.clamp(clearValue + counterDomain * 0.65 + checkmate * 0.55 + antiInfinity * 0.35
                    - sustainCost - counterRisk * 0.72 - recoveryRisk * 0.62, -1.5, 1.5);

            double wasteRisk = s.expensiveWastePenalty(0.55) + (s.tick - s.memory.lastSukunaDomainTick < 420.0 ? 0.35 : 0.0)
                    + (s.brainDamaged ? 0.55 : 0.0) + (s.targetDomainBreak ? 0.55 : 0.0);
            double emergency = intent == OpSukunaDomainIntent.COUNTER_DOMAIN ? (s.targetCastingDomain ? 1.55 : 0.75) : 0.0;
            if (s.domainDeathSpiral) emergency += 1.05;
            double castScore = -0.35 + confidence * 1.45 + domainScore * 1.15 + emergency + (1.0 - counterRisk) * 0.25 - counterRisk * 0.95 - wasteRisk;
            if (intent == OpSukunaDomainIntent.CHECKMATE && counterRisk < 0.35) castScore += 0.35;
            if (intent == OpSukunaDomainIntent.CROWD_RESET && (s.groupActivePressure > 0.65 || s.groupApexPressure > 0.45)) castScore += 0.3;
            if (intent == OpSukunaDomainIntent.SURVIVAL_STALL && s.lethalForecast) castScore += 0.25;

            double hwbScore = -0.2 + targetDomainThreat * 2.4 + (s.targetCastingDomain || s.domainField.dominantCasting ? 1.4 : 0.0)
                    + (s.domainField.singleDominantSureHit && s.domainField.dominantVoidLike && !s.selfSimpleDomain ? 1.1 : 0.0)
                    - (s.selfHealthRatio > 0.75 && !s.targetDomain && !s.domainField.singleDominantSureHit ? 0.35 : 0.0)
                    - (s.catastrophicDomain && (s.simpleDomainExpiresSoon || s.healthLosingRace) ? 0.75 : 0.0);
            double punishScore = Mth.clamp(counterRisk * 0.45 + targetDomainThreat * 0.35 + (s.targetCooldown ? 0.25 : 0.0), 0.0, 1.0);
            String plan = intent.name() + ":clear=" + round(clearValue) + ",cost=" + round(sustainCost) + ",recovery=" + round(recoveryRisk);
            return new DomainAssessment(counterRisk, targetDomainThreat, castScore, hwbScore, punishScore, intent, confidence,
                    plan, clearValue, sustainCost, recoveryRisk, domainScore);
        }
    }

    static class DomainProfile {
        final String name;
        final double threat;
        final double brainDamageRisk;
        final double neutralizationRisk;
        final double tankTolerance;
        final double escapeValue;
        final double clashMultiplier;
        final boolean voidLike;
        final boolean barrierless;

        DomainProfile(String name, double threat, double brainDamageRisk, double neutralizationRisk, double tankTolerance,
                      double escapeValue, double clashMultiplier, boolean voidLike, boolean barrierless) {
            this.name = name;
            this.threat = threat;
            this.brainDamageRisk = brainDamageRisk;
            this.neutralizationRisk = neutralizationRisk;
            this.tankTolerance = tankTolerance;
            this.escapeValue = escapeValue;
            this.clashMultiplier = clashMultiplier;
            this.voidLike = voidLike;
            this.barrierless = barrierless;
        }

        static DomainProfile create(OpSukunaSnapshot s) {
            boolean barrierless = s.read.barrierlessDomain;
            double clash = Math.max(0.65, s.read.domainMastery);
            if (s.domainField.singleDominantSureHit && s.domainField.dominantVoidLike) {
                return new DomainProfile("GOJO_MURYO_GLOBAL", 1.0, 1.0, 0.95, 0.04, s.domainField.dominantBarrierless ? 0.12 : 0.45, clash, true, s.domainField.dominantBarrierless);
            }
            if (s.domainField.singleDominantSureHit) {
                return new DomainProfile(s.domainField.dominantProfile, Math.max(0.68, s.domainField.dominantThreat), 0.35, 0.65, 0.16,
                        s.domainField.dominantBarrierless ? 0.08 : 0.3, clash, false, s.domainField.dominantBarrierless);
            }
            if (s.gojoTarget || s.read.primaryTechnique == TechniqueIDs.GOJO || s.read.secondaryTechnique == TechniqueIDs.GOJO) {
                return new DomainProfile("GOJO_MURYO", 1.0, 1.0, 0.95, 0.04, barrierless ? 0.12 : 0.45, clash, true, barrierless);
            }
            if (s.read.primaryTechnique == TechniqueIDs.MAHITO || s.read.secondaryTechnique == TechniqueIDs.MAHITO) {
                return new DomainProfile("MAHITO_SOUL", 0.92, 0.35, 0.85, 0.08, barrierless ? 0.12 : 0.38, clash, false, barrierless);
            }
            if (s.read.primaryTechnique == TechniqueIDs.HIGURUMA || s.read.secondaryTechnique == TechniqueIDs.HIGURUMA) {
                return new DomainProfile("HIGURUMA_JUDGEMAN", 0.78, 0.12, 0.65, 0.20, barrierless ? 0.08 : 0.32, clash, false, barrierless);
            }
            if (s.read.primaryTechnique == TechniqueIDs.HAKARI || s.read.secondaryTechnique == TechniqueIDs.HAKARI) {
                return new DomainProfile("HAKARI_JACKPOT", 0.68, 0.08, 0.25, 0.36, barrierless ? 0.08 : 0.28, clash, false, barrierless);
            }
            double learnedThreat = Mth.clamp(s.read.domainBias * 0.45 + s.targetStats.domainTicks / Math.max(80.0, s.targetStats.seenTicks) * 0.55
                    + s.damageTakenBurst * 0.35 + s.voidExposure * 0.25, 0.22, 0.9);
            double neutralization = Mth.clamp((s.targetNeutralization ? 0.55 : 0.0) + s.voidExposure * 0.35 + s.targetSkillDanger * 0.25, 0.0, 0.85);
            double brain = Mth.clamp((s.catastrophicDomain ? 0.35 : 0.0) + (hasExtremeVoidDebuff(s.sukuna) ? 0.35 : 0.0), 0.0, 0.8);
            double tankTolerance = Mth.clamp(0.62 - learnedThreat * 0.5 - neutralization * 0.35 - brain * 0.25, 0.08, 0.55);
            return new DomainProfile("GENERIC_DOMAIN", learnedThreat, brain, neutralization, tankTolerance, barrierless ? 0.08 : 0.25, clash, false, barrierless);
        }
    }

    static class DomainDeliberation {
        static final DomainDeliberation NONE = new DomainDeliberation(OpSukunaDomainOption.NONE, false, 0.0, 0.0, 0.0, "none");
        final OpSukunaDomainOption option;
        final boolean escapeFeasible;
        final double worldCutValue;
        final double counterDomainValue;
        final double tankValue;
        final String reason;

        DomainDeliberation(OpSukunaDomainOption option, boolean escapeFeasible, double worldCutValue, double counterDomainValue, double tankValue, String reason) {
            this.option = option;
            this.escapeFeasible = escapeFeasible;
            this.worldCutValue = worldCutValue;
            this.counterDomainValue = counterDomainValue;
            this.tankValue = tankValue;
            this.reason = reason;
        }

        static DomainDeliberation choose(OpSukunaSnapshot s) {
            if (!hasConcreteDomainSignal(s) && s.domainAssessment.targetDomainThreat <= 0.55) {
                return NONE;
            }
            DomainProfile p = s.domainProfile;
            boolean escape = s.domainEscapeWindow && !p.barrierless;
            double worldCut = s.worldCutReady ? Mth.clamp(s.targetKillEstimate.resourceValue * 0.85 + s.killPressure(0.55) * 0.75
                    + p.neutralizationRisk * 0.45 + p.brainDamageRisk * 0.35 + (s.targetCastingDomain ? 0.45 : 0.0)
                    + (s.gojoTarget || p.voidLike || s.domainField.dominantVoidLike ? 0.3 : 0.0), 0.0, 2.0) : -1.0;
            double simple = s.canUseSimpleDomain ? Mth.clamp(p.threat * 0.85 + s.domainAssessment.targetDomainThreat * 0.7
                    - p.brainDamageRisk * 0.25 - (s.simpleDomainExpiresSoon ? 0.35 : 0.0), 0.0, 1.6) : -1.0;
            double combo = s.canUseSimpleDomain && s.canUseDomainAmplification && (s.gojoTarget || s.infinity || s.domainField.dominantVoidLike)
                    ? simple + 0.55 + p.neutralizationRisk * 0.35 : -1.0;
            double counter = canSelectDomain(s) ? Mth.clamp(s.domainAssessment.castScore + p.threat * 0.75 + p.brainDamageRisk * 0.55
                    + (p.voidLike ? 0.4 : 0.0) + (s.healthLosingRace ? 0.25 : 0.0)
                    - Math.max(0.0, p.clashMultiplier - 1.0) * 0.25, -1.0, 2.2) : -1.0;
            double tank = Mth.clamp(p.tankTolerance + s.selfHealthRatio * 0.35 + (s.canUseRct ? 0.25 : 0.0)
                    - p.threat * 0.75 - p.neutralizationRisk * 0.45 - p.brainDamageRisk * 0.55
                    - (s.healthLosingRace || s.lethalForecast ? 0.65 : 0.0), -1.0, 1.2);
            double escapeValue = escape ? Mth.clamp(p.escapeValue + (s.targetCastingDomain ? 0.8 : 0.0) + p.threat * 0.45, 0.0, 1.6) : -1.0;

            OpSukunaDomainOption option = OpSukunaDomainOption.SIMPLE_DOMAIN_HOLD;
            double best = simple;
            if (worldCut > best && worldCut >= 0.78) {
                option = OpSukunaDomainOption.WORLD_CUT_KILL_OR_BREAK;
                best = worldCut;
            }
            if (escapeValue > best && escapeValue >= 0.72) {
                option = OpSukunaDomainOption.ESCAPE_BEFORE_CLOSE;
                best = escapeValue;
            }
            if (combo > best && combo >= 0.85) {
                option = OpSukunaDomainOption.SIMPLE_DOMAIN_PLUS_DA;
                best = combo;
            }
            if (counter > best && (counter >= 0.82 || p.voidLike && simple < 0.72)) {
                option = OpSukunaDomainOption.COUNTER_DOMAIN;
                best = counter;
            }
            if (tank > best && tank >= 0.58 && p.threat < 0.62 && !p.voidLike) {
                option = OpSukunaDomainOption.TANK_AND_RECOVER;
            }
            String reason = option.name() + ":profile=" + p.name + ",escape=" + escape + ",world_cut=" + round(worldCut)
                    + ",counter=" + round(counter) + ",tank=" + round(tank);
            return new DomainDeliberation(option, escape, worldCut, counter, tank, reason);
        }

        double bonus(OpSukunaDomainOption expected) {
            return option == expected ? 1.35 : 0.0;
        }
    }

    static class DomainAssessment {
        final double counterRisk;
        final double targetDomainThreat;
        final double castScore;
        final double hwbScore;
        final double punishScore;
        final OpSukunaDomainIntent intent;
        final double confidence;
        final String plan;
        final double clearValue;
        final double sustainCost;
        final double recoveryRisk;
        final double domainScore;

        DomainAssessment(double counterRisk, double targetDomainThreat, double castScore, double hwbScore, double punishScore, OpSukunaDomainIntent intent, double confidence,
                         String plan, double clearValue, double sustainCost, double recoveryRisk, double domainScore) {
            this.counterRisk = counterRisk;
            this.targetDomainThreat = targetDomainThreat;
            this.castScore = castScore;
            this.hwbScore = hwbScore;
            this.punishScore = punishScore;
            this.intent = intent;
            this.confidence = confidence;
            this.plan = plan;
            this.clearValue = clearValue;
            this.sustainCost = sustainCost;
            this.recoveryRisk = recoveryRisk;
            this.domainScore = domainScore;
        }
    }
}
