package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.util.DomainMasterySystem;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

final class OpSukunaPlayerRead {
    double primaryTechnique;
    double secondaryTechnique;
    double curseEnergy;
    double curseEnergyMax;
    double outputLevel;
    double moveset;
    double rct;
    double domainBias;
    double healBias;
    double aggression;
    double fleeBias;
    double rangedBias;
    double tankBias;
    double hwbBias;
    double techniqueMastery;
    double simpleDomainLevel;
    double domainMastery = 1.0;
    double bfChance;
    double ceShield;
    boolean barrierlessDomain;
    boolean infusedDomain;
    boolean simpleDomain;
    boolean hwbActive;
    boolean worldSlash;
    boolean ultimate;
    boolean itadoriAwakening;
    boolean mahoragaReady;

    static OpSukunaPlayerRead read(LivingEntity target) {
        OpSukunaPlayerRead read = new OpSukunaPlayerRead();
        read.primaryTechnique = target.getPersistentData().getDouble("PlayerCurseTechnique");
        read.secondaryTechnique = target.getPersistentData().getDouble("PlayerCurseTechnique2");
        read.outputLevel = 1.0;
        if (target instanceof Player) {
            target.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
                read.primaryTechnique = vars.PlayerCurseTechnique;
                read.secondaryTechnique = vars.PlayerCurseTechnique2;
                read.curseEnergy = noisy(vars.PlayerCursePower, target, 0);
                read.curseEnergyMax = noisy(vars.PlayerCursePowerMAX, target, 9);
                read.domainBias = Math.max(read.domainBias, vars.PlayerCursePower > 450.0 ? 0.45 : 0.15);
            });
            target.getCapability(com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
                read.outputLevel = noisy(vars.OutputLevel, target, 1);
                read.moveset = noisy(vars.Moveset, target, 2);
                read.rct = noisy(vars.RCTMastery + vars.RCTRegen + (vars.RCTOutputActive ? 1.0 : 0.0), target, 3);
                read.techniqueMastery = noisy(vars.TechniqueMastery, target, 4);
                read.simpleDomainLevel = noisy(vars.SimpleDomainLevel, target, 5);
                read.bfChance = noisy(vars.BFChance + vars.blackflashmastery, target, 6);
                read.ceShield = noisy(vars.CEShield, target, 7);
                read.barrierlessDomain = vars.BarrierlessDomain;
                read.infusedDomain = vars.InfusedDomain;
                read.simpleDomain = vars.SimpleDomain;
                read.hwbActive = vars.hwb_active;
                read.worldSlash = vars.WorldSlash;
                read.ultimate = vars.Ultimate;
                read.itadoriAwakening = vars.ItadoriAwakening > 0.0;
                read.mahoragaReady = vars.Mahoraga > 0.0 || vars.MahoragaCanAdapt > 0.0;
                read.healBias += Mth.clamp(read.rct / 6.0, 0.0, 0.45);
                read.hwbBias += vars.hwb_active ? 0.6 : 0.0;
                read.tankBias += Mth.clamp((vars.HPCap + vars.HealthAttribute + vars.CEShield) / 120.0, 0.0, 0.35);
            });
            read.domainMastery = noisy(DomainMasterySystem.getFinalMultiplier(target), target, 8);
        }
        read.applyTechniqueBias(target);
        read.applyEffectBias(target);
        return read;
    }

    String techniqueKey() {
        return Math.round(primaryTechnique) + ":" + Math.round(secondaryTechnique);
    }

    private void applyTechniqueBias(LivingEntity target) {
        if (OpSukunaEngineCore.isGojoTarget(target, this)) {
            rangedBias += 0.55;
            domainBias += 0.35;
        }
        if (primaryTechnique == TechniqueIDs.ITADORI || secondaryTechnique == TechniqueIDs.ITADORI || looksLikeItadoriModulo(target)) {
            aggression += 0.65;
            tankBias += 0.35;
        }
        if (primaryTechnique == TechniqueIDs.HAKARI || secondaryTechnique == TechniqueIDs.HAKARI || primaryTechnique == TechniqueIDs.OKKOTSU || secondaryTechnique == TechniqueIDs.OKKOTSU) {
            healBias += 0.35;
            domainBias += 0.25;
        }
        if (primaryTechnique == TechniqueIDs.JOGO || secondaryTechnique == TechniqueIDs.JOGO || primaryTechnique == TechniqueIDs.URAUME || secondaryTechnique == TechniqueIDs.URAUME) {
            rangedBias += 0.45;
        }
        if (barrierlessDomain || infusedDomain || ultimate) {
            domainBias += 0.35;
        }
        if (worldSlash) {
            rangedBias += 0.25;
        }
        if (itadoriAwakening || bfChance > 2.0) {
            aggression += 0.25;
        }
        if (mahoragaReady) {
            tankBias += 0.25;
            domainBias += 0.15;
        }
        if (OpSukunaEngineCore.isGojoTarget(target, this)) {
            aggression += 0.85;
            rangedBias += 0.65;
            healBias += 0.7;
            domainBias += 0.9;
            hwbBias += 0.9;
            tankBias += 0.65;
        }
        aggression = Mth.clamp(aggression, 0.0, 1.0);
        rangedBias = Mth.clamp(rangedBias, 0.0, 1.0);
        healBias = Mth.clamp(healBias, 0.0, 1.0);
        domainBias = Mth.clamp(domainBias, 0.0, 1.0);
        tankBias = Mth.clamp(tankBias, 0.0, 1.0);
        hwbBias = Mth.clamp(hwbBias, 0.0, 1.0);
    }

    private void applyEffectBias(LivingEntity target) {
        if (target.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) domainBias += 0.55;
        if (target.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.SIMPLE_DOMAIN_MAX.get())) hwbBias += 0.25;
        if (target.hasEffect(JujutsucraftaddonModMobEffects.HWB.get())) hwbBias += 0.6;
        if (target.hasEffect(JujutsucraftaddonModMobEffects.DOMAIN_BREAK.get())) domainBias += 0.35;
        if (target.hasEffect(MobEffects.REGENERATION) || target.hasEffect(JujutsucraftaddonModMobEffects.ANTI_HEAL.get())) healBias += 0.35;
        if (target.hasEffect(JujutsucraftaddonModMobEffects.WORLD_CUT.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.WORLD_SLASH_EFFECT.get())) rangedBias += 0.35;
        if (target.hasEffect(JujutsucraftaddonModMobEffects.BLACK_FLASH_CUT.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.KOKUSEN_EFFECT.get())) aggression += 0.4;
        if (target.hasEffect(JujutsucraftaddonModMobEffects.FATIGUE.get())) aggression -= 0.15;
        if (target.hasEffect(JujutsucraftaddonModMobEffects.DODGE.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.COUNTER.get())) fleeBias += 0.25;
        aggression = Mth.clamp(aggression, 0.0, 1.0);
        fleeBias = Mth.clamp(fleeBias, 0.0, 1.0);
        rangedBias = Mth.clamp(rangedBias, 0.0, 1.0);
        healBias = Mth.clamp(healBias, 0.0, 1.0);
        domainBias = Mth.clamp(domainBias, 0.0, 1.0);
        hwbBias = Mth.clamp(hwbBias, 0.0, 1.0);
    }

    private static double noisy(double value, LivingEntity target, int salt) {
        double seed = Math.abs((target.getUUID().getLeastSignificantBits() + salt * 734287L) % 1000L) / 1000.0;
        double error = (seed - 0.5) * 0.30;
        return value * (1.0 + error);
    }

    private static boolean looksLikeItadoriModulo(LivingEntity target) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        String name = id == null ? target.getType().toString().toLowerCase() : id.toString().toLowerCase();
        if (name.contains("itadori") || name.contains("yuji")) return true;
        double skill = target.getPersistentData().getDouble("skill");
        return target.getMaxHealth() >= 60.0 && (target.hasEffect(JujutsucraftaddonModMobEffects.IMBUED_FISTS.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.BLACK_FLASH_CUT.get()) || skill == 21.0);
    }
}
