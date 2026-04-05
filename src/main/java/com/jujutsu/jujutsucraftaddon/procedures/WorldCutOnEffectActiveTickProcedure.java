package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class WorldCutOnEffectActiveTickProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
            double moveset = addonVars.Moveset;

            if (moveset >= 1 && moveset <= 3) {
                if (addonVars.CustceneDone < 3) {
                    applyAnimationDamage(world, entity);
                    addonVars.CustceneDone++;
                    addonVars.syncPlayerVariables(entity);
                }
            } else if (moveset == 4) {
                WorldSlashVariantsProcedure.execute(world, x, y, z, entity);
            }
        });

        WorldCutEffectStartedappliedProcedure.execute(entity);
    }

    /**
     * @author Satushi / JJKUR
     * @reason Applies technical damage to trigger the base mod's animation system.
     */
    private static void applyAnimationDamage(LevelAccessor world, Entity entity) {
        ResourceKey<DamageType> damageTypeKey = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("jujutsucraft:start_animation"));
        DamageSource damageSource = new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(damageTypeKey));
        entity.hurt(damageSource, 1.0F);
    }
}
