package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.JJKURParticleGeneratorProcedure;
import net.mcreator.jujutsucraft.procedures.ParticleGeneratorProcedure;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ParticleGeneratorProcedure.class, remap = false)
public abstract class ParticleGeneratorProcedureMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void execute(LevelAccessor world, double caliber_radius, double count, double inaccuracy, double speed, double x1, double x2, double y1, double y2, double z1, double z2, String id, CallbackInfo ci) {
        JJKURParticleGeneratorProcedure.execute(world, caliber_radius, count, inaccuracy, speed, x1, x2, y1, y2, z1, z2, id, ci);
    }
}