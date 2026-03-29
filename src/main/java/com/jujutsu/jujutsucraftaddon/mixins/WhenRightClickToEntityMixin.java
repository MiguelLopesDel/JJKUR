package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.entity.*;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.ReturnInsideItemProcedure;
import net.mcreator.jujutsucraft.procedures.WhenRightClickToEntityProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WhenRightClickToEntityProcedure.class, priority = -10000)
public abstract class WhenRightClickToEntityMixin {

    /**
     * @author Satushi
     * @reason Refactored for v43. Handles Sukuna body transfer with automatic equipment (JJKUR Feature).
     */
    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private static void execute(Event event, LevelAccessor world, Entity entity, Entity sourceentity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null || sourceentity == null) return;

        double T1 = 0.0;
        double T2 = 0.0;
        boolean rightClicked = false;
        JujutsucraftModVariables.PlayerVariables pVars = null;

        if (sourceentity instanceof Player _player) {
            pVars = _player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
            if (pVars != null) {
                T1 = pVars.PlayerCurseTechnique;
                T2 = pVars.PlayerCurseTechnique2;
            }
        }

        // 1. Sukuna Body Transfer Logic (Finger use)
        if (sourceentity instanceof LivingEntity _livSource && _livSource.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get()) 
            && ReturnInsideItemProcedure.execute(sourceentity).getItem() == JujutsucraftModItems.SUKUNA_FINGER.get()) {
            
            Entity spawned = null;
            if (!(entity instanceof ItadoriYujiEntity) && !(entity instanceof ItadoriYujiShibuyaEntity)) {
                if (entity instanceof FushiguroMegumiEntity || entity instanceof FushiguroMegumiShibuyaEntity) {
                    rightClicked = true;
                    if (world instanceof ServerLevel _level) {
                        spawned = ((EntityType) JujutsucraftModEntities.SUKUNA_FUSHIGURO.get()).spawn(_level, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), MobSpawnType.MOB_SUMMONED);
                        if (spawned instanceof LivingEntity _livSpawned) {
                            // Addon Feature: Automatic Equipment
                            _livSpawned.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JujutsucraftModItems.HAIR_FUSHIGURO_MEGUMI_HELMET.get()));
                            _livSpawned.setItemSlot(EquipmentSlot.CHEST, new ItemStack(JujutsucraftModItems.UNIFORM_NORMAL_CHESTPLATE.get()));
                            _livSpawned.setItemSlot(EquipmentSlot.LEGS, new ItemStack(JujutsucraftModItems.UNIFORM_NORMAL_LEGGINGS.get()));
                            _livSpawned.setItemSlot(EquipmentSlot.FEET, new ItemStack(JujutsucraftModItems.UNIFORM_NORMAL_BOOTS.get()));
                        }
                    }
                }
            } else {
                rightClicked = true;
                if (world instanceof ServerLevel _level) {
                    spawned = ((EntityType) JujutsucraftModEntities.SUKUNA.get()).spawn(_level, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), MobSpawnType.MOB_SUMMONED);
                }
            }

            if (rightClicked) {
                if (spawned != null) {
                    spawned.setYRot(entity.getYRot());
                    spawned.setYBodyRot(entity.getYRot());
                    spawned.setYHeadRot(entity.getYRot());
                    spawned.setXRot(entity.getXRot());
                }
                
                _livSource.swing(InteractionHand.MAIN_HAND, true);
                _livSource.removeEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get());

                if (pVars != null) {
                    pVars.BodyItem = ItemStack.EMPTY;
                    pVars.syncPlayerVariables(sourceentity);
                }
                
                if (!entity.level().isClientSide()) entity.discard();
                return; // End execution after transfer (v43 Standard)
            }
        }

        // 2. Active Effect Checks (v43 Logic - Blocking touch skills)
        if (sourceentity instanceof LivingEntity _livS) {
            if (_livS.hasEffect((MobEffect) JujutsucraftModMobEffects.UNSTABLE.get()) ||
                _livS.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_AMPLIFICATION.get()) ||
                _livS.hasEffect((MobEffect) JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get())) {
                return;
            }
        }

        // 3. Technique Interaction Logic
        if (!(entity instanceof DomainExpansionEntityEntity)) {
            if (entity instanceof LivingEntity _livE && _livE.hasEffect((MobEffect) JujutsucraftModMobEffects.INFINITY_EFFECT.get())) {
                return;
            }

            // Projection Sorcery Touch (ID 19)
            if ((T1 == 19.0 || T2 == 19.0) && !(entity instanceof FrameEntity) && !(entity instanceof EntityProjectionSorceryEntity) && entity.getPersistentData().getDouble("select") == 0.0) {
                if (!(entity instanceof LivingEntity _livE && _livE.hasEffect((MobEffect) JujutsucraftModMobEffects.PROJECTION_SORCERY.get()))) {
                    if (pVars != null && (pVars.PlayerCursePower + pVars.PlayerCursePowerChange >= 40.0)) {
                        pVars.PlayerCursePowerChange -= 40.0;
                        pVars.syncPlayerVariables(sourceentity);
                        if (entity instanceof LivingEntity _livE2 && !world.isClientSide()) {
                            _livE2.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.PROJECTION_SORCERY.get(), 50, 0, false, false));
                        }
                        if (sourceentity instanceof LivingEntity _livS) _livS.swing(InteractionHand.MAIN_HAND, true);
                    }
                }
            }

            // Ice Formation Touch (ID 24)
            if ((T1 == 24.0 || T2 == 24.0) && entity.getPercentFrozen() * 100.0F < 5.0F) {
                if (pVars != null && (pVars.PlayerCursePower + pVars.PlayerCursePowerChange >= 40.0)) {
                    pVars.PlayerCursePowerChange -= 40.0;
                    pVars.syncPlayerVariables(sourceentity);
                    
                    if (world instanceof Level _level) {
                        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:ice_generate"));
                        if (!_level.isClientSide()) {
                            _level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), sound, SoundSource.NEUTRAL, 0.5F, 1.0F);
                        } else {
                            _level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.NEUTRAL, 0.5F, 1.0F, false);
                        }
                    }

                    if (entity instanceof LivingEntity _livE && !world.isClientSide()) {
                        _livE.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 5, false, false));
                        _livE.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, false));
                    }

                    entity.setTicksFrozen(100);
                    if (sourceentity instanceof LivingEntity _livS) _livS.swing(InteractionHand.MAIN_HAND, true);
                }
            }
        }
    }
}
