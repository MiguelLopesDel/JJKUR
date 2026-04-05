package com.jujutsu.jujutsucraftaddon.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.FistGunProcedure;
import net.mcreator.jujutsucraft.procedures.GaragaraProcedure;
import net.mcreator.jujutsucraft.procedures.OtherDomainExpansionProcedure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

public class CursedTechniqueWukong {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        CompoundTag persistentData = entity.getPersistentData();
        int skill = (int) Math.round(persistentData.getDouble("skill") - 10000.0);

        switch (skill) {
            case 3 -> GaragaraProcedure.execute(world, entity);
            case 4 -> FistGunProcedure.execute(world, entity);
            case 5 -> {
                if (entity.isShiftKeyDown()) {
                    CloneDespawn.execute(world, x, y, z, entity);
                }
                persistentData.putDouble("skill", 0.0);
            }
            case 6 -> {
                WukongWrath.execute(world, x, y, z, entity);
                persistentData.putDouble("skill", 0.0);
            }
            case 7 -> {
                if (entity instanceof LivingEntity livingEntity && !world.isClientSide()) {
                    livingEntity.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.GUARD.get(), 1200, 3, false, false));
                }
                persistentData.putDouble("skill", 0.0);
            }
            case 8 -> CloneMeteor.execute(world, x, y, z, entity);
            case 9 -> {
                if (entity instanceof LivingEntity livingEntity && !world.isClientSide()) {
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 4, false, false));
                }
                persistentData.putDouble("skill", 0.0);
            }
            case 20 -> OtherDomainExpansionProcedure.execute(world, x, y, z, entity);
            default -> {
                if (entity instanceof LivingEntity livingEntity) {
                    livingEntity.removeEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get());
                }
            }
        }
    }
}
