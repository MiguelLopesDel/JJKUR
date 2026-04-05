package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class SummonAllShadowsProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
        if (entity == null || sourceentity == null || !(world instanceof ServerLevel serverLevel)) return;

        if (!(sourceentity instanceof LivingEntity livingSource)) return;

        if (livingSource.getHealth() > livingSource.getMaxHealth() / 2.0) return;

        CompoundTag sourceNBT = sourceentity.getPersistentData();
        if (sourceNBT.getDouble("Started") != 0) return;

        double friendNum = sourceNBT.getDouble("friend_num");
        String ownerUuid = sourceentity.getStringUUID();
        BlockPos pos = BlockPos.containing(x, y, z);

        spawnEnhancedShadow(serverLevel, JujutsucraftModEntities.DIVINE_DOG_BLACK, pos, friendNum, ownerUuid);
        spawnEnhancedShadow(serverLevel, JujutsucraftModEntities.DIVINE_DOG_WHITE, pos, friendNum, ownerUuid);
        spawnEnhancedShadow(serverLevel, JujutsucraftModEntities.DIVINE_DOG_TOTALITY, pos, friendNum, ownerUuid);
        spawnEnhancedShadow(serverLevel, JujutsucraftModEntities.TOAD, pos, friendNum, ownerUuid);
        spawnEnhancedShadow(serverLevel, JujutsucraftModEntities.TOAD_2, pos, friendNum, ownerUuid);
        spawnEnhancedShadow(serverLevel, JujutsucraftModEntities.MERGED_BEAST_AGITO, pos, friendNum, ownerUuid);
        spawnEnhancedShadow(serverLevel, JujutsucraftModEntities.EIGHT_HANDLED_SWORD_DIVERGENT_SILA_DIVINE_GENERAL_MAHORAGA, pos, friendNum, ownerUuid);
        spawnEnhancedShadow(serverLevel, JujutsucraftModEntities.PIERCING_OX, pos, friendNum, ownerUuid);
        spawnEnhancedShadow(serverLevel, JujutsucraftModEntities.ROUND_DEER, pos, friendNum, ownerUuid);
        spawnEnhancedShadow(serverLevel, JujutsucraftModEntities.TIGER_FUNERAL, pos, friendNum, ownerUuid);
        spawnEnhancedShadow(serverLevel, JujutsucraftModEntities.GREAT_SERPENT, pos, friendNum, ownerUuid);
        spawnEnhancedShadow(serverLevel, JujutsucraftModEntities.MAX_ELEPHANT, pos, friendNum, ownerUuid);
        spawnEnhancedShadow(serverLevel, JujutsucraftModEntities.NUE, pos, friendNum, ownerUuid);
        spawnEnhancedShadow(serverLevel, JujutsucraftModEntities.RABBIT_ESCAPE, pos, friendNum, ownerUuid);

        if (entity instanceof Player player && !world.isClientSide()) {
            player.displayClientMessage(Component.literal("“When did I say it’s a fair fight? Now it's 10 vs 1”"), false);
        }

        sourceNBT.putDouble("Started", 1.0);
    }

    private static void spawnEnhancedShadow(ServerLevel level, Supplier<? extends EntityType<?>> typeSupplier, BlockPos pos, double friendNum, String ownerUuid) {
        Entity spawned = typeSupplier.get().spawn(level, pos, MobSpawnType.MOB_SUMMONED);
        if (spawned instanceof LivingEntity shadow) {
            shadow.setYRot(level.getRandom().nextFloat() * 360.0F);
            
            CompoundTag nbt = shadow.getPersistentData();
            nbt.putString("OWNER_UUID", ownerUuid);
            nbt.putDouble("friend_num", friendNum);
            nbt.putDouble("friend_num2", friendNum);
            nbt.putDouble("friend_num_worker", friendNum);
            nbt.putBoolean("Buffed", true);

            if (!level.isClientSide()) {
                shadow.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get(), -1, 254, false, false));
                int currentAmp = shadow.hasEffect(MobEffects.DAMAGE_BOOST) ? shadow.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;
                shadow.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, currentAmp + 1, false, false));
            }

            Attribute sizeAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("jujutsucraft:size"));
            if (sizeAttr != null && shadow.getAttributes().hasAttribute(sizeAttr)) {
                double baseSize = shadow.getAttribute(sizeAttr).getBaseValue();
                shadow.getAttribute(sizeAttr).setBaseValue(baseSize * 2.5);
            }
            
            shadow.setHealth(shadow.getMaxHealth());
            level.addFreshEntity(shadow);
        }
    }
}
