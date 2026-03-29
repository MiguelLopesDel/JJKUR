package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.procedures.SpawnLevel1Procedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = SpawnLevel1Procedure.class, priority = -10000)
public abstract class MobSpawning1Mixin {

    @ModifyConstant(method = "execute", constant = @Constant(doubleValue = 1.0), remap = false)
    private static double modifyMobSpawnChance(double constant) {
        return constant / 2.0;
    }
}
