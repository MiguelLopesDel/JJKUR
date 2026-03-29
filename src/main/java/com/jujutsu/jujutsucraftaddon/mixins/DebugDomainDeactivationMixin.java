package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.procedures.CursedTechniqueOnPotionActiveTickProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;

@Mixin(value = CursedTechniqueOnPotionActiveTickProcedure.class, priority = -10000)
public class DebugDomainDeactivationMixin {

    @Inject(method = "execute", at = @At("HEAD"), remap = false)
    private static void debugExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity instanceof Player player && !world.isClientSide()) {
            double skill = entity.getPersistentData().getDouble("skill");
            double cnt3 = entity.getPersistentData().getDouble("cnt3");
            boolean pressZ = entity.getPersistentData().getBoolean("PRESS_Z");
            boolean hasDomain = player.hasEffect((MobEffect)JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
            
            JujutsucraftModVariables.PlayerVariables vars = player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftModVariables.PlayerVariables());

            // Monitora se o domínio está ativo ou se o player está tentando desativar (skill ID termina em 21 ou 19)
            if (hasDomain || skill % 100 == 21 || vars.PlayerSelectCurseTechnique == 21) {
                System.out.println("[DEBUG JJKUR] Player: " + player.getName().getString() +
                                   " | Skill: " + skill +
                                   " | SelectTech: " + vars.PlayerSelectCurseTechnique +
                                   " | PRESS_Z: " + pressZ +
                                   " | cnt3: " + cnt3 +
                                   " | HasDomain: " + hasDomain +
                                   " | SecondTech: " + vars.SecondTechnique +
                                   " | TechID1: " + vars.PlayerCurseTechnique +
                                   " | TechID2: " + vars.PlayerCurseTechnique2);
            }
        }
    }
}
