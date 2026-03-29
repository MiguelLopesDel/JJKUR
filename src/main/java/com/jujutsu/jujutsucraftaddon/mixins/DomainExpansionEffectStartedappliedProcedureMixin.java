package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.entity.FushiguroMegumiShibuyaEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.DomainExpansionEffectStartedappliedProcedure;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = DomainExpansionEffectStartedappliedProcedure.class, priority = -10000)
public abstract class DomainExpansionEffectStartedappliedProcedureMixin {

    /**
     * @author Satushi
     * @reason Insert Domain Expansion Logic for Barrierless and Other Domains Type
     */
    @Inject(
            method = "execute",
            at = @At("HEAD"),
            remap = false
    )
    private static void execute(Entity entity, CallbackInfo ci) {
        if (entity == null) {
            return;
        }

        LevelAccessor world = entity.level();
        if (entity instanceof FushiguroMegumiShibuyaEntity) {
            if (Math.random() < (1.0 / 70.0)) {
                if (world instanceof ServerLevel _level) {
                    Entity entityToSpawn = JujutsucraftModEntities.FUSHIGURO_TOJI_BUG.get().spawn(_level, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), MobSpawnType.MOB_SUMMONED);
                    if (entityToSpawn != null) {
                        entityToSpawn.setDeltaMovement(0, 0, 0);
                    }
                }
            }
        }

        JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());

        if (addonVars.BarrierlessCount <= 199) {
            double _setval = addonVars.BarrierlessCount + 1;
            entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.BarrierlessCount = _setval;
                capability.syncPlayerVariables(entity);
            });
        } else if (entity instanceof ServerPlayer _plr0) {
            Advancement _adv = _plr0.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:barrierless_domain_perfected"));
            if (_adv != null) {
                AdvancementProgress _ap = _plr0.getAdvancements().getOrStartProgress(_adv);
                if (!_ap.isDone()) {
                    for (String criteria : _ap.getRemainingCriteria()) {
                        _plr0.getAdvancements().award(_adv, criteria);
                    }
                }
            }
        }

        if (addonVars.OstVariable == 0) {
            JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
            if (baseVars.PlayerCurseTechnique2 == 20) {
                if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
                    _entity.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.HOUR_CINDERELLA.get(), 100, 1, false, false));
                }
            }
        }
    }
}
