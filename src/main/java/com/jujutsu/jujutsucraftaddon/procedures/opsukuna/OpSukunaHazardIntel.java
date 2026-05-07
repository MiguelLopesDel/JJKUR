package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.procedures.DetectEnemyProjectileProcedure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

final class OpSukunaHazardIntel {
    private OpSukunaHazardIntel() {
    }

static DangerScan scanDanger(LevelAccessor world, LivingEntity sukuna) {
    if (!(world instanceof Level level)) {
        return DangerScan.NONE;
    }
    AABB box = sukuna.getBoundingBox().inflate(9.0);
    double areaRisk = 0.0;
    double incomingRisk = 0.0;
    Vec3 dodgeVector = Vec3.ZERO;
    Vec3 sukunaCenter = sukuna.position().add(0.0, sukuna.getBbHeight() * 0.5, 0.0);
    for (Entity entity : level.getEntities(sukuna, box, entity -> entity instanceof Projectile)) {
        if (entity instanceof Projectile projectile && projectile.getOwner() == sukuna) {
            continue;
        }
        boolean enemyProjectile = DetectEnemyProjectileProcedure.execute(sukuna, entity);
        double dist = Math.max(1.0, entity.distanceTo(sukuna));
        areaRisk += (enemyProjectile ? 1.35 : 0.45) / dist;
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() > 1.0E-4) {
            Vec3 toSukuna = sukunaCenter.subtract(entity.position());
            Vec3 travel = velocity.normalize();
            double approach = travel.dot(toSukuna.normalize());
            double missDistance = toSukuna.subtract(travel.scale(toSukuna.dot(travel))).length();
            if (approach > 0.55 && missDistance < 3.5) {
                double risk = approach * (3.5 - missDistance) / 3.5 * Mth.clamp(9.0 / dist, 0.0, 1.0);
                if (enemyProjectile) {
                    risk *= 1.35;
                }
                incomingRisk += risk;
                Vec3 lateral = new Vec3(-travel.z, 0.0, travel.x);
                if (lateral.lengthSqr() > 1.0E-4) {
                    dodgeVector = dodgeVector.add(lateral.normalize().scale(risk));
                }
            }
        }
    }
    if (dodgeVector.lengthSqr() < 1.0E-4) {
        dodgeVector = new Vec3(1.0, 0.0, 0.0);
    } else {
        dodgeVector = dodgeVector.normalize();
    }
    return new DangerScan(Mth.clamp(areaRisk, 0.0, 1.0), Mth.clamp(incomingRisk, 0.0, 1.0), dodgeVector);
}

static PurpleThreat scanPurpleThreat(LevelAccessor world, LivingEntity sukuna, LivingEntity target, double targetSkill, OpSukunaPlayerRead read) {
    if (!(world instanceof Level level)) {
        return PurpleThreat.NONE;
    }
    Vec3 sukunaCenter = sukuna.position().add(0.0, sukuna.getBbHeight() * 0.5, 0.0);
    boolean gojo = OpSukunaEngineCore.isGojoTarget(target, read);
    boolean windup = targetSkill == 215.0
            || target.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get())
            || target.hasEffect(JujutsucraftaddonModMobEffects.WORLD_GOJO.get())
            || (gojo && target.getPersistentData().getDouble("cnt1") > 0.0 && target.getPersistentData().getDouble("cnt1") < 85.0);
    double risk = windup ? Mth.clamp(1.0 - target.distanceTo(sukuna) / 48.0, 0.25, 0.85) : 0.0;
    boolean projectile = false;
    boolean lineOfFire = false;
    Vec3 dodge = Vec3.ZERO;
    AABB box = sukuna.getBoundingBox().inflate(42.0);
    for (Entity entity : level.getEntities(sukuna, box, OpSukunaHazardIntel::isPurpleEntity)) {
        if (entity instanceof Projectile projectileEntity && projectileEntity.getOwner() == sukuna) {
            continue;
        }
        projectile = true;
        Vec3 velocity = purpleVelocity(entity);
        Vec3 fromPurple = sukunaCenter.subtract(entity.position());
        double dist = Math.max(1.0, fromPurple.length());
        double size = Math.max(2.5, entity.getBbWidth() + entity.getPersistentData().getDouble("Range") * 0.18
                + entity.getPersistentData().getDouble("BlockRange") * 0.08);
        double localRisk = Mth.clamp(size / dist, 0.0, 0.75);
        if (velocity.lengthSqr() > 1.0E-4) {
            Vec3 travel = velocity.normalize();
            double approach = travel.dot(fromPurple.normalize());
            double miss = fromPurple.subtract(travel.scale(fromPurple.dot(travel))).length();
            if (approach > 0.35 && miss < size + 2.75) {
                lineOfFire = true;
                localRisk += approach * Mth.clamp((size + 2.75 - miss) / Math.max(1.0, size + 2.75), 0.0, 1.0);
                Vec3 lateral = new Vec3(-travel.z, 0.0, travel.x);
                if (lateral.lengthSqr() > 1.0E-4) {
                    dodge = dodge.add(lateral.normalize().scale(localRisk));
                }
            }
        } else {
            dodge = dodge.add(OpSukunaEngineCore.horizontal(sukuna.position().subtract(entity.position())).scale(localRisk));
        }
        risk = Math.max(risk, localRisk);
    }
    if (dodge.lengthSqr() < 1.0E-4) {
        Vec3 targetLine = OpSukunaEngineCore.horizontal(sukuna.position().subtract(target.position()));
        dodge = targetLine.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(-targetLine.z, 0.0, targetLine.x);
    }
    risk = Mth.clamp(risk, 0.0, 1.0);
    return new PurpleThreat(risk > 0.22 || windup, risk, windup, projectile, lineOfFire, dodge.normalize());
}

private static boolean isPurpleEntity(Entity entity) {
    ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
    String name = id == null ? entity.getType().toString().toLowerCase() : id.toString().toLowerCase();
    CompoundTag data = entity.getPersistentData();
    return name.equals("jujutsucraft:purple")
            || name.contains("purple")
            || data.getDouble("purple") == 2.0
            || data.getBoolean("flag_purple");
}

private static Vec3 purpleVelocity(Entity entity) {
    CompoundTag data = entity.getPersistentData();
    Vec3 persistent = new Vec3(data.getDouble("x_power"), data.getDouble("y_power"), data.getDouble("z_power"));
    return persistent.lengthSqr() > 1.0E-4 ? persistent : entity.getDeltaMovement();
}

static class DangerScan {
    static final DangerScan NONE = new DangerScan(0.0, 0.0, new Vec3(1.0, 0.0, 0.0));
    final double areaRisk;
    final double incomingProjectileRisk;
    final Vec3 dodgeVector;

    DangerScan(double areaRisk, double incomingProjectileRisk, Vec3 dodgeVector) {
        this.areaRisk = areaRisk;
        this.incomingProjectileRisk = incomingProjectileRisk;
        this.dodgeVector = dodgeVector;
    }
}

static class PurpleThreat {
    static final PurpleThreat NONE = new PurpleThreat(false, 0.0, false, false, false, new Vec3(1.0, 0.0, 0.0));
    final boolean threat;
    final double risk;
    final boolean windup;
    final boolean projectile;
    final boolean lineOfFire;
    final Vec3 dodgeVector;

    PurpleThreat(boolean threat, double risk, boolean windup, boolean projectile, boolean lineOfFire, Vec3 dodgeVector) {
        this.threat = threat;
        this.risk = risk;
        this.windup = windup;
        this.projectile = projectile;
        this.lineOfFire = lineOfFire;
        this.dodgeVector = dodgeVector;
    }
}

}
