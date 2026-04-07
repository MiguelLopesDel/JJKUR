package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.JJKURSukunaAIBuff;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AISukunaProcedure.class, priority = -10000)
public abstract class SukunaAIBuffMixin {

    /**
     * @author Satushi / FULL RESTORATION
     * @reason Refactored for v43. Restored missing Meteor Passenger logic, effect clearing, and exact probabilities.
     */
    @Inject(method = "execute", at = @At("HEAD"), remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        JJKURSukunaAIBuff.onExecute(world, x, y, z, entity, ci);
    }
}
