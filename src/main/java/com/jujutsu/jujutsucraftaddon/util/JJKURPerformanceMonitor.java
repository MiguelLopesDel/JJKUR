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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JJKUR Performance Monitor (Sampling Profiler)
 * Monitora hot-spots de execução no Mod Base e Addon e salva em arquivo externo.
 */
//@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class JJKURPerformanceMonitor {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LOG_FILE = "jjkur_performance.log";
    private static final Map<String, AtomicInteger> SAMPLES = new ConcurrentHashMap<>();
    private static final int SAMPLE_INTERVAL_MS = 50;
    private static final int REPORT_INTERVAL_TICKS = 1200;
    
    private static Thread monitorThread;
    private static volatile boolean running = false;
    private static int tickCounter = 0;

    public static void start() {
        if (running) return;
        running = true;
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
        LOGGER.info("JJKUR Performance Monitor iniciado. Logs em: " + LOG_FILE);
    }

    public static void stop() {
        running = false;
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
    }

    private static void sample() {
        Map<Thread, StackTraceElement[]> allStacks = Thread.getAllStackTraces();
        
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStacks.entrySet()) {
            Thread t = entry.getKey();
            if (t.getName().contains("Server thread") || t.getName().contains("Render thread") || t.getName().contains("Client thread")) {
                for (StackTraceElement element : entry.getValue()) {
                    String className = element.getClassName();
                    if (className.startsWith("net.mcreator.jujutsucraft") || 
                        className.startsWith("com.jujutsu.jujutsucraftaddon")) {
                        
                        String methodSig = className + "." + element.getMethodName() + ":" + element.getLineNumber();
                        SAMPLES.computeIfAbsent(methodSig, k -> new AtomicInteger(0)).incrementAndGet();
                        break; 
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            if (tickCounter >= REPORT_INTERVAL_TICKS) {
                tickCounter = 0;
                printReport();
            }
        }
    }

    private static void printReport() {
        if (SAMPLES.isEmpty()) return;

        List<Map.Entry<String, AtomicInteger>> sorted = new ArrayList<>(SAMPLES.entrySet());
        sorted.sort((a, b) -> b.getValue().get() - a.getValue().get());

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println("======= JJKUR PERFORMANCE HOTSPOTS [" + timestamp + "] =======");
            int count = 0;
            for (Map.Entry<String, AtomicInteger> entry : sorted) {
                if (count++ >= 20) break;
                writer.println(String.format("[%d amostras] %s", entry.getValue().get(), entry.getKey()));
            }
            writer.println("==================================================================\n");
            writer.flush();
            LOGGER.info("[JJKUR] Relatório de performance salvo em " + LOG_FILE);
        } catch (IOException e) {
            LOGGER.error("[JJKUR] Erro ao escrever relatório de performance", e);
        }
        
        SAMPLES.clear(); 
    }
}
