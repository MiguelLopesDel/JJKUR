package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.MalevolentShrineEntity;
import net.mcreator.jujutsucraft.entity.EntityMalevolentShrineEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.AIMalevolentShrineProcedure;
import net.mcreator.jujutsucraft.procedures.GetEntityFromUUIDProcedure;
import net.mcreator.jujutsucraft.procedures.LogicOwnerExistProcedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(value = AIMalevolentShrineProcedure.class, priority = -10000)
public abstract class AIMalevolentShrineMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity != null) {
            boolean flag = false;
            Entity entity_a = null;
            double x_pos = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            double yaw = 0.0;
            double pitch = 0.0;
            double dis = 0.0;
            double HP = 0.0;
            entity.setDeltaMovement(new Vec3(0.0, Math.min(entity.getDeltaMovement().y(), 0.0), 0.0));
            ServerLevel _level;

            if (entity instanceof EntityMalevolentShrineEntity || entity instanceof MalevolentShrineEntity) {
                if (!entity.getPersistentData().getBoolean("flag_start")) {
                    entity.getPersistentData().putBoolean("flag_start", true);
                    yaw = (double) entity.getYRot();
                    pitch = (double) entity.getXRot();

                    for (int index0 = 0; index0 < 18; ++index0) {
                        x_pos = entity.getX() + Math.cos(Math.toRadians((double) (entity.getYRot() + 90.0F))) * ((double) entity.getBbWidth() - 2.5);
                        y_pos = entity.getY();
                        z_pos = entity.getZ() + Math.sin(Math.toRadians((double) (entity.getYRot() + 90.0F))) * ((double) entity.getBbWidth() - 2.5);

                        for (int index1 = 0; index1 < 100 && !world.getBlockState(BlockPos.containing(x_pos, y_pos - 1.0, z_pos)).canOcclude(); ++index1) {
                            --y_pos;
                        }

                        if (world instanceof ServerLevel _serverLevel) {
                            Entity entityinstance = ((EntityType) JujutsucraftModEntities.ENTITY_SKULL.get()).create(_serverLevel, (CompoundTag) null, (Consumer) null, BlockPos.containing(x_pos, y_pos, z_pos), MobSpawnType.MOB_SUMMONED, false, false);
                            if (entityinstance != null) {
                                entityinstance.setYRot(world.getRandom().nextFloat() * 360.0F);
                                if (!entityinstance.level().isClientSide() && entityinstance.getServer() != null) {
                                    entityinstance.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, entityinstance.position(), entityinstance.getRotationVector(), entityinstance.level() instanceof ServerLevel ? (ServerLevel) entityinstance.level() : null, 4, entityinstance.getName().getString(), entityinstance.getDisplayName(), entityinstance.level().getServer(), entityinstance), "data merge entity @s {Invulnerable:1b}");
                                }

                                entityinstance.setYRot(entity.getYRot());
                                entityinstance.setXRot(0.0F);
                                entityinstance.setYBodyRot(entityinstance.getYRot());
                                entityinstance.setYHeadRot(entityinstance.getYRot());
                                entityinstance.yRotO = entityinstance.getYRot();
                                entityinstance.xRotO = entityinstance.getXRot();
                                if (entityinstance instanceof LivingEntity _entity) {
                                    _entity.yBodyRotO = _entity.getYRot();
                                    _entity.yHeadRotO = _entity.getYRot();
                                }

                                entityinstance.getPersistentData().putDouble("NameRanged_ranged", entity.getPersistentData().getDouble("NameRanged_ranged"));
                                entityinstance.getPersistentData().putString("OWNER_UUID", entity.getPersistentData().getString("OWNER_UUID"));
                                _serverLevel.addFreshEntity(entityinstance);
                            }
                        }

                        entity.setYRot(entity.getYRot() + 20.0F);
                        entity.setXRot(0.0F);
                        entity.setYBodyRot(entity.getYRot());
                        entity.setYHeadRot(entity.getYRot());
                        entity.yRotO = entity.getYRot();
                        entity.xRotO = entity.getXRot();
                        if (entity instanceof LivingEntity _entity) {
                            _entity.yBodyRotO = _entity.getYRot();
                            _entity.yHeadRotO = _entity.getYRot();
                        }
                    }

                    entity.setYRot((float) yaw);
                    entity.setXRot((float) pitch);
                    entity.setYBodyRot(entity.getYRot());
                    entity.setYHeadRot(entity.getYRot());
                    entity.yRotO = entity.getYRot();
                    entity.xRotO = entity.getXRot();
                    if (entity instanceof LivingEntity _entity) {
                        _entity.yBodyRotO = _entity.getYRot();
                        _entity.yHeadRotO = _entity.getYRot();
                    }
                }

                if (world instanceof ServerLevel) {
                    _level = (ServerLevel) world;
                    _level.getServer().getCommands().performPrefixedCommand((new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), (Entity) null)).withSuppressedOutput(), "particle dust 0.251 0.000 0.000 4 ~ ~ ~ 4 0 4 1 30 force");
                }
            }

            flag = false;
            if (entity.getPersistentData().getDouble("NameRanged_ranged") != 0.0 && LogicOwnerExistProcedure.execute(world, entity)) {
                entity_a = GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().getString("OWNER_UUID"));
                if (entity_a != null && entity.getPersistentData().getDouble("NameRanged_ranged") == entity_a.getPersistentData().getDouble("NameRanged")) {
                    flag = true;
                    if (!(entity_a instanceof LivingEntity _livEnt && _livEnt.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) && entity_a.getPersistentData().getDouble("select") == 0.0) {
                        entity.getPersistentData().putBoolean("flag", true);
                    }

                    if ((entity instanceof EntityMalevolentShrineEntity || entity instanceof MalevolentShrineEntity) && entity_a.getPersistentData().getDouble("brokenBrain") >= 1.0 && entity_a.getPersistentData().getDouble("cnt1") >= 45.0) {
                        if (world instanceof ServerLevel _serverLevel) {
                            _serverLevel.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 100, 1.0, 1.0, 1.0, 0.5);
                            _serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 100, 1.0, 1.0, 1.0, 0.5);
                        }

                        if (!entity.getPersistentData().getBoolean("flag_a")) {
                            entity.getPersistentData().putBoolean("flag_a", true);
                            if (world instanceof Level _level2) {
                                if (!_level2.isClientSide()) {
                                    _level2.playSound((Player) null, BlockPos.containing(x, y, z), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:stone_crash")), SoundSource.NEUTRAL, 2.0F, 1.0F);
                                    _level2.playSound((Player) null, BlockPos.containing(x, y, z), (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.end_gateway.spawn")), SoundSource.NEUTRAL, 2.0F, 0.8F);
                                } else {
                                    _level2.playLocalSound(x, y, z, (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:stone_crash")), SoundSource.NEUTRAL, 2.0F, 1.0F, false);
                                    _level2.playLocalSound(x, y, z, (SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.end_gateway.spawn")), SoundSource.NEUTRAL, 2.0F, 0.8F, false);
                                }
                            }
                        }
                    }
                }
            }

            if (!flag || entity.getPersistentData().getDouble("skill") == 1.0) {
                entity.getPersistentData().putBoolean("flag", true);
            }

            if (entity.getPersistentData().getBoolean("flag") && !entity.level().isClientSide()) {
                entity.discard();
            }
        }
    }
}
