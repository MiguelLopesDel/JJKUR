package com.jujutsu.jujutsucraftaddon.mixins;

import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Debug mixin: logs who sets "skill" to a suspicious ID (>= 4000 or == -1000).
 * Uses async writes + deduplication: same (skillId, stackTrace) pair is logged once,
 * then only a counter is incremented.
 * Remove after the source is identified.
 */
@Mixin(value = CompoundTag.class, remap = false)
public abstract class SkillSetDebugMixin {

    // --- static async writer state ---
    private static final ConcurrentHashMap<String, AtomicInteger> SEEN = new ConcurrentHashMap<>();
    private static final LinkedBlockingQueue<String> WRITE_QUEUE = new LinkedBlockingQueue<>(4096);
    private static volatile boolean WRITER_STARTED = false;
    private static final Object WRITER_LOCK = new Object();
    private static Path LOG_PATH;

    static {
        startWriterThread();
    }

    private static void startWriterThread() {
        synchronized (WRITER_LOCK) {
            if (WRITER_STARTED) return;
            WRITER_STARTED = true;
        }
        LOG_PATH = Paths.get("run", "jjkur-op-ai-metrics", "skill_debug.log");
        Thread t = new Thread(() -> {
            try {
                Files.createDirectories(LOG_PATH.getParent());
                try (BufferedWriter bw = Files.newBufferedWriter(LOG_PATH,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    bw.write("=== SkillSetDebug session started " +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                            " ===\n");
                    bw.flush();
                    while (true) {
                        String line = WRITE_QUEUE.poll(5, TimeUnit.SECONDS);
                        if (line == null) {
                            bw.flush();
                            continue;
                        }
                        bw.write(line);
                        // drain batch
                        String next;
                        int batch = 0;
                        while ((next = WRITE_QUEUE.poll()) != null && batch++ < 64) {
                            bw.write(next);
                        }
                        bw.flush();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                System.err.println("[JJKU-DEBUG] skill_debug.log write error: " + e.getMessage());
            }
        }, "jjku-skill-debug-writer");
        t.setDaemon(true);
        t.start();
    }

    @Inject(method = "putDouble", at = @At("HEAD"))
    private void onPutDouble(String key, double value, CallbackInfo ci) {
        if (!"skill".equals(key)) return;
        if (value < 4000.0 && value != -1000.0) return;

        long skillId = (long) value;
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();

        // Build a compact key from the first 10 meaningful frames
        // skip: getStackTrace(0), onPutDouble(1), putDouble(2) — start at 3
        StringBuilder keyBuilder = new StringBuilder().append(skillId).append(':');
        int limit = Math.min(trace.length, 13);
        for (int i = 3; i < limit; i++) {
            StackTraceElement e = trace[i];
            keyBuilder.append(e.getClassName()).append('#').append(e.getMethodName()).append(';');
        }
        String dedupeKey = keyBuilder.toString();

        AtomicInteger counter = SEEN.computeIfAbsent(dedupeKey, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        if (count == 1) {
            // First occurrence — write full stack trace
            StringBuilder sb = new StringBuilder();
            sb.append("[JJKU-DEBUG] skill=").append(skillId).append(" (first occurrence)\n");
            for (int i = 3; i < limit; i++) {
                sb.append("  at ").append(trace[i]).append("\n");
            }
            WRITE_QUEUE.offer(sb.toString());
        } else if (count % 50 == 0) {
            // Every 50 occurrences, write a summary line
            WRITE_QUEUE.offer("[JJKU-DEBUG] skill=" + skillId + " seen " + count + "x (same stack)\n");
        }
        // Between 1 and 50: silently count
    }
}
