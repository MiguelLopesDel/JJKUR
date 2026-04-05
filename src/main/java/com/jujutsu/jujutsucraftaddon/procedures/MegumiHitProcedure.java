package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class MegumiHitProcedure {

    public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
        if (entity == null || sourceentity == null || !(world instanceof ServerLevel serverLevel)) return;

        CompoundTag entityNBT = entity.getPersistentData();
        double friendNum = entityNBT.getDouble("friend_num");
        String ownerUuid = entity.getStringUUID();
        BlockPos entityPos = entity.blockPosition();
        BlockPos sourcePos = sourceentity.blockPosition();

        if (Math.random() <= 0.02 && entityNBT.getDouble("TenShadowsTechnique14") != -2) {
            ItemStack helmet = (entity instanceof LivingEntity living) ? living.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY;
            if (ForgeRegistries.ITEMS.getKey(helmet.getItem()).toString().equals("jujutsucraft:mahoraga_wheel_helmet")) {
                spawnShikigami(serverLevel, entity, sourcePos, JujutsucraftModEntities.EIGHT_HANDLED_SWORD_DIVERGENT_SILA_DIVINE_GENERAL_MAHORAGA, "MAHORAGA!!!", 2.0, friendNum, ownerUuid, null);
            }
        }

        if (Math.random() < 1.0 / 50.0) {
            if (entityNBT.getDouble("TenShadowsTechnique13") < 0) {
                spawnShikigami(serverLevel, entity, entityPos, JujutsucraftModEntities.MERGED_BEAST_AGITO, "Nue Kon - Kango Ju Agito...", 2.0, friendNum, ownerUuid, null);
            }
        } else if (Math.random() < 1.0 / 45.0) {
            if (entityNBT.getDouble("TenShadowsTechnique4") < 0) {
                spawnShikigami(serverLevel, entity, entityPos, JujutsucraftModEntities.NUE, "NUE", 5.0, friendNum, ownerUuid, "Giant");
            }
        } else if (Math.random() < 1.0 / 40.0) {
            if (entityNBT.getDouble("TenShadowsTechnique10") < 0) {
                spawnShikigami(serverLevel, entity, entityPos, JujutsucraftModEntities.PIERCING_OX, "Kangyu..", 2.0, friendNum, ownerUuid, null);
            }
        } else if (Math.random() < 1.0 / 35.0) {
            if (entityNBT.getDouble("TenShadowsTechnique6") < 0) {
                spawnShikigami(serverLevel, entity, entityPos, JujutsucraftModEntities.TOAD, "Gama..", 2.0, friendNum, ownerUuid, null);
            }
        } else if (Math.random() < 1.0 / 30.0) {
            if (entityNBT.getDouble("TenShadowsTechnique8") < 0) {
                if (entity instanceof Player player && !player.level().isClientSide()) {
                    player.displayClientMessage(Component.literal("Datto"), false);
                }
                for (int i = 0; i < 6; i++) {
                    spawnShikigami(serverLevel, null, entityPos, JujutsucraftModEntities.RABBIT_ESCAPE, null, 2.0, friendNum, ownerUuid, null);
                }
            }
        } else if (Math.random() < 1.0 / 27.0) {
            if (entityNBT.getDouble("TenShadowsTechnique11") < 0) {
                spawnShikigami(serverLevel, entity, entityPos, JujutsucraftModEntities.TIGER_FUNERAL, " Koso!", 2.0, friendNum, ownerUuid, null);
            }
        } else if (Math.random() < 1.0 / 25.0) {
            if (entityNBT.getDouble("TenShadowsTechnique5") < 0) {
                spawnShikigami(serverLevel, entity, entityPos, JujutsucraftModEntities.GREAT_SERPENT, "Orochi", 2.0, friendNum, ownerUuid, null);
            }
        } else if (Math.random() < 1.0 / 20.0) {
            if (entityNBT.getDouble("TenShadowsTechnique7") < 0) {
                BlockPos spawnPos = entityPos.above(30);
                spawnShikigami(serverLevel, entity, spawnPos, JujutsucraftModEntities.MAX_ELEPHANT, "BANSHO!", 5.0, friendNum, ownerUuid, "flag_fall");
            }
        } else if (Math.random() < 1.0 / 15.0) {
            if (entityNBT.getDouble("TenShadowsTechnique3") < 0) {
                spawnShikigami(serverLevel, entity, entityPos, JujutsucraftModEntities.DIVINE_DOG_TOTALITY, "Gyokuken Kon..", 2.0, friendNum, ownerUuid, null);
            }
        } else if (Math.random() < 1.0 / 10.0) {
            if (entityNBT.getDouble("TenShadowsTechnique2") < 0) {
                spawnShikigami(serverLevel, entity, entityPos, JujutsucraftModEntities.DIVINE_DOG_WHITE, "Gyokuken..", 2.0, friendNum, ownerUuid, null);
            }
        } else if (Math.random() < 1.0 / 10.0) {
            if (entityNBT.getDouble("TenShadowsTechnique1") < 0) {
                spawnShikigami(serverLevel, entity, entityPos, JujutsucraftModEntities.DIVINE_DOG_BLACK, "Gyokuken..", 2.0, friendNum, ownerUuid, null);
            }
        }
    }

    private static void spawnShikigami(ServerLevel level, Entity owner, BlockPos pos, Supplier<? extends EntityType<?>> typeSupplier, String message, double size, double friendNum, String ownerUuid, String extraTag) {
        Entity spawned = typeSupplier.get().spawn(level, pos, MobSpawnType.MOB_SUMMONED);
        if (spawned instanceof LivingEntity spirit) {
            spirit.setYRot(level.getRandom().nextFloat() * 360.0F);
            CompoundTag nbt = spirit.getPersistentData();
            nbt.putString("OWNER_UUID", ownerUuid);
            nbt.putDouble("friend_num", friendNum);
            if (extraTag != null) nbt.putBoolean(extraTag, true);

            int duration = extraTag != null && extraTag.equals("flag_fall") ? 80 : 60;
            if (!spirit.level().isClientSide()) {
                spirit.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.MANIFESTATION.get(), duration, 1, false, false));
            }
            
            Attribute sizeAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("jujutsucraft:size"));
            if (sizeAttr != null && spirit.getAttributes().hasAttribute(sizeAttr)) {
                spirit.getAttribute(sizeAttr).setBaseValue(size);
            }

            if (message != null && owner instanceof Player player && !player.level().isClientSide()) {
                player.displayClientMessage(Component.literal(message), false);
            }
        }
    }
}
