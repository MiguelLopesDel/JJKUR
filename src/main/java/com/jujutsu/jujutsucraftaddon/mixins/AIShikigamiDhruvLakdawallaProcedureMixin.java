package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.entity.ShikigamiHeterocephalusGlaberEntity;
import net.mcreator.jujutsucraft.entity.ShikigamiPterosaurEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModBlocks;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AIShikigamiDhruvLakdawallaProcedure.class, priority = -10000)
public abstract class AIShikigamiDhruvLakdawallaProcedureMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity != null) {
            boolean logicStart = false;
            boolean placed = false;
            Entity owner_uuid = null;
            double distance = 0.0;
            double NUM2 = 0.0;
            double rnd = 0.0;
            double x_pos = 0.0;
            double tick = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            double NUM1 = 0.0;
            double level = 0.0;

            if (entity.isAlive()) {
                if (entity instanceof LivingEntity _livEntity) {
                    if (!_livEntity.level().isClientSide()) {
                        _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_BACK_STEP.get(), 10, 9, false, false));
                    }
                }

                if (!entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo")))) {
                    AIActiveProcedure.execute(world, x, y, z, entity);
                    if (entity instanceof ShikigamiPterosaurEntity) {
                        AIActiveFlyingProcedure.execute(world, entity);
                    } else if (entity instanceof ShikigamiHeterocephalusGlaberEntity _hetero) {
                        if (_hetero.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.SIZE.get())) {
                            double currentSize = _hetero.getAttribute((Attribute) JujutsucraftModAttributes.SIZE.get()).getBaseValue();
                            if (currentSize < 6.0) {
                                _hetero.getAttribute((Attribute) JujutsucraftModAttributes.SIZE.get()).setBaseValue(Math.min(currentSize + 0.1, 6.0));
                            }
                        }
                        _hetero.setMaxUpStep((float) ((double) _hetero.getStepHeight() + 0.01));
                    }

                    if (entity instanceof LivingEntity _livEntity) {
                        NUM1 = (double) (2L + Math.round(entity.getPersistentData().getDouble("Strength") * 0.5));
                        double attackDamage = _livEntity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? _livEntity.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() : 0.0;
                        NUM2 = (double) Math.round(Math.floor(Math.min((NUM1 + attackDamage * 3.0) / 4.0, 3.0)));

                        if (!_livEntity.hasEffect(MobEffects.DAMAGE_BOOST)) {
                            if (!_livEntity.level().isClientSide()) {
                                _livEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, (int) NUM1, false, false));
                            }
                        }

                        int currentRes = _livEntity.hasEffect(MobEffects.DAMAGE_RESISTANCE) ? _livEntity.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() : 0;
                        if ((double) currentRes < NUM2) {
                            if (!_livEntity.level().isClientSide()) {
                                _livEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, (int) NUM2, false, false));
                            }
                        }
                    }
                } else if (entity.getPersistentData().getDouble("mode") == 0.0 || entity.getPersistentData().getDouble("skill") <= 0.0) {
                    if (world instanceof ServerLevel _serverLevel) {
                        _serverLevel.sendParticles(ParticleTypes.SQUID_INK, entity.getX(), entity.getY() + (double) entity.getBbHeight() * 0.5, entity.getZ(), 10, 0.25, 0.25, 0.25, 0.0);
                    }
                    if (!entity.level().isClientSide()) {
                        entity.discard();
                    }
                }

                // Restore "mode" logic for Pterosaur (Original Addon IA)
                if (entity.getPersistentData().getDouble("mode") == 1.0) {
                    AttackTackleFlyingProcedure.execute(world, x, y, z, entity);
                    entity.getPersistentData().putDouble("cnt_x", -100.0);
                    if (entity.getPersistentData().getDouble("skill") == 0.0) {
                        if (entity.getPersistentData().getDouble("continue") > 0.0) {
                            entity.getPersistentData().putDouble("continue", entity.getPersistentData().getDouble("continue") - 1.0);
                            entity.getPersistentData().putDouble("skill", 1.0);
                            entity.getPersistentData().putDouble("mode", 1.0);
                        }
                        ResetCounterProcedure.execute(entity);
                    }
                } else if ((entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null) instanceof LivingEntity) {
                    entity.getPersistentData().putDouble("cnt_x", entity.getPersistentData().getDouble("cnt_x") + 1.0);
                    if (entity.getPersistentData().getDouble("cnt_x") > 10.0 && entity.getPersistentData().getDouble("skill") == 0.0) {
                        entity.getPersistentData().putDouble("cnt_x", 0.0);
                        ResetCounterProcedure.execute(entity);
                        distance = GetDistanceProcedure.execute(entity);
                        // Restore LogicStart call (Original Addon IA)
                        logicStart = LogicStartProcedure.execute(entity);

                        if (entity instanceof ShikigamiPterosaurEntity) {
                            entity.getPersistentData().putDouble("cnt_x", -50.0);
                            entity.getPersistentData().putDouble("mode", 1.0); // Restore mode = 1.0
                            entity.getPersistentData().putDouble("skill", 1.0);
                            entity.getPersistentData().putDouble("continue", Math.random() < 0.75 ? 1 : 0);
                            if (entity instanceof LivingEntity _livEntity && !_livEntity.level().isClientSide()) {
                                _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
                            }
                        } else if (entity instanceof ShikigamiHeterocephalusGlaberEntity _hetero) {
                            if (!_hetero.hasEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get())) {
                                if (distance < 15.0 && Math.random() < 0.5) {
                                    rnd = 2204.0;
                                    level = 1.0;
                                    tick = 25.0;
                                } else {
                                    CalculateAttackProcedure.execute(world, entity);
                                }
                            } else {
                                CalculateAttackProcedure.execute(world, entity);
                            }
                        } else {
                            CalculateAttackProcedure.execute(world, entity);
                        }

                        if (rnd > 0.0) {
                            entity.getPersistentData().putDouble("skill", (double) Math.round(rnd));
                            if (entity instanceof LivingEntity _livEntity && !_livEntity.level().isClientSide()) {
                                _livEntity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
                                // Restore Level-based Cooldown logic (Original Addon IA)
                                MobEffect effect = (level > 0.0) ? (MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get() : (MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME.get();
                                _livEntity.addEffect(new MobEffectInstance(effect, (int) tick, 0, false, false));
                            }
                        }
                    }
                } else {
                    entity.getPersistentData().putDouble("cnt_x", 0.0);
                }

                if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo")))) {
                    placed = true;
                } else if (entity.getPersistentData().getDouble("skill") > 0.0) {
                    if (GetDistanceNearestEnemyProcedure.execute(world, entity) < 15.0) {
                        placed = true;
                    }
                } else if (entity.getPersistentData().getDouble("skill") == 0.0 && Math.random() < 0.1) {
                    placed = true;
                }

                if (placed) {
                    x_pos = x + (Math.random() - 0.5) * (double) entity.getBbWidth() * 0.5;
                    y_pos = y + Math.random() * (double) entity.getBbWidth() * 0.5;
                    z_pos = z + (Math.random() - 0.5) * (double) entity.getBbWidth() * 0.5;

                    double x_pos2 = x + (Mth.nextInt(RandomSource.create(), -5, 5) - 0.5) * (double) entity.getBbWidth() * 0.5;
                    double y_pos2 = y + (Mth.nextInt(RandomSource.create(), -3, 3)) * (double) entity.getBbWidth() * 0.5;
                    double z_pos2 = z + (Mth.nextInt(RandomSource.create(), -5, 5) - 0.5) * (double) entity.getBbWidth() * 0.5;

                    if (world instanceof ServerLevel _level) {
                        String cmd1 = "setblock ~ ~ ~ jujutsucraft:domain keep";
                        _level.getServer().getCommands().performPrefixedCommand((new CommandSourceStack(CommandSource.NULL, new Vec3(x_pos2, y_pos2, z_pos2), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)).withSuppressedOutput(), cmd1);
                        _level.getServer().getCommands().performPrefixedCommand((new CommandSourceStack(CommandSource.NULL, new Vec3(x_pos, y_pos, z_pos), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)).withSuppressedOutput(), cmd1);
                        _level.getServer().getCommands().performPrefixedCommand((new CommandSourceStack(CommandSource.NULL, new Vec3(x_pos, y_pos, z_pos), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)).withSuppressedOutput(), "setblock ~1 ~ ~ jujutsucraft:domain keep");
                        _level.getServer().getCommands().performPrefixedCommand((new CommandSourceStack(CommandSource.NULL, new Vec3(x_pos, y_pos, z_pos), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)).withSuppressedOutput(), "setblock ~ ~ ~1 jujutsucraft:domain keep");
                        _level.getServer().getCommands().performPrefixedCommand((new CommandSourceStack(CommandSource.NULL, new Vec3(x_pos, y_pos, z_pos), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)).withSuppressedOutput(), "setblock ~ ~1 ~ jujutsucraft:domain keep");
                        _level.getServer().getCommands().performPrefixedCommand((new CommandSourceStack(CommandSource.NULL, new Vec3(x_pos, y_pos, z_pos), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)).withSuppressedOutput(), "setblock ~ ~ ~-1 jujutsucraft:domain keep");
                        _level.getServer().getCommands().performPrefixedCommand((new CommandSourceStack(CommandSource.NULL, new Vec3(x_pos, y_pos, z_pos), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)).withSuppressedOutput(), "setblock ~-1 ~ ~ jujutsucraft:domain keep");
                        _level.getServer().getCommands().performPrefixedCommand((new CommandSourceStack(CommandSource.NULL, new Vec3(x_pos, y_pos, z_pos), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)).withSuppressedOutput(), "setblock ~ ~-1 ~ jujutsucraft:domain keep");
                    }

                    if (world.getBlockState(BlockPos.containing(x_pos, y_pos, z_pos)).getBlock() == JujutsucraftModBlocks.DOMAIN.get()) {
                        String ownerStr = entity.getPersistentData().getString("OWNER_UUID");
                        if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo"))) && GetEntityFromUUIDProcedure.execute(world, ownerStr) instanceof LivingEntity) {
                            if (!world.isClientSide()) {
                                BlockPos _bp = BlockPos.containing(x_pos, y_pos, z_pos);
                                BlockEntity _blockEntity = world.getBlockEntity(_bp);
                                BlockState _bs = world.getBlockState(_bp);
                                if (_blockEntity != null) {
                                    _blockEntity.getPersistentData().putString("OWNER_UUID", ownerStr);
                                }
                                if (world instanceof Level _level) {
                                    _level.sendBlockUpdated(_bp, _bs, _bs, 3);
                                }
                            }
                        } else if (!world.isClientSide()) {
                            BlockPos _bp = BlockPos.containing(x_pos, y_pos, z_pos);
                            BlockEntity _blockEntity = world.getBlockEntity(_bp);
                            BlockState _bs = world.getBlockState(_bp);
                            if (_blockEntity != null) {
                                _blockEntity.getPersistentData().putString("OWNER_UUID", entity.getStringUUID());
                            }
                            if (world instanceof Level _level) {
                                _level.sendBlockUpdated(_bp, _bs, _bs, 3);
                            }
                        }
                    }
                }
            }

            if (entity.getPersistentData().getBoolean("Shikigami")) {
                owner_uuid = GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().getString("OWNER_UUID"));
                if (owner_uuid instanceof LivingEntity) {
                    if (!owner_uuid.isAlive() && !entity.level().isClientSide() && entity.getServer() != null) {
                        entity.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), entity.level() instanceof ServerLevel ? (ServerLevel) entity.level() : null, 4, entity.getName().getString(), entity.getDisplayName(), entity.level().getServer(), entity), "kill @s");
                    }
                } else if (!entity.level().isClientSide() && entity.getServer() != null) {
                    entity.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), entity.level() instanceof ServerLevel ? (ServerLevel) entity.level() : null, 4, entity.getName().getString(), entity.getDisplayName(), entity.level().getServer(), entity), "kill @s");
                }

                if (owner_uuid instanceof LivingEntity _owner) {
                    if (_owner.hasEffect((MobEffect) JujutsucraftModMobEffects.UNSTABLE.get())) {
                        if (!entity.level().isClientSide() && entity.getServer() != null) {
                            entity.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), entity.level() instanceof ServerLevel ? (ServerLevel) entity.level() : null, 4, entity.getName().getString(), entity.getDisplayName(), entity.level().getServer(), entity), "kill @s");
                        }
                        if (!entity.level().isClientSide()) {
                            entity.discard();
                        }
                    }
                }
            }
        }
    }
}
