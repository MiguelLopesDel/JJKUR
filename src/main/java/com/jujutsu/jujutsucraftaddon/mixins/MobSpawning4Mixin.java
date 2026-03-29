package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.SpawnLevel3Procedure;
import net.mcreator.jujutsucraft.procedures.SpawnLevel4Procedure;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpawnLevel4Procedure.class, priority = -10000)
public abstract class MobSpawning4Mixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void modifyMobSpawnChance(LevelAccessor world, CallbackInfoReturnable<Boolean> cir) {
        double spawnRateModifier = (double) world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_MOB_SPAWNING_RATE);
        if (spawnRateModifier <= 0) spawnRateModifier = 1.0;

        double num1 = JujutsucraftModVariables.MapVariables.get(world).STRONGEST_PLAYER;
        double chance;

        // Addon Buffed Spawn Chances for Level 4
        if (num1 <= 7.0) {
            chance = 0.01;
        } else if (num1 <= 9.0) {
            chance = 0.05;
        } else if (num1 <= 11.0) {
            chance = 0.15;
        } else if (num1 <= 13.0) {
            chance = 0.25;
        } else {
            chance = 0.5;
        }

        // Addon logic: chance / modifier AND respect Level 3 spawning hierarchy
        boolean result = (Math.random() < (chance / spawnRateModifier)) && SpawnLevel3Procedure.execute(world);
        cir.setReturnValue(result);
    }
}
