package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.mcreator.jujutsucraft.entity.EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
import net.mcreator.jujutsucraft.entity.MergedBeastAgitoEntity;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.Comparator;
import java.util.List;

public class AnimationKeybindOnKeyPressedProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        CompoundTag persistentData = entity.getPersistentData();
        double cursePowerAmount = persistentData.getDouble("NBT_CursePowerAmount");

        if (cursePowerAmount == 0) {
            ItemStack mainHandItem = (entity instanceof LivingEntity _livEnt) ? _livEnt.getMainHandItem() : ItemStack.EMPTY;
            ResourceLocation itemRegistryName = ForgeRegistries.ITEMS.getKey(mainHandItem.getItem());

            if (itemRegistryName != null && itemRegistryName.toString().equals("jujutsucraft:split_soul_katana")) {
                if (!entity.isShiftKeyDown() && !entity.isSprinting() && (!(entity instanceof LivingEntity _livEnt5) || !_livEnt5.hasEffect(MobEffects.MOVEMENT_SLOWDOWN))) {
                    if (entity.onGround() && world.isClientSide() && entity instanceof AbstractClientPlayer player) {
                        var animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player).get(new ResourceLocation("jujutsucraftaddon", "player_animation"));
                        if (animation != null && !animation.isActive()) {
                            animation.setAnimation(new KeyframeAnimationPlayer(PlayerAnimationRegistry.getAnimation(new ResourceLocation("jujutsucraftaddon", "idletoji"))));
                        }
                    }
                }
            }
        }

        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
            if (vars.PlayerCurseTechnique2 == TechniqueIDs.GOJO) {
                if (entity instanceof ServerPlayer _plr && _plr.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:gojo_training_part_3")) != null) {
                    if (_plr.getAdvancements().getOrStartProgress(_plr.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:gojo_training_part_3"))).isDone()) {
                        if (entity.isShiftKeyDown() && (!(entity instanceof LivingEntity _livEnt11) || !_livEnt11.hasEffect(JujutsucraftaddonModMobEffects.DASH_COOLDOWN.get()))) {
                            Vec3 eyePos = entity.getEyePosition(1f);
                            Vec3 lookVec = entity.getViewVector(1f).scale(20);
                            BlockPos targetPos = entity.level().clip(new ClipContext(eyePos, eyePos.add(lookVec), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity)).getBlockPos();

                            double targetX = targetPos.getX();
                            double targetY = targetPos.getY() + 1;
                            double targetZ = targetPos.getZ();

                            entity.teleportTo(targetX, targetY, targetZ);
                            if (entity instanceof ServerPlayer _serverPlayer) {
                                _serverPlayer.connection.teleport(targetX, targetY, targetZ, entity.getYRot(), entity.getXRot());
                            }

                            if (entity instanceof LivingEntity _living && !world.isClientSide()) {
                                _living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.DASH_COOLDOWN.get(), 80, 1, false, false));
                            }

                            if (!world.isClientSide() && entity.getServer() != null) {
                                entity.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), world instanceof ServerLevel ? (ServerLevel) world : null, 4,
                                        entity.getName().getString(), entity.getDisplayName(), entity.getServer(), entity), "execute as @s run effect clear @s jujutsucraft:cursed_technique");
                            }
                        }
                    }
                }
            }

            if (vars.PlayerCurseTechnique2 == TechniqueIDs.MEGUMI) {
                entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                    if (addonVars.InfusedDomain) {
                        double friendNum = persistentData.getDouble("friend_num");
                        Vec3 center = new Vec3(x, y, z);
                        List<Entity> entitiesFound = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(50.0), e -> true).stream()
                                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).toList();

                        for (Entity iterator : entitiesFound) {
                            if (iterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("jujutsucraft:ten_shadows_technique"))) &&
                                    iterator.getPersistentData().getDouble("friend_num") == friendNum) {

                                iterator.teleportTo(entity.getX(), entity.getY() + 1, entity.getZ());
                                if (iterator instanceof ServerPlayer _serverPlayer) {
                                    _serverPlayer.connection.teleport(entity.getX(), entity.getY() + 1, entity.getZ(), iterator.getYRot(), iterator.getXRot());
                                }

                                if (entity.isShiftKeyDown()) {
                                    if (iterator instanceof LivingEntity _living && !world.isClientSide()) {
                                        _living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, -1, 1, false, false));
                                        _living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, -1, 254, false, false));
                                    }
                                    if (iterator instanceof GeoEntity) {
                                        if (iterator instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity mahoraga) {
                                            mahoraga.setAnimation("guard");
                                        } else if (iterator instanceof MergedBeastAgitoEntity agito) {
                                            agito.setAnimation("guard");
                                        }
                                        iterator.setShiftKeyDown(true);
                                    }
                                    if (entity instanceof Player _player && !world.isClientSide()) {
                                        _player.displayClientMessage(Component.literal("Defense Mode"), false);
                                    }
                                } else {
                                    if (iterator instanceof LivingEntity _living) {
                                        _living.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                                        _living.removeEffect(MobEffects.BLINDNESS);
                                    }
                                    if (iterator instanceof GeoEntity) {
                                        iterator.setShiftKeyDown(false);
                                        iterator.setSprinting(true);
                                    }
                                    if (entity instanceof Player _player && !world.isClientSide()) {
                                        _player.displayClientMessage(Component.literal("Attack Mode"), false);
                                    }
                                }
                            }
                        }
                    }
                });
            }
        });
    }
}
