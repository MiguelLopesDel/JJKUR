package com.jujutsu.jujutsucraftaddon.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.TechniqueNeedleProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class YorozuzuAttackProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        if (Math.random() < 1.0 / 300.0) {
            if (world instanceof ServerLevel serverLevel) {
                CompoundTag entityNBT = entity.getPersistentData();
                double friendNum = entityNBT.getDouble("friend_num");
                String ownerUuid = entity.getStringUUID();
                int damageBoostAmp = (entity instanceof LivingEntity living && living.hasEffect(MobEffects.DAMAGE_BOOST)) ? 
                                     living.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;

                Vec3 center = new Vec3(x, y, z);
                List<Entity> targets = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(15.0), e -> e != entity).stream()
                        .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).toList();

                for (Entity target : targets) {
                    spawnNeedle(serverLevel, x, y, z, target, ownerUuid, friendNum, damageBoostAmp);
                }
            }
            TechniqueNeedleProcedure.execute(world, entity);
        }
    }

    private static void spawnNeedle(ServerLevel level, double x, double y, double z, Entity target, String ownerUuid, double friendNum, int damageBoostAmp) {
        Entity needle = JujutsucraftModEntities.NEEDLE.get().spawn(level, target.blockPosition(), MobSpawnType.MOB_SUMMONED);
        if (needle instanceof LivingEntity livingNeedle) {
            needle.setYRot(level.getRandom().nextFloat() * 360.0F);
            
            CompoundTag nbt = needle.getPersistentData();
            nbt.putString("OWNER_UUID", ownerUuid);
            nbt.putDouble("friend_num", friendNum);
            nbt.putDouble("friend_num2", friendNum);
            nbt.putDouble("skill", 2909.0);
            
            double randomVal = Math.random();
            nbt.putDouble("NameRanged_ranged", randomVal);
            nbt.putDouble("NameRanged", randomVal);

            double targetX = x + Mth.nextInt(RandomSource.create(), -2, 2);
            needle.teleportTo(targetX, y, z);

            if (!level.isClientSide()) {
                int ctAmp = livingNeedle.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get()) ? 
                            livingNeedle.getEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get()).getAmplifier() : 0;
                livingNeedle.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), 10000, ctAmp, false, false));
                livingNeedle.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 3, false, false));
                livingNeedle.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, damageBoostAmp, false, false));
            }

            livingNeedle.setHealth(livingNeedle.getMaxHealth());
            level.addFreshEntity(livingNeedle);
        }
    }
}
