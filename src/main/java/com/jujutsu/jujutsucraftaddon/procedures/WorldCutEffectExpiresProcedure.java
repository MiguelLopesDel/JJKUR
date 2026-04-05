package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.entity.DismantleEntity;
import com.jujutsu.jujutsucraftaddon.entity.DismantleVariantEntity;
import com.jujutsu.jujutsucraftaddon.entity.WorldSlashFinalEntity;
import com.jujutsu.jujutsucraftaddon.entity.WorldSlashVariantEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModEntities;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.procedures.KeyStartTechniqueOnKeyReleasedProcedure;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.function.BiFunction;

public class WorldCutEffectExpiresProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
            double moveset = addonVars.Moveset;
            double outputLevel = addonVars.OutputLevel;

            if (moveset == 2) {
                boolean hasWSEffect = (entity instanceof LivingEntity living && living.hasEffect(JujutsucraftaddonModMobEffects.WORLD_SLASH_EFFECT.get()));
                
                if (!hasWSEffect) {
                    if (outputLevel >= 3) {
                        spawnSphere(world, x, y, z, entity, outputLevel * 1.5, outputLevel, 
                            (lvl, shooter) -> new DismantleEntity(JujutsucraftaddonModEntities.DISMANTLE.get(), lvl));
                    } else {
                        spawnDismantleBurst(world, entity, outputLevel, false);
                    }
                } else {
                    if (outputLevel >= 3) {
                        spawnSphere(world, x, y, z, entity, outputLevel * 1.5, outputLevel, 
                            (lvl, shooter) -> new WorldSlashVariantEntity(JujutsucraftaddonModEntities.WORLD_SLASH_VARIANT.get(), lvl));
                    } else {
                        spawnDismantleBurst(world, entity, outputLevel, true);
                    }
                }
            } else if (moveset == 1) {
                CleaveWebSpawnProcedure.execute(world, x, y, z, entity);
            } else if (moveset == 3) {
                if (outputLevel >= 3) {
                    spawnSphere(world, x, y, z, entity, outputLevel * 1.5, outputLevel, 
                        (lvl, shooter) -> new WorldSlashFinalEntity(JujutsucraftaddonModEntities.WORLD_SLASH_FINAL.get(), lvl));
                }
            } else if (moveset == 4) {
                if (outputLevel >= 5) {
                    KeyStartTechniqueOnKeyReleasedProcedure.execute(entity);
                    addonVars.OutputLevel -= 4;
                }
            } else if (moveset == 5) {
                WSCleaveWebProcedure.execute(world, x, y, z, entity);
            }

            addonVars.OutputLevel = Math.max(0, addonVars.OutputLevel - 1);
            addonVars.CustceneDone = 0;
            addonVars.syncPlayerVariables(entity);
        });
    }

    private static void spawnSphere(LevelAccessor world, double x, double y, double z, Entity entity, double radiusVal, double output, BiFunction<Level, Entity, AbstractArrow> factory) {
        if (!(world instanceof ServerLevel sLevel)) return;
        int radius = (int) radiusVal - 1;
        for (int i = -radius; i <= radius; i++) {
            for (int xi = -radius; xi <= radius; xi++) {
                for (int zi = -radius; zi <= radius; zi++) {
                    double distSq = (double) (xi * xi) / (radius * radius) + (double) (i * i) / (radius * radius) + (double) (zi * zi) / (radius * radius);
                    if (distSq <= 1.0) {
                        spawnProjectile(sLevel, x + xi, entity.getEyeY(), z + zi, entity, output, factory);
                    }
                }
            }
        }
    }

    private static void spawnDismantleBurst(LevelAccessor world, Entity entity, double output, boolean isWS) {
        if (!(world instanceof ServerLevel sLevel)) return;
        
        BiFunction<Level, Entity, AbstractArrow> f1 = (lvl, shooter) -> isWS ? new WorldSlashFinalEntity(JujutsucraftaddonModEntities.WORLD_SLASH_FINAL.get(), lvl) : new DismantleEntity(JujutsucraftaddonModEntities.DISMANTLE.get(), lvl);
        BiFunction<Level, Entity, AbstractArrow> f2 = (lvl, shooter) -> isWS ? new WorldSlashVariantEntity(JujutsucraftaddonModEntities.WORLD_SLASH_VARIANT.get(), lvl) : new DismantleVariantEntity(JujutsucraftaddonModEntities.DISMANTLE_VARIANT.get(), lvl);

        spawnProjectile(sLevel, entity.getX(), entity.getEyeY() - 0.1, entity.getZ(), entity, output, f1);
        spawnProjectile(sLevel, entity.getX(), entity.getEyeY() - 0.1, entity.getZ(), entity, output, f2);
        spawnProjectile(sLevel, entity.getX(), entity.getEyeY() - 0.1, entity.getZ(), entity, output, f1);
        spawnProjectile(sLevel, entity.getX(), entity.getEyeY() - 0.1, entity.getZ(), entity, output, f2);
    }

    private static void spawnProjectile(ServerLevel world, double px, double py, double pz, Entity owner, double output, BiFunction<Level, Entity, AbstractArrow> factory) {
        AbstractArrow arrow = factory.apply(world, owner);
        arrow.setOwner(owner);
        arrow.setBaseDamage(10 * output);
        arrow.setKnockback(0);
        arrow.setSilent(true);
        arrow.setPierceLevel((byte) 1);
        arrow.setPos(px, py, pz);
        arrow.shoot(owner.getLookAngle().x, owner.getLookAngle().y, owner.getLookAngle().z, 10, 0);
        world.addFreshEntity(arrow);
    }
}
