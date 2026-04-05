package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.KeyStartTechniqueOnKeyPressedProcedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class WorldSlashVariantsProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) return;

        ItemStack chest = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(chest.getItem());
        String armorName = registryName != null ? registryName.toString() : "";

        if (!armorName.equals("jujutsucraft:sukuna_body_chestplate") && chest.getItem() != JujutsucraftaddonModItems.SUKUNA_ARMOR_THREE_CHESTPLATE.get()) {
            return;
        }

        livingEntity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
            if (addonVars.OutputLevel >= 5) {
                applyAreaEffects(world, x, y, z, livingEntity);
                handleNBTAndCommands(world, livingEntity);
                KeyStartTechniqueOnKeyPressedProcedure.execute(world, x, y, z, livingEntity);
            }
        });
    }

    private static void applyAreaEffects(LevelAccessor world, double x, double y, double z, LivingEntity source) {
        Vec3 center = new Vec3(x, y, z);
        List<Entity> targets = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(30.0), e -> e != source);

        for (Entity target : targets) {
            if (target instanceof LivingEntity livingTarget) {
                if (!world.isClientSide()) {
                    livingTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 254, false, false));

                    int currentUnstable = livingTarget.hasEffect(JujutsucraftModMobEffects.UNSTABLE.get())
                            ? livingTarget.getEffect(JujutsucraftModMobEffects.UNSTABLE.get()).getAmplifier() : 0;
                    livingTarget.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.UNSTABLE.get(), 10000, currentUnstable + 2, false, false));
                }

                livingTarget.removeEffect(JujutsucraftModMobEffects.REVERSE_CURSED_TECHNIQUE.get());
                livingTarget.removeEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get());
                livingTarget.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            }
        }
        if (!targets.isEmpty() && !world.isClientSide())
            source.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 254, false, false));
    }

    private static void handleNBTAndCommands(LevelAccessor world, LivingEntity entity) {
        CompoundTag nbt = entity.getPersistentData();
        double cnt6 = nbt.getDouble("cnt6");

        if (cnt6 < 50) {
            nbt.putDouble("cnt6", cnt6 + 50 + Mth.nextInt(RandomSource.create(), 1, 40));
        }

        if (Math.random() < 1.0 / 30.0) {
            if (!world.isClientSide() && entity.getServer() != null) {
                CommandSourceStack stack = new CommandSourceStack(
                        CommandSource.NULL, entity.position(), entity.getRotationVector(),
                        world instanceof ServerLevel ? (ServerLevel) world : null, 4,
                        entity.getName().getString(), entity.getDisplayName(), entity.getServer(), entity
                );
                entity.getServer().getCommands().performPrefixedCommand(stack, "particle jjkueffects:aura_black ~ ~-1 ~ 0 0 0 1 1 force");
            }
        }
    }
}
