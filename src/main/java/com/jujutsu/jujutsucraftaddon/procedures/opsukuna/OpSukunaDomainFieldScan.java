package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class OpSukunaDomainFieldScan {
    static final OpSukunaDomainFieldScan NONE = new OpSukunaDomainFieldScan(0, 0, 0, false, false, false, false, false, false,
            null, "", "NONE", Vec3.ZERO, 0.0, 0.0);

    final int activeEnemyDomains;
    final int activeVoidDomains;
    final int clashDomains;
    final boolean clashSuppressed;
    final boolean singleDominantSureHit;
    final boolean dominantCasting;
    final boolean dominantVoidLike;
    final boolean dominantBarrierless;
    final boolean sukunaInsideDominant;
    final LivingEntity dominantOwner;
    final String dominantOwnerKey;
    final String dominantProfile;
    final Vec3 dominantCenter;
    final double dominantRadius;
    final double dominantThreat;

    OpSukunaDomainFieldScan(int activeEnemyDomains, int activeVoidDomains, int clashDomains, boolean clashSuppressed,
                    boolean singleDominantSureHit, boolean dominantCasting, boolean dominantVoidLike, boolean dominantBarrierless,
                    boolean sukunaInsideDominant, LivingEntity dominantOwner, String dominantOwnerKey,
                    String dominantProfile, Vec3 dominantCenter, double dominantRadius, double dominantThreat) {
        this.activeEnemyDomains = activeEnemyDomains;
        this.activeVoidDomains = activeVoidDomains;
        this.clashDomains = clashDomains;
        this.clashSuppressed = clashSuppressed;
        this.singleDominantSureHit = singleDominantSureHit;
        this.dominantCasting = dominantCasting;
        this.dominantVoidLike = dominantVoidLike;
        this.dominantBarrierless = dominantBarrierless;
        this.sukunaInsideDominant = sukunaInsideDominant;
        this.dominantOwner = dominantOwner;
        this.dominantOwnerKey = dominantOwnerKey;
        this.dominantProfile = dominantProfile;
        this.dominantCenter = dominantCenter;
        this.dominantRadius = dominantRadius;
        this.dominantThreat = dominantThreat;
    }

    static OpSukunaDomainFieldScan scan(LevelAccessor world, LivingEntity sukuna) {
        if (!(world instanceof Level level)) {
            return NONE;
        }
        AABB box = sukuna.getBoundingBox().inflate(58.0, 24.0, 58.0);
        int signals = 0;
        int active = 0;
        int voidDomains = 0;
        int clash = 0;
        DomainCandidate dominant = null;
        for (LivingEntity owner : level.getEntitiesOfClass(LivingEntity.class, box, entity -> OpSukunaEngineCore.isValidEnemy(world, sukuna, entity))) {
            OpSukunaPlayerRead read = OpSukunaPlayerRead.read(owner);
            CompoundTag data = owner.getPersistentData();
            double skill = data.getDouble("skill");
            double domainNumber = OpSukunaEngineCore.domainNumber(owner);
            boolean casting = OpSukunaEngineCore.isDomainCastSignal(owner, skill, domainNumber, read);
            boolean activeDomain = owner.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
            if (!activeDomain && !casting) {
                continue;
            }

            Vec3 center = domainCenter(owner);
            double radius = domainRadius(world, owner, read);
            boolean inside = isInsideDomain(sukuna, center, radius);
            boolean failed = data.getBoolean("Failed") && !data.getBoolean("Cover");
            boolean voidLike = OpSukunaEngineCore.isGojoTarget(owner, read) || skill == 220.0 || domainNumber == 2.0;
            boolean barrierless = read.barrierlessDomain || data.getBoolean("OPEN_DOMAIN") || data.getBoolean("barrierless_domain");
            double threat = (voidLike ? 1.0 : 0.68) + (inside ? 0.45 : 0.0) + (casting ? 0.25 : 0.0) - (failed ? 0.35 : 0.0);

            signals++;
            if (activeDomain) {
                active++;
            }
            if (activeDomain && voidLike) {
                voidDomains++;
            }
            if (failed || data.getDouble("cnt_domain_cancel") > 0.0 || data.getBoolean("Cover")) {
                clash++;
            }
            DomainCandidate candidate = new DomainCandidate(owner, center, radius, threat, inside, activeDomain, casting, failed, voidLike, barrierless);
            if (dominant == null || candidate.threat > dominant.threat) {
                dominant = candidate;
            }
        }
        if (signals <= 0 || dominant == null) {
            return NONE;
        }

        boolean suffering = sukuna.hasEffect(JujutsucraftModMobEffects.NEUTRALIZATION.get())
                || sukuna.hasEffect(JujutsucraftModMobEffects.BRAIN_DAMAGE.get())
                || sukuna.getPersistentData().getDouble("skill") == -999.0
                || OpSukunaEngineCore.hasExtremeVoidDebuff(sukuna);
        boolean suppressed = active > 1 && !suffering;
        int effectiveInside = 0;
        for (LivingEntity owner : level.getEntitiesOfClass(LivingEntity.class, box, entity -> OpSukunaEngineCore.isValidEnemy(world, sukuna, entity))) {
            OpSukunaPlayerRead read = OpSukunaPlayerRead.read(owner);
            CompoundTag data = owner.getPersistentData();
            boolean activeDomain = owner.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
            if (activeDomain && !data.getBoolean("Failed") && isInsideDomain(sukuna, domainCenter(owner), domainRadius(world, owner, read))) {
                effectiveInside++;
            }
        }
        boolean single = !suppressed && dominant.active && dominant.inside && effectiveInside == 1 && !dominant.failed;
        String profile = dominant.voidLike ? "GOJO_MURYO" : "GENERIC_DOMAIN";
        return new OpSukunaDomainFieldScan(active, voidDomains, clash, suppressed, single, dominant.casting, dominant.voidLike, dominant.barrierless,
                dominant.inside, dominant.owner, OpSukunaEngineCore.targetKey(dominant.owner), profile, dominant.center, dominant.radius,
                Mth.clamp(dominant.threat, 0.0, 1.0));
    }

    private static Vec3 domainCenter(LivingEntity owner) {
        CompoundTag data = owner.getPersistentData();
        double x = data.contains("x_pos_doma") ? data.getDouble("x_pos_doma") : owner.getX();
        double y = data.contains("y_pos_doma") ? data.getDouble("y_pos_doma") : owner.getY() + owner.getBbHeight() * 0.5;
        double z = data.contains("z_pos_doma") ? data.getDouble("z_pos_doma") : owner.getZ();
        return new Vec3(x, y, z);
    }

    private static double domainRadius(LevelAccessor world, LivingEntity owner, OpSukunaPlayerRead read) {
        double radius = JujutsucraftModVariables.MapVariables.get(world).DomainExpansionRadius;
        double explicit = owner.getPersistentData().getDouble("DomainExpansionSizer");
        if (explicit > 0.0) {
            radius = Math.max(radius, explicit);
        }
        if (read.barrierlessDomain) {
            radius *= 1.35;
        }
        return Mth.clamp(radius + 3.0, 12.0, 72.0);
    }

    private static boolean isInsideDomain(LivingEntity entity, Vec3 center, double radius) {
        Vec3 pos = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
        double dx = center.x - pos.x;
        double dy = center.y - pos.y;
        double dz = center.z - pos.z;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }
}

final class DomainCandidate {
    final LivingEntity owner;
    final Vec3 center;
    final double radius;
    final double threat;
    final boolean inside;
    final boolean active;
    final boolean casting;
    final boolean failed;
    final boolean voidLike;
    final boolean barrierless;

    DomainCandidate(LivingEntity owner, Vec3 center, double radius, double threat, boolean inside, boolean active,
                    boolean casting, boolean failed, boolean voidLike, boolean barrierless) {
        this.owner = owner;
        this.center = center;
        this.radius = radius;
        this.threat = threat;
        this.inside = inside;
        this.active = active;
        this.casting = casting;
        this.failed = failed;
        this.voidLike = voidLike;
        this.barrierless = barrierless;
    }
}

