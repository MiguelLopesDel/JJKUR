package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.TechniqueMasteryProgression;
import net.mcreator.jujutsucraft.procedures.CooldownTimeEffectExpiresProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CooldownTimeEffectExpiresProcedure.class, priority = -10000)
public class CooldownTimeEffectExpiresMixin {
    @Inject(method = "execute", at = @At("TAIL"), remap = false)
    private static void execute(Entity entity, CallbackInfo ci) {
        if (entity != null) {
            LevelAccessor world = entity.level();
            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();

            TechniqueMasteryProgression.execute(world, x, y, z, entity);
        }
    }
}
