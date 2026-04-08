package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.JJKURDomainExpansionEffectExpiresProcedure;
import net.mcreator.jujutsucraft.procedures.DomainExpansionEffectExpiresProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DomainExpansionEffectExpiresProcedure.class, priority = -10000)
public abstract class DomainExpansionEffectExpiresProcedureMixin {

    /**
     * @author Sat
     * @reason Give one effect for fix the barrier in the end of domain
     */
    @Inject(method = "execute", at = @At("HEAD"), remap = false)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        JJKURDomainExpansionEffectExpiresProcedure.onExecute(entity);
    }
}
