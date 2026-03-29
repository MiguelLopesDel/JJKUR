package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.SimpleAnimProcedure;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.KeySimpleDomainOnKeyPressedProcedure;
import net.mcreator.jujutsucraft.procedures.KeySimpleDomainOnKeyReleasedProcedure;
import net.mcreator.jujutsucraft.procedures.LogicSimpleDomainProcedure;
import net.mcreator.jujutsucraft.procedures.PlayAnimationProcedure;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeySimpleDomainOnKeyPressedProcedure.class, priority = -10000)
public abstract class SimpleDomainKeyMixin {

    /**
     * @author Satushi / Audit Correction
     * @reason Refactored for clean code and compatibility with JJKUR custom Simple Domain animations.
     * FIXED: Restored specific UI feedback messages and local sound support.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        SimpleAnimProcedure.execute(world, entity);

        double strength = 1.0;
        if (entity instanceof LivingEntity _liv && _liv.hasEffect(MobEffects.DAMAGE_BOOST)) {
            strength = _liv.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 2.0;
        }

        double cost = 50.0;
        boolean isCreative = false;
        JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .orElse(new JujutsucraftModVariables.PlayerVariables());

        if (entity instanceof Player _player) {
            if (_player instanceof ServerPlayer _sp) {
                isCreative = _sp.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
            } else if (_player.level().isClientSide()) {
                isCreative = Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.CREATIVE;
            }

            if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.SIX_EYES.get())) {
                int amp = _liv.getEffect((MobEffect) JujutsucraftModMobEffects.SIX_EYES.get()).getAmplifier();
                cost = Math.round(cost * Math.pow(0.5, (double) (amp + 1)));
            }
        }

        boolean fallingBlossomCapable = entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:can_use_falling_blossom_emotion")));
        boolean simpleDomainCapable = entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:can_use_simple_domain")));
        
        boolean canUseFallingBlossom = false;
        if (fallingBlossomCapable) {
            if (simpleDomainCapable) {
                if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_SIMPLE_DOMAIN.get())) {
                    int simpleDomainAmp = _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) ? 
                                         _liv.getEffect((MobEffect) JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).getAmplifier() : 0;
                    if (simpleDomainAmp > 0) {
                        canUseFallingBlossom = false;
                    } else {
                        canUseFallingBlossom = true;
                    }
                }
            } else {
                canUseFallingBlossom = true;
            }
        }

        if (!canUseFallingBlossom && entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get())) {
            canUseFallingBlossom = true;
        }

        // --- BRANCH DECISION: SIMPLE DOMAIN VS FALLING BLOSSOM ---
        boolean wantsFallingBlossom = false;
        if (entity instanceof Player _player) {
            if (_player.isShiftKeyDown() && _player instanceof ServerPlayer _sp) {
                boolean mastery = _sp.getAdvancements().getOrStartProgress(_sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:mastery_falling_blossom_emotion"))).isDone();
                if (mastery && baseVars.PlayerCursePowerFormer > 50.0) {
                    wantsFallingBlossom = true;
                }
            }
        } else if (canUseFallingBlossom) {
            wantsFallingBlossom = true;
        }

        if (!wantsFallingBlossom) {
            handleSimpleDomain(world, x, y, z, entity, baseVars, cost, isCreative, strength);
        } else {
            handleFallingBlossom(world, x, y, z, entity, strength);
        }
    }

    private static void handleSimpleDomain(LevelAccessor world, double x, double y, double z, Entity entity, JujutsucraftModVariables.PlayerVariables baseVars, double cost, boolean isCreative, double strength) {
        if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) && _liv.getEffect((MobEffect) JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).getAmplifier() > 0) {
            _liv.removeEffect((MobEffect) JujutsucraftModMobEffects.SIMPLE_DOMAIN.get());
            if (entity instanceof Player _player && !_player.level().isClientSide()) {
                _player.displayClientMessage(Component.literal(Component.translatable("effect.simple_domain").getString() + ": false"), false);
            }
            playClickSound(entity);
            return;
        }

        if (entity instanceof LivingEntity _liv && (_liv.hasEffect((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get()) || _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_SIMPLE_DOMAIN.get()))) {
            return;
        }

        if (LogicSimpleDomainProcedure.execute()) {
            boolean on = false;
            if (!(entity instanceof Player)) {
                on = entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:can_use_simple_domain")));
            } else {
                ServerPlayer _sp = (ServerPlayer) entity;
                boolean mastery = _sp.getAdvancements().getOrStartProgress(_sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:mastery_simple_domain"))).isDone();
                
                // CORRECTED: Restored specific UI feedback logic
                if (mastery && baseVars.PlayerCursePowerFormer > 50.0) {
                    if (baseVars.PlayerCursePower >= cost || isCreative) {
                        if (!isCreative) {
                            baseVars.PlayerCursePower -= cost;
                            baseVars.syncPlayerVariables(entity);
                        }
                        on = true;
                    } else if (!_sp.level().isClientSide()) {
                        _sp.displayClientMessage(Component.literal(Component.translatable("jujutsu.message.dont_use").getString()), false);
                    }
                } else if (!_sp.level().isClientSide()) {
                    _sp.displayClientMessage(Component.literal(Component.translatable("jujutsu.message.not_mastered").getString()), false);
                }
                playClickSound(entity);
            }

            if (on && entity instanceof LivingEntity _liv) {
                if (!_liv.level().isClientSide()) {
                    _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.SIMPLE_DOMAIN.get(), (int) (100.0 + strength * 20.0), (int) strength, true, true));
                    _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 30, false, false));
                }
                if (entity instanceof Player _player && !_player.level().isClientSide()) {
                    _player.displayClientMessage(Component.literal(Component.translatable("effect.simple_domain").getString() + ": true"), false);
                }
                if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                    _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(-16.0);
                }

                // Animation Side determination
                double num1 = baseVars.PlayerCurseTechnique;
                double num2 = baseVars.PlayerCurseTechnique2;
                boolean hwb = entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:can_use_hollow_wicker_basket"))) || 
                              _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get()) ||
                              (entity instanceof Player && (num1 == 1.0 || num2 == 1.0 || num1 == 7.0 || num2 == 7.0 || num1 == 12.0 || num2 == 12.0 || num1 == 24.0 || num2 == 24.0));

                if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get())) {
                    _liv.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get()).setBaseValue(hwb ? 1.0 : 0.0);
                }
                PlayAnimationProcedure.execute(world, entity);
            }
        }
    }

    private static void handleFallingBlossom(LevelAccessor world, double x, double y, double z, Entity entity, double strength) {
        boolean on = true;
        if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get())) {
            on = false;
            KeySimpleDomainOnKeyReleasedProcedure.execute(entity);
        }

        if (on && entity instanceof LivingEntity _liv) {
            if (!_liv.level().isClientSide()) {
                _liv.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get(), Integer.MAX_VALUE, (int) strength, true, true));
                _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 30, false, false));
            }
            if (entity instanceof Player _player && !_player.level().isClientSide()) {
                _player.displayClientMessage(Component.literal("§l" + Component.translatable("effect.jujutsucraft.falling_blossom_emotion").getString()), false);
            }
            
            // CORRECTED: Restored Local Sound support
            if (world instanceof Level _level) {
                SoundEvent frameSound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:frame_set"));
                if (!_level.isClientSide()) {
                    _level.playSound(null, BlockPos.containing(x, y, z), frameSound, SoundSource.NEUTRAL, 1.0F, 1.0F);
                } else {
                    _level.playLocalSound(x, y, z, frameSound, SoundSource.NEUTRAL, 1.0F, 1.0F, false);
                }
            }
        }
    }

    private static void playClickSound(Entity entity) {
        if (!entity.level().isClientSide() && entity.getServer() != null) {
            entity.getServer().getCommands().performPrefixedCommand(
                new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), 
                (ServerLevel) entity.level(), 4, entity.getName().getString(), entity.getDisplayName(), 
                entity.level().getServer(), entity), "playsound ui.button.click master @s"
            );
        }
    }
}
