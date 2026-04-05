package com.jujutsu.jujutsucraftaddon.procedures;

import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class MegumiReleaseProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null || !(world instanceof ServerLevel serverLevel)) return;

        Entity mahoraga = JujutsucraftModEntities.EIGHT_HANDLED_SWORD_DIVERGENT_SILA_DIVINE_GENERAL_MAHORAGA.get().spawn(serverLevel, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
        if (mahoraga instanceof LivingEntity livingMahoraga) {
            mahoraga.setYRot(world.getRandom().nextFloat() * 360.0F);
            
            var sizeAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("jujutsucraft:size"));
            if (sizeAttr != null && livingMahoraga.getAttributes().hasAttribute(sizeAttr)) {
                livingMahoraga.getAttribute(sizeAttr).setBaseValue(2.0);
            }

            if (livingMahoraga.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                double baseDamage = livingMahoraga.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
                livingMahoraga.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseDamage * 6.0);
            }

            if (livingMahoraga.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                double baseHealth = livingMahoraga.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
                livingMahoraga.getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHealth * 6.0);
            }

            livingMahoraga.setHealth(livingMahoraga.getMaxHealth());

            if (!world.isClientSide()) {
                livingMahoraga.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 3, false, false));
            }

            if (livingMahoraga.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED)) {
                livingMahoraga.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4);
            }

            livingMahoraga.getPersistentData().putBoolean("Buffed", true);
            serverLevel.addFreshEntity(livingMahoraga);
        }

        if (!world.isClientSide() && entity.getServer() != null) {
            CommandSourceStack stack = new CommandSourceStack(
                CommandSource.NULL, 
                entity.position(), 
                entity.getRotationVector(), 
                serverLevel, 
                4, 
                entity.getName().getString(), 
                entity.getDisplayName(), 
                entity.getServer(), 
                entity
            );
            entity.getServer().getCommands().performPrefixedCommand(stack, "kill");
        }

        Vec3 center = new Vec3(x, y, z);
        List<Entity> entitiesInRange = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(15.0), e -> e != entity);
        for (Entity target : entitiesInRange) {
            if (target instanceof LivingEntity livingTarget && !world.isClientSide()) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 254, false, false));
            }
        }
    }
}
