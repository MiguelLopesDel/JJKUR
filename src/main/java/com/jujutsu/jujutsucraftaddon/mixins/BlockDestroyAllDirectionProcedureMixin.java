package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.JJKURBlockDestroyAllDirectionProcedure;
import net.mcreator.jujutsucraft.procedures.BlockDestroyAllDirectionProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockDestroyAllDirectionProcedure.class, priority = -10000)
public abstract class BlockDestroyAllDirectionProcedureMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void onExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        JJKURBlockDestroyAllDirectionProcedure.execute(world, x, y, z, entity, ci);
    }
}
