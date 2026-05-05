package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.entity.SatushiEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModEntities;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.util.DomainMasterySystem;
import com.jujutsu.jujutsucraftaddon.util.OpSukunaBrainMemoryHolder;
import com.jujutsu.jujutsucraftaddon.util.OpSukunaBrainTelemetry;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.entity.SukunaFushiguroEntity;
import net.mcreator.jujutsucraft.entity.SukunaPerfectEntity;
import net.mcreator.jujutsucraft.entity.JujutsuSorcererEntity;
import net.mcreator.jujutsucraft.entity.CurseUserEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.CalculateAttackProcedure;
import net.mcreator.jujutsucraft.procedures.GetDistanceProcedure;
import net.mcreator.jujutsucraft.procedures.KeyReverseCursedTechniqueOnKeyPressedProcedure;
import net.mcreator.jujutsucraft.procedures.KeyReverseCursedTechniqueOnKeyReleasedProcedure;
import net.mcreator.jujutsucraft.procedures.KeySimpleDomainOnKeyPressedProcedure;
import net.mcreator.jujutsucraft.procedures.LogicConfilmDomainProcedure;
import net.mcreator.jujutsucraft.procedures.LogicAttackProcedure;
import net.mcreator.jujutsucraft.procedures.LogicCooldownCombatProcedure;
import net.mcreator.jujutsucraft.procedures.LogicStartPassiveProcedure;
import net.mcreator.jujutsucraft.procedures.LogicStartProcedure;
import net.mcreator.jujutsucraft.procedures.ResetCounterProcedure;
import net.mcreator.jujutsucraft.procedures.ReturnShadowProcedure;
import net.mcreator.jujutsucraft.procedures.StartGuardProcedure;
import net.mcreator.jujutsucraft.procedures.WhenBackStepProcedure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OpSukunaBrain {
    private static final String SAVE_KEY = "JJKUR_OP_AI_RAM";
    private static final double FAST_DISMANTLE = 105.0;
    private static final double CLEAVE = 106.0;
    private static final double OPEN = 107.0;
    private static final double DOMAIN = 20.0;
    private static final double TEN_SHADOWS_UTILITY = 612.0;
    private static final double AGITO = 617.0;
    private static final double MAHORAGA = 618.0;
    private static final double TEN_SHADOWS_DOMAIN = 620.0;
    private static final double PASSIVE_FAST = 111.0;
    private static final double PASSIVE_CLOSE = 112.0;
    private static final double PASSIVE_ANTI_RANGE = 113.0;

    private OpSukunaBrain() {
    }

    public static boolean tryExecute(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, LivingEntity target, CompoundTag nbt) {
        if (!isSupportedSukuna(sukuna)) {
            return false;
        }

        BrainMemory memory = memory(sukuna);
        target = chooseFocusTarget(world, sukuna, target, memory);
        if (target == null || !target.isAlive() || nbt.getDouble("cnt_target") <= 6.0) {
            memory.lastHealth = sukuna.getHealth();
            return false;
        }

        Snapshot s = Snapshot.capture(world, x, y, z, sukuna, target, nbt, memory);
        memory.observe(s);
        maintainKeyedDefenses(s);
        tryEmergencyReaction(s);

        if (nbt.getDouble("skill") != 0.0) {
            return true;
        }

        ResetCounterProcedure.execute(sukuna);
        if (s.selfDomain && s.distance < 48.0 && !sukuna.level().isClientSide()) {
            sukuna.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20, 0, false, false));
        }

        Action best = chooseBestAction(s);
        String topScores = OpSukunaBrainTelemetry.enabled(world) ? describeTopActions(s, best) : "";
        best.execute(world, x, y, z, sukuna, nbt, s);
        recordDecision(world, sukuna, target, s, best, topScores);
        memory.rememberAction(best.name);
        memory.lastAction = best.name;
        memory.lastActionTarget = targetKey(target);
        return true;
    }

    public static CompoundTag save(LivingEntity sukuna) {
        return memory(sukuna).save();
    }

    public static void load(LivingEntity sukuna, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return;
        }
        if (sukuna instanceof OpSukunaBrainMemoryHolder holder) {
            holder.jjkur$setOpSukunaBrainMemory(BrainMemory.load(tag));
        }
    }

    public static String saveKey() {
        return SAVE_KEY;
    }

    private static void recordDecision(LevelAccessor world, LivingEntity sukuna, LivingEntity target, Snapshot s, Action best, String topScores) {
        if (!OpSukunaBrainTelemetry.enabled(world)) {
            return;
        }
        OpSukunaBrainTelemetry.Decision decision = new OpSukunaBrainTelemetry.Decision();
        decision.action = best.name;
        decision.kind = best.kind.name();
        decision.topScores = topScores;
        decision.score = best.score;
        decision.adjustedScore = adjustedScore(s, best);
        decision.skill = best.skill;
        decision.selfHealth = s.selfHealth;
        decision.targetHealth = s.targetHealth;
        decision.selfHealthRatio = s.selfHealthRatio;
        decision.targetHealthRatio = s.targetHealthRatio;
        decision.distance = s.distance;
        decision.predictedDistance = s.predictedDistance;
        decision.damageDealt = s.damageDealt;
        decision.damageTaken = s.damageTaken;
        decision.targetSkill = s.targetSkill;
        decision.archetype = s.archetype;
        decision.techniqueKey = s.read.techniqueKey();
        decision.primaryTechnique = s.read.primaryTechnique;
        decision.secondaryTechnique = s.read.secondaryTechnique;
        decision.targetThreat = s.targetThreat;
        decision.targetPower = s.targetPowerProfile.score;
        decision.meleeThreat = s.meleeThreat;
        decision.rangeThreat = s.rangeThreat;
        decision.pressure = s.pressure;
        decision.opportunity = s.opportunity;
        decision.immediateThreat = s.immediateThreat;
        decision.survivalUrgency = s.survivalUrgency;
        decision.incomingKillRisk = s.incomingKillRisk;
        decision.hitConfidence = s.hitConfidence;
        decision.whiffOpportunity = s.whiffOpportunity;
        decision.stuckLevel = s.stuckLevel;
        decision.groupPressure = s.groupPressure;
        decision.groupActivePressure = s.groupActivePressure;
        decision.groupApexPressure = s.groupApexPressure;
        decision.rctStrain = s.rctStrain;
        decision.brainDamage = s.brainDamageLevel;
        decision.idealDistance = s.idealDistance;
        decision.gojo = s.read.primaryTechnique == TechniqueIDs.GOJO || s.read.secondaryTechnique == TechniqueIDs.GOJO || isSatushi(target);
        decision.targetDomain = s.targetDomain;
        decision.targetCastingDomain = s.targetCastingDomain;
        decision.targetCooldown = s.targetCooldown;
        decision.targetUnstable = s.targetUnstable;
        decision.targetDodge = s.targetDodge;
        decision.targetCounter = s.targetCounter;
        decision.targetGuard = s.targetGuard;
        decision.targetEscaping = s.targetEscaping;
        decision.targetWhiffed = s.targetWhiffed;
        decision.targetOverextended = s.targetOverextended;
        decision.clearShot = s.clearShot;
        decision.pathBlocked = s.pathBlocked;
        decision.normalReady = s.normalReady;
        decision.passiveReady = s.passiveReady;
        decision.domainReady = s.domainReady;
        decision.worldCutCapable = s.worldCutCapable;
        decision.trivialTarget = s.trivialTarget;
        decision.mahoragaAvailable = s.canUseMahoraga || s.mahoragaExist;
        decision.enemyDomainTrap = s.enemyDomainTrap;
        decision.domainEscapeWindow = s.domainEscapeWindow;
        decision.domainResponseTicks = s.memory.firstDomainThreatTick < 0.0 ? -1.0 : s.tick - s.memory.firstDomainThreatTick;
        decision.purpleThreat = s.purpleThreat;
        decision.purpleRisk = s.purpleRisk;
        decision.purpleEvade = "PURPLE_EVADE".equals(best.name) || "PURPLE_BACKSTEP".equals(best.name);
        decision.purpleHit = s.purpleThreat && s.damageTaken > sukuna.getMaxHealth() * 0.08;
        decision.purpleAvoided = s.memory.pendingPurpleResponseTick >= 0.0 && s.tick - s.memory.pendingPurpleResponseTick <= 45.0 && s.damageTaken <= sukuna.getMaxHealth() * 0.02;
        decision.stuck = s.stuckLevel > 0.62;
        decision.infinityWaste = s.infinitySignal > 0.0 && !s.selfDomain && !best.worldCut && best.skill != 0.0
                && best.kind != ActionKind.DOMAIN_AMPLIFICATION && best.kind != ActionKind.TEN_SHADOWS && best.kind != ActionKind.DOMAIN;
        decision.adaptationConfidence = s.adaptation.confidence;
        decision.trapPressure = s.adaptation.trapMastery;
        decision.evasionPressure = s.adaptation.antiEvasionNeed;
        decision.rangePressure = s.adaptation.rangeKiteNeed;
        decision.domainPlan = s.domainAssessment.plan;
        decision.domainClearValue = s.domainAssessment.clearValue;
        decision.domainSustainCost = s.domainAssessment.sustainCost;
        decision.domainRecoveryRisk = s.domainAssessment.recoveryRisk;
        decision.domainCounterRisk = s.domainAssessment.counterRisk;
        decision.domainCastScore = s.domainAssessment.castScore;
        decision.domainHwbScore = s.domainAssessment.hwbScore;
        decision.domainPunishScore = s.domainAssessment.punishScore;
        decision.domainScore = s.domainAssessment.domainScore;
        decision.preferredRange = s.adaptation.preferredRange;
        decision.safePunishWindow = s.adaptation.safePunishWindow;
        decision.learnedBurstRisk = s.adaptation.learnedBurstRisk;
        OpSukunaBrainTelemetry.recordDecision(world, sukuna, target, decision);
    }

    private static BrainMemory memory(LivingEntity sukuna) {
        if (sukuna instanceof OpSukunaBrainMemoryHolder holder) {
            return holder.jjkur$getOpSukunaBrainMemory();
        }
        return new BrainMemory();
    }

    private static boolean isSupportedSukuna(LivingEntity entity) {
        return entity instanceof SukunaPerfectEntity
                || entity instanceof SukunaFushiguroEntity
                || entity instanceof com.jujutsu.jujutsucraftaddon.entity.SukunaFushiguroEntity
                || entity instanceof com.jujutsu.jujutsucraftaddon.entity.SukunaMangaEntity;
    }

    private static boolean hasWorldCut(LivingEntity entity) {
        return entity instanceof SukunaPerfectEntity
                || (entity instanceof SukunaFushiguroEntity sf && sf.getEntityData().get(SukunaFushiguroEntity.DATA_world_cut));
    }

    private static boolean isFushiguroBody(LivingEntity entity) {
        return entity instanceof SukunaFushiguroEntity
                || entity instanceof com.jujutsu.jujutsucraftaddon.entity.SukunaFushiguroEntity
                || entity instanceof com.jujutsu.jujutsucraftaddon.entity.SukunaMangaEntity;
    }

    private static boolean isPerfectMode(LivingEntity entity) {
        return entity instanceof SukunaFushiguroEntity sf && sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode);
    }

    private static double rctLimit(LivingEntity sukuna) {
        return sukuna.getMaxHealth() >= 800.0F ? 400.0 : 200.0;
    }

    private static double rctLevel(LivingEntity sukuna) {
        return sukuna.getCapability(com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .map(vars -> vars.RCTLimitLevel > 0.0 ? Math.min(Math.round(vars.RCTCount / 5000.0), vars.RCTLimitLevel) : Math.round(vars.RCTCount / 5000.0))
                .orElse(0.0);
    }

    private static double brainDamageLevel(LivingEntity sukuna) {
        double effect = sukuna.hasEffect(JujutsucraftModMobEffects.BRAIN_DAMAGE.get()) ? sukuna.getEffect(JujutsucraftModMobEffects.BRAIN_DAMAGE.get()).getAmplifier() + 1.0 : 0.0;
        double capability = sukuna.getCapability(com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .map(vars -> vars.BrainDamage)
                .orElse(0.0);
        return Math.max(effect, capability);
    }

    private static LivingEntity chooseFocusTarget(LevelAccessor world, LivingEntity sukuna, LivingEntity current, BrainMemory memory) {
        ThreatScan scan = ThreatScan.scan(world, sukuna, current, memory);
        LivingEntity best = scan.bestTarget;
        if (best == null) {
            return current;
        }
        if (current == null || !current.isAlive() || !isValidEnemy(world, sukuna, current)) {
            switchTarget(sukuna, best, memory);
            return best;
        }

        String currentKey = targetKey(current);
        String bestKey = targetKey(best);
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
            switchTarget(sukuna, best, memory);
            return best;
        }
        memory.lastPrimaryTarget = currentKey;
        return current;
    }

    private static void switchTarget(LivingEntity sukuna, LivingEntity target, BrainMemory memory) {
        if (sukuna instanceof Mob mob) {
            mob.setTarget(target);
        }
        memory.lastPrimaryTarget = targetKey(target);
        memory.lastTargetSwitchTick = sukuna.tickCount;
    }

    private static boolean isValidEnemy(LevelAccessor world, LivingEntity sukuna, LivingEntity candidate) {
        if (candidate == sukuna || !candidate.isAlive() || candidate.isSpectator()) {
            return false;
        }
        if (candidate instanceof Player player && player.getAbilities().instabuild) {
            return false;
        }
        if (isAlly(sukuna, candidate)) {
            return false;
        }
        return LogicAttackProcedure.execute(world, sukuna, candidate);
    }

    private static boolean isAlly(LivingEntity sukuna, LivingEntity candidate) {
        CompoundTag self = sukuna.getPersistentData();
        CompoundTag other = candidate.getPersistentData();
        double friend = self.getDouble("friend_num");
        if (friend != 0.0 && (friend == other.getDouble("friend_num") || friend == other.getDouble("friend_num2") || friend == other.getDouble("friend_num_worker"))) {
            return true;
        }
        String selfUuid = sukuna.getStringUUID();
        String otherUuid = candidate.getStringUUID();
        String selfOwner = self.getString("OWNER_UUID");
        String otherOwner = other.getString("OWNER_UUID");
        if ((!selfOwner.isEmpty() && (selfOwner.equals(otherUuid) || selfOwner.equals(otherOwner)))
                || (!otherOwner.isEmpty() && (otherOwner.equals(selfUuid) || otherOwner.equals(selfOwner)))) {
            return true;
        }
        if (sukuna instanceof TamableAnimal tameSelf && tameSelf.getOwner() == candidate) {
            return true;
        }
        if (candidate instanceof TamableAnimal tameOther && tameOther.getOwner() == sukuna) {
            return true;
        }
        if (sukuna instanceof TamableAnimal tameSelf && candidate instanceof TamableAnimal tameOther
                && tameSelf.getOwnerUUID() != null && tameSelf.getOwnerUUID().equals(tameOther.getOwnerUUID())) {
            return true;
        }
        return false;
    }

    private static boolean hasEntityTypeTag(Entity entity, String tag) {
        return entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(tag)));
    }

    private static boolean isSatushi(LivingEntity target) {
        return target instanceof SatushiEntity;
    }

    private static boolean hasClearShot(LevelAccessor world, LivingEntity attacker, LivingEntity target, double lenience) {
        if (!(world instanceof Level level)) {
            return true;
        }
        Vec3 start = attacker.getEyePosition(1.0F);
        Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
        HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(end) <= lenience * lenience;
    }

    private static boolean hasClearMovementSegment(LevelAccessor world, LivingEntity entity, Vec3 destination, double lenience) {
        if (!(world instanceof Level level)) {
            return true;
        }
        Vec3 start = entity.position().add(0.0, Math.min(1.2, entity.getBbHeight() * 0.45), 0.0);
        Vec3 end = destination.add(0.0, Math.min(1.2, entity.getBbHeight() * 0.45), 0.0);
        HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(end) <= lenience * lenience;
    }

    private static boolean pathBlocked(LevelAccessor world, LivingEntity entity, Vec3 destination) {
        if (!hasClearMovementSegment(world, entity, destination, Math.max(1.2, entity.getBbWidth()))) {
            return true;
        }
        if (entity instanceof Mob mob && destination.distanceToSqr(entity.position()) < 32.0 * 32.0) {
            net.minecraft.world.level.pathfinder.Path path = mob.getNavigation().createPath(destination.x, destination.y, destination.z, 0);
            if (path == null || path.getNodeCount() <= 0) {
                return true;
            }
            if (path.canReach()) {
                return false;
            }
            Vec3 end = path.getEndNode() == null ? entity.position() : new Vec3(path.getEndNode().x, path.getEndNode().y, path.getEndNode().z);
            return end.distanceToSqr(destination) > 5.0 * 5.0;
        }
        return false;
    }

    private static boolean isWalkableDestination(LevelAccessor world, LivingEntity entity, Vec3 destination) {
        if (!(world instanceof Level level)) {
            return true;
        }
        AABB box = entity.getBoundingBox().move(destination.subtract(entity.position()));
        if (!level.noCollision(entity, box.deflate(0.02))) {
            return false;
        }
        BlockPos below = BlockPos.containing(destination.x, destination.y - 0.2, destination.z);
        return level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)
                || !level.getBlockState(BlockPos.containing(destination)).getCollisionShape(level, BlockPos.containing(destination), CollisionContext.of(entity)).isEmpty();
    }

    private static boolean canUseRct(LivingEntity sukuna) {
        if (sukuna.hasEffect(JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get())
                || sukuna.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())
                || sukuna.getPersistentData().getDouble("skill") != 0.0
                || sukuna.hasEffect(JujutsucraftaddonModMobEffects.ANTI_HEAL.get())
                || sukuna.hasEffect(JujutsucraftaddonModMobEffects.RCT_CUT.get())) {
            return false;
        }
        if (sukuna.getPersistentData().getBoolean("CursedSpirit") || sukuna.getPersistentData().getDouble("CursedSpirit") == 1.0) {
            return true;
        }
        return hasEntityTypeTag(sukuna, "jujutsucraft:can_use_reverse_cursed_technique")
                || sukuna.getPersistentData().getBoolean("entity_can_use_rct");
    }

    private static boolean canUseSimpleDomain(LivingEntity sukuna) {
        if (sukuna.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())
                || sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_SIMPLE_DOMAIN.get())
                || sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
            return false;
        }
        if (sukuna.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())
                && sukuna.getEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).getAmplifier() > 0) {
            return false;
        }
        return hasEntityTypeTag(sukuna, "jujutsucraft:can_use_simple_domain")
                || hasEntityTypeTag(sukuna, "jujutsucraft:can_use_hollow_wicker_basket")
                || sukuna.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get());
    }

    private static boolean canUseBurnoutRct(LivingEntity sukuna) {
        if (!(sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get()) || sukuna.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get()))
                || sukuna.hasEffect(JujutsucraftaddonModMobEffects.BROKEN_BRAIN.get())
                || sukuna.getPersistentData().getDouble("skill") != 0.0) {
            return false;
        }
        double brainDamage = sukuna.getCapability(com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .orElse(new com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables.PlayerVariables()).BrainDamage;
        return brainDamage < 5.0 && sukuna.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get());
    }

    private static int effectAmplifier(LivingEntity entity, MobEffect effect) {
        MobEffectInstance instance = entity.getEffect(effect);
        return instance == null ? -1 : instance.getAmplifier();
    }

    private static int effectDuration(LivingEntity entity, MobEffect effect) {
        MobEffectInstance instance = entity.getEffect(effect);
        return instance == null ? 0 : instance.getDuration();
    }

    private static double domainNumber(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        double selected = data.getDouble("select");
        double skillDomain = data.getDouble("skill_domain");
        if (skillDomain > 0.0) return skillDomain;
        return selected > 0.0 ? selected : 0.0;
    }

    private static boolean isDomainCastSignal(LivingEntity target, double targetSkill, double targetDomainNumber, PlayerRead read) {
        int roundedSkill = (int) Math.round(Math.abs(targetSkill));
        boolean domainSkill = targetSkill == 20.0 || targetSkill == 220.0 || roundedSkill % 100 == 20;
        boolean gojoStartup = (read.primaryTechnique == TechniqueIDs.GOJO || read.secondaryTechnique == TechniqueIDs.GOJO || isSatushi(target))
                && target.getPersistentData().getDouble("cnt1") > 0.0
                && target.getPersistentData().getDouble("cnt1") < 55.0;
        return domainSkill || targetDomainNumber == 2.0 || gojoStartup
                || target.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get())
                || target.hasEffect(JujutsucraftModMobEffects.BRAIN_DAMAGE.get());
    }

    private static boolean hasDomainEscapeCandidate(Snapshot s) {
        Vec3 self = s.sukuna.position();
        Vec3 away = s.domainEscapeVector.lengthSqr() > 1.0E-4 ? s.domainEscapeVector : horizontal(self.subtract(s.target.position()));
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(1.0, 0.0, 0.0);
        }
        Vec3 lateral = new Vec3(-away.z, 0.0, away.x).normalize();
        return canUseEscapeCandidate(s, self.add(away.scale(9.0)))
                || canUseEscapeCandidate(s, self.add(away.scale(7.0)).add(lateral.scale(4.0)))
                || canUseEscapeCandidate(s, self.add(away.scale(7.0)).add(lateral.scale(-4.0)))
                || canUseEscapeCandidate(s, self.add(away.scale(5.0)).add(0.0, 1.0, 0.0));
    }

    private static boolean canUseEscapeCandidate(Snapshot s, Vec3 candidate) {
        if (!isWalkableDestination(s.world, s.sukuna, candidate) || !hasClearMovementSegment(s.world, s.sukuna, candidate, 1.2)) {
            return false;
        }
        return candidate.distanceToSqr(s.target.position()) > s.sukuna.position().distanceToSqr(s.target.position()) + 18.0;
    }

    private static boolean hasExtremeVoidDebuff(LivingEntity entity) {
        return effectAmplifier(entity, MobEffects.MOVEMENT_SLOWDOWN) >= 4
                || effectAmplifier(entity, MobEffects.BLINDNESS) >= 0
                || effectAmplifier(entity, MobEffects.WEAKNESS) >= 3;
    }

    private static Action chooseBestAction(Snapshot s) {
        if (s.purpleThreat) {
            Action purple = bestOf(s, List.of(
                    Action.move("PURPLE_EVADE", Movement.PURPLE_EVADE, scorePurpleEvade(s)),
                    Action.worldCut("PURPLE_INTERRUPT", scoreWorldCut(s) + (s.purpleWindup ? 0.8 : 0.15)),
                    Action.domainAmplification(scoreDomainAmplification(s) + 0.35),
                    Action.backstep("PURPLE_BACKSTEP", scoreEvasiveBackstep(s) + s.purpleRisk * 1.2)));
            if (purple.score > 0.55) return purple;
        }

        if (s.lethalForecast || s.survivalMode) {
            Action survival = bestOf(s, List.of(
                    Action.backstep("SURVIVAL_RESET", scoreBackstep(s) + 1.1),
                    Action.guard("PANIC_SURVIVAL", scoreGuard(s) + 0.8),
                    Action.guardTiming(scoreGuardTiming(s) + 0.85),
                    Action.calculate("BLACK_FLASH_RECOVERY", scoreBlackFlashRecovery(s)),
                    Action.rct("RCT", scoreRct(s) + 0.55),
                    Action.burnoutRct(scoreBurnoutRct(s) + 0.5),
                    Action.simpleDomain(scoreSimpleDomain(s) + 0.55),
                    Action.domain(scoreDomain(s) + (s.domainDeathSpiral ? 0.9 : 0.0)),
                    Action.worldCut("SURVIVAL_CUT", scoreWorldCut(s) + (s.domainDeathSpiral || s.blackFlashChain ? 0.65 : 0.0)),
                    Action.mahoraga(scoreMahoraga(s) + 0.7),
                    Action.heianReset(scoreHeianReset(s))));
            if (survival.score > 0.65) return survival;
        }

        if (s.targetDomain || s.targetCastingDomain || s.domainAssessment.targetDomainThreat > 0.55) {
            Action antiDomain = bestOf(s, List.of(
                    Action.move("DomainEscape", Movement.DOMAIN_ESCAPE, scoreDomainEscape(s)),
                    Action.simpleDomain(scoreSimpleDomain(s) + (s.simpleDomainExpiresSoon || s.domainDeathSpiral ? 0.25 : 1.0)),
                    Action.domain(scoreDomain(s) + (s.catastrophicDomain ? 0.9 : 0.6) + (s.domainDeathSpiral ? 0.8 : 0.0)),
                    Action.tenShadowsDomain(scoreTenShadowsDomain(s) + 0.6 + (s.catastrophicDomain ? 0.35 : 0.0)),
                    Action.worldCut("INTERRUPT", scoreWorldCut(s) + 0.35 + (s.catastrophicDomain || s.domainDeathSpiral ? 0.55 : 0.0)),
                    Action.mahoraga(scoreMahoraga(s) + (s.catastrophicDomain ? 0.45 : 0.0)),
                    Action.move("MaintainRange", Movement.MAINTAIN_RANGE, scoreMaintainRange(s) + (s.pathBlocked ? -0.35 : 0.25))));
            if (antiDomain.score > 0.55) return antiDomain;
        }

        if (s.infinitySignal > 0.0) {
            Action antiInfinity = bestOf(s, List.of(
                    Action.domainAmplification(scoreDomainAmplification(s) + 0.8),
                    Action.domain(scoreDomain(s) + 0.35),
                    Action.worldCut("ANTI_INFINITY", scoreWorldCut(s) + 0.55),
                    Action.mahoraga(scoreMahoraga(s) + 0.25),
                    Action.move("MaintainRange", Movement.MAINTAIN_RANGE, scoreMaintainRange(s) + (s.pathBlocked ? -0.35 : 0.0))));
            if (antiInfinity.score > 0.45) return antiInfinity;
        }

        if (s.targetCooldown || s.targetUnstable || s.targetAttacking) {
            Action punish = bestOf(s, offensiveActions(s, "PUNISH"));
            if (punish.score > 0.72) return punish;
        }

        if (s.killConfirm) {
            Action finisher = bestOf(s, offensiveActions(s, "KILL_CONFIRM"));
            if (finisher.score > 0.65) return finisher;
        }

        if (s.trivialTarget) {
            return bestOf(s, List.of(
                    Action.skill("RESOURCE_EFFICIENT", FAST_DISMANTLE, 50.0, false, scoreDismantle(s) + 0.45),
                    Action.skill("TrivialCleave", CLEAVE, 100.0, false, scoreCleave(s) + 0.25),
                    Action.calculate("TRIVIAL_CLEANUP", scoreBasic(s) + 0.4)));
        }

        List<Action> actions = baselineActions(s);
        Action best = actions.get(0);
        double bestScore = adjustedScore(s, best);
        for (Action action : actions) {
            double adjusted = adjustedScore(s, action);
            if (adjusted > bestScore) {
                best = action;
                bestScore = adjusted;
            } else if (s.isMeguna && adjusted >= bestScore - 0.16 && shouldPreferDiverseTie(s, best, action)) {
                best = action;
                bestScore = adjusted;
            }
        }
        return best;
    }

    private static List<Action> baselineActions(Snapshot s) {
        List<Action> actions = new ArrayList<>();
        actions.add(Action.backstep(scoreBackstep(s)));
        actions.add(Action.guard(scoreGuard(s)));
        actions.add(Action.guardTiming(scoreGuardTiming(s)));
        actions.add(Action.backstep("EvasiveBackstep", scoreEvasiveBackstep(s)));
        actions.add(Action.rct("RCT", scoreRct(s)));
        actions.add(Action.burnoutRct(scoreBurnoutRct(s)));
        actions.add(Action.simpleDomain(scoreSimpleDomain(s)));
        actions.add(Action.domainAmplification(scoreDomainAmplification(s)));
        actions.add(Action.calculate("Chase", scoreChase(s)));
        actions.add(Action.calculate("CalculateAttack", scoreBasic(s)));
        actions.add(Action.calculate("BLACK_FLASH_RECOVERY", scoreBlackFlashRecovery(s)));
        actions.add(Action.move("ProjectileDodge", Movement.PROJECTILE_DODGE, scoreProjectileDodge(s)));
        actions.add(Action.move("PURPLE_EVADE", Movement.PURPLE_EVADE, scorePurpleEvade(s)));
        actions.add(Action.move("DomainEscape", Movement.DOMAIN_ESCAPE, scoreDomainEscape(s)));
        actions.add(Action.move("MaintainRange", Movement.MAINTAIN_RANGE, scoreMaintainRange(s)));
        actions.add(Action.move("CutOffEscape", Movement.CUT_OFF_ESCAPE, scoreCutOffEscape(s)));
        actions.add(Action.move("StrafePressure", Movement.STRAFE_PRESSURE, scoreStrafePressure(s)));
        actions.add(Action.move("BaitWhiff", Movement.BAIT_WHIFF, scoreBaitWhiff(s)));
        actions.addAll(offensiveActions(s, ""));
        actions.add(Action.summon(scoreSummon(s)));
        actions.add(Action.agito(scoreAgito(s)));
        actions.add(Action.mahoraga(scoreMahoraga(s)));
        actions.add(Action.tenShadowsDomain(scoreTenShadowsDomain(s)));
        actions.add(Action.heianReset(scoreHeianReset(s)));
        return actions;
    }

    private static List<Action> offensiveActions(Snapshot s, String priorityName) {
        List<Action> actions = new ArrayList<>();
        String prefix = priorityName == null || priorityName.isEmpty() ? "" : priorityName + "_";
        if (s.normalReady) {
            actions.add(Action.skill(prefix + "Dismantle", FAST_DISMANTLE, 50.0, false, scoreDismantle(s)));
            actions.add(Action.skill(prefix + "Cleave", CLEAVE, 100.0, false, scoreCleave(s)));
            actions.add(Action.skill(prefix + "Open", OPEN, 250.0, false, scoreOpen(s)));
            if (s.worldCutCapable) {
                actions.add(Action.worldCut(prefix + "WorldCut", scoreWorldCut(s)));
            }
            if (s.domainReady && !s.selfDomain && !s.brainDamaged) {
                actions.add(Action.domain(scoreDomain(s)));
            }
        }
        if (s.passiveReady && !s.combatCooldown && !s.infinity) {
            actions.add(Action.skill(prefix + "Passive111", PASSIVE_FAST, 50.0, true, scorePassive111(s)));
            actions.add(Action.skill(prefix + "Passive112", PASSIVE_CLOSE, 50.0, true, scorePassive112(s)));
            actions.add(Action.skill(prefix + "Passive113", PASSIVE_ANTI_RANGE, 50.0, true, scorePassive113(s)));
        }
        if (actions.isEmpty()) {
            actions.add(Action.calculate(prefix + "CalculateAttack", scoreBasic(s)));
        }
        return actions;
    }

    private static Action bestOf(Snapshot s, List<Action> actions) {
        Action best = actions.get(0);
        double bestScore = adjustedScore(s, best);
        for (Action action : actions) {
            double adjusted = adjustedScore(s, action);
            if (adjusted > bestScore) {
                best = action;
                bestScore = adjusted;
            } else if (s.isMeguna && adjusted >= bestScore - 0.16 && shouldPreferDiverseTie(s, best, action)) {
                best = action;
                bestScore = adjusted;
            }
        }
        return best;
    }

    private static double adjustedScore(Snapshot s, Action action) {
        return action.score + s.memory.actionBias(action.name) + s.adaptation.actionBias(action.name);
    }

    private static boolean shouldPreferDiverseTie(Snapshot s, Action best, Action candidate) {
        if (candidate.name.equals(s.memory.lastAction) || candidate.score < 0.35) {
            return false;
        }
        if (best.name.equals(s.memory.lastAction) && s.memory.lastActionStreak >= 2) {
            return true;
        }
        int bucket = Math.floorMod(candidate.name.hashCode() ^ targetKey(s.target).hashCode() ^ ((int) s.tick / 20), 5);
        return bucket == 0 && candidate.kind != ActionKind.DOMAIN && !candidate.name.equals(best.name);
    }

    private static String describeTopActions(Snapshot s, Action selected) {
        List<Action> actions = baselineActions(s);
        if (s.purpleThreat) {
            actions.add(Action.move("PURPLE_EVADE", Movement.PURPLE_EVADE, scorePurpleEvade(s)));
            actions.add(Action.worldCut("PURPLE_INTERRUPT", scoreWorldCut(s) + (s.purpleWindup ? 0.8 : 0.15)));
        }
        if (s.targetDomain || s.targetCastingDomain || s.domainAssessment.targetDomainThreat > 0.55) {
            actions.add(Action.move("DomainEscape", Movement.DOMAIN_ESCAPE, scoreDomainEscape(s)));
            actions.add(Action.domain(scoreDomain(s)));
            actions.add(Action.tenShadowsDomain(scoreTenShadowsDomain(s)));
        }
        actions.sort(Comparator.comparingDouble((Action action) -> adjustedScore(s, action)).reversed());
        StringBuilder builder = new StringBuilder(selected.name).append('=').append(round(selected.score));
        int limit = Math.min(5, actions.size());
        for (int i = 0; i < limit; i++) {
            Action action = actions.get(i);
            builder.append(';').append(action.name).append('=').append(round(adjustedScore(s, action)));
        }
        return builder.toString();
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static double scoreSimpleDomain(Snapshot s) {
        if (!s.canUseSimpleDomain || s.selfSimpleDomain || s.selfDomain) return -1.0;
        double bridgePenalty = s.catastrophicDomain && (s.domainDeathSpiral || s.simpleDomainExpiresSoon || s.healthLosingRace) ? 0.65 : 0.0;
        return s.domainAssessment.hwbScore - bridgePenalty;
    }

    private static double scoreDomainEscape(Snapshot s) {
        if (!s.domainEscapeWindow || s.selfDomain || s.selfSimpleDomain) return -1.0;
        return 0.25 + s.domainAssessment.targetDomainThreat * 1.45 + (s.catastrophicDomain ? 0.65 : 0.0)
                + (s.pathBlocked ? -0.45 : 0.0) + close(s.distance, 28.0) * 0.35;
    }

    private static double scoreGuard(Snapshot s) {
        if (s.sukuna.hasEffect(JujutsucraftModMobEffects.GUARD.get()) || s.sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_GUARD.get())) return -1.0;
        double recency = s.tick - s.memory.lastGuardTick < 14.0 ? -1.2 : 0.0;
        double comboRisk = s.meleeThreat * close(s.distance, s.itadoriModulo ? 10.0 : 7.0) + s.targetSkillDanger * 0.65 + s.damageTakenBurst * 1.8 + s.immediateThreat * 0.9;
        double domainPenalty = s.targetDomain || s.targetCastingDomain ? -0.35 : 0.0;
        double chainPenalty = s.blackFlashChain && s.healthLosingRace ? -0.5 : 0.0;
        return recency - 0.05 + comboRisk + domainPenalty + chainPenalty + s.survivalUrgency * 0.35 + s.adaptation.learnedBurstRisk * 0.22;
    }

    private static double scoreGuardTiming(Snapshot s) {
        if (s.sukuna.hasEffect(JujutsucraftModMobEffects.GUARD.get()) || s.sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_GUARD.get())) return -1.0;
        double recency = s.tick - s.memory.lastGuardTimingTick < 5.0 ? -1.4 : 0.0;
        double startup = s.targetSkillStartup ? 1.15 : 0.0;
        double hitNow = s.immediateThreat * 1.95 + s.meleeThreat * close(s.predictedDistance, s.itadoriModulo ? 13.0 : 8.0) * 1.15;
        return recency + hitNow + startup + s.damageTakenBurst * 0.9 + s.survivalUrgency * 0.25 - (s.targetDomain || s.targetCastingDomain ? 0.25 : 0.0);
    }

    private static double scoreEvasiveBackstep(Snapshot s) {
        double recency = s.tick - Math.max(s.memory.lastBackstepTick, s.memory.lastEvasiveBackstepTick) < 6.0 ? -1.3 : 0.0;
        double projectile = s.incomingProjectileRisk * 2.8;
        double burst = s.targetSkillStartup && s.predictedDistance < (s.itadoriModulo ? 15.0 : 10.0) ? 1.25 : 0.0;
        double closePunish = s.meleeThreat * close(s.distance, s.itadoriModulo ? 12.0 : 7.0);
        return recency + projectile + burst + closePunish + s.immediateThreat * 0.85 + s.survivalUrgency * 0.25;
    }

    private static double scoreRct(Snapshot s) {
        if (!s.canUseRct || s.selfRct || s.selfAntiHeal) return -1.0;
        if (s.rctStrain > 0.92) return -1.0;
        double recency = s.tick - s.memory.lastRecoveryWindowTick < 24.0 ? -1.1 : 0.0;
        double health = Mth.clamp((0.78 - s.selfHealthRatio) * 2.2, 0.0, 1.5);
        double burst = s.damageTakenBurst * 1.35 + s.recentDamageRatio * 5.0;
        double lethal = s.lethalForecast ? 0.9 : 0.0;
        double strainPenalty = s.rctStrain > 0.68 ? (s.rctStrain - 0.68) * 2.4 : 0.0;
        return recency - 0.2 + health + burst + lethal - strainPenalty;
    }

    private static double scoreBurnoutRct(Snapshot s) {
        if (!s.canUseBurnoutRct || s.nbt.getBoolean("PRESS_BURNOUT")) return -1.0;
        double recency = s.tick - s.memory.lastBurnoutRctTick < 160.0 ? -1.5 : 0.0;
        double value = (s.sukuna.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get()) ? 1.15 : 0.0)
                + (s.sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get()) ? 0.85 : 0.0)
                + (s.lethalForecast ? 0.35 : 0.0);
        return recency + value;
    }

    private static double scoreDomainAmplification(Snapshot s) {
        if (s.sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get()) || s.selfDomain) return -1.0;
        double recency = s.tick - s.memory.lastDomainAmplificationTick < 45.0 ? -0.6 : 0.0;
        double infinityValue = s.infinitySignal * (s.distance < 12.0 ? 1.9 : 1.35);
        double domainValue = (s.targetNeutralization || s.targetDomainAmplification ? 0.35 : 0.0);
        double burstValue = s.itadoriModulo && s.distance < 10.0 ? 0.75 : 0.0;
        return recency - 0.05 + infinityValue + domainValue + burstValue + s.targetSkillDanger * 0.2 + (s.purpleWindup ? 0.25 : 0.0);
    }

    private static double scoreBackstep(Snapshot s) {
        double recency = s.tick - s.memory.lastBackstepTick < 8.0 ? -2.0 : 0.0;
        double burstRange = s.itadoriModulo ? close(s.distance, 11.0) * 1.35 : close(s.distance, 7.5) * 0.8;
        double danger = s.dangerArea * 1.15 + s.targetSkillDanger * 0.8 + s.meleeThreat * burstRange + s.damageTakenBurst * 2.0;
        double health = (1.0 - s.selfHealthRatio) * s.recentDamageRatio * 6.0;
        double punishLoss = s.targetCooldown || s.targetUnstable ? -0.4 : 0.0;
        return recency + danger + health + punishLoss + s.survivalUrgency * 0.35;
    }

    private static double scoreBlackFlashRecovery(Snapshot s) {
        if (s.infinitySignal > 0.0 || s.selfDomain || s.combatCooldown || s.targetGuard || s.targetDodge || s.targetCounter) return -1.0;
        if (!s.rctFatigued && !s.survivalMode) return -0.35;
        if (s.itadoriModulo && !s.rctFatigued && s.whiffOpportunity < 0.45 && s.predictedDistance > 5.5) return -0.75;
        double range = band(s.predictedDistance, 2.0, 6.8);
        double window = s.whiffOpportunity * 0.65 + (s.targetCooldown || s.targetUnstable ? 0.45 : 0.0) + (s.targetOverextended ? 0.25 : 0.0);
        double tooDangerous = s.lethalForecast && s.incomingKillRisk > 1.05 && window < 0.45 ? 0.75 : 0.0;
        return -0.1 + s.blackFlashRecoveryValue * 0.85 + range * 0.75 + window + s.hitConfidence * 0.25 - tooDangerous;
    }

    private static double scoreProjectileDodge(Snapshot s) {
        double recency = s.tick - s.memory.lastProjectileDodgeTick < 10.0 ? -1.2 : 0.0;
        return recency + s.incomingProjectileRisk * 3.6 + s.targetSkillDanger * 0.35 + s.immediateThreat * 0.35;
    }

    private static double scorePurpleEvade(Snapshot s) {
        if (!s.purpleThreat) return -1.0;
        double recency = s.tick - s.memory.lastPurpleEvadeTick < 8.0 ? -0.55 : 0.0;
        double windupInterruptWindow = s.purpleWindup && s.clearShot && s.worldCutCapable ? -0.15 : 0.0;
        return recency + s.purpleRisk * 3.0 + (s.purpleProjectile ? 0.65 : 0.0) + (s.purpleLineOfFire ? 0.55 : 0.0)
                + (s.distance < 22.0 ? 0.25 : 0.0) + windupInterruptWindow;
    }

    private static double scoreMaintainRange(Snapshot s) {
        if (s.pathBlocked || s.stuckLevel > 0.55) return -0.35 + s.groupActivePressure * 0.2;
        double error = Math.abs(s.predictedDistance - s.idealDistance) / Math.max(8.0, s.idealDistance);
        double unsafeClose = s.meleeThreat * close(s.distance, s.itadoriModulo ? 8.0 : 6.0) * (s.itadoriModulo ? 0.55 : 0.7);
        return 0.16 + Mth.clamp(error, 0.0, 0.9) + unsafeClose + s.dangerArea * 0.15
                + s.groupPressure * 0.35 + s.groupActivePressure * 0.25 + s.groupApexPressure * 0.2
                + s.adaptation.rangeKiteNeed * 0.38 + s.adaptation.trapMastery * 0.18
                + (s.itadoriModulo && s.distance > 16.0 ? -0.45 : 0.0);
    }

    private static double scoreCutOffEscape(Snapshot s) {
        return 0.12 + (s.targetEscaping ? 0.75 : 0.0) + s.targetFleeBias * 0.55 + s.adaptation.rangeKiteNeed * 0.35
                + (s.targetHealthRatio < 0.35 ? 0.25 : 0.0);
    }

    private static double scoreStrafePressure(Snapshot s) {
        double range = band(s.distance, s.itadoriModulo ? 6.0 : 5.0, s.itadoriModulo ? 16.0 : 24.0);
        double cooldown = s.normalReady ? -0.18 : 0.18;
        return 0.18 + range * 0.42 + s.rangeThreat * 0.25 + s.meleeThreat * 0.22 + s.whiffOpportunity * 0.5 + cooldown
                + s.adaptation.trapMastery * 0.32 + s.adaptation.rangeKiteNeed * 0.2
                + (s.itadoriModulo && !s.pathBlocked ? 0.2 : 0.0) + s.stuckLevel * 0.25;
    }

    private static double scoreBaitWhiff(Snapshot s) {
        if (s.selfDomain || s.targetDomain || s.targetCastingDomain) return -0.4;
        double band = band(s.distance, s.itadoriModulo ? 8.0 : 5.0, s.itadoriModulo ? 14.0 : 13.0);
        double bait = s.meleeThreat * 0.55 + s.read.aggression * 0.45 + s.targetSkillDanger * 0.25;
        return 0.1 + band * 0.45 + bait + (s.normalReady ? -0.18 : 0.12) + s.whiffOpportunity * 0.35
                + s.adaptation.antiEvasionNeed * 0.45 + s.adaptation.safePunishWindow * 0.25;
    }

    private static double scoreChase(Snapshot s) {
        double ideal = Math.abs(s.distance - s.idealDistance) / 32.0;
        return 0.22 + Mth.clamp(ideal, 0.0, 0.8) + s.targetFleeBias * 0.35 - s.dangerArea * 0.35
                - s.adaptation.trapMastery * 0.45 - s.adaptation.antiEvasionNeed * 0.25;
    }

    private static double scoreBasic(Snapshot s) {
        if (s.infinitySignal > 0.0) return -0.55;
        double yujiPunish = s.yujiBurstDuel && (s.whiffOpportunity > 0.0 || !s.lethalForecast) ? 0.42 : 0.0;
        return 0.18 + (s.distance > 45.0 ? 0.35 : 0.0) + s.dangerArea * 0.2 + (s.trivialTarget ? 1.0 : 0.0) + yujiPunish;
    }

    private static double scoreDismantle(Snapshot s) {
        if (s.infinitySignal > 0.0) return -0.75;
        double range = band(s.distance, 5.0, 48.0);
        double domainBonus = s.selfDomain ? 0.45 : 0.0;
        double killValue = s.targetKillEstimate.finisherValue * 0.32 + s.targetPowerProfile.score * 0.18;
        double yujiPunish = s.yujiBurstDuel && (s.targetCooldown || s.targetUnstable || s.targetWhiffed || s.targetOverextended || s.targetSkillStartup && !s.lethalForecast)
                ? 0.55 : 0.0;
        return 0.42 + range * 0.75 + s.opportunity * 0.42 + s.whiffOpportunity * 0.65 + s.hitConfidence * 0.35
                + s.domainAssessment.punishScore * 0.15 + s.killPressure(0.20) + killValue + domainBonus + yujiPunish
                - s.adaptation.antiEvasionNeed * 0.35 - s.expensiveWastePenalty(0.20);
    }

    private static double scoreCleave(Snapshot s) {
        if (s.infinitySignal > 0.0) return -0.85;
        double domainBonus = s.selfDomain ? 0.45 : 0.0;
        double killValue = s.targetKillEstimate.finisherValue * 0.38 + (s.targetKillEstimate.difficult ? 0.22 : 0.0);
        double yujiWindow = s.yujiBurstDuel && s.distance <= 10.5 && (s.targetCooldown || s.targetUnstable || s.targetWhiffed || s.targetOverextended || !s.lethalForecast)
                ? 0.65 : 0.0;
        return 0.36 + close(s.distance, s.itadoriModulo ? 9.5 : 7.0) * 1.0 + s.meleeThreat * 0.35 + s.opportunity * 0.62 + s.whiffOpportunity * 0.95
                + s.hitConfidence * 0.45 + s.killPressure(0.28) + domainBonus - s.dodgeCounterRisk * 0.35 - s.adaptation.antiEvasionNeed * 0.5
                + killValue + yujiWindow + (s.trivialTarget ? 0.45 : 0.0);
    }

    private static double scoreOpen(Snapshot s) {
        if (s.infinitySignal > 0.0 && !s.selfDomain) return -0.45;
        if (s.trivialTarget) return -1.25;
        if (s.openShotQuality <= 0.0 && !s.selfDomain) return -1.35;
        if (s.itadoriModulo && !s.selfDomain && (!s.clearShot || s.distance < 12.0 || s.immediateThreat > 0.75
                || !(s.targetCooldown || s.targetUnstable || s.targetWhiffed || s.openShotQuality > 0.78))) return -0.75;
        double range = band(s.distance, 8.0, 40.0);
        double domainBonus = s.selfDomain ? 0.55 : 0.0;
        double hardTargetValue = s.targetKillEstimate.resourceValue * 0.55 + s.targetPowerProfile.score * 0.25;
        return 0.34 + range * 0.8 + s.healScore * 0.45 + s.tankScore * 0.35 + s.postHealWindow * 0.4
                + s.domainAssessment.punishScore * 0.2 + s.whiffOpportunity * 0.7 + s.hitConfidence * 0.35 + s.killPressure(0.35) + domainBonus - s.dangerArea * 0.25
                + s.adaptation.rangeKiteNeed * 0.24 + s.adaptation.safePunishWindow * 0.18
                + (s.itadoriModulo && s.clearShot && s.distance >= 12.0 && s.distance <= 28.0 && (s.targetCooldown || s.targetUnstable || s.targetWhiffed) && s.immediateThreat < 0.65 ? 0.35 : -0.55)
                + s.openShotQuality * 0.65
                + hardTargetValue
                - s.expensiveWastePenalty(0.45);
    }

    private static double scoreWorldCut(Snapshot s) {
        if (!s.worldCutCapable) return -2.0;
        if (s.trivialTarget) return -2.0;
        double range = band(s.distance, 4.0, 58.0);
        double gojoBonus = (s.read.primaryTechnique == TechniqueIDs.GOJO || s.read.secondaryTechnique == TechniqueIDs.GOJO) ? 0.65 : 0.0;
        double antiDomainSpecial = s.targetPerfectAntiDomain ? 0.45 : 0.0;
        double castBonus = s.targetCastingDomain ? 0.55 : 0.0;
        double burstPunish = s.itadoriModulo && (s.targetCooldown || s.targetUnstable || s.targetWhiffed || s.targetOverextended || s.distance >= 9.0 && s.immediateThreat < 0.85) ? 1.05 : 0.0;
        double crisis = (s.domainDeathSpiral ? 0.75 : 0.0) + (s.blackFlashChain && s.healthLosingRace ? 0.45 : 0.0);
        double killEstimate = s.targetKillEstimate.resourceValue * 0.7 + (s.targetKillEstimate.difficult ? 0.25 : 0.0);
        return 0.30 + range * 0.65 + s.infinitySignal * 1.9 + gojoBonus + antiDomainSpecial + castBonus + s.tankScore * 0.45 + s.targetDomainCounterBias * 0.25
                + s.domainAssessment.punishScore * 0.35 + s.killPressure(0.55) + s.opportunity * 0.65 + s.whiffOpportunity * 0.8
                + s.hitConfidence * 0.25 + burstPunish + crisis + s.adaptation.antiEvasionNeed * 0.38 + s.adaptation.rangeKiteNeed * 0.16 - close(s.distance, 3.5) * 0.4
                + killEstimate
                - s.expensiveWastePenalty(0.65);
    }

    private static double scoreDomain(Snapshot s) {
        if (s.trivialTarget) return -1.5;
        if (s.targetThreat < 0.45 && !s.targetDomain && !s.targetCastingDomain && s.infinitySignal <= 0.0 && !s.domainDeathSpiral) return -0.8;
        return s.domainAssessment.castScore + s.domainConfidence * 0.55
                + (s.catastrophicDomain ? 0.35 : 0.0)
                + (s.blackFlashChain && s.healthLosingRace ? 0.35 : 0.0);
    }

    private static double scoreSummon(Snapshot s) {
        if (!s.canUseTenShadows || s.trivialTarget || !s.normalReady || s.selfDomain) return -2.0;
        return -0.05 + s.rangeThreat * 0.45 + s.healScore * 0.35 + s.pressure * 0.35
                + (s.targetEscaping ? 0.25 : 0.0) + (s.targetThreat > 0.55 ? 0.25 : 0.0)
                + (s.infinitySignal > 0.0 ? 0.35 : 0.0) + (s.purpleThreat ? 0.15 : 0.0)
                - s.expensiveWastePenalty(0.42);
    }

    private static double scoreAgito(Snapshot s) {
        if (!s.canUseAgito || !s.normalReady || s.trivialTarget || s.selfDomain) return -2.0;
        return 0.05 + s.healScore * 0.75 + s.rangeThreat * 0.45 + s.pressure * 0.45 + s.postHealWindow * 0.35
                + (s.targetThreat > 0.65 ? 0.35 : 0.0) + s.killPressure(0.25) * 0.25
                + (s.infinitySignal > 0.0 ? 0.45 : 0.0) + (s.purpleThreat ? 0.18 : 0.0)
                - s.expensiveWastePenalty(0.58);
    }

    private static double scoreMahoraga(Snapshot s) {
        if (!s.canUseMahoraga || !s.normalReady || s.trivialTarget || s.mahoragaExist) return -2.0;
        double adaptation = (s.infinitySignal > 0.0 ? 1.05 : 0.0) + (s.mahoragaWheel ? 0.35 : 0.0);
        return -0.15 + adaptation + s.domainAssessment.targetDomainThreat * 0.8 + s.damageTakenBurst * 0.85
                + (s.catastrophicDomain ? 0.45 : 0.0)
                + (s.purpleThreat ? 0.22 : 0.0)
                + (s.lethalForecast ? 0.95 : 0.0) + (s.targetThreat > 0.9 ? 0.55 : 0.0)
                + (s.selfHealthRatio < 0.38 ? 0.45 : 0.0) - s.expensiveWastePenalty(0.82);
    }

    private static double scoreTenShadowsDomain(Snapshot s) {
        if (!s.canUseTenShadowsDomain || !s.normalReady || s.trivialTarget || s.selfDomain || s.brainDamaged) return -2.0;
        double betterThanShrine = s.isMeguna && (s.mahoragaExist || s.domainAssessment.counterRisk > 0.45 || s.infinitySignal <= 0.0) ? 0.35 : -0.05;
        return betterThanShrine + s.domainAssessment.castScore + s.healScore * 0.25 + s.tankScore * 0.25
                + (s.targetDomain || s.targetCastingDomain ? 0.55 : 0.0) - s.expensiveWastePenalty(0.65);
    }

    private static double scoreHeianReset(Snapshot s) {
        if (!s.isMeguna || s.isPerfectMode || s.trivialTarget) return -2.0;
        boolean failedPlan = s.memory.megunaHighThreatTicks > 220.0 && (s.targetHealthRatio > 0.55 || s.selfHealthRatio < 0.55);
        return -0.5 + (s.lethalForecast ? 1.65 : 0.0) + (failedPlan ? 0.75 : 0.0)
                + (s.selfHealthRatio < 0.28 ? 0.55 : 0.0) + s.damageTakenBurst * 0.5;
    }

    private static double scorePassive111(Snapshot s) {
        return 0.2 + band(s.distance, 2.0, 16.0) * 0.45 + s.targetSkillDanger * 0.25;
    }

    private static double scorePassive112(Snapshot s) {
        return 0.28 + close(s.distance, 8.0) * 0.55 + s.meleeThreat * 0.35;
    }

    private static double scorePassive113(Snapshot s) {
        return 0.22 + band(s.distance, 10.0, 48.0) * 0.45 + s.rangeThreat * 0.45;
    }

    private static double close(double distance, double max) {
        return Mth.clamp((max - distance) / max, 0.0, 1.0);
    }

    private static double band(double distance, double min, double max) {
        if (distance < min) {
            return Mth.clamp(distance / Math.max(1.0, min), 0.0, 1.0);
        }
        if (distance > max) {
            return Mth.clamp(1.0 - (distance - max) / Math.max(1.0, max), 0.0, 1.0);
        }
        return 1.0;
    }

    private static void useDomain(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, CompoundTag nbt, Snapshot s) {
        nbt.putDouble("skill", 1.0);
        ReturnShadowProcedure.execute(world, x, y, z, sukuna);
        setSkill(sukuna, nbt, DOMAIN, 20.0, false);
        s.memory.lastSukunaDomainTick = s.tick;
    }

    private static void backstep(LevelAccessor world, LivingEntity sukuna, Snapshot s) {
        CompoundTag nbt = sukuna.getPersistentData();
        nbt.putBoolean("PRESS_S", true);
        WhenBackStepProcedure.execute(world, sukuna);
        nbt.putBoolean("PRESS_S", false);
        s.memory.lastBackstepTick = s.tick;
    }

    private static void guard(LevelAccessor world, LivingEntity sukuna, Snapshot s) {
        StartGuardProcedure.execute(world, sukuna);
        s.memory.lastGuardTick = s.tick;
    }

    private static void domainAmplification(LivingEntity sukuna, Snapshot s) {
        if (!sukuna.level().isClientSide()) {
            sukuna.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get(), 30, 254, false, false));
        }
        s.memory.lastDomainAmplificationTick = s.tick;
    }

    private static void maintainKeyedDefenses(Snapshot s) {
        if (s.sukuna.getPersistentData().getBoolean("PRESS_M") && !shouldKeepRct(s)) {
            KeyReverseCursedTechniqueOnKeyReleasedProcedure.execute(s.sukuna);
        }
        if (s.sukuna.getPersistentData().getBoolean("PRESS_BURNOUT") && !shouldKeepBurnoutRct(s)) {
            BurnoutKeyOnKeyReleasedProcedure.execute(s.sukuna);
        } else if (s.sukuna.getPersistentData().getBoolean("PRESS_BURNOUT")) {
            CounterBurnoutProcedure.execute(s.sukuna);
        }
    }

    private static boolean shouldKeepRct(Snapshot s) {
        if (s.selfAntiHeal || s.sukuna.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) {
            return false;
        }
        return s.selfHealth < s.sukuna.getMaxHealth() - 0.5
                && s.nbt.getDouble("cnt_reverse_lim") < (s.sukuna.getMaxHealth() >= 800.0F ? 400.0 : 200.0);
    }

    private static boolean shouldKeepBurnoutRct(Snapshot s) {
        return s.canUseBurnoutRct && (s.sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get())
                || s.sukuna.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get()));
    }

    private static void tryEmergencyReaction(Snapshot s) {
        Action reaction = bestOf(s, List.of(
                Action.move("PURPLE_EVADE", Movement.PURPLE_EVADE, scorePurpleEvade(s) + 0.35),
                Action.guardTiming(scoreGuardTiming(s) + 0.55),
                Action.backstep("EmergencyBackstep", scoreEvasiveBackstep(s) + 0.45),
                Action.rct("EmergencyRCT", scoreRct(s) + 0.25)));
        double threshold = s.nbt.getDouble("skill") != 0.0 ? 1.05 : 1.25;
        if (reaction.score > threshold) {
            reaction.execute(s.world, s.x, s.y, s.z, s.sukuna, s.nbt, s);
            s.memory.lastAction = reaction.name;
            s.memory.lastActionTarget = targetKey(s.target);
        }
    }

    private static void guardTiming(LevelAccessor world, LivingEntity sukuna, Snapshot s) {
        StartGuardProcedure.execute(world, sukuna);
        faceTarget(sukuna, s.target);
        s.memory.lastGuardTimingTick = s.tick;
        s.memory.lastGuardTick = s.tick;
    }

    private static void useRct(LivingEntity sukuna, Snapshot s) {
        KeyReverseCursedTechniqueOnKeyPressedProcedure.execute(sukuna);
        s.memory.lastRecoveryWindowTick = s.tick;
    }

    private static void useSimpleDomain(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, Snapshot s) {
        KeySimpleDomainOnKeyPressedProcedure.execute(world, x, y, z, sukuna);
        s.memory.lastSimpleDomainTick = s.tick;
    }

    private static void useBurnoutRct(LivingEntity sukuna, Snapshot s) {
        BurnoutKeyOnKeyPressedProcedure.execute(sukuna);
        s.memory.lastBurnoutRctTick = s.tick;
    }

    private static void moveTactically(LivingEntity sukuna, Snapshot s, Movement movement) {
        Vec3 self = sukuna.position();
        Vec3 target = s.target.position();
        Vec3 toward = horizontal(target.subtract(self));
        if (toward.lengthSqr() < 1.0E-4) {
            toward = new Vec3(1.0, 0.0, 0.0);
        }
        Vec3 away = toward.scale(-1.0);
        Vec3 lateral = new Vec3(-toward.z, 0.0, toward.x).normalize().scale(s.strafeSide);
        Vec3 destination;
        double speed = 1.75;
        double impulse = 0.36;

        if (movement == Movement.PROJECTILE_DODGE) {
            destination = self.add(s.projectileDodgeVector.scale(8.0)).add(toward.scale(1.5));
            speed = 1.45;
            impulse = 0.46;
            s.memory.lastProjectileDodgeTick = s.tick;
        } else if (movement == Movement.PURPLE_EVADE) {
            Vec3 purple = s.purpleDodgeVector.lengthSqr() > 1.0E-4 ? s.purpleDodgeVector : lateral;
            Vec3 cover = s.clearShot ? away.scale(2.5) : Vec3.ZERO;
            destination = self.add(purple.scale(10.0 + s.purpleRisk * 5.0)).add(cover);
            if (s.sukuna.onGround() && s.purpleRisk > 0.72 && isWalkableDestination(s.world, s.sukuna, self.add(0.0, 1.15, 0.0))) {
                destination = destination.add(0.0, 1.15, 0.0);
                s.sukuna.setDeltaMovement(s.sukuna.getDeltaMovement().add(0.0, 0.28, 0.0));
            }
            speed = 1.75;
            impulse = 0.52;
            s.memory.lastPurpleEvadeTick = s.tick;
        } else if (movement == Movement.DOMAIN_ESCAPE) {
            Vec3 exit = s.domainEscapeVector.lengthSqr() > 1.0E-4 ? s.domainEscapeVector : away;
            destination = self.add(exit.scale(s.catastrophicDomain ? 12.0 : 8.0)).add(lateral.scale(2.5));
            speed = 1.55;
            impulse = 0.42;
        } else if (movement == Movement.MAINTAIN_RANGE) {
            if (s.itadoriModulo && (s.pathBlocked || s.distance > 16.0)) {
                destination = self.add(lateral.scale(4.5)).add(toward.scale(s.distance > 13.0 ? 3.0 : 0.9));
                speed = 1.45;
                impulse = 0.34;
            } else {
            double error = s.distance - s.idealDistance;
            Vec3 group = s.groupEscapeVector.scale(s.groupPressure * 5.0);
            Vec3 adjust = error < 0.0 ? away.scale(Math.min(8.0, -error + 2.0)) : toward.scale(Math.min(8.0, error + 2.0));
            destination = self.add(adjust).add(lateral.scale(3.5)).add(group);
            speed = 1.35;
            }
        } else if (movement == Movement.CUT_OFF_ESCAPE) {
            Vec3 predicted = s.target.position().add(s.target.getDeltaMovement().scale(14.0));
            destination = predicted.add(lateral.scale(-2.0)).add(toward.scale(1.5));
            speed = 1.4;
            impulse = 0.42;
        } else if (movement == Movement.BAIT_WHIFF) {
            double desired = s.itadoriModulo ? 11.5 : 9.5;
            Vec3 adjust = s.distance < desired ? away.scale(desired - s.distance + 2.5) : toward.scale(Math.min(3.0, s.distance - desired));
            destination = self.add(adjust).add(lateral.scale(s.itadoriModulo ? 3.5 : 5.5));
            speed = 1.45;
            impulse = 0.44;
        } else {
            double inward = s.distance > s.idealDistance + 3.0 ? 2.5 : 0.6;
            destination = self.add(lateral.scale(s.itadoriModulo ? 4.0 : 6.0)).add(toward.scale(inward)).add(s.groupEscapeVector.scale(s.groupPressure * 3.0));
            speed = 1.35;
        }

        destination = refineDestination(sukuna, s, destination, toward, lateral, away);
        faceTarget(sukuna, s.target);
        moveTo(sukuna, destination, speed, impulse);
        s.memory.lastMoveTick = s.tick;
        s.memory.rememberDestination(destination);
    }

    private static Vec3 refineDestination(LivingEntity sukuna, Snapshot s, Vec3 destination, Vec3 toward, Vec3 lateral, Vec3 away) {
        if (s.stuckLevel > 0.62 && s.tick - s.memory.lastEscapeTick > 8.0) {
            if (sukuna instanceof Mob mob) {
                mob.getNavigation().stop();
            }
            s.memory.strafeSide = s.memory.strafeSide == 0 ? -s.strafeSide : -s.memory.strafeSide;
            s.memory.lastEscapeTick = s.tick;
            Vec3 escapeLateral = lateral.normalize().scale(s.memory.strafeSide);
            return bestVisibleDestination(s, List.of(
                    sukuna.position().add(escapeLateral.scale(5.0)).add(away.scale(1.5)),
                    sukuna.position().add(escapeLateral.scale(3.5)).add(toward.scale(2.5)),
                    sukuna.position().add(away.scale(4.0)),
                    sukuna.position().add(toward.scale(3.0)).add(escapeLateral.scale(2.0))));
        }
        if (isWalkableDestination(s.world, sukuna, destination) && !pathBlocked(s.world, sukuna, destination)) {
            return destination;
        }
        s.memory.strafeSide = s.memory.strafeSide == 0 ? -s.strafeSide : -s.memory.strafeSide;
        Vec3 altLateral = lateral.normalize().scale(s.memory.strafeSide);
        return bestVisibleDestination(s, List.of(
                sukuna.position().add(altLateral.scale(4.0)).add(toward.scale(s.itadoriModulo ? 1.5 : 0.5)),
                sukuna.position().add(altLateral.scale(-4.0)).add(toward.scale(1.0)),
                sukuna.position().add(away.scale(3.0)).add(altLateral.scale(2.0)),
                sukuna.position().add(toward.scale(3.0)).add(altLateral.scale(2.0))));
    }

    private static Vec3 bestVisibleDestination(Snapshot s, List<Vec3> candidates) {
        Vec3 fallback = candidates.isEmpty() ? s.sukuna.position() : candidates.get(0);
        for (Vec3 candidate : candidates) {
            if (isWalkableDestination(s.world, s.sukuna, candidate)
                    && !pathBlocked(s.world, s.sukuna, candidate)
                    && hasClearMovementSegment(s.world, s.sukuna, candidate, 1.2)) {
                return candidate;
            }
        }
        return fallback;
    }

    private static Vec3 horizontal(Vec3 vector) {
        Vec3 flat = new Vec3(vector.x, 0.0, vector.z);
        return flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
    }

    private static void moveTo(LivingEntity sukuna, Vec3 destination, double speed) {
        moveTo(sukuna, destination, speed, 0.36);
    }

    private static void moveTo(LivingEntity sukuna, Vec3 destination, double speed, double impulseScale) {
        Vec3 self = sukuna.position();
        if (sukuna instanceof Mob mob) {
            mob.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
        }
        Vec3 impulse = destination.subtract(self);
        if (impulse.lengthSqr() > 1.0E-4) {
            double wallPenalty = hasClearMovementSegment(sukuna.level(), sukuna, destination, 1.0) ? 1.0 : 0.35;
            Vec3 flat = horizontal(impulse).scale(Mth.clamp(impulseScale * wallPenalty, 0.0, 0.48));
            sukuna.setDeltaMovement(sukuna.getDeltaMovement().add(flat.x, 0.0, flat.z));
        }
    }

    private static void faceTarget(LivingEntity sukuna, LivingEntity target) {
        Vec3 delta = target.position().subtract(sukuna.position());
        if (delta.lengthSqr() < 1.0E-4) {
            return;
        }
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)) * (180.0 / Math.PI)));
        sukuna.setYRot(yaw);
        sukuna.setXRot(Mth.clamp(pitch, -70.0F, 70.0F));
        sukuna.setYBodyRot(yaw);
        sukuna.setYHeadRot(yaw);
    }

    private static void setSkill(LivingEntity sukuna, CompoundTag nbt, double skill, double tick, boolean combatOnly) {
        nbt.putDouble("cnt_x", 0.0);
        nbt.putDouble("skill", skill);
        if (!sukuna.level().isClientSide()) {
            MobEffect cooldown = combatOnly ? JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get() : JujutsucraftModMobEffects.COOLDOWN_TIME.get();
            int duration = combatOnly ? (int) tick : Math.max(1, (int) tick / 2);
            sukuna.addEffect(new MobEffectInstance(cooldown, duration, 0, false, false));
            sukuna.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
        }
    }

    private static void setWorldCut(LivingEntity sukuna, CompoundTag nbt) {
        if (!hasWorldCut(sukuna)) {
            setSkill(sukuna, nbt, FAST_DISMANTLE, 50.0, false);
            return;
        }
        nbt.putDouble("cnt_x", 0.0);
        nbt.putDouble("cnt6", Math.max(nbt.getDouble("cnt6"), 5.0));
        nbt.putDouble("cnt7", 2.0);
        nbt.putBoolean("flag_dismantle", true);
        setSkill(sukuna, nbt, FAST_DISMANTLE, 100.0, false);
    }

    private static void equipMahoragaWheel(LivingEntity sukuna, Snapshot s) {
        if (!s.canUseMahoraga || s.mahoragaWheel || s.trivialTarget) {
            return;
        }
        if (s.infinitySignal > 0.0 || s.targetThreat > 0.72 || s.lethalForecast || s.damageTakenBurst > 0.45) {
            sukuna.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()));
        }
    }

    private static String targetKey(LivingEntity target) {
        return target.getStringUUID();
    }

    private static String typeKey(LivingEntity target) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        return id == null ? target.getClass().getName() : id.toString();
    }

    private enum Movement {
        NONE,
        PROJECTILE_DODGE,
        PURPLE_EVADE,
        DOMAIN_ESCAPE,
        MAINTAIN_RANGE,
        CUT_OFF_ESCAPE,
        STRAFE_PRESSURE,
        BAIT_WHIFF
    }

    private enum DomainIntent {
        NONE,
        COUNTER_DOMAIN,
        CHECKMATE,
        ANTI_INFINITY,
        CROWD_RESET,
        SURVIVAL_STALL
    }

    private enum ActionKind {
        MELEE,
        NORMAL_SLASH,
        WORLD_CUT,
        DOMAIN,
        TEN_SHADOWS,
        TRANSFORM,
        DOMAIN_AMPLIFICATION,
        MICRO_DEFENSE,
        RANGE_CONTROL
    }

    private static class Action {
        final String name;
        final ActionKind kind;
        final double skill;
        final double cooldownTicks;
        final boolean combatOnly;
        final boolean backstep;
        final boolean domain;
        final boolean hwb;
        final boolean guard;
        final boolean guardTiming;
        final boolean recoveryWindow;
        final boolean domainAmplification;
        final boolean worldCut;
        final boolean tenShadows;
        final boolean heianReset;
        final Movement movement;
        final double score;

        private Action(String name, ActionKind kind, double skill, double cooldownTicks, boolean combatOnly, boolean backstep, boolean domain, boolean hwb,
                       boolean guard, boolean guardTiming, boolean recoveryWindow, boolean domainAmplification, boolean worldCut,
                       boolean tenShadows, boolean heianReset, Movement movement, double score) {
            this.name = name;
            this.kind = kind;
            this.skill = skill;
            this.cooldownTicks = cooldownTicks;
            this.combatOnly = combatOnly;
            this.backstep = backstep;
            this.domain = domain;
            this.hwb = hwb;
            this.guard = guard;
            this.guardTiming = guardTiming;
            this.recoveryWindow = recoveryWindow;
            this.domainAmplification = domainAmplification;
            this.worldCut = worldCut;
            this.tenShadows = tenShadows;
            this.heianReset = heianReset;
            this.movement = movement;
            this.score = score;
        }

        static Action skill(String name, double skill, double cooldownTicks, boolean combatOnly, double score) {
            ActionKind kind = skill == CLEAVE ? ActionKind.MELEE : ActionKind.NORMAL_SLASH;
            return create(name, kind, skill, cooldownTicks, combatOnly, score);
        }

        static Action worldCut(double score) {
            return worldCut("WorldCut", score);
        }

        static Action worldCut(String name, double score) {
            return create(name, ActionKind.WORLD_CUT, FAST_DISMANTLE, 100.0, false, false, false, false, false, false, false, false, true, false, false, Movement.NONE, score);
        }

        static Action backstep(double score) {
            return backstep("Backstep", score);
        }

        static Action backstep(String name, double score) {
            return create(name, ActionKind.MICRO_DEFENSE, 0.0, 0.0, false, true, false, false, false, false, false, false, false, false, false, Movement.NONE, score);
        }

        static Action guard(double score) {
            return guard("Guard", score);
        }

        static Action guard(String name, double score) {
            return create(name, ActionKind.MICRO_DEFENSE, 0.0, 0.0, false, false, false, false, true, false, false, false, false, false, false, Movement.NONE, score);
        }

        static Action guardTiming(double score) {
            return create("GUARD_TIMING", ActionKind.MICRO_DEFENSE, 0.0, 0.0, false, false, false, false, false, true, false, false, false, false, false, Movement.NONE, score);
        }

        static Action rct(String name, double score) {
            return create(name, ActionKind.MICRO_DEFENSE, 0.0, 0.0, false, false, false, false, false, false, true, false, false, false, false, Movement.NONE, score);
        }

        static Action burnoutRct(double score) {
            return create("BURNOUT_RCT", ActionKind.MICRO_DEFENSE, 0.0, 0.0, false, false, false, false, false, false, false, true, false, false, false, Movement.NONE, score);
        }

        static Action domainAmplification(double score) {
            return create("DomainAmplification", ActionKind.DOMAIN_AMPLIFICATION, 0.0, 0.0, false, false, false, false, false, false, false, true, false, false, false, Movement.NONE, score);
        }

        static Action domain(double score) {
            return create("Domain", ActionKind.DOMAIN, DOMAIN, 20.0, false, false, true, false, false, false, false, false, false, false, false, Movement.NONE, score);
        }

        static Action simpleDomain(double score) {
            return create("SIMPLE_DOMAIN", ActionKind.MICRO_DEFENSE, 0.0, 0.0, false, false, false, true, false, false, false, false, false, false, false, Movement.NONE, score);
        }

        static Action move(String name, Movement movement, double score) {
            return create(name, ActionKind.RANGE_CONTROL, 0.0, 0.0, false, false, false, false, false, false, false, false, false, false, false, movement, score);
        }

        static Action calculate(String name, double score) {
            return create(name, ActionKind.MELEE, 0.0, 0.0, false, false, false, false, false, false, false, false, false, false, false, Movement.NONE, score);
        }

        static Action summon(double score) {
            return create("SUMMON", ActionKind.TEN_SHADOWS, TEN_SHADOWS_UTILITY, 90.0, false, false, false, false, false, false, false, false, false, true, false, Movement.NONE, score);
        }

        static Action agito(double score) {
            return create("AGITO", ActionKind.TEN_SHADOWS, AGITO, 140.0, false, false, false, false, false, false, false, false, false, true, false, Movement.NONE, score);
        }

        static Action mahoraga(double score) {
            return create("MAHORAGA", ActionKind.TEN_SHADOWS, MAHORAGA, 180.0, false, false, false, false, false, false, false, false, false, true, false, Movement.NONE, score);
        }

        static Action tenShadowsDomain(double score) {
            return create("TEN_SHADOWS_DOMAIN", ActionKind.TEN_SHADOWS, TEN_SHADOWS_DOMAIN, 20.0, false, false, false, false, false, false, false, false, false, true, false, Movement.NONE, score);
        }

        static Action heianReset(double score) {
            return create("SURVIVAL_RESET_HEIAN", ActionKind.TRANSFORM, 0.0, 0.0, false, false, false, false, false, false, false, false, false, false, true, Movement.NONE, score);
        }

        private static Action create(String name, ActionKind kind, double skill, double cooldownTicks, boolean combatOnly, double score) {
            return create(name, kind, skill, cooldownTicks, combatOnly, false, false, false, false, false, false, false, false, false, false, Movement.NONE, score);
        }

        private static Action create(String name, ActionKind kind, double skill, double cooldownTicks, boolean combatOnly, boolean backstep, boolean domain, boolean hwb,
                                     boolean guard, boolean guardTiming, boolean recoveryWindow, boolean domainAmplification, boolean worldCut,
                                     boolean tenShadows, boolean heianReset, Movement movement, double score) {
            return new Action(name, kind, skill, cooldownTicks, combatOnly, backstep, domain, hwb, guard, guardTiming, recoveryWindow,
                    domainAmplification, worldCut, tenShadows, heianReset, movement, score);
        }

        void execute(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, CompoundTag nbt, Snapshot s) {
            if (hwb) {
                OpSukunaBrain.useSimpleDomain(world, x, y, z, sukuna, s);
            } else if (guard) {
                OpSukunaBrain.guard(world, sukuna, s);
            } else if (guardTiming) {
                OpSukunaBrain.guardTiming(world, sukuna, s);
            } else if (recoveryWindow) {
                OpSukunaBrain.useRct(sukuna, s);
            } else if (domainAmplification) {
                if ("BURNOUT_RCT".equals(name)) {
                    OpSukunaBrain.useBurnoutRct(sukuna, s);
                } else {
                    OpSukunaBrain.domainAmplification(sukuna, s);
                }
            } else if (backstep) {
                OpSukunaBrain.backstep(world, sukuna, s);
                if (name.contains("Backstep")) {
                    s.memory.lastEvasiveBackstepTick = s.tick;
                }
            } else if (domain) {
                OpSukunaBrain.useDomain(world, x, y, z, sukuna, nbt, s);
            } else if (worldCut) {
                OpSukunaBrain.setWorldCut(sukuna, nbt);
            } else if (heianReset) {
                JJKURSukunaAIBuff.forceHeianTransformation(world, sukuna, false);
            } else if (tenShadows) {
                OpSukunaBrain.equipMahoragaWheel(sukuna, s);
                OpSukunaBrain.setSkill(sukuna, nbt, skill, cooldownTicks, combatOnly);
            } else if (skill != 0.0) {
                OpSukunaBrain.setSkill(sukuna, nbt, skill, cooldownTicks, combatOnly);
            } else if (movement != Movement.NONE) {
                OpSukunaBrain.moveTactically(sukuna, s, movement);
            } else {
                CalculateAttackProcedure.execute(world, sukuna);
            }
        }
    }

    private static class Snapshot {
        final LevelAccessor world;
        final double x;
        final double y;
        final double z;
        final LivingEntity sukuna;
        final LivingEntity target;
        final CompoundTag nbt;
        final BrainMemory memory;
        final CombatStats targetStats;
        final CombatStats typeStats;
        final CombatStats techniqueStats;
        final CombatStats archetypeStats;
        final PlayerRead read;
        final double tick;
        final double distance;
        final double predictedDistance;
        final double selfHealth;
        final double targetHealth;
        final double selfHealthRatio;
        final double targetHealthRatio;
        final double damageTaken;
        final double damageDealt;
        final double targetHeal;
        final double recentDamageRatio;
        final double damageTakenBurst;
        final double targetSkill;
        final double targetSkillDanger;
        final double meleeThreat;
        final double rangeThreat;
        final double tankScore;
        final double healScore;
        final double powerScore;
        final double targetThreat;
        final double pressure;
        final double opportunity;
        final double domainCounterRisk;
        final double targetDomainCounterBias;
        final double dodgeCounterRisk;
        final double targetFleeBias;
        final double dangerArea;
        final double incomingProjectileRisk;
        final double purpleRisk;
        final double postHealWindow;
        final double idealDistance;
        final double infinitySignal;
        final double immediateThreat;
        final double whiffOpportunity;
        final double hitConfidence;
        final double groupPressure;
        final double groupActivePressure;
        final double groupApexPressure;
        final Vec3 projectileDodgeVector;
        final Vec3 purpleDodgeVector;
        final Vec3 groupEscapeVector;
        final Vec3 domainEscapeVector;
        final DomainAssessment domainAssessment;
        final AdaptationView adaptation;
        final CombatRelation targetCombatRelation;
        final PowerProfile targetPowerProfile;
        final KillEstimate targetKillEstimate;
        final double rctLimit;
        final double rctStrain;
        final double rctLevel;
        final double brainDamageLevel;
        final double incomingKillRisk;
        final double survivalUrgency;
        final double blackFlashRecoveryValue;
        final double openShotQuality;
        final double stuckLevel;
        final double domainConfidence;
        final boolean selfDomain;
        final boolean targetDomain;
        final boolean targetCooldown;
        final boolean targetUnstable;
        final boolean combatCooldown;
        final boolean passiveReady;
        final boolean normalReady;
        final boolean domainReady;
        final boolean infinity;
        final boolean targetCastingDomain;
        final boolean purpleThreat;
        final boolean purpleWindup;
        final boolean purpleProjectile;
        final boolean purpleLineOfFire;
        final boolean worldCutCapable;
        final boolean brainDamaged;
        final boolean targetHwb;
        final boolean targetSimpleDomain;
        final boolean targetDomainBreak;
        final boolean targetDodge;
        final boolean targetCounter;
        final boolean targetGuard;
        final boolean targetRegen;
        final boolean targetAntiHeal;
        final boolean targetAttacking;
        final boolean targetNeutralization;
        final boolean targetDomainAmplification;
        final boolean targetPerfectAntiDomain;
        final boolean targetEscaping;
        final boolean targetSkillStartup;
        final boolean targetWhiffed;
        final boolean targetOverextended;
        final boolean selfRct;
        final boolean selfSimpleDomain;
        final boolean selfAntiHeal;
        final boolean itadoriModulo;
        final boolean trivialTarget;
        final boolean isFushiguro;
        final boolean isPerfectMode;
        final boolean isMeguna;
        final boolean canUseTenShadows;
        final boolean canUseAgito;
        final boolean canUseMahoraga;
        final boolean canUseTenShadowsDomain;
        final boolean canUseRct;
        final boolean canUseSimpleDomain;
        final boolean canUseBurnoutRct;
        final boolean rctFatigued;
        final boolean survivalMode;
        final boolean mahoragaWheel;
        final boolean mahoragaExist;
        final boolean lethalForecast;
        final boolean killConfirm;
        final boolean clearShot;
        final boolean pathBlocked;
        final boolean yujiBurstDuel;
        final boolean enemyDomainTrap;
        final boolean catastrophicDomain;
        final boolean domainEscapeWindow;
        final boolean domainDeathSpiral;
        final boolean simpleDomainExpiresSoon;
        final boolean blackFlashChain;
        final boolean healthLosingRace;
        final int strafeSide;
        final String archetype;
        final DomainIntent domainIntent;

        static Snapshot capture(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, LivingEntity target, CompoundTag nbt,
                                BrainMemory memory) {
            CombatStats targetStats = memory.target(targetKey(target));
            CombatStats typeStats = memory.type(typeKey(target));
            PlayerRead read = PlayerRead.read(target);
            String preliminary = classify(target, read, targetStats, typeStats, null);
            CombatStats archetypeStats = memory.archetype(preliminary);
            String archetype = classify(target, read, targetStats, typeStats, archetypeStats);
            archetypeStats = memory.archetype(archetype);
            String techniqueKey = read.techniqueKey();
            CombatStats techniqueStats = memory.technique(techniqueKey);
            return new Snapshot(world, x, y, z, sukuna, target, nbt, memory, targetStats, typeStats, techniqueStats, archetypeStats, read, archetype);
        }

        private Snapshot(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, LivingEntity target, CompoundTag nbt,
                         BrainMemory memory, CombatStats targetStats, CombatStats typeStats, CombatStats techniqueStats,
                         CombatStats archetypeStats, PlayerRead read, String archetype) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.sukuna = sukuna;
            this.target = target;
            this.nbt = nbt;
            this.memory = memory;
            this.targetStats = targetStats;
            this.typeStats = typeStats;
            this.techniqueStats = techniqueStats;
            this.archetypeStats = archetypeStats;
            this.read = read;
            this.archetype = archetype;
            this.tick = sukuna.tickCount;
            this.distance = GetDistanceProcedure.execute(sukuna);
            this.predictedDistance = predictedDistance(sukuna, target);
            this.stuckLevel = memory.updateStuck(sukuna, sukuna.tickCount);
            this.selfHealth = sukuna.getHealth();
            this.targetHealth = target.getHealth();
            this.selfHealthRatio = selfHealth / Math.max(1.0, sukuna.getMaxHealth());
            this.targetHealthRatio = targetHealth / Math.max(1.0, target.getMaxHealth());
            this.damageTaken = Math.max(0.0, memory.lastHealth - selfHealth);
            this.damageDealt = Math.max(0.0, targetStats.lastHealth - targetHealth);
            this.targetHeal = Math.max(0.0, targetHealth - targetStats.lastHealth);
            this.recentDamageRatio = damageTaken / Math.max(1.0, sukuna.getMaxHealth());
            this.damageTakenBurst = Mth.clamp((damageTaken + targetStats.damageTakenAvg) / Math.max(1.0, sukuna.getMaxHealth() * 0.12), 0.0, 1.0);
            this.targetSkill = target.getPersistentData().getDouble("skill");
            this.selfDomain = sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
            this.targetDomain = target.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
            this.targetCooldown = target.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get()) || target.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get());
            this.targetUnstable = target.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get());
            this.combatCooldown = LogicCooldownCombatProcedure.execute(sukuna);
            this.passiveReady = LogicStartPassiveProcedure.execute(sukuna);
            this.normalReady = LogicStartProcedure.execute(sukuna);
            this.domainReady = LogicConfilmDomainProcedure.execute(world, x, y, z, sukuna);
            this.infinity = target.hasEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.INFINITY.get());
            this.infinitySignal = (infinity ? 1.0 : 0.0) + (read.primaryTechnique == TechniqueIDs.GOJO || read.secondaryTechnique == TechniqueIDs.GOJO ? 0.35 : 0.0);
            double targetDomainNumber = domainNumber(target);
            this.targetCastingDomain = isDomainCastSignal(target, targetSkill, targetDomainNumber, read);
            this.worldCutCapable = hasWorldCut(sukuna);
            this.brainDamaged = sukuna.hasEffect(JujutsucraftModMobEffects.BRAIN_DAMAGE.get());
            this.isFushiguro = isFushiguroBody(sukuna);
            this.isPerfectMode = isPerfectMode(sukuna);
            this.isMeguna = isFushiguro && !isPerfectMode;
            this.mahoragaWheel = sukuna.getItemBySlot(EquipmentSlot.HEAD).getItem() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get();
            this.mahoragaExist = nbt.getDouble("TenShadowsTechnique14") == -1.0;
            this.canUseTenShadows = isMeguna && nbt.getBoolean("flag_start");
            this.canUseAgito = canUseTenShadows && nbt.getDouble("TenShadowsTechnique13") >= 0.0;
            this.canUseMahoraga = canUseTenShadows && nbt.getDouble("TenShadowsTechnique14") >= 0.0;
            this.canUseTenShadowsDomain = canUseTenShadows && (nbt.getDouble("TenShadowsTechnique14") >= 0.0 || mahoragaExist);
            this.itadoriModulo = isItadoriModulo(target);
            this.targetHwb = target.hasEffect(JujutsucraftaddonModMobEffects.HWB.get());
            this.targetSimpleDomain = read.simpleDomain || target.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.SIMPLE_DOMAIN_MAX.get());
            this.selfRct = sukuna.hasEffect(JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get());
            this.selfSimpleDomain = sukuna.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) || sukuna.hasEffect(JujutsucraftaddonModMobEffects.SIMPLE_DOMAIN_MAX.get()) || sukuna.hasEffect(JujutsucraftaddonModMobEffects.HWB.get());
            this.simpleDomainExpiresSoon = selfSimpleDomain
                    && Math.max(effectDuration(sukuna, JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()),
                    Math.max(effectDuration(sukuna, JujutsucraftaddonModMobEffects.SIMPLE_DOMAIN_MAX.get()),
                            effectDuration(sukuna, JujutsucraftaddonModMobEffects.HWB.get()))) <= 45;
            this.canUseRct = OpSukunaBrain.canUseRct(sukuna);
            this.canUseSimpleDomain = OpSukunaBrain.canUseSimpleDomain(sukuna);
            this.canUseBurnoutRct = OpSukunaBrain.canUseBurnoutRct(sukuna);
            this.targetDodge = target.hasEffect(JujutsucraftaddonModMobEffects.DODGE.get());
            this.targetCounter = target.hasEffect(JujutsucraftaddonModMobEffects.COUNTER.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.COUNTER_CD.get());
            this.targetGuard = target.hasEffect(JujutsucraftModMobEffects.GUARD.get()) || target.getPersistentData().getBoolean("guard");
            this.targetRegen = target.hasEffect(MobEffects.REGENERATION) || read.rct > 0.0;
            this.targetAntiHeal = target.hasEffect(JujutsucraftaddonModMobEffects.ANTI_HEAL.get());
            this.selfAntiHeal = sukuna.hasEffect(JujutsucraftaddonModMobEffects.ANTI_HEAL.get()) || sukuna.hasEffect(JujutsucraftaddonModMobEffects.RCT_CUT.get());
            this.targetAttacking = target.getPersistentData().getBoolean("attack") || target.getPersistentData().getDouble("Damage") != 0.0 || targetSkill != 0.0;
            this.targetNeutralization = target.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get());
            this.targetDomainAmplification = target.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get());
            this.targetPerfectAntiDomain = isSatushi(target) || hasEntityTypeTag(target, "jujutsucraft:can_use_simple_domain")
                    && hasEntityTypeTag(target, "jujutsucraft:can_use_hollow_wicker_basket")
                    && hasEntityTypeTag(target, "jujutsucraft:can_use_falling_blossom_emotion");
            int selfNeutralizationAmp = effectAmplifier(sukuna, JujutsucraftModMobEffects.NEUTRALIZATION.get());
            boolean gojoDomainSignal = targetDomainNumber == 2.0
                    || targetSkill == 220.0
                    || ((int) Math.round(Math.abs(targetSkill))) % 100 == 20
                    || read.primaryTechnique == TechniqueIDs.GOJO
                    || read.secondaryTechnique == TechniqueIDs.GOJO
                    || isSatushi(target);
            this.enemyDomainTrap = targetDomain && !selfDomain && distance < 42.0;
            this.catastrophicDomain = (targetDomain || targetCastingDomain || enemyDomainTrap)
                    && (gojoDomainSignal
                    || targetSkill == -999.0
                    || nbt.getDouble("skill") == -999.0
                    || selfNeutralizationAmp == 12 || selfNeutralizationAmp == 2 || selfNeutralizationAmp >= 37
                    || sukuna.hasEffect(JujutsucraftModMobEffects.BRAIN_DAMAGE.get())
                    || hasExtremeVoidDebuff(sukuna));
            Vec3 awayFromDomain = horizontal(sukuna.position().subtract(target.position()));
            this.domainEscapeVector = awayFromDomain.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : awayFromDomain;
            DangerScan danger = scanDanger(world, sukuna);
            PurpleThreat purple = scanPurpleThreat(world, sukuna, target, targetSkill, read);
            ThreatScan threatScan = ThreatScan.scan(world, sukuna, target, memory);
            ThreatCandidate targetCandidate = threatScan.candidateOf(target);
            this.targetCombatRelation = targetCandidate == null ? readCombatRelation(world, sukuna, target, read, memory) : targetCandidate.relation;
            this.yujiBurstDuel = itadoriModulo && distance < 20.0 && targetCombatRelation != CombatRelation.PASSIVE_VALID;
            this.targetPowerProfile = targetCandidate == null ? readPowerProfile(sukuna, target, read, targetStats, typeStats) : targetCandidate.powerProfile;
            this.targetKillEstimate = targetCandidate == null ? estimateKill(sukuna, target, targetPowerProfile, targetStats, read) : targetCandidate.killEstimate;
            this.dangerArea = danger.areaRisk;
            this.incomingProjectileRisk = danger.incomingProjectileRisk;
            this.projectileDodgeVector = danger.dodgeVector;
            this.purpleRisk = purple.risk;
            this.purpleThreat = purple.threat;
            this.purpleWindup = purple.windup;
            this.purpleProjectile = purple.projectile;
            this.purpleLineOfFire = purple.lineOfFire;
            this.purpleDodgeVector = purple.dodgeVector;
            this.groupPressure = threatScan.groupPressure;
            this.groupActivePressure = threatScan.groupActivePressure;
            this.groupApexPressure = threatScan.groupApexPressure;
            this.groupEscapeVector = threatScan.escapeVector;
            this.postHealWindow = targetHeal > target.getMaxHealth() * 0.03 || target.hasEffect(MobEffects.REGENERATION) ? 1.0 : 0.0;
            this.targetSkillDanger = scoreTargetSkill(targetSkill, targetStats);
            this.targetSkillStartup = targetSkill != 0.0 && (memory.lastTargetSkill == 0.0 || memory.lastTargetSkill != targetSkill || tick - memory.lastTargetSkillTick <= 8.0);
            this.targetWhiffed = targetSkill == 0.0 && memory.lastTargetSkill != 0.0 && tick - memory.lastTargetSkillTick <= 24.0 && damageTaken <= sukuna.getMaxHealth() * 0.015;
            this.targetOverextended = predictedDistance < distance - 0.85 || target.getDeltaMovement().dot(sukuna.position().subtract(target.position())) > 0.035;
            this.healScore = Mth.clamp(targetStats.targetHealAvg / Math.max(1.0, target.getMaxHealth() * 0.06) + read.healBias * 0.4 + postHealWindow * 0.25, 0.0, 1.0);
            double damageEfficiency = Mth.clamp(targetStats.damageDealtAvg / Math.max(1.0, target.getMaxHealth() * 0.06), 0.0, 1.0);
            this.tankScore = Mth.clamp((target.getMaxHealth() / Math.max(1.0, sukuna.getMaxHealth()) - 0.65) + (1.0 - damageEfficiency) * 0.55 + read.tankBias * 0.35, 0.0, 1.0);
            this.meleeThreat = Mth.clamp(targetStats.meleeRatio() * 0.55 + typeStats.meleeRatio() * 0.2 + archetypeStats.meleeRatio() * 0.2
                    + targetStats.damageTakenAvg / Math.max(1.0, sukuna.getMaxHealth() * 0.08) + read.aggression * 0.25, 0.0, 1.0);
            this.rangeThreat = Mth.clamp(targetStats.rangedRatio() * 0.45 + typeStats.rangedRatio() * 0.25 + archetypeStats.rangedRatio() * 0.25 + read.rangedBias * 0.45, 0.0, 1.0);
            this.domainCounterRisk = Mth.clamp(read.domainBias * 0.35 + read.hwbBias * 0.2 + targetStats.counterDomainAfterSukuna / 4.0 + typeStats.counterDomainAfterSukuna / 7.0
                    + archetypeStats.counterDomainAfterSukuna / 7.0, 0.0, 1.0);
            this.targetDomainCounterBias = Mth.clamp(targetStats.domainTicks / Math.max(1.0, targetStats.seenTicks) + read.domainBias, 0.0, 1.0);
            this.dodgeCounterRisk = Mth.clamp((targetDodge ? 0.55 : 0.0) + (targetCounter ? 0.55 : 0.0), 0.0, 1.0);
            this.targetFleeBias = Mth.clamp(targetStats.fleeTicks / Math.max(1.0, targetStats.seenTicks) + read.fleeBias * 0.45, 0.0, 1.0);
            this.adaptation = AdaptationView.create(this);
            this.powerScore = estimatePower(this);
            this.targetThreat = estimateThreat(this);
            this.trivialTarget = isTrivialTarget(this);
            this.pressure = Mth.clamp(powerScore * 0.45 + targetSkillDanger * 0.25 + (1.0 - selfHealthRatio) * 0.3 + dangerArea * 0.35, 0.0, 1.0);
            this.whiffOpportunity = Mth.clamp((targetWhiffed ? 0.85 : 0.0) + (targetOverextended ? 0.45 : 0.0)
                    + (targetCooldown ? 0.35 : 0.0) + (targetUnstable ? 0.35 : 0.0), 0.0, 1.0);
            this.opportunity = Mth.clamp((targetCooldown ? 0.35 : 0.0) + (targetUnstable ? 0.35 : 0.0) + (1.0 - targetHealthRatio) * 0.3
                    + postHealWindow * 0.25 + whiffOpportunity * 0.45 + (target.hasEffect(JujutsucraftaddonModMobEffects.FATIGUE.get()) ? 0.2 : 0.0), 0.0, 1.0);
            this.idealDistance = adaptation.preferredRange > 1.0
                    ? Mth.clamp(adaptation.preferredRange, 8.0, 30.0)
                    : idealDistance(archetype, infinitySignal, tankScore, healScore);
            this.targetDomainBreak = target.hasEffect(JujutsucraftaddonModMobEffects.DOMAIN_BREAK.get());
            this.targetEscaping = target.getDeltaMovement().dot(sukuna.position().subtract(target.position())) < -0.03 || predictedDistance > distance + 1.5;
            this.immediateThreat = Mth.clamp(incomingProjectileRisk * 0.9 + dangerArea * 0.45 + targetSkillDanger * (targetSkillStartup ? 1.15 : 0.55)
                    + meleeThreat * close(predictedDistance, itadoriModulo ? 13.0 : 8.0) + damageTakenBurst * 0.5, 0.0, 1.6);
            this.hitConfidence = Mth.clamp(band(predictedDistance, 3.0, worldCutCapable ? 54.0 : 38.0) + whiffOpportunity * 0.35
                    + (targetGuard || targetDodge || targetCounter ? -0.25 : 0.0) + (targetEscaping ? -0.15 : 0.0), 0.0, 1.0);
            this.rctLimit = rctLimit(sukuna);
            this.rctStrain = Mth.clamp(nbt.getDouble("cnt_reverse_lim") / Math.max(1.0, rctLimit), 0.0, 1.2);
            this.rctLevel = rctLevel(sukuna);
            this.brainDamageLevel = brainDamageLevel(sukuna);
            this.incomingKillRisk = Mth.clamp(immediateThreat * 0.45 + targetSkillDanger * 0.5 + damageTakenBurst * 0.75
                    + groupActivePressure * 0.35 + groupApexPressure * 0.45 + (itadoriModulo ? 0.35 : 0.0), 0.0, 1.6);
            this.rctFatigued = rctStrain > 0.68 || brainDamageLevel >= 4.0 || selfAntiHeal
                    || (rctLevel <= 1.0 && selfHealthRatio < 0.55 && damageTakenBurst > 0.2);
            double projectedRct = (selfRct || canUseRct && !selfAntiHeal) ? sukuna.getMaxHealth() * (rctFatigued ? 0.012 : 0.022) : 0.0;
            this.healthLosingRace = memory.recentDamageWindow > Math.max(projectedRct + memory.recentHealWindow, sukuna.getMaxHealth() * 0.055)
                    || damageTaken > Math.max(projectedRct * 1.5, sukuna.getMaxHealth() * 0.08)
                    || memory.burstChainTicks >= 4.0 && memory.recentDamageWindow > memory.recentHealWindow + sukuna.getMaxHealth() * 0.035;
            this.blackFlashChain = memory.blackFlashChainTicks >= 2.0
                    || (damageTakenBurst > 0.48 && distance < 9.0 && ("BLACK_FLASH_USER".equals(archetype) || itadoriModulo
                    || target.hasEffect(JujutsucraftaddonModMobEffects.BLACK_FLASH_CUT.get())
                    || target.hasEffect(JujutsucraftaddonModMobEffects.KOKUSEN_EFFECT.get())));
            this.survivalUrgency = Mth.clamp((1.0 - selfHealthRatio) * 0.55 + incomingKillRisk * 0.55 + rctStrain * 0.35
                    + (rctFatigued ? 0.35 : 0.0) + (targetPowerProfile.high ? 0.12 : 0.0)
                    + (healthLosingRace ? 0.22 : 0.0) + (blackFlashChain ? 0.18 : 0.0), 0.0, 1.6);
            this.blackFlashRecoveryValue = Mth.clamp(rctStrain * 0.65 + (brainDamageLevel >= 3.0 ? 0.25 : 0.0)
                    + close(predictedDistance, 6.5) * 0.35 + whiffOpportunity * 0.35 + (targetCooldown || targetUnstable ? 0.25 : 0.0), 0.0, 1.35);
            this.clearShot = hasClearShot(world, sukuna, target, Math.max(1.5, target.getBbWidth() * 0.85));
            this.pathBlocked = pathBlocked(world, sukuna, target.position());
            this.domainEscapeWindow = targetCastingDomain && !targetDomain && !read.barrierlessDomain && !selfDomain && hasDomainEscapeCandidate(this)
                    && distance < (catastrophicDomain ? 38.0 : 30.0);
            this.openShotQuality = clearShot
                    ? Mth.clamp(1.0 - close(distance, 5.0) * 0.25 + (targetOverextended ? 0.12 : 0.0), 0.0, 1.0)
                    : 0.0;
            this.survivalMode = rctFatigued && (targetThreat > 0.45 || incomingKillRisk > 0.55 || groupActivePressure > 0.35);
            this.domainDeathSpiral = catastrophicDomain && enemyDomainTrap
                    && (simpleDomainExpiresSoon || healthLosingRace || selfHealthRatio < 0.42 || brainDamageLevel >= 2.0 || memory.domainTrapTicks > 35.0);
            this.lethalForecast = selfHealthRatio < 0.22
                    || (selfHealthRatio < 0.42 && (damageTakenBurst > 0.55 || targetSkillDanger > 0.75 || targetDomain))
                    || damageTaken >= selfHealth * 0.55
                    || (rctFatigued && selfHealthRatio < 0.48 && incomingKillRisk > 0.78)
                    || (selfHealthRatio < 0.62 && incomingKillRisk > 1.18)
                    || (domainDeathSpiral)
                    || (blackFlashChain && healthLosingRace && selfHealthRatio < 0.62);
            this.killConfirm = targetHealthRatio < 0.34
                    || (targetHealth <= 32.0 && (targetCooldown || targetUnstable || postHealWindow > 0.0))
                    || (targetAntiHeal && targetHealthRatio < 0.52);
            int baseStrafe = ((sukuna.getUUID().getLeastSignificantBits() ^ target.getUUID().getMostSignificantBits()) & 1L) == 0L ? 1 : -1;
            this.strafeSide = memory.strafeSide == 0 ? baseStrafe : memory.strafeSide;
            this.domainAssessment = DomainTactics.assess(this);
            this.domainIntent = domainAssessment.intent;
            this.domainConfidence = domainAssessment.confidence;
        }

        double killPressure(double expectedDamageRatio) {
            return targetHealthRatio <= expectedDamageRatio ? 1.0 : Mth.clamp((expectedDamageRatio * 1.5 - targetHealthRatio) / Math.max(0.01, expectedDamageRatio * 1.5), 0.0, 1.0);
        }

        double expensiveWastePenalty(double minimumThreat) {
            if (targetThreat >= minimumThreat || targetDomain || targetCastingDomain || infinitySignal > 0.0) return 0.0;
            return Mth.clamp((minimumThreat - targetThreat) * 2.2, 0.0, 1.3);
        }
    }

    private static String classify(LivingEntity target, PlayerRead read, CombatStats targetStats, CombatStats typeStats, CombatStats archetypeStats) {
        if (isItadoriModulo(target)) return "MELEE_BURST_TANK";
        if (target.hasEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.INFINITY.get())
                || read.primaryTechnique == TechniqueIDs.GOJO || read.secondaryTechnique == TechniqueIDs.GOJO) return "INFINITY_USER";
        if (target.hasEffect(JujutsucraftaddonModMobEffects.BLACK_FLASH_CUT.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.FATIGUE_BLACK_FLASH.get())
                || read.primaryTechnique == TechniqueIDs.ITADORI || read.secondaryTechnique == TechniqueIDs.ITADORI) return "BLACK_FLASH_USER";
        if (read.domainBias > 0.55 || targetStats.counterDomainAfterSukuna > 2.0 || typeStats.counterDomainAfterSukuna > 4.0) return "DOMAIN_COUNTER";
        if (read.healBias > 0.5 || targetStats.targetHealAvg > target.getMaxHealth() * 0.025) return "HEALER";
        if (read.tankBias > 0.55 || target.getMaxHealth() > 80.0) return "TANK";
        if (read.rangedBias > 0.55 || targetStats.rangedRatio() > 0.45 || typeStats.rangedRatio() > 0.55) return "RANGED_PRESSURE";
        if (read.aggression > 0.55 || targetStats.meleeRatio() > 0.45) return "MELEE_BURST";
        return "UNKNOWN_OP";
    }

    private static double predictedDistance(LivingEntity sukuna, LivingEntity target) {
        Vec3 predicted = target.position().add(target.getDeltaMovement().scale(8.0));
        return predicted.distanceTo(sukuna.position());
    }

    private static double idealDistance(String archetype, double infinitySignal, double tankScore, double healScore) {
        if (infinitySignal > 0.1) return 18.0;
        if ("MELEE_BURST_TANK".equals(archetype)) return 12.0;
        if ("MELEE_BURST".equals(archetype) || "BLACK_FLASH_USER".equals(archetype)) return 12.0;
        if ("RANGED_PRESSURE".equals(archetype)) return 16.0;
        if (tankScore > 0.65 || healScore > 0.55) return 24.0;
        return 14.0;
    }

    private static boolean isItadoriModulo(LivingEntity target) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        String name = id == null ? target.getType().toString().toLowerCase() : id.toString().toLowerCase();
        return name.equals("jjsk:itadori_yuji_modulo")
                || name.equals("jujutsucrafts:itadori_yuji_modulo")
                || name.equals("jujutsucraftaddon:itadori_yuji_modulo")
                || name.contains("itadori_yuji_modulo");
    }

    private static DangerScan scanDanger(LevelAccessor world, LivingEntity sukuna) {
        if (!(world instanceof Level level)) {
            return DangerScan.NONE;
        }
        AABB box = sukuna.getBoundingBox().inflate(9.0);
        double areaRisk = 0.0;
        double incomingRisk = 0.0;
        Vec3 dodgeVector = Vec3.ZERO;
        Vec3 sukunaCenter = sukuna.position().add(0.0, sukuna.getBbHeight() * 0.5, 0.0);
        for (Entity entity : level.getEntities(sukuna, box, entity -> entity instanceof Projectile)) {
            if (entity instanceof Projectile projectile && projectile.getOwner() == sukuna) {
                continue;
            }
            double dist = Math.max(1.0, entity.distanceTo(sukuna));
            areaRisk += 1.0 / dist;
            Vec3 velocity = entity.getDeltaMovement();
            if (velocity.lengthSqr() > 1.0E-4) {
                Vec3 toSukuna = sukunaCenter.subtract(entity.position());
                Vec3 travel = velocity.normalize();
                double approach = travel.dot(toSukuna.normalize());
                double missDistance = toSukuna.subtract(travel.scale(toSukuna.dot(travel))).length();
                if (approach > 0.55 && missDistance < 3.5) {
                    double risk = approach * (3.5 - missDistance) / 3.5 * Mth.clamp(9.0 / dist, 0.0, 1.0);
                    incomingRisk += risk;
                    Vec3 lateral = new Vec3(-travel.z, 0.0, travel.x);
                    if (lateral.lengthSqr() > 1.0E-4) {
                        dodgeVector = dodgeVector.add(lateral.normalize().scale(risk));
                    }
                }
            }
        }
        if (dodgeVector.lengthSqr() < 1.0E-4) {
            dodgeVector = new Vec3(1.0, 0.0, 0.0);
        } else {
            dodgeVector = dodgeVector.normalize();
        }
        return new DangerScan(Mth.clamp(areaRisk, 0.0, 1.0), Mth.clamp(incomingRisk, 0.0, 1.0), dodgeVector);
    }

    private static PurpleThreat scanPurpleThreat(LevelAccessor world, LivingEntity sukuna, LivingEntity target, double targetSkill, PlayerRead read) {
        if (!(world instanceof Level level)) {
            return PurpleThreat.NONE;
        }
        Vec3 sukunaCenter = sukuna.position().add(0.0, sukuna.getBbHeight() * 0.5, 0.0);
        boolean gojo = read.primaryTechnique == TechniqueIDs.GOJO || read.secondaryTechnique == TechniqueIDs.GOJO || isSatushi(target);
        boolean windup = targetSkill == 215.0
                || target.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get())
                || target.hasEffect(JujutsucraftaddonModMobEffects.WORLD_GOJO.get())
                || (gojo && target.getPersistentData().getDouble("cnt1") > 0.0 && target.getPersistentData().getDouble("cnt1") < 85.0);
        double risk = windup ? Mth.clamp(1.0 - target.distanceTo(sukuna) / 48.0, 0.25, 0.85) : 0.0;
        boolean projectile = false;
        boolean lineOfFire = false;
        Vec3 dodge = Vec3.ZERO;
        AABB box = sukuna.getBoundingBox().inflate(42.0);
        for (Entity entity : level.getEntities(sukuna, box, OpSukunaBrain::isPurpleEntity)) {
            if (entity instanceof Projectile projectileEntity && projectileEntity.getOwner() == sukuna) {
                continue;
            }
            projectile = true;
            Vec3 velocity = purpleVelocity(entity);
            Vec3 fromPurple = sukunaCenter.subtract(entity.position());
            double dist = Math.max(1.0, fromPurple.length());
            double size = Math.max(2.5, entity.getBbWidth() + entity.getPersistentData().getDouble("Range") * 0.18
                    + entity.getPersistentData().getDouble("BlockRange") * 0.08);
            double localRisk = Mth.clamp(size / dist, 0.0, 0.75);
            if (velocity.lengthSqr() > 1.0E-4) {
                Vec3 travel = velocity.normalize();
                double approach = travel.dot(fromPurple.normalize());
                double miss = fromPurple.subtract(travel.scale(fromPurple.dot(travel))).length();
                if (approach > 0.35 && miss < size + 2.75) {
                    lineOfFire = true;
                    localRisk += approach * Mth.clamp((size + 2.75 - miss) / Math.max(1.0, size + 2.75), 0.0, 1.0);
                    Vec3 lateral = new Vec3(-travel.z, 0.0, travel.x);
                    if (lateral.lengthSqr() > 1.0E-4) {
                        dodge = dodge.add(lateral.normalize().scale(localRisk));
                    }
                }
            } else {
                dodge = dodge.add(horizontal(sukuna.position().subtract(entity.position())).scale(localRisk));
            }
            risk = Math.max(risk, localRisk);
        }
        if (dodge.lengthSqr() < 1.0E-4) {
            Vec3 targetLine = horizontal(sukuna.position().subtract(target.position()));
            dodge = targetLine.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(-targetLine.z, 0.0, targetLine.x);
        }
        risk = Mth.clamp(risk, 0.0, 1.0);
        return new PurpleThreat(risk > 0.22 || windup, risk, windup, projectile, lineOfFire, dodge.normalize());
    }

    private static boolean isPurpleEntity(Entity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        String name = id == null ? entity.getType().toString().toLowerCase() : id.toString().toLowerCase();
        CompoundTag data = entity.getPersistentData();
        return name.equals("jujutsucraft:purple")
                || name.contains("purple")
                || data.getDouble("purple") == 2.0
                || data.getBoolean("flag_purple");
    }

    private static Vec3 purpleVelocity(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        Vec3 persistent = new Vec3(data.getDouble("x_power"), data.getDouble("y_power"), data.getDouble("z_power"));
        return persistent.lengthSqr() > 1.0E-4 ? persistent : entity.getDeltaMovement();
    }

    private static class DangerScan {
        static final DangerScan NONE = new DangerScan(0.0, 0.0, new Vec3(1.0, 0.0, 0.0));
        final double areaRisk;
        final double incomingProjectileRisk;
        final Vec3 dodgeVector;

        DangerScan(double areaRisk, double incomingProjectileRisk, Vec3 dodgeVector) {
            this.areaRisk = areaRisk;
            this.incomingProjectileRisk = incomingProjectileRisk;
            this.dodgeVector = dodgeVector;
        }
    }

    private static class PurpleThreat {
        static final PurpleThreat NONE = new PurpleThreat(false, 0.0, false, false, false, new Vec3(1.0, 0.0, 0.0));
        final boolean threat;
        final double risk;
        final boolean windup;
        final boolean projectile;
        final boolean lineOfFire;
        final Vec3 dodgeVector;

        PurpleThreat(boolean threat, double risk, boolean windup, boolean projectile, boolean lineOfFire, Vec3 dodgeVector) {
            this.threat = threat;
            this.risk = risk;
            this.windup = windup;
            this.projectile = projectile;
            this.lineOfFire = lineOfFire;
            this.dodgeVector = dodgeVector;
        }
    }

    private enum CombatRelation {
        ACTIVE_ATTACKER,
        IMMINENT_HOSTILE,
        POTENTIAL_HOSTILE,
        PASSIVE_VALID,
        TACTICAL_OBJECT
    }

    private static CombatRelation readCombatRelation(LevelAccessor world, LivingEntity sukuna, LivingEntity target, PlayerRead read, BrainMemory memory) {
        boolean active = target.getLastHurtMob() == sukuna
                || sukuna.getLastHurtByMob() == target
                || (target instanceof Mob mob && mob.getTarget() == sukuna)
                || target.getPersistentData().getBoolean("attack")
                || target.getPersistentData().getDouble("Damage") != 0.0
                || target.getPersistentData().getDouble("skill") != 0.0
                || ownerChainAttackingSukuna(world, sukuna, target);
        if (active) {
            return CombatRelation.ACTIVE_ATTACKER;
        }
        if (isTacticalObject(target)) {
            return CombatRelation.TACTICAL_OBJECT;
        }
        CombatStats stats = memory.target(targetKey(target));
        boolean learnedCombatant = stats.damageTakenAvg > sukuna.getMaxHealth() * 0.012 || stats.skillUses > 0.0 || stats.domainTicks > 0.0;
        boolean jujutsuActor = isJujutsuActor(target, read);
        boolean combatProfile = jujutsuActor || read.domainBias > 0.12 || read.aggression > 0.2 || read.rangedBias > 0.2 || read.healBias > 0.2
                || target.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                || target.hasEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get())
                || target.hasEffect(JujutsucraftaddonModMobEffects.INFINITY.get());
        if (combatProfile && LogicAttackProcedure.execute(world, sukuna, target)) {
            return CombatRelation.IMMINENT_HOSTILE;
        }
        if (learnedCombatant || target instanceof Player || target.getMaxHealth() > 60.0) {
            return CombatRelation.POTENTIAL_HOSTILE;
        }
        return CombatRelation.PASSIVE_VALID;
    }

    private static boolean ownerChainAttackingSukuna(LevelAccessor world, LivingEntity sukuna, LivingEntity target) {
        if (target instanceof TamableAnimal tame) {
            LivingEntity owner = tame.getOwner();
            if (owner != null) {
                if (owner == sukuna) {
                    return false;
                }
                return owner.getLastHurtMob() == sukuna || (owner instanceof Mob mob && mob.getTarget() == sukuna);
            }
        }
        String ownerId = target.getPersistentData().getString("OWNER_UUID");
        if (ownerId.isEmpty() || ownerId.equals(sukuna.getStringUUID()) || !(world instanceof Level level)) {
            return false;
        }
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(36.0), e -> ownerId.equals(e.getStringUUID()))) {
            if (nearby.getLastHurtMob() == sukuna || (nearby instanceof Mob mob && mob.getTarget() == sukuna)) {
                return true;
            }
        }
        return false;
    }

    private static PowerProfile readPowerProfile(LivingEntity sukuna, LivingEntity target, PlayerRead read, CombatStats targetStats, CombatStats typeStats) {
        double dataPower = readBaseDataPower(target);
        double nbtPower = Math.max(target.getPersistentData().getDouble("power"), target.getPersistentData().getDouble("Power"));
        double boostPower = damageBoostPower(target);
        double sixEyesPower = Math.max(Math.max(dataPower, nbtPower), boostPower);
        double playerPower = Mth.clamp(Math.max(read.curseEnergy, read.curseEnergyMax) / 1600.0, 0.0, 1.0);
        double outputPower = Mth.clamp((read.outputLevel - 1.0) / 5.0, 0.0, 0.75);
        double masteryPower = Mth.clamp(read.techniqueMastery / 600.0, 0.0, 0.65);
        double hpPower = Mth.clamp(target.getMaxHealth() / Math.max(40.0, sukuna.getMaxHealth() * 0.6), 0.0, 1.0);
        double armorPower = Mth.clamp((target.getArmorValue() + safeAttribute(target, Attributes.ARMOR_TOUGHNESS, 0.0) * 1.5) / 32.0, 0.0, 0.75);
        double attackPower = Mth.clamp(safeAttribute(target, Attributes.ATTACK_DAMAGE, 0.0) / 28.0, 0.0, 0.9);
        double speedPower = Mth.clamp((safeAttribute(target, Attributes.MOVEMENT_SPEED, 0.23) - 0.23) / 0.35, 0.0, 0.5);
        double knockbackPower = Mth.clamp(safeAttribute(target, Attributes.KNOCKBACK_RESISTANCE, 0.0), 0.0, 0.5);
        double effectPower = effectPower(target, read);
        double specialTier = specialTier(target, read);
        double basePower = Math.max(sixEyesPower > 0.0 ? Mth.clamp(sixEyesPower / 30.0, 0.0, 1.25) : 0.0, playerPower);
        double score = Mth.clamp(basePower * 0.45 + outputPower + masteryPower * 0.35 + hpPower * 0.35 + armorPower * 0.25
                + attackPower * 0.35 + speedPower * 0.12 + knockbackPower * 0.15 + effectPower + specialTier
                + Mth.clamp((targetStats.damageTakenAvg + typeStats.damageTakenAvg) / Math.max(1.0, sukuna.getMaxHealth() * 0.1), 0.0, 0.45), 0.0, 1.35);
        boolean apex = specialTier >= 0.85 || sixEyesPower >= 28.0;
        boolean high = apex || specialTier >= 0.45 || score >= 0.58 || target instanceof Player && (read.curseEnergy > 350.0 || read.outputLevel >= 3.0);
        return new PowerProfile(score, basePower, specialTier, effectPower, high, apex);
    }

    private static double effectPower(LivingEntity target, PlayerRead read) {
        double value = 0.0;
        value += Mth.clamp(damageBoostPower(target) / 36.0, 0.0, 0.75);
        if (target.hasEffect(MobEffects.DAMAGE_RESISTANCE)) value += 0.2;
        if (target.hasEffect(MobEffects.REGENERATION) || read.rct > 0.0) value += 0.18;
        if (target.hasEffect(JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get())) value += 0.18;
        if (target.hasEffect(JujutsucraftModMobEffects.GUARD.get())) value += 0.12;
        if (target.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) value += 0.34;
        if (target.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.SIMPLE_DOMAIN_MAX.get())) value += 0.12;
        if (target.hasEffect(JujutsucraftaddonModMobEffects.HWB.get())) value += 0.12;
        if (target.hasEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.INFINITY.get())) value += 0.45;
        if (target.hasEffect(JujutsucraftaddonModMobEffects.DODGE.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.COUNTER.get())) value += 0.12;
        return Mth.clamp(value, 0.0, 0.9);
    }

    private static KillEstimate estimateKill(LivingEntity sukuna, LivingEntity target, PowerProfile power, CombatStats targetStats, PlayerRead read) {
        double health = target.getHealth();
        double maxHealth = Math.max(1.0, target.getMaxHealth());
        double mitigation = 1.0 + target.getArmorValue() / 24.0 + safeAttribute(target, Attributes.ARMOR_TOUGHNESS, 0.0) / 18.0
                + (target.hasEffect(MobEffects.DAMAGE_RESISTANCE) ? 0.35 : 0.0)
                + (target.hasEffect(JujutsucraftModMobEffects.GUARD.get()) ? 0.25 : 0.0);
        double regen = target.hasEffect(MobEffects.REGENERATION) || read.rct > 0.0 || targetStats.targetHealAvg > maxHealth * 0.02 ? 0.25 : 0.0;
        double effectiveHealth = health * mitigation * (1.0 + regen + power.effectScore * 0.22);
        double slashRatio = Mth.clamp(effectiveHealth / Math.max(1.0, sukuna.getMaxHealth() * 0.45), 0.0, 3.0);
        double finisher = Mth.clamp((maxHealth * 0.45 - health) / Math.max(1.0, maxHealth * 0.45), 0.0, 1.0);
        double resource = Mth.clamp((slashRatio - 0.45) / 1.55 + power.score * 0.45 + regen * 0.4, 0.0, 1.0);
        boolean difficult = effectiveHealth > sukuna.getMaxHealth() * 0.42 || power.high || regen > 0.0;
        return new KillEstimate(effectiveHealth, slashRatio, finisher, resource, difficult);
    }

    private static int damageBoostPower(LivingEntity target) {
        MobEffectInstance effect = target.getEffect(MobEffects.DAMAGE_BOOST);
        return effect == null ? 0 : effect.getAmplifier();
    }

    private static double safeAttribute(LivingEntity target, Attribute attribute, double fallback) {
        try {
            return target.getAttribute(attribute) == null ? fallback : target.getAttributeValue(attribute);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static boolean isJujutsuActor(LivingEntity target, PlayerRead read) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        String name = id == null ? target.getType().toString().toLowerCase() : id.toString().toLowerCase();
        return name.startsWith("jujutsucraft:")
                || name.startsWith("jujutsucraftaddon:")
                || name.startsWith("jjsk:")
                || name.startsWith("jujutsucrafts:")
                || read.primaryTechnique != 0.0
                || read.secondaryTechnique != 0.0
                || read.curseEnergy > 0.0
                || target.getPersistentData().getDouble("skill") != 0.0
                || target.getPersistentData().contains("skill_domain");
    }

    private static boolean isTacticalObject(LivingEntity target) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        String name = id == null ? target.getType().toString().toLowerCase() : id.toString().toLowerCase();
        if (name.contains("rika") || name.contains("mahoraga") || name.contains("agito")) {
            return false;
        }
        return name.contains("barrier") || name.contains("veil") || name.contains("domain") || name.contains("shrine")
                || name.contains("clone") || name.contains("shadow") || name.contains("construct") || name.contains("cursed_spirit_ball")
                || name.contains("blue_entity") || name.contains("red_entity") || name.contains("cleave_web");
    }

    private static double specialTier(LivingEntity target, PlayerRead read) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        String name = id == null ? target.getType().toString().toLowerCase() : id.toString().toLowerCase();
        if (name.contains("satushi") || name.contains("satuxi") || name.contains("satuchi")) {
            return 1.05;
        }
        if (name.contains("gojo") || name.contains("sukuna") || name.contains("mahoraga")
                || read.primaryTechnique == TechniqueIDs.GOJO || read.secondaryTechnique == TechniqueIDs.GOJO) {
            return 0.95;
        }
        if (name.contains("yuta") || name.contains("okkotsu") || name.contains("rika") || name.contains("kenjaku") || name.contains("geto")
                || name.contains("tsukumo") || name.contains("kashimo") || name.contains("hakari") || name.contains("jogo")
                || name.contains("hanami") || name.contains("uraume") || name.contains("toji") || name.contains("maki")
                || name.contains("itadori_shinjuku") || read.primaryTechnique == TechniqueIDs.OKKOTSU || read.secondaryTechnique == TechniqueIDs.OKKOTSU
                || read.primaryTechnique == TechniqueIDs.HAKARI || read.secondaryTechnique == TechniqueIDs.HAKARI
                || read.primaryTechnique == TechniqueIDs.JOGO || read.secondaryTechnique == TechniqueIDs.JOGO
                || read.primaryTechnique == TechniqueIDs.URAUME || read.secondaryTechnique == TechniqueIDs.URAUME) {
            return 0.55;
        }
        if (name.contains("itadori") || name.contains("yuji") || name.contains("megumi") || name.contains("mahito") || name.contains("choso")) {
            return 0.35;
        }
        return 0.0;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static double readBaseDataPower(LivingEntity target) {
        try {
            if (target instanceof JujutsuSorcererEntity sorcerer) {
                return sorcerer.getEntityData().get(JujutsuSorcererEntity.DATA_power);
            }
            if (target instanceof CurseUserEntity curseUser) {
                return curseUser.getEntityData().get(CurseUserEntity.DATA_power);
            }
        } catch (RuntimeException ignored) {
        }
        return readSynchedDouble(target, "DATA_power");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static double readSynchedDouble(LivingEntity target, String fieldName) {
        try {
            java.lang.reflect.Field field = target.getClass().getField(fieldName);
            Object accessor = field.get(null);
            if (accessor instanceof EntityDataAccessor dataAccessor) {
                Object value = target.getEntityData().get(dataAccessor);
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return 0.0;
    }

    private static class PowerProfile {
        final double score;
        final double basePower;
        final double specialTier;
        final double effectScore;
        final boolean high;
        final boolean apex;

        PowerProfile(double score, double basePower, double specialTier, double effectScore, boolean high, boolean apex) {
            this.score = score;
            this.basePower = basePower;
            this.specialTier = specialTier;
            this.effectScore = effectScore;
            this.high = high;
            this.apex = apex;
        }
    }

    private static class KillEstimate {
        final double effectiveHealth;
        final double slashRatio;
        final double finisherValue;
        final double resourceValue;
        final boolean difficult;

        KillEstimate(double effectiveHealth, double slashRatio, double finisherValue, double resourceValue, boolean difficult) {
            this.effectiveHealth = effectiveHealth;
            this.slashRatio = slashRatio;
            this.finisherValue = finisherValue;
            this.resourceValue = resourceValue;
            this.difficult = difficult;
        }
    }

    private static class ThreatScan {
        static final ThreatScan EMPTY = new ThreatScan(List.of(), null, 0.0, 0.0, 0.0, Vec3.ZERO, false, false);
        final List<ThreatCandidate> candidates;
        final LivingEntity bestTarget;
        final double groupPressure;
        final double groupActivePressure;
        final double groupApexPressure;
        final Vec3 escapeVector;
        final boolean bestImmediatePriority;
        final boolean currentOutclassed;

        ThreatScan(List<ThreatCandidate> candidates, LivingEntity bestTarget, double groupPressure, double groupActivePressure, double groupApexPressure,
                   Vec3 escapeVector, boolean bestImmediatePriority, boolean currentOutclassed) {
            this.candidates = candidates;
            this.bestTarget = bestTarget;
            this.groupPressure = groupPressure;
            this.groupActivePressure = groupActivePressure;
            this.groupApexPressure = groupApexPressure;
            this.escapeVector = escapeVector;
            this.bestImmediatePriority = bestImmediatePriority;
            this.currentOutclassed = currentOutclassed;
        }

        static ThreatScan scan(LevelAccessor world, LivingEntity sukuna, LivingEntity current, BrainMemory memory) {
            if (!(world instanceof Level level)) {
                return EMPTY;
            }
            AABB box = sukuna.getBoundingBox().inflate(42.0, 18.0, 42.0);
            List<ThreatCandidate> candidates = level.getEntitiesOfClass(LivingEntity.class, box, target -> isValidEnemy(world, sukuna, target)).stream()
                    .map(target -> ThreatCandidate.read(sukuna, target, current, memory))
                    .sorted(Comparator.comparingDouble((ThreatCandidate candidate) -> candidate.score).reversed())
                    .toList();
            if (candidates.isEmpty()) {
                return EMPTY;
            }

            Vec3 escape = Vec3.ZERO;
            double pressureRaw = 0.0;
            double activeRaw = 0.0;
            double apexRaw = 0.0;
            for (ThreatCandidate candidate : candidates) {
                if (candidate.distance <= 18.0) {
                    Vec3 away = horizontal(sukuna.position().subtract(candidate.target.position()));
                    double spatial = Mth.clamp((18.0 - candidate.distance) / 18.0, 0.0, 1.0);
                    double escapeWeight = candidate.spatialPressure * spatial;
                    escape = escape.add(away.scale(escapeWeight));
                    pressureRaw += escapeWeight;
                    if (candidate.relation == CombatRelation.ACTIVE_ATTACKER) {
                        activeRaw += escapeWeight;
                    }
                    if (candidate.powerProfile.apex) {
                        apexRaw += escapeWeight;
                    }
                }
            }
            double pressure = Mth.clamp(pressureRaw / 3.5, 0.0, 1.0);
            double activePressure = Mth.clamp(activeRaw / 2.0, 0.0, 1.0);
            double apexPressure = Mth.clamp(apexRaw / 1.5, 0.0, 1.0);
            if (escape.lengthSqr() < 1.0E-4) {
                escape = current != null ? horizontal(sukuna.position().subtract(current.position())) : Vec3.ZERO;
            } else {
                escape = escape.normalize();
            }

            ThreatCandidate best = chooseBestCandidate(candidates);
            double currentScore = current == null ? 0.0 : candidates.stream()
                    .filter(candidate -> candidate.target == current)
                    .map(candidate -> candidate.score)
                    .findFirst()
                    .orElse(0.0);
            boolean outclassed = current != null && !targetKey(best.target).equals(targetKey(current)) && best.score >= currentScore + 0.85;
            return new ThreatScan(candidates, best.target, pressure, activePressure, apexPressure, escape, best.immediatePriority, outclassed);
        }

        private static ThreatCandidate chooseBestCandidate(List<ThreatCandidate> candidates) {
            boolean hasRelevantHigh = candidates.stream().anyMatch(candidate ->
                    (candidate.relation == CombatRelation.ACTIVE_ATTACKER || candidate.relation == CombatRelation.IMMINENT_HOSTILE)
                            && candidate.powerProfile.high);
            if (!hasRelevantHigh) {
                return candidates.get(0);
            }
            return candidates.stream()
                    .filter(candidate -> candidate.relation != CombatRelation.PASSIVE_VALID || candidate.immediatePriority)
                    .max(Comparator.comparingDouble(candidate -> candidate.score))
                    .orElse(candidates.get(0));
        }

        double scoreOf(LivingEntity target) {
            String key = targetKey(target);
            for (ThreatCandidate candidate : candidates) {
                if (targetKey(candidate.target).equals(key)) {
                    return candidate.score;
                }
            }
            return 0.0;
        }

        ThreatCandidate candidateOf(LivingEntity target) {
            String key = targetKey(target);
            for (ThreatCandidate candidate : candidates) {
                if (targetKey(candidate.target).equals(key)) {
                    return candidate;
                }
            }
            return null;
        }
    }

    private static class ThreatCandidate {
        final LivingEntity target;
        final double score;
        final double distance;
        final double spatialPressure;
        final CombatRelation relation;
        final PowerProfile powerProfile;
        final KillEstimate killEstimate;
        final boolean immediatePriority;

        ThreatCandidate(LivingEntity target, double score, double distance, double spatialPressure, CombatRelation relation,
                        PowerProfile powerProfile, KillEstimate killEstimate, boolean immediatePriority) {
            this.target = target;
            this.score = score;
            this.distance = distance;
            this.spatialPressure = spatialPressure;
            this.relation = relation;
            this.powerProfile = powerProfile;
            this.killEstimate = killEstimate;
            this.immediatePriority = immediatePriority;
        }

        static ThreatCandidate read(LivingEntity sukuna, LivingEntity target, LivingEntity current, BrainMemory memory) {
            double distance = sukuna.distanceTo(target);
            CompoundTag nbt = target.getPersistentData();
            CombatStats stats = memory.target(targetKey(target));
            CombatStats typeStats = memory.type(typeKey(target));
            PlayerRead read = PlayerRead.read(target);
            CombatRelation relation = readCombatRelation(sukuna.level(), sukuna, target, read, memory);
            PowerProfile power = readPowerProfile(sukuna, target, read, stats, typeStats);
            KillEstimate kill = estimateKill(sukuna, target, power, stats, read);
            double skill = nbt.getDouble("skill");
            boolean domain = target.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
            boolean castingDomain = skill == 20.0 || (read.primaryTechnique == TechniqueIDs.GOJO && nbt.getDouble("cnt1") > 0.0 && nbt.getDouble("cnt1") < 40.0);
            boolean healer = target.hasEffect(MobEffects.REGENERATION) || read.rct > 0.0 || stats.targetHealAvg > target.getMaxHealth() * 0.025;
            boolean burst = skill != 0.0 || nbt.getBoolean("attack") || nbt.getDouble("Damage") != 0.0;
            boolean lowHp = target.getHealth() / Math.max(1.0, target.getMaxHealth()) < 0.34;
            boolean chasing = target.getDeltaMovement().dot(sukuna.position().subtract(target.position())) > 0.025;
            boolean cooldown = target.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get()) || target.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get())
                    || target.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get());
            double proximity = Mth.clamp((38.0 - distance) / 38.0, 0.0, 1.0);
            double recentDamage = Mth.clamp(stats.damageTakenAvg / Math.max(1.0, sukuna.getMaxHealth() * 0.06), 0.0, 1.0);
            double relationScore = switch (relation) {
                case ACTIVE_ATTACKER -> 2.2;
                case IMMINENT_HOSTILE -> 1.45;
                case POTENTIAL_HOSTILE -> 0.75;
                case PASSIVE_VALID -> -0.45;
                case TACTICAL_OBJECT -> -0.2;
            };
            double score = relationScore
                    + power.score * 1.8
                    + kill.resourceValue * 0.65
                    + kill.finisherValue * 0.5
                    + proximity * (relation == CombatRelation.PASSIVE_VALID ? 0.12 : 0.32)
                    + (domain ? 1.25 : 0.0)
                    + (castingDomain ? 2.2 : 0.0)
                    + (healer ? 0.75 : 0.0)
                    + (burst ? 0.65 : 0.0)
                    + (lowHp ? 0.8 : 0.0)
                    + (chasing ? 0.35 : 0.0)
                    + (cooldown ? 0.25 : 0.0)
                    + recentDamage * 0.65
                    + read.domainBias * 0.45
                    + read.aggression * 0.25
                    + read.rangedBias * 0.2
                    + (target == current ? 0.32 : 0.0);
            if (relation == CombatRelation.PASSIVE_VALID && !power.high && !burst && recentDamage < 0.25) {
                score -= 1.15;
            }
            if (relation == CombatRelation.TACTICAL_OBJECT && !burst && recentDamage < 0.2) {
                score -= 0.9;
            }
            boolean immediate = castingDomain || (domain && distance < 36.0) || (healer && target.getHealth() < target.getMaxHealth() * 0.7)
                    || (lowHp && distance < 34.0 && relation != CombatRelation.PASSIVE_VALID) || (burst && distance < 12.0)
                    || (relation == CombatRelation.ACTIVE_ATTACKER && distance < 18.0 && (power.high || recentDamage > 0.25));
            if (immediate) {
                score += 0.65;
            }
            double pressure = switch (relation) {
                case ACTIVE_ATTACKER -> 1.15;
                case IMMINENT_HOSTILE -> 0.8;
                case POTENTIAL_HOSTILE -> 0.45;
                case PASSIVE_VALID -> 0.08;
                case TACTICAL_OBJECT -> burst || recentDamage > 0.25 ? 0.55 : 0.25;
            };
            pressure += power.score * (power.apex ? 1.0 : 0.45);
            return new ThreatCandidate(target, score, distance, pressure, relation, power, kill, immediate);
        }
    }

    private static double scoreTargetSkill(double skill, CombatStats stats) {
        if (skill <= 0.0) return stats.damageTakenAvg > stats.damageDealtAvg ? 0.25 : 0.0;
        double repeat = stats.skillUseRatio(skill);
        double base = skill >= 20.0 ? 0.32 : 0.12;
        if (skill >= 100.0) base += 0.22;
        if (skill >= 1000.0) base += 0.22;
        return Mth.clamp(base + repeat * 0.42 + stats.damageTakenPeak * 0.01, 0.0, 1.0);
    }

    private static double estimatePower(Snapshot s) {
        double hpPower = s.target.getMaxHealth() / Math.max(1.0, s.sukuna.getMaxHealth());
        double damagePower = (s.targetStats.damageTakenAvg + s.damageTaken) / Math.max(1.0, s.sukuna.getMaxHealth() * 0.08);
        double survivalPower = s.targetHealthRatio > 0.72 && s.targetStats.seenTicks > 200.0 ? 0.18 : 0.0;
        double domainPower = s.targetDomain || s.targetStats.domainTicks > 80.0 ? 0.25 : 0.0;
        return Mth.clamp(hpPower * 0.18 + damagePower * 0.32 + s.healScore * 0.15 + survivalPower + domainPower
                + s.read.outputLevel * 0.04 + s.targetPowerProfile.score * 0.55, 0.0, 1.0);
    }

    private static double estimateThreat(Snapshot s) {
        double specialDefense = (s.infinitySignal > 0.0 ? 0.8 : 0.0)
                + (s.targetDomain || s.targetCastingDomain ? 0.55 : 0.0)
                + (s.targetSimpleDomain || s.targetHwb || s.targetNeutralization || s.targetDomainAmplification ? 0.25 : 0.0);
        double specialBody = (s.itadoriModulo ? 0.9 : 0.0)
                + (s.target instanceof Player ? 0.2 : 0.0)
                + (s.read.ultimate ? 0.2 : 0.0);
        return Mth.clamp(s.powerScore + s.targetSkillDanger * 0.35 + s.tankScore * 0.25 + s.healScore * 0.2
                + s.meleeThreat * 0.2 + s.rangeThreat * 0.2 + s.damageTakenBurst * 0.35 + specialDefense + specialBody, 0.0, 1.5);
    }

    private static boolean isTrivialTarget(Snapshot s) {
        if (s.targetCombatRelation != CombatRelation.PASSIVE_VALID || s.targetPowerProfile.high) return false;
        if (s.target instanceof Player || s.itadoriModulo || s.infinitySignal > 0.0 || s.targetDomain || s.targetCastingDomain) return false;
        if (s.targetSimpleDomain || s.targetHwb || s.targetNeutralization || s.targetDomainAmplification || s.targetRegen) return false;
        if (s.target.getMaxHealth() > 40.0 || s.targetThreat >= 0.32) return false;
        return s.damageTakenBurst < 0.12 && s.targetSkillDanger < 0.25;
    }

    private static class AdaptationView {
        static final double TARGET_WEIGHT = 1.0;
        static final double TECHNIQUE_WEIGHT = 0.55;
        static final double TYPE_WEIGHT = 0.35;
        static final double ARCHETYPE_WEIGHT = 0.25;

        final double confidence;
        final double trapMastery;
        final double antiEvasionNeed;
        final double rangeKiteNeed;
        final double learnedBurstRisk;
        final double preferredRange;
        final double safePunishWindow;
        final Map<String, Double> learnedActionBias;

        private AdaptationView(double confidence, double trapMastery, double antiEvasionNeed, double rangeKiteNeed, double learnedBurstRisk,
                               double preferredRange, double safePunishWindow, Map<String, Double> learnedActionBias) {
            this.confidence = confidence;
            this.trapMastery = trapMastery;
            this.antiEvasionNeed = antiEvasionNeed;
            this.rangeKiteNeed = rangeKiteNeed;
            this.learnedBurstRisk = learnedBurstRisk;
            this.preferredRange = preferredRange;
            this.safePunishWindow = safePunishWindow;
            this.learnedActionBias = learnedActionBias;
        }

        static AdaptationView create(Snapshot s) {
            double seen = weighted(s, stats -> stats.seenTicks);
            double confidence = Mth.clamp((seen - 40.0) / 760.0, 0.0, 1.0);
            double trap = confidence * weightedRatio(s, stats -> stats.trapTicks);
            double evasion = confidence * weightedRatio(s, stats -> stats.evasionTicks);
            double range = confidence * weightedRatio(s, stats -> stats.rangePressureTicks);
            double burst = confidence * Mth.clamp(weighted(s, stats -> stats.damageTakenAvg) / Math.max(1.0, s.sukuna.getMaxHealth() * 0.08), 0.0, 1.0);
            double preferred = weightedDistance(s);
            double success = weighted(s, stats -> stats.actionSuccessTicks);
            double failure = weighted(s, stats -> stats.actionFailureTicks);
            double safePunish = confidence * Mth.clamp(success / Math.max(1.0, success + failure), 0.0, 1.0);
            Map<String, Double> actionBias = new HashMap<>();
            addActionBias(actionBias, s.targetStats, TARGET_WEIGHT);
            addActionBias(actionBias, s.techniqueStats, TECHNIQUE_WEIGHT);
            addActionBias(actionBias, s.typeStats, TYPE_WEIGHT);
            addActionBias(actionBias, s.archetypeStats, ARCHETYPE_WEIGHT);
            double totalWeight = TARGET_WEIGHT + TECHNIQUE_WEIGHT + TYPE_WEIGHT + ARCHETYPE_WEIGHT;
            actionBias.replaceAll((key, value) -> Mth.clamp(value / totalWeight * confidence, -0.45, 0.42));
            return new AdaptationView(confidence, Mth.clamp(trap, 0.0, 1.0), Mth.clamp(evasion, 0.0, 1.0),
                    Mth.clamp(range, 0.0, 1.0), Mth.clamp(burst, 0.0, 1.0), preferred, Mth.clamp(safePunish, 0.0, 1.0), actionBias);
        }

        double actionBias(String action) {
            return learnedActionBias.getOrDefault(action, 0.0);
        }

        private interface StatReader {
            double read(CombatStats stats);
        }

        private static double weighted(Snapshot s, StatReader reader) {
            double total = TARGET_WEIGHT + TECHNIQUE_WEIGHT + TYPE_WEIGHT + ARCHETYPE_WEIGHT;
            return (reader.read(s.targetStats) * TARGET_WEIGHT
                    + reader.read(s.techniqueStats) * TECHNIQUE_WEIGHT
                    + reader.read(s.typeStats) * TYPE_WEIGHT
                    + reader.read(s.archetypeStats) * ARCHETYPE_WEIGHT) / total;
        }

        private static double weightedRatio(Snapshot s, StatReader reader) {
            double numerator = reader.read(s.targetStats) * TARGET_WEIGHT
                    + reader.read(s.techniqueStats) * TECHNIQUE_WEIGHT
                    + reader.read(s.typeStats) * TYPE_WEIGHT
                    + reader.read(s.archetypeStats) * ARCHETYPE_WEIGHT;
            double denominator = s.targetStats.seenTicks * TARGET_WEIGHT
                    + s.techniqueStats.seenTicks * TECHNIQUE_WEIGHT
                    + s.typeStats.seenTicks * TYPE_WEIGHT
                    + s.archetypeStats.seenTicks * ARCHETYPE_WEIGHT;
            return Mth.clamp(numerator / Math.max(1.0, denominator), 0.0, 1.0);
        }

        private static double weightedDistance(Snapshot s) {
            double numerator = distanceContribution(s.targetStats, TARGET_WEIGHT)
                    + distanceContribution(s.techniqueStats, TECHNIQUE_WEIGHT)
                    + distanceContribution(s.typeStats, TYPE_WEIGHT)
                    + distanceContribution(s.archetypeStats, ARCHETYPE_WEIGHT);
            double denominator = distanceWeight(s.targetStats, TARGET_WEIGHT)
                    + distanceWeight(s.techniqueStats, TECHNIQUE_WEIGHT)
                    + distanceWeight(s.typeStats, TYPE_WEIGHT)
                    + distanceWeight(s.archetypeStats, ARCHETYPE_WEIGHT);
            return denominator <= 0.0 ? 0.0 : numerator / denominator;
        }

        private static double distanceContribution(CombatStats stats, double weight) {
            return stats.distanceAvg > 0.0 ? stats.distanceAvg * stats.seenTicks * weight : 0.0;
        }

        private static double distanceWeight(CombatStats stats, double weight) {
            return stats.distanceAvg > 0.0 ? stats.seenTicks * weight : 0.0;
        }

        private static void addActionBias(Map<String, Double> out, CombatStats stats, double weight) {
            for (String action : stats.actionSuccess.keySet()) {
                double success = stats.actionSuccess.getOrDefault(action, 0.0);
                double failure = stats.actionFailure.getOrDefault(action, 0.0);
                double total = success + failure;
                if (total < 0.35) {
                    continue;
                }
                double value = Mth.clamp((success - failure) / Math.max(1.0, total), -1.0, 1.0) * weight;
                out.put(action, out.getOrDefault(action, 0.0) + value);
            }
            for (String action : stats.actionFailure.keySet()) {
                if (stats.actionSuccess.containsKey(action)) {
                    continue;
                }
                double failure = stats.actionFailure.getOrDefault(action, 0.0);
                if (failure >= 0.35) {
                    out.put(action, out.getOrDefault(action, 0.0) - Mth.clamp(failure / 4.0, 0.0, 1.0) * weight);
                }
            }
        }
    }

    private static class DomainTactics {
        static DomainAssessment assess(Snapshot s) {
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

            double targetDomainThreat = Mth.clamp((s.targetCastingDomain ? 1.0 : 0.0) + (s.targetDomain ? 0.75 : 0.0) + s.read.domainBias * 0.55
                    + (s.read.ultimate ? 0.25 : 0.0) + (s.read.curseEnergy > 450.0 ? 0.25 : 0.0)
                    + (s.catastrophicDomain ? 0.35 : 0.0), 0.0, 1.0);
            double killValue = Mth.clamp(s.targetKillEstimate.resourceValue * 0.45 + s.killPressure(0.45) * 0.85
                    + (s.targetCooldown || s.targetUnstable || s.targetWhiffed ? 0.18 : 0.0), 0.0, 1.0);
            double checkmate = Mth.clamp(killValue + s.tankScore * 0.28 + s.healScore * 0.25 + s.pressure * 0.25 - counterRisk * 0.75
                    - (s.targetPerfectAntiDomain ? 0.45 : 0.0), 0.0, 1.0);
            double simpleStillEnough = s.canUseSimpleDomain && s.selfHealthRatio > 0.42 && !s.lethalForecast && !s.domainDeathSpiral ? 0.25 : 0.0;
            double counterDomain = Mth.clamp(targetDomainThreat * 0.95 + (s.targetCastingDomain ? 0.45 : 0.0)
                    + (s.catastrophicDomain ? 0.25 : 0.0) + (s.healthLosingRace ? 0.18 : 0.0) - simpleStillEnough, 0.0, 1.0);
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

            DomainIntent intent = DomainIntent.NONE;
            double confidence = 0.0;
            if (counterDomain >= confidence) {
                intent = DomainIntent.COUNTER_DOMAIN;
                confidence = counterDomain;
            }
            if (checkmate > confidence) {
                intent = DomainIntent.CHECKMATE;
                confidence = checkmate;
            }
            if (antiInfinity > confidence) {
                intent = DomainIntent.ANTI_INFINITY;
                confidence = antiInfinity;
            }
            if (crowdReset > confidence) {
                intent = DomainIntent.CROWD_RESET;
                confidence = crowdReset;
            }
            if (survivalStall > confidence) {
                intent = DomainIntent.SURVIVAL_STALL;
                confidence = survivalStall;
            }
            if (confidence < 0.42) {
                intent = DomainIntent.NONE;
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
            double emergency = intent == DomainIntent.COUNTER_DOMAIN ? (s.targetCastingDomain ? 1.55 : 0.75) : 0.0;
            if (s.domainDeathSpiral) emergency += 1.05;
            double castScore = -0.35 + confidence * 1.45 + domainScore * 1.15 + emergency + (1.0 - counterRisk) * 0.25 - counterRisk * 0.95 - wasteRisk;
            if (intent == DomainIntent.CHECKMATE && counterRisk < 0.35) castScore += 0.35;
            if (intent == DomainIntent.CROWD_RESET && (s.groupActivePressure > 0.65 || s.groupApexPressure > 0.45)) castScore += 0.3;
            if (intent == DomainIntent.SURVIVAL_STALL && s.lethalForecast) castScore += 0.25;

            double hwbScore = -0.2 + targetDomainThreat * 2.4 + (s.targetCastingDomain ? 1.4 : 0.0) - (s.selfHealthRatio > 0.75 && !s.targetDomain ? 0.35 : 0.0)
                    - (s.catastrophicDomain && (s.simpleDomainExpiresSoon || s.healthLosingRace) ? 0.75 : 0.0);
            double punishScore = Mth.clamp(counterRisk * 0.45 + targetDomainThreat * 0.35 + (s.targetCooldown ? 0.25 : 0.0), 0.0, 1.0);
            String plan = intent.name() + ":clear=" + round(clearValue) + ",cost=" + round(sustainCost) + ",recovery=" + round(recoveryRisk);
            return new DomainAssessment(counterRisk, targetDomainThreat, castScore, hwbScore, punishScore, intent, confidence,
                    plan, clearValue, sustainCost, recoveryRisk, domainScore);
        }
    }

    private static class DomainAssessment {
        final double counterRisk;
        final double targetDomainThreat;
        final double castScore;
        final double hwbScore;
        final double punishScore;
        final DomainIntent intent;
        final double confidence;
        final String plan;
        final double clearValue;
        final double sustainCost;
        final double recoveryRisk;
        final double domainScore;

        DomainAssessment(double counterRisk, double targetDomainThreat, double castScore, double hwbScore, double punishScore, DomainIntent intent, double confidence,
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

    private static class PlayerRead {
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

        static PlayerRead read(LivingEntity target) {
            PlayerRead read = new PlayerRead();
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
            if (primaryTechnique == TechniqueIDs.GOJO || secondaryTechnique == TechniqueIDs.GOJO) {
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
            if (isSatushi(target)) {
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

    public static class BrainMemory {
        final Map<String, CombatStats> targets = new HashMap<>();
        final Map<String, CombatStats> types = new HashMap<>();
        final Map<String, CombatStats> techniques = new HashMap<>();
        final Map<String, CombatStats> archetypes = new HashMap<>();
        final Map<String, Double> actionValues = new HashMap<>();
        double lastHealth;
        double lastBackstepTick = -1000.0;
        double lastProjectileDodgeTick = -1000.0;
        double lastMoveTick = -1000.0;
        double lastGuardTick = -1000.0;
        double lastGuardTimingTick = -1000.0;
        double lastEvasiveBackstepTick = -1000.0;
        double lastRecoveryWindowTick = -1000.0;
        double lastSimpleDomainTick = -1000.0;
        double lastBurnoutRctTick = -1000.0;
        double lastDomainAmplificationTick = -1000.0;
        double lastSukunaDomainTick = -1000.0;
        double lastPurpleEvadeTick = -1000.0;
        double pendingPurpleResponseTick = -1000.0;
        double firstDomainThreatTick = -1.0;
        double lastTargetSwitchTick = -1000.0;
        double lastTargetSkill;
        double lastTargetSkillTick = -1000.0;
        double lastTargetDistance;
        double lastPosX = Double.NaN;
        double lastPosY = Double.NaN;
        double lastPosZ = Double.NaN;
        double stuckTicks;
        double lastDestinationX = Double.NaN;
        double lastDestinationY = Double.NaN;
        double lastDestinationZ = Double.NaN;
        double lastEscapeTick = -1000.0;
        int strafeSide;
        double megunaHighThreatTicks;
        double groupPressure;
        double recentDamageWindow;
        double recentHealWindow;
        double burstChainTicks;
        double domainTrapTicks;
        double blackFlashChainTicks;
        String lastAction = "None";
        String lastActionTarget = "";
        String lastPrimaryTarget = "";
        int lastActionStreak;

        CombatStats target(String key) {
            return targets.computeIfAbsent(key, unused -> new CombatStats());
        }

        CombatStats type(String key) {
            return types.computeIfAbsent(key, unused -> new CombatStats());
        }

        CombatStats technique(String key) {
            return techniques.computeIfAbsent(key, unused -> new CombatStats());
        }

        CombatStats archetype(String key) {
            return archetypes.computeIfAbsent(key, unused -> new CombatStats());
        }

        void observe(Snapshot s) {
            observeActionValue(s);
            observeStats(s.targetStats, s, 1.0);
            observeStats(s.typeStats, s, 0.35);
            observeStats(s.techniqueStats, s, 0.35);
            observeStats(s.archetypeStats, s, 0.35);
            recentDamageWindow = recentDamageWindow * 0.72 + s.damageTaken;
            recentHealWindow = recentHealWindow * 0.72 + (Math.max(0.0, s.selfHealth - lastHealth));
            boolean burst = s.damageTakenBurst > 0.42 || s.damageTaken > s.sukuna.getMaxHealth() * 0.055;
            burstChainTicks = burst ? Math.min(40.0, burstChainTicks + 1.0) : Math.max(0.0, burstChainTicks - 1.6);
            boolean blackFlashSignal = burst && s.distance < 9.5 && ("BLACK_FLASH_USER".equals(s.archetype) || s.itadoriModulo
                    || s.target.hasEffect(JujutsucraftaddonModMobEffects.BLACK_FLASH_CUT.get())
                    || s.target.hasEffect(JujutsucraftaddonModMobEffects.KOKUSEN_EFFECT.get()));
            blackFlashChainTicks = blackFlashSignal ? Math.min(30.0, blackFlashChainTicks + 1.0) : Math.max(0.0, blackFlashChainTicks - 1.0);
            domainTrapTicks = s.enemyDomainTrap ? Math.min(120.0, domainTrapTicks + 1.0) : Math.max(0.0, domainTrapTicks - 2.0);
            if (s.targetDomain || s.targetCastingDomain || s.domainAssessment.targetDomainThreat > 0.55) {
                if (firstDomainThreatTick < 0.0) {
                    firstDomainThreatTick = s.tick;
                }
            } else {
                firstDomainThreatTick = -1.0;
            }
            if (s.purpleThreat && pendingPurpleResponseTick < 0.0) {
                pendingPurpleResponseTick = s.tick;
            } else if (!s.purpleThreat && pendingPurpleResponseTick >= 0.0 && s.tick - pendingPurpleResponseTick > 45.0) {
                pendingPurpleResponseTick = -1000.0;
            }
            lastHealth = s.selfHealth;
            groupPressure = s.groupPressure;
            megunaHighThreatTicks = s.isMeguna && s.targetThreat > 0.62 && !s.trivialTarget ? megunaHighThreatTicks + 1.0 : Math.max(0.0, megunaHighThreatTicks - 2.0);
            if (s.targetDomain && s.tick - lastSukunaDomainTick < 600.0) {
                s.targetStats.counterDomainAfterSukuna += 1.0;
                s.typeStats.counterDomainAfterSukuna += 0.35;
                s.techniqueStats.counterDomainAfterSukuna += 0.35;
                s.archetypeStats.counterDomainAfterSukuna += 0.35;
            }
            if (s.targetSkill != 0.0) {
                if (lastTargetSkill != s.targetSkill) {
                    lastTargetSkillTick = s.tick;
                }
                lastTargetSkill = s.targetSkill;
            } else if (lastTargetSkill != 0.0 && s.tick - lastTargetSkillTick > 24.0) {
                lastTargetSkill = 0.0;
            }
            lastTargetDistance = s.distance;
        }

        double updateStuck(LivingEntity sukuna, double tick) {
            Vec3 pos = sukuna.position();
            if (!Double.isFinite(lastPosX) || !Double.isFinite(lastPosY) || !Double.isFinite(lastPosZ)) {
                lastPosX = pos.x;
                lastPosY = pos.y;
                lastPosZ = pos.z;
                return 0.0;
            }
            double dx = pos.x - lastPosX;
            double dz = pos.z - lastPosZ;
            double movedSqr = dx * dx + dz * dz;
            boolean recentlyTriedMove = tick - lastMoveTick <= 18.0;
            boolean hasDestination = Double.isFinite(lastDestinationX) && new Vec3(lastDestinationX, lastDestinationY, lastDestinationZ).distanceToSqr(pos) > 2.25;
            boolean blockedMotion = sukuna.horizontalCollision || sukuna.getDeltaMovement().horizontalDistanceSqr() < 0.0025;
            if ((recentlyTriedMove || hasDestination) && movedSqr < 0.018 && blockedMotion) {
                stuckTicks = Math.min(28.0, stuckTicks + 1.0);
            } else {
                stuckTicks = Math.max(0.0, stuckTicks - 2.0);
            }
            lastPosX = pos.x;
            lastPosY = pos.y;
            lastPosZ = pos.z;
            return Mth.clamp(stuckTicks / 12.0, 0.0, 1.0);
        }

        void rememberDestination(Vec3 destination) {
            lastDestinationX = destination.x;
            lastDestinationY = destination.y;
            lastDestinationZ = destination.z;
        }

        double actionBias(String action) {
            double repeatPenalty = action.equals(lastAction) ? Math.min(0.55, lastActionStreak * 0.09) : 0.0;
            return Mth.clamp(actionValues.getOrDefault(action, 0.0) - repeatPenalty, -0.55, 0.35);
        }

        void rememberAction(String action) {
            lastActionStreak = action.equals(lastAction) ? Math.min(20, lastActionStreak + 1) : 1;
        }

        private void observeActionValue(Snapshot s) {
            if ("None".equals(lastAction) || !lastActionTarget.equals(targetKey(s.target))) {
                return;
            }
            double reward = s.damageDealt / Math.max(1.0, s.target.getMaxHealth()) * 3.0
                    - s.damageTaken / Math.max(1.0, s.sukuna.getMaxHealth()) * 2.6
                    - s.targetHeal / Math.max(1.0, s.target.getMaxHealth()) * 1.2
                    + (s.targetCooldown || s.targetUnstable ? 0.08 : 0.0)
                    + (s.targetEscaping && ("CutOffEscape".equals(lastAction) || "StrafePressure".equals(lastAction)) ? 0.08 : 0.0);
            if (s.trivialTarget && isExpensiveAction(lastAction)) {
                reward -= 0.45;
            }
            if ((s.targetCooldown || s.targetUnstable) && lastAction.startsWith("PUNISH")) {
                reward += 0.12;
            }
            reward = Mth.clamp(reward, -0.5, 0.5);
            double old = actionValues.getOrDefault(lastAction, 0.0);
            actionValues.put(lastAction, old + (reward - old) * 0.08);
            observeActionOutcome(s.targetStats, lastAction, reward, 1.0);
            observeActionOutcome(s.typeStats, lastAction, reward, 0.35);
            observeActionOutcome(s.techniqueStats, lastAction, reward, 0.55);
            observeActionOutcome(s.archetypeStats, lastAction, reward, 0.25);
        }

        private static boolean isExpensiveAction(String action) {
            return action.contains("Open") || action.contains("WorldCut") || action.contains("Domain")
                    || "MAHORAGA".equals(action) || "AGITO".equals(action) || "SUMMON".equals(action);
        }

        private void observeStats(CombatStats stats, Snapshot s, double weight) {
            stats.sample("damageTakenAvg", s.damageTaken, 0.18 * weight);
            stats.sample("damageDealtAvg", s.damageDealt, 0.18 * weight);
            stats.sample("targetHealAvg", s.targetHeal, 0.16 * weight);
            stats.sample("distanceAvg", s.distance, 0.08 * weight);
            stats.damageTakenPeak = Math.max(stats.damageTakenPeak, s.damageTaken);
            stats.targetHealPeak = Math.max(stats.targetHealPeak, s.targetHeal);
            stats.seenTicks += weight;
            stats.lastHealth = s.targetHealth;
            if (s.distance < 8.0 || s.predictedDistance < 8.0) stats.meleeTicks += weight;
            if (s.distance > 24.0 || s.predictedDistance > 24.0) stats.rangedTicks += weight;
            if (s.target.position().distanceTo(s.sukuna.position()) > s.distance + 0.4) stats.fleeTicks += weight;
            if (s.targetDomain) stats.domainTicks += weight;
            if ((s.targetDodge || s.targetCounter || s.targetGuard) && s.damageDealt <= s.target.getMaxHealth() * 0.012) stats.evasionTicks += weight;
            if ((s.rangeThreat > 0.45 || s.targetEscaping || s.distance > 22.0) && s.damageTaken > 0.0) stats.rangePressureTicks += weight;
            if (s.stuckLevel > 0.55 || s.pathBlocked || s.enemyDomainTrap) stats.trapTicks += weight;
            if (s.targetSkill > 0.0) stats.addSkillUse(s.targetSkill, weight);
        }

        private void observeActionOutcome(CombatStats stats, String action, double reward, double weight) {
            if (reward > 0.04) {
                stats.actionSuccessTicks += weight;
                stats.actionSuccess.put(action, stats.actionSuccess.getOrDefault(action, 0.0) + weight * Mth.clamp(reward * 2.0, 0.1, 1.0));
            } else if (reward < -0.04) {
                stats.actionFailureTicks += weight;
                stats.actionFailure.put(action, stats.actionFailure.getOrDefault(action, 0.0) + weight * Mth.clamp(-reward * 2.0, 0.1, 1.0));
            }
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("lastHealth", lastHealth);
            tag.putDouble("lastBackstepTick", lastBackstepTick);
            tag.putDouble("lastProjectileDodgeTick", lastProjectileDodgeTick);
            tag.putDouble("lastMoveTick", lastMoveTick);
            tag.putDouble("lastGuardTick", lastGuardTick);
            tag.putDouble("lastGuardTimingTick", lastGuardTimingTick);
            tag.putDouble("lastEvasiveBackstepTick", lastEvasiveBackstepTick);
            tag.putDouble("lastRecoveryWindowTick", lastRecoveryWindowTick);
            tag.putDouble("lastSimpleDomainTick", lastSimpleDomainTick);
            tag.putDouble("lastBurnoutRctTick", lastBurnoutRctTick);
            tag.putDouble("lastDomainAmplificationTick", lastDomainAmplificationTick);
            tag.putDouble("lastSukunaDomainTick", lastSukunaDomainTick);
            tag.putDouble("lastPurpleEvadeTick", lastPurpleEvadeTick);
            tag.putDouble("pendingPurpleResponseTick", pendingPurpleResponseTick);
            tag.putDouble("firstDomainThreatTick", firstDomainThreatTick);
            tag.putDouble("lastTargetSwitchTick", lastTargetSwitchTick);
            tag.putDouble("lastTargetSkill", lastTargetSkill);
            tag.putDouble("lastTargetSkillTick", lastTargetSkillTick);
            tag.putDouble("lastTargetDistance", lastTargetDistance);
            if (Double.isFinite(lastPosX) && Double.isFinite(lastPosY) && Double.isFinite(lastPosZ)) {
                tag.putDouble("lastPosX", lastPosX);
                tag.putDouble("lastPosY", lastPosY);
                tag.putDouble("lastPosZ", lastPosZ);
            }
            tag.putDouble("stuckTicks", stuckTicks);
            if (Double.isFinite(lastDestinationX) && Double.isFinite(lastDestinationY) && Double.isFinite(lastDestinationZ)) {
                tag.putDouble("lastDestinationX", lastDestinationX);
                tag.putDouble("lastDestinationY", lastDestinationY);
                tag.putDouble("lastDestinationZ", lastDestinationZ);
            }
            tag.putDouble("lastEscapeTick", lastEscapeTick);
            tag.putInt("strafeSide", strafeSide);
            tag.putDouble("megunaHighThreatTicks", megunaHighThreatTicks);
            tag.putDouble("groupPressure", groupPressure);
            tag.putDouble("recentDamageWindow", recentDamageWindow);
            tag.putDouble("recentHealWindow", recentHealWindow);
            tag.putDouble("burstChainTicks", burstChainTicks);
            tag.putDouble("domainTrapTicks", domainTrapTicks);
            tag.putDouble("blackFlashChainTicks", blackFlashChainTicks);
            tag.putString("lastAction", lastAction);
            tag.putString("lastActionTarget", lastActionTarget);
            tag.putString("lastPrimaryTarget", lastPrimaryTarget);
            tag.putInt("lastActionStreak", lastActionStreak);
            tag.put("targets", saveMap(targets));
            tag.put("types", saveMap(types));
            tag.put("techniques", saveMap(techniques));
            tag.put("archetypes", saveMap(archetypes));
            tag.put("actionValues", saveDoubleMap(actionValues));
            return tag;
        }

        public static BrainMemory load(CompoundTag tag) {
            BrainMemory memory = new BrainMemory();
            memory.lastHealth = tag.getDouble("lastHealth");
            memory.lastBackstepTick = tag.getDouble("lastBackstepTick");
            memory.lastProjectileDodgeTick = tag.getDouble("lastProjectileDodgeTick");
            memory.lastMoveTick = tag.getDouble("lastMoveTick");
            memory.lastGuardTick = tag.getDouble("lastGuardTick");
            memory.lastGuardTimingTick = tag.contains("lastGuardTimingTick") ? tag.getDouble("lastGuardTimingTick") : -1000.0;
            memory.lastEvasiveBackstepTick = tag.contains("lastEvasiveBackstepTick") ? tag.getDouble("lastEvasiveBackstepTick") : -1000.0;
            memory.lastRecoveryWindowTick = tag.contains("lastRecoveryWindowTick") ? tag.getDouble("lastRecoveryWindowTick") : -1000.0;
            memory.lastSimpleDomainTick = tag.contains("lastSimpleDomainTick") ? tag.getDouble("lastSimpleDomainTick") : -1000.0;
            memory.lastBurnoutRctTick = tag.contains("lastBurnoutRctTick") ? tag.getDouble("lastBurnoutRctTick") : -1000.0;
            memory.lastDomainAmplificationTick = tag.getDouble("lastDomainAmplificationTick");
            memory.lastSukunaDomainTick = tag.getDouble("lastSukunaDomainTick");
            memory.lastPurpleEvadeTick = tag.contains("lastPurpleEvadeTick") ? tag.getDouble("lastPurpleEvadeTick") : -1000.0;
            memory.pendingPurpleResponseTick = tag.contains("pendingPurpleResponseTick") ? tag.getDouble("pendingPurpleResponseTick") : -1000.0;
            memory.firstDomainThreatTick = tag.contains("firstDomainThreatTick") ? tag.getDouble("firstDomainThreatTick") : -1.0;
            memory.lastTargetSwitchTick = tag.contains("lastTargetSwitchTick") ? tag.getDouble("lastTargetSwitchTick") : -1000.0;
            memory.lastTargetSkill = tag.getDouble("lastTargetSkill");
            memory.lastTargetSkillTick = tag.getDouble("lastTargetSkillTick");
            memory.lastTargetDistance = tag.getDouble("lastTargetDistance");
            memory.lastPosX = tag.contains("lastPosX") ? tag.getDouble("lastPosX") : Double.NaN;
            memory.lastPosY = tag.contains("lastPosY") ? tag.getDouble("lastPosY") : Double.NaN;
            memory.lastPosZ = tag.contains("lastPosZ") ? tag.getDouble("lastPosZ") : Double.NaN;
            memory.stuckTicks = tag.getDouble("stuckTicks");
            memory.lastDestinationX = tag.contains("lastDestinationX") ? tag.getDouble("lastDestinationX") : Double.NaN;
            memory.lastDestinationY = tag.contains("lastDestinationY") ? tag.getDouble("lastDestinationY") : Double.NaN;
            memory.lastDestinationZ = tag.contains("lastDestinationZ") ? tag.getDouble("lastDestinationZ") : Double.NaN;
            memory.lastEscapeTick = tag.contains("lastEscapeTick") ? tag.getDouble("lastEscapeTick") : -1000.0;
            memory.strafeSide = tag.getInt("strafeSide");
            memory.megunaHighThreatTicks = tag.getDouble("megunaHighThreatTicks");
            memory.groupPressure = tag.getDouble("groupPressure");
            memory.recentDamageWindow = tag.getDouble("recentDamageWindow");
            memory.recentHealWindow = tag.getDouble("recentHealWindow");
            memory.burstChainTicks = tag.getDouble("burstChainTicks");
            memory.domainTrapTicks = tag.getDouble("domainTrapTicks");
            memory.blackFlashChainTicks = tag.getDouble("blackFlashChainTicks");
            memory.lastAction = tag.getString("lastAction");
            memory.lastActionTarget = tag.getString("lastActionTarget");
            memory.lastPrimaryTarget = tag.getString("lastPrimaryTarget");
            memory.lastActionStreak = tag.getInt("lastActionStreak");
            loadMap(tag.getCompound("targets"), memory.targets);
            loadMap(tag.getCompound("types"), memory.types);
            loadMap(tag.getCompound("techniques"), memory.techniques);
            loadMap(tag.getCompound("archetypes"), memory.archetypes);
            loadDoubleMap(tag.getCompound("actionValues"), memory.actionValues);
            return memory;
        }

        private static CompoundTag saveMap(Map<String, CombatStats> map) {
            CompoundTag tag = new CompoundTag();
            for (Map.Entry<String, CombatStats> entry : map.entrySet()) {
                tag.put(entry.getKey(), entry.getValue().save());
            }
            return tag;
        }

        private static void loadMap(CompoundTag tag, Map<String, CombatStats> map) {
            for (String key : tag.getAllKeys()) {
                map.put(key, CombatStats.load(tag.getCompound(key)));
            }
        }

        private static CompoundTag saveDoubleMap(Map<String, Double> map) {
            CompoundTag tag = new CompoundTag();
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                tag.putDouble(entry.getKey(), entry.getValue());
            }
            return tag;
        }

        private static void loadDoubleMap(CompoundTag tag, Map<String, Double> map) {
            for (String key : tag.getAllKeys()) {
                map.put(key, tag.getDouble(key));
            }
        }
    }

    private static class CombatStats {
        double lastHealth;
        double damageTakenAvg;
        double damageDealtAvg;
        double targetHealAvg;
        double distanceAvg;
        double damageTakenPeak;
        double targetHealPeak;
        double seenTicks;
        double meleeTicks;
        double rangedTicks;
        double fleeTicks;
        double domainTicks;
        double skillUses;
        double counterDomainAfterSukuna;
        double evasionTicks;
        double rangePressureTicks;
        double trapTicks;
        double actionSuccessTicks;
        double actionFailureTicks;
        final Map<Long, Double> skills = new HashMap<>();
        final Map<String, Double> actionSuccess = new HashMap<>();
        final Map<String, Double> actionFailure = new HashMap<>();

        void sample(String key, double value, double alpha) {
            if (!Double.isFinite(value)) return;
            if ("damageTakenAvg".equals(key)) damageTakenAvg = sampleValue(damageTakenAvg, value, alpha);
            else if ("damageDealtAvg".equals(key)) damageDealtAvg = sampleValue(damageDealtAvg, value, alpha);
            else if ("targetHealAvg".equals(key)) targetHealAvg = sampleValue(targetHealAvg, value, alpha);
            else if ("distanceAvg".equals(key)) distanceAvg = sampleValue(distanceAvg, value, alpha);
        }

        double meleeRatio() {
            return meleeTicks / Math.max(1.0, seenTicks);
        }

        double rangedRatio() {
            return rangedTicks / Math.max(1.0, seenTicks);
        }

        void addSkillUse(double skill, double amount) {
            skillUses += amount;
            long key = Math.round(skill);
            skills.put(key, skills.getOrDefault(key, 0.0) + amount);
        }

        double skillUseRatio(double skill) {
            return skills.getOrDefault(Math.round(skill), 0.0) / Math.max(1.0, skillUses);
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("lastHealth", lastHealth);
            tag.putDouble("damageTakenAvg", damageTakenAvg);
            tag.putDouble("damageDealtAvg", damageDealtAvg);
            tag.putDouble("targetHealAvg", targetHealAvg);
            tag.putDouble("distanceAvg", distanceAvg);
            tag.putDouble("damageTakenPeak", damageTakenPeak);
            tag.putDouble("targetHealPeak", targetHealPeak);
            tag.putDouble("seenTicks", seenTicks);
            tag.putDouble("meleeTicks", meleeTicks);
            tag.putDouble("rangedTicks", rangedTicks);
            tag.putDouble("fleeTicks", fleeTicks);
            tag.putDouble("domainTicks", domainTicks);
            tag.putDouble("skillUses", skillUses);
            tag.putDouble("counterDomainAfterSukuna", counterDomainAfterSukuna);
            tag.putDouble("evasionTicks", evasionTicks);
            tag.putDouble("rangePressureTicks", rangePressureTicks);
            tag.putDouble("trapTicks", trapTicks);
            tag.putDouble("actionSuccessTicks", actionSuccessTicks);
            tag.putDouble("actionFailureTicks", actionFailureTicks);
            CompoundTag skillTag = new CompoundTag();
            for (Map.Entry<Long, Double> entry : skills.entrySet()) {
                skillTag.putDouble(Long.toString(entry.getKey()), entry.getValue());
            }
            tag.put("skills", skillTag);
            tag.put("actionSuccess", BrainMemory.saveDoubleMap(actionSuccess));
            tag.put("actionFailure", BrainMemory.saveDoubleMap(actionFailure));
            return tag;
        }

        static CombatStats load(CompoundTag tag) {
            CombatStats stats = new CombatStats();
            stats.lastHealth = tag.getDouble("lastHealth");
            stats.damageTakenAvg = tag.getDouble("damageTakenAvg");
            stats.damageDealtAvg = tag.getDouble("damageDealtAvg");
            stats.targetHealAvg = tag.getDouble("targetHealAvg");
            stats.distanceAvg = tag.getDouble("distanceAvg");
            stats.damageTakenPeak = tag.getDouble("damageTakenPeak");
            stats.targetHealPeak = tag.getDouble("targetHealPeak");
            stats.seenTicks = tag.getDouble("seenTicks");
            stats.meleeTicks = tag.getDouble("meleeTicks");
            stats.rangedTicks = tag.getDouble("rangedTicks");
            stats.fleeTicks = tag.getDouble("fleeTicks");
            stats.domainTicks = tag.getDouble("domainTicks");
            stats.skillUses = tag.getDouble("skillUses");
            stats.counterDomainAfterSukuna = tag.getDouble("counterDomainAfterSukuna");
            stats.evasionTicks = tag.getDouble("evasionTicks");
            stats.rangePressureTicks = tag.getDouble("rangePressureTicks");
            stats.trapTicks = tag.getDouble("trapTicks");
            stats.actionSuccessTicks = tag.getDouble("actionSuccessTicks");
            stats.actionFailureTicks = tag.getDouble("actionFailureTicks");
            CompoundTag skillTag = tag.getCompound("skills");
            for (String key : skillTag.getAllKeys()) {
                try {
                    stats.skills.put(Long.parseLong(key), skillTag.getDouble(key));
                } catch (NumberFormatException ignored) {
                }
            }
            BrainMemory.loadDoubleMap(tag.getCompound("actionSuccess"), stats.actionSuccess);
            BrainMemory.loadDoubleMap(tag.getCompound("actionFailure"), stats.actionFailure);
            return stats;
        }

        private static double sampleValue(double old, double value, double alpha) {
            return old == 0.0 ? value : old + (value - old) * alpha;
        }
    }
}
