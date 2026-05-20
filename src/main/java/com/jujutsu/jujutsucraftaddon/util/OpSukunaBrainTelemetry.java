package com.jujutsu.jujutsucraftaddon.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jujutsu.jujutsucraftaddon.JujutsucraftaddonMod;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.entity.SukunaFushiguroEntity;
import net.mcreator.jujutsucraft.entity.SukunaPerfectEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.stream.Stream;

@Mod.EventBusSubscriber
public final class OpSukunaBrainTelemetry {
    private static final Path DIR = FMLPaths.GAMEDIR.get().resolve("jjkur-op-ai-metrics");
    private static final Map<String, RuntimeFight> RUNTIME_FIGHTS = new HashMap<>();
    private static final Map<String, String> CURRENT_FIGHT_BY_SUKUNA = new HashMap<>();
    private static final Map<String, BenchmarkContext> BENCHMARK_CONTEXT_BY_FIGHT = new HashMap<>();
    private static final Map<String, BenchmarkContext> BENCHMARK_CONTEXT_BY_SUKUNA = new HashMap<>();
    private static final List<PendingDamage> PENDING_DAMAGE = new ArrayList<>();
    private static final Map<String, ResolvedDamage> RECENT_RESOLVED_DAMAGE = new HashMap<>();
    private static BufferedWriter writer;
    private static LocalDate writerDate;

    private OpSukunaBrainTelemetry() {
    }

    public static boolean enabled(LevelAccessor world) {
        return world != null && !world.isClientSide()
                && devTelemetryAllowed()
                && world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_OP_SUKUNA_METRICS);
    }

    public static boolean devTelemetryAllowed() {
        return Boolean.getBoolean("jjku.opSukunaMetrics")
                || Boolean.getBoolean("jjku.devMetrics")
                || "true".equalsIgnoreCase(System.getenv("JJKU_OP_SUKUNA_METRICS_DEV"));
    }

    public static boolean isOpSukunaSubject(LivingEntity entity) {
        return entity instanceof SukunaPerfectEntity
                || entity instanceof SukunaFushiguroEntity
                || entity instanceof com.jujutsu.jujutsucraftaddon.entity.SukunaFushiguroEntity
                || entity instanceof com.jujutsu.jujutsucraftaddon.entity.SukunaMangaEntity;
    }

    public static synchronized Path metricsDir() {
        return DIR;
    }

    public static synchronized boolean hasBenchmarkContext(LivingEntity sukuna) {
        return sukuna != null && BENCHMARK_CONTEXT_BY_SUKUNA.containsKey(sukuna.getStringUUID());
    }

    public static synchronized void registerBenchmarkFight(String fightId, String benchmarkId, String scenario, String variant, int runIndex, int arenaIndex) {
        if (fightId == null || fightId.isEmpty()) {
            return;
        }
        BENCHMARK_CONTEXT_BY_FIGHT.put(fightId, new BenchmarkContext(benchmarkId, scenario, variant, runIndex, arenaIndex));
    }

    public static synchronized void registerBenchmarkFight(String fightId, LivingEntity sukuna, LivingEntity target, String benchmarkId, String scenario,
                                                           String variant, int runIndex, int arenaIndex) {
        registerBenchmarkFight(fightId, benchmarkId, scenario, variant, runIndex, arenaIndex);
        if (sukuna != null) {
            BenchmarkContext context = new BenchmarkContext(benchmarkId, scenario, variant, runIndex, arenaIndex);
            BENCHMARK_CONTEXT_BY_SUKUNA.put(sukuna.getStringUUID(), context);
            CURRENT_FIGHT_BY_SUKUNA.put(sukuna.getStringUUID(), fightId);
            RuntimeFight fight = RUNTIME_FIGHTS.computeIfAbsent(fightId, RuntimeFight::new);
            fight.sukunaUuid = sukuna.getStringUUID();
            if (target != null) {
                fight.targetUuid = target.getStringUUID();
                fight.targetType = typeName(target);
                fight.targetName = target.getName().getString();
            }
        }
    }

    public static synchronized void clearBenchmarkContext(String benchmarkId) {
        if (benchmarkId == null || benchmarkId.isEmpty()) {
            return;
        }
        BENCHMARK_CONTEXT_BY_FIGHT.entrySet().removeIf(entry -> benchmarkId.equals(entry.getValue().benchmarkId));
        BENCHMARK_CONTEXT_BY_SUKUNA.entrySet().removeIf(entry -> benchmarkId.equals(entry.getValue().benchmarkId));
    }

    public static synchronized void recordBenchmarkEvent(LevelAccessor world, String benchmarkId, String scenario, String variant, int runIndex,
                                                         int arenaIndex, String event, String outcome, String details) {
        if (!enabled(world)) {
            return;
        }
        StringBuilder json = new StringBuilder(512);
        json.append('{');
        field(json, "event", event == null || event.isEmpty() ? "benchmark_event" : event).append(',');
        number(json, "schema", 1.0).append(',');
        field(json, "dev_only", "true").append(',');
        number(json, "game_time", world.dayTime()).append(',');
        field(json, "benchmark_id", benchmarkId).append(',');
        field(json, "scenario", scenario).append(',');
        field(json, "variant", variant).append(',');
        number(json, "run_index", runIndex).append(',');
        number(json, "arena_index", arenaIndex).append(',');
        field(json, "outcome", outcome == null ? "" : outcome).append(',');
        field(json, "details", details == null ? "" : details);
        json.append('}');
        write(json.toString());
    }

    public static synchronized void recordBenchmarkSample(LevelAccessor world, String benchmarkId, String scenario, String variant, int runIndex,
                                                          int arenaIndex, int age, LivingEntity sukuna, LivingEntity target, String phase) {
        if (!enabled(world) || sukuna == null || target == null) {
            return;
        }
        String fightId = sukuna.getStringUUID() + ":" + target.getStringUUID();
        StringBuilder json = new StringBuilder(1536);
        json.append('{');
        field(json, "event", "benchmark_sample").append(',');
        number(json, "schema", 1.0).append(',');
        field(json, "dev_only", "true").append(',');
        number(json, "game_time", world.dayTime()).append(',');
        number(json, "sukuna_tick", sukuna.tickCount).append(',');
        field(json, "fight_id", fightId).append(',');
        field(json, "benchmark_id", benchmarkId).append(',');
        field(json, "scenario", scenario).append(',');
        field(json, "variant", variant).append(',');
        number(json, "run_index", runIndex).append(',');
        number(json, "arena_index", arenaIndex).append(',');
        number(json, "age", age).append(',');
        field(json, "phase", phase == null ? "" : phase).append(',');
        field(json, "sukuna_uuid", sukuna.getStringUUID()).append(',');
        field(json, "target_uuid", target.getStringUUID()).append(',');
        field(json, "sukuna_type", typeName(sukuna)).append(',');
        field(json, "target_type", typeName(target)).append(',');
        number(json, "sukuna_health", sukuna.getHealth()).append(',');
        number(json, "sukuna_max_health", sukuna.getMaxHealth()).append(',');
        number(json, "sukuna_absorption", sukuna.getAbsorptionAmount()).append(',');
        number(json, "target_health", target.getHealth()).append(',');
        number(json, "target_max_health", target.getMaxHealth()).append(',');
        number(json, "target_absorption", target.getAbsorptionAmount()).append(',');
        number(json, "distance", sukuna.distanceTo(target)).append(',');
        number(json, "sukuna_x", sukuna.getX()).append(',');
        number(json, "sukuna_y", sukuna.getY()).append(',');
        number(json, "sukuna_z", sukuna.getZ()).append(',');
        number(json, "target_x", target.getX()).append(',');
        number(json, "target_y", target.getY()).append(',');
        number(json, "target_z", target.getZ()).append(',');
        number(json, "sukuna_hurt_time", sukuna.hurtTime).append(',');
        number(json, "target_hurt_time", target.hurtTime).append(',');
        number(json, "sukuna_fall_distance", sukuna.fallDistance).append(',');
        bool(json, "sukuna_on_ground", sukuna.onGround()).append(',');
        bool(json, "target_on_ground", target.onGround()).append(',');
        field(json, "sukuna_effects", effectsSnapshot(sukuna)).append(',');
        field(json, "target_effects", effectsSnapshot(target)).append(',');
        appendLocalEntityCounts(world, sukuna, json).append(',');
        number(json, "sukuna_skill", sukuna.getPersistentData().getDouble("skill")).append(',');
        number(json, "sukuna_cnt_target", sukuna.getPersistentData().getDouble("cnt_target")).append(',');
        number(json, "sukuna_cooldown", sukuna.getPersistentData().getDouble("COOLDOWN_TICKS")).append(',');
        number(json, "sukuna_last_hook_game_time", sukuna.getPersistentData().getLong("JJKUR_OP_AI_LAST_HOOK_GAME_TIME")).append(',');
        number(json, "sukuna_last_hook_tick", sukuna.getPersistentData().getInt("JJKUR_OP_AI_LAST_HOOK_TICK")).append(',');
        number(json, "sukuna_domain_expansion", sukuna.getPersistentData().getDouble("DomainExpansion")).append(',');
        number(json, "target_skill", target.getPersistentData().getDouble("skill")).append(',');
        number(json, "target_cnt_target", target.getPersistentData().getDouble("cnt_target")).append(',');
        number(json, "target_domain_expansion", target.getPersistentData().getDouble("DomainExpansion")).append(',');
        field(json, "sukuna_mob_target", mobTargetUuid(sukuna)).append(',');
        field(json, "target_mob_target", mobTargetUuid(target));
        json.append('}');
        write(json.toString());
    }

    public static synchronized void recordAiGate(LevelAccessor world, LivingEntity sukuna, LivingEntity target, String stage, String reason) {
        if (!enabled(world) || sukuna == null) {
            return;
        }
        BenchmarkContext context = BENCHMARK_CONTEXT_BY_SUKUNA.get(sukuna.getStringUUID());
        if (context == null) {
            return;
        }
        LivingEntity effectiveTarget = target != null ? target : (sukuna instanceof net.minecraft.world.entity.Mob mob ? mob.getTarget() : null);
        String targetUuid = effectiveTarget == null ? "" : effectiveTarget.getStringUUID();
        String targetType = effectiveTarget == null ? "" : typeName(effectiveTarget);
        StringBuilder json = new StringBuilder(640);
        json.append('{');
        field(json, "event", "ai_gate").append(',');
        number(json, "schema", 1.0).append(',');
        field(json, "dev_only", "true").append(',');
        number(json, "game_time", world.dayTime()).append(',');
        number(json, "sukuna_tick", sukuna.tickCount).append(',');
        field(json, "benchmark_id", context.benchmarkId).append(',');
        field(json, "scenario", context.scenario).append(',');
        field(json, "variant", context.variant).append(',');
        number(json, "run_index", context.runIndex).append(',');
        number(json, "arena_index", context.arenaIndex).append(',');
        field(json, "stage", stage == null ? "" : stage).append(',');
        field(json, "reason", reason == null ? "" : reason).append(',');
        field(json, "sukuna_uuid", sukuna.getStringUUID()).append(',');
        field(json, "target_uuid", targetUuid).append(',');
        field(json, "target_type", targetType).append(',');
        number(json, "cnt_target", sukuna.getPersistentData().getDouble("cnt_target")).append(',');
        number(json, "skill", sukuna.getPersistentData().getDouble("skill")).append(',');
        bool(json, "op_ai_controlled", sukuna.getPersistentData().getBoolean("JJKUR_OP_AI_CONTROLLED")).append(',');
        field(json, "mob_target_uuid", mobTargetUuid(sukuna));
        json.append('}');
        write(json.toString());
    }

    private static StringBuilder appendLocalEntityCounts(LevelAccessor world, LivingEntity sukuna, StringBuilder json) {
        int living = 0;
        int projectiles = 0;
        int benchmarkTagged = 0;
        if (world instanceof net.minecraft.world.level.Level level) {
            AABB box = sukuna.getBoundingBox().inflate(36.0);
            for (Entity entity : level.getEntities((Entity) null, box)) {
                if (entity instanceof LivingEntity) {
                    living++;
                }
                if (entity instanceof Projectile) {
                    projectiles++;
                }
                if (entity.getTags().contains("jjku_op_sukuna_benchmark")) {
                    benchmarkTagged++;
                }
            }
        }
        number(json, "nearby_living_entities", living).append(',');
        number(json, "nearby_projectiles", projectiles).append(',');
        return number(json, "nearby_benchmark_entities", benchmarkTagged);
    }

    @SubscribeEvent(receiveCanceled = true)
    public static synchronized void onLivingAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        LivingEntity sukuna = opSukunaFromSource(event.getSource());
        if (sukuna == null || target == sukuna || !enabled(target.level())) {
            return;
        }
        if (!shouldRecordDetailedOutgoingDamage(sukuna, target)) {
            return;
        }
        String fightId = sukuna.getStringUUID() + ":" + target.getStringUUID();
        PendingDamage pending = new PendingDamage(sukuna, target, event.getSource(), event.getAmount(), fightId);
        PENDING_DAMAGE.add(pending);
        rememberOutgoingDamage(sukuna, target, fightId, "attempt=" + fmt(event.getAmount()) + ",source=" + damageSourceName(event.getSource()));
        writeDamageAttempt(target.level(), sukuna, target, fightId, pending);
    }

    @SubscribeEvent
    public static synchronized void onLivingHurt(LivingHurtEvent event) {
        updatePendingPipeline(event.getEntity(), event.getSource(), event.getAmount(), false);
    }

    @SubscribeEvent
    public static synchronized void onLivingDamage(LivingDamageEvent event) {
        updatePendingPipeline(event.getEntity(), event.getSource(), event.getAmount(), true);
        LivingEntity sukuna = event.getEntity();
        if (!isOpSukunaSubject(sukuna) || !enabled(sukuna.level())) {
            return;
        }
        String sukunaId = sukuna.getStringUUID();
        String fightId = CURRENT_FIGHT_BY_SUKUNA.getOrDefault(sukunaId, sukunaId + ":unknown");
        RuntimeFight fight = RUNTIME_FIGHTS.computeIfAbsent(fightId, RuntimeFight::new);
        fight.sukunaUuid = sukunaId;
        fight.addDamage(damageSummary(sukuna.level(), sukuna, event.getSource(), event.getAmount()));
        writeSukunaDamageResolved(sukuna.level(), sukuna, fightId, event);
    }

    @SubscribeEvent
    public static synchronized void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_DAMAGE.isEmpty()) {
            return;
        }
        Map<String, List<PendingDamage>> readyGroups = new HashMap<>();
        Iterator<PendingDamage> iterator = PENDING_DAMAGE.iterator();
        while (iterator.hasNext()) {
            PendingDamage pending = iterator.next();
            LivingEntity target = pending.target;
            if (target == null || target.level().isClientSide()) {
                iterator.remove();
                continue;
            }
            long now = target.level().getGameTime();
            if (now <= pending.gameTime && target.isAlive()) {
                continue;
            }
            readyGroups.computeIfAbsent(pending.groupKey(), key -> new ArrayList<>()).add(pending);
            iterator.remove();
        }
        for (List<PendingDamage> group : readyGroups.values()) {
            PendingDamage first = group.get(0);
            LivingEntity target = first.target;
            long now = target.level().getGameTime();
            double finalHealth = target.isAlive() ? healthAndAbsorption(target) : 0.0;
            double preHealth = 0.0;
            double attemptedAmount = 0.0;
            double hurtAmount = 0.0;
            double damageAmount = 0.0;
            for (PendingDamage pending : group) {
                preHealth = Math.max(preHealth, pending.preHealth);
                attemptedAmount += pending.attemptedAmount;
                hurtAmount += pending.hurtAmount;
                damageAmount += pending.damageAmount;
            }
            double actualDamage = Math.max(0.0, preHealth - finalHealth);
            ResolvedDamage previous = RECENT_RESOLVED_DAMAGE.get(first.fightId);
            double aggregateDamage = previous != null && now - previous.gameTime <= 4 ? previous.actualDamage + actualDamage : actualDamage;
            RECENT_RESOLVED_DAMAGE.put(first.fightId, new ResolvedDamage(aggregateDamage, now));
            rememberOutgoingDamage(first.sukuna, target, first.fightId,
                    "actual=" + fmt(actualDamage) + ",attempt=" + fmt(attemptedAmount)
                            + ",hurt=" + fmt(hurtAmount) + ",damage=" + fmt(damageAmount)
                            + ",attempt_count=" + group.size() + ",source=" + damageSourceName(first.source));
            writeDamageResolved(target.level(), first.sukuna, target, first.fightId, first, actualDamage,
                    attemptedAmount, hurtAmount, damageAmount, group.size(), preHealth);
        }
        long cleanupTick = event.getServer().overworld().getGameTime();
        RECENT_RESOLVED_DAMAGE.entrySet().removeIf(entry -> cleanupTick - entry.getValue().gameTime > 40);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity sukuna = event.getEntity();
        if (isOpSukunaSubject(sukuna) && enabled(sukuna.level())) {
            recordSukunaDeath(sukuna.level(), sukuna, event.getSource());
            return;
        }
        if (!enabled(event.getEntity().level())) {
            return;
        }
        LivingEntity killerSukuna = opSukunaFromSource(event.getSource());
        if (killerSukuna != null && killerSukuna != event.getEntity() && hasBenchmarkContext(killerSukuna)) {
            writeTargetDeathResolved(event.getEntity().level(), killerSukuna, event.getEntity(), event.getSource());
        }
    }

    public static void recordDecision(LevelAccessor world, LivingEntity sukuna, LivingEntity target, Decision decision) {
        if (!enabled(world) || sukuna == null || target == null || decision == null) {
            return;
        }
        String fightId = sukuna.getStringUUID() + ":" + target.getStringUUID();
        if (hasBenchmarkContext(sukuna) && shouldSkipBenchmarkDecision(fightId, sukuna, decision)) {
            return;
        }
        StringBuilder json = new StringBuilder(2048);
        json.append('{');
        field(json, "event", "decision").append(',');
        number(json, "schema", 3.0).append(',');
        field(json, "dev_only", "true").append(',');
        number(json, "game_time", world.dayTime()).append(',');
        number(json, "sukuna_tick", sukuna.tickCount).append(',');
        field(json, "fight_id", fightId).append(',');
        appendBenchmarkContext(json, fightId, sukuna.getStringUUID());
        field(json, "sukuna_uuid", sukuna.getStringUUID()).append(',');
        field(json, "target_uuid", target.getStringUUID()).append(',');
        field(json, "sukuna_type", typeName(sukuna)).append(',');
        field(json, "target_type", typeName(target)).append(',');
        field(json, "target_name", target.getName().getString()).append(',');
        field(json, "action", decision.action).append(',');
        field(json, "kind", decision.kind).append(',');
        field(json, "ai_mode", decision.aiMode).append(',');
        field(json, "top_scores", decision.topScores).append(',');
        number(json, "score", decision.score).append(',');
        number(json, "adjusted_score", decision.adjustedScore).append(',');
        number(json, "skill", decision.skill).append(',');
        number(json, "self_health", decision.selfHealth).append(',');
        number(json, "target_health", decision.targetHealth).append(',');
        number(json, "self_health_ratio", decision.selfHealthRatio).append(',');
        number(json, "target_health_ratio", decision.targetHealthRatio).append(',');
        number(json, "distance", decision.distance).append(',');
        number(json, "hitbox_distance", decision.hitboxDistance).append(',');
        number(json, "center_distance", decision.centerDistance).append(',');
        number(json, "predicted_distance", decision.predictedDistance).append(',');
        number(json, "damage_dealt", decision.damageDealt).append(',');
        number(json, "damage_taken", decision.damageTaken).append(',');
        number(json, "target_skill", decision.targetSkill).append(',');
        field(json, "archetype", decision.archetype).append(',');
        field(json, "technique_key", decision.techniqueKey).append(',');
        number(json, "primary_technique", decision.primaryTechnique).append(',');
        number(json, "secondary_technique", decision.secondaryTechnique).append(',');
        number(json, "target_threat", decision.targetThreat).append(',');
        number(json, "target_power", decision.targetPower).append(',');
        number(json, "melee_threat", decision.meleeThreat).append(',');
        number(json, "range_threat", decision.rangeThreat).append(',');
        number(json, "pressure", decision.pressure).append(',');
        number(json, "opportunity", decision.opportunity).append(',');
        number(json, "immediate_threat", decision.immediateThreat).append(',');
        number(json, "survival_urgency", decision.survivalUrgency).append(',');
        number(json, "incoming_kill_risk", decision.incomingKillRisk).append(',');
        number(json, "hit_confidence", decision.hitConfidence).append(',');
        number(json, "whiff_opportunity", decision.whiffOpportunity).append(',');
        number(json, "stuck_level", decision.stuckLevel).append(',');
        number(json, "group_pressure", decision.groupPressure).append(',');
        number(json, "group_active_pressure", decision.groupActivePressure).append(',');
        number(json, "group_apex_pressure", decision.groupApexPressure).append(',');
        number(json, "rct_strain", decision.rctStrain).append(',');
        number(json, "brain_damage", decision.brainDamage).append(',');
        number(json, "ideal_distance", decision.idealDistance).append(',');
        bool(json, "gojo", decision.gojo).append(',');
        bool(json, "target_domain", decision.targetDomain).append(',');
        bool(json, "target_casting_domain", decision.targetCastingDomain).append(',');
        bool(json, "target_cooldown", decision.targetCooldown).append(',');
        bool(json, "target_unstable", decision.targetUnstable).append(',');
        bool(json, "target_dodge", decision.targetDodge).append(',');
        bool(json, "target_counter", decision.targetCounter).append(',');
        bool(json, "target_guard", decision.targetGuard).append(',');
        bool(json, "target_escaping", decision.targetEscaping).append(',');
        bool(json, "target_whiffed", decision.targetWhiffed).append(',');
        bool(json, "target_overextended", decision.targetOverextended).append(',');
        bool(json, "clear_shot", decision.clearShot).append(',');
        bool(json, "path_blocked", decision.pathBlocked).append(',');
        bool(json, "normal_ready", decision.normalReady).append(',');
        bool(json, "passive_ready", decision.passiveReady).append(',');
        bool(json, "domain_ready", decision.domainReady).append(',');
        bool(json, "domain_blocked_by_cooldown", decision.domainBlockedByCooldown).append(',');
        field(json, "domain_block_reason", decision.domainBlockReason).append(',');
        bool(json, "domain_recently_opened", decision.domainRecentlyOpened).append(',');
        bool(json, "self_domain", decision.selfDomain).append(',');
        number(json, "domain_cast_requested_skill", decision.domainCastRequestedSkill).append(',');
        bool(json, "domain_cast_pending", decision.domainCastPending).append(',');
        bool(json, "domain_cast_confirmed", decision.domainCastConfirmed).append(',');
        field(json, "domain_cast_failed_reason", decision.domainCastFailedReason).append(',');
        bool(json, "world_cut_capable", decision.worldCutCapable).append(',');
        bool(json, "world_cut_ready", decision.worldCutReady).append(',');
        number(json, "world_cut_cooldown", decision.worldCutCooldown).append(',');
        field(json, "world_cut_block_reason", decision.worldCutBlockReason).append(',');
        bool(json, "trivial_target", decision.trivialTarget).append(',');
        field(json, "sukuna_form", decision.sukunaForm).append(',');
        bool(json, "can_transform_heian", decision.canTransformHeian).append(',');
        field(json, "heian_emergency_reason", decision.heianEmergencyReason).append(',');
        bool(json, "ten_shadows_available", decision.tenShadowsAvailable).append(',');
        bool(json, "mahoraga_available", decision.mahoragaAvailable).append(',');
        bool(json, "enemy_domain_trap", decision.enemyDomainTrap).append(',');
        bool(json, "domain_escape_window", decision.domainEscapeWindow).append(',');
        number(json, "domain_response_ticks", decision.domainResponseTicks).append(',');
        number(json, "active_enemy_domains", decision.activeEnemyDomains).append(',');
        number(json, "active_void_domains", decision.activeVoidDomains).append(',');
        bool(json, "domain_clash_suppressed", decision.domainClashSuppressed).append(',');
        bool(json, "single_dominant_sure_hit", decision.singleDominantSureHit).append(',');
        number(json, "simple_domain_duration", decision.simpleDomainDuration).append(',');
        number(json, "simple_domain_cooldown", decision.simpleDomainCooldown).append(',');
        bool(json, "anti_domain_gap_imminent", decision.antiDomainGapImminent).append(',');
        field(json, "dominant_domain_owner", decision.dominantDomainOwner).append(',');
        field(json, "dominant_domain_profile", decision.dominantDomainProfile).append(',');
        bool(json, "purple_threat", decision.purpleThreat).append(',');
        number(json, "purple_risk", decision.purpleRisk).append(',');
        bool(json, "purple_evade", decision.purpleEvade).append(',');
        bool(json, "purple_hit", decision.purpleHit).append(',');
        bool(json, "purple_avoided", decision.purpleAvoided).append(',');
        bool(json, "stuck", decision.stuck).append(',');
        bool(json, "infinity_waste", decision.infinityWaste).append(',');
        number(json, "adaptation_confidence", decision.adaptationConfidence).append(',');
        number(json, "trap_pressure", decision.trapPressure).append(',');
        number(json, "evasion_pressure", decision.evasionPressure).append(',');
        number(json, "range_pressure", decision.rangePressure).append(',');
        field(json, "domain_plan", decision.domainPlan).append(',');
        number(json, "domain_clear_value", decision.domainClearValue).append(',');
        number(json, "domain_sustain_cost", decision.domainSustainCost).append(',');
        number(json, "domain_recovery_risk", decision.domainRecoveryRisk).append(',');
        number(json, "domain_counter_risk", decision.domainCounterRisk).append(',');
        number(json, "domain_cast_score", decision.domainCastScore).append(',');
        number(json, "domain_hwb_score", decision.domainHwbScore).append(',');
        number(json, "domain_punish_score", decision.domainPunishScore).append(',');
        number(json, "domain_score", decision.domainScore).append(',');
        field(json, "domain_deliberation", decision.domainDeliberation).append(',');
        field(json, "domain_phase", decision.domainPhase).append(',');
        number(json, "domain_commit_deadline", decision.domainCommitDeadline).append(',');
        field(json, "domain_gate_reason", decision.domainGateReason).append(',');
        field(json, "domain_profile", decision.domainProfile).append(',');
        field(json, "chosen_domain_option", decision.chosenDomainOption).append(',');
        bool(json, "escape_feasible", decision.escapeFeasible).append(',');
        number(json, "world_cut_domain_value", decision.worldCutDomainValue).append(',');
        number(json, "counter_domain_value", decision.counterDomainValue).append(',');
        number(json, "tank_domain_value", decision.tankDomainValue).append(',');
        field(json, "infinity_bypass_mode", decision.infinityBypassMode).append(',');
        field(json, "melee_unlock_reason", decision.meleeUnlockReason).append(',');
        number(json, "preferred_range", decision.preferredRange).append(',');
        number(json, "safe_punish_window", decision.safePunishWindow).append(',');
        number(json, "learned_burst_risk", decision.learnedBurstRisk).append(',');
        number(json, "range_action_streak", decision.rangeActionStreak).append(',');
        number(json, "no_impact_action_streak", decision.noImpactActionStreak).append(',');
        bool(json, "yuji_burst_duel", decision.yujiBurstDuel).append(',');
        bool(json, "action_started", decision.actionStarted).append(',');
        field(json, "block_reason", decision.blockReason).append(',');
        field(json, "fallback_action", decision.fallbackAction).append(',');
        number(json, "void_exposure", decision.voidExposure).append(',');
        bool(json, "simple_domain_ready", decision.simpleDomainReady).append(',');
        bool(json, "simple_domain_key_blocked", decision.simpleDomainKeyBlocked).append(',');
        field(json, "simple_domain_key_source", decision.simpleDomainKeySource).append(',');
        bool(json, "domain_amplification_ready", decision.domainAmplificationReady).append(',');
        bool(json, "rct_ready", decision.rctReady).append(',');
        bool(json, "shrine_mode", decision.shrineMode).append(',');
        bool(json, "fuga_active", decision.fugaActive).append(',');
        number(json, "fuga_charge", decision.fugaCharge).append(',');
        number(json, "fuga_charge_max", decision.fugaChargeMax).append(',');
        number(json, "fuga_charge_rate", decision.fugaChargeRate).append(',');
        number(json, "fuga_hold_ticks", decision.fugaHoldTicks).append(',');
        field(json, "fuga_release_reason", decision.fugaReleaseReason);
        json.append('}');
        rememberDecision(world, sukuna, target, fightId, decision);
        write(json.toString());
        writeActionResult(world, sukuna, target, fightId, decision);
        writeDomainState(world, sukuna, target, fightId, decision);
        writeMovementState(world, sukuna, target, fightId, decision);
        if (decision.action.startsWith("SIMPLE_DOMAIN") && !decision.actionStarted) {
            writeSimpleDomainFailed(world, sukuna, target, fightId, decision);
        }
        if (decision.selfHealth <= 0.0 || decision.targetHealth <= 0.0) {
            writeFightOutcome(world, sukuna, target, fightId, decision);
        }
    }

    private static boolean shouldSkipBenchmarkDecision(String fightId, LivingEntity sukuna, Decision decision) {
        String profile = benchmarkTelemetryProfile();
        if ("full".equals(profile)) {
            return false;
        }
        int existing = benchmarkDecisionCount(fightId);
        if (existing < 16) {
            return false;
        }
        if (isBenchmarkCriticalDecision(decision)) {
            return false;
        }
        int stride = "minimal".equals(profile) ? 6 : 3;
        return Math.floorMod(sukuna.tickCount, stride) != 0;
    }

    private static boolean isBenchmarkCriticalDecision(Decision decision) {
        return !decision.actionStarted
                || (decision.blockReason != null && !"ready".equals(decision.blockReason) && !decision.blockReason.isEmpty())
                || (decision.fallbackAction != null && !decision.fallbackAction.isEmpty())
                || decision.damageDealt > 0.0
                || decision.damageTaken > 0.0
                || decision.targetDomain
                || decision.targetCastingDomain
                || decision.singleDominantSureHit
                || decision.domainCastPending
                || decision.domainCastConfirmed
                || decision.worldCutReady
                || decision.selfHealthRatio <= 0.38
                || decision.noImpactActionStreak >= 3.0
                || decision.rangeActionStreak >= 3.0;
    }

    private static String benchmarkTelemetryProfile() {
        String prop = System.getProperty("jjku.opSukunaBenchTelemetryProfile", "");
        if (prop != null && !prop.isEmpty()) {
            return prop.toLowerCase(java.util.Locale.ROOT);
        }
        String env = System.getenv("JJKU_OP_SUKUNA_BENCH_TELEMETRY_PROFILE");
        if (env != null && !env.isEmpty()) {
            return env.toLowerCase(java.util.Locale.ROOT);
        }
        return "balanced";
    }

    private static synchronized int benchmarkDecisionCount(String fightId) {
        RuntimeFight fight = RUNTIME_FIGHTS.get(fightId);
        return fight == null ? 0 : fight.decisions.size();
    }

    public static synchronized double recentActualDamageDealt(LivingEntity sukuna, LivingEntity target, long maxAgeTicks) {
        if (sukuna == null || target == null) {
            return -1.0;
        }
        ResolvedDamage resolved = RECENT_RESOLVED_DAMAGE.get(sukuna.getStringUUID() + ":" + target.getStringUUID());
        if (resolved == null) {
            return -1.0;
        }
        long now = sukuna.level().getGameTime();
        return now - resolved.gameTime <= maxAgeTicks ? resolved.actualDamage : -1.0;
    }

    private static void writeActionResult(LevelAccessor world, LivingEntity sukuna, LivingEntity target, String fightId, Decision decision) {
        StringBuilder json = eventBase(world, sukuna, target, fightId, "action_result", 512);
        field(json, "action", decision.action).append(',');
        field(json, "kind", decision.kind).append(',');
        bool(json, "started", decision.actionStarted).append(',');
        field(json, "block_reason", decision.blockReason).append(',');
        field(json, "fallback_action", decision.fallbackAction).append(',');
        number(json, "damage_dealt", decision.damageDealt).append(',');
        number(json, "damage_taken", decision.damageTaken).append(',');
        bool(json, "no_impact", decision.damageDealt <= 0.0 && decision.damageTaken <= 0.0).append(',');
        bool(json, "blocked", !decision.actionStarted || !"ready".equals(decision.blockReason));
        json.append('}');
        write(json.toString());
    }

    private static void writeDomainState(LevelAccessor world, LivingEntity sukuna, LivingEntity target, String fightId, Decision decision) {
        StringBuilder json = eventBase(world, sukuna, target, fightId, "domain_state", 768);
        number(json, "active_enemy_domains", decision.activeEnemyDomains).append(',');
        number(json, "active_void_domains", decision.activeVoidDomains).append(',');
        bool(json, "gojo_muryokusho", decision.gojo || "GOJO_MURYO_GLOBAL".equals(decision.domainProfile)).append(',');
        bool(json, "self_domain", decision.selfDomain).append(',');
        number(json, "domain_cast_requested_skill", decision.domainCastRequestedSkill).append(',');
        bool(json, "domain_cast_pending", decision.domainCastPending).append(',');
        bool(json, "domain_cast_confirmed", decision.domainCastConfirmed).append(',');
        field(json, "domain_cast_failed_reason", decision.domainCastFailedReason).append(',');
        bool(json, "clash", decision.domainClashSuppressed).append(',');
        bool(json, "single_dominant_sure_hit", decision.singleDominantSureHit).append(',');
        field(json, "dominant_domain_owner", decision.dominantDomainOwner).append(',');
        field(json, "dominant_domain_profile", decision.dominantDomainProfile).append(',');
        number(json, "simple_domain_duration", decision.simpleDomainDuration).append(',');
        number(json, "simple_domain_cooldown", decision.simpleDomainCooldown).append(',');
        bool(json, "simple_domain_ready", decision.simpleDomainReady).append(',');
        bool(json, "anti_domain_gap_imminent", decision.antiDomainGapImminent).append(',');
        number(json, "domain_response_ticks", decision.domainResponseTicks).append(',');
        field(json, "chosen_action", decision.action);
        json.append('}');
        write(json.toString());
    }

    private static void writeMovementState(LevelAccessor world, LivingEntity sukuna, LivingEntity target, String fightId, Decision decision) {
        StringBuilder json = eventBase(world, sukuna, target, fightId, "movement_state", 512);
        bool(json, "stuck", decision.stuck).append(',');
        bool(json, "path_blocked", decision.pathBlocked).append(',');
        bool(json, "escape_feasible", decision.escapeFeasible).append(',');
        number(json, "distance", decision.distance).append(',');
        number(json, "predicted_distance", decision.predictedDistance).append(',');
        number(json, "stuck_level", decision.stuckLevel).append(',');
        field(json, "action", decision.action).append(',');
        bool(json, "purple_threat", decision.purpleThreat);
        json.append('}');
        write(json.toString());
    }

    private static void writeSimpleDomainFailed(LevelAccessor world, LivingEntity sukuna, LivingEntity target, String fightId, Decision decision) {
        StringBuilder json = eventBase(world, sukuna, target, fightId, "simple_domain_failed", 512);
        field(json, "action", decision.action).append(',');
        field(json, "block_reason", decision.blockReason).append(',');
        field(json, "fallback_action", decision.fallbackAction).append(',');
        number(json, "simple_domain_duration", decision.simpleDomainDuration).append(',');
        number(json, "simple_domain_cooldown", decision.simpleDomainCooldown).append(',');
        bool(json, "simple_domain_ready", decision.simpleDomainReady).append(',');
        bool(json, "enemy_domain_trap", decision.enemyDomainTrap).append(',');
        bool(json, "escape_feasible", decision.escapeFeasible).append(',');
        number(json, "void_exposure", decision.voidExposure);
        json.append('}');
        write(json.toString());
    }

    private static void writeDamageAttempt(LevelAccessor world, LivingEntity sukuna, LivingEntity target, String fightId, PendingDamage pending) {
        StringBuilder json = eventBase(world, sukuna, target, fightId, "damage_attempt", 512);
        number(json, "attempted_damage", pending.attemptedAmount).append(',');
        number(json, "target_health_before", pending.preHealth).append(',');
        field(json, "damage_source", damageSourceName(pending.source)).append(',');
        field(json, "direct_entity", entityName(pending.source == null ? null : pending.source.getDirectEntity())).append(',');
        field(json, "attacker", entityName(pending.source == null ? null : pending.source.getEntity()));
        json.append('}');
        write(json.toString());
    }

    private static void writeSukunaDamageResolved(LevelAccessor world, LivingEntity sukuna, String fightId, LivingDamageEvent event) {
        BenchmarkContext context = BENCHMARK_CONTEXT_BY_SUKUNA.get(sukuna.getStringUUID());
        StringBuilder json = new StringBuilder(768);
        json.append('{');
        field(json, "event", "sukuna_damage_resolved").append(',');
        number(json, "schema", 3.0).append(',');
        field(json, "dev_only", "true").append(',');
        number(json, "game_time", world.dayTime()).append(',');
        number(json, "sukuna_tick", sukuna.tickCount).append(',');
        field(json, "fight_id", fightId).append(',');
        if (context != null) {
            field(json, "benchmark_id", context.benchmarkId).append(',');
            field(json, "scenario", context.scenario).append(',');
            field(json, "variant", context.variant).append(',');
            number(json, "run_index", context.runIndex).append(',');
            number(json, "arena_index", context.arenaIndex).append(',');
        }
        field(json, "sukuna_uuid", sukuna.getStringUUID()).append(',');
        field(json, "sukuna_type", typeName(sukuna)).append(',');
        number(json, "actual_damage", event.getAmount()).append(',');
        number(json, "sukuna_health_after", healthAndAbsorption(sukuna)).append(',');
        bool(json, "sukuna_alive", sukuna.isAlive()).append(',');
        field(json, "damage_source", damageSourceName(event.getSource())).append(',');
        field(json, "direct_entity", entityName(event.getSource() == null ? null : event.getSource().getDirectEntity())).append(',');
        field(json, "attacker", entityName(event.getSource() == null ? null : event.getSource().getEntity())).append(',');
        field(json, "sukuna_effects", effectsSnapshot(sukuna));
        json.append('}');
        write(json.toString());
    }

    private static void writeDamageResolved(LevelAccessor world, LivingEntity sukuna, LivingEntity target, String fightId, PendingDamage pending, double actualDamage,
                                            double attemptedAmount, double hurtAmount, double damageAmount, int attemptCount, double preHealth) {
        StringBuilder json = eventBase(world, sukuna, target, fightId, "damage_resolved", 768);
        number(json, "attempted_damage", attemptedAmount).append(',');
        number(json, "hurt_damage", hurtAmount).append(',');
        number(json, "pipeline_damage", damageAmount).append(',');
        number(json, "actual_damage", actualDamage).append(',');
        number(json, "attempt_count", attemptCount).append(',');
        number(json, "target_health_before", preHealth).append(',');
        number(json, "target_health_after", target.isAlive() ? healthAndAbsorption(target) : 0.0).append(',');
        bool(json, "target_alive", target.isAlive()).append(',');
        field(json, "damage_source", damageSourceName(pending.source));
        json.append('}');
        write(json.toString());
    }

    private static void writeTargetDeathResolved(LevelAccessor world, LivingEntity sukuna, LivingEntity target, net.minecraft.world.damagesource.DamageSource source) {
        String fightId = sukuna.getStringUUID() + ":" + target.getStringUUID();
        StringBuilder json = eventBase(world, sukuna, target, fightId, "target_death_resolved", 768);
        number(json, "target_health_after", target.isAlive() ? healthAndAbsorption(target) : 0.0).append(',');
        bool(json, "target_alive", target.isAlive()).append(',');
        field(json, "damage_source", damageSourceName(source)).append(',');
        field(json, "direct_entity", entityName(source == null ? null : source.getDirectEntity())).append(',');
        field(json, "attacker", entityName(source == null ? null : source.getEntity()));
        json.append('}');
        write(json.toString());
    }

    private static void writeFightOutcome(LevelAccessor world, LivingEntity sukuna, LivingEntity target, String fightId, Decision decision) {
        StringBuilder json = eventBase(world, sukuna, target, fightId, "fight_outcome", 512);
        field(json, "outcome", decision.selfHealth <= 0.0 ? "sukuna_dead" : "target_dead").append(',');
        field(json, "death_reason", decision.selfHealth <= 0.0 ? classifyDeathReason(RUNTIME_FIGHTS.get(fightId)) : "").append(',');
        number(json, "damage_dealt", decision.damageDealt).append(',');
        number(json, "damage_taken", decision.damageTaken).append(',');
        field(json, "last_action", decision.action).append(',');
        field(json, "target_type", typeName(target)).append(',');
        field(json, "technique_key", decision.techniqueKey);
        json.append('}');
        write(json.toString());
    }

    private static StringBuilder eventBase(LevelAccessor world, LivingEntity sukuna, LivingEntity target, String fightId, String event, int capacity) {
        StringBuilder json = new StringBuilder(capacity);
        json.append('{');
        field(json, "event", event).append(',');
        number(json, "schema", 3.0).append(',');
        field(json, "dev_only", "true").append(',');
        number(json, "game_time", world.dayTime()).append(',');
        number(json, "sukuna_tick", sukuna.tickCount).append(',');
        field(json, "fight_id", fightId).append(',');
        appendBenchmarkContext(json, fightId, sukuna.getStringUUID());
        field(json, "sukuna_uuid", sukuna.getStringUUID()).append(',');
        field(json, "target_uuid", target.getStringUUID()).append(',');
        field(json, "sukuna_type", typeName(sukuna)).append(',');
        field(json, "target_type", typeName(target)).append(',');
        return json;
    }

    private static synchronized void rememberDecision(LevelAccessor world, LivingEntity sukuna, LivingEntity target, String fightId, Decision decision) {
        CURRENT_FIGHT_BY_SUKUNA.put(sukuna.getStringUUID(), fightId);
        RuntimeFight fight = RUNTIME_FIGHTS.computeIfAbsent(fightId, RuntimeFight::new);
        fight.sukunaUuid = sukuna.getStringUUID();
        fight.targetUuid = target.getStringUUID();
        fight.targetType = typeName(target);
        fight.targetName = target.getName().getString();
        fight.techniqueKey = decision.techniqueKey;
        fight.archetype = decision.archetype;
        fight.addDecision(decisionSummary(world, decision));
        if (!decision.actionStarted || !decision.blockReason.isEmpty() && !"ready".equals(decision.blockReason) || !decision.fallbackAction.isEmpty()) {
            fight.addBlocked(blockedSummary(world, decision));
        }
        String domain = domainSummary(world, decision);
        if (!domain.equals(fight.lastDomain)) {
            fight.lastDomain = domain;
            fight.addDomain(domain);
        }
    }

    private static synchronized void recordSukunaDeath(LevelAccessor world, LivingEntity sukuna, DamageSource source) {
        String sukunaId = sukuna.getStringUUID();
        String fightId = CURRENT_FIGHT_BY_SUKUNA.getOrDefault(sukunaId, sukunaId + ":unknown");
        RuntimeFight fight = RUNTIME_FIGHTS.computeIfAbsent(fightId, RuntimeFight::new);
        fight.sukunaUuid = sukunaId;
        String reason = classifyDeathReason(fight);
        StringBuilder death = deathEventBase(world, sukuna, fight, "sukuna_death", 1024);
        field(death, "death_reason", reason).append(',');
        field(death, "damage_source", damageSourceName(source)).append(',');
        field(death, "direct_entity", entityName(source == null ? null : source.getDirectEntity())).append(',');
        field(death, "attacker", entityName(source == null ? null : source.getEntity())).append(',');
        number(death, "self_health", sukuna.getHealth()).append(',');
        number(death, "self_max_health", sukuna.getMaxHealth()).append(',');
        field(death, "effects", effectsSnapshot(sukuna)).append(',');
        field(death, "lethal_damage", fight.recentDamage()).append(',');
        field(death, "bad_decision_window", fight.recentDecisions());
        death.append('}');
        write(death.toString());

        StringBuilder context = deathEventBase(world, sukuna, fight, "death_context", 1024);
        field(context, "death_reason", reason).append(',');
        field(context, "domain_context", fight.recentDomains()).append(',');
        field(context, "blocked_context", fight.recentBlocked()).append(',');
        field(context, "anti_domain_protection", antiDomainProtection(sukuna)).append(',');
        field(context, "effects", effectsSnapshot(sukuna));
        context.append('}');
        write(context.toString());

        StringBuilder bad = deathEventBase(world, sukuna, fight, "bad_decision_window", 1024);
        field(bad, "death_reason", reason).append(',');
        field(bad, "recent_decisions", fight.recentDecisions()).append(',');
        field(bad, "recent_blocked", fight.recentBlocked());
        bad.append('}');
        write(bad.toString());

        StringBuilder lethal = deathEventBase(world, sukuna, fight, "lethal_damage", 1024);
        field(lethal, "death_reason", reason).append(',');
        field(lethal, "damage_sequence", fight.recentDamage()).append(',');
        field(lethal, "damage_source", damageSourceName(source));
        lethal.append('}');
        write(lethal.toString());

        StringBuilder outcome = deathEventBase(world, sukuna, fight, "fight_outcome", 1024);
        field(outcome, "outcome", "sukuna_dead").append(',');
        field(outcome, "death_reason", reason).append(',');
        field(outcome, "last_action", fight.lastAction()).append(',');
        field(outcome, "target_type", fight.targetType).append(',');
        field(outcome, "technique_key", fight.techniqueKey);
        outcome.append('}');
        write(outcome.toString());
        flush();
    }

    private static StringBuilder deathEventBase(LevelAccessor world, LivingEntity sukuna, RuntimeFight fight, String event, int capacity) {
        StringBuilder json = new StringBuilder(capacity);
        json.append('{');
        field(json, "event", event).append(',');
        number(json, "schema", 4.0).append(',');
        field(json, "dev_only", "true").append(',');
        number(json, "game_time", world.dayTime()).append(',');
        number(json, "sukuna_tick", sukuna.tickCount).append(',');
        field(json, "fight_id", fight.fightId).append(',');
        appendBenchmarkContext(json, fight.fightId, sukuna.getStringUUID());
        field(json, "sukuna_uuid", sukuna.getStringUUID()).append(',');
        field(json, "target_uuid", fight.targetUuid).append(',');
        field(json, "sukuna_type", typeName(sukuna)).append(',');
        field(json, "target_type", fight.targetType).append(',');
        field(json, "target_name", fight.targetName).append(',');
        return json;
    }

    private static void appendBenchmarkContext(StringBuilder json, String fightId, String sukunaUuid) {
        BenchmarkContext context = BENCHMARK_CONTEXT_BY_FIGHT.get(fightId);
        if (context == null && sukunaUuid != null && !sukunaUuid.isEmpty()) {
            context = BENCHMARK_CONTEXT_BY_SUKUNA.get(sukunaUuid);
        }
        if (context == null) {
            return;
        }
        field(json, "benchmark_id", context.benchmarkId).append(',');
        field(json, "scenario", context.scenario).append(',');
        field(json, "variant", context.variant).append(',');
        number(json, "run_index", context.runIndex).append(',');
        number(json, "arena_index", context.arenaIndex).append(',');
    }

    public static synchronized String summary() {
        Summary s = readSummary();
        return s.report();
    }

    public static synchronized String report() {
        Summary s = readSummary();
        return s.report() + "\n\n" + s.regressionsMarkdown();
    }

    public static synchronized Path exportReports() {
        Summary s = readSummary();
        try {
            Files.createDirectories(DIR);
            s.writeReports(DIR);
        } catch (IOException e) {
            JujutsucraftaddonMod.LOGGER.warn("Failed to export OpSukuna metric reports", e);
        }
        return DIR;
    }

    public static synchronized Path exportBenchmarkReports(String benchmarkId) {
        Path exportDir = DIR.resolve("benchmarks").resolve(benchmarkId == null || benchmarkId.isEmpty() ? "unknown" : benchmarkId);
        exportReports();
        try {
            Files.createDirectories(exportDir);
            copyIfExists(DIR.resolve("summary.json"), exportDir.resolve("summary.json"));
            copyIfExists(DIR.resolve("fights.json"), exportDir.resolve("fights.json"));
            copyIfExists(DIR.resolve("regressions.md"), exportDir.resolve("regressions.md"));
            try (Stream<Path> files = Files.list(DIR)) {
                for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".jsonl")).toList()) {
                    copyBenchmarkJsonl(file, exportDir.resolve(file.getFileName().toString()), benchmarkId);
                }
            }
        } catch (IOException e) {
            JujutsucraftaddonMod.LOGGER.warn("Failed to export OpSukuna benchmark reports", e);
        }
        return exportDir;
    }

    public static synchronized void flush() {
        if (writer == null) {
            return;
        }
        try {
            writer.flush();
        } catch (IOException e) {
            JujutsucraftaddonMod.LOGGER.warn("Failed to flush OpSukuna metrics", e);
        }
    }

    public static synchronized int reset() {
        flush();
        closeWriter();
        RUNTIME_FIGHTS.clear();
        CURRENT_FIGHT_BY_SUKUNA.clear();
        BENCHMARK_CONTEXT_BY_FIGHT.clear();
        int deleted = 0;
        if (Files.isDirectory(DIR)) {
            try (Stream<Path> files = Files.list(DIR)) {
                for (Path file : files.filter(path -> {
                    String name = path.getFileName().toString();
                    return name.endsWith(".jsonl") || "summary.json".equals(name) || "regressions.md".equals(name) || "fights.json".equals(name);
                }).toList()) {
                    Files.deleteIfExists(file);
                    deleted++;
                }
            } catch (IOException e) {
                JujutsucraftaddonMod.LOGGER.warn("Failed to reset OpSukuna metrics", e);
            }
        }
        return deleted;
    }

    private static synchronized void write(String line) {
        try {
            ensureWriter();
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            JujutsucraftaddonMod.LOGGER.warn("Failed to write OpSukuna metrics", e);
            closeWriter();
        }
    }

    private static void ensureWriter() throws IOException {
        LocalDate today = LocalDate.now();
        if (writer != null && today.equals(writerDate)) {
            return;
        }
        closeWriter();
        Files.createDirectories(DIR);
        Path file = DIR.resolve("op_sukuna_brain-" + today + ".jsonl");
        writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        writerDate = today;
    }

    private static void closeWriter() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }
        writer = null;
        writerDate = null;
    }

    private static void copyIfExists(Path source, Path target) throws IOException {
        if (Files.exists(source)) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void copyBenchmarkJsonl(Path source, Path target, String benchmarkId) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        String marker = "\"benchmark_id\":\"" + escape(benchmarkId) + "\"";
        try (Stream<String> lines = Files.lines(source, StandardCharsets.UTF_8);
             BufferedWriter out = Files.newBufferedWriter(target, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {
            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                if (line.contains(marker)) {
                    out.write(line);
                    out.newLine();
                }
            }
        }
    }

    private static boolean shouldRecordDetailedOutgoingDamage(LivingEntity sukuna, LivingEntity target) {
        BenchmarkContext context = BENCHMARK_CONTEXT_BY_SUKUNA.get(sukuna.getStringUUID());
        if (context == null) {
            return true;
        }
        String fightId = CURRENT_FIGHT_BY_SUKUNA.get(sukuna.getStringUUID());
        return fightId != null && fightId.equals(target.getPersistentData().getString("JJKU_BENCHMARK_FIGHT_ID"));
    }

    private static Summary readSummary() {
        flush();
        Summary summary = new Summary();
        if (!Files.isDirectory(DIR)) {
            return summary;
        }
        try (Stream<Path> files = Files.list(DIR)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".jsonl")).toList()) {
                try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
                    lines.forEach(line -> readLine(summary, line));
                }
            }
        } catch (IOException e) {
            JujutsucraftaddonMod.LOGGER.warn("Failed to read OpSukuna metrics", e);
        }
        return summary;
    }

    private static void readLine(Summary summary, String line) {
        try {
            JsonElement parsed = JsonParser.parseString(line);
            if (!parsed.isJsonObject()) {
                return;
            }
            JsonObject obj = parsed.getAsJsonObject();
            String event = str(obj, "event");
            if ("decision".equals(event)) {
                summary.accept(obj);
            } else if ("sukuna_death".equals(event) || "fight_outcome".equals(event)) {
                summary.acceptOutcome(obj);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static String typeName(LivingEntity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id == null ? entity.getClass().getName() : id.toString();
    }

    private static String mobTargetUuid(LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != null) {
            return mob.getTarget().getStringUUID();
        }
        return "";
    }

    private static StringBuilder field(StringBuilder json, String name, String value) {
        return json.append('"').append(name).append("\":\"").append(escape(value)).append('"');
    }

    private static StringBuilder number(StringBuilder json, String name, double value) {
        if (!Double.isFinite(value)) {
            value = 0.0;
        }
        return json.append('"').append(name).append("\":").append(value);
    }

    private static StringBuilder bool(StringBuilder json, String name, boolean value) {
        return json.append('"').append(name).append("\":").append(value);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static String str(JsonObject obj, String name) {
        JsonElement value = obj.get(name);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static double dbl(JsonObject obj, String name) {
        JsonElement value = obj.get(name);
        return value == null || value.isJsonNull() ? 0.0 : value.getAsDouble();
    }

    private static boolean bool(JsonObject obj, String name) {
        JsonElement value = obj.get(name);
        return value != null && !value.isJsonNull() && value.getAsBoolean();
    }

    private static String decisionSummary(LevelAccessor world, Decision decision) {
        return "t=" + (long) world.dayTime()
                + ",a=" + decision.action
                + ",k=" + decision.kind
                + ",hp=" + fmt(decision.selfHealthRatio)
                + ",d=" + fmt(decision.distance)
                + ",domain=" + decision.dominantDomainProfile
                + ",void=" + fmt(decision.voidExposure)
                + ",sd=" + (long) decision.simpleDomainDuration
                + ",path=" + decision.pathBlocked
                + ",stuck=" + fmt(decision.stuckLevel)
                + ",block=" + decision.blockReason
                + ",fallback=" + decision.fallbackAction;
    }

    private static String blockedSummary(LevelAccessor world, Decision decision) {
        return "t=" + (long) world.dayTime()
                + ",a=" + decision.action
                + ",started=" + decision.actionStarted
                + ",reason=" + decision.blockReason
                + ",fallback=" + decision.fallbackAction;
    }

    private static String domainSummary(LevelAccessor world, Decision decision) {
        return "t=" + (long) world.dayTime()
                + ",enemy=" + (long) decision.activeEnemyDomains
                + ",void=" + (long) decision.activeVoidDomains
                + ",profile=" + decision.dominantDomainProfile
                + ",owner=" + decision.dominantDomainOwner
                + ",sd=" + (long) decision.simpleDomainDuration
                + ",gap=" + decision.antiDomainGapImminent
                + ",escape=" + decision.escapeFeasible
                + ",action=" + decision.action;
    }

    private static void updatePendingPipeline(LivingEntity target, DamageSource source, float amount, boolean finalDamageStage) {
        if (target == null || PENDING_DAMAGE.isEmpty()) {
            return;
        }
        for (int i = PENDING_DAMAGE.size() - 1; i >= 0; i--) {
            PendingDamage pending = PENDING_DAMAGE.get(i);
            if (pending.target == target && sameDamageSource(pending.source, source) && target.level().getGameTime() - pending.gameTime <= 2) {
                if (finalDamageStage) {
                    pending.damageAmount = amount;
                } else {
                    pending.hurtAmount = amount;
                }
                return;
            }
        }
    }

    private static void rememberOutgoingDamage(LivingEntity sukuna, LivingEntity target, String fightId, String value) {
        RuntimeFight fight = RUNTIME_FIGHTS.computeIfAbsent(fightId, RuntimeFight::new);
        fight.sukunaUuid = sukuna.getStringUUID();
        fight.targetUuid = target.getStringUUID();
        fight.targetType = typeName(target);
        fight.targetName = target.getName().getString();
        fight.addDamage("outgoing,t=" + sukuna.level().dayTime() + "," + value);
    }

    private static LivingEntity opSukunaFromSource(DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && isOpSukunaSubject(living)) {
            return living;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof LivingEntity living && isOpSukunaSubject(living)) {
            return living;
        }
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity living && isOpSukunaSubject(living)) {
            return living;
        }
        return null;
    }

    private static boolean sameDamageSource(DamageSource first, DamageSource second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        return damageSourceName(first).equals(damageSourceName(second))
                && first.getEntity() == second.getEntity()
                && first.getDirectEntity() == second.getDirectEntity();
    }

    private static double healthAndAbsorption(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    private static String damageSummary(LevelAccessor world, LivingEntity sukuna, DamageSource source, float amount) {
        return "t=" + (long) world.dayTime()
                + ",amount=" + fmt(amount)
                + ",hp_after=" + fmt(Math.max(0.0, sukuna.getHealth() - amount))
                + ",source=" + damageSourceName(source)
                + ",direct=" + entityName(source == null ? null : source.getDirectEntity())
                + ",attacker=" + entityName(source == null ? null : source.getEntity())
                + ",pos=" + fmt(sukuna.getX()) + "/" + fmt(sukuna.getY()) + "/" + fmt(sukuna.getZ())
                + ",effects=" + effectsSnapshot(sukuna)
                + ",anti_domain=" + antiDomainProtection(sukuna);
    }

    private static String classifyDeathReason(RuntimeFight fight) {
        if (fight == null) {
            return "unknown";
        }
        String decisions = fight.recentDecisions();
        String domains = fight.recentDomains();
        String blocked = fight.recentBlocked();
        String damage = fight.recentDamage();
        boolean domain = domains.contains("enemy=1") || domains.contains("enemy=2") || domains.contains("enemy=3")
                || domains.contains("void=1") || domains.contains("GOJO_MURYO") || decisions.contains("domain=GOJO_MURYO")
                || decisions.contains("domain=GENERIC_DOMAIN") || decisions.contains("void=0.9") || decisions.contains("void=1.");
        boolean voidLike = domains.contains("GOJO_MURYO") || decisions.contains("domain=GOJO_MURYO") || domains.contains("void=1") || decisions.contains("void=1.");
        boolean noProtection = decisions.contains("sd=0") || domains.contains("sd=0");
        boolean gap = decisions.contains("gap=true") || domains.contains("gap=true") || damage.contains("anti_domain=none");
        boolean purple = decisions.contains("a=PURPLE_EVADE") || decisions.contains("purple") || damage.toLowerCase(java.util.Locale.ROOT).contains("purple");
        boolean stuck = decisions.contains("path=true") || decisions.contains("stuck=0.7") || decisions.contains("stuck=0.8")
                || decisions.contains("stuck=0.9") || decisions.contains("stuck=1.") || blocked.contains("DomainEscape");
        boolean rct = decisions.contains("a=RCT") || decisions.contains("rct") || decisions.contains("hp=0.1") || decisions.contains("hp=0.2");
        boolean loop = fight.noImpactOrBlockedCount() >= 3 || repeatedLastActions(fight.decisions, 4);
        if (domain && voidLike && noProtection) return "domain_caught";
        if (domain && gap) return "anti_domain_gap";
        if (purple) return "purple_hit";
        if (domain && stuck) return "stuck_in_domain";
        if (rct && blocked.contains("RCT")) return "rct_failed";
        if (loop) return "bad_action_loop";
        return "unknown";
    }

    private static boolean repeatedLastActions(Deque<String> decisions, int threshold) {
        String last = null;
        int count = 0;
        for (String decision : decisions) {
            String action = extractToken(decision, "a=");
            if (action.equals(last) && !action.isEmpty()) {
                count++;
            } else {
                last = action;
                count = 1;
            }
        }
        return count >= threshold;
    }

    private static String extractToken(String text, String key) {
        int start = text.indexOf(key);
        if (start < 0) return "";
        start += key.length();
        int end = text.indexOf(',', start);
        return end < 0 ? text.substring(start) : text.substring(start, end);
    }

    private static String antiDomainProtection(LivingEntity entity) {
        int simple = effectDuration(entity, JujutsucraftModMobEffects.SIMPLE_DOMAIN.get());
        int max = effectDuration(entity, JujutsucraftaddonModMobEffects.SIMPLE_DOMAIN_MAX.get());
        int hwb = effectDuration(entity, JujutsucraftaddonModMobEffects.HWB.get());
        int domain = effectDuration(entity, JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
        if (domain > 0) return "domain:" + domain;
        if (max > 0) return "simple_domain_max:" + max;
        if (simple > 0) return "simple_domain:" + simple;
        if (hwb > 0) return "hwb:" + hwb;
        return "none";
    }

    private static int effectDuration(LivingEntity entity, net.minecraft.world.effect.MobEffect effect) {
        MobEffectInstance instance = entity.getEffect(effect);
        return instance == null ? 0 : instance.getDuration();
    }

    private static String effectsSnapshot(LivingEntity entity) {
        return entity.getActiveEffects().stream()
                .limit(12)
                .map(effect -> {
                    ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
                    return (id == null ? "unknown" : id.toString()) + "@" + effect.getAmplifier() + ":" + effect.getDuration();
                })
                .collect(java.util.stream.Collectors.joining("|"));
    }

    private static String damageSourceName(DamageSource source) {
        return source == null ? "" : source.getMsgId();
    }

    private static String entityName(Entity entity) {
        if (entity == null) {
            return "";
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return (id == null ? entity.getClass().getName() : id.toString()) + ":" + entity.getStringUUID();
    }

    private static final class PendingDamage {
        final LivingEntity sukuna;
        final LivingEntity target;
        final DamageSource source;
        final String fightId;
        final long gameTime;
        final double preHealth;
        final double attemptedAmount;
        double hurtAmount;
        double damageAmount;

        PendingDamage(LivingEntity sukuna, LivingEntity target, DamageSource source, double attemptedAmount, String fightId) {
            this.sukuna = sukuna;
            this.target = target;
            this.source = source;
            this.attemptedAmount = attemptedAmount;
            this.fightId = fightId;
            this.gameTime = target.level().getGameTime();
            this.preHealth = healthAndAbsorption(target);
        }

        String groupKey() {
            return fightId + ":" + target.getStringUUID() + ":" + gameTime;
        }
    }

    private record ResolvedDamage(double actualDamage, long gameTime) {
    }

    private static final class RuntimeFight {
        final String fightId;
        final Deque<String> decisions = new ArrayDeque<>();
        final Deque<String> blocked = new ArrayDeque<>();
        final Deque<String> domains = new ArrayDeque<>();
        final Deque<String> damage = new ArrayDeque<>();
        String sukunaUuid = "";
        String targetUuid = "";
        String targetType = "";
        String targetName = "";
        String techniqueKey = "";
        String archetype = "";
        String lastDomain = "";

        RuntimeFight(String fightId) {
            this.fightId = fightId == null || fightId.isEmpty() ? "unknown" : fightId;
        }

        void addDecision(String value) {
            addLimited(decisions, value, 20);
        }

        void addBlocked(String value) {
            addLimited(blocked, value, 10);
        }

        void addDomain(String value) {
            addLimited(domains, value, 10);
        }

        void addDamage(String value) {
            addLimited(damage, value, 10);
        }

        String recentDecisions() {
            return String.join(" || ", decisions);
        }

        String recentBlocked() {
            return String.join(" || ", blocked);
        }

        String recentDomains() {
            return String.join(" || ", domains);
        }

        String recentDamage() {
            return String.join(" || ", damage);
        }

        String lastAction() {
            return decisions.isEmpty() ? "" : extractToken(decisions.getLast(), "a=");
        }

        int noImpactOrBlockedCount() {
            int count = 0;
            for (String value : blocked) {
                if (!value.isEmpty()) count++;
            }
            return count;
        }

        private static void addLimited(Deque<String> deque, String value, int limit) {
            deque.addLast(value == null ? "" : value);
            while (deque.size() > limit) {
                deque.removeFirst();
            }
        }
    }

    public static final class Decision {
        public String action = "";
        public String kind = "";
        public String aiMode = "";
        public String topScores = "";
        public double score;
        public double adjustedScore;
        public double skill;
        public double selfHealth;
        public double targetHealth;
        public double selfHealthRatio;
        public double targetHealthRatio;
        public double distance;
        public double hitboxDistance;
        public double centerDistance;
        public double predictedDistance;
        public double damageDealt;
        public double damageTaken;
        public double targetSkill;
        public String archetype = "";
        public String techniqueKey = "";
        public double primaryTechnique;
        public double secondaryTechnique;
        public double targetThreat;
        public double targetPower;
        public double meleeThreat;
        public double rangeThreat;
        public double pressure;
        public double opportunity;
        public double immediateThreat;
        public double survivalUrgency;
        public double incomingKillRisk;
        public double hitConfidence;
        public double whiffOpportunity;
        public double stuckLevel;
        public double groupPressure;
        public double groupActivePressure;
        public double groupApexPressure;
        public double rctStrain;
        public double brainDamage;
        public double idealDistance;
        public boolean gojo;
        public boolean targetDomain;
        public boolean targetCastingDomain;
        public boolean targetCooldown;
        public boolean targetUnstable;
        public boolean targetDodge;
        public boolean targetCounter;
        public boolean targetGuard;
        public boolean targetEscaping;
        public boolean targetWhiffed;
        public boolean targetOverextended;
        public boolean clearShot;
        public boolean pathBlocked;
        public boolean normalReady;
        public boolean passiveReady;
        public boolean domainReady;
        public boolean domainBlockedByCooldown;
        public String domainBlockReason = "";
        public boolean domainRecentlyOpened;
        public boolean selfDomain;
        public double domainCastRequestedSkill;
        public boolean domainCastPending;
        public boolean domainCastConfirmed;
        public String domainCastFailedReason = "";
        public boolean worldCutCapable;
        public boolean worldCutReady;
        public double worldCutCooldown;
        public String worldCutBlockReason = "";
        public boolean trivialTarget;
        public String sukunaForm = "";
        public boolean canTransformHeian;
        public String heianEmergencyReason = "";
        public boolean tenShadowsAvailable;
        public boolean mahoragaAvailable;
        public boolean enemyDomainTrap;
        public boolean domainEscapeWindow;
        public double domainResponseTicks;
        public double activeEnemyDomains;
        public double activeVoidDomains;
        public boolean domainClashSuppressed;
        public boolean singleDominantSureHit;
        public double simpleDomainDuration;
        public double simpleDomainCooldown;
        public boolean antiDomainGapImminent;
        public String dominantDomainOwner = "";
        public String dominantDomainProfile = "";
        public boolean purpleThreat;
        public double purpleRisk;
        public boolean purpleEvade;
        public boolean purpleHit;
        public boolean purpleAvoided;
        public boolean stuck;
        public boolean infinityWaste;
        public double adaptationConfidence;
        public double trapPressure;
        public double evasionPressure;
        public double rangePressure;
        public String domainPlan = "";
        public double domainClearValue;
        public double domainSustainCost;
        public double domainRecoveryRisk;
        public double domainCounterRisk;
        public double domainCastScore;
        public double domainHwbScore;
        public double domainPunishScore;
        public double domainScore;
        public String domainDeliberation = "";
        public String domainPhase = "";
        public double domainCommitDeadline;
        public String domainGateReason = "";
        public String domainProfile = "";
        public String chosenDomainOption = "";
        public boolean escapeFeasible;
        public double worldCutDomainValue;
        public double counterDomainValue;
        public double tankDomainValue;
        public String infinityBypassMode = "";
        public String meleeUnlockReason = "";
        public double preferredRange;
        public double safePunishWindow;
        public double learnedBurstRisk;
        public double rangeActionStreak;
        public double noImpactActionStreak;
        public boolean yujiBurstDuel;
        public boolean actionStarted = true;
        public String blockReason = "";
        public String fallbackAction = "";
        public double voidExposure;
        public boolean simpleDomainReady;
        public boolean simpleDomainKeyBlocked;
        public String simpleDomainKeySource = "";
        public boolean domainAmplificationReady;
        public boolean rctReady;
        public boolean shrineMode;
        public boolean fugaActive;
        public double fugaCharge;
        public double fugaChargeMax;
        public double fugaChargeRate;
        public double fugaHoldTicks;
        public String fugaReleaseReason = "";
    }

    private static final class Summary {
        final Map<String, FightStats> fights = new HashMap<>();
        final Map<String, Integer> actions = new HashMap<>();
        final Map<String, GroupStats> byTargetType = new HashMap<>();
        final Map<String, GroupStats> byArchetype = new HashMap<>();
        final Map<String, GroupStats> byTechnique = new HashMap<>();
        final Map<String, Integer> deathReasons = new HashMap<>();
        final Map<String, Integer> deathsByTargetType = new HashMap<>();
        final Map<String, Integer> deathsByArchetype = new HashMap<>();
        final Map<String, Integer> deathsByTechnique = new HashMap<>();
        final Map<String, Integer> deathsByDomain = new HashMap<>();
        int decisions;
        int sukunaDeaths;
        int maxRepeat;
        int currentRepeat;
        String lastAction = "";
        double firstTick = -1.0;
        double lastTick;
        double damageDealt;
        double damageTaken;
        int gojoDomainSignals;
        int gojoDomainTraps;
        int domainResponses;
        double domainResponseTicks;
        int purpleThreats;
        int purpleAvoided;
        int purpleHits;
        int infinityWaste;
        double adaptationConfidence;
        double trapPressure;
        double evasionPressure;
        double rangePressure;
        double domainClearValue;
        double domainSustainCost;
        double domainRecoveryRisk;
        double domainScore;
        double adjustedScore;
        int targetDodge;
        int targetCounter;
        int targetGuard;
        int targetEscaping;
        int pathBlocked;
        int clearShot;
        int worldCutCapable;
        int worldCutSelected;
        int domainSelected;
        int domainBlockedByCooldown;
        int mahoragaSelected;
        int rangeActions;
        int maxRangeActionStreak;
        int maxNoImpactActionStreak;
        int defensiveActions;
        int gojoDecisions;
        int yujiModuloDecisions;

        void accept(JsonObject obj) {
            decisions++;
            fights.computeIfAbsent(str(obj, "fight_id"), FightStats::new).accept(obj);
            String action = str(obj, "action");
            String kind = str(obj, "kind");
            actions.put(action, actions.getOrDefault(action, 0) + 1);
            if (action.equals(lastAction)) {
                currentRepeat++;
            } else {
                currentRepeat = 1;
                lastAction = action;
            }
            maxRepeat = Math.max(maxRepeat, currentRepeat);
            double tick = dbl(obj, "game_time");
            if (firstTick < 0.0) {
                firstTick = tick;
            }
            lastTick = Math.max(lastTick, tick);
            damageDealt += dbl(obj, "damage_dealt");
            damageTaken += dbl(obj, "damage_taken");
            if (bool(obj, "gojo") && (bool(obj, "target_domain") || bool(obj, "target_casting_domain"))) {
                gojoDomainSignals++;
                if (bool(obj, "enemy_domain_trap")) {
                    gojoDomainTraps++;
                }
                double response = dbl(obj, "domain_response_ticks");
                if (response >= 0.0) {
                    domainResponses++;
                    domainResponseTicks += response;
                }
            }
            if (bool(obj, "purple_threat")) {
                purpleThreats++;
                if (bool(obj, "purple_avoided")) {
                    purpleAvoided++;
                }
                if (bool(obj, "purple_hit")) {
                    purpleHits++;
                }
            }
            if (bool(obj, "infinity_waste")) {
                infinityWaste++;
            }
            adaptationConfidence += dbl(obj, "adaptation_confidence");
            trapPressure += dbl(obj, "trap_pressure");
            evasionPressure += dbl(obj, "evasion_pressure");
            rangePressure += dbl(obj, "range_pressure");
            domainClearValue += dbl(obj, "domain_clear_value");
            domainSustainCost += dbl(obj, "domain_sustain_cost");
            domainRecoveryRisk += dbl(obj, "domain_recovery_risk");
            domainScore += dbl(obj, "domain_score");
            adjustedScore += dbl(obj, "adjusted_score");
            if (bool(obj, "target_dodge")) targetDodge++;
            if (bool(obj, "target_counter")) targetCounter++;
            if (bool(obj, "target_guard")) targetGuard++;
            if (bool(obj, "target_escaping")) targetEscaping++;
            if (bool(obj, "path_blocked")) pathBlocked++;
            if (bool(obj, "clear_shot")) clearShot++;
            if (bool(obj, "world_cut_capable")) worldCutCapable++;
            if ("WORLD_CUT".equals(kind)) worldCutSelected++;
            if ("DOMAIN".equals(kind)) domainSelected++;
            if (bool(obj, "domain_blocked_by_cooldown")) domainBlockedByCooldown++;
            if ("MAHORAGA".equals(action)) mahoragaSelected++;
            if ("RANGE_CONTROL".equals(kind)) rangeActions++;
            maxRangeActionStreak = Math.max(maxRangeActionStreak, (int) dbl(obj, "range_action_streak"));
            maxNoImpactActionStreak = Math.max(maxNoImpactActionStreak, (int) dbl(obj, "no_impact_action_streak"));
            if ("MICRO_DEFENSE".equals(kind) || "DOMAIN_AMPLIFICATION".equals(kind)) defensiveActions++;
            if (bool(obj, "gojo")) gojoDecisions++;
            String targetType = str(obj, "target_type");
            if (targetType.contains("itadori_yuji_modulo") || bool(obj, "yuji_burst_duel")) yujiModuloDecisions++;

            byTargetType.computeIfAbsent(targetType, GroupStats::new).accept(obj);
            byArchetype.computeIfAbsent(str(obj, "archetype"), GroupStats::new).accept(obj);
            byTechnique.computeIfAbsent(str(obj, "technique_key"), GroupStats::new).accept(obj);
        }

        void acceptOutcome(JsonObject obj) {
            FightStats fight = fights.computeIfAbsent(str(obj, "fight_id"), FightStats::new);
            fight.acceptOutcome(obj);
            if (!"sukuna_death".equals(str(obj, "event"))) {
                return;
            }
            sukunaDeaths++;
            addCount(deathReasons, normalizeKey(str(obj, "death_reason")));
            addCount(deathsByTargetType, normalizeKey(str(obj, "target_type")));
            addCount(deathsByArchetype, normalizeKey(fight.archetype));
            addCount(deathsByTechnique, normalizeKey(str(obj, "technique_key").isEmpty() ? fight.techniqueKey : str(obj, "technique_key")));
            addCount(deathsByDomain, extractDomainProfile(str(obj, "bad_decision_window") + " " + str(obj, "domain_context")));
        }

        double damageDealtPer100() {
            return damageDealt / Math.max(1.0, lastTick - firstTick) * 100.0;
        }

        double damageTakenPer100() {
            return damageTaken / Math.max(1.0, lastTick - firstTick) * 100.0;
        }

        double entropy() {
            double total = Math.max(1.0, decisions);
            double entropy = 0.0;
            for (int count : actions.values()) {
                double p = count / total;
                entropy -= p * (Math.log(p) / Math.log(2.0));
            }
            return entropy;
        }

        double avgDomainResponse() {
            return domainResponseTicks / Math.max(1.0, domainResponses);
        }

        double rate(int numerator, int denominator) {
            return numerator / Math.max(1.0, (double) denominator);
        }

        double avgAdaptation() {
            return adaptationConfidence / Math.max(1.0, decisions);
        }

        double avgTrap() {
            return trapPressure / Math.max(1.0, decisions);
        }

        double avgEvasion() {
            return evasionPressure / Math.max(1.0, decisions);
        }

        double avgRange() {
            return rangePressure / Math.max(1.0, decisions);
        }

        double avgDomainClear() {
            return domainClearValue / Math.max(1.0, decisions);
        }

        double avgDomainCost() {
            return domainSustainCost / Math.max(1.0, decisions);
        }

        double avgDomainRecovery() {
            return domainRecoveryRisk / Math.max(1.0, decisions);
        }

        String topActions() {
            return actions.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .limit(5)
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(java.util.stream.Collectors.joining("|"));
        }

        String report() {
            StringBuilder out = new StringBuilder(2048);
            out.append("OpSukuna DEV metrics");
            if (!devTelemetryAllowed()) {
                out.append(" (disabled: start dev with -Djjku.opSukunaMetrics=true or env JJKU_OP_SUKUNA_METRICS_DEV=true)");
            }
            out.append('\n');
            out.append("global: decisions=").append(decisions)
                    .append(", fights=").append(fights.size())
                    .append(", damage_dealt/taken_per_100t=").append(fmt(damageDealtPer100())).append('/').append(fmt(damageTakenPer100()))
                    .append(", action_entropy=").append(fmt(entropy()))
                    .append(", max_repeat=").append(maxRepeat)
                    .append(", avg_adjusted_score=").append(fmt(adjustedScore / Math.max(1.0, decisions)))
                    .append('\n');
            out.append("symptoms: stuck_rate=").append(fmt(rate(pathBlocked, decisions)))
                    .append(", evasion_seen=").append(fmt(rate(targetDodge + targetCounter + targetGuard, decisions)))
                    .append(", escaping_rate=").append(fmt(rate(targetEscaping, decisions)))
                    .append(", clear_shot_rate=").append(fmt(rate(clearShot, decisions)))
                    .append(", infinity_waste=").append(infinityWaste)
                    .append(", purple_avoid/hit=").append(fmt(rate(purpleAvoided, purpleThreats))).append('/').append(fmt(rate(purpleHits, purpleThreats)))
                    .append(", gojo_domain_trap=").append(fmt(rate(gojoDomainTraps, gojoDomainSignals)))
                    .append(", avg_domain_response_ticks=").append(fmt(avgDomainResponse()))
                    .append('\n');
            out.append("adaptation: confidence=").append(fmt(avgAdaptation()))
                    .append(", trap/evasion/range=").append(fmt(avgTrap())).append('/').append(fmt(avgEvasion())).append('/').append(fmt(avgRange()))
                    .append(", top_actions=").append(topActions())
                    .append('\n');
            out.append("domain: selected_rate=").append(fmt(rate(domainSelected, decisions)))
                    .append(", cooldown_block_rate=").append(fmt(rate(domainBlockedByCooldown, decisions)))
                    .append(", clear/cost/recovery/score=").append(fmt(avgDomainClear())).append('/').append(fmt(avgDomainCost())).append('/')
                    .append(fmt(avgDomainRecovery())).append('/').append(fmt(domainScore / Math.max(1.0, decisions)))
                    .append('\n');
            out.append("resources: world_cut_capable_rate=").append(fmt(rate(worldCutCapable, decisions)))
                    .append(", world_cut_selected_rate=").append(fmt(rate(worldCutSelected, decisions)))
                    .append(", mahoraga_selected_rate=").append(fmt(rate(mahoragaSelected, decisions)))
                    .append(", range_action_rate=").append(fmt(rate(rangeActions, decisions)))
                    .append(", max_range_streak=").append(maxRangeActionStreak)
                    .append(", max_no_impact_streak=").append(maxNoImpactActionStreak)
                    .append(", defense_action_rate=").append(fmt(rate(defensiveActions, decisions)))
                    .append('\n');
            out.append("focus: gojo_decisions=").append(gojoDecisions)
                    .append(", itadori_yuji_modulo_decisions=").append(yujiModuloDecisions)
                    .append('\n');
            out.append("Sukuna deaths: total=").append(sukunaDeaths)
                    .append(", reasons=").append(topCounts(deathReasons, 5))
                    .append(", targets=").append(topCounts(deathsByTargetType, 4))
                    .append('\n');
            out.append("by_target_type: ").append(topGroups(byTargetType, 4)).append('\n');
            out.append("by_archetype: ").append(topGroups(byArchetype, 4)).append('\n');
            out.append("by_technique: ").append(topGroups(byTechnique, 4)).append('\n');
            out.append("analysis_hints: high stuck/path_blocked => improve movement/escape; high evasion with low hit_confidence => penalize direct slash and favor bait/WorldCut/Open clear-shot; high domain cost/recovery over clear => domain too eager; high infinity_waste => prefer DA/domain/WorldCut only when capable.");
            return out.toString();
        }

        void writeReports(Path dir) throws IOException {
            Files.writeString(dir.resolve("summary.json"), toJson(), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("regressions.md"), regressionsMarkdown(), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("fights.json"), fightsJson(), StandardCharsets.UTF_8);
        }

        String toJson() {
            StringBuilder out = new StringBuilder(4096);
            out.append("{\n");
            jsonNumberLine(out, "schema", 3, true);
            jsonNumberLine(out, "decisions", decisions, true);
            jsonNumberLine(out, "fights", fights.size(), true);
            jsonNumberLine(out, "sukuna_deaths", sukunaDeaths, true);
            jsonNumberLine(out, "domain_death_or_trap_rate", rate(gojoDomainTraps, Math.max(1, gojoDomainSignals)), true);
            jsonNumberLine(out, "avg_domain_response_ticks", avgDomainResponse(), true);
            jsonNumberLine(out, "simple_domain_cooldown_block_rate", rate(domainBlockedByCooldown, decisions), true);
            jsonNumberLine(out, "path_blocked_rate", rate(pathBlocked, decisions), true);
            jsonNumberLine(out, "world_cut_selected_rate", rate(worldCutSelected, decisions), true);
            jsonNumberLine(out, "mahoraga_selected_rate", rate(mahoragaSelected, decisions), true);
            jsonNumberLine(out, "damage_dealt_per_100t", damageDealtPer100(), true);
            jsonNumberLine(out, "damage_taken_per_100t", damageTakenPer100(), true);
            jsonStringLine(out, "top_actions", topActions(), true);
            out.append("  \"death_reasons\": ").append(countsJson(deathReasons)).append(",\n");
            out.append("  \"deaths_by_target_type\": ").append(countsJson(deathsByTargetType)).append(",\n");
            out.append("  \"deaths_by_archetype\": ").append(countsJson(deathsByArchetype)).append(",\n");
            out.append("  \"deaths_by_technique\": ").append(countsJson(deathsByTechnique)).append(",\n");
            out.append("  \"deaths_by_domain\": ").append(countsJson(deathsByDomain)).append(",\n");
            out.append("  \"by_target_type\": ").append(groupsJson(byTargetType)).append(",\n");
            out.append("  \"by_archetype\": ").append(groupsJson(byArchetype)).append(",\n");
            out.append("  \"by_technique\": ").append(groupsJson(byTechnique)).append('\n');
            out.append("}\n");
            return out.toString();
        }

        String regressionsMarkdown() {
            StringBuilder out = new StringBuilder(2048);
            out.append("# OpSukuna AI Regressions\n\n");
            out.append("## Symptoms\n");
            addSymptom(out, rate(pathBlocked, decisions) > 0.18, "High stuck/path blocked rate: " + fmt(rate(pathBlocked, decisions)));
            addSymptom(out, rate(gojoDomainTraps, gojoDomainSignals) > 0.28, "High Gojo/Muryokusho domain trap rate: " + fmt(rate(gojoDomainTraps, gojoDomainSignals)));
            addSymptom(out, avgDomainResponse() > 45.0 && domainResponses > 0, "Slow domain response: " + fmt(avgDomainResponse()) + " ticks");
            addSymptom(out, maxNoImpactActionStreak >= 4, "Repeated no-impact actions: max streak " + maxNoImpactActionStreak);
            addSymptom(out, infinityWaste > 0, "Infinity waste actions observed: " + infinityWaste);
            if (out.toString().endsWith("## Symptoms\n")) {
                out.append("- No strong regression symptom detected in current metrics.\n");
            }
            out.append("\n## Worst Matchups\n");
            out.append("- target_type: ").append(topGroups(byTargetType, 5)).append('\n');
            out.append("- technique: ").append(topGroups(byTechnique, 5)).append('\n');
            out.append("- archetype: ").append(topGroups(byArchetype, 5)).append('\n');
            out.append("\n## Repeated Bad Decisions\n");
            out.append("- top_actions=").append(topActions()).append(", max_repeat=").append(maxRepeat)
                    .append(", max_range_streak=").append(maxRangeActionStreak)
                    .append(", max_no_impact_streak=").append(maxNoImpactActionStreak).append('\n');
            out.append("\n## Avoidable Deaths\n");
            if (sukunaDeaths <= 0) {
                out.append("- No Sukuna deaths recorded.\n");
            } else {
                out.append("- deaths=").append(sukunaDeaths)
                        .append(", reasons=").append(topCounts(deathReasons, 8))
                        .append(", target_type=").append(topCounts(deathsByTargetType, 5))
                        .append(", domain=").append(topCounts(deathsByDomain, 5)).append('\n');
                fights.values().stream()
                        .filter(fight -> "sukuna_dead".equals(fight.outcome))
                        .sorted(Comparator.comparingDouble((FightStats fight) -> fight.lastTick).reversed())
                        .limit(5)
                        .forEach(fight -> out.append("- ").append(fight.fightId)
                                .append(": reason=").append(fight.deathReason)
                                .append(", target=").append(fight.targetType)
                                .append(", last_action=").append(fight.lastAction)
                                .append(", recent=").append(fight.badDecisionWindow).append('\n'));
            }
            out.append("\n## Recommended Next Probe\n");
            out.append("- Reproduce the worst target/technique row above and inspect `decision`, `domain_state`, `movement_state`, and `action_result` JSONL events around the first losing window.\n");
            return out.toString();
        }

        private void addSymptom(StringBuilder out, boolean active, String text) {
            if (active) {
                out.append("- ").append(text).append('\n');
            }
        }

        private String fightsJson() {
            StringBuilder out = new StringBuilder(4096);
            out.append("[\n");
            List<FightStats> ordered = new ArrayList<>(fights.values());
            ordered.sort(Comparator.comparingDouble((FightStats fight) -> fight.damageTaken).reversed());
            for (int i = 0; i < ordered.size(); i++) {
                if (i > 0) out.append(",\n");
                out.append(ordered.get(i).toJson());
            }
            out.append("\n]\n");
            return out.toString();
        }

        private String groupsJson(Map<String, GroupStats> groups) {
            StringBuilder out = new StringBuilder();
            out.append("[");
            List<GroupStats> ordered = new ArrayList<>(groups.values());
            ordered.sort(Comparator.comparingInt((GroupStats stats) -> stats.decisions).reversed());
            for (int i = 0; i < Math.min(10, ordered.size()); i++) {
                if (i > 0) out.append(',');
                out.append(ordered.get(i).toJson());
            }
            out.append("]");
            return out.toString();
        }

        private String countsJson(Map<String, Integer> counts) {
            StringBuilder out = new StringBuilder();
            out.append("{");
            List<Map.Entry<String, Integer>> ordered = new ArrayList<>(counts.entrySet());
            ordered.sort(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()));
            for (int i = 0; i < ordered.size(); i++) {
                if (i > 0) out.append(',');
                out.append('"').append(escape(ordered.get(i).getKey())).append("\":").append(ordered.get(i).getValue());
            }
            out.append("}");
            return out.toString();
        }

        private void jsonNumberLine(StringBuilder out, String key, double value, boolean comma) {
            out.append("  \"").append(key).append("\": ").append(Double.isFinite(value) ? value : 0.0);
            if (comma) out.append(',');
            out.append('\n');
        }

        private void jsonStringLine(StringBuilder out, String key, String value, boolean comma) {
            out.append("  \"").append(key).append("\": \"").append(escape(value)).append("\"");
            if (comma) out.append(',');
            out.append('\n');
        }

        private String topGroups(Map<String, GroupStats> groups, int limit) {
            return groups.values().stream()
                    .sorted(Comparator.comparingInt((GroupStats stats) -> stats.decisions).reversed())
                    .limit(limit)
                    .map(GroupStats::brief)
                    .collect(java.util.stream.Collectors.joining(" || "));
        }

        private String topCounts(Map<String, Integer> counts, int limit) {
            if (counts.isEmpty()) {
                return "none";
            }
            return counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .limit(limit)
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(java.util.stream.Collectors.joining("|"));
        }

        private static void addCount(Map<String, Integer> counts, String key) {
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }

        private static String normalizeKey(String key) {
            return key == null || key.isEmpty() ? "unknown" : key;
        }

        private static String extractDomainProfile(String text) {
            if (text == null || text.isEmpty()) return "unknown";
            if (text.contains("GOJO_MURYO")) return "GOJO_MURYO";
            if (text.contains("MAHITO_SOUL")) return "MAHITO_SOUL";
            if (text.contains("HIGURUMA_JUDGEMAN")) return "HIGURUMA_JUDGEMAN";
            if (text.contains("HAKARI_JACKPOT")) return "HAKARI_JACKPOT";
            if (text.contains("GENERIC_DOMAIN")) return "GENERIC_DOMAIN";
            return "unknown";
        }
    }

    private static final class GroupStats {
        final String key;
        final Map<String, Integer> actions = new HashMap<>();
        int decisions;
        double damageDealt;
        double damageTaken;
        double targetThreat;
        double targetPower;
        double hitConfidence;
        double adaptation;
        double trap;
        double evasion;
        double range;
        double domainClear;
        double domainCost;
        double domainRecovery;
        int stuck;
        int dodge;
        int counter;
        int guard;
        int escaping;
        int domainSelected;
        int worldCutSelected;

        GroupStats(String key) {
            this.key = key == null || key.isEmpty() ? "unknown" : key;
        }

        void accept(JsonObject obj) {
            decisions++;
            String action = str(obj, "action");
            String kind = str(obj, "kind");
            actions.put(action, actions.getOrDefault(action, 0) + 1);
            damageDealt += dbl(obj, "damage_dealt");
            damageTaken += dbl(obj, "damage_taken");
            targetThreat += dbl(obj, "target_threat");
            targetPower += dbl(obj, "target_power");
            hitConfidence += dbl(obj, "hit_confidence");
            adaptation += dbl(obj, "adaptation_confidence");
            trap += dbl(obj, "trap_pressure");
            evasion += dbl(obj, "evasion_pressure");
            range += dbl(obj, "range_pressure");
            domainClear += dbl(obj, "domain_clear_value");
            domainCost += dbl(obj, "domain_sustain_cost");
            domainRecovery += dbl(obj, "domain_recovery_risk");
            if (bool(obj, "stuck") || bool(obj, "path_blocked")) stuck++;
            if (bool(obj, "target_dodge")) dodge++;
            if (bool(obj, "target_counter")) counter++;
            if (bool(obj, "target_guard")) guard++;
            if (bool(obj, "target_escaping")) escaping++;
            if ("DOMAIN".equals(kind)) domainSelected++;
            if ("WORLD_CUT".equals(kind)) worldCutSelected++;
        }

        String brief() {
            double total = Math.max(1.0, decisions);
            return key
                    + "{n=" + decisions
                    + ",dmg=" + fmt(damageDealt / total) + "/" + fmt(damageTaken / total)
                    + ",threat=" + fmt(targetThreat / total)
                    + ",power=" + fmt(targetPower / total)
                    + ",hit=" + fmt(hitConfidence / total)
                    + ",adapt=" + fmt(adaptation / total)
                    + ",trap/eva/range=" + fmt(trap / total) + "/" + fmt(evasion / total) + "/" + fmt(range / total)
                    + ",stuck/eva/escape=" + fmt(stuck / total) + "/" + fmt((dodge + counter + guard) / total) + "/" + fmt(escaping / total)
                    + ",domain=" + fmt(domainSelected / total) + " c/c/r=" + fmt(domainClear / total) + "/" + fmt(domainCost / total) + "/" + fmt(domainRecovery / total)
                    + ",wc=" + fmt(worldCutSelected / total)
                    + ",top=" + actions.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .limit(3)
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(java.util.stream.Collectors.joining("|"))
                    + "}";
        }

        String toJson() {
            double total = Math.max(1.0, decisions);
            return "{\"key\":\"" + escape(key) + "\",\"decisions\":" + decisions
                    + ",\"damage_dealt_avg\":" + (damageDealt / total)
                    + ",\"damage_taken_avg\":" + (damageTaken / total)
                    + ",\"stuck_rate\":" + (stuck / total)
                    + ",\"domain_selected_rate\":" + (domainSelected / total)
                    + ",\"world_cut_selected_rate\":" + (worldCutSelected / total)
                    + ",\"top_actions\":\"" + escape(actions.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .limit(3)
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(java.util.stream.Collectors.joining("|"))) + "\"}";
        }
    }

    private static final class FightStats {
        final String fightId;
        final Map<String, Integer> actions = new HashMap<>();
        String targetType = "";
        String techniqueKey = "";
        String archetype = "";
        double firstTick = -1.0;
        double lastTick;
        double damageDealt;
        double damageTaken;
        int decisions;
        int domainSignals;
        int domainGap;
        int pathBlocked;
        int actionBlocked;
        int fallbackUsed;
        String outcome = "";
        String deathReason = "";
        String lastAction = "";
        String badDecisionWindow = "";

        FightStats(String fightId) {
            this.fightId = fightId == null || fightId.isEmpty() ? "unknown" : fightId;
        }

        void accept(JsonObject obj) {
            decisions++;
            double tick = dbl(obj, "game_time");
            if (firstTick < 0.0) firstTick = tick;
            lastTick = Math.max(lastTick, tick);
            targetType = str(obj, "target_type");
            techniqueKey = str(obj, "technique_key");
            archetype = str(obj, "archetype");
            damageDealt += dbl(obj, "damage_dealt");
            damageTaken += dbl(obj, "damage_taken");
            String action = str(obj, "action");
            lastAction = action;
            actions.put(action, actions.getOrDefault(action, 0) + 1);
            if (bool(obj, "target_domain") || bool(obj, "target_casting_domain") || bool(obj, "single_dominant_sure_hit")) domainSignals++;
            if (bool(obj, "anti_domain_gap_imminent")) domainGap++;
            if (bool(obj, "path_blocked")) pathBlocked++;
            if (!bool(obj, "action_started")) actionBlocked++;
            if (!str(obj, "fallback_action").isEmpty()) fallbackUsed++;
        }

        void acceptOutcome(JsonObject obj) {
            String event = str(obj, "event");
            String objOutcome = str(obj, "outcome");
            if (!objOutcome.isEmpty()) {
                outcome = objOutcome;
            } else if ("sukuna_death".equals(event)) {
                outcome = "sukuna_dead";
            }
            if (!str(obj, "death_reason").isEmpty()) deathReason = str(obj, "death_reason");
            if (!str(obj, "target_type").isEmpty()) targetType = str(obj, "target_type");
            if (!str(obj, "technique_key").isEmpty()) techniqueKey = str(obj, "technique_key");
            if (!str(obj, "last_action").isEmpty()) lastAction = str(obj, "last_action");
            if (!str(obj, "bad_decision_window").isEmpty()) badDecisionWindow = str(obj, "bad_decision_window");
            if (!str(obj, "recent_decisions").isEmpty()) badDecisionWindow = str(obj, "recent_decisions");
            double tick = dbl(obj, "game_time");
            if (firstTick < 0.0 && tick > 0.0) firstTick = tick;
            lastTick = Math.max(lastTick, tick);
        }

        String toJson() {
            return "  {\"fight_id\":\"" + escape(fightId) + "\",\"target_type\":\"" + escape(targetType)
                    + "\",\"technique_key\":\"" + escape(techniqueKey) + "\",\"archetype\":\"" + escape(archetype)
                    + "\",\"outcome\":\"" + escape(outcome)
                    + "\",\"death_reason\":\"" + escape(deathReason)
                    + "\",\"last_action\":\"" + escape(lastAction)
                    + "\",\"ticks\":" + Math.max(0.0, lastTick - firstTick)
                    + ",\"decisions\":" + decisions
                    + ",\"damage_dealt\":" + damageDealt
                    + ",\"damage_taken\":" + damageTaken
                    + ",\"domain_signals\":" + domainSignals
                    + ",\"anti_domain_gap_ticks\":" + domainGap
                    + ",\"path_blocked\":" + pathBlocked
                    + ",\"action_blocked\":" + actionBlocked
                    + ",\"fallback_used\":" + fallbackUsed
                    + ",\"bad_decision_window\":\"" + escape(badDecisionWindow) + "\""
                    + ",\"top_actions\":\"" + escape(actions.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .limit(5)
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(java.util.stream.Collectors.joining("|"))) + "\"}";
        }
    }

    private static final class BenchmarkContext {
        final String benchmarkId;
        final String scenario;
        final String variant;
        final int runIndex;
        final int arenaIndex;

        BenchmarkContext(String benchmarkId, String scenario, String variant, int runIndex, int arenaIndex) {
            this.benchmarkId = benchmarkId == null ? "" : benchmarkId;
            this.scenario = scenario == null ? "" : scenario;
            this.variant = variant == null ? "" : variant;
            this.runIndex = runIndex;
            this.arenaIndex = arenaIndex;
        }
    }
}
