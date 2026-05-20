package com.jujutsu.jujutsucraftaddon.util;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.procedures.GojoDomainForceProcedure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Path;
import java.util.UUID;

@Mod.EventBusSubscriber
public final class OpSukunaAiTestRunner {
    private static final String TEST_TAG = "jjku_op_sukuna_ai_test";
    private static final int DEFAULT_DURATION_TICKS = 2400;
    private static final int DEFAULT_FORCE_DOMAIN_TICK = 120;
    private static final ResourceLocation SUKUNA_PERFECT = new ResourceLocation("jujutsucraft", "sukuna_perfect");
    private static final ResourceLocation GOJO_SATORU = new ResourceLocation("jujutsucraft", "gojo_satoru");

    private static TestRun active;

    private OpSukunaAiTestRunner() {
    }

    public static int runGojoMuryo(CommandSourceStack source, int durationTicks, int forceDomainAtTick) {
        if (active != null) {
            source.sendFailure(Component.literal("OpSukuna AI test already running: " + active.status()));
            return 0;
        }
        if (!OpSukunaBrainTelemetry.devTelemetryAllowed()) {
            source.sendFailure(Component.literal("OpSukuna AI test requires -Djjku.opSukunaMetrics=true or JJKU_OP_SUKUNA_METRICS_DEV=true."));
            return 0;
        }
        ServerLevel level = source.getLevel();
        EntityType<?> sukunaType = ForgeRegistries.ENTITY_TYPES.getValue(SUKUNA_PERFECT);
        EntityType<?> gojoType = ForgeRegistries.ENTITY_TYPES.getValue(GOJO_SATORU);
        if (sukunaType == null || gojoType == null) {
            source.sendFailure(Component.literal("Missing test entity type: sukuna=" + sukunaType + ", gojo=" + gojoType));
            return 0;
        }

        int safeDuration = Math.max(200, durationTicks);
        int safeForceTick = Math.max(1, Math.min(forceDomainAtTick, safeDuration - 20));
        BlockPos center = BlockPos.containing(source.getPosition());
        active = new TestRun(level, center, safeDuration, safeForceTick, source.getTextName());
        if (!active.prepare(sukunaType, gojoType)) {
            source.sendFailure(Component.literal("Failed to start OpSukuna AI test: " + active.failureReason));
            active = null;
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Started OpSukuna AI test gojo_muryo: duration=" + safeDuration
                + ", force_domain_tick=" + safeForceTick + ", center=" + center.toShortString()), true);
        return 1;
    }

    public static int runGojoMuryo(CommandSourceStack source) {
        return runGojoMuryo(source, DEFAULT_DURATION_TICKS, DEFAULT_FORCE_DOMAIN_TICK);
    }

    public static int status(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(active == null ? "No OpSukuna AI test running." : active.status()), false);
        return active == null ? 0 : 1;
    }

    public static int stop(CommandSourceStack source) {
        if (active == null) {
            source.sendSuccess(() -> Component.literal("No OpSukuna AI test running."), false);
            return 0;
        }
        TestRun run = active;
        active = null;
        run.cleanup();
        source.sendSuccess(() -> Component.literal("Stopped OpSukuna AI test: " + run.status()), true);
        return 1;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active == null) {
            return;
        }
        TestRun run = active;
        run.tick();
        if (run.done) {
            active = null;
        }
    }

    private static final class TestRun {
        private final ServerLevel level;
        private final BlockPos center;
        private final int durationTicks;
        private final int forceDomainAtTick;
        private final String requester;
        private UUID sukunaId;
        private UUID gojoId;
        private int age;
        private boolean domainForced;
        private boolean done;
        private String failureReason = "";

        private TestRun(ServerLevel level, BlockPos center, int durationTicks, int forceDomainAtTick, String requester) {
            this.level = level;
            this.center = center;
            this.durationTicks = durationTicks;
            this.forceDomainAtTick = forceDomainAtTick;
            this.requester = requester == null || requester.isEmpty() ? "server" : requester;
        }

        private boolean prepare(EntityType<?> sukunaType, EntityType<?> gojoType) {
            level.getGameRules().getRule(JujutsucraftaddonModGameRules.JJKU_OP_SUKUNA_METRICS).set(true, level.getServer());
            OpSukunaBrainTelemetry.reset();
            cleanup();
            buildArena();
            LivingEntity sukuna = spawnLiving(sukunaType, Vec3.atBottomCenterOf(center).add(-5.0, 1.0, 0.0));
            LivingEntity gojo = spawnLiving(gojoType, Vec3.atBottomCenterOf(center).add(5.0, 1.0, 0.0));
            if (sukuna == null || gojo == null) {
                if (sukuna != null) sukuna.discard();
                if (gojo != null) gojo.discard();
                failureReason = "entity_spawn_failed";
                return false;
            }
            sukunaId = sukuna.getUUID();
            gojoId = gojo.getUUID();
            wireTargets(sukuna, gojo);
            return true;
        }

        private LivingEntity spawnLiving(EntityType<?> type, Vec3 pos) {
            Entity entity = type.create(level);
            if (!(entity instanceof LivingEntity living)) {
                return null;
            }
            living.moveTo(pos.x, pos.y, pos.z, 0.0f, 0.0f);
            living.addTag(TEST_TAG);
            living.setHealth(living.getMaxHealth());
            if (living instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            level.addFreshEntity(living);
            return living;
        }

        private void tick() {
            age++;
            LivingEntity sukuna = entity(sukunaId);
            LivingEntity gojo = entity(gojoId);
            if (sukuna == null || gojo == null || !sukuna.isAlive() || !gojo.isAlive()) {
                finish(sukuna, gojo, "entity_dead");
                return;
            }
            wireTargets(sukuna, gojo);
            keepInArena(sukuna, -5.0);
            keepInArena(gojo, 5.0);
            if (!domainForced && age >= forceDomainAtTick) {
                GojoDomainForceProcedure.force(gojo);
                domainForced = true;
            }
            if (age >= durationTicks) {
                finish(sukuna, gojo, "duration_elapsed");
            }
        }

        private void wireTargets(LivingEntity sukuna, LivingEntity gojo) {
            if (sukuna instanceof Mob mob) {
                mob.setTarget(gojo);
            }
            if (gojo instanceof Mob mob) {
                mob.setTarget(sukuna);
            }
            sukuna.setLastHurtByMob(gojo);
            gojo.setLastHurtByMob(sukuna);
        }

        private void keepInArena(LivingEntity entity, double xOffset) {
            if (entity.distanceToSqr(Vec3.atCenterOf(center)) > 34.0 * 34.0) {
                entity.teleportTo(center.getX() + 0.5 + xOffset, center.getY() + 1.0, center.getZ() + 0.5);
                entity.fallDistance = 0.0f;
            }
        }

        private LivingEntity entity(UUID id) {
            Entity entity = id == null ? null : level.getEntity(id);
            return entity instanceof LivingEntity living ? living : null;
        }

        private void finish(LivingEntity sukuna, LivingEntity gojo, String reason) {
            done = true;
            Path dir = OpSukunaBrainTelemetry.exportReports();
            String summary = OpSukunaBrainTelemetry.summary();
            String report = OpSukunaBrainTelemetry.report();
            boolean sukunaDead = sukuna == null || !sukuna.isAlive();
            boolean gojoDead = gojo == null || !gojo.isAlive();
            String verdict = verdict(report, sukunaDead);
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("OpSukuna AI test finished by " + requester
                    + ": reason=" + reason
                    + ", ticks=" + age
                    + ", domain_forced=" + domainForced
                    + ", sukuna_dead=" + sukunaDead
                    + ", gojo_dead=" + gojoDead
                    + "\n" + verdict
                    + "\nFiles: " + dir
                    + "\n" + summary), false);
        }

        private String verdict(String report, boolean sukunaDead) {
            if (!domainForced) {
                return "verdict=warning: gojo_domain_not_forced";
            }
            if (report == null || report.isEmpty() || !report.contains("decisions=")) {
                return "verdict=warning: insufficient_telemetry";
            }
            if (sukunaDead && (report.contains("domain_caught") || report.contains("anti_domain_gap"))) {
                return "verdict=failed: sukuna_domain_death";
            }
            if (!report.contains("domain_signals=") && !report.contains("domain_state")) {
                return "verdict=warning: no_domain_signal_recorded";
            }
            return sukunaDead ? "verdict=warning: sukuna_dead_non_domain" : "verdict=ok";
        }

        private String status() {
            return "OpSukuna AI test gojo_muryo: tick=" + age + "/" + durationTicks
                    + ", force_domain_tick=" + forceDomainAtTick
                    + ", domain_forced=" + domainForced
                    + ", center=" + center.toShortString();
        }

        private void cleanup() {
            AABB box = new AABB(center).inflate(48.0);
            for (Entity entity : level.getEntities((Entity) null, box, entity -> entity.getTags().contains(TEST_TAG))) {
                entity.discard();
            }
        }

        private void buildArena() {
            int radius = 18;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos floor = center.offset(dx, -1, dz);
                    level.setBlock(floor, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                    for (int dy = 0; dy <= 5; dy++) {
                        level.setBlock(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}
