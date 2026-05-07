package com.jujutsu.jujutsucraftaddon.command;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.util.OpSukunaAiTestRunner;
import com.jujutsu.jujutsucraftaddon.util.OpSukunaBrainTelemetry;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber()
public class JjkuOpAiMetricsCommand {
    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jjkuopai")
                .requires(source -> source.hasPermission(2))
                .executes(arguments -> {
                    arguments.getSource().sendSuccess(() -> Component.literal("Use /jjkuopai metrics status|summary|report|export|finish|flush|reset or /jjkuopai aitest run gojo_muryo"), false);
                    return 1;
                })
                .then(Commands.literal("metrics")
                        .executes(arguments -> {
                            arguments.getSource().sendSuccess(() -> Component.literal(status(arguments.getSource().getUnsidedLevel())), false);
                            return 1;
                        })
                        .then(Commands.literal("status").executes(arguments -> {
                            arguments.getSource().sendSuccess(() -> Component.literal(status(arguments.getSource().getUnsidedLevel())), false);
                            return 1;
                        }))
                        .then(Commands.literal("summary").executes(arguments -> {
                            String summary = OpSukunaBrainTelemetry.summary();
                            arguments.getSource().sendSuccess(() -> Component.literal(summary), false);
                            return 1;
                        }))
                        .then(Commands.literal("report").executes(arguments -> {
                            String report = OpSukunaBrainTelemetry.report();
                            arguments.getSource().sendSuccess(() -> Component.literal(report), false);
                            return 1;
                        }))
                        .then(Commands.literal("export").executes(arguments -> {
                            java.nio.file.Path dir = OpSukunaBrainTelemetry.exportReports();
                            arguments.getSource().sendSuccess(() -> Component.literal("OpSukuna metrics exported to " + dir), true);
                            return 1;
                        }))
                        .then(Commands.literal("finish").executes(arguments -> {
                            java.nio.file.Path dir = OpSukunaBrainTelemetry.exportReports();
                            String summary = OpSukunaBrainTelemetry.summary();
                            arguments.getSource().sendSuccess(() -> Component.literal("OpSukuna metrics finished. Files: " + dir + "\n" + summary), true);
                            return 1;
                        }))
                        .then(Commands.literal("flush").executes(arguments -> {
                            OpSukunaBrainTelemetry.flush();
                            arguments.getSource().sendSuccess(() -> Component.literal("OpSukuna metrics flushed."), false);
                            return 1;
                        }))
                        .then(Commands.literal("reset").executes(arguments -> {
                            int deleted = OpSukunaBrainTelemetry.reset();
                            arguments.getSource().sendSuccess(() -> Component.literal("OpSukuna metrics reset. Deleted files: " + deleted), true);
                            return deleted;
                        })))
                .then(Commands.literal("aitest")
                        .executes(arguments -> OpSukunaAiTestRunner.status(arguments.getSource()))
                        .then(Commands.literal("status")
                                .executes(arguments -> OpSukunaAiTestRunner.status(arguments.getSource())))
                        .then(Commands.literal("stop")
                                .executes(arguments -> OpSukunaAiTestRunner.stop(arguments.getSource())))
                        .then(Commands.literal("run")
                                .then(Commands.literal("gojo_muryo")
                                        .executes(arguments -> OpSukunaAiTestRunner.runGojoMuryo(arguments.getSource()))
                                        .then(Commands.argument("durationTicks", IntegerArgumentType.integer(200, 12000))
                                                .then(Commands.argument("forceDomainAtTick", IntegerArgumentType.integer(1, 11980))
                                                        .executes(arguments -> OpSukunaAiTestRunner.runGojoMuryo(arguments.getSource(),
                                                                IntegerArgumentType.getInteger(arguments, "durationTicks"),
                                                                IntegerArgumentType.getInteger(arguments, "forceDomainAtTick")))))))));
    }

    private static String status(net.minecraft.world.level.LevelAccessor world) {
        boolean devAllowed = OpSukunaBrainTelemetry.devTelemetryAllowed();
        boolean gamerule = world != null && world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_OP_SUKUNA_METRICS);
        boolean enabled = OpSukunaBrainTelemetry.enabled(world);
        return "OpSukuna metrics: command=registered, enabled=" + enabled
                + ", dev_flag=" + devAllowed
                + ", gamerule_jjkuOPSukunaMetrics=" + gamerule
                + ". To collect: start dev with -Djjku.opSukunaMetrics=true or env JJKU_OP_SUKUNA_METRICS_DEV=true, then /gamerule jjkuOPSukunaMetrics true.";
    }
}
