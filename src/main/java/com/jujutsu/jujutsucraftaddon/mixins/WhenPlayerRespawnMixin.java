package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.PlayerSetProfessionProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerSetProfessionProcedure.class, priority = -10000)
public abstract class WhenPlayerRespawnMixin {

    /**
     * @author Satushi / RIGOROUS RESTORATION
     * @reason RESTORED: Full parity with original JJKUR logic (File A). 
     * Corrected NBT types, technique-based NonSorcerer check, and Profession attribute mapping.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        // RESTORED: Survival check covering the whole logic
        if (entity.isAlive()) {
            JujutsucraftModVariables.PlayerVariables pVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftModVariables.PlayerVariables());

            // Reset NBT Tags
            entity.getPersistentData().putBoolean("JujutsuSorcerer", false);
            entity.getPersistentData().putBoolean("CurseUser", false);
            entity.getPersistentData().putBoolean("CursedSpirit", false);

            double profession = pVars.PlayerProfession;

            // Logic based on original JJKUR design
            if (profession == -2.0) {
                entity.getPersistentData().putBoolean("CursedSpirit", true);
                entity.getPersistentData().putBoolean("CurseUser", true);
            } else if (profession == -1.0) {
                // RESTORED: Must be Double 1.0 for compatibility with addon systems
                entity.getPersistentData().putDouble("CursedSpirit", 1.0);
            } else if (profession == 1.0) {
                entity.getPersistentData().putBoolean("CurseUser", true);
            } else {
                entity.getPersistentData().putBoolean("JujutsuSorcerer", true);
            }

            // RESTORED: NonSorcerer defined by lack of technique, not profession
            entity.getPersistentData().putBoolean("NonSorcerer", pVars.PlayerCurseTechnique == 0.0);

            // Sync Attribute based on Technique (Original JJKUR Correction)
            if (entity instanceof LivingEntity _liv && _liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.PROFESSION.get())) {
                _liv.getAttribute((Attribute) JujutsucraftModAttributes.PROFESSION.get()).setBaseValue(pVars.PlayerCurseTechnique);
            }
        }
    }
}
