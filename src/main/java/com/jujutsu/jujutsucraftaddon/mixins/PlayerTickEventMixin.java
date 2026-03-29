package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.PartialRikaEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.PlayerTickEventProcedure;
import net.mcreator.jujutsucraft.procedures.PlayerTickSecondTechniqueProcedure;
import net.mcreator.jujutsucraft.procedures.WhenPlayerActiveTickInfinityProcedure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(value = PlayerTickEventProcedure.class, priority = -10000)
public abstract class PlayerTickEventMixin {

    /**
     * @author Satushi
     * @reason Refactored for v43 energy optimization (10-tick interval) while preserving JJKUR custom Six Eyes and animation balance.
     */
    @Inject(at = @At("HEAD"), method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V", remap = false, cancellable = true)
    private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity == null) return;

        // Skip start phase to avoid duplicate execution
        if (event instanceof PlayerTickEvent _playerTickEvent && _playerTickEvent.phase != Phase.END) {
            return;
        }

        ci.cancel();

        ResourceLocation entityTypeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityTypeKey != null && entityTypeKey.toString().startsWith("jujutsucraft") && !(entity instanceof PartialRikaEntity)) {
            return;
        }

        if (entity.isAlive()) {
            // 1. Animation Damage Logic (JJKUR Balance)
            if (entity instanceof LivingEntity _liv) {
                double animationValue = 0.0;
                if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                    animationValue = _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).getBaseValue();
                }

                if (animationValue != 0.0) {
                    boolean protect = _liv.hasEffect(JujutsucraftaddonModMobEffects.MURASAKI_EFFECT.get()) || 
                                     _liv.hasEffect(JujutsucraftaddonModMobEffects.WORLD_CUT.get());
                    
                    if (!protect) {
                        entity.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("jujutsucraft:start_animation")))), 1.0F);
                    }
                }
            }

            // 2. Energy Processing (v43 10-tick Optimization)
            if (entity instanceof ServerPlayer _serverPlayer && _serverPlayer.tickCount % 10 == 0) {
                JujutsucraftModVariables.PlayerVariables playerVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
                
                if (playerVars.PlayerCurseTechnique != 0.0) {
                    if (_serverPlayer.isCreative()) {
                        if (playerVars.PlayerCursePower < playerVars.PlayerCursePowerMAX || playerVars.PlayerCursePowerChange != 0.0) {
                            playerVars.PlayerCursePower = playerVars.PlayerCursePowerMAX;
                            playerVars.PlayerCursePowerChange = 0.0;
                            playerVars.syncPlayerVariables(entity);
                        }
                    } else {
                        double currentChange = playerVars.PlayerCursePowerChange;

                        // Cursed Technique Cost Reductions
                        if (currentChange < 0.0) {
                            // Sukuna Passive
                            if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) {
                                currentChange *= 0.5;
                            }

                            // Six Eyes Passive (JJKUR Gamerule Logic)
                            if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.SIX_EYES.get())) {
                                int amp = _liv.getEffect((MobEffect) JujutsucraftModMobEffects.SIX_EYES.get()).getAmplifier();
                                double reductionFactor = (double) world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_SIX_EYES_LEVEL) / 10.0;
                                currentChange *= Math.pow(reductionFactor, (double) (amp + 1));
                            }
                        }

                        // Natural Regeneration
                        double regen = 1.0 + (2.0 + playerVars.PlayerLevel) / 1.1 * 0.2;
                        if (entity instanceof LivingEntity _liv && _liv.getHealth() >= _liv.getMaxHealth()) {
                            regen *= 2.0;
                        }
                        if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.ZONE.get())) {
                            regen *= 1.2;
                        }

                        currentChange += Math.round(regen);

                        // Apply Final Changes
                        if (currentChange != 0.0) {
                            playerVars.PlayerCursePower = Math.round(Math.max(Math.min(playerVars.PlayerCursePower + currentChange, playerVars.PlayerCursePowerMAX), 0.0));
                            playerVars.PlayerCursePowerChange = 0.0;
                            playerVars.syncPlayerVariables(entity);

                            // Cursed Spirit Death by CE depletion
                            if (playerVars.PlayerCursePower <= 0.0 && entity.getPersistentData().getBoolean("CursedSpirit")) {
                                if (entity instanceof LivingEntity _liv) {
                                    _liv.removeEffect((MobEffect) JujutsucraftModMobEffects.ZONE.get());
                                    _liv.removeEffect((MobEffect) JujutsucraftModMobEffects.INFINITY_EFFECT.get());
                                }
                                entity.kill();
                            }
                        }
                    }

                    // Insect Advancement Support
                    if (_serverPlayer.getAdvancements().getOrStartProgress(_serverPlayer.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:advancement_insect"))).isDone()) {
                        if (_serverPlayer.getFoodData().getFoodLevel() < 20) {
                            _serverPlayer.getFoodData().setFoodLevel(20);
                        }
                    }
                }
            }

            // 3. Continuous Tick Procs (Every Tick)
            WhenPlayerActiveTickInfinityProcedure.execute(entity);
        }

        // 4. Secondary Technique Processing
        PlayerTickSecondTechniqueProcedure.execute(world, x, y, z, entity);
    }
}
