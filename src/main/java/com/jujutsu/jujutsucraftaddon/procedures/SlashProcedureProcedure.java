package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModParticleTypes;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public class SlashProcedureProcedure {

    public static void execute(LevelAccessor world, Entity entity, Entity sourceentity, double amount) {
        if (entity == null || sourceentity == null) return;
        if (sourceentity.isShiftKeyDown()) return;

        sourceentity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
            if (!(entity instanceof LivingEntity target)) return;

            CompoundTag sourceNBT = sourceentity.getPersistentData();
            if (sourceNBT.getDouble("skill") != 105) return;

            if (!vars.WorldSlash) {
                handleNormalSlash(world, target, sourceentity);
            } else {
                handleWorldSlash(world, target, sourceentity, vars, amount);
            }
        });
    }

    private static void handleNormalSlash(LevelAccessor world, LivingEntity target, Entity source) {
        ResourceLocation registryName = ForgeRegistries.ENTITY_TYPES.getKey(source.getType());
        String entityType = registryName != null ? registryName.toString() : "";

        if (ModList.get().isLoaded("jjkueffects")) {
            if (entityType.equals("jujutsucraft:sukuna") || entityType.equals("jujutsucraft:sukuna_fushiguro")) {
                spawnParticleCommand(world, target, "particle jjkueffects:dismantle ~ ~1 ~ 0 0 0 1 1 force");
            } else if (entityType.equals("jujutsucraft:sukuna_perfect")) {
                String particle = Math.random() < 0.5 ? "jjkueffects:de" : "jjkueffects:de_2";
                spawnParticleCommand(world, target, "particle " + particle + " ~ ~1 ~ 0 0 0 1 1 force");
            } else {
                spawnParticleCommand(world, target, "particle jjkueffects:dismantle ~ ~1 ~ 0 0 0 1 1 force");
            }
        } else {
            var particle = Math.random() < 0.5 ? JujutsucraftaddonModParticleTypes.HAITI_3.get() : JujutsucraftaddonModParticleTypes.KAI_3.get();
            spawnServerParticles(world, target, particle);
        }
    }

    private static void handleWorldSlash(LevelAccessor world, LivingEntity target, Entity source, JujutsucraftaddonModVariables.PlayerVariables vars, double amount) {
        if (vars.Moveset == 4) {
            applyFatigue(target);
            LimbssProcedure.execute(world, target, amount);
        }

        if (ModList.get().isLoaded("jjkueffects")) {
            String particle = Math.random() < 0.5 ? "jjkueffects:de" : "jjkueffects:de_2";
            spawnParticleCommand(world, target, "particle " + particle + " ~ ~1 ~ 0 0 0 1 1 force");
        } else {
            var particle = Math.random() < 0.5 ? JujutsucraftaddonModParticleTypes.KAI_5.get() : JujutsucraftaddonModParticleTypes.HAITI_5.get();
            spawnServerParticles(world, target, particle);
        }
    }

    private static void applyFatigue(LivingEntity target) {
        if (target.level().isClientSide()) return;
        int currentAmp = target.hasEffect(JujutsucraftModMobEffects.FATIGUE.get()) ? target.getEffect(JujutsucraftModMobEffects.FATIGUE.get()).getAmplifier() : 0;
        target.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.FATIGUE.get(), 120, currentAmp, false, true));
    }

    private static void spawnParticleCommand(LevelAccessor world, Entity target, String command) {
        if (!(world instanceof ServerLevel sLevel)) return;
        if (target.getServer() == null) return;
        
        CommandSourceStack stack = new CommandSourceStack(
            CommandSource.NULL, 
            target.position(), 
            target.getRotationVector(), 
            sLevel, 
            4, 
            target.getName().getString(), 
            target.getDisplayName(), 
            target.getServer(), 
            target
        );
        target.getServer().getCommands().performPrefixedCommand(stack, command);
    }

    private static void spawnServerParticles(LevelAccessor world, Entity target, net.minecraft.core.particles.ParticleOptions particle) {
        if (world instanceof ServerLevel sLevel) {
            sLevel.sendParticles(particle, target.getX(), target.getY() + 1, target.getZ(), 0, 0, 0, 0, 1);
        }
    }
}
