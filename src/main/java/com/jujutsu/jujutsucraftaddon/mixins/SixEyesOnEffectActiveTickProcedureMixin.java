package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.procedures.DisplayOverlayProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.GojoMoveBlueProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.NoSixEyesGojoProcedure;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.SixEyesOnEffectActiveTickProcedure;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SixEyesOnEffectActiveTickProcedure.class, priority = -10000)
public abstract class SixEyesOnEffectActiveTickProcedureMixin {

    /**
     * @author Satushi
     * @reason Refactored for v43. Injects Gojo-specific conditions, training checks, and vision overlays.
     */
    @Inject(method = "execute", at = @At("TAIL"), remap = false)
    private static void onExecuteTail(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity == null) return;

        // 1. Gojo Restriction Logic (Based on Gamerule)
        if (world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_GOJO_ONLY_SIX_EYES)) {
            if (entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftModMobEffects.SIX_EYES.get())) {
                NoSixEyesGojoProcedure.execute(world, entity);
            }
        }

        // 2. Special Vision Overlay (Gojo Vision)
        DisplayOverlayProcedure.execute(world, entity);

        // 3. Gojo Training & Custom Moves
        if (entity instanceof ServerPlayer _sp && _sp.server != null) {
            JujutsucraftModVariables.PlayerVariables baseVars = _sp.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftModVariables.PlayerVariables());

            // If using Gojo's Technique (ID 2.0)
            if (baseVars.PlayerCurseTechnique2 == 2.0) {
                // Check Part 1 Training
                boolean part1Done = _sp.getAdvancements().getOrStartProgress(_sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:gojo_training_part_1"))).isDone();
                if (part1Done) {
                    GojoMoveBlueProcedure.execute(world, x, y, z, _sp);
                }

                // Check Part 3 Training (Placeholder for future logic)
                boolean part3Done = _sp.getAdvancements().getOrStartProgress(_sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:gojo_training_part_3"))).isDone();
                if (part3Done) {
                    // Logic for training part 3 can be added here
                }
            }
        }
    }
}
