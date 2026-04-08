package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class AirJumpRechargeProcedure {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            execute(event.player);
        }
    }

    public static void execute(Entity entity) {
        if (entity == null || !(entity instanceof LivingEntity living)) return;

        if (living.tickCount % 10 == 0 && living.onGround()) {
            boolean isSukuna = living.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get());
            boolean isPerfectPhysic = false;

            if (living.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("forge", "no_curse_power")))) {
                isPerfectPhysic = true;
            }
            if (living.hasEffect(JujutsucraftModMobEffects.PHYSICAL_GIFTED_EFFECT.get())) {
                isPerfectPhysic = true;
            }

            if (isSukuna || isPerfectPhysic) {
                MobEffect doubleJumpEffect = JujutsucraftModMobEffects.DOUBLE_JUMP_EFFECT.get();
                MobEffectInstance currentEffect = living.getEffect(doubleJumpEffect);
                
                int currentAmp = currentEffect != null ? currentEffect.getAmplifier() : -1;
                
                if (currentAmp < 4) {
                    living.addEffect(new MobEffectInstance(doubleJumpEffect, 999999, currentAmp + 1, false, false));
                } else if (currentEffect != null && currentEffect.getDuration() < 100) {
                    living.addEffect(new MobEffectInstance(doubleJumpEffect, 999999, 4, false, false));
                }
            }
        }
    }
}