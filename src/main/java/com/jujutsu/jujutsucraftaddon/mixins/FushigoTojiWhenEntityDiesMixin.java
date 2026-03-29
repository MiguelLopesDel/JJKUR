package com.jujutsu.jujutsucraftaddon.mixins;


import net.mcreator.jujutsucraft.entity.FushiguroTojiBugEntity;
import net.mcreator.jujutsucraft.entity.FushiguroTojiEntity;
import net.mcreator.jujutsucraft.procedures.FushiguroTojiEntityDiesProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FushiguroTojiEntityDiesProcedure.class, priority = -10000)
public class FushigoTojiWhenEntityDiesMixin {

    @Inject(method = "execute", at = @At("HEAD"), remap = false)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity instanceof FushiguroTojiEntity toji) {
            if (!(toji.animationprocedure.equals("deathtoji"))) {
                toji.setAnimation("deathtoji");
            }
        } else if (entity instanceof FushiguroTojiBugEntity tojiBug) {
            if (!(tojiBug.animationprocedure.equals("deathtoji"))) {
                tojiBug.setAnimation("deathtoji");
            }
        }
    }
}
