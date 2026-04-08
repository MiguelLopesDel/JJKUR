package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class JJKURDomainExpansionEffectExpiresProcedure {
    public static void onExecute(Entity entity) {
        if (entity != null) {
            if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
                if (!(_entity.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()))) {
                    _entity.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.DOMAIN_TIME.get(), 40, 1, false, false));
                }
            }

            if ((entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables())).BurnOutRCT) {
                BurnoutKeyOnKeyPressedProcedure.execute(entity);
            }
        }
    }
}
