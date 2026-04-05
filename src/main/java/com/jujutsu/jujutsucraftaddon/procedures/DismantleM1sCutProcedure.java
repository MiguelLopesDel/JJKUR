package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModParticleTypes;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.fml.ModList;

public class DismantleM1sCutProcedure {

    public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
        if (entity == null || sourceentity == null) return;

        if (sourceentity instanceof LivingEntity attacker && attacker.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
            return;
        }

        sourceentity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
            if (addonVars.WorldSlash && addonVars.Moveset == 3) {
                sourceentity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(baseVars -> {
                    if (baseVars.PlayerCursePower > 10) {
                        spawnImpactParticles(world, entity);
                        drainEnergy(sourceentity, 10);
                        drainEnergy(entity, 10);
                    }
                });
            }
        });
    }

    private static void spawnImpactParticles(LevelAccessor world, Entity target) {
        if (!(world instanceof ServerLevel sLevel)) return;

        double x = target.getX();
        double y = target.getY() + 1;
        double z = target.getZ();

        if (ModList.get().isLoaded("jjkueffects")) {
            String particle = Math.random() < 0.5 ? "particle jjkueffects:de ~ ~1 ~ 0 0 0 1 1 force"
                                                  : "particle jjkueffects:de_2 ~ ~1 ~ 0 0 0 1 1 force";
            
            if (target.getServer() != null) {
                target.getServer().getCommands().performPrefixedCommand(
                    new CommandSourceStack(CommandSource.NULL, target.position(), target.getRotationVector(),
                    sLevel, 4, target.getName().getString(), target.getDisplayName(), sLevel.getServer(), target),
                    particle);
            }
        } else {
            ParticleOptions particleType = Math.random() < 0.5 ? JujutsucraftaddonModParticleTypes.KAI_5.get() 
                                                               : JujutsucraftaddonModParticleTypes.HAITI_5.get();
            sLevel.sendParticles(particleType, x, y, z, 1, 0, 0, 0, 1);
        }
    }

    private static void drainEnergy(Entity entity, double amount) {
        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
            vars.PlayerCursePower = Math.max(0, vars.PlayerCursePower - amount);
            vars.syncPlayerVariables(entity);
        });
    }
}
