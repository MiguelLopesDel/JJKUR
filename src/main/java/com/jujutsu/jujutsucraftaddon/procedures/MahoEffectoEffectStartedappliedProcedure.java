package com.jujutsu.jujutsucraftaddon.procedures;

import net.mcreator.jujutsucraft.entity.EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public class MahoEffectoEffectStartedappliedProcedure {

    public static void execute(Entity entity) {
        if (entity == null) return;

        if (!entity.level().isClientSide() && entity.getServer() != null) {
            CommandSourceStack stack = new CommandSourceStack(
                CommandSource.NULL, 
                entity.position(), 
                entity.getRotationVector(), 
                entity.level() instanceof ServerLevel _level ? _level : null, 
                4, 
                entity.getName().getString(), 
                entity.getDisplayName(), 
                entity.getServer(), 
                entity
            );
            entity.getServer().getCommands().performPrefixedCommand(stack, "execute as @e[distance=..30] run effect clear @e[distance=..30] jujutsucraft:domain_expansion");
        }

        if (entity instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity mahoraga) {
            mahoraga.setAnimation("sword_overhead");
        }
    }
}
