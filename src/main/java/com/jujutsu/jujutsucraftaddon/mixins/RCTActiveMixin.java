package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.RctOutputProcedure;
import net.mcreator.jujutsucraft.procedures.ReverseCursedTechniqueOnEffectActiveTickProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ReverseCursedTechniqueOnEffectActiveTickProcedure.class, priority = -10000)
public abstract class RCTActiveMixin {

    /**
     * @author Satushi
     * @reason Modifies RCT Fatigue rate via Gamerule and Healer profession. Also triggers custom Addon RCT output logic.
     */
    @ModifyConstant(method = "execute", constant = @Constant(intValue = 20), remap = false)
    private static int modifyFatigueIncrement(int value, LevelAccessor world, double x, double y, double z, Entity entity) {
        if (world != null && entity != null) {
            int customFatigue = world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_FATIGUE_RATE);
            
            // Healer Profession Bonus: 50% less fatigue increment
            JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftaddonModVariables.PlayerVariables());
            
            if ("Healer".equals(addonVars.Profession)) {
                return customFatigue / 2;
            }
            return customFatigue;
        }
        return value;
    }

    @Inject(method = "execute", at = @At("HEAD"), remap = false)
    private static void onExecuteHead(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity != null) {
            JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftaddonModVariables.PlayerVariables());
            
            if (addonVars.RCTOutputActive) {
                RctOutputProcedure.execute(world, entity);
            }
        }
    }
}
