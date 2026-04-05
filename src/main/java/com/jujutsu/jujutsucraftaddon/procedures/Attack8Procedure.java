package com.jujutsu.jujutsucraftaddon.procedures;

import net.mcreator.jujutsucraft.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;

public class Attack8Procedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
        if (entity == null || sourceentity == null) return;
        if (!(entity instanceof LivingEntity)) return;

        ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityType == null || !entityType.getNamespace().equals("jujutsucraft")) return;

        CompoundTag sourceData = sourceentity.getPersistentData();
        if (sourceData.getDouble("Fight") != 0) return;

        String entityName = entityType.toString();

        if (entityName.equals("jujutsucraft:eight_handled_sword_divergent_sila_divine_general_mahoraga")) {
            Vec3 center = new Vec3(x, y, z);
            List<Entity> entities = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(10.0), e -> e instanceof Monster).stream()
                    .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).toList();

            for (Entity iterator : entities) {
                if (iterator instanceof SukunaEntity) {
                    playSound(world, x, y, z, "jujutsucraftaddon:sukunavsmaho");
                    sourceData.putDouble("Fight", 1);
                    return;
                }
            }
        } else if (sourceentity instanceof FushiguroTojiEntity) {
            if (entity instanceof GetoSuguruEntity) {
                playSound(world, x, y, z, "jujutsucraftaddon:getovstoji");
                sourceData.putDouble("Fight", 1);
            } else if (entity instanceof GojoSatoruSchoolDaysEntity) {
                playSound(world, x, y, z, "jujutsucraftaddon:gojovstoji");
                sourceData.putDouble("Fight", 1);
            }
        } else if (sourceentity instanceof NanamiKentoEntity) {
            if (entity instanceof MahitoEntity) {
                playSound(world, x, y, z, "jujutsucraftaddon:nanami");
                sourceData.putDouble("Fight", 1);
            }
        } else if (sourceentity instanceof ItadoriYujiShibuyaEntity) {
            if (entity instanceof MahitoEntity) {
                playSound(world, x, y, z, "jujutsucraftaddon:itadorivsmahito");
                sourceData.putDouble("Fight", 1);
            } else if (entity instanceof ChosoEntity) {
                playSound(world, x, y, z, "jujutsucraftaddon:chosovsyuuuji");
                sourceData.putDouble("Fight", 1);
            }
        } else if (sourceentity instanceof FushiguroTojiBugEntity) {
            if (entity instanceof DagonEntity) {
                playSound(world, x, y, z, "jujutsucraftaddon:tojivsdagonvsyujivsmahito");
                sourceData.putDouble("Fight", 1);
            }
        } else if (sourceentity instanceof SukunaEntity) {
            if (entity instanceof JogoEntity) {
                playSound(world, x, y, z, "jujutsucraftaddon:sukunavsjogo");
                sourceData.putDouble("Fight", 1);
            }
        } else if (sourceentity instanceof OkkotsuYutaEntity) {
            if (entity instanceof GetoSuguruCurseUserEntity) {
                playSound(world, x, y, z, "jujutsucraftaddon:yutavsgeto");
                sourceData.putDouble("Fight", 1);
            }
        }
    }

    private static void playSound(LevelAccessor world, double x, double y, double z, String soundPath) {
        if (world instanceof Level level) {
            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(soundPath));
            if (sound == null) return;

            if (!level.isClientSide()) {
                level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.MUSIC, 1, 1);
            } else {
                level.playLocalSound(x, y, z, sound, SoundSource.MUSIC, 1, 1, false);
            }
        }
    }
}
