package com.jujutsu.jujutsucraftaddon.util;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JJKUR Performance Monitor
 * Fornece métricas de tempo real (ms/tick) com impacto zero na build final.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class JJKURPerformanceMonitor {
    public static final boolean ENABLE_PROFILER = System.getProperty("jjkur.debug", "false").equals("true");
    
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LOG_FILE = "jjkur_performance.log";
    private static final Map<String, MethodStats> STATS = new ConcurrentHashMap<>();
    private static final int SAMPLE_INTERVAL_MS = 10; 
    
    private static Thread monitorThread;
    private static volatile boolean running = false;
    private static long sessionStartTime;
    private static final AtomicLong totalSamples = new AtomicLong(0);
    private static final AtomicLong tickCounter = new AtomicLong(0);

    public static void start() {
        if (!ENABLE_PROFILER || running) return;
        
        running = true;
        sessionStartTime = System.currentTimeMillis();
        monitorThread = new Thread(() -> {
            while (running) {
                try {
                    sample();
                    Thread.sleep(SAMPLE_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "JJKUR-Profiler");
        monitorThread.setDaemon(true);
        monitorThread.start();
        LOGGER.warn("[JJKUR] PROFILER ATIVO - Monitorando tempo real (ms/tick).");
    }

    private static void sample() {
        totalSamples.incrementAndGet();
        Map<Thread, StackTraceElement[]> allStacks = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStacks.entrySet()) {
            Thread t = entry.getKey();
            if (t.getName().contains("Server thread") || t.getName().contains("Render thread")) {
                for (StackTraceElement element : entry.getValue()) {
                    String className = element.getClassName();
                    if (className.startsWith("net.mcreator.jujutsucraft") || className.startsWith("com.jujutsu.jujutsucraftaddon")) {
                        String methodSig = className + "." + element.getMethodName();
                        STATS.computeIfAbsent(methodSig, k -> new MethodStats()).hit();
                        break; 
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!ENABLE_PROFILER) return;
        
        if (event.phase == TickEvent.Phase.END) {
            tickCounter.incrementAndGet();
            // Relatório a cada 1 minuto (aprox 1200 ticks)
            if (tickCounter.get() % 1200 == 0) { 
                printReport();
            }
        }
    }

    private static void printReport() {
        if (STATS.isEmpty()) return;

        List<Map.Entry<String, MethodStats>> sorted = new ArrayList<>(STATS.entrySet());
        sorted.sort((a, b) -> b.getValue().hits.get() - a.getValue().hits.get());

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        long currentTicks = tickCounter.get();

        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println("======= JJKUR REAL-TIME PERFORMANCE REPORT [" + timestamp + "] =======");
            writer.println("Ticks Monitored: " + currentTicks + " | Samples: " + totalSamples.get());
            writer.println(String.format("%-80s | %-12s | %-12s | %-8s", "Method Signature", "Total Time", "ms/Tick", "Load %"));
            writer.println("--------------------------------------------------------------------------------------------------------------------");

            for (int i = 0; i < Math.min(30, sorted.size()); i++) {
                Map.Entry<String, MethodStats> entry = sorted.get(i);
                long hits = entry.getValue().hits.get();
                
                // Cálculo estatístico de tempo real
                double totalMs = hits * SAMPLE_INTERVAL_MS;
                double msPerTick = totalMs / Math.max(1, currentTicks);
                double loadPct = (hits / (double) totalSamples.get()) * 100.0;

                writer.println(String.format("%-80s | %-9.0f ms | %-9.2f ms | %-7.2f%%", 
                    entry.getKey(), totalMs, msPerTick, loadPct));
            }
            writer.println("====================================================================================================================\n");
            LOGGER.info("[JJKUR] Relatório de Tempo Real salvo em " + LOG_FILE);
        } catch (IOException e) {
            LOGGER.error("Erro ao salvar profiler", e);
        }
    }

    private static class MethodStats {
        final AtomicInteger hits = new AtomicInteger(0);
        void hit() { hits.incrementAndGet(); }
    }
}
