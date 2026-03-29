package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.CongratulationsProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.FingerEatedProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.RejectProcedure;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.SukunaFingerFoodEatenProcedure;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SukunaFingerFoodEatenProcedure.class, priority = -10000)
public abstract class SukunaFingerFoodEatenProcedureMixin {

    /**
     * @author Satushi
     * @reason Refactored for v43. Implements clan-based success rates, rejection debuffs, and technique restrictions.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .orElse(new JujutsucraftModVariables.PlayerVariables());
        JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .orElse(new JujutsucraftaddonModVariables.PlayerVariables());

        // Max 20 fingers check
        if (baseVars.BodyItem.getCount() > 20.0) return;

        boolean isCreative = (entity instanceof Player _p && _p.getAbilities().instabuild);
        
        // 1. Technique Restriction Check ("Nuh Uh")
        boolean allowedTechnique = (baseVars.PlayerCurseTechnique2 == 6.0 || baseVars.PlayerCurseTechnique2 == 21.0 || baseVars.PlayerCurseTechnique2 == 1.0);
        
        if (!allowedTechnique && !isCreative) {
            if (entity instanceof Player _p && !_p.level().isClientSide()) {
                _p.displayClientMessage(Component.literal("Nuh Uh"), false);
            }
            CongratulationsProcedure.execute(world, x, y, z);
            return;
        }

        // 2. First Finger Logic (Clan-based Probability)
        if (baseVars.BodyItem.getCount() == 0.0 && !isCreative) {
            double successChance = 0.25; // Default 1/4 (25%)
            
            if ("Sukuna".equals(addonVars.Clans)) successChance = 0.5; // 1/2
            else if ("Itadori".equals(addonVars.Clans)) successChance = 0.333; // 1/3
            else if ("Perfect Vessel".equals(addonVars.Subrace)) successChance = 0.333; // 1/3
            else if ("The Fallen One".equals(addonVars.Trait)) successChance = 1.0; // Guaranteed

            if (Math.random() < successChance) {
                applySuccess(world, x, y, z, entity);
            } else {
                applyFailure(world, x, y, z, entity);
            }
        } else {
            // Already a vessel or Creative mode
            FingerEatedProcedure.execute(world, x, y, z, entity);
        }
    }

    private static void applySuccess(LevelAccessor world, double x, double y, double z, Entity entity) {
        FingerEatedProcedure.execute(world, x, y, z, entity);
        if (entity instanceof LivingEntity _liv && !_liv.level().isClientSide()) {
            _liv.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.SUKUNA_SPAWNING.get(), 60, 1, false, false));
        }
    }

    private static void applyFailure(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity instanceof LivingEntity _liv && !_liv.level().isClientSide()) {
            _liv.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.HAHAHAHA.get(), 120, 254, false, false));
            _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 254, false, false));
            _liv.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 254, false, false));
        }
        RejectProcedure.execute(world, x, y, z, entity);
    }
}
