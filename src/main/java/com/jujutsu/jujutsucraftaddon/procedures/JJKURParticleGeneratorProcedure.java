package com.jujutsu.jujutsucraftaddon.procedures;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

public class JJKURParticleGeneratorProcedure {

    private static final Map<String, ParticleOptions> PARTICLE_CACHE = new HashMap<>();

    public static void execute(LevelAccessor world, double caliber_radius, double count, double inaccuracy, double speed, double x1, double x2, double y1, double y2, double z1, double z2, String id, CallbackInfo ci) {
        if (id == null || !(world instanceof ServerLevel _level)) return;

        if (ci != null) ci.cancel();

        ParticleOptions particleOptions = PARTICLE_CACHE.get(id);
        if (particleOptions == null) {
            ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(ResourceLocation.parse(id));
            if (type instanceof ParticleOptions opt) {
                particleOptions = opt;
                PARTICLE_CACHE.put(id, particleOptions);
            } else {
                return;
            }
        }

        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double baseDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (baseDist != 0.0) {
            dx /= baseDist;
            dy /= baseDist;
            dz /= baseDist;
        }

        double rad_inaccuracy = Math.toRadians(inaccuracy * 0.5);
        double cosTheta = Math.cos(rad_inaccuracy);
        double sinTheta = Math.sin(rad_inaccuracy);
        int maxCount = (int) Math.max(count, 1.0);

        var random = _level.getRandom();

        for (int i = 0; i < maxCount; i++) {
            double dirX = dx;
            double dirY = dy;
            double dirZ = dz;

            if (inaccuracy != 0.0) {
                double rnd1 = random.nextDouble() - 0.5;
                double rnd2 = random.nextDouble() - 0.5;
                double rnd3 = random.nextDouble() - 0.5;
                double rDist = Math.sqrt(rnd1 * rnd1 + rnd2 * rnd2 + rnd3 * rnd3);
                if (rDist != 0.0) {
                    rnd1 /= rDist;
                    rnd2 /= rDist;
                    rnd3 /= rDist;
                }

                double cross1 = rnd2 * dz - rnd3 * dy;
                double cross2 = rnd3 * dx - rnd1 * dz;
                double cross3 = rnd1 * dy - rnd2 * dx;
                double dot = rnd1 * dx + rnd2 * dy + rnd3 * dz;
                dirX = dx * cosTheta + cross1 * sinTheta + rnd1 * dot * (1.0 - cosTheta);
                dirY = dy * cosTheta + cross2 * sinTheta + rnd2 * dot * (1.0 - cosTheta);
                dirZ = dz * cosTheta + cross3 * sinTheta + rnd3 * dot * (1.0 - cosTheta);
            }

            double spawnX = x1 + caliber_radius * (random.nextDouble() - 0.5);
            double spawnY = y1 + caliber_radius * (random.nextDouble() - 0.5);
            double spawnZ = z1 + caliber_radius * (random.nextDouble() - 0.5);

            _level.sendParticles(particleOptions, spawnX, spawnY, spawnZ, 0, dirX, dirY, dirZ, speed);
        }
    }
}
