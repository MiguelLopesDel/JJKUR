package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.JJKURStartCursedTechniqueProcedure;
import net.mcreator.jujutsucraft.procedures.StartCursedTechniqueProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StartCursedTechniqueProcedure.class, priority = -10000)
public abstract class StartCursedTechniqueProcedureMixin {

    /**
     * @author Satushi / JJKUR Optimization
     * @reason Move logic to a procedure class to allow hotswapping and prevent IllegalClassLoadError.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void onExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        JJKURStartCursedTechniqueProcedure.execute(world, x, y, z, entity, ci);
    }
}
