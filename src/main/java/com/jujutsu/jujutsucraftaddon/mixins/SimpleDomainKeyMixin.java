package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.SimpleAnimProcedure;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
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

        double strength = (entity instanceof LivingEntity _liv && _liv.hasEffect(MobEffects.DAMAGE_BOOST))
                ? _liv.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 2.0 : 1.0;

        double initialCost = 50.0;
        boolean isCreative = isCreativeMode(entity);

        if (entity instanceof LivingEntity living && living.hasEffect(JujutsucraftModMobEffects.SIX_EYES.get())) {
            int amp = living.getEffect(JujutsucraftModMobEffects.SIX_EYES.get()).getAmplifier();
            initialCost = Math.round(initialCost * Math.pow(0.5, amp + 1));
        }

        final double finalCost = initialCost;
        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(baseVars -> {
            boolean fallingCapable = entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:can_use_falling_blossom_emotion")));
            boolean simpleCapable = entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:can_use_simple_domain")));

            boolean canUseFalling = false;
            if (fallingCapable) {
                if (simpleCapable) {
                    if (entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_SIMPLE_DOMAIN.get())) {
                        int simpleAmp = _liv.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) ?
                                _liv.getEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).getAmplifier() : 0;
                        canUseFalling = simpleAmp <= 0;
                    }
                } else {
                    canUseFalling = true;
                }
            }

            if (!canUseFalling && entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get())) {
                canUseFalling = true;
            }

            boolean wantsFalling = false;
            if (entity instanceof ServerPlayer sp) {
                if (sp.isShiftKeyDown()) {
                    var adv = sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:mastery_falling_blossom_emotion"));
                    if (adv != null && sp.getAdvancements().getOrStartProgress(adv).isDone() && baseVars.PlayerCursePowerFormer > 50.0) {
                        wantsFalling = true;
                    }
                }
            } else if (!(entity instanceof Player) && canUseFalling) {
                wantsFalling = true;
            }

            if (!wantsFalling) {
                handleSimpleDomain(world, x, y, z, entity, baseVars, finalCost, isCreative, strength);
            } else {
                handleFallingBlossom(world, x, y, z, entity, strength);
            }
        });
    }

    private static void handleSimpleDomain(LevelAccessor world, double x, double y, double z, Entity entity, JujutsucraftModVariables.PlayerVariables baseVars, double cost, boolean isCreative, double strength) {
        if (entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()) && _liv.getEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get()).getAmplifier() > 0) {
            _liv.removeEffect(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get());
            if (entity instanceof Player _player && !entity.level().isClientSide()) {
                _player.displayClientMessage(Component.literal(Component.translatable("effect.simple_domain").getString() + ": false"), false);
            }
            playClickSound(entity);
            return;
        }

        if (entity instanceof LivingEntity _liv && (_liv.hasEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get()) || _liv.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME_SIMPLE_DOMAIN.get()))) {
            return;
        }

        if (LogicSimpleDomainProcedure.execute()) {
            boolean on = false;
            if (!(entity instanceof Player)) {
                on = entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:can_use_simple_domain")));
            } else if (entity instanceof ServerPlayer sp) {
                var adv = sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:mastery_simple_domain"));
                boolean mastery = adv != null && sp.getAdvancements().getOrStartProgress(adv).isDone();

                if (mastery && baseVars.PlayerCursePowerFormer > 50.0) {
                    if (baseVars.PlayerCursePower >= cost || isCreative) {
                        if (!isCreative) {
                            baseVars.PlayerCursePower -= cost;
                            baseVars.syncPlayerVariables(entity);
                        }
                        on = true;
                    } else if (!sp.level().isClientSide()) {
                        sp.displayClientMessage(Component.literal(Component.translatable("jujutsu.message.dont_use").getString()), false);
                    }
                } else if (!sp.level().isClientSide()) {
                    sp.displayClientMessage(Component.literal(Component.translatable("jujutsu.message.not_mastered").getString()), false);
                }
                playClickSound(entity);
            }

            if (on && entity instanceof LivingEntity _liv) {
                if (!entity.level().isClientSide()) {
                    _liv.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.SIMPLE_DOMAIN.get(), (int) (100.0 + strength * 20.0), (int) strength, true, true));
                    _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 30, false, false));
                }
                if (entity instanceof Player _player && !entity.level().isClientSide()) {
                    _player.displayClientMessage(Component.literal(Component.translatable("effect.simple_domain").getString() + ": true"), false);
                }

                var anim1 = JujutsucraftModAttributes.ANIMATION_1.get();
                var anim2 = JujutsucraftModAttributes.ANIMATION_2.get();

                if (_liv.getAttributes().hasAttribute(anim1)) {
                    _liv.getAttribute(anim1).setBaseValue(-16.0);
                }

                double num1 = baseVars.PlayerCurseTechnique;
                double num2 = baseVars.PlayerCurseTechnique2;
                boolean hwb = entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:can_use_hollow_wicker_basket"))) ||
                        _liv.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get()) ||
                        (entity instanceof Player && (num1 == TechniqueIDs.SUKUNA || num2 == TechniqueIDs.SUKUNA ||
                                num1 == TechniqueIDs.KASHIMO || num2 == TechniqueIDs.KASHIMO ||
                                num1 == TechniqueIDs.RYU || num2 == TechniqueIDs.RYU ||
                                num1 == TechniqueIDs.URAUME || num2 == TechniqueIDs.URAUME));

                if (_liv.getAttributes().hasAttribute(anim2)) {
                    _liv.getAttribute(anim2).setBaseValue(hwb ? 1.0 : 0.0);
                }
                PlayAnimationProcedure.execute(world, entity);
            }
        }
    }

    private static void handleFallingBlossom(LevelAccessor world, double x, double y, double z, Entity entity, double strength) {
        boolean on = true;
        if (entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get())) {
            on = false;
            KeySimpleDomainOnKeyReleasedProcedure.execute(entity);
        }

        if (on && entity instanceof LivingEntity _liv) {
            if (!entity.level().isClientSide()) {
                _liv.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.FALLING_BLOSSOM_EMOTION.get(), Integer.MAX_VALUE, (int) strength, true, true));
                _liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 30, false, false));
            }
            if (entity instanceof Player _player && !entity.level().isClientSide()) {
                _player.displayClientMessage(Component.literal("§l" + Component.translatable("effect.jujutsucraft.falling_blossom_emotion").getString()), false);
            }

            if (world instanceof Level _level) {
                SoundEvent frameSound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:frame_set"));
                if (frameSound != null) {
                    if (!_level.isClientSide()) {
                        _level.playSound(null, BlockPos.containing(x, y, z), frameSound, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    } else {
                        _level.playLocalSound(x, y, z, frameSound, SoundSource.NEUTRAL, 1.0F, 1.0F, false);
                    }
                }
            }
        }
    }

    private static boolean isCreativeMode(Entity entity) {
        if (entity instanceof ServerPlayer _sp) {
            return _sp.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
        } else if (entity.level().isClientSide() && entity instanceof Player _p) {
            var info = Minecraft.getInstance().getConnection().getPlayerInfo(_p.getGameProfile().getId());
            return info != null && info.getGameMode() == GameType.CREATIVE;
        }
        return false;
    }

    private static void playClickSound(Entity entity) {
        if (!entity.level().isClientSide() && entity.getServer() != null) {
            CommandSourceStack stack = new CommandSourceStack(
                    CommandSource.NULL, entity.position(), entity.getRotationVector(),
                    (ServerLevel) entity.level(), 4, entity.getName().getString(), entity.getDisplayName(),
                    entity.level().getServer(), entity
            );
            entity.getServer().getCommands().performPrefixedCommand(stack, "playsound ui.button.click master @s");
        }
    }
}
