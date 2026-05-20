package com.jujutsu.jujutsucraftaddon.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Mod.EventBusSubscriber
public final class OpSukunaBenchmarkRunner {
    private static final String TEST_TAG = "jjku_op_sukuna_benchmark";
    private static final String SCENARIO_ITADORI_MODULO = "itadori_modulo";
    private static final ResourceLocation SUKUNA_PERFECT = new ResourceLocation("jujutsucraft", "sukuna_perfect");
    private static final int DEFAULT_FIGHTS = 100;
    private static final int DEFAULT_DURATION_TICKS = 2400;
    private static final int DEFAULT_PARALLEL_ARENAS = 2;
    private static final int MAX_PARALLEL_ARENAS = 32;
    private static final int DEFAULT_ARENA_SPACING = 1024;
    private static final int MIN_SAFE_ARENA_SPACING = 1024;
    private static final int MAX_ARENA_SPACING = 4096;
    private static final int DEFAULT_ARENA_STARTS_PER_TICK = 1;
    private static final int DEFAULT_SAMPLE_INTERVAL_TICKS = 10;
    private static final int ARENA_WARMUP_TICKS = 8;
    private static final int ARENA_COOLDOWN_TICKS = 40;
    private static final int ARENA_PREP_TIMEOUT_TICKS = 320;
    private static final int ARENA_BUILD_ROWS_PER_TICK = 7;
    private static final int ARENA_PRESPAWN_PURGE_TICKS = 6;
    private static final int INSTANT_INVALID_TICKS = 5;
    private static final int ARENA_BUILD_RADIUS = 22;
    private static final int ARENA_MOVEMENT_RADIUS = 34;
    private static final int ARENA_CHUNK_MARGIN_BLOCKS = 48;
    private static final int CHUNK_STABLE_READY_TICKS = 5;
    private static final int AI_HOOK_LOST_GRACE_TICKS = 80;
    private static final int AI_HOOK_LOST_MAX_TICKS = 40;
    private static final int TARGET_LOCK_LOST_MAX_TICKS = 40;
    private static final int TICK_STALL_MAX_TICKS = 40;
    private static final DateTimeFormatter BENCHMARK_ID_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

    private static Batch active;
    private static String lastSummary = "No benchmark has finished yet.";
    private static Path lastExportDir;

    private OpSukunaBenchmarkRunner() {
    }

    public static boolean isBenchmarkActive() {
        return active != null;
    }

    public static int run(CommandSourceStack source, String scenario, int fights, int durationTicks, int parallelArenas, String variantSet) {
        if (active != null) {
            source.sendFailure(Component.literal("OpSukuna benchmark already running: " + active.status()));
            return 0;
        }
        if (!OpSukunaBrainTelemetry.devTelemetryAllowed()) {
            source.sendFailure(Component.literal("OpSukuna benchmark requires -Djjku.opSukunaMetrics=true or JJKU_OP_SUKUNA_METRICS_DEV=true."));
            return 0;
        }
        if (!SCENARIO_ITADORI_MODULO.equals(scenario)) {
            source.sendFailure(Component.literal("Unsupported benchmark scenario: " + scenario));
            return 0;
        }
        ServerLevel level = source.getLevel();
        EntityType<?> sukunaType = ForgeRegistries.ENTITY_TYPES.getValue(SUKUNA_PERFECT);
        EntityType<?> targetType = findItadoriModuloType();
        if (sukunaType == null || targetType == null) {
            source.sendFailure(Component.literal("Missing benchmark entity type: sukuna=" + sukunaType + ", itadoriModulo=" + targetType));
            return 0;
        }

        int safeFights = Math.max(1, Math.min(fights, 10000));
        int safeDuration = Math.max(200, Math.min(durationTicks, 12000));
        int safeParallel = Math.max(1, Math.min(parallelArenas, Math.min(maxParallelByForcedChunks(), Math.min(MAX_PARALLEL_ARENAS, safeFights))));
        String safeVariantSet = variantSet == null || variantSet.isEmpty() ? "standard" : variantSet;
        int arenaSpacing = intEnvOrProperty("JJKU_OP_SUKUNA_BENCH_ARENA_SPACING", "jjku.opSukunaBenchArenaSpacing", DEFAULT_ARENA_SPACING, 64, MAX_ARENA_SPACING);
        int startsPerTick = intEnvOrProperty("JJKU_OP_SUKUNA_BENCH_ARENA_STARTS_PER_TICK", "jjku.opSukunaBenchArenaStartsPerTick", DEFAULT_ARENA_STARTS_PER_TICK, 1, 8);
        String telemetryProfile = envOrProperty("JJKU_OP_SUKUNA_BENCH_TELEMETRY_PROFILE", "jjku.opSukunaBenchTelemetryProfile", "balanced").toLowerCase(Locale.ROOT);
        int sampleInterval = intEnvOrProperty("JJKU_OP_SUKUNA_BENCH_SAMPLE_INTERVAL", "jjku.opSukunaBenchSampleInterval",
                sampleIntervalForProfile(telemetryProfile), 2, 80);
        BlockPos center = BlockPos.containing(source.getPosition());
        active = new Batch(level, center, sukunaType, targetType, safeFights, safeDuration, safeParallel, safeVariantSet, source.getTextName(), false, arenaSpacing, startsPerTick, telemetryProfile, sampleInterval);
        if (!active.prepare()) {
            source.sendFailure(Component.literal("Failed to start OpSukuna benchmark: " + active.failureReason));
            active = null;
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Started OpSukuna benchmark: " + active.status()), true);
        return 1;
    }

    public static int runDefault(CommandSourceStack source) {
        return run(source, SCENARIO_ITADORI_MODULO, DEFAULT_FIGHTS, DEFAULT_DURATION_TICKS, DEFAULT_PARALLEL_ARENAS, "standard");
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!autoBenchmarkRequested()) {
            return;
        }
        if (active != null || !OpSukunaBrainTelemetry.devTelemetryAllowed()) {
            return;
        }
        ServerLevel level = event.getServer().overworld();
        EntityType<?> sukunaType = ForgeRegistries.ENTITY_TYPES.getValue(SUKUNA_PERFECT);
        EntityType<?> targetType = findItadoriModuloType();
        if (sukunaType == null || targetType == null) {
            event.getServer().getPlayerList().broadcastSystemMessage(Component.literal("JJKU_BENCH_FAILED missing_entity_type sukuna=" + sukunaType + ", itadoriModulo=" + targetType), false);
            return;
        }
        String scenario = envOrProperty("JJKU_OP_SUKUNA_BENCH_SCENARIO", "jjku.opSukunaBenchScenario", SCENARIO_ITADORI_MODULO);
        if (!SCENARIO_ITADORI_MODULO.equals(scenario)) {
            event.getServer().getPlayerList().broadcastSystemMessage(Component.literal("JJKU_BENCH_FAILED unsupported_scenario=" + scenario), false);
            return;
        }
        int fights = intEnvOrProperty("JJKU_OP_SUKUNA_BENCH_FIGHTS", "jjku.opSukunaBenchFights", DEFAULT_FIGHTS, 1, 10000);
        int duration = intEnvOrProperty("JJKU_OP_SUKUNA_BENCH_DURATION", "jjku.opSukunaBenchDuration", DEFAULT_DURATION_TICKS, 200, 12000);
        int parallel = intEnvOrProperty("JJKU_OP_SUKUNA_BENCH_PARALLEL", "jjku.opSukunaBenchParallel", DEFAULT_PARALLEL_ARENAS, 1, MAX_PARALLEL_ARENAS);
        parallel = Math.min(Math.min(parallel, fights), maxParallelByForcedChunks());
        String variantSet = envOrProperty("JJKU_OP_SUKUNA_BENCH_VARIANT_SET", "jjku.opSukunaBenchVariantSet", "standard");
        boolean autoStop = boolEnvOrProperty("JJKU_OP_SUKUNA_BENCH_AUTO_STOP", "jjku.opSukunaBenchAutoStop", false);
        int arenaSpacing = intEnvOrProperty("JJKU_OP_SUKUNA_BENCH_ARENA_SPACING", "jjku.opSukunaBenchArenaSpacing", DEFAULT_ARENA_SPACING, 64, MAX_ARENA_SPACING);
        int startsPerTick = intEnvOrProperty("JJKU_OP_SUKUNA_BENCH_ARENA_STARTS_PER_TICK", "jjku.opSukunaBenchArenaStartsPerTick", DEFAULT_ARENA_STARTS_PER_TICK, 1, 8);
        String telemetryProfile = envOrProperty("JJKU_OP_SUKUNA_BENCH_TELEMETRY_PROFILE", "jjku.opSukunaBenchTelemetryProfile", "balanced").toLowerCase(Locale.ROOT);
        int sampleInterval = intEnvOrProperty("JJKU_OP_SUKUNA_BENCH_SAMPLE_INTERVAL", "jjku.opSukunaBenchSampleInterval",
                sampleIntervalForProfile(telemetryProfile), 2, 80);
        active = new Batch(level, level.getSharedSpawnPos(), sukunaType, targetType, fights, duration, parallel, variantSet, "auto", autoStop, arenaSpacing, startsPerTick, telemetryProfile, sampleInterval);
        if (!active.prepare()) {
            event.getServer().getPlayerList().broadcastSystemMessage(Component.literal("JJKU_BENCH_FAILED " + active.failureReason), false);
            active = null;
            return;
        }
        event.getServer().getPlayerList().broadcastSystemMessage(Component.literal("JJKU_BENCH_STARTED " + active.status()), false);
    }

    public static int status(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(active == null ? "No OpSukuna benchmark running. Last: " + lastSummary : active.status()), false);
        return active == null ? 0 : 1;
    }

    public static int stop(CommandSourceStack source) {
        if (active == null) {
            source.sendSuccess(() -> Component.literal("No OpSukuna benchmark running."), false);
            return 0;
        }
        Batch batch = active;
        active = null;
        batch.stop("manual_stop");
        source.sendSuccess(() -> Component.literal("Stopped OpSukuna benchmark: " + lastSummary), true);
        return 1;
    }

    public static int export(CommandSourceStack source) {
        if (active != null) {
            source.sendFailure(Component.literal("Benchmark is still running: " + active.status()));
            return 0;
        }
        if (lastExportDir == null) {
            source.sendSuccess(() -> Component.literal("No benchmark export available yet."), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Last OpSukuna benchmark export: " + lastExportDir + "\n" + lastSummary), true);
        return 1;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active == null) {
            return;
        }
        Batch batch = active;
        batch.tick();
        if (batch.done) {
            active = null;
        }
    }

    private static EntityType<?> findItadoriModuloType() {
        for (ResourceLocation id : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            String name = id.toString().toLowerCase(Locale.ROOT);
            if (name.equals("jjsk:itadori_yuji_modulo")
                    || name.equals("jujutsucrafts:itadori_yuji_modulo")
                    || name.equals("jujutsucraftaddon:itadori_yuji_modulo")
                    || name.contains("itadori_yuji_modulo")) {
                return ForgeRegistries.ENTITY_TYPES.getValue(id);
            }
        }
        return null;
    }

    private static boolean autoBenchmarkRequested() {
        return Boolean.getBoolean("jjku.opSukunaBench")
                || "true".equalsIgnoreCase(System.getenv("JJKU_OP_SUKUNA_BENCH"));
    }

    private static String envOrProperty(String envKey, String propertyKey, String fallback) {
        String value = System.getProperty(propertyKey);
        if (value == null || value.isEmpty()) {
            value = System.getenv(envKey);
        }
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static int intEnvOrProperty(String envKey, String propertyKey, int fallback, int min, int max) {
        String value = envOrProperty(envKey, propertyKey, "");
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean boolEnvOrProperty(String envKey, String propertyKey, boolean fallback) {
        String value = envOrProperty(envKey, propertyKey, "");
        return value.isEmpty() ? fallback : Boolean.parseBoolean(value);
    }

    private static int sampleIntervalForProfile(String profile) {
        if ("minimal".equalsIgnoreCase(profile)) {
            return 20;
        }
        if ("full".equalsIgnoreCase(profile)) {
            return 5;
        }
        return DEFAULT_SAMPLE_INTERVAL_TICKS;
    }

    private static int maxParallelByForcedChunks() {
        int radius = Math.max(ARENA_BUILD_RADIUS, ARENA_MOVEMENT_RADIUS) + ARENA_CHUNK_MARGIN_BLOCKS;
        int chunksPerAxis = Math.floorDiv(radius, 16) * 2 + 3;
        int estimatedChunksPerArena = chunksPerAxis * chunksPerAxis;
        return Math.max(1, Math.min(MAX_PARALLEL_ARENAS, 768 / Math.max(1, estimatedChunksPerArena)));
    }

    private static final class Batch {
        private final ServerLevel level;
        private final BlockPos origin;
        private final EntityType<?> sukunaType;
        private final EntityType<?> targetType;
        private final int totalFights;
        private final int durationTicks;
        private final int parallelArenas;
        private final int arenaSpacing;
        private final int startsPerTick;
        private final String telemetryProfile;
        private final int sampleIntervalTicks;
        private final String variantSet;
        private final String requester;
        private final boolean autoStopServer;
        private final String benchmarkId;
        private boolean previousMetricsRule;
        private boolean previousOpSukunaRule;
        private boolean previousMobSpawningRule;
        private boolean previousSpawnChangerRule;
        private int effectiveArenaSpacing;
        private boolean unsafeArenaSpacingAdjusted;
        private final List<Arena> arenas = new ArrayList<>();
        private final List<FightResult> results = new ArrayList<>();
        private int started;
        private int finished;
        private int wins;
        private int losses;
        private int timeouts;
        private int errors;
        private int invalidLab;
        private boolean done;
        private String failureReason = "";

        private Batch(ServerLevel level, BlockPos origin, EntityType<?> sukunaType, EntityType<?> targetType, int totalFights,
                      int durationTicks, int parallelArenas, String variantSet, String requester, boolean autoStopServer,
                      int arenaSpacing, int startsPerTick, String telemetryProfile, int sampleIntervalTicks) {
            this.level = level;
            this.origin = origin;
            this.sukunaType = sukunaType;
            this.targetType = targetType;
            this.totalFights = totalFights;
            this.durationTicks = durationTicks;
            this.parallelArenas = parallelArenas;
            this.arenaSpacing = arenaSpacing;
            this.startsPerTick = startsPerTick;
            this.telemetryProfile = telemetryProfile == null || telemetryProfile.isEmpty() ? "balanced" : telemetryProfile;
            this.sampleIntervalTicks = Math.max(2, sampleIntervalTicks);
            this.variantSet = variantSet;
            this.requester = requester == null || requester.isEmpty() ? "server" : requester;
            this.autoStopServer = autoStopServer;
            this.benchmarkId = "opsukuna-" + SCENARIO_ITADORI_MODULO + "-" + LocalDateTime.now().format(BENCHMARK_ID_FORMAT);
        }

        private boolean prepare() {
            previousMetricsRule = level.getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_OP_SUKUNA_METRICS);
            previousOpSukunaRule = level.getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_OP_SUKUNA);
            previousMobSpawningRule = level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
            previousSpawnChangerRule = level.getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_SPAWN_CHANGER);
            level.getGameRules().getRule(JujutsucraftaddonModGameRules.JJKU_OP_SUKUNA_METRICS).set(true, level.getServer());
            level.getGameRules().getRule(JujutsucraftaddonModGameRules.JJKU_OP_SUKUNA).set(true, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getGameRules().getRule(JujutsucraftaddonModGameRules.JJKU_SPAWN_CHANGER).set(false, level.getServer());
            int recommendedSpacing = recommendedArenaSpacing();
            effectiveArenaSpacing = Math.max(arenaSpacing, recommendedSpacing);
            unsafeArenaSpacingAdjusted = effectiveArenaSpacing > arenaSpacing;
            OpSukunaBrainTelemetry.reset();
            int columns = Math.max(1, (int) Math.ceil(Math.sqrt(parallelArenas)));
            for (int i = 0; i < parallelArenas; i++) {
                BlockPos center = origin.offset((i % columns) * effectiveArenaSpacing, 0, (i / columns) * effectiveArenaSpacing);
                arenas.add(new Arena(i, center));
            }
            purgeStaleBenchmarkEntities();
            OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variantSet, -1, -1,
                    "benchmark_start", "running", "fights=" + totalFights + ",duration_ticks=" + durationTicks
                            + ",parallel_arenas=" + parallelArenas + ",arena_spacing=" + arenaSpacing
                            + ",effective_arena_spacing=" + effectiveArenaSpacing
                            + ",unsafe_arena_spacing_adjusted=" + unsafeArenaSpacingAdjusted
                            + ",op_sukuna=true,metrics=true,do_mob_spawning=false,spawn_changer=false"
                            + ",starts_per_tick=" + startsPerTick
                            + ",telemetry_profile=" + telemetryProfile + ",sample_interval_ticks=" + sampleIntervalTicks);
            return true;
        }

        private void tick() {
            for (Arena arena : arenas) {
                arena.tick();
            }
            int startedThisTick = 0;
            for (Arena arena : arenas) {
                if (arena.canAcceptRun() && started < totalFights) {
                    arena.schedule(++started);
                    startedThisTick++;
                    if (startedThisTick >= startsPerTick) {
                        break;
                    }
                }
            }
            if (started < totalFights && arenas.stream().allMatch(arena -> arena.state == ArenaState.QUARANTINED)) {
                int remaining = totalFights - started;
                started = totalFights;
                finished += remaining;
                invalidLab += remaining;
                OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variantSet, -1, -1,
                        "lab_invalid", "all_arenas_quarantined", "remaining_fights=" + remaining);
            }
            if (finished >= totalFights) {
                finish("complete");
            }
        }

        private void stop(String reason) {
            for (Arena arena : arenas) {
                arena.cleanup();
                arena.forceChunks(false);
            }
            restoreRules();
            finish(reason);
        }

        private void finish(String reason) {
            if (done) {
                return;
            }
            done = true;
            for (Arena arena : arenas) {
                arena.cleanup();
                arena.forceChunks(false);
            }
            restoreRules();
            int validFights = Math.max(0, finished - invalidLab);
            double winRate = totalFights <= 0 ? 0.0 : wins / (double) totalFights;
            double validWinRate = validFights <= 0 ? 0.0 : wins / (double) validFights;
            boolean passed = invalidLab == 0 && validFights > 0 && validWinRate >= 0.70;
            OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variantSet, -1, -1,
                    "benchmark_end", reason, "wins=" + wins + ",losses=" + losses + ",timeouts=" + timeouts + ",errors=" + errors
                            + ",invalid_lab=" + invalidLab
                            + ",valid_fights=" + validFights
                            + ",win_rate=" + String.format(Locale.ROOT, "%.3f", winRate)
                            + ",win_rate_valid=" + String.format(Locale.ROOT, "%.3f", validWinRate)
                            + ",lab_valid=" + (invalidLab == 0));
            OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variantSet, -1, -1,
                    "JJKU_BENCH_FINISHED", reason, "wins=" + wins + ",losses=" + losses + ",timeouts=" + timeouts + ",errors=" + errors
                            + ",invalid_lab=" + invalidLab
                            + ",valid_fights=" + validFights
                            + ",win_rate=" + String.format(Locale.ROOT, "%.3f", winRate)
                            + ",win_rate_valid=" + String.format(Locale.ROOT, "%.3f", validWinRate)
                            + ",lab_valid=" + (invalidLab == 0));
            Path dir = OpSukunaBrainTelemetry.exportBenchmarkReports(benchmarkId);
            writeBenchmarkFallbackReports(dir, validWinRate);
            lastExportDir = dir;
            lastSummary = "benchmark_id=" + benchmarkId
                    + ", scenario=" + SCENARIO_ITADORI_MODULO
                    + ", fights=" + finished + "/" + totalFights
                    + ", valid_fights=" + validFights
                    + ", invalid_lab=" + invalidLab
                    + ", wins=" + wins
                    + ", losses=" + losses
                    + ", timeouts=" + timeouts
                    + ", errors=" + errors
                    + ", win_rate=" + String.format(Locale.ROOT, "%.3f", winRate)
                    + ", win_rate_valid=" + String.format(Locale.ROOT, "%.3f", validWinRate)
                    + ", passed=" + passed
                    + ", unsafe_arena_spacing_adjusted=" + unsafeArenaSpacingAdjusted
                    + ", effective_arena_spacing=" + effectiveArenaSpacing
                    + ", export=" + dir;
            OpSukunaBrainTelemetry.clearBenchmarkContext(benchmarkId);
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("JJKU_BENCH_FINISHED " + lastSummary), false);
            if (autoStopServer) {
                level.getServer().halt(false);
            }
        }

        private void restoreRules() {
            level.getGameRules().getRule(JujutsucraftaddonModGameRules.JJKU_OP_SUKUNA_METRICS).set(previousMetricsRule, level.getServer());
            level.getGameRules().getRule(JujutsucraftaddonModGameRules.JJKU_OP_SUKUNA).set(previousOpSukunaRule, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(previousMobSpawningRule, level.getServer());
            level.getGameRules().getRule(JujutsucraftaddonModGameRules.JJKU_SPAWN_CHANGER).set(previousSpawnChangerRule, level.getServer());
        }

        private int recommendedArenaSpacing() {
            double domainRadius = JujutsucraftModVariables.MapVariables.get(level).DomainExpansionRadius * 18.0;
            int dynamic = (int) Math.ceil(domainRadius * 2.0 + 128.0);
            int safe = Math.max(MIN_SAFE_ARENA_SPACING, dynamic);
            return Math.min(MAX_ARENA_SPACING, safe);
        }

        private void purgeStaleBenchmarkEntities() {
            for (Arena arena : arenas) {
                AABB box = new AABB(arena.center).inflate(Math.max(80.0, effectiveArenaSpacing * 0.48));
                for (Entity entity : level.getEntities((Entity) null, box, e -> e.getTags().contains(TEST_TAG))) {
                    entity.discard();
                }
            }
        }

        private void writeBenchmarkFallbackReports(Path dir, double winRate) {
            try {
                Files.createDirectories(dir);
                BenchmarkPostprocessResult processed = postprocessBenchmarkJsonl(dir);
                StringBuilder summary = new StringBuilder(4096);
                summary.append("{\n")
                        .append("  \"benchmark_id\": \"").append(escapeJson(benchmarkId)).append("\",\n")
                        .append("  \"total_fights\": ").append(processed.totalFights).append(",\n")
                        .append("  \"valid_fights\": ").append(processed.validFights).append(",\n")
                        .append("  \"invalid_fights\": ").append(processed.invalidLabFights).append(",\n")
                        .append("  \"wins\": ").append(processed.wins).append(",\n")
                        .append("  \"win_rate\": ").append(String.format(Locale.ROOT, "%.6f", processed.winRate)).append(",\n")
                        .append("  \"win_rate_valid\": ").append(String.format(Locale.ROOT, "%.6f", processed.validWinRate)).append(",\n")
                        .append("  \"passed\": ").append(processed.invalidLabFights == 0 && processed.validWinRate >= 0.70).append(",\n")
                        .append("  \"win_gate\": 0.7,\n")
                        .append("  \"outcomes\": {\"target_dead\": ").append(processed.outcomes.getOrDefault("target_dead", 0))
                        .append(", \"sukuna_dead\": ").append(processed.outcomes.getOrDefault("sukuna_dead", 0))
                        .append(", \"timeout\": ").append(processed.outcomes.getOrDefault("timeout", 0))
                        .append(", \"both_dead\": ").append(processed.outcomes.getOrDefault("both_dead", 0))
                        .append(", \"lab_invalid\": ").append(processed.outcomes.getOrDefault("lab_invalid", 0))
                        .append(", \"runner_error\": ").append(processed.outcomes.getOrDefault("runner_error", 0))
                        .append(", \"errors\": ").append(processed.outcomes.getOrDefault("errors", 0)).append("},\n")
                        .append("  \"lab_invalid_reasons\": ").append(jsonCounterArray(processed.labInvalidReasons)).append(",\n")
                        .append("  \"top_actions\": ").append(jsonCounterArray(processed.topActions)).append(",\n")
                        .append("  \"block_reasons\": ").append(jsonCounterArray(processed.blockReasons)).append(",\n")
                        .append("  \"fallbacks\": ").append(jsonCounterArray(processed.fallbacks)).append(",\n")
                        .append("  \"target_types\": ").append(jsonCounterArray(processed.targetTypes)).append(",\n")
                        .append("  \"gate_reasons\": ").append(jsonCounterArray(processed.gateReasons)).append(",\n")
                        .append("  \"bad_windows\": ").append(processed.badWindowsCount).append(",\n")
                        .append("  \"parse_errors\": ").append(processed.parseErrors).append(",\n")
                        .append("  \"telemetry_quality\": {\n")
                        .append("    \"valid\": ").append(processed.telemetryValid).append(",\n")
                        .append("    \"real_decision_fights\": ").append(processed.realDecisionFights).append(",\n")
                        .append("    \"synthetic_decision_fights\": ").append(processed.syntheticDecisionFights).append(",\n")
                        .append("    \"missing_decision_fights\": ").append(processed.missingDecisionFights).append(",\n")
                        .append("    \"contamination_fights\": ").append(processed.contaminationFights).append(",\n")
                        .append("    \"invalid_lab_fights\": ").append(processed.invalidLabFights).append(",\n")
                        .append("    \"decision_data_valid\": ").append(processed.decisionDataValid).append(",\n")
                        .append("    \"invalid_reason\": \"").append(escapeJson(processed.telemetryInvalidReason)).append("\"\n")
                        .append("  },\n")
                        .append("  \"generated_by\": \"forge_runner_postprocess\"\n")
                        .append("}\n");
                atomicWriteString(dir.resolve("benchmark_summary.json"), summary.toString());

                StringBuilder fightsJsonl = new StringBuilder(Math.max(1024, processed.fights.size() * 380));
                for (FightAnalysis fight : processed.fights) {
                    fightsJsonl.append(fight.toJson()).append('\n');
                }
                atomicWriteString(dir.resolve("fight_summaries.jsonl"), fightsJsonl.toString());

                StringBuilder badJsonl = new StringBuilder(Math.max(1024, processed.badWindows.size() * 200));
                for (String line : processed.badWindows) {
                    badJsonl.append(line).append('\n');
                }
                atomicWriteString(dir.resolve("bad_windows.jsonl"), badJsonl.toString());

                String report = "# OP Sukuna Benchmark\n\n"
                        + "- quality: `" + (processed.invalidLabFights > 0 ? "INVALID_LAB" : (processed.telemetryValid ? "VALID_FOR_AI_TUNING" : "INVALID_FOR_AI_TUNING")) + "`\n"
                        + "- benchmark_id: `" + benchmarkId + "`\n"
                        + "- fights: " + processed.totalFights + "\n"
                        + "- valid_fights: " + processed.validFights + "\n"
                        + "- invalid_fights: " + processed.invalidLabFights + "\n"
                        + "- wins: " + processed.wins + "\n"
                        + "- win_rate: " + String.format(Locale.ROOT, "%.3f", processed.winRate) + "\n"
                        + "- win_rate_valid: " + String.format(Locale.ROOT, "%.3f", processed.validWinRate) + "\n"
                        + "- gate: 0.700\n"
                        + "- passed: " + (processed.invalidLabFights == 0 && processed.validWinRate >= 0.70) + "\n"
                        + "- outcomes: `" + processed.outcomes + "`\n"
                        + "- lab_invalid_reasons: `" + processed.labInvalidReasons + "`\n"
                        + "- gate_reasons: `" + processed.gateReasons + "`\n"
                        + "- bad_windows: " + processed.badWindowsCount + "\n"
                        + "- parse_errors: " + processed.parseErrors + "\n"
                        + "- real_decision_fights: " + processed.realDecisionFights + "\n"
                        + "- synthetic_decision_fights: " + processed.syntheticDecisionFights + "\n"
                        + "- missing_decision_fights: " + processed.missingDecisionFights + "\n"
                        + "- contamination_fights: " + processed.contaminationFights + "\n"
                        + "- telemetry_invalid_reason: `" + processed.telemetryInvalidReason + "`\n"
                        + "- generated_by: `forge_runner_postprocess`\n";
                atomicWriteString(dir.resolve("benchmark_report.md"), report);
            } catch (IOException ignored) {
            }
        }

        private BenchmarkPostprocessResult postprocessBenchmarkJsonl(Path dir) throws IOException {
            Map<Integer, FightAnalysis> fightsByRun = new TreeMap<>();
            int parseErrors = 0;
            try (var files = Files.list(dir)) {
                for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".jsonl")).toList()) {
                    try (var lines = Files.lines(file, StandardCharsets.UTF_8)) {
                        for (String line : (Iterable<String>) lines::iterator) {
                            String trimmed = line == null ? "" : line.trim();
                            if (trimmed.isEmpty()) {
                                continue;
                            }
                            JsonObject obj;
                            try {
                                JsonElement parsed = JsonParser.parseString(trimmed);
                                if (!parsed.isJsonObject()) {
                                    parseErrors++;
                                    continue;
                                }
                                obj = parsed.getAsJsonObject();
                            } catch (RuntimeException ex) {
                                parseErrors++;
                                continue;
                            }
                            String event = str(obj, "event");
                            int runIndex = intNum(obj, "run_index", -1);
                            if (runIndex < 0) {
                                continue;
                            }
                            FightAnalysis fight = fightsByRun.computeIfAbsent(runIndex, FightAnalysis::new);
                            fight.arenaIndex = intNum(obj, "arena_index", fight.arenaIndex);
                            String variant = str(obj, "variant");
                            if (!variant.isEmpty()) {
                                fight.variant = variant;
                            }
                            if ("benchmark_fight_end".equals(event)) {
                                fight.outcome = str(obj, "outcome");
                                fight.parseEndDetails(str(obj, "details"));
                            } else if ("decision".equals(event)) {
                                fight.ingestDecision(obj);
                            } else if ("benchmark_sample".equals(event)) {
                                fight.ingestSample(obj);
                            } else if ("damage_resolved".equals(event)) {
                                fight.damageResolved += num(obj, "actual_damage", 0.0);
                            } else if ("sukuna_damage_resolved".equals(event)) {
                                fight.damageTakenResolved += num(obj, "actual_damage", 0.0);
                            } else if ("target_death_resolved".equals(event)) {
                                fight.targetDeathEvents++;
                            } else if ("arena_contamination".equals(event)) {
                                fight.contaminationEvents++;
                            } else if ("lab_invalid".equals(event)) {
                                fight.labInvalidEvents++;
                                String reason = str(obj, "outcome");
                                if (!reason.isEmpty()) {
                                    fight.labInvalidReason = reason;
                                }
                            } else if ("ai_gate".equals(event)) {
                                fight.ingestGate(obj);
                            }
                        }
                    } catch (UncheckedIOException ignored) {
                    }
                }
            }

            List<FightAnalysis> fights = new ArrayList<>(fightsByRun.values());
            fights.sort(Comparator.comparingInt(f -> f.runIndex));
            for (FightAnalysis fight : fights) {
                fight.finalizeSynthesizedDecisions();
            }

            Map<String, Integer> outcomes = new LinkedHashMap<>();
            Map<String, Integer> topActions = new HashMap<>();
            Map<String, Integer> blockReasons = new HashMap<>();
            Map<String, Integer> fallbacks = new HashMap<>();
            Map<String, Integer> targetTypes = new HashMap<>();
            Map<String, Integer> gateReasons = new HashMap<>();
            Map<String, Integer> labInvalidReasons = new HashMap<>();
            List<String> badWindows = new ArrayList<>();
            int wins = 0;
            int realDecisionFights = 0;
            int syntheticDecisionFights = 0;
            int missingDecisionFights = 0;
            int contaminationFights = 0;
            int invalidLabFights = 0;
            for (FightAnalysis fight : fights) {
                fight.normalizeLabOutcome();
                String outcome = fight.outcome == null || fight.outcome.isEmpty() ? "unknown" : fight.outcome;
                outcomes.put(outcome, outcomes.getOrDefault(outcome, 0) + 1);
                if ("target_dead".equals(outcome)) {
                    wins++;
                }
                if ("lab_invalid".equals(outcome) || fight.labInvalidEvents > 0) {
                    invalidLabFights++;
                    String reason = fight.labInvalidReason == null || fight.labInvalidReason.isEmpty() ? "unknown_lab_failure" : fight.labInvalidReason;
                    labInvalidReasons.put(reason, labInvalidReasons.getOrDefault(reason, 0) + 1);
                }
                if (fight.hasRealDecisions) {
                    realDecisionFights++;
                } else if (fight.syntheticDecisionCount > 0) {
                    syntheticDecisionFights++;
                } else {
                    missingDecisionFights++;
                }
                if (fight.contaminationEvents > 0 || fight.contaminationHits > 0) {
                    contaminationFights++;
                }
                mergeCounter(topActions, fight.topActions);
                mergeCounter(blockReasons, fight.blockReasons);
                mergeCounter(fallbacks, fight.fallbacks);
                if (!fight.targetType.isEmpty()) {
                    targetTypes.put(fight.targetType, targetTypes.getOrDefault(fight.targetType, 0) + 1);
                }
                mergeCounter(gateReasons, fight.gateReasons);
                if (fight.isBadWindow()) {
                    badWindows.add(fight.badWindowJson());
                }
            }

            int total = fights.size();
            double winRate = total <= 0 ? 0.0 : wins / (double) total;
            int validFights = Math.max(0, total - invalidLabFights);
            double validWinRate = validFights <= 0 ? 0.0 : wins / (double) validFights;
            boolean decisionDataValid = contaminationFights == 0 && missingDecisionFights == 0 && parseErrors == 0 && realDecisionFights > 0;
            boolean telemetryValid = invalidLabFights == 0 && decisionDataValid;
            String telemetryInvalidReason = telemetryValid ? "" : ("contamination_fights=" + contaminationFights
                    + ",missing_decision_fights=" + missingDecisionFights
                    + ",parse_errors=" + parseErrors
                    + ",real_decision_fights=" + realDecisionFights
                    + ",invalid_lab_fights=" + invalidLabFights);
            return new BenchmarkPostprocessResult(fights, badWindows, outcomes, topActions, blockReasons, fallbacks, targetTypes, gateReasons,
                    labInvalidReasons, total, validFights, invalidLabFights, wins, winRate, validWinRate, parseErrors,
                    realDecisionFights, syntheticDecisionFights, missingDecisionFights, contaminationFights, telemetryValid, decisionDataValid, telemetryInvalidReason);
        }

        private static void atomicWriteString(Path target, String content) throws IOException {
            Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }

        private static String jsonCounterArray(Map<String, Integer> counter) {
            List<Map.Entry<String, Integer>> ordered = counter.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(16)
                    .toList();
            StringBuilder out = new StringBuilder(256);
            out.append('[');
            boolean first = true;
            for (Map.Entry<String, Integer> entry : ordered) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                out.append("[\"").append(escapeJson(entry.getKey())).append("\",").append(entry.getValue()).append(']');
            }
            out.append(']');
            return out.toString();
        }

        private static void mergeCounter(Map<String, Integer> target, Map<String, Integer> source) {
            for (Map.Entry<String, Integer> entry : source.entrySet()) {
                target.put(entry.getKey(), target.getOrDefault(entry.getKey(), 0) + entry.getValue());
            }
        }

        private static String str(JsonObject obj, String key) {
            if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
                return "";
            }
            try {
                return obj.get(key).getAsString();
            } catch (RuntimeException ignored) {
                return "";
            }
        }

        private static int intNum(JsonObject obj, String key, int fallback) {
            if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
                return fallback;
            }
            try {
                return Math.round(obj.get(key).getAsFloat());
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }

        private static double num(JsonObject obj, String key, double fallback) {
            if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
                return fallback;
            }
            try {
                return obj.get(key).getAsDouble();
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }

        private static boolean bool(JsonObject obj, String key) {
            if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
                return false;
            }
            try {
                return obj.get(key).getAsBoolean();
            } catch (RuntimeException ignored) {
                return false;
            }
        }

        private static final class BenchmarkPostprocessResult {
            private final List<FightAnalysis> fights;
            private final List<String> badWindows;
            private final Map<String, Integer> outcomes;
            private final Map<String, Integer> topActions;
            private final Map<String, Integer> blockReasons;
            private final Map<String, Integer> fallbacks;
            private final Map<String, Integer> targetTypes;
            private final Map<String, Integer> gateReasons;
            private final Map<String, Integer> labInvalidReasons;
            private final int totalFights;
            private final int validFights;
            private final int invalidLabFights;
            private final int wins;
            private final double winRate;
            private final double validWinRate;
            private final int parseErrors;
            private final int badWindowsCount;
            private final int realDecisionFights;
            private final int syntheticDecisionFights;
            private final int missingDecisionFights;
            private final int contaminationFights;
            private final boolean telemetryValid;
            private final boolean decisionDataValid;
            private final String telemetryInvalidReason;

            private BenchmarkPostprocessResult(List<FightAnalysis> fights, List<String> badWindows,
                                              Map<String, Integer> outcomes, Map<String, Integer> topActions,
                                              Map<String, Integer> blockReasons, Map<String, Integer> fallbacks,
                                              Map<String, Integer> targetTypes, Map<String, Integer> gateReasons,
                                              Map<String, Integer> labInvalidReasons, int totalFights, int validFights, int invalidLabFights, int wins,
                                              double winRate, double validWinRate, int parseErrors, int realDecisionFights,
                                              int syntheticDecisionFights, int missingDecisionFights,
                                              int contaminationFights, boolean telemetryValid, boolean decisionDataValid, String telemetryInvalidReason) {
                this.fights = fights;
                this.badWindows = badWindows;
                this.outcomes = outcomes;
                this.topActions = topActions;
                this.blockReasons = blockReasons;
                this.fallbacks = fallbacks;
                this.targetTypes = targetTypes;
                this.gateReasons = gateReasons;
                this.labInvalidReasons = labInvalidReasons;
                this.totalFights = totalFights;
                this.validFights = validFights;
                this.invalidLabFights = invalidLabFights;
                this.wins = wins;
                this.winRate = winRate;
                this.validWinRate = validWinRate;
                this.parseErrors = parseErrors;
                this.badWindowsCount = badWindows.size();
                this.realDecisionFights = realDecisionFights;
                this.syntheticDecisionFights = syntheticDecisionFights;
                this.missingDecisionFights = missingDecisionFights;
                this.contaminationFights = contaminationFights;
                this.telemetryValid = telemetryValid;
                this.decisionDataValid = decisionDataValid;
                this.telemetryInvalidReason = telemetryInvalidReason;
            }
        }

        private static final class FightAnalysis {
            private final int runIndex;
            private int arenaIndex = -1;
            private String variant = "";
            private String outcome = "";
            private String targetType = "";
            private int ticks = 0;
            private double finalSukunaHealth = 0.0;
            private double finalTargetHealth = 0.0;
            private double damageResolved = 0.0;
            private double damageTakenResolved = 0.0;
            private int targetDeathEvents = 0;
            private int contaminationEvents = 0;
            private int contaminationHits = 0;
            private int labInvalidEvents = 0;
            private String labInvalidReason = "";
            private int decisions = 0;
            private int blockedActions = 0;
            private int rangeActions = 0;
            private int noImpactDecisions = 0;
            private int syntheticDecisionCount = 0;
            private int firstOffenseDecisionIndex = -1;
            private boolean hasRealDecisions = false;
            private SampleState previousSample;
            private final Map<String, Integer> topActions = new HashMap<>();
            private final Map<String, Integer> blockReasons = new HashMap<>();
            private final Map<String, Integer> fallbacks = new HashMap<>();
            private final Map<String, Integer> gateReasons = new HashMap<>();

            private FightAnalysis(int runIndex) {
                this.runIndex = runIndex;
            }

            private void parseEndDetails(String details) {
                if (details == null || details.isEmpty()) {
                    return;
                }
                String[] pieces = details.split(",");
                for (String piece : pieces) {
                    int idx = piece.indexOf('=');
                    if (idx < 0) {
                        continue;
                    }
                    String key = piece.substring(0, idx).trim();
                    String value = piece.substring(idx + 1).trim();
                    try {
                        if ("ticks".equals(key)) {
                            ticks = Math.round(Float.parseFloat(value));
                        } else if ("sukuna_health".equals(key)) {
                            finalSukunaHealth = Double.parseDouble(value);
                        } else if ("target_health".equals(key)) {
                            finalTargetHealth = Double.parseDouble(value);
                        } else if ("contamination_hits".equals(key)) {
                            contaminationHits = Math.round(Float.parseFloat(value));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            private void ingestDecision(JsonObject obj) {
                hasRealDecisions = true;
                targetType = str(obj, "target_type");
                String action = str(obj, "action");
                String kind = str(obj, "kind");
                boolean started = bool(obj, "action_started");
                String blockReason = str(obj, "block_reason");
                String fallbackAction = str(obj, "fallback_action");
                double dealt = num(obj, "damage_dealt", 0.0);
                double taken = num(obj, "damage_taken", 0.0);
                recordDecision(action, kind, started, blockReason, fallbackAction, dealt, taken, false);
            }

            private void ingestSample(JsonObject obj) {
                if (hasRealDecisions) {
                    return;
                }
                targetType = str(obj, "target_type");
                int skill = intNum(obj, "sukuna_skill", 0);
                double cntTarget = num(obj, "sukuna_cnt_target", 0.0);
                double targetHealth = num(obj, "target_health", 0.0);
                double sukunaHealth = num(obj, "sukuna_health", 0.0);
                String action = skill == 0 ? (cntTarget <= 6.0 ? "OBSERVED_NO_TARGET_LOCK" : "OBSERVED_IDLE")
                        : ("OBSERVED_SKILL_" + skill);
                String kind = skill == 0 ? "OBSERVED_IDLE" : "OBSERVED_EXECUTION";
                double dealt = 0.0;
                double taken = 0.0;
                if (previousSample != null) {
                    dealt = Math.max(0.0, previousSample.targetHealth - targetHealth);
                    taken = Math.max(0.0, previousSample.sukunaHealth - sukunaHealth);
                }
                recordDecision(action, kind, true, "synthesized_from_benchmark_sample", "", dealt, taken, true);
                previousSample = new SampleState(targetHealth, sukunaHealth);
            }

            private void ingestGate(JsonObject obj) {
                String stage = str(obj, "stage");
                String reason = str(obj, "reason");
                String key = (stage.isEmpty() ? "unknown_stage" : stage) + ":" + (reason.isEmpty() ? "unknown_reason" : reason);
                gateReasons.put(key, gateReasons.getOrDefault(key, 0) + 1);
            }

            private void recordDecision(String action, String kind, boolean started, String blockReason, String fallbackAction, double dealt, double taken, boolean synthetic) {
                if (action == null || action.isEmpty()) {
                    action = "unknown";
                }
                decisions++;
                if (synthetic) {
                    syntheticDecisionCount++;
                }
                topActions.put(action, topActions.getOrDefault(action, 0) + 1);
                if (!synthetic && (!started || (!blockReason.isEmpty() && !"ready".equals(blockReason)))) {
                    blockedActions++;
                    if (!blockReason.isEmpty()) {
                        blockReasons.put(blockReason, blockReasons.getOrDefault(blockReason, 0) + 1);
                    }
                }
                if (!fallbackAction.isEmpty()) {
                    fallbacks.put(fallbackAction, fallbacks.getOrDefault(fallbackAction, 0) + 1);
                }
                if ("RANGE_CONTROL".equals(kind)) {
                    rangeActions++;
                }
                if (dealt <= 0.0 && taken <= 0.0) {
                    noImpactDecisions++;
                }
                if (firstOffenseDecisionIndex < 0 && isOffensive(action, kind)) {
                    firstOffenseDecisionIndex = decisions - 1;
                }
            }

            private void finalizeSynthesizedDecisions() {
                previousSample = null;
            }

            private void normalizeLabOutcome() {
                if ("timeout".equals(outcome)) {
                    int minExpectedDecisions = Math.max(40, ticks / 80);
                    if (decisions < minExpectedDecisions) {
                        outcome = "lab_invalid";
                        labInvalidEvents++;
                        labInvalidReason = "decision_cadence_too_low";
                    }
                }
            }

            private boolean isBadWindow() {
                if (!"target_dead".equals(outcome)) {
                    return true;
                }
                if (decisions <= 0) {
                    return true;
                }
                double den = Math.max(1.0, decisions);
                return blockedActions / den > 0.12 || noImpactDecisions / den > 0.55 || firstOffenseDecisionIndex < 0;
            }

            private String badWindowJson() {
                StringBuilder out = new StringBuilder(256);
                out.append("{\"run_index\":").append(runIndex)
                        .append(",\"variant\":\"").append(escapeJson(variant)).append("\"")
                        .append(",\"outcome\":\"").append(escapeJson(outcome)).append("\"")
                        .append(",\"symptoms\":[");
                List<String> symptoms = new ArrayList<>();
                if (!"target_dead".equals(outcome)) {
                    symptoms.add("outcome=" + outcome);
                }
                if (decisions <= 0) {
                    symptoms.add("missing_decision_telemetry");
                }
                if (!hasRealDecisions) {
                    symptoms.add("telemetry_incomplete");
                    if (!gateReasons.isEmpty()) {
                        String dominantGate = gateReasons.entrySet().stream()
                                .max(Map.Entry.comparingByValue())
                                .map(Map.Entry::getKey)
                                .orElse("unknown");
                        symptoms.add("ai_gate:" + dominantGate);
                    }
                }
                if (damageResolved <= 0.0) {
                    symptoms.add("zero_resolved_damage");
                }
                if (contaminationEvents > 0 || contaminationHits > 0) {
                    symptoms.add("arena_contamination");
                }
                if (labInvalidEvents > 0 || "lab_invalid".equals(outcome)) {
                    symptoms.add("lab_invalid:" + (labInvalidReason.isEmpty() ? "unknown_lab_failure" : labInvalidReason));
                }
                double den = Math.max(1.0, decisions);
                if (blockedActions / den > 0.12) {
                    symptoms.add("blocked_actions_high");
                }
                if (noImpactDecisions / den > 0.55) {
                    symptoms.add("no_impact_loop");
                }
                if (firstOffenseDecisionIndex < 0) {
                    symptoms.add("slow_first_offense");
                }
                for (int i = 0; i < symptoms.size(); i++) {
                    if (i > 0) {
                        out.append(',');
                    }
                    out.append('"').append(escapeJson(symptoms.get(i))).append('"');
                }
                out.append("]}");
                return out.toString();
            }

            private String toJson() {
                List<Map.Entry<String, Integer>> actionTop = topActions.entrySet().stream()
                        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                        .limit(8)
                        .toList();
                StringBuilder actions = new StringBuilder(96);
                actions.append('[');
                for (int i = 0; i < actionTop.size(); i++) {
                    if (i > 0) {
                        actions.append(',');
                    }
                    actions.append("[\"").append(escapeJson(actionTop.get(i).getKey())).append("\",").append(actionTop.get(i).getValue()).append(']');
                }
                actions.append(']');

                return "{\"run_index\":" + runIndex
                        + ",\"arena_index\":" + arenaIndex
                        + ",\"variant\":\"" + escapeJson(variant) + "\""
                        + ",\"outcome\":\"" + escapeJson(outcome) + "\""
                        + ",\"ticks\":" + ticks
                        + ",\"final_sukuna_health\":" + String.format(Locale.ROOT, "%.3f", finalSukunaHealth)
                        + ",\"final_target_health\":" + String.format(Locale.ROOT, "%.3f", finalTargetHealth)
                        + ",\"decisions\":" + decisions
                        + ",\"blocked_actions\":" + blockedActions
                        + ",\"range_actions\":" + rangeActions
                        + ",\"no_impact_decisions\":" + noImpactDecisions
                        + ",\"first_offense_decision_index\":" + firstOffenseDecisionIndex
                        + ",\"resolved_damage_dealt\":" + String.format(Locale.ROOT, "%.3f", damageResolved)
                        + ",\"resolved_damage_taken\":" + String.format(Locale.ROOT, "%.3f", damageTakenResolved)
                        + ",\"target_death_events\":" + targetDeathEvents
                        + ",\"contamination_events\":" + contaminationEvents
                        + ",\"contamination_hits\":" + contaminationHits
                        + ",\"lab_invalid_events\":" + labInvalidEvents
                        + ",\"lab_invalid_reason\":\"" + escapeJson(labInvalidReason) + "\""
                        + ",\"synthetic_decision_count\":" + syntheticDecisionCount
                        + ",\"synthetic_decisions\":" + (!hasRealDecisions)
                        + ",\"gate_reasons\":" + jsonCounterArray(gateReasons)
                        + ",\"top_actions\":" + actions
                        + "}";
            }
        }

        private static boolean isOffensive(String action, String kind) {
            if ("MELEE".equals(kind) || "NORMAL_SLASH".equals(kind) || "WORLD_CUT".equals(kind)
                    || "DOMAIN".equals(kind) || "TEN_SHADOWS".equals(kind) || "TRANSFORM".equals(kind)) {
                return true;
            }
            if (action == null || action.isEmpty()) {
                return false;
            }
            if ("OBSERVED_IDLE".equals(action) || "OBSERVED_NO_TARGET_LOCK".equals(action)) {
                return false;
            }
            return action.startsWith("OBSERVED_SKILL_");
        }

        private record SampleState(double targetHealth, double sukunaHealth) {
        }

        private static String escapeJson(String value) {
            return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
        }

        private String status() {
            return "benchmark_id=" + benchmarkId
                    + ", scenario=" + SCENARIO_ITADORI_MODULO
                    + ", started=" + started + "/" + totalFights
                    + ", finished=" + finished
                    + ", wins=" + wins
                    + ", losses=" + losses
                    + ", timeouts=" + timeouts
                    + ", errors=" + errors
                    + ", duration_ticks=" + durationTicks
                    + ", parallel_arenas=" + parallelArenas
                    + ", requester=" + requester;
        }

        private String variantFor(int runIndex) {
            if (!"standard".equalsIgnoreCase(variantSet)) {
                return variantSet;
            }
            return switch (runIndex % 4) {
                case 1 -> "open_close";
                case 2 -> "open_mid";
                case 3 -> "open_far";
                default -> "open_offset";
            };
        }

        private double startDistance(String variant) {
            return switch (variant) {
                case "open_close" -> 5.0;
                case "open_far" -> 18.0;
                case "open_offset" -> 10.0;
                default -> 10.0;
            };
        }

        private enum ArenaState {
            IDLE,
            PREPARING_CHUNKS,
            BUILDING_ARENA,
            PURGING_ARENA,
            WARMUP,
            RUNNING,
            CLEANUP,
            COOLDOWN,
            QUARANTINED
        }

        private final class Arena {
            private final int index;
            private final BlockPos center;
            private UUID sukunaId;
            private UUID targetId;
            private int runIndex;
            private int age;
            private int phaseAge;
            private int chunkForceCursor;
            private int chunksReadyStableTicks;
            private int buildX;
            private int lastContaminationReportAge = -9999;
            private int contaminationHits;
            private int targetLockLostTicks;
            private int sukunaTickStallTicks;
            private int targetTickStallTicks;
            private int lastObservedSukunaTick = -1;
            private int lastObservedTargetTick = -1;
            private ArenaState state = ArenaState.IDLE;
            private String variant = "";
            private String labInvalidExtra = "";
            private String lastPurgeSample = "";

            private Arena(int index, BlockPos center) {
                this.index = index;
                this.center = center;
            }

            private boolean canAcceptRun() {
                return state == ArenaState.IDLE;
            }

            private void schedule(int runIndex) {
                cleanup();
                this.runIndex = runIndex;
                this.age = 0;
                this.phaseAge = 0;
                this.chunkForceCursor = 0;
                this.chunksReadyStableTicks = 0;
                this.buildX = -ARENA_BUILD_RADIUS;
                this.contaminationHits = 0;
                this.targetLockLostTicks = 0;
                this.sukunaTickStallTicks = 0;
                this.targetTickStallTicks = 0;
                this.lastObservedSukunaTick = -1;
                this.lastObservedTargetTick = -1;
                this.lastContaminationReportAge = -9999;
                this.labInvalidExtra = "";
                this.lastPurgeSample = "";
                this.variant = variantFor(runIndex);
                this.state = ArenaState.PREPARING_CHUNKS;
                OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                        "arena_prepare", "preparing_chunks", labDetails("state=preparing_chunks"));
            }

            private void tick() {
                switch (state) {
                    case PREPARING_CHUNKS -> tickPreparingChunks();
                    case BUILDING_ARENA -> tickBuildingArena();
                    case PURGING_ARENA -> tickPurgingArena();
                    case WARMUP -> tickWarmup();
                    case RUNNING -> tickRunning();
                    case CLEANUP -> tickCleanup();
                    case COOLDOWN -> tickCooldown();
                    case IDLE, QUARANTINED -> {
                    }
                }
            }

            private void tickPreparingChunks() {
                phaseAge++;
                if (chunkForceCursor < forcedChunkCount()) {
                    forceNextChunk(true);
                    return;
                }
                if (chunksReady()) {
                    chunksReadyStableTicks++;
                    if (chunksReadyStableTicks >= CHUNK_STABLE_READY_TICKS) {
                        int prepareTicks = phaseAge;
                        phaseAge = 0;
                        state = ArenaState.BUILDING_ARENA;
                        OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                                "arena_healthcheck", "chunks_ready", labDetails("chunks_ready=true,forced_chunk_count=" + forcedChunkCount()
                                        + ",chunk_bounds=" + chunkBoundsString() + ",chunk_prepare_ticks=" + prepareTicks));
                        return;
                    }
                } else {
                    chunksReadyStableTicks = 0;
                }
                if (phaseAge >= ARENA_PREP_TIMEOUT_TICKS) {
                    labInvalid("chunks_not_ready_before_spawn", "forced_chunk_count=" + forcedChunkCount()
                            + ",chunk_bounds=" + chunkBoundsString() + ",chunks_missing_sample=" + missingChunksSample());
                }
            }

            private void tickBuildingArena() {
                if (phaseAge == 0) {
                    cleanup();
                }
                phaseAge++;
                if (!buildArenaStep()) {
                    return;
                }
                phaseAge = 0;
                state = ArenaState.PURGING_ARENA;
                OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                        "arena_healthcheck", "arena_built", labDetails("build_rows=45"));
            }

            private void tickPurgingArena() {
                phaseAge++;
                cleanup();
                int purged = purgeArenaBeforeSpawn("pre_spawn_purge_tick_" + phaseAge);
                if (phaseAge == 1 || purged > 0 || phaseAge >= ARENA_PRESPAWN_PURGE_TICKS) {
                    OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                            "arena_purge", "pre_spawn", labDetails("purged=" + purged + ",purge_tick=" + phaseAge));
                }
                if (phaseAge < ARENA_PRESPAWN_PURGE_TICKS) {
                    return;
                }
                phaseAge = 0;
                spawnForWarmup();
            }

            private void spawnForWarmup() {
                double distance = startDistance(variant);
                Vec3 base = Vec3.atBottomCenterOf(center);
                Vec3 sukunaPos = base.add(-distance * 0.5, 1.0, "open_offset".equals(variant) ? -3.0 : 0.0);
                Vec3 targetPos = base.add(distance * 0.5, 1.0, "open_offset".equals(variant) ? 3.0 : 0.0);
                LivingEntity sukuna = spawnLiving(sukunaType, sukunaPos);
                LivingEntity target = spawnLiving(targetType, targetPos);
                if (sukuna == null || target == null) {
                    if (sukuna != null) sukuna.discard();
                    if (target != null) target.discard();
                    labInvalid("entity_spawn_failed");
                    return;
                }
                sukunaId = sukuna.getUUID();
                targetId = target.getUUID();
                String fightId = sukuna.getStringUUID() + ":" + target.getStringUUID();
                markBenchmarkEntity(sukuna, fightId);
                markBenchmarkEntity(target, fightId);
                OpSukunaBrainTelemetry.registerBenchmarkFight(fightId, sukuna, target, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index);
                holdWarmupState(sukuna, target);
                if (!validateSpawnedPair(sukuna, target)) {
                    labInvalid("entity_spawn_unstable", "sukuna=" + entityStatus(sukuna) + ",target=" + entityStatus(target)
                            + ",own_tagged_entities=" + countOwnTaggedEntities() + ",purged_sample=" + lastPurgeSample);
                    return;
                }
                phaseAge = 0;
                state = ArenaState.WARMUP;
                OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                        "spawn_validation", "spawned", labDetails("fight_id=" + fightId + ",distance=" + String.format(Locale.ROOT, "%.1f", distance)
                                + ",sukuna=" + entityStatus(sukuna) + ",target=" + entityStatus(target)));
                OpSukunaBrainTelemetry.recordBenchmarkSample(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index, 0, sukuna, target, "spawn");
            }

            private void tickWarmup() {
                phaseAge++;
                LivingEntity sukuna = entity(sukunaId);
                LivingEntity target = entity(targetId);
                if (sukuna == null || target == null) {
                    labInvalid("entity_missing_during_warmup", entityMissingDetails("warmup"));
                    return;
                }
                if (!sukuna.isAlive() || !target.isAlive()) {
                    labInvalid("entity_died_during_warmup", warmupDamageDetails(sukuna, target));
                    return;
                }
                if (sukuna.getHealth() < sukuna.getMaxHealth() - 0.01F || target.getHealth() < target.getMaxHealth() - 0.01F) {
                    labInvalid("warmup_damage", warmupDamageDetails(sukuna, target));
                    return;
                }
                if (!chunksReady()) {
                    labInvalid("chunk_unloaded_during_warmup", entityMissingDetails("warmup_chunk_check"));
                    return;
                }
                holdWarmupState(sukuna, target);
                keepInArena(sukuna, -5.0);
                keepInArena(target, 5.0);
                if (phaseAge >= ARENA_WARMUP_TICKS) {
                    age = 0;
                    state = ArenaState.RUNNING;
                    initializeCombatState(sukuna, target);
                    wireTargets(sukuna, target);
                    OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                            "benchmark_fight_start", "running", labDetails("fight_id=" + sukuna.getStringUUID() + ":" + target.getStringUUID()));
                    OpSukunaBrainTelemetry.recordBenchmarkSample(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index, age, sukuna, target, "start");
                }
            }

            private void tickRunning() {
                age++;
                LivingEntity sukuna = entity(sukunaId);
                LivingEntity target = entity(targetId);
                if (sukuna == null || target == null) {
                    labInvalid("entity_missing_without_death_event", entityMissingDetails("running"));
                    return;
                }
                boolean sukunaDead = !sukuna.isAlive();
                boolean targetDead = !target.isAlive();
                if (sukunaDead || targetDead) {
                    if (age <= INSTANT_INVALID_TICKS && sukunaDead && targetDead) {
                        labInvalid("instant_both_dead");
                        return;
                    }
                    if (targetDead && !sukunaDead) {
                        complete("target_dead");
                    } else if (sukunaDead && !targetDead) {
                        complete("sukuna_dead");
                    } else {
                        complete("both_dead");
                    }
                    return;
                }
                if (!chunksReady()) {
                    labInvalid("chunk_unloaded_during_running", "chunks_missing_sample=" + missingChunksSample());
                    return;
                }
                if (detectTickProgressStalled(sukuna, target)) {
                    return;
                }
                if (detectAiHookLost(sukuna, target)) {
                    return;
                }
                if (detectTargetLockLost(sukuna, target)) {
                    return;
                }
                maintainCombatState(sukuna, target);
                wireTargets(sukuna, target);
                keepInArena(sukuna, -5.0);
                keepInArena(target, 5.0);
                if (detectArenaContamination(sukuna, target)) {
                    labInvalid("arena_contamination");
                    return;
                }
                if (age % sampleIntervalTicks == 0) {
                    OpSukunaBrainTelemetry.recordBenchmarkSample(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index, age, sukuna, target, "tick");
                }
                if (age >= durationTicks) {
                    complete("timeout");
                }
            }

            private void tickCleanup() {
                phaseAge++;
                cleanup();
                if (countOwnTaggedEntities() == 0) {
                    OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                            "post_cleanup_snapshot", "clean", labDetails("remaining_entities=0"));
                    forceChunks(false);
                    sukunaId = null;
                    targetId = null;
                    state = ArenaState.COOLDOWN;
                    phaseAge = 0;
                    return;
                }
                if (phaseAge >= ARENA_COOLDOWN_TICKS * 2) {
                    state = ArenaState.QUARANTINED;
                    OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                            "arena_quarantined", "cleanup_failed", labDetails("remaining_entities=" + countOwnTaggedEntities()));
                    forceChunks(false);
                }
            }

            private void tickCooldown() {
                phaseAge++;
                if (phaseAge >= ARENA_COOLDOWN_TICKS) {
                    state = ArenaState.IDLE;
                    variant = "";
                    runIndex = 0;
                }
            }

            private void complete(String outcome) {
                finishFight(outcome, "");
            }

            private void labInvalid(String reason) {
                labInvalid(reason, "");
            }

            private void labInvalid(String reason, String extra) {
                labInvalidExtra = extra == null ? "" : extra;
                finishFight("lab_invalid", reason == null || reason.isEmpty() ? "unknown_lab_failure" : reason);
            }

            private void finishFight(String outcome, String labReason) {
                LivingEntity sukuna = entity(sukunaId);
                LivingEntity target = entity(targetId);
                if (sukuna != null && target != null) {
                    OpSukunaBrainTelemetry.recordBenchmarkSample(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index, age, sukuna, target, "end");
                }
                OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                        "pre_finish_snapshot", outcome, labDetails("sukuna=" + entityStatus(sukuna) + ",target=" + entityStatus(target)
                                + (labInvalidExtra.isEmpty() ? "" : "," + labInvalidExtra)));
                finished++;
                if ("lab_invalid".equals(outcome)) {
                    invalidLab++;
                    OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                            "lab_invalid", labReason, labDetails("reason=" + labReason
                                    + (labInvalidExtra.isEmpty() ? "" : "," + labInvalidExtra)));
                } else if ("target_dead".equals(outcome)) {
                    wins++;
                } else if ("timeout".equals(outcome)) {
                    timeouts++;
                } else if ("runner_error".equals(outcome)) {
                    errors++;
                } else {
                    losses++;
                }
                results.add(new FightResult(runIndex, index, variant, outcome, age,
                        sukuna == null ? 0.0F : sukuna.getHealth(),
                        target == null ? 0.0F : target.getHealth()));
                OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                        "benchmark_fight_end", outcome, "ticks=" + age
                                + ",sukuna_health=" + (sukuna == null ? 0.0F : sukuna.getHealth())
                                + ",target_health=" + (target == null ? 0.0F : target.getHealth())
                                + ",contamination_hits=" + contaminationHits
                                + (labReason.isEmpty() ? "" : ",lab_reason=" + labReason));
                state = ArenaState.CLEANUP;
                phaseAge = 0;
                labInvalidExtra = "";
                cleanup();
            }

            private LivingEntity spawnLiving(EntityType<?> type, Vec3 pos) {
                Entity entity = type.create(level);
                if (!(entity instanceof LivingEntity living)) {
                    return null;
                }
                living.moveTo(pos.x, pos.y, pos.z, 0.0f, 0.0f);
                living.addTag(TEST_TAG);
                living.getPersistentData().putString("JJKU_BENCHMARK_ID", benchmarkId);
                living.getPersistentData().putInt("JJKU_BENCHMARK_RUN", runIndex);
                living.getPersistentData().putInt("JJKU_BENCHMARK_ARENA", index);
                living.setHealth(living.getMaxHealth());
                if (living instanceof Mob mob) {
                    mob.setPersistenceRequired();
                }
                if (!level.addFreshEntity(living)) {
                    OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                            "spawn_validation", "spawn_cancelled_or_not_added",
                            labDetails("type=" + type + ",rules=op_sukuna/metrics/on,mob_spawning/off,spawn_changer/off"));
                    return null;
                }
                if (level.getEntity(living.getUUID()) == null) {
                    OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                            "spawn_validation", "spawn_missing_after_add",
                            labDetails("type=" + type + ",uuid=" + living.getStringUUID()));
                    living.discard();
                    return null;
                }
                return living;
            }

            private void markBenchmarkEntity(LivingEntity living, String fightId) {
                living.getPersistentData().putString("JJKU_BENCHMARK_ID", benchmarkId);
                living.getPersistentData().putString("JJKU_BENCHMARK_SCENARIO", SCENARIO_ITADORI_MODULO);
                living.getPersistentData().putString("JJKU_BENCHMARK_VARIANT", variant);
                living.getPersistentData().putString("JJKU_BENCHMARK_FIGHT_ID", fightId);
                living.getPersistentData().putInt("JJKU_BENCHMARK_RUN", runIndex);
                living.getPersistentData().putInt("JJKU_BENCHMARK_ARENA", index);
            }

            private void initializeCombatState(LivingEntity sukuna, LivingEntity target) {
                if (sukuna instanceof Mob mob) {
                    mob.setNoAi(false);
                }
                if (target instanceof Mob mob) {
                    mob.setNoAi(false);
                }
                CompoundTag sNbt = sukuna.getPersistentData();
                CompoundTag tNbt = target.getPersistentData();
                sNbt.putDouble("cnt_target", 7.0);
                tNbt.putDouble("cnt_target", 7.0);
                sNbt.putDouble("skill", 0.0);
                tNbt.putDouble("skill", 0.0);
                sNbt.putDouble("COOLDOWN_TICKS", 0.0);
                tNbt.putDouble("COOLDOWN_TICKS", 0.0);
                sNbt.putBoolean("JJKUR_OP_AI_CONTROLLED", true);
            }

            private void maintainCombatState(LivingEntity sukuna, LivingEntity target) {
                if (sukuna instanceof Mob mob) {
                    mob.setNoAi(false);
                }
                if (target instanceof Mob mob) {
                    mob.setNoAi(false);
                }
                CompoundTag sNbt = sukuna.getPersistentData();
                CompoundTag tNbt = target.getPersistentData();
                if (sNbt.getDouble("cnt_target") <= 6.0) {
                    sNbt.putDouble("cnt_target", 7.0);
                }
                if (tNbt.getDouble("cnt_target") <= 6.0) {
                    tNbt.putDouble("cnt_target", 7.0);
                }
                sNbt.putBoolean("JJKUR_OP_AI_CONTROLLED", true);
            }

            private void holdWarmupState(LivingEntity sukuna, LivingEntity target) {
                clearWarmupCombat(sukuna);
                clearWarmupCombat(target);
                sukuna.setHealth(sukuna.getMaxHealth());
                target.setHealth(target.getMaxHealth());
                sukuna.fallDistance = 0.0F;
                target.fallDistance = 0.0F;
            }

            private void clearWarmupCombat(LivingEntity living) {
                CompoundTag nbt = living.getPersistentData();
                nbt.putDouble("cnt_target", 0.0);
                nbt.putDouble("skill", 0.0);
                nbt.putDouble("Damage", 0.0);
                nbt.putBoolean("attack", false);
                if (living instanceof Mob mob) {
                    mob.setTarget(null);
                    mob.setNoAi(true);
                }
                living.setLastHurtByMob(null);
            }

            private String warmupDamageDetails(LivingEntity sukuna, LivingEntity target) {
                return "sukuna=" + entityStatus(sukuna)
                        + ",target=" + entityStatus(target)
                        + ",sukuna_health_delta=" + String.format(Locale.ROOT, "%.2f", sukuna.getMaxHealth() - Math.max(0.0F, sukuna.getHealth()))
                        + ",target_health_delta=" + String.format(Locale.ROOT, "%.2f", target.getMaxHealth() - Math.max(0.0F, target.getHealth()))
                        + ",sukuna_skill=" + sukuna.getPersistentData().getDouble("skill")
                        + ",target_skill=" + target.getPersistentData().getDouble("skill")
                        + ",sukuna_target=" + (sukuna instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getStringUUID() : "")
                        + ",target_target=" + (target instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getStringUUID() : "");
            }

            private void wireTargets(LivingEntity sukuna, LivingEntity target) {
                if (sukuna instanceof Mob mob) {
                    mob.setTarget(target);
                }
                if (target instanceof Mob mob) {
                    mob.setTarget(sukuna);
                }
                sukuna.setLastHurtByMob(target);
                target.setLastHurtByMob(sukuna);
            }

            private boolean validateSpawnedPair(LivingEntity sukuna, LivingEntity target) {
                if (sukuna == null || target == null || !sukuna.isAlive() || !target.isAlive()) {
                    return false;
                }
                return !sukuna.isRemoved() && !target.isRemoved();
            }

            private void keepInArena(LivingEntity entity, double xOffset) {
                if (entity.distanceToSqr(Vec3.atCenterOf(center)) > ARENA_MOVEMENT_RADIUS * ARENA_MOVEMENT_RADIUS) {
                    entity.teleportTo(center.getX() + 0.5 + xOffset, center.getY() + 1.0, center.getZ() + 0.5);
                    entity.fallDistance = 0.0f;
                }
            }

            private boolean detectAiHookLost(LivingEntity sukuna, LivingEntity target) {
                if (age <= AI_HOOK_LOST_GRACE_TICKS) {
                    return false;
                }
                long lastHookGameTime = sukuna.getPersistentData().getLong("JJKUR_OP_AI_LAST_HOOK_GAME_TIME");
                long currentGameTime = level.getGameTime();
                if (lastHookGameTime <= 0L || currentGameTime - lastHookGameTime > AI_HOOK_LOST_MAX_TICKS) {
                    labInvalid("ai_hook_lost", "last_hook_game_time=" + lastHookGameTime
                            + ",current_game_time=" + currentGameTime
                            + ",sukuna_tick=" + sukuna.tickCount
                            + ",target_tick=" + target.tickCount
                            + ",sukuna=" + entityStatus(sukuna)
                            + ",target=" + entityStatus(target));
                    return true;
                }
                return false;
            }

            private boolean detectTargetLockLost(LivingEntity sukuna, LivingEntity target) {
                boolean sukunaWrong = sukuna instanceof Mob mob && mob.getTarget() != target;
                boolean targetWrong = target instanceof Mob mob && mob.getTarget() != sukuna;
                if (sukunaWrong || targetWrong) {
                    targetLockLostTicks++;
                    if (targetLockLostTicks == 1 || targetLockLostTicks % 20 == 0) {
                        OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                                "target_lock_lost", "repairing", labDetails("target_lock_lost_ticks=" + targetLockLostTicks
                                        + ",sukuna_target=" + (sukuna instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getStringUUID() : "")
                                        + ",target_target=" + (target instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getStringUUID() : "")));
                    }
                    if (targetLockLostTicks > TARGET_LOCK_LOST_MAX_TICKS) {
                        labInvalid("target_lock_unstable", "target_lock_lost_ticks=" + targetLockLostTicks
                                + ",sukuna_target=" + (sukuna instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getStringUUID() : "")
                                + ",target_target=" + (target instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getStringUUID() : ""));
                        return true;
                    }
                } else {
                    targetLockLostTicks = 0;
                }
                return false;
            }

            private boolean detectTickProgressStalled(LivingEntity sukuna, LivingEntity target) {
                if (lastObservedSukunaTick >= 0 && sukuna.tickCount <= lastObservedSukunaTick) {
                    sukunaTickStallTicks++;
                } else {
                    sukunaTickStallTicks = 0;
                }
                if (lastObservedTargetTick >= 0 && target.tickCount <= lastObservedTargetTick) {
                    targetTickStallTicks++;
                } else {
                    targetTickStallTicks = 0;
                }
                lastObservedSukunaTick = sukuna.tickCount;
                lastObservedTargetTick = target.tickCount;
                if (age > AI_HOOK_LOST_GRACE_TICKS
                        && (sukunaTickStallTicks > TICK_STALL_MAX_TICKS || targetTickStallTicks > TICK_STALL_MAX_TICKS)) {
                    labInvalid("tick_progress_stalled", "sukuna_tick_stall=" + sukunaTickStallTicks
                            + ",target_tick_stall=" + targetTickStallTicks
                            + ",sukuna_tick=" + sukuna.tickCount
                            + ",target_tick=" + target.tickCount);
                    return true;
                }
                return false;
            }

            private boolean detectArenaContamination(LivingEntity sukuna, LivingEntity target) {
                if (age - lastContaminationReportAge < 40) {
                    return false;
                }
                double domainRadius = JujutsucraftModVariables.MapVariables.get(level).DomainExpansionRadius * 18.0;
                double scanRadius = Math.max(80.0, Math.min(Math.max(domainRadius, effectiveArenaSpacing * 0.65), 1024.0));
                AABB box = new AABB(center).inflate(scanRadius);
                int foreign = 0;
                int staleTagged = 0;
                double nearest = Double.MAX_VALUE;
                int nearestArena = -1;
                int nearestRun = -1;
                StringBuilder sample = new StringBuilder(256);
                for (Entity entity : level.getEntities((Entity) null, box, entity -> entity.getTags().contains(TEST_TAG))) {
                    if (!(entity instanceof LivingEntity living) || living == sukuna || living == target) {
                        continue;
                    }
                    CompoundTag data = living.getPersistentData();
                    String otherBenchmark = data.getString("JJKU_BENCHMARK_ID");
                    if (!benchmarkId.equals(otherBenchmark)) {
                        staleTagged++;
                        continue;
                    }
                    int otherArena = data.getInt("JJKU_BENCHMARK_ARENA");
                    if (otherArena == index) {
                        continue;
                    }
                    foreign++;
                    double dist = Math.min(living.distanceTo(sukuna), living.distanceTo(target));
                    if (dist < nearest) {
                        nearest = dist;
                        nearestArena = otherArena;
                        nearestRun = data.getInt("JJKU_BENCHMARK_RUN");
                    }
                    if (sample.length() < 220) {
                        if (sample.length() > 0) {
                            sample.append('|');
                        }
                        sample.append(entityDebug(living, sukuna, target));
                    }
                }
                if (foreign > 0) {
                    contaminationHits++;
                    lastContaminationReportAge = age;
                    OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                            "arena_contamination", "foreign_benchmark_entities",
                            "foreign=" + foreign
                                    + ",nearest_distance=" + String.format(Locale.ROOT, "%.2f", nearest)
                                    + ",nearest_arena=" + nearestArena
                                    + ",nearest_run=" + nearestRun
                                    + ",scan_radius=" + String.format(Locale.ROOT, "%.1f", scanRadius)
                                    + ",arena_spacing=" + effectiveArenaSpacing
                                    + ",stale_tagged_ignored=" + staleTagged
                                    + ",foreign_sample=" + sample);
                    return true;
                }
                return false;
            }

            private LivingEntity entity(UUID id) {
                Entity entity = id == null ? null : level.getEntity(id);
                return entity instanceof LivingEntity living ? living : null;
            }

            private void cleanup() {
                AABB box = new AABB(center).inflate(Math.max(48.0, effectiveArenaSpacing * 0.45));
                for (Entity entity : level.getEntities((Entity) null, box, entity -> {
                    if (!entity.getTags().contains(TEST_TAG)) {
                        return false;
                    }
                    CompoundTag data = entity.getPersistentData();
                    return benchmarkId.equals(data.getString("JJKU_BENCHMARK_ID")) && data.getInt("JJKU_BENCHMARK_ARENA") == index;
                })) {
                    entity.discard();
                }
            }

            private int countOwnTaggedEntities() {
                AABB box = new AABB(center).inflate(Math.max(48.0, effectiveArenaSpacing * 0.45));
                int count = 0;
                for (Entity entity : level.getEntities((Entity) null, box, entity -> {
                    if (!entity.getTags().contains(TEST_TAG)) {
                        return false;
                    }
                    CompoundTag data = entity.getPersistentData();
                    return benchmarkId.equals(data.getString("JJKU_BENCHMARK_ID")) && data.getInt("JJKU_BENCHMARK_ARENA") == index;
                })) {
                    count++;
                }
                return count;
            }

            private int purgeArenaBeforeSpawn(String reason) {
                int purged = 0;
                StringBuilder sample = new StringBuilder(256);
                AABB box = new AABB(center).inflate(48.0);
                for (Entity entity : level.getEntities((Entity) null, box, entity -> !(entity instanceof Player))) {
                    if (entity.isRemoved()) {
                        continue;
                    }
                    if (sample.length() < 220) {
                        if (sample.length() > 0) {
                            sample.append('|');
                        }
                        sample.append(entityDebug(entity, null, null));
                    }
                    entity.discard();
                    purged++;
                }
                lastPurgeSample = sample.toString();
                if (purged > 0) {
                    OpSukunaBrainTelemetry.recordBenchmarkEvent(level, benchmarkId, SCENARIO_ITADORI_MODULO, variant, runIndex, index,
                            "arena_purge_detail", reason, labDetails("purged=" + purged + ",purged_sample=" + lastPurgeSample));
                }
                return purged;
            }

            private boolean buildArenaStep() {
                int radius = ARENA_BUILD_RADIUS;
                int rows = 0;
                for (; buildX <= radius && rows < ARENA_BUILD_ROWS_PER_TICK; buildX++, rows++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        BlockPos floor = center.offset(buildX, -1, dz);
                        level.setBlock(floor, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                        for (int dy = 0; dy <= 6; dy++) {
                            level.setBlock(center.offset(buildX, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
                if (buildX > radius) {
                    return true;
                }
                return false;
            }

            private void forceChunks(boolean force) {
                for (int chunkX = minChunkX(); chunkX <= maxChunkX(); chunkX++) {
                    for (int chunkZ = minChunkZ(); chunkZ <= maxChunkZ(); chunkZ++) {
                        level.setChunkForced(chunkX, chunkZ, force);
                    }
                }
            }

            private void forceNextChunk(boolean force) {
                int local = chunkForceCursor++;
                int width = maxChunkX() - minChunkX() + 1;
                int chunkX = minChunkX() + local % width;
                int chunkZ = minChunkZ() + local / width;
                level.setChunkForced(chunkX, chunkZ, force);
            }

            private boolean chunksReady() {
                for (int chunkX = minChunkX(); chunkX <= maxChunkX(); chunkX++) {
                    for (int chunkZ = minChunkZ(); chunkZ <= maxChunkZ(); chunkZ++) {
                        BlockPos probe = new BlockPos(chunkX * 16 + 8, center.getY(), chunkZ * 16 + 8);
                        if (!level.hasChunkAt(probe)) {
                            return false;
                        }
                    }
                }
                return true;
            }

            private int forcedChunkRadiusBlocks() {
                return Math.max(ARENA_BUILD_RADIUS, ARENA_MOVEMENT_RADIUS) + ARENA_CHUNK_MARGIN_BLOCKS;
            }

            private int minChunkX() {
                return Math.floorDiv(center.getX() - forcedChunkRadiusBlocks(), 16);
            }

            private int maxChunkX() {
                return Math.floorDiv(center.getX() + forcedChunkRadiusBlocks(), 16);
            }

            private int minChunkZ() {
                return Math.floorDiv(center.getZ() - forcedChunkRadiusBlocks(), 16);
            }

            private int maxChunkZ() {
                return Math.floorDiv(center.getZ() + forcedChunkRadiusBlocks(), 16);
            }

            private int forcedChunkCount() {
                return (maxChunkX() - minChunkX() + 1) * (maxChunkZ() - minChunkZ() + 1);
            }

            private String chunkBoundsString() {
                return minChunkX() + "/" + minChunkZ() + ".." + maxChunkX() + "/" + maxChunkZ();
            }

            private String missingChunksSample() {
                StringBuilder sample = new StringBuilder(160);
                int count = 0;
                for (int chunkX = minChunkX(); chunkX <= maxChunkX(); chunkX++) {
                    for (int chunkZ = minChunkZ(); chunkZ <= maxChunkZ(); chunkZ++) {
                        BlockPos probe = new BlockPos(chunkX * 16 + 8, center.getY(), chunkZ * 16 + 8);
                        if (!level.hasChunkAt(probe)) {
                            count++;
                            if (sample.length() < 120) {
                                if (sample.length() > 0) {
                                    sample.append('|');
                                }
                                sample.append(chunkX).append('/').append(chunkZ);
                            }
                        }
                    }
                }
                return "count=" + count + ",sample=" + sample;
            }

            private String labDetails(String extra) {
                return "state=" + state
                        + ",phase_age=" + phaseAge
                        + ",age=" + age
                        + ",center=" + center.getX() + "/" + center.getY() + "/" + center.getZ()
                        + ",chunks_ready=" + chunksReady()
                        + ",contamination_hits=" + contaminationHits
                        + ",sukuna_uuid=" + (sukunaId == null ? "" : sukunaId)
                        + ",target_uuid=" + (targetId == null ? "" : targetId)
                        + ",sukuna_present=" + (sukunaId != null && entity(sukunaId) != null)
                        + ",target_present=" + (targetId != null && entity(targetId) != null)
                        + (extra == null || extra.isEmpty() ? "" : "," + extra);
            }

            private String entityMissingDetails(String phase) {
                LivingEntity sukuna = entity(sukunaId);
                LivingEntity target = entity(targetId);
                return "missing_phase=" + phase
                        + ",expected_sukuna_uuid=" + (sukunaId == null ? "" : sukunaId)
                        + ",expected_target_uuid=" + (targetId == null ? "" : targetId)
                        + ",sukuna=" + entityStatus(sukuna)
                        + ",target=" + entityStatus(target)
                        + ",own_tagged_remaining=" + countOwnTaggedEntities()
                        + ",last_purge_sample=" + lastPurgeSample;
            }

            private String entityStatus(LivingEntity entity) {
                if (entity == null) {
                    return "null";
                }
                return entityTypeName(entity)
                        + "@alive=" + entity.isAlive()
                        + "/removed=" + entity.isRemoved()
                        + "/health=" + String.format(Locale.ROOT, "%.2f", entity.getHealth())
                        + "/pos=" + String.format(Locale.ROOT, "%.1f:%.1f:%.1f", entity.getX(), entity.getY(), entity.getZ());
            }

            private String entityDebug(Entity entity, LivingEntity sukuna, LivingEntity target) {
                CompoundTag data = entity.getPersistentData();
                double distSukuna = sukuna == null ? -1.0 : entity.distanceTo(sukuna);
                double distTarget = target == null ? -1.0 : entity.distanceTo(target);
                return entityTypeName(entity)
                        + "#uuid=" + entity.getUUID()
                        + "#bench=" + data.getString("JJKU_BENCHMARK_ID")
                        + "#arena=" + data.getInt("JJKU_BENCHMARK_ARENA")
                        + "#run=" + data.getInt("JJKU_BENCHMARK_RUN")
                        + "#pos=" + String.format(Locale.ROOT, "%.1f:%.1f:%.1f", entity.getX(), entity.getY(), entity.getZ())
                        + "#dist_s=" + String.format(Locale.ROOT, "%.1f", distSukuna)
                        + "#dist_t=" + String.format(Locale.ROOT, "%.1f", distTarget);
            }

            private String entityTypeName(Entity entity) {
                ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                return typeId == null ? entity.getType().toString() : typeId.toString();
            }
        }
    }

    private record FightResult(int runIndex, int arenaIndex, String variant, String outcome, int ticks, float sukunaHealth, float targetHealth) {
    }
}
