package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import com.jujutsu.jujutsucraftaddon.procedures.*;

import com.jujutsu.jujutsucraftaddon.entity.SatushiEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModEntities;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.util.DomainMasterySystem;
import com.jujutsu.jujutsucraftaddon.util.OpSukunaBrainMemoryHolder;
import com.jujutsu.jujutsucraftaddon.util.OpSukunaBrainTelemetry;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.entity.GojoSatoruEntity;
import net.mcreator.jujutsucraft.entity.SukunaFushiguroEntity;
import net.mcreator.jujutsucraft.entity.SukunaPerfectEntity;
import net.mcreator.jujutsucraft.entity.JujutsuSorcererEntity;
import net.mcreator.jujutsucraft.entity.CurseUserEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.CalculateAttackProcedure;
import net.mcreator.jujutsucraft.procedures.AntiInfinityProcedure;
import net.mcreator.jujutsucraft.procedures.DetectEnemyProjectileProcedure;
import net.mcreator.jujutsucraft.procedures.GetDistanceProcedure;
import net.mcreator.jujutsucraft.procedures.GetReachProcedure;
import net.mcreator.jujutsucraft.procedures.InsideSolidCalculateProcedure;
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

class OpSukunaEngineCore {
    static final String SAVE_KEY = "JJKUR_OP_AI_RAM";
    static final double OPENING_ENGAGE_WINDOW_TICKS = 120.0;
    private static final int SKILL_STALL_MAX_TICKS = 48;
    private static final double SKILL_STALL_MIN_DAMAGE_RATIO = 0.015;
    private static final double SKILL_STALL_MAX_DAMAGE_TAKEN_RATIO = 0.05;
    private static final int SKILL_STALL_MAX_RESETS_PER_FIGHT = 3;
    static final double FAST_DISMANTLE = 105.0;
    static final double CLEAVE = 106.0;
    static final double OPEN = 107.0;
    static final double DOMAIN = 120.0;
    static final double TEN_SHADOWS_UTILITY = 612.0;
    static final double AGITO = 617.0;
    static final double MAHORAGA = 618.0;
    static final double TEN_SHADOWS_DOMAIN = 620.0;
    static final double PASSIVE_FAST = 111.0;
    static final double PASSIVE_CLOSE = 112.0;
    static final double PASSIVE_ANTI_RANGE = 113.0;
    static final double PERFECT_WORLD_CUT_COOLDOWN = 400.0;

    OpSukunaEngineCore() {
    }

    public static boolean tryExecute(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, LivingEntity target, CompoundTag nbt) {
        if (!isSupportedSukuna(sukuna)) {
            return false;
        }

        OpSukunaBrainMemory memory = memory(sukuna);
        double preTargetSkill = nbt.getDouble("skill");
        if (isSuspiciousSkillId(preTargetSkill)) {
            clearActiveSkill(nbt, memory);
            OpSukunaBrainTelemetry.recordAiGate(world, sukuna, target, "try_execute_gate", "unknown_skill_precleared_" + (long) preTargetSkill);
        }
        target = OpSukunaTargetIntel.chooseFocusTarget(world, sukuna, target, memory);
        if (target == null || !target.isAlive()) {
            OpSukunaBrainTelemetry.recordAiGate(world, sukuna, target, "try_execute_gate", "invalid_target");
            memory.lastHealth = sukuna.getHealth();
            return false;
        }
        if (nbt.getDouble("cnt_target") <= 6.0) {
            if (OpSukunaBrainTelemetry.hasBenchmarkContext(sukuna) && target == ((sukuna instanceof Mob mob) ? mob.getTarget() : null)) {
                nbt.putDouble("cnt_target", 7.0);
                OpSukunaBrainTelemetry.recordAiGate(world, sukuna, target, "try_execute_gate", "cnt_target_bootstrap");
            } else {
                OpSukunaBrainTelemetry.recordAiGate(world, sukuna, target, "try_execute_gate", "cnt_target_low");
                memory.lastHealth = sukuna.getHealth();
                return false;
            }
        }
        if (nbt.getDouble("cnt_target") <= 6.0) {
            memory.lastHealth = sukuna.getHealth();
            return false;
        }

        if (memory.lastTargetSwitchTick < -500.0) {
            memory.lastTargetSwitchTick = sukuna.tickCount;
            memory.lastPrimaryTarget = targetKey(target);
        }

        OpSukunaSnapshot s = OpSukunaSnapshot.capture(world, x, y, z, sukuna, target, nbt, memory);
        nbt.putBoolean("JJKUR_OP_AI_CONTROLLED", true);
        memory.observe(s);
        maintainKeyedDefenses(s);
        maintainFugaCharge(s);
        tryEmergencyReaction(s);

        if (nbt.getDouble("skill") != 0.0) {
            double activeSkill = nbt.getDouble("skill");
            if (isSuspiciousSkillId(activeSkill)) {
                clearActiveSkill(nbt, memory);
                OpSukunaBrainTelemetry.recordAiGate(world, sukuna, target, "try_execute_gate", "unknown_skill_cleared_" + (long) activeSkill);
            } else {
                if (memory.skillActiveSinceTick < 0.0) {
                    memory.skillActiveSinceTick = s.tick;
                    memory.skillActiveStartTargetHealth = s.targetHealth;
                    memory.skillActiveStartSukunaHealth = s.selfHealth;
                }
                double stallTicks = s.tick - memory.skillActiveSinceTick;
                double hpDropped = memory.skillActiveStartTargetHealth - s.targetHealth;
                double selfHpDropped = memory.skillActiveStartSukunaHealth - s.selfHealth;
                double targetHpThreshold = target.getMaxHealth() * SKILL_STALL_MIN_DAMAGE_RATIO;
                double selfDamageThreshold = sukuna.getMaxHealth() * SKILL_STALL_MAX_DAMAGE_TAKEN_RATIO;
                double stallBudget = activeSkill == OPEN && s.itadoriModulo && !s.selfDomain ? 24.0 : SKILL_STALL_MAX_TICKS;
                boolean skillTrulyIdle = hpDropped < targetHpThreshold && selfHpDropped <= selfDamageThreshold;
                boolean withinResetBudget = memory.skillStallResets < SKILL_STALL_MAX_RESETS_PER_FIGHT;
                if (stallTicks > stallBudget && skillTrulyIdle && withinResetBudget) {
                    nbt.putDouble("skill", 0.0);
                    memory.skillActiveSinceTick = -1.0;
                    memory.skillActiveStartTargetHealth = 0.0;
                    memory.skillActiveStartSukunaHealth = 0.0;
                    memory.skillStallResets = Math.min(SKILL_STALL_MAX_RESETS_PER_FIGHT, memory.skillStallResets + 1);
                    ResetCounterProcedure.execute(sukuna);
                    OpSukunaBrainTelemetry.recordAiGate(world, sukuna, target, "try_execute_gate", "skill_stall_reset");
                } else {
                    OpSukunaTelemetryMapper.recordExecutionState(world, sukuna, target, s, "busy_skill");
                    OpSukunaBrainTelemetry.recordAiGate(world, sukuna, target, "try_execute_gate", "busy_skill");
                    return true;
                }
            }
        } else {
            memory.skillActiveSinceTick = -1.0;
            memory.skillActiveStartSukunaHealth = 0.0;
        }

        ResetCounterProcedure.execute(sukuna);
        if (s.selfDomain && s.distance < 48.0 && !sukuna.level().isClientSide()) {
            sukuna.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20, 0, false, false));
        }

        OpSukunaAction best = OpSukunaActionScorer.chooseBestAction(s);
        String topScores = OpSukunaBrainTelemetry.enabled(world) ? describeTopActions(s, best) : "";
        boolean started = best.execute(world, x, y, z, sukuna, nbt, s);
        if (!started) {
            String blocked = best.blockReason(s);
            OpSukunaAction fallback = chooseFallbackAction(s, best);
            s.actionStarted = fallback.execute(world, x, y, z, sukuna, nbt, s);
            s.blockReason = blocked;
            s.fallbackAction = fallback.name;
            best = fallback;
        } else {
            s.actionStarted = true;
            s.blockReason = "ready";
            s.fallbackAction = "";
        }
        OpSukunaTelemetryMapper.recordDecision(world, sukuna, target, s, best, topScores);
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
            holder.jjkur$setOpSukunaBrainMemory(OpSukunaBrainMemory.load(tag));
        }
    }

    public static String saveKey() {
        return SAVE_KEY;
    }

    static OpSukunaBrainMemory memory(LivingEntity sukuna) {
        if (sukuna instanceof OpSukunaBrainMemoryHolder holder) {
            return holder.jjkur$getOpSukunaBrainMemory();
        }
        return new OpSukunaBrainMemory();
    }

    static boolean isSupportedSukuna(LivingEntity entity) {
        return entity instanceof SukunaPerfectEntity
                || entity instanceof SukunaFushiguroEntity
                || entity instanceof com.jujutsu.jujutsucraftaddon.entity.SukunaFushiguroEntity
                || entity instanceof com.jujutsu.jujutsucraftaddon.entity.SukunaMangaEntity;
    }

    static boolean hasWorldCut(LivingEntity entity) {
        return entity instanceof SukunaPerfectEntity
                || (entity instanceof SukunaFushiguroEntity sf && sf.getEntityData().get(SukunaFushiguroEntity.DATA_world_cut));
    }

    static boolean isFushiguroBody(LivingEntity entity) {
        return entity instanceof SukunaFushiguroEntity
                || entity instanceof com.jujutsu.jujutsucraftaddon.entity.SukunaFushiguroEntity;
    }

    static boolean isMegunaBody(LivingEntity entity) {
        return isFushiguroBody(entity) && !isPerfectMode(entity);
    }

    static boolean canTransformHeian(LivingEntity entity) {
        return entity instanceof SukunaFushiguroEntity sf && !sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode);
    }

    static boolean isPerfectMode(LivingEntity entity) {
        return entity instanceof SukunaFushiguroEntity sf && sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode);
    }

    static String sukunaForm(LivingEntity entity) {
        if (entity instanceof SukunaPerfectEntity) return "SUKUNA_PERFECT";
        if (entity instanceof SukunaFushiguroEntity sf) {
            return sf.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode) ? "FUSHIGURO_PERFECT_MODE" : "MEGUNA";
        }
        if (entity instanceof com.jujutsu.jujutsucraftaddon.entity.SukunaFushiguroEntity) return "ADDON_FUSHIGURO";
        if (entity instanceof com.jujutsu.jujutsucraftaddon.entity.SukunaMangaEntity) return "SUKUNA_MANGA";
        return "SUKUNA";
    }

    static double rctLimit(LivingEntity sukuna) {
        return sukuna.getMaxHealth() >= 800.0F ? 400.0 : 200.0;
    }

    static double rctLevel(LivingEntity sukuna) {
        return sukuna.getCapability(com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .map(vars -> vars.RCTLimitLevel > 0.0 ? Math.min(Math.round(vars.RCTCount / 5000.0), vars.RCTLimitLevel) : Math.round(vars.RCTCount / 5000.0))
                .orElse(0.0);
    }

    static double brainDamageLevel(LivingEntity sukuna) {
        double effect = sukuna.hasEffect(JujutsucraftModMobEffects.BRAIN_DAMAGE.get()) ? sukuna.getEffect(JujutsucraftModMobEffects.BRAIN_DAMAGE.get()).getAmplifier() + 1.0 : 0.0;
        double capability = sukuna.getCapability(com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .map(vars -> vars.BrainDamage)
                .orElse(0.0);
        return Math.max(effect, capability);
    }

    static void switchTarget(LivingEntity sukuna, LivingEntity target, OpSukunaBrainMemory memory) {
        if (sukuna instanceof Mob mob) {
            mob.setTarget(target);
        }
        memory.lastPrimaryTarget = targetKey(target);
        memory.lastTargetSwitchTick = sukuna.tickCount;
    }

    // IDs >= 4000 or == -1000 are never set by OpSukuna — they come from base AI and must be cleared immediately.
    static boolean isSuspiciousSkillId(double skill) {
        return skill >= 4000.0 || skill == -1000.0;
    }

    static void clearActiveSkill(CompoundTag nbt, OpSukunaBrainMemory memory) {
        nbt.putDouble("skill", 0.0);
        nbt.putBoolean("attack", false);
        memory.skillActiveSinceTick = -1.0;
        memory.skillActiveStartTargetHealth = 0.0;
        memory.skillActiveStartSukunaHealth = 0.0;
    }

    private static final java.util.Set<String> PROJECTILE_ENTITY_SUFFIXES = java.util.Set.of(
            "projectile_slash", "blood_ball", "rock_fragment", "slash", "projectile",
            "arrow", "crow", "bullet", "needle", "bird", "piercing_blood", "crimson_binding"
    );

    static boolean isValidEnemy(LevelAccessor world, LivingEntity sukuna, LivingEntity candidate) {
        if (!(candidate instanceof Mob) && !(candidate instanceof Player)) return false;
        if (candidate == sukuna || !candidate.isAlive() || candidate.isSpectator()) {
            return false;
        }
        ResourceLocation candidateType = ForgeRegistries.ENTITY_TYPES.getKey(candidate.getType());
        if (candidateType != null) {
            String path = candidateType.getPath();
            for (String suffix : PROJECTILE_ENTITY_SUFFIXES) {
                if (path.contains(suffix)) return false;
            }
        }
        if (candidate instanceof Player player && player.getAbilities().instabuild) {
            return false;
        }
        if (isAlly(sukuna, candidate)) {
            return false;
        }
        return LogicAttackProcedure.execute(world, sukuna, candidate);
    }

    static boolean isAlly(LivingEntity sukuna, LivingEntity candidate) {
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

    static boolean hasEntityTypeTag(Entity entity, String tag) {
        return entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(tag)));
    }

    static boolean isSatushi(LivingEntity target) {
        return target instanceof SatushiEntity;
    }

    static boolean isGojoTarget(LivingEntity target, OpSukunaPlayerRead read) {
        if (target instanceof GojoSatoruEntity || isSatushi(target)) {
            return true;
        }
        if (read != null && (read.primaryTechnique == TechniqueIDs.GOJO || read.secondaryTechnique == TechniqueIDs.GOJO)) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        String type = id == null ? target.getType().toString().toLowerCase() : id.toString().toLowerCase();
        String name = target.getName().getString().toLowerCase();
        return type.contains("gojo") || type.contains("satoru") || name.contains("gojo") || name.contains("satoru")
                || hasEntityTypeTag(target, "jujutsucraft:gojo")
                || hasEntityTypeTag(target, "jujutsucraftaddon:gojo");
    }

    static double hitboxDistance(LivingEntity self, LivingEntity target) {
        AABB a = self.getBoundingBox();
        AABB b = target.getBoundingBox();
        double dx = Math.max(0.0, Math.max(a.minX - b.maxX, b.minX - a.maxX));
        double dy = Math.max(0.0, Math.max(a.minY - b.maxY, b.minY - a.maxY));
        double dz = Math.max(0.0, Math.max(a.minZ - b.maxZ, b.minZ - a.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static boolean hasClearShot(LevelAccessor world, LivingEntity attacker, LivingEntity target, double lenience) {
        if (!(world instanceof Level level)) {
            return true;
        }
        Vec3 start = attacker.getEyePosition(1.0F);
        Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
        HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(end) <= lenience * lenience;
    }

    static boolean hasClearMovementSegment(LevelAccessor world, LivingEntity entity, Vec3 destination, double lenience) {
        if (!(world instanceof Level level)) {
            return true;
        }
        Vec3 start = entity.position().add(0.0, Math.min(1.2, entity.getBbHeight() * 0.45), 0.0);
        Vec3 end = destination.add(0.0, Math.min(1.2, entity.getBbHeight() * 0.45), 0.0);
        HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(end) <= lenience * lenience;
    }

    static boolean pathBlocked(LevelAccessor world, LivingEntity entity, Vec3 destination) {
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

    static boolean isWalkableDestination(LevelAccessor world, LivingEntity entity, Vec3 destination) {
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

    static boolean canUseRct(LivingEntity sukuna) {
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

    static boolean canUseSimpleDomain(LivingEntity sukuna) {
        if (sukuna.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())
                || sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_SIMPLE_DOMAIN.get())
                || sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
            return false;
        }
        if (hasSimpleDomainDefense(sukuna)) {
            return false;
        }
        return hasEntityTypeTag(sukuna, "jujutsucraft:can_use_simple_domain")
                || hasEntityTypeTag(sukuna, "jujutsucraft:can_use_hollow_wicker_basket");
    }

    static boolean canUseDomainAmplification(LivingEntity sukuna) {
        return !sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get())
                && !sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())
                && hasEntityTypeTag(sukuna, "jujutsucraft:can_use_domain_amplification");
    }

    static boolean canUseBurnoutRct(LivingEntity sukuna) {
        if (!(sukuna instanceof Player)) {
            return false;
        }
        if (!(sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get()) || sukuna.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get()))
                || sukuna.hasEffect(JujutsucraftaddonModMobEffects.BROKEN_BRAIN.get())
                || sukuna.getPersistentData().getDouble("skill") != 0.0) {
            return false;
        }
        double brainDamage = sukuna.getCapability(com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .orElse(new com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables.PlayerVariables()).BrainDamage;
        return brainDamage < 5.0 && sukuna.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get());
    }

    static int effectAmplifier(LivingEntity entity, MobEffect effect) {
        MobEffectInstance instance = entity.getEffect(effect);
        return instance == null ? -1 : instance.getAmplifier();
    }

    static int effectDuration(LivingEntity entity, MobEffect effect) {
        MobEffectInstance instance = entity.getEffect(effect);
        return instance == null ? 0 : instance.getDuration();
    }

    static double domainNumber(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        double selected = data.getDouble("select");
        double skillDomain = data.getDouble("skill_domain");
        if (skillDomain > 0.0) return skillDomain;
        return selected > 0.0 ? selected : 0.0;
    }

    static boolean isDomainCastSignal(LivingEntity target, double targetSkill, double targetDomainNumber, OpSukunaPlayerRead read) {
        int roundedSkill = (int) Math.round(Math.abs(targetSkill));
        boolean domainSkill = targetSkill == 20.0 || targetSkill == 220.0 || roundedSkill % 100 == 20;
        boolean gojoStartup = isGojoTarget(target, read)
                && target.getPersistentData().getDouble("cnt1") > 0.0
                && target.getPersistentData().getDouble("cnt1") < 55.0;
        return domainSkill || targetDomainNumber == 2.0 || gojoStartup
                || target.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get())
                || target.hasEffect(JujutsucraftModMobEffects.BRAIN_DAMAGE.get());
    }

    static boolean hasDomainEscapeCandidate(OpSukunaSnapshot s) {
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

    static boolean canUseEscapeCandidate(OpSukunaSnapshot s, Vec3 candidate) {
        if (!isWalkableDestination(s.world, s.sukuna, candidate) || !hasClearMovementSegment(s.world, s.sukuna, candidate, 1.2)) {
            return false;
        }
        return candidate.distanceToSqr(s.target.position()) > s.sukuna.position().distanceToSqr(s.target.position()) + 18.0;
    }

    static boolean hasExtremeVoidDebuff(LivingEntity entity) {
        return effectAmplifier(entity, MobEffects.MOVEMENT_SLOWDOWN) >= 4
                || effectAmplifier(entity, MobEffects.BLINDNESS) >= 0
                || effectAmplifier(entity, MobEffects.WEAKNESS) >= 3;
    }

    static boolean hasSimpleDomainDefense(LivingEntity entity) {
        return entity.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get())
                || entity.hasEffect(JujutsucraftaddonModMobEffects.SIMPLE_DOMAIN_MAX.get())
                || entity.hasEffect(JujutsucraftaddonModMobEffects.HWB.get());
    }

    static int simpleDomainDuration(LivingEntity entity) {
        return Math.max(effectDuration(entity, JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()),
                Math.max(effectDuration(entity, JujutsucraftaddonModMobEffects.SIMPLE_DOMAIN_MAX.get()),
                        effectDuration(entity, JujutsucraftaddonModMobEffects.HWB.get())));
    }

    static OpSukunaAction chooseBestAction(OpSukunaSnapshot s) {
        return OpSukunaDecisionPipeline.chooseBestAction(s);
    }

    static List<OpSukunaAction> baselineActions(OpSukunaSnapshot s) {
        return OpSukunaActionCatalog.baselineActions(s);
    }

    static List<OpSukunaAction> domainResponseActions(OpSukunaSnapshot s) {
        List<OpSukunaAction> actions = new ArrayList<>();
        actions.addAll(OpSukunaDomainIntel.domainResponseActions(s));
        return actions;
    }

    static List<OpSukunaAction> criticalDomainReserveActions(OpSukunaSnapshot s) {
        return OpSukunaActionCatalog.criticalDomainReserveActions(s);
    }

    static List<OpSukunaAction> offensiveActions(OpSukunaSnapshot s, String priorityName) {
        return OpSukunaActionCatalog.offensiveActions(s, priorityName);
    }

    static List<OpSukunaAction> noImpactBreakerActions(OpSukunaSnapshot s, String priorityName) {
        return OpSukunaActionCatalog.noImpactBreakerActions(s, priorityName);
    }

    static List<OpSukunaAction> openingEngageActions(OpSukunaSnapshot s) {
        return OpSukunaActionCatalog.openingEngageActions(s);
    }

    static List<OpSukunaAction> shrineOffenseActions(OpSukunaSnapshot s, String priorityName) {
        return OpSukunaActionCatalog.shrineOffenseActions(s, priorityName);
    }

    static OpSukunaAction bestOf(OpSukunaSnapshot s, List<OpSukunaAction> actions) {
        return OpSukunaActionScorer.bestOf(s, actions);
    }

    static OpSukunaAction chooseFallbackAction(OpSukunaSnapshot s, OpSukunaAction blocked) {
        OpSukunaAction fallback = bestOf(s, List.of(
                OpSukunaAction.domain(scoreDomain(s) + (s.catastrophicDomain ? 0.55 : 0.0)),
                OpSukunaAction.simpleDomain(scoreSimpleDomain(s) + (s.voidExposure > 0.45 ? 0.45 : 0.0)),
                OpSukunaAction.domainAmplification(scoreDomainAmplification(s) + (s.infinitySignal > 0.0 || s.voidExposure > 0.45 ? 0.35 : 0.0)),
                OpSukunaAction.rct("FALLBACK_RCT", scoreRct(s)),
                OpSukunaAction.guardTiming(scoreGuardTiming(s) + 0.15),
                OpSukunaAction.backstep("FALLBACK_BACKSTEP", scoreEvasiveBackstep(s) + 0.15),
                OpSukunaAction.move("FALLBACK_STRAFE", OpSukunaMovement.STRAFE_PRESSURE, scoreStrafePressure(s) + 0.05),
                OpSukunaAction.calculate("FALLBACK_CALCULATE", scoreBasic(s))));
        return fallback == blocked || fallback.score < -0.5 ? OpSukunaAction.calculate("FALLBACK_CALCULATE", scoreBasic(s)) : fallback;
    }

    static double adjustedScore(OpSukunaSnapshot s, OpSukunaAction action) {
        return OpSukunaScorePolicy.adjustedScore(s, action);
    }

    static boolean shouldPreferDiverseTie(OpSukunaSnapshot s, OpSukunaAction best, OpSukunaAction candidate) {
        return OpSukunaScorePolicy.shouldPreferDiverseTie(s, best, candidate);
    }

    static boolean shouldForceOffensiveTempo(OpSukunaSnapshot s) {
        if (s.trivialTarget || s.lethalForecast || s.survivalMode || hasConcreteDomainSignal(s) || s.purpleThreat) {
            return false;
        }
        if (s.infinitySignal > 0.0 && !s.canBypassInfinityNow) {
            return false;
        }
        if (!s.normalReady && !(s.passiveReady && !s.combatCooldown)) {
            return false;
        }
        boolean inContact = s.hitboxDistance <= Math.max(2.75, s.selfReach + 0.75);
        boolean wastingMovement = s.memory.rangeActionStreak >= 2 || s.memory.noImpactActionStreak >= 2;
        boolean punishWindow = s.targetCooldown || s.targetUnstable || s.targetWhiffed || s.targetOverextended || s.targetHealthRatio < 0.45;
        boolean cleanPressure = s.hitConfidence > 0.42 && s.immediateThreat < 0.9 && s.selfHealthRatio > 0.32;
        boolean itadoriTempo = s.itadoriModulo && s.distance <= 18.0 && s.immediateThreat < 0.85 && s.selfHealthRatio > 0.38
                && (s.normalReady || s.passiveReady && !s.combatCooldown)
                && (s.memory.noImpactActionStreak >= 1 || s.targetHealthRatio > 0.38);
        return inContact || wastingMovement && cleanPressure || punishWindow && cleanPressure || itadoriTempo;
    }

    static boolean shouldForceOpeningEngage(OpSukunaSnapshot s) {
        if (s.trivialTarget || s.lethalForecast || s.survivalMode || hasConcreteDomainSignal(s) || s.purpleThreat) {
            return false;
        }
        if (s.infinitySignal > 0.0 && !s.canBypassInfinityNow) {
            return false;
        }
        if (!s.normalReady && !(s.passiveReady && !s.combatCooldown)) {
            return false;
        }
        if (s.selfHealthRatio < 0.33 || s.immediateThreat > 1.05) {
            return false;
        }
        if (s.memory.lastTargetSwitchTick < -500.0) {
            return false;
        }
        boolean openingWindow = s.tick - s.memory.lastTargetSwitchTick <= OPENING_ENGAGE_WINDOW_TICKS;
        boolean slowOpen = s.memory.noImpactActionStreak >= 1 || s.damageDealt <= s.target.getMaxHealth() * 0.02;
        return openingWindow && (s.distance > 7.0 || slowOpen);
    }

    static boolean shouldForcePressureChase(OpSukunaSnapshot s) {
        if (!s.itadoriModulo || s.trivialTarget || s.lethalForecast || s.survivalMode || hasConcreteDomainSignal(s) || s.purpleThreat) {
            return false;
        }
        if (s.infinitySignal > 0.0 && !s.canBypassInfinityNow) {
            return false;
        }
        if (s.selfHealthRatio < 0.34 && s.immediateThreat > 0.55) {
            return false;
        }
        if (s.memory.noImpactActionStreak >= 2 && s.damageDealt <= s.target.getMaxHealth() * 0.02 && s.distance <= 24.0) {
            return false;
        }
        if (s.memory.noImpactActionStreak >= 5 && s.distance <= 18.0) {
            return false;
        }
        boolean tooFar = s.distance > 14.0;
        boolean noImpact = s.memory.noImpactActionStreak >= 2;
        boolean lostTempo = s.targetHealthRatio > 0.45 && s.damageDealt <= s.target.getMaxHealth() * 0.01 && s.damageTakenBurst < 0.22;
        return (tooFar || noImpact) && lostTempo;
    }

    static boolean canConfirmRawMelee(OpSukunaSnapshot s) {
        return s.hitboxDistance <= rawMeleeConfirmReach(s);
    }

    static boolean canProbeRawMelee(OpSukunaSnapshot s) {
        if (!s.itadoriModulo || s.selfHealthRatio < 0.34 || s.immediateThreat > 0.95) {
            return false;
        }
        if (s.memory.noImpactActionStreak < 2 && !s.targetCooldown && !s.targetUnstable && !s.targetWhiffed) {
            return false;
        }
        return s.hitboxDistance <= rawMeleeConfirmReach(s) + 1.25;
    }

    static double rawMeleeConfirmReach(OpSukunaSnapshot s) {
        return Math.max(2.75, s.selfReach + (s.itadoriModulo ? 0.65 : 0.45));
    }

    static boolean isRawMeleeAction(OpSukunaAction action) {
        return action.kind == OpSukunaActionKind.MELEE && action.skill == 0.0;
    }

    static boolean shouldBreakNoImpactLoop(OpSukunaSnapshot s) {
        if (s.trivialTarget || s.lethalForecast || s.survivalMode || hasConcreteDomainSignal(s) || s.purpleThreat) {
            return false;
        }
        if (s.infinitySignal > 0.0 && !s.canBypassInfinityNow) {
            return false;
        }
        if (s.memory.noImpactActionStreak < 4 && s.memory.rangeActionStreak < 4) {
            return false;
        }
        boolean contact = s.hitboxDistance <= Math.max(2.75, s.selfReach + 0.75);
        if (!s.normalReady && !s.worldCutReady && !contact) {
            return false;
        }
        return s.itadoriModulo || s.damageDealt <= s.target.getMaxHealth() * 0.02;
    }

    static String describeTopActions(OpSukunaSnapshot s, OpSukunaAction selected) {
        List<OpSukunaAction> actions = baselineActions(s);
        if (s.purpleThreat) {
            actions.add(OpSukunaAction.move("PURPLE_EVADE", OpSukunaMovement.PURPLE_EVADE, scorePurpleEvade(s)));
            actions.add(OpSukunaAction.worldCut("PURPLE_INTERRUPT", scoreWorldCut(s) + (s.purpleWindup ? 0.8 : 0.15)));
        }
        if (s.targetDomain || s.targetCastingDomain || s.domainAssessment.targetDomainThreat > 0.55) {
            actions.add(OpSukunaAction.move("DomainEscape", OpSukunaMovement.DOMAIN_ESCAPE, scoreDomainEscape(s)));
            actions.add(OpSukunaAction.domain(scoreDomain(s)));
            actions.add(OpSukunaAction.tenShadowsDomain(scoreTenShadowsDomain(s)));
        }
        actions.sort(Comparator.comparingDouble((OpSukunaAction action) -> adjustedScore(s, action)).reversed());
        StringBuilder builder = new StringBuilder(selected.name).append('=').append(round(selected.score));
        int limit = Math.min(5, actions.size());
        for (int i = 0; i < limit; i++) {
            OpSukunaAction action = actions.get(i);
            builder.append(';').append(action.name).append('=').append(round(adjustedScore(s, action)));
        }
        return builder.toString();
    }

    static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    static double scoreSimpleDomain(OpSukunaSnapshot s) {
        if (!s.canUseSimpleDomain || s.selfSimpleDomain || s.selfDomain) return -1.0;
        if (!shouldSpendSimpleDomain(s)) return -999.0;
        double bridgePenalty = s.catastrophicDomain && (s.domainDeathSpiral || s.simpleDomainExpiresSoon || s.healthLosingRace) ? 0.65 : 0.0;
        double dominantVoid = s.domainField.singleDominantSureHit && s.domainField.dominantVoidLike ? 1.25 : 0.0;
        return s.domainAssessment.hwbScore + dominantVoid - bridgePenalty;
    }

    static boolean shouldSpendSimpleDomain(OpSukunaSnapshot s) {
        if (!s.canUseSimpleDomain || s.selfSimpleDomain || s.selfDomain) return false;
        if (s.domainField.clashSuppressed && !s.antiDomainGapImminent && !s.domainDeathSpiral) return false;
        if (s.domainField.activeEnemyDomains > 1 && !s.domainField.singleDominantSureHit && !s.antiDomainGapImminent && !s.domainDeathSpiral) return false;
        if (s.brainDamaged && s.gojoTarget && !s.targetDomain && !s.domainField.singleDominantSureHit
                && !s.antiDomainGapImminent && !s.domainDeathSpiral) {
            return false;
        }
        boolean concreteThreat = s.targetDomain || s.targetCastingDomain || s.domainField.singleDominantSureHit;
        if (!concreteThreat) return false;
        if (s.targetCastingDomain && (s.gojoTarget || s.domainAssessment.targetDomainThreat > 0.72 || s.domainField.dominantVoidLike)) {
            return shouldCoverDomainStartup(s);
        }
        if (s.antiDomainGapImminent || s.domainDeathSpiral) return true;
        if (s.domainField.singleDominantSureHit) {
            return s.voidExposure >= 0.9 || s.catastrophicDomain || s.domainField.dominantVoidLike || !s.domainDeliberation.escapeFeasible;
        }
        return s.targetDomain && (s.voidExposure >= 0.9 || s.catastrophicDomain || !s.domainDeliberation.escapeFeasible);
    }

    static boolean shouldFirstInstantDomainEscape(OpSukunaSnapshot s) {
        return s.targetCastingDomain
                && !s.targetDomain
                && !s.domainField.singleDominantSureHit
                && !s.selfDomain
                && !s.selfSimpleDomain
                && s.domainEscapeWindow
                && !s.pathBlocked
                && domainThreatAge(s) <= 12.0;
    }

    static boolean hasConcreteDomainSignal(OpSukunaSnapshot s) {
        return s.targetDomain || s.targetCastingDomain || s.domainField.singleDominantSureHit || s.domainField.dominantCasting;
    }

    static String domainPhase(OpSukunaSnapshot s) {
        if (!hasConcreteDomainSignal(s)) return "NONE";
        if (shouldFirstInstantDomainEscape(s)) return "ESCAPE_BEFORE_CLOSE";
        if (shouldForceCounterDomainNow(s)) return "COUNTER_DOMAIN_NOW";
        if (muryoSureHitEndgame(s)) return "MURYO_ENDGAME";
        if (dominantDomainReserveCritical(s)) return "COMMIT_RESERVE";
        return "DOMAIN_RESPONSE";
    }

    static double domainCommitDeadline(OpSukunaSnapshot s) {
        if (!hasConcreteDomainSignal(s)) return -1.0;
        return Math.max(0.0, 12.0 - domainThreatAge(s));
    }

    static boolean shouldForceCounterDomainNow(OpSukunaSnapshot s) {
        if (!canSelectDomain(s) || s.domainField.clashSuppressed) {
            return false;
        }
        boolean muryoLike = "GOJO_MURYO".equals(s.domainProfile.name)
                || s.domainField.dominantVoidLike
                || s.gojoTarget && (s.targetCastingDomain || s.targetDomain);
        if (!muryoLike) {
            return false;
        }
        boolean concreteThreat = s.targetCastingDomain || s.targetDomain || s.domainField.singleDominantSureHit;
        if (!concreteThreat) {
            return false;
        }
        boolean escapeWindowSpent = domainThreatAge(s) > (muryoLike ? 8.0 : 12.0) || !s.domainDeliberation.escapeFeasible || s.pathBlocked;
        boolean sureHitClosing = s.domainField.singleDominantSureHit || s.voidExposure >= 0.9 || s.catastrophicDomain;
        boolean reserveExpiring = s.selfSimpleDomain && s.simpleDomainDuration <= 120;
        return escapeWindowSpent || sureHitClosing || reserveExpiring;
    }

    static boolean canEmergencyCounterDomain(OpSukunaSnapshot s) {
        if (s.selfDomain || s.brainDamaged || s.domainField.clashSuppressed) {
            return false;
        }
        boolean muryoSignal = "GOJO_MURYO".equals(s.domainProfile.name)
                || s.domainField.dominantVoidLike
                || s.gojoTarget && (s.targetCastingDomain || s.targetDomain || s.targetSkill == 220.0);
        if (!muryoSignal) {
            return false;
        }
        boolean concreteThreat = s.targetCastingDomain || s.targetSkill == 220.0 || s.domainField.dominantCasting
                || s.domainField.singleDominantSureHit || s.targetDomain;
        if (!concreteThreat) {
            return false;
        }
        if (GetDistanceProcedure.execute(s.sukuna) > JujutsucraftModVariables.MapVariables.get(s.world).DomainExpansionRadius - 4.0) {
            return false;
        }
        int unstableDuration = effectDuration(s.sukuna, JujutsucraftModMobEffects.UNSTABLE.get());
        if (unstableDuration > 16) {
            return false;
        }
        return s.sukuna.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())
                || s.sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get())
                || unstableDuration > 0
                || s.domainReady;
    }

    static boolean shouldCoverDomainStartup(OpSukunaSnapshot s) {
        if (s.domainField.singleDominantSureHit || s.targetDomain || s.antiDomainGapImminent || s.domainDeathSpiral) {
            return true;
        }
        if (!s.targetCastingDomain) {
            return false;
        }
        if (!s.domainDeliberation.escapeFeasible || s.pathBlocked) {
            return true;
        }
        return domainThreatAge(s) >= 14.0 || s.voidExposure >= 1.25 || s.catastrophicDomain;
    }

    static boolean shouldAbortDomainEscape(OpSukunaSnapshot s) {
        if (!s.targetCastingDomain && !s.domainField.singleDominantSureHit && !s.targetDomain) {
            return false;
        }
        return s.pathBlocked
                || !s.domainDeliberation.escapeFeasible
                || s.domainField.singleDominantSureHit
                || s.targetDomain
                || domainThreatAge(s) > 18.0;
    }

    static double domainThreatAge(OpSukunaSnapshot s) {
        return s.memory.firstDomainThreatTick < 0.0 ? 0.0 : Math.max(0.0, s.tick - s.memory.firstDomainThreatTick);
    }

    static boolean dominantDomainReserveCritical(OpSukunaSnapshot s) {
        return s.domainField.singleDominantSureHit
                && !s.selfDomain
                && !s.domainField.clashSuppressed
                && (s.domainField.dominantVoidLike || s.voidExposure >= 1.15 || s.catastrophicDomain)
                && (!s.domainDeliberation.escapeFeasible || s.pathBlocked || "GOJO_MURYO".equals(s.domainProfile.name))
                && (s.simpleDomainDuration <= 120 || !s.selfSimpleDomain && s.simpleDomainCooldown > 0 || s.antiDomainGapImminent);
    }

    static boolean muryoSureHitEndgame(OpSukunaSnapshot s) {
        return s.domainField.singleDominantSureHit
                && !s.selfDomain
                && !s.domainField.clashSuppressed
                && (s.domainField.dominantVoidLike || "GOJO_MURYO".equals(s.domainProfile.name) || s.voidExposure >= 1.15)
                && (!s.domainDeliberation.escapeFeasible || s.pathBlocked || "GOJO_MURYO".equals(s.domainProfile.name))
                && s.simpleDomainDuration <= 120;
    }

    static boolean hasPrimaryAntiDomainReserve(OpSukunaSnapshot s) {
        return canSelectDomain(s)
                || s.worldCutReady
                || !s.selfSimpleDomain && s.canUseSimpleDomain && shouldSpendSimpleDomain(s)
                || s.canUseTenShadowsDomain && s.normalReady && !s.brainDamaged
                || s.canUseMahoraga && s.normalReady && !s.mahoragaExist;
    }

    static double scoreDomainEscape(OpSukunaSnapshot s) {
        if (!s.domainEscapeWindow || s.selfDomain || s.selfSimpleDomain && !s.antiDomainGapImminent) return -1.0;
        return 0.25 + s.domainAssessment.targetDomainThreat * 1.45 + (s.catastrophicDomain ? 0.65 : 0.0)
                + (s.pathBlocked ? -0.45 : 0.0) + close(s.distance, 28.0) * 0.35;
    }

    static double scoreGuard(OpSukunaSnapshot s) {
        if (s.sukuna.hasEffect(JujutsucraftModMobEffects.GUARD.get()) || s.sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_GUARD.get())) return -1.0;
        double recency = s.tick - s.memory.lastGuardTick < 14.0 ? -1.2 : 0.0;
        double comboRisk = s.meleeThreat * close(s.distance, s.itadoriModulo ? 10.0 : 7.0) + s.targetSkillDanger * 0.65 + s.damageTakenBurst * 1.8 + s.immediateThreat * 0.9;
        double domainPenalty = s.targetDomain || s.targetCastingDomain ? -0.35 : 0.0;
        double chainPenalty = s.blackFlashChain && s.healthLosingRace ? -0.5 : 0.0;
        return recency - 0.05 + comboRisk + domainPenalty + chainPenalty + s.survivalUrgency * 0.35 + s.adaptation.learnedBurstRisk * 0.22;
    }

    static double scoreGuardTiming(OpSukunaSnapshot s) {
        if (s.sukuna.hasEffect(JujutsucraftModMobEffects.GUARD.get()) || s.sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_GUARD.get())) return -1.0;
        double recency = s.tick - s.memory.lastGuardTimingTick < 5.0 ? -1.4 : 0.0;
        double startup = s.targetSkillStartup ? 1.15 : 0.0;
        double hitNow = s.immediateThreat * 1.95 + s.meleeThreat * close(s.predictedDistance, s.itadoriModulo ? 13.0 : 8.0) * 1.15;
        return recency + hitNow + startup + s.damageTakenBurst * 0.9 + s.survivalUrgency * 0.25 - (s.targetDomain || s.targetCastingDomain ? 0.25 : 0.0);
    }

    static double scoreEvasiveBackstep(OpSukunaSnapshot s) {
        double recency = s.tick - Math.max(s.memory.lastBackstepTick, s.memory.lastEvasiveBackstepTick) < 6.0 ? -1.3 : 0.0;
        double projectile = s.incomingProjectileRisk * 2.8;
        double burst = s.targetSkillStartup && s.predictedDistance < (s.itadoriModulo ? 15.0 : 10.0) ? 1.25 : 0.0;
        double closePunish = s.meleeThreat * close(s.distance, s.itadoriModulo ? 12.0 : 7.0);
        double sustainDomain = s.selfDomain && s.domainAssessment.sustainCost < 0.3 ? -1.4 : 0.0;
        return recency + projectile + burst + closePunish + sustainDomain + s.immediateThreat * 0.85 + s.survivalUrgency * 0.25;
    }

    static double scoreRct(OpSukunaSnapshot s) {
        // Emergency override BEFORE canUseRct guard — CURSED_TECHNIQUE blocks canUseRct
        // permanently once any skill fires, so near-death must bypass that check
        if (!s.selfRct && !s.selfAntiHeal && s.rctStrain <= 0.92
                && s.selfHealthRatio < 0.38 && s.itadoriModulo) return 2.5;
        if (!s.canUseRct || s.selfRct || s.selfAntiHeal) return -1.0;
        if (s.rctStrain > 0.92) return -1.0;
        double recency = s.tick - s.memory.lastRecoveryWindowTick < 24.0 ? -1.1 : 0.0;
        double health = Mth.clamp((0.78 - s.selfHealthRatio) * 2.2, 0.0, 1.5);
        double burst = s.damageTakenBurst * 1.35 + s.recentDamageRatio * 5.0;
        double lethal = s.lethalForecast ? 0.9 : 0.0;
        double strainPenalty = s.rctStrain > 0.68 ? (s.rctStrain - 0.68) * 2.4 : 0.0;
        double baseFatigue = baseFatigueStrain(s.sukuna);
        double fatiguePenalty = baseFatigue > 0.55 ? (baseFatigue - 0.55) * 0.9 : 0.0;
        return recency - 0.2 + health + burst + lethal - strainPenalty - fatiguePenalty;
    }

    static double scoreBurnoutRct(OpSukunaSnapshot s) {
        if (!s.canUseBurnoutRct || s.nbt.getBoolean("PRESS_BURNOUT")) return -1.0;
        double recency = s.tick - s.memory.lastBurnoutRctTick < 160.0 ? -1.5 : 0.0;
        double value = (s.sukuna.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get()) ? 1.15 : 0.0)
                + (s.sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get()) ? 0.85 : 0.0)
                + (s.lethalForecast ? 0.35 : 0.0);
        return recency + value;
    }

    static double scoreDomainAmplification(OpSukunaSnapshot s) {
        if (!s.canUseDomainAmplification) return -1.0;
        double recency = s.tick - s.memory.lastDomainAmplificationTick < 45.0 ? -0.6 : 0.0;
        double infinityValue = s.infinitySignal * (s.distance < 12.0 ? 1.9 : 1.35);
        double domainValue = (s.targetNeutralization || s.targetDomainAmplification ? 0.35 : 0.0) + s.voidExposure * 0.45;
        double burstValue = s.itadoriModulo && s.distance < 10.0 ? 0.75 : 0.0;
        boolean hasTacticalNeed = s.infinitySignal > 0.0 || s.targetDomain || s.targetCastingDomain
                || s.targetNeutralization || s.targetDomainAmplification || s.voidExposure > 0.2;
        double wastePenalty = hasTacticalNeed ? 0.0 : 0.45;
        return recency - 0.05 + infinityValue + domainValue + burstValue + s.targetSkillDanger * 0.2 + (s.purpleWindup ? 0.25 : 0.0) - wastePenalty;
    }

    static double scoreBackstep(OpSukunaSnapshot s) {
        double recency = s.tick - s.memory.lastBackstepTick < 8.0 ? -2.0 : 0.0;
        double burstRange = s.itadoriModulo ? close(s.distance, 11.0) * 1.35 : close(s.distance, 7.5) * 0.8;
        double danger = s.dangerArea * 1.15 + s.targetSkillDanger * 0.8 + s.meleeThreat * burstRange + s.damageTakenBurst * 2.0;
        double health = (1.0 - s.selfHealthRatio) * s.recentDamageRatio * 6.0;
        double punishLoss = s.targetCooldown || s.targetUnstable ? -0.4 : 0.0;
        double sustainDomain = s.selfDomain && s.domainAssessment.sustainCost < 0.3 ? -1.4 : 0.0;
        return recency + danger + health + punishLoss + sustainDomain + s.survivalUrgency * 0.35;
    }

    static double scoreBlackFlashRecovery(OpSukunaSnapshot s) {
        if (s.infinitySignal > 0.0 && !s.canBypassInfinityNow || s.selfDomain || s.combatCooldown || s.targetGuard || s.targetDodge || s.targetCounter) return -1.0;
        if (!s.rctFatigued && !s.survivalMode) return -0.35;
        if (s.itadoriModulo && !s.rctFatigued && s.whiffOpportunity < 0.45 && s.predictedDistance > 5.5) return -0.75;
        double range = band(s.predictedDistance, 2.0, 6.8);
        double window = s.whiffOpportunity * 0.65 + (s.targetCooldown || s.targetUnstable ? 0.45 : 0.0) + (s.targetOverextended ? 0.25 : 0.0);
        double tooDangerous = s.lethalForecast && s.incomingKillRisk > 1.05 && window < 0.45 ? 0.75 : 0.0;
        double fatigueRecovery = baseFatigueStrain(s.sukuna) * 0.35;
        return -0.1 + s.blackFlashRecoveryValue * 0.85 + fatigueRecovery + range * 0.75 + window + s.hitConfidence * 0.25 - tooDangerous;
    }

    static double baseFatigueStrain(LivingEntity entity) {
        if (!entity.hasEffect(JujutsucraftModMobEffects.FATIGUE.get())) {
            return 0.0;
        }
        int amplifier = effectAmplifier(entity, JujutsucraftModMobEffects.FATIGUE.get());
        int duration = effectDuration(entity, JujutsucraftModMobEffects.FATIGUE.get());
        return Mth.clamp((amplifier + 1.0) * 0.18 + Math.min(duration, 2400) / 2400.0 * 0.45, 0.0, 1.0);
    }

    static double scoreProjectileDodge(OpSukunaSnapshot s) {
        double recency = s.tick - s.memory.lastProjectileDodgeTick < 10.0 ? -1.2 : 0.0;
        return recency + s.incomingProjectileRisk * 3.6 + s.targetSkillDanger * 0.35 + s.immediateThreat * 0.35;
    }

    static double scorePurpleEvade(OpSukunaSnapshot s) {
        if (!s.purpleThreat) return -1.0;
        double recency = s.tick - s.memory.lastPurpleEvadeTick < 8.0 ? -0.55 : 0.0;
        double windupInterruptWindow = s.purpleWindup && s.clearShot && s.worldCutCapable ? -0.15 : 0.0;
        double trappedDomainPenalty = (s.targetDomain || s.targetCastingDomain || s.domainField.singleDominantSureHit || s.voidExposure > 0.45)
                && (s.pathBlocked || !s.domainDeliberation.escapeFeasible) ? 1.35 : 0.0;
        return recency + s.purpleRisk * 3.0 + (s.purpleProjectile ? 0.65 : 0.0) + (s.purpleLineOfFire ? 0.55 : 0.0)
                + (s.distance < 22.0 ? 0.25 : 0.0) + windupInterruptWindow - trappedDomainPenalty;
    }

    static double scoreMaintainRange(OpSukunaSnapshot s) {
        if (muryoSureHitEndgame(s)) return -2.0;
        if (shouldForceOpeningEngage(s)) return -0.75;
        if ((s.targetDomain || s.targetCastingDomain || s.domainField.singleDominantSureHit || s.voidExposure > 0.45)
                && (s.pathBlocked || !s.domainDeliberation.escapeFeasible)) return -0.85;
        if (s.selfInsideSolid) return 0.85 + s.stuckLevel;
        if (s.pathBlocked || s.stuckLevel > 0.55) return 0.05 + s.groupActivePressure * 0.2 + s.adaptation.trapMastery * 0.35;
        if (s.memory.rangeActionStreak >= 2 && s.damageDealt <= s.target.getMaxHealth() * 0.01 && s.damageTakenBurst < 0.18) return -0.45;
        double error = Math.abs(s.predictedDistance - s.idealDistance) / Math.max(8.0, s.idealDistance);
        double unsafeClose = s.meleeThreat * close(s.distance, s.itadoriModulo ? 8.0 : 6.0) * (s.itadoriModulo ? 0.55 : 0.7);
        return 0.16 + Mth.clamp(error, 0.0, 0.9) + unsafeClose + s.dangerArea * 0.15
                + s.groupPressure * 0.35 + s.groupActivePressure * 0.25 + s.groupApexPressure * 0.2
                + s.adaptation.rangeKiteNeed * 0.38 + s.adaptation.trapMastery * 0.18
                + (s.itadoriModulo && s.distance > 16.0 ? -0.45 : 0.0);
    }

    static double scorePressureChase(OpSukunaSnapshot s) {
        if (s.lethalForecast || s.survivalMode || hasConcreteDomainSignal(s) || s.purpleThreat) return -1.0;
        if (s.infinitySignal > 0.0 && !s.canBypassInfinityNow) return -0.8;
        if (s.pathBlocked && s.stuckLevel > 0.65) return -0.35;
        double desired = s.itadoriModulo ? 5.0 : Math.max(5.0, s.idealDistance - 3.0);
        double distanceNeed = Mth.clamp((s.distance - desired) / (s.itadoriModulo ? 22.0 : 30.0), 0.0, 1.25);
        double noImpact = Mth.clamp(s.memory.noImpactActionStreak / 4.0, 0.0, 0.85);
        // If stuck and already close, the noImpact boost pushes Sukuna to keep chasing (wrong) — zero it out
        if (s.memory.noImpactActionStreak > 5 && s.distance < 15.0) noImpact = 0.0;
        double targetWindow = s.targetCooldown || s.targetUnstable || s.targetWhiffed || s.targetOverextended ? 0.35 : 0.0;
        double tempo = s.itadoriModulo ? 0.35 : 0.0;
        if (shouldForceOpeningEngage(s)) {
            tempo += 0.55;
        }
        double threatPenalty = s.immediateThreat > 0.9 && s.distance < (s.itadoriModulo ? 10.0 : 7.0) ? 0.55 : 0.0;
        // If noImpact streak is high but Sukuna is already close, chasing more won't help — try something else
        double stuckChasePenalty = s.memory.noImpactActionStreak > 6 && s.distance < 15.0 ? -0.5 : 0.0;
        // After 4+ consecutive range actions without opening engage, stop looping and commit
        double rangeLoopPenalty = !shouldForceOpeningEngage(s) && s.memory.rangeActionStreak > 4 ? -0.45 : 0.0;
        // When Itadori is near-death and Sukuna is close, stop chasing and start hitting
        double killConfirmPenalty = s.itadoriModulo && s.targetHealthRatio < 0.35 && s.distance < 12.0 ? -0.65 : 0.0;
        double itadoriLoopPenalty = s.itadoriModulo && s.memory.noImpactActionStreak >= 3 && s.distance <= 18.0 ? -1.0 : 0.0;
        return 0.1 + distanceNeed * 1.15 + noImpact + s.targetFleeBias * 0.35 + targetWindow + tempo
                + s.hitConfidence * 0.12 - s.dangerArea * 0.25 - threatPenalty + stuckChasePenalty + rangeLoopPenalty + killConfirmPenalty + itadoriLoopPenalty;
    }

    static double scoreCutOffEscape(OpSukunaSnapshot s) {
        if (s.yujiBurstDuel && s.distance < 16.0) return -0.85;
        if (s.memory.rangeActionStreak >= 2 && !s.targetEscaping) return -0.55;
        return 0.12 + (s.targetEscaping ? 0.75 : 0.0) + s.targetFleeBias * 0.55 + s.adaptation.rangeKiteNeed * 0.35
                + (s.targetHealthRatio < 0.35 ? 0.25 : 0.0);
    }

    static double scoreStrafePressure(OpSukunaSnapshot s) {
        if (muryoSureHitEndgame(s)) return -2.0;
        double range = band(s.distance, s.itadoriModulo ? 6.0 : 5.0, s.itadoriModulo ? 16.0 : 24.0);
        double cooldown = s.normalReady ? -0.18 : 0.18;
        double tempoPenalty = shouldForceOffensiveTempo(s) ? 0.65 : 0.0;
        return 0.18 + range * 0.42 + s.rangeThreat * 0.25 + s.meleeThreat * 0.22 + s.whiffOpportunity * 0.5 + cooldown
                + s.adaptation.trapMastery * 0.32 + s.adaptation.rangeKiteNeed * 0.2
                + (s.itadoriModulo && !s.pathBlocked ? 0.2 : 0.0) + s.stuckLevel * 0.25 - tempoPenalty
                - (shouldForceOpeningEngage(s) ? 0.55 : 0.0);
    }

    static double scoreBaitWhiff(OpSukunaSnapshot s) {
        if ((s.targetDomain || s.targetCastingDomain || s.domainField.singleDominantSureHit || s.voidExposure > 0.45)
                && (s.pathBlocked || !s.domainDeliberation.escapeFeasible)) return -0.9;
        if (s.selfDomain || s.targetDomain || s.targetCastingDomain) return -0.4;
        double band = band(s.distance, s.itadoriModulo ? 8.0 : 5.0, s.itadoriModulo ? 14.0 : 13.0);
        double bait = s.meleeThreat * 0.55 + s.read.aggression * 0.45 + s.targetSkillDanger * 0.25;
        return 0.1 + band * 0.45 + bait + (s.normalReady ? -0.18 : 0.12) + s.whiffOpportunity * 0.35
                + s.adaptation.antiEvasionNeed * 0.45 + s.adaptation.safePunishWindow * 0.25;
    }

    static double scoreChase(OpSukunaSnapshot s) {
        if (s.yujiBurstDuel && s.distance < 14.0 && s.immediateThreat > 0.45) return -0.55;
        double ideal = Math.abs(s.distance - s.idealDistance) / 32.0;
        return 0.22 + Mth.clamp(ideal, 0.0, 0.8) + s.targetFleeBias * 0.35 - s.dangerArea * 0.35
                - s.adaptation.trapMastery * 0.45 - s.adaptation.antiEvasionNeed * 0.25
                - (s.pathBlocked ? 0.5 : 0.0) - s.stuckLevel * 0.35;
    }

    static double scoreBasic(OpSukunaSnapshot s) {
        if (s.infinitySignal > 0.0 && !s.canBypassInfinityNow) return -0.55;
        double yujiPunish = s.yujiBurstDuel && (s.whiffOpportunity > 0.0 || !s.lethalForecast) ? 0.42 : 0.0;
        double infinityMelee = s.canBypassInfinityNow && s.infinitySignal > 0.0 && s.hitboxDistance <= s.selfReach + 2.0 ? 1.05 : 0.0;
        double contactTempo = s.hitboxDistance <= s.selfReach + 1.0 ? 0.65 : close(s.distance, 7.0) * 0.35;
        // Sukuna is close but stuck in a no-damage loop — force a melee attempt
        double stuckPunchBoost = s.memory.noImpactActionStreak > 4 && s.distance < 8.0 ? 0.55 : 0.0;
        return 0.18 + (s.distance > 45.0 ? 0.35 : 0.0) + s.dangerArea * 0.2 + (s.trivialTarget ? 1.0 : 0.0)
                + yujiPunish + infinityMelee + contactTempo + s.opportunity * 0.28 + s.hitConfidence * 0.22
                + stuckPunchBoost;
    }

    static double scoreDismantle(OpSukunaSnapshot s) {
        if (s.selfDomain) return -1.25;
        if (s.infinitySignal > 0.0 && !s.canBypassInfinityNow) return -0.75;
        // After extended no-damage loop vs Itadori, DISMANTLE animation wastes time — suppress it
        if (s.itadoriModulo && !s.selfDomain && s.memory.noImpactActionStreak >= 10) return -1.0;
        double range = band(s.distance, 5.0, 48.0);
        double killValue = s.targetKillEstimate.finisherValue * 0.32 + s.targetPowerProfile.score * 0.18;
        double yujiPunish = s.yujiBurstDuel && (s.targetCooldown || s.targetUnstable || s.targetWhiffed || s.targetOverextended || s.targetSkillStartup && !s.lethalForecast)
                ? 0.55 : 0.0;
        double rapidTempo = shouldForceOffensiveTempo(s) ? 0.35 : 0.0;
        // Extra pressure in mid-health window — Itadori is still tanky but vulnerable enough to punish
        double midHealthPressure = s.itadoriModulo && s.targetHealthRatio > 0.25 && s.targetHealthRatio < 0.65 ? 0.25 : 0.0;
        return 0.52 + range * 0.78 + s.opportunity * 0.48 + s.whiffOpportunity * 0.65 + s.hitConfidence * 0.42
                + s.domainAssessment.punishScore * 0.15 + s.killPressure(0.20) + killValue + yujiPunish
                + rapidTempo + midHealthPressure
                - s.adaptation.antiEvasionNeed * 0.35 - s.expensiveWastePenalty(0.20);
    }

    static double scoreCleave(OpSukunaSnapshot s) {
        if (s.infinitySignal > 0.0 && !s.canBypassInfinityNow) return -0.85;
        double contactReach = Math.max(1.25, Math.min(s.selfReach + 0.6, s.itadoriModulo ? 3.25 : 2.75));
        if (!s.selfDomain && s.hitboxDistance > contactReach) {
            return -0.95 + close(s.hitboxDistance, contactReach + 1.0) * 0.2;
        }
        double domainBonus = s.selfDomain ? 1.65 + band(s.distance, 4.0, 48.0) * 0.85 : 0.0;
        double infinityMelee = s.canBypassInfinityNow && s.infinitySignal > 0.0 && s.hitboxDistance <= s.selfReach + 2.0 ? 1.15 : 0.0;
        double killValue = s.targetKillEstimate.finisherValue * 0.38 + (s.targetKillEstimate.difficult ? 0.22 : 0.0);
        double yujiWindow = s.yujiBurstDuel && s.distance <= 10.5 && (s.targetCooldown || s.targetUnstable || s.targetWhiffed || s.targetOverextended || !s.lethalForecast)
                ? 0.65 : 0.0;
        double reachValue = s.selfDomain ? close(s.distance, 48.0) * 0.75 : close(s.hitboxDistance, contactReach) * 1.15;
        double midHealthPressure = s.itadoriModulo && s.targetHealthRatio > 0.25 && s.targetHealthRatio < 0.65 ? 0.25 : 0.0;
        return 0.62 + reachValue + s.meleeThreat * 0.35 + s.opportunity * 0.68 + s.whiffOpportunity * 0.95
                + s.hitConfidence * 0.45 + s.killPressure(0.28) + domainBonus - s.dodgeCounterRisk * 0.35 - s.adaptation.antiEvasionNeed * 0.5
                + killValue + yujiWindow + infinityMelee + (s.trivialTarget ? 0.45 : 0.0) + midHealthPressure;
    }

    static double scoreOpen(OpSukunaSnapshot s) {
        if (s.infinitySignal > 0.0 && !s.selfDomain && !s.canBypassInfinityNow) return -0.45;
        if (s.trivialTarget) return -1.25;
        if (s.openShotQuality <= 0.0 && !s.selfDomain) return -1.35;
        // OPEN consistently dodged by Itadori in no-damage loops — stop trying it
        if (s.itadoriModulo && !s.selfDomain && s.memory.noImpactActionStreak >= 5) return -2.0;
        if (s.itadoriModulo && !s.selfDomain && (!s.clearShot || s.distance < 12.0 || s.immediateThreat > 0.75
                || !(s.targetCooldown || s.targetUnstable || s.targetWhiffed || s.openShotQuality > 0.78))) return -0.75;
        double range = band(s.distance, 8.0, 40.0);
        double domainBonus = s.selfDomain ? 1.25 : 0.0;
        double hardTargetValue = s.targetKillEstimate.resourceValue * 0.55 + s.targetPowerProfile.score * 0.25;
        return 0.34 + range * 0.8 + s.healScore * 0.45 + s.tankScore * 0.35 + s.postHealWindow * 0.4
                + s.domainAssessment.punishScore * 0.2 + s.whiffOpportunity * 0.7 + s.hitConfidence * 0.35 + s.killPressure(0.35) + domainBonus - s.dangerArea * 0.25
                + s.adaptation.rangeKiteNeed * 0.24 + s.adaptation.safePunishWindow * 0.18
                + (s.itadoriModulo && s.clearShot && s.distance >= 12.0 && s.distance <= 28.0 && (s.targetCooldown || s.targetUnstable || s.targetWhiffed) && s.immediateThreat < 0.65 ? 0.35 : -0.55)
                + s.openShotQuality * 0.65
                + hardTargetValue
                - s.expensiveWastePenalty(0.45);
    }

    static double shrineOpenPriority(OpSukunaSnapshot s) {
        if (!s.selfDomain) return 0.0;
        double chargeNeed = s.fugaCharge < s.fugaChargeMax ? 0.45 : 0.0;
        double hardTarget = s.targetPowerProfile.high || s.infinitySignal > 0.0 ? 0.35 : 0.0;
        double crowded = Mth.clamp(s.groupActivePressure * 0.35 + s.groupApexPressure * 0.25, 0.0, 0.5);
        return 0.7 + chargeNeed + hardTarget + crowded;
    }

    static double scoreWorldCut(OpSukunaSnapshot s) {
        if (!s.worldCutReady) return -2.0;
        if (s.trivialTarget) return -2.0;
        double range = band(s.distance, 4.0, 58.0);
        double gojoBonus = s.gojoTarget ? 0.65 : 0.0;
        double antiDomainSpecial = s.targetPerfectAntiDomain ? 0.45 : 0.0;
        double castBonus = s.targetCastingDomain ? 0.55 : 0.0;
        double burstPunish = s.itadoriModulo && (s.targetCooldown || s.targetUnstable || s.targetWhiffed || s.targetOverextended
                || s.distance >= 9.0 && s.immediateThreat < 0.85 || s.yujiBurstDuel) ? 1.05 : 0.0;
        double commitPunish = s.itadoriModulo && s.targetSkill != 0.0 && burstPunish == 0.0 ? 0.60 : 0.0;
        double crisis = (s.domainDeathSpiral ? 0.75 : 0.0) + (s.blackFlashChain && s.healthLosingRace ? 0.45 : 0.0);
        double killEstimate = s.targetKillEstimate.resourceValue * 0.7 + (s.targetKillEstimate.difficult ? 0.25 : 0.0);
        return 0.30 + range * 0.65 + s.infinitySignal * 1.9 + gojoBonus + antiDomainSpecial + castBonus + s.tankScore * 0.45 + s.targetDomainCounterBias * 0.25
                + s.domainAssessment.punishScore * 0.35 + s.killPressure(0.55) + s.opportunity * 0.65 + s.whiffOpportunity * 0.8
                + s.hitConfidence * 0.25 + burstPunish + commitPunish + crisis + s.adaptation.antiEvasionNeed * 0.38 + s.adaptation.rangeKiteNeed * 0.16 - close(s.distance, 3.5) * 0.4
                + killEstimate
                - s.expensiveWastePenalty(0.65);
    }

    static double scoreDomain(OpSukunaSnapshot s) {
        if (!canSelectDomain(s)) return -2.0;
        if (s.trivialTarget) return -1.5;
        if (s.domainField.clashSuppressed && !s.antiDomainGapImminent && !s.domainDeathSpiral) return -1.1;
        if (s.targetThreat < 0.45 && !s.targetDomain && !s.targetCastingDomain && s.infinitySignal <= 0.0 && !s.domainDeathSpiral) return -0.8;
        return s.domainAssessment.castScore + s.domainConfidence * 0.55
                + (s.catastrophicDomain ? 0.35 : 0.0)
                + (s.blackFlashChain && s.healthLosingRace ? 0.35 : 0.0);
    }

    static double scoreSummon(OpSukunaSnapshot s) {
        if (!s.canUseTenShadows || s.trivialTarget || !s.normalReady || s.selfDomain) return -2.0;
        return -0.05 + s.rangeThreat * 0.45 + s.healScore * 0.35 + s.pressure * 0.35
                + (s.targetEscaping ? 0.25 : 0.0) + (s.targetThreat > 0.55 ? 0.25 : 0.0)
                + (s.infinitySignal > 0.0 ? 0.55 : 0.0) + (s.gojoTarget && s.isMeguna ? 0.25 : 0.0) + (s.purpleThreat ? 0.15 : 0.0)
                - s.expensiveWastePenalty(0.42);
    }

    static double scoreAgito(OpSukunaSnapshot s) {
        if (!s.canUseAgito || !s.normalReady || s.trivialTarget || s.selfDomain) return -2.0;
        return 0.05 + s.healScore * 0.75 + s.rangeThreat * 0.45 + s.pressure * 0.45 + s.postHealWindow * 0.35
                + (s.targetThreat > 0.65 ? 0.35 : 0.0) + s.killPressure(0.25) * 0.25
                + (s.infinitySignal > 0.0 ? 0.45 : 0.0) + (s.purpleThreat ? 0.18 : 0.0)
                - s.expensiveWastePenalty(0.58);
    }

    static double scoreMahoraga(OpSukunaSnapshot s) {
        if (!s.canUseMahoraga || !s.normalReady || s.trivialTarget || s.mahoragaExist) return -2.0;
        double adaptation = (s.infinitySignal > 0.0 ? 1.05 : 0.0) + (s.mahoragaWheel ? 0.35 : 0.0);
        return -0.15 + adaptation + s.domainAssessment.targetDomainThreat * 0.8 + s.damageTakenBurst * 0.85
                + (s.catastrophicDomain ? 0.45 : 0.0)
                + (s.purpleThreat ? 0.22 : 0.0) + (s.gojoTarget && s.isMeguna ? 0.35 : 0.0)
                + (s.lethalForecast ? 0.95 : 0.0) + (s.targetThreat > 0.9 ? 0.55 : 0.0)
                + (s.selfHealthRatio < 0.38 ? 0.45 : 0.0) - s.expensiveWastePenalty(0.82);
    }

    static double scoreTenShadowsDomain(OpSukunaSnapshot s) {
        if (!s.canUseTenShadowsDomain || !s.normalReady || s.trivialTarget || s.selfDomain || s.brainDamaged) return -2.0;
        if (s.isMeguna && canSelectDomain(s)) {
            return -2.0;
        }
        double betterThanShrine = s.isMeguna ? -0.45 : -0.05;
        return betterThanShrine + s.domainAssessment.castScore + s.healScore * 0.25 + s.tankScore * 0.25
                + (s.targetDomain || s.targetCastingDomain || s.domainField.singleDominantSureHit ? 0.55 : 0.0) - s.expensiveWastePenalty(0.65);
    }

    static double scoreHeianReset(OpSukunaSnapshot s) {
        if (!s.isMeguna || !s.canTransformHeian || s.isPerfectMode || s.trivialTarget || !s.gojoTarget || s.memory.heianResetUsed) return -2.0;
        if (s.memory.megunaHighThreatTicks < 260.0) return -2.0;
        boolean failedPlan = s.targetHealthRatio > 0.55 || s.selfHealthRatio < 0.55 || s.domainDeathSpiral;
        boolean emergency = s.selfHealthRatio < 0.30 || s.lethalForecast || s.domainDeathSpiral;
        boolean tenShadowsTried = s.mahoragaExist || s.mahoragaWheel || s.memory.megunaHighThreatTicks > 520.0;
        if (!failedPlan || !emergency || !tenShadowsTried) return -2.0;
        return 0.15 + (s.lethalForecast ? 1.25 : 0.0) + (s.domainDeathSpiral ? 0.9 : 0.0)
                + (s.selfHealthRatio < 0.28 ? 0.55 : 0.0) + s.damageTakenBurst * 0.5;
    }

    static String heianReason(OpSukunaSnapshot s) {
        if (!s.isMeguna || !s.canTransformHeian || !s.gojoTarget) return "not_allowed";
        if (s.memory.heianResetUsed) return "already_used";
        if (s.memory.megunaHighThreatTicks < 260.0) return "meguna_plan_active";
        if (!(s.mahoragaExist || s.mahoragaWheel || s.memory.megunaHighThreatTicks > 520.0)) return "ten_shadows_not_spent";
        if (s.domainDeathSpiral) return "domain_death_spiral";
        if (s.lethalForecast) return "lethal_forecast";
        if (s.selfHealthRatio < 0.30) return "low_health";
        return "not_emergency";
    }

    static boolean canSelectDomain(OpSukunaSnapshot s) {
        boolean emergencyCounter = canEmergencyCounterDomain(s);
        if ((!s.domainReady && !emergencyCounter) || s.selfDomain || s.brainDamaged) return false;
        boolean lethalEnemyDomain = (s.targetDomain || s.targetCastingDomain || s.domainField.singleDominantSureHit || s.domainDeathSpiral)
                && (s.catastrophicDomain || s.lethalForecast || s.domainAssessment.targetDomainThreat > 0.78);
        if (s.domainBlockedByCooldown && !emergencyCounter) return false;
        if (s.domainRecentlyOpened && !lethalEnemyDomain) return false;
        if (emergencyCounter || lethalEnemyDomain) {
            return true;
        }
        if (!hasConcreteDomainSignal(s)) {
            return canSelectOffensiveDomain(s);
        }
        return s.domainAssessment.clearValue > s.domainAssessment.sustainCost + s.domainAssessment.recoveryRisk * 0.55
                || s.domainAssessment.castScore > 0.72;
    }

    static boolean canSelectOffensiveDomain(OpSukunaSnapshot s) {
        // Override: extended Itadori duel with no progress — domain is the correct answer
        // even if domainTactics chose NONE intent (no obvious tactical signal for domain)
        boolean extendedItadoriDuel = s.itadoriModulo && s.domainReady && !s.selfDomain
                && s.targetStats.seenTicks > 400.0
                && s.memory.noImpactActionStreak >= 3
                && s.targetHealthRatio > 0.25;
        if (!extendedItadoriDuel && s.domainAssessment.intent == OpSukunaDomainIntent.NONE) {
            return false;
        }
        double castThreshold = 0.92;
        if (s.itadoriModulo && s.targetHealthRatio > 0.4) {
            castThreshold = 0.78;
        }
        if (s.itadoriModulo && s.selfHealthRatio < 0.55 && s.normalReady) {
            castThreshold = 0.65;
        }
        if (s.domainAssessment.castScore < castThreshold) {
            return false;
        }
        double netValue = s.domainAssessment.clearValue - s.domainAssessment.sustainCost - s.domainAssessment.recoveryRisk * 0.55;
        boolean decisiveValue = netValue > 0.32 || s.domainAssessment.castScore > 1.08
                || (s.itadoriModulo && s.domainAssessment.castScore > castThreshold + 0.05);
        if (!decisiveValue) {
            return false;
        }
        boolean checkmate = s.domainAssessment.intent == OpSukunaDomainIntent.CHECKMATE
                && s.killConfirm
                && s.hitConfidence > 0.45
                && s.targetThreat >= 0.62;
        boolean antiInfinity = s.domainAssessment.intent == OpSukunaDomainIntent.ANTI_INFINITY
                && s.infinity
                && !s.canBypassInfinityNow
                && s.targetThreat >= 0.78
                && (s.memory.noImpactActionStreak >= 2 || s.targetStats.seenTicks > 120.0);
        boolean crowdReset = s.domainAssessment.intent == OpSukunaDomainIntent.CROWD_RESET
                && (s.groupApexPressure > 0.55 || s.groupActivePressure > 0.95)
                && s.targetThreat >= 0.55;
        boolean survivalStall = s.domainAssessment.intent == OpSukunaDomainIntent.SURVIVAL_STALL
                && (s.lethalForecast || s.survivalUrgency > 1.0 || s.healthLosingRace && s.selfHealthRatio < 0.55);
        boolean apexDuel = s.targetPowerProfile.apex
                && s.targetThreat >= 1.05
                && (s.damageTakenBurst > 0.28 || s.targetStats.damageTakenAvg > s.sukuna.getMaxHealth() * 0.035)
                && s.domainAssessment.castScore > 1.15;
        boolean itadoriBurstThreat = s.itadoriModulo
                && s.targetThreat >= 0.55
                && (s.damageTakenBurst > 0.22 || s.memory.noImpactActionStreak >= 2 || s.targetStats.seenTicks > 120.0);
        return checkmate || antiInfinity || crowdReset || survivalStall || apexDuel || itadoriBurstThreat;
    }

    static boolean readDomainReady(LevelAccessor world, double x, double y, double z, LivingEntity sukuna) {
        boolean previousFlag = sukuna.getPersistentData().getBoolean("flag_domain");
        boolean ready = LogicConfilmDomainProcedure.execute(world, x, y, z, sukuna);
        sukuna.getPersistentData().putBoolean("flag_domain", previousFlag);
        return ready;
    }

    static String domainBlockReason(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, LivingEntity target,
                                            OpSukunaBrainMemory memory, double tick, boolean domainReady, boolean domainRecentlyOpened) {
        if (domainReady) return "ready";
        if (sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) return "own_domain_active";
        if (effectAmplifier(sukuna, JujutsucraftModMobEffects.BRAIN_DAMAGE.get()) >= 2) {
            return effectDetail(sukuna, JujutsucraftModMobEffects.BRAIN_DAMAGE.get(), "brain_damage");
        }
        if (sukuna.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get())) {
            return effectDetail(sukuna, JujutsucraftModMobEffects.UNSTABLE.get(), "unstable");
        }
        if (sukuna.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) {
            return effectDetail(sukuna, JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), "cursed_technique");
        }
        if (sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get())) {
            return effectDetail(sukuna, JujutsucraftModMobEffects.COOLDOWN_TIME.get(), "cooldown_time");
        }
        if (domainRecentlyOpened) {
            return "recent_domain_opened:" + Math.max(0.0, Math.round(420.0 - (tick - memory.lastSukunaDomainTick)));
        }
        if (sukuna.getPersistentData().getDouble("skill") != 0.0) {
            return "skill_active:" + round(sukuna.getPersistentData().getDouble("skill"));
        }
        if (target != null && target.getPersistentData().getDouble("skill") != 0.0) {
            return "target_skill_active:" + round(target.getPersistentData().getDouble("skill"));
        }
        if (GetDistanceProcedure.execute(sukuna) > JujutsucraftModVariables.MapVariables.get(world).DomainExpansionRadius - 4.0) {
            return "outside_domain_range";
        }
        if (sukuna.getPersistentData().getDouble("friend_num") != 0.0) {
            return "ally_domain_or_skill_possible";
        }
        return "logic_confirm_domain_false";
    }

    static String effectDetail(LivingEntity entity, MobEffect effect, String label) {
        MobEffectInstance instance = entity.getEffect(effect);
        if (instance == null) return label + ":none";
        return label + ":amp=" + instance.getAmplifier() + ",dur=" + instance.getDuration();
    }

    static String domainCastFailureReason(LivingEntity sukuna, CompoundTag nbt) {
        if (sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) return "confirmed_domain_expansion";
        if (effectAmplifier(sukuna, JujutsucraftModMobEffects.BRAIN_DAMAGE.get()) >= 2) {
            return effectDetail(sukuna, JujutsucraftModMobEffects.BRAIN_DAMAGE.get(), "brain_damage");
        }
        if (sukuna.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get())) {
            return effectDetail(sukuna, JujutsucraftModMobEffects.UNSTABLE.get(), "unstable");
        }
        double skill = nbt.getDouble("skill");
        if (skill == 0.0) return "skill_cleared_before_domain";
        if (skill != DOMAIN) return "skill_changed:" + round(skill);
        if (!sukuna.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) return "cursed_technique_missing";
        return "domain_not_confirmed";
    }

    static double scorePassive111(OpSukunaSnapshot s) {
        return 0.2 + band(s.distance, 2.0, 16.0) * 0.45 + s.targetSkillDanger * 0.25;
    }

    static double scorePassive112(OpSukunaSnapshot s) {
        return 0.28 + close(s.distance, 8.0) * 0.55 + s.meleeThreat * 0.35;
    }

    static double scorePassive113(OpSukunaSnapshot s) {
        return 0.22 + band(s.distance, 10.0, 48.0) * 0.45 + s.rangeThreat * 0.45;
    }

    static double close(double distance, double max) {
        return Mth.clamp((max - distance) / max, 0.0, 1.0);
    }

    static double band(double distance, double min, double max) {
        if (distance < min) {
            return Mth.clamp(distance / Math.max(1.0, min), 0.0, 1.0);
        }
        if (distance > max) {
            return Mth.clamp(1.0 - (distance - max) / Math.max(1.0, max), 0.0, 1.0);
        }
        return 1.0;
    }

    static void useDomain(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, CompoundTag nbt, OpSukunaSnapshot s) {
        if (!s.domainReady && canEmergencyCounterDomain(s)) {
            sukuna.removeEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get());
            sukuna.removeEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get());
            sukuna.removeEffect(JujutsucraftModMobEffects.UNSTABLE.get());
            nbt.putBoolean("JJKUR_OP_AI_EMERGENCY_COUNTER_DOMAIN", true);
        }
        nbt.putDouble("skill", 1.0);
        ReturnShadowProcedure.execute(world, x, y, z, sukuna);
        setSkill(sukuna, nbt, DOMAIN, 20.0, false);
        s.memory.requestSukunaDomainCast(s.tick, DOMAIN);
    }

    static void backstep(LevelAccessor world, LivingEntity sukuna, OpSukunaSnapshot s) {
        CompoundTag nbt = sukuna.getPersistentData();
        nbt.putBoolean("PRESS_S", true);
        WhenBackStepProcedure.execute(world, sukuna);
        nbt.putBoolean("PRESS_S", false);
        s.memory.lastBackstepTick = s.tick;
    }

    static void guard(LevelAccessor world, LivingEntity sukuna, OpSukunaSnapshot s) {
        StartGuardProcedure.execute(world, sukuna);
        s.memory.lastGuardTick = s.tick;
    }

    static void domainAmplification(LivingEntity sukuna, OpSukunaSnapshot s) {
        if (!sukuna.level().isClientSide()) {
            int amp = Math.max(4, effectAmplifier(sukuna, MobEffects.DAMAGE_BOOST) + 4);
            sukuna.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get(), 20, amp, false, false));
        }
        s.memory.lastDomainAmplificationTick = s.tick;
    }

    static void maintainKeyedDefenses(OpSukunaSnapshot s) {
        if (s.sukuna.getPersistentData().getBoolean("PRESS_M") && !shouldKeepRct(s)) {
            KeyReverseCursedTechniqueOnKeyReleasedProcedure.execute(s.sukuna);
        }
        if (s.sukuna.getPersistentData().getBoolean("PRESS_BURNOUT") && !shouldKeepBurnoutRct(s)) {
            BurnoutKeyOnKeyReleasedProcedure.execute(s.sukuna);
        } else if (s.sukuna.getPersistentData().getBoolean("PRESS_BURNOUT")) {
            CounterBurnoutProcedure.execute(s.sukuna);
        }
    }

    static void maintainFugaCharge(OpSukunaSnapshot s) {
        CompoundTag nbt = s.nbt;
        boolean openActive = Math.round(nbt.getDouble("skill")) == (long) OPEN;
        if (!openActive) {
            if (!s.fugaActive) {
                nbt.putBoolean("PRESS_Z", false);
            }
            return;
        }

        if (!s.selfDomain) {
            if (s.fugaCharge >= 5.0 || !s.clearShot || s.immediateThreat > 1.05) {
                nbt.putBoolean("PRESS_Z", false);
            }
            return;
        }

        String release = fugaReleaseReason(s);
        if (release.isEmpty()) {
            nbt.putBoolean("PRESS_Z", true);
            s.memory.fugaLastDecision = "hold";
        } else {
            nbt.putBoolean("PRESS_Z", false);
            s.memory.fugaLastDecision = release;
        }
    }

    static String fugaReleaseReason(OpSukunaSnapshot s) {
        if (!s.clearShot && s.fugaHoldTicks >= 18.0) return "no_clear_shot";
        if (s.fugaCharge >= s.fugaChargeMax - 0.05) return "full_charge";
        if (s.fugaDuration <= 24 && s.fugaCharge >= 1.0) return "effect_expiring";
        if (s.lethalForecast || s.selfHealthRatio < 0.24) return "survival_emergency";
        if ((s.targetEscaping || s.distance > 44.0) && s.fugaCharge >= 3.0) return "target_window_closing";
        if (s.fugaHoldTicks >= 70.0 && s.fugaChargeRate < 0.035 && s.fugaCharge >= 1.5) return "charge_stalled";
        if (s.fugaHoldTicks >= 110.0 && s.fugaCharge >= 2.5) return "partial_charge_timeout";
        return "";
    }

    static boolean shouldKeepRct(OpSukunaSnapshot s) {
        if (s.selfAntiHeal) {
            return false;
        }
        if (s.selfHealth >= s.sukuna.getMaxHealth() - 0.5) {
            return false;
        }
        if (s.nbt.getDouble("cnt_reverse_lim") >= (s.sukuna.getMaxHealth() >= 800.0F ? 400.0 : 200.0)) {
            return false;
        }
        if (s.sukuna.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) {
            return shouldHoldRctThroughTechnique(s);
        }
        return true;
    }

    static boolean shouldHoldRctThroughTechnique(OpSukunaSnapshot s) {
        if (s.rctStrain > 0.88 || baseFatigueStrain(s.sukuna) > 0.84) {
            return false;
        }
        boolean muryoCrisis = s.gojoTarget && (s.brainDamaged || s.catastrophicDomain || s.domainDeathSpiral || s.voidExposure >= 0.9);
        boolean burstCrisis = s.lethalForecast || s.healthLosingRace && s.selfHealthRatio < 0.52 || s.damageTakenBurst > 0.45;
        boolean lowHealth = s.selfHealthRatio < 0.42;
        return muryoCrisis || burstCrisis || lowHealth;
    }

    static void tryPreloadRctCombo(LivingEntity sukuna, OpSukunaSnapshot s) {
        if (!s.canUseRct || s.selfRct || s.selfAntiHeal || s.rctFatigued) {
            return;
        }
        boolean criticalHeal = s.selfHealthRatio < 0.38 || s.lethalForecast || s.damageTakenBurst > 0.52;
        boolean muryoPreload = s.gojoTarget && (s.targetCastingDomain || s.targetDomain || s.domainField.singleDominantSureHit)
                && s.selfHealthRatio < 0.62 && !s.selfDomain;
        if (!criticalHeal && !muryoPreload) {
            return;
        }
        KeyReverseCursedTechniqueOnKeyPressedProcedure.execute(sukuna);
        s.memory.lastRecoveryWindowTick = s.tick;
    }

    static boolean shouldKeepBurnoutRct(OpSukunaSnapshot s) {
        return s.canUseBurnoutRct && (s.sukuna.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get())
                || s.sukuna.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get()));
    }

    static void tryEmergencyReaction(OpSukunaSnapshot s) {
        OpSukunaAction reaction = bestOf(s, List.of(
                OpSukunaAction.move("PURPLE_EVADE", OpSukunaMovement.PURPLE_EVADE, scorePurpleEvade(s) + 0.35),
                OpSukunaAction.guardTiming(scoreGuardTiming(s) + 0.55),
                OpSukunaAction.backstep("EmergencyBackstep", scoreEvasiveBackstep(s) + 0.45),
                OpSukunaAction.rct("EmergencyRCT", scoreRct(s) + 0.25)));
        double threshold = s.nbt.getDouble("skill") != 0.0 ? 1.05 : 1.25;
        if (reaction.score > threshold) {
            reaction.execute(s.world, s.x, s.y, s.z, s.sukuna, s.nbt, s);
            s.memory.lastAction = reaction.name;
            s.memory.lastActionTarget = targetKey(s.target);
        }
    }

    static void guardTiming(LevelAccessor world, LivingEntity sukuna, OpSukunaSnapshot s) {
        StartGuardProcedure.execute(world, sukuna);
        OpSukunaMovementController.faceTarget(sukuna, s.target);
        s.memory.lastGuardTimingTick = s.tick;
        s.memory.lastGuardTick = s.tick;
    }

    static void useRct(LivingEntity sukuna, OpSukunaSnapshot s) {
        // CURSED_TECHNIQUE blocks the RCT key press — strip it first on emergency heals
        if (!sukuna.level().isClientSide() && sukuna.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) {
            sukuna.removeEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get());
        }
        KeyReverseCursedTechniqueOnKeyPressedProcedure.execute(sukuna);
        s.memory.lastRecoveryWindowTick = s.tick;
    }

    static void useSimpleDomain(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, OpSukunaSnapshot s) {
        if (!canUseSimpleDomain(sukuna) || hasSimpleDomainDefense(sukuna)) {
            return;
        }
        CompoundTag nbt = sukuna.getPersistentData();
        boolean previousAllow = nbt.getBoolean("JJKUR_OP_AI_ALLOW_SIMPLE_DOMAIN_KEY");
        nbt.putBoolean("JJKUR_OP_AI_ALLOW_SIMPLE_DOMAIN_KEY", true);
        nbt.putString("JJKUR_OP_AI_SIMPLE_DOMAIN_KEY_SOURCE", "op_engine");
        try {
            KeySimpleDomainOnKeyPressedProcedure.execute(world, x, y, z, sukuna);
        } finally {
            nbt.putBoolean("JJKUR_OP_AI_ALLOW_SIMPLE_DOMAIN_KEY", previousAllow);
        }
        if (hasSimpleDomainDefense(sukuna)) {
            s.memory.lastSimpleDomainTick = s.tick;
        }
    }

    static void useBurnoutRct(LivingEntity sukuna, OpSukunaSnapshot s) {
        BurnoutKeyOnKeyPressedProcedure.execute(sukuna);
        s.memory.lastBurnoutRctTick = s.tick;
    }

    static Vec3 horizontal(Vec3 vector) {
        Vec3 flat = new Vec3(vector.x, 0.0, vector.z);
        return flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
    }

    static boolean isVoidDomainMovementLocked(OpSukunaSnapshot s) {
        return s.domainField.singleDominantSureHit
                && s.domainField.dominantVoidLike
                && !s.selfDomain
                && !s.selfSimpleDomain
                && (s.brainDamageLevel >= 2.0 || hasExtremeVoidDebuff(s.sukuna));
    }

    static void setSkill(LivingEntity sukuna, CompoundTag nbt, double skill, double tick, boolean combatOnly) {
        nbt.putDouble("cnt_x", 0.0);
        nbt.putDouble("skill", skill);
        if (!sukuna.level().isClientSide()) {
            MobEffect cooldown = combatOnly ? JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get() : JujutsucraftModMobEffects.COOLDOWN_TIME.get();
            int duration = combatOnly ? (int) tick : Math.max(1, (int) tick / 2);
            sukuna.addEffect(new MobEffectInstance(cooldown, duration, 0, false, false));
            sukuna.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
        }
    }

    static void setWorldCut(LivingEntity sukuna, CompoundTag nbt, OpSukunaSnapshot s) {
        if (!hasWorldCut(sukuna)) {
            setSkill(sukuna, nbt, FAST_DISMANTLE, 50.0, false);
            return;
        }
        if (sukuna instanceof SukunaPerfectEntity && s.tick - s.memory.lastWorldCutTick < PERFECT_WORLD_CUT_COOLDOWN) {
            setSkill(sukuna, nbt, FAST_DISMANTLE, 50.0, false);
            return;
        }
        nbt.putDouble("cnt_x", 0.0);
        nbt.putDouble("cnt6", Math.max(nbt.getDouble("cnt6"), 5.0));
        nbt.putDouble("cnt7", 2.0);
        nbt.putBoolean("flag_dismantle", true);
        setSkill(sukuna, nbt, FAST_DISMANTLE, 100.0, false);
        s.memory.lastWorldCutTick = s.tick;
    }

    static void equipMahoragaWheel(LivingEntity sukuna, OpSukunaSnapshot s) {
        if (!s.canUseMahoraga || s.mahoragaWheel || s.trivialTarget) {
            return;
        }
        if (s.infinitySignal > 0.0 || s.targetThreat > 0.72 || s.lethalForecast || s.damageTakenBurst > 0.45) {
            sukuna.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()));
        }
    }

    static String targetKey(LivingEntity target) {
        return target.getStringUUID();
    }

    static String typeKey(LivingEntity target) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        return id == null ? target.getClass().getName() : id.toString();
    }

    record DomainCastStatus(double requestedSkill, boolean pending, boolean confirmed, String failedReason) {
    }



    static String classify(LivingEntity target, OpSukunaPlayerRead read, OpSukunaCombatStats targetStats, OpSukunaCombatStats typeStats, OpSukunaCombatStats archetypeStats) {
        if (isItadoriModulo(target)) return "MELEE_BURST_TANK";
        if (target.hasEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.INFINITY.get())
                || isGojoTarget(target, read)) return "INFINITY_USER";
        if (target.hasEffect(JujutsucraftaddonModMobEffects.BLACK_FLASH_CUT.get()) || target.hasEffect(JujutsucraftaddonModMobEffects.FATIGUE_BLACK_FLASH.get())
                || read.primaryTechnique == TechniqueIDs.ITADORI || read.secondaryTechnique == TechniqueIDs.ITADORI) return "BLACK_FLASH_USER";
        if (read.domainBias > 0.55 || targetStats.counterDomainAfterSukuna > 2.0 || typeStats.counterDomainAfterSukuna > 4.0) return "DOMAIN_COUNTER";
        if (read.healBias > 0.5 || targetStats.targetHealAvg > target.getMaxHealth() * 0.025) return "HEALER";
        if (read.tankBias > 0.55 || target.getMaxHealth() > 80.0) return "TANK";
        if (read.rangedBias > 0.55 || targetStats.rangedRatio() > 0.45 || typeStats.rangedRatio() > 0.55) return "RANGED_PRESSURE";
        if (read.aggression > 0.55 || targetStats.meleeRatio() > 0.45) return "MELEE_BURST";
        return "UNKNOWN_OP";
    }

    static double predictedDistance(LivingEntity sukuna, LivingEntity target) {
        Vec3 predicted = target.position().add(target.getDeltaMovement().scale(8.0));
        return predicted.distanceTo(sukuna.position());
    }

    static double idealDistance(String archetype, double infinitySignal, double tankScore, double healScore) {
        if (infinitySignal > 0.1) return 18.0;
        if ("MELEE_BURST_TANK".equals(archetype)) return 12.0;
        if ("MELEE_BURST".equals(archetype) || "BLACK_FLASH_USER".equals(archetype)) return 12.0;
        if ("RANGED_PRESSURE".equals(archetype)) return 16.0;
        if (tankScore > 0.65 || healScore > 0.55) return 24.0;
        return 14.0;
    }

    static boolean isItadoriModulo(LivingEntity target) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        String name = id == null ? target.getType().toString().toLowerCase() : id.toString().toLowerCase();
        return name.equals("jjsk:itadori_yuji_modulo")
                || name.equals("jujutsucrafts:itadori_yuji_modulo")
                || name.equals("jujutsucraftaddon:itadori_yuji_modulo")
                || name.contains("itadori_yuji_modulo");
    }

    enum CombatRelation {
        ACTIVE_ATTACKER,
        IMMINENT_HOSTILE,
        POTENTIAL_HOSTILE,
        PASSIVE_VALID,
        TACTICAL_OBJECT
    }

    static CombatRelation readCombatRelation(LevelAccessor world, LivingEntity sukuna, LivingEntity target, OpSukunaPlayerRead read, OpSukunaBrainMemory memory) {
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
        OpSukunaCombatStats stats = memory.target(targetKey(target));
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

    static boolean ownerChainAttackingSukuna(LevelAccessor world, LivingEntity sukuna, LivingEntity target) {
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

    static PowerProfile readPowerProfile(LivingEntity sukuna, LivingEntity target, OpSukunaPlayerRead read, OpSukunaCombatStats targetStats, OpSukunaCombatStats typeStats) {
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

    static double effectPower(LivingEntity target, OpSukunaPlayerRead read) {
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

    static KillEstimate estimateKill(LivingEntity sukuna, LivingEntity target, PowerProfile power, OpSukunaCombatStats targetStats, OpSukunaPlayerRead read) {
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

    static int damageBoostPower(LivingEntity target) {
        MobEffectInstance effect = target.getEffect(MobEffects.DAMAGE_BOOST);
        return effect == null ? 0 : effect.getAmplifier();
    }

    static double safeAttribute(LivingEntity target, Attribute attribute, double fallback) {
        try {
            return target.getAttribute(attribute) == null ? fallback : target.getAttributeValue(attribute);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    static boolean isJujutsuActor(LivingEntity target, OpSukunaPlayerRead read) {
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

    static boolean isTacticalObject(LivingEntity target) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        String name = id == null ? target.getType().toString().toLowerCase() : id.toString().toLowerCase();
        if (name.contains("rika") || name.contains("mahoraga") || name.contains("agito")) {
            return false;
        }
        return name.contains("barrier") || name.contains("veil") || name.contains("domain") || name.contains("shrine")
                || name.contains("clone") || name.contains("shadow") || name.contains("construct") || name.contains("cursed_spirit_ball")
                || name.contains("blue_entity") || name.contains("red_entity") || name.contains("cleave_web");
    }

    static double specialTier(LivingEntity target, OpSukunaPlayerRead read) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        String name = id == null ? target.getType().toString().toLowerCase() : id.toString().toLowerCase();
        if (name.contains("satushi") || name.contains("satuxi") || name.contains("satuchi")) {
            return 1.05;
        }
        if (name.contains("gojo") || name.contains("sukuna") || name.contains("mahoraga")
                || isGojoTarget(target, read)) {
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
    static double readBaseDataPower(LivingEntity target) {
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
    static double readSynchedDouble(LivingEntity target, String fieldName) {
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

    static class PowerProfile {
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

    static class KillEstimate {
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

    static class ThreatScan {
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

        static ThreatScan scan(LevelAccessor world, LivingEntity sukuna, LivingEntity current, OpSukunaBrainMemory memory) {
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

        static ThreatCandidate chooseBestCandidate(List<ThreatCandidate> candidates) {
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

    static class ThreatCandidate {
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

        static ThreatCandidate read(LivingEntity sukuna, LivingEntity target, LivingEntity current, OpSukunaBrainMemory memory) {
            double distance = sukuna.distanceTo(target);
            CompoundTag nbt = target.getPersistentData();
            OpSukunaCombatStats stats = memory.target(targetKey(target));
            OpSukunaCombatStats typeStats = memory.type(typeKey(target));
            OpSukunaPlayerRead read = OpSukunaPlayerRead.read(target);
            CombatRelation relation = readCombatRelation(sukuna.level(), sukuna, target, read, memory);
            PowerProfile power = readPowerProfile(sukuna, target, read, stats, typeStats);
            KillEstimate kill = estimateKill(sukuna, target, power, stats, read);
            double skill = nbt.getDouble("skill");
            boolean domain = target.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
            boolean castingDomain = skill == 20.0 || (isGojoTarget(target, read) && nbt.getDouble("cnt1") > 0.0 && nbt.getDouble("cnt1") < 40.0);
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

    static double scoreTargetSkill(double skill, OpSukunaCombatStats stats) {
        if (skill <= 0.0) return stats.damageTakenAvg > stats.damageDealtAvg ? 0.25 : 0.0;
        double repeat = stats.skillUseRatio(skill);
        double base = skill >= 20.0 ? 0.32 : 0.12;
        if (skill >= 100.0) base += 0.22;
        if (skill >= 1000.0) base += 0.22;
        return Mth.clamp(base + repeat * 0.42 + stats.damageTakenPeak * 0.01, 0.0, 1.0);
    }

    static double estimatePower(OpSukunaSnapshot s) {
        double hpPower = s.target.getMaxHealth() / Math.max(1.0, s.sukuna.getMaxHealth());
        double damagePower = (s.targetStats.damageTakenAvg + s.damageTaken) / Math.max(1.0, s.sukuna.getMaxHealth() * 0.08);
        double survivalPower = s.targetHealthRatio > 0.72 && s.targetStats.seenTicks > 200.0 ? 0.18 : 0.0;
        double domainPower = s.targetDomain || s.targetStats.domainTicks > 80.0 ? 0.25 : 0.0;
        return Mth.clamp(hpPower * 0.18 + damagePower * 0.32 + s.healScore * 0.15 + survivalPower + domainPower
                + s.read.outputLevel * 0.04 + s.targetPowerProfile.score * 0.55, 0.0, 1.0);
    }

    static double estimateThreat(OpSukunaSnapshot s) {
        double specialDefense = (s.infinitySignal > 0.0 ? 0.8 : 0.0)
                + (s.targetDomain || s.targetCastingDomain ? 0.55 : 0.0)
                + (s.targetSimpleDomain || s.targetHwb || s.targetNeutralization || s.targetDomainAmplification ? 0.25 : 0.0);
        double specialBody = (s.itadoriModulo ? 0.9 : 0.0)
                + (s.target instanceof Player ? 0.2 : 0.0)
                + (s.read.ultimate ? 0.2 : 0.0);
        return Mth.clamp(s.powerScore + s.targetSkillDanger * 0.35 + s.tankScore * 0.25 + s.healScore * 0.2
                + s.meleeThreat * 0.2 + s.rangeThreat * 0.2 + s.damageTakenBurst * 0.35 + specialDefense + specialBody, 0.0, 1.5);
    }

    static boolean isTrivialTarget(OpSukunaSnapshot s) {
        if (s.targetCombatRelation != CombatRelation.PASSIVE_VALID || s.targetPowerProfile.high) return false;
        if (s.target instanceof Player || s.itadoriModulo || s.infinitySignal > 0.0 || s.targetDomain || s.targetCastingDomain) return false;
        if (s.targetSimpleDomain || s.targetHwb || s.targetNeutralization || s.targetDomainAmplification || s.targetRegen) return false;
        if (s.target.getMaxHealth() > 40.0 || s.targetThreat >= 0.32) return false;
        return s.damageTakenBurst < 0.12 && s.targetSkillDanger < 0.25;
    }

    static String infinityBypassMode(LivingEntity sukuna, LivingEntity target, OpSukunaSnapshot s) {
        CompoundTag nbt = sukuna.getPersistentData();
        if (sukuna.hasEffect(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get())) return "DOMAIN_AMPLIFICATION";
        if (s.selfDomain) return "OWN_DOMAIN";
        if (s.targetNeutralization) return "TARGET_NEUTRALIZED";
        if (s.worldCutReady || nbt.getBoolean("flag_dismantle") && nbt.getDouble("cnt7") >= 1.0) return "WORLD_CUT";
        if ((s.mahoragaWheel || s.mahoragaExist) && s.gojoTarget && s.tick - s.memory.lastWorldCutTick > 80.0) return "MAHORAGA_ADAPTATION";
        if (s.antiInfinityBypass) return "ANTI_INFINITY_PROCEDURE";
        return "NONE";
    }

    static class AdaptationView {
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

        static AdaptationView create(OpSukunaSnapshot s) {
            double techniqueWeight = s.itadoriModulo ? 0.75 : TECHNIQUE_WEIGHT;
            double typeWeight = s.itadoriModulo ? 0.75 : TYPE_WEIGHT;
            double archetypeWeight = s.itadoriModulo ? 0.55 : ARCHETYPE_WEIGHT;
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
            addActionBias(actionBias, s.techniqueStats, techniqueWeight);
            addActionBias(actionBias, s.typeStats, typeWeight);
            addActionBias(actionBias, s.archetypeStats, archetypeWeight);
            double totalWeight = TARGET_WEIGHT + techniqueWeight + typeWeight + archetypeWeight;
            actionBias.replaceAll((key, value) -> Mth.clamp(value / totalWeight * confidence, -0.45, 0.42));
            return new AdaptationView(confidence, Mth.clamp(trap, 0.0, 1.0), Mth.clamp(evasion, 0.0, 1.0),
                    Mth.clamp(range, 0.0, 1.0), Mth.clamp(burst, 0.0, 1.0), preferred, Mth.clamp(safePunish, 0.0, 1.0), actionBias);
        }

        double actionBias(String action) {
            return learnedActionBias.getOrDefault(action, 0.0);
        }

        private interface StatReader {
            double read(OpSukunaCombatStats stats);
        }

        static double weighted(OpSukunaSnapshot s, StatReader reader) {
            double total = TARGET_WEIGHT + TECHNIQUE_WEIGHT + TYPE_WEIGHT + ARCHETYPE_WEIGHT;
            return (reader.read(s.targetStats) * TARGET_WEIGHT
                    + reader.read(s.techniqueStats) * TECHNIQUE_WEIGHT
                    + reader.read(s.typeStats) * TYPE_WEIGHT
                    + reader.read(s.archetypeStats) * ARCHETYPE_WEIGHT) / total;
        }

        static double weightedRatio(OpSukunaSnapshot s, StatReader reader) {
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

        static double weightedDistance(OpSukunaSnapshot s) {
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

        static double distanceContribution(OpSukunaCombatStats stats, double weight) {
            return stats.distanceAvg > 0.0 ? stats.distanceAvg * stats.seenTicks * weight : 0.0;
        }

        static double distanceWeight(OpSukunaCombatStats stats, double weight) {
            return stats.distanceAvg > 0.0 ? stats.seenTicks * weight : 0.0;
        }

        static void addActionBias(Map<String, Double> out, OpSukunaCombatStats stats, double weight) {
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






}
