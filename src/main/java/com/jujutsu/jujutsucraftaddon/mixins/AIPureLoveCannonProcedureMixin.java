package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import net.mcreator.jujutsucraft.entity.CrowEntity;
import net.mcreator.jujutsucraft.entity.PureLoveCannonEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

@Mixin(value = AIPureLoveCannonProcedure.class, priority = -10000)
public abstract class AIPureLoveCannonProcedureMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity == null) return;

        if (world instanceof ServerLevel _level) {
            _level.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_FIRE_SPARK.get(), x, y, z, 1, 1.0, 1.0, 1.0, 2.0);
            _level.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_THUNDER_PURPLE.get(), x, y, z, 6, 1.0, 1.0, 1.0, 1.0);
        }

        if (entity instanceof PureLoveCannonEntity _pureLove && (Boolean) _pureLove.getEntityData().get(PureLoveCannonEntity.DATA_move)) {
            entity.getPersistentData().putDouble("cnt1", entity.getPersistentData().getDouble("cnt1") + 1.0);

            if (entity.getPersistentData().getDouble("cnt2") == 0.0) {
                double cnt1 = entity.getPersistentData().getDouble("cnt1");

                if (cnt1 > 12.0) {
                    if (cnt1 == 13.0) {
                        playSound(world, x, y, z, "block.end_gateway.spawn", 2.0F, 0.75F);
                        playSound(world, x, y, z, "block.end_gateway.spawn", 2.0F, 1.0F);
                        if (world instanceof ServerLevel _level) {
                            _level.sendParticles(ParticleTypes.CLOUD, x, y, z, 20, 0.3, 0.3, 0.3, 2.0);
                            _level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 40, 1.0, 1.0, 1.0, 0.5);
                        }
                    }
                    if (world instanceof ServerLevel _level) {
                        _level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 10, 1.0, 1.0, 1.0, 0.5);
                        _level.sendParticles(ParticleTypes.CLOUD, x, y, z, 20, 1.0, 1.0, 1.0, 0.5);
                    }
                }

                double velocity = Math.sqrt(Math.pow(entity.getDeltaMovement().x(), 2.0) + Math.pow(entity.getDeltaMovement().y(), 2.0) + Math.pow(entity.getDeltaMovement().z(), 2.0));
                if ((velocity < 0.5 && cnt1 > 12.0) || cnt1 > 50.0 || !entity.isAlive()) {
                    entity.getPersistentData().putDouble("cnt2", 1.0);
                }

                if (entity.getPersistentData().getDouble("cnt2") > 0.0) {
                    entity.getPersistentData().putDouble("x_pos", entity.getX());
                    entity.getPersistentData().putDouble("y_pos", entity.getY());
                    entity.getPersistentData().putDouble("z_pos", entity.getZ());
                    if (!entity.getPersistentData().getBoolean("flag_start")) {
                        entity.getPersistentData().putBoolean("flag_start", true);
                    }
                }

                if (cnt1 > 10.0) {
                    if (!entity.getPersistentData().getBoolean("Stop")) {
                        entity.setDeltaMovement(new Vec3(entity.getPersistentData().getDouble("x_power") * 0.5, entity.getPersistentData().getDouble("y_power") * 0.5, entity.getPersistentData().getDouble("z_power") * 0.5));
                        BulletDomainHit2Procedure.execute(world, entity);
                    } else {
                        entity.setDeltaMovement(Vec3.ZERO);
                        entity.getPersistentData().putBoolean("Stop", true);
                    }
                } else {
                    entity.setDeltaMovement(Vec3.ZERO);
                }

                // Addon Gamerule Scaling
                double multiplier = (double) world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_RIKA_PURE_LOVE);
                entity.getPersistentData().putDouble("Damage", 52.0);
                entity.getPersistentData().putDouble("Range", 12.0 * multiplier);
                entity.getPersistentData().putDouble("knockback", 2.0);
                entity.getPersistentData().putDouble("effectConfirm", 2.0);
                RangeAttackProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);

            } else {
                entity.setDeltaMovement(Vec3.ZERO);
                entity.getPersistentData().putDouble("cnt2", entity.getPersistentData().getDouble("cnt2") + 1.0);
                double cnt2 = entity.getPersistentData().getDouble("cnt2");
                double x_pos = entity.getPersistentData().getDouble("x_pos");
                double y_pos = entity.getPersistentData().getDouble("y_pos") + (cnt2 - 2.0) * 2.0;
                double z_pos = entity.getPersistentData().getDouble("z_pos");

                if (world instanceof ServerLevel _level) {
                    runCommand(entity, new Vec3(x_pos, y_pos, z_pos), "particle jujutsucraft:particle_thunder_purple ~ ~ ~ 8 8 8 2 150 force");
                    runCommand(entity, new Vec3(x_pos, y_pos, z_pos), "particle minecraft:explosion ~ ~ ~ 8 8 8 1 150 force");
                }

                if (cnt2 < 15.0) {
                    double multiplier = (double) world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_RIKA_PURE_LOVE);
                    entity.getPersistentData().putDouble("Damage", 52.0);
                    entity.getPersistentData().putDouble("Range", 24.0 * multiplier * ReturnEntitySizeProcedure.execute(entity));
                    entity.getPersistentData().putDouble("knockback", 2.0);
                    entity.getPersistentData().putDouble("effectConfirm", 2.0);
                    RangeAttackProcedure.execute(world, x_pos, y_pos, z_pos, entity);
                    playSound(world, x_pos, y_pos, z_pos, "block.end_gateway.spawn", 5.0F, 1.0F);
                    playSound(world, x_pos, y_pos, z_pos, "block.end_gateway.spawn", 5.0F, 0.5F);
                }

                if (cnt2 < 3.0) {
                    double multiplier = (double) world.getLevelData().getGameRules().getInt(JujutsucraftaddonModGameRules.JJKU_RIKA_PURE_LOVE);
                    entity.getPersistentData().putDouble("BlockRange", 24.0 * multiplier);
                    entity.getPersistentData().putDouble("BlockDamage", 18.0 * multiplier);
                    entity.getPersistentData().putBoolean("noParticle", true);
                    BlockDestroyAllDirectionProcedure.execute(world, x_pos, y_pos, z_pos, entity);
                    playSound(world, x_pos, y_pos, z_pos, "block.end_gateway.spawn", 5.0F, 1.0F);
                    playSound(world, x_pos, y_pos, z_pos, "block.end_gateway.spawn", 5.0F, 0.75F);
                }
            }

            if (entity.getPersistentData().getDouble("cnt2") > 30.0 && !entity.level().isClientSide()) {
                entity.discard();
            }
        } else {
            // Not moving state - handle owner sync
            handleOwnerSync(world, x, y, z, entity);
        }
    }

    @Unique
    private static void handleOwnerSync(LevelAccessor world, double x, double y, double z, Entity entity) {
        boolean logic_attack = false;
        entity.getPersistentData().putBoolean("Stop", false);

        if (entity.getPersistentData().getDouble("NameRanged_ranged") != 0.0 && LogicOwnerExistProcedure.execute(world, entity)) {
            Entity owner = GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().getString("OWNER_UUID"));
            if (owner != null && entity.getPersistentData().getDouble("NameRanged_ranged") == owner.getPersistentData().getDouble("NameRanged") &&
                    owner.getPersistentData().getDouble("skill") != 0.0 && owner.getPersistentData().getDouble("cnt1") < 80.0) {

                logic_attack = true;
                entity.teleportTo(owner.getPersistentData().getDouble("x_pos"), owner.getPersistentData().getDouble("y_pos"), owner.getPersistentData().getDouble("z_pos"));
                if (entity instanceof ServerPlayer _serverPlayer) {
                    _serverPlayer.connection.teleport(owner.getPersistentData().getDouble("x_pos"), owner.getPersistentData().getDouble("y_pos"), owner.getPersistentData().getDouble("z_pos"), entity.getYRot(), entity.getXRot());
                }

                entity.setYRot(owner.getYRot());
                entity.setXRot(0.0F);
                entity.setYBodyRot(entity.getYRot());
                entity.setYHeadRot(entity.getYRot());
                entity.yRotO = entity.getYRot();
                entity.xRotO = entity.getXRot();
                if (entity instanceof LivingEntity _living) {
                    _living.yBodyRotO = _living.getYRot();
                    _living.yHeadRotO = _living.getYRot();
                }

                entity.getPersistentData().putDouble("x_power", owner.getPersistentData().getDouble("x_power"));
                entity.getPersistentData().putDouble("y_power", owner.getPersistentData().getDouble("y_power"));
                entity.getPersistentData().putDouble("z_power", owner.getPersistentData().getDouble("z_power"));
            }
        }

        if (Math.random() < 0.05)
            playSound(world, x, y, z, "jujutsucraft:electric_shock", 2.0F, (float) (0.5 + Math.random()));
        if (Math.random() < 0.5) playSound(world, x, y, z, "block.end_gateway.spawn", 1.0F, 0.5F);

        entity.setDeltaMovement(Vec3.ZERO);
        entity.getPersistentData().putBoolean("Stop", false);
        entity.getPersistentData().putDouble("cnt1", 0.0);
        entity.getPersistentData().putDouble("cnt2", 0.0);

        double num1 = -200.0 - Math.random();
        if ((!logic_attack || !entity.isAlive()) && world instanceof ServerLevel _serverLevel) {
            Entity crow = JujutsucraftModEntities.CROW.get().spawn(_serverLevel, BlockPos.containing(entity.getX(), num1, entity.getZ()), MobSpawnType.MOB_SUMMONED);
            if (crow != null) crow.setYRot(world.getRandom().nextFloat() * 360.0F);
        }

        boolean reChange = false;
        Vec3 center = new Vec3(entity.getX(), num1, entity.getZ());

        for (Entity target : world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(0.5), e -> true)
                .stream()
                .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(center)))
                .toList()) {
            if (target instanceof CrowEntity && target.isAlive()) {
                if (!target.level().isClientSide() && target.getServer() != null) {
                    target.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, target.position(), target.getRotationVector(), target.level() instanceof ServerLevel ? (ServerLevel) target.level() : null, 4, target.getName().getString(), target.getDisplayName(), target.level().getServer(), target), "kill @s");
                }
                if (!target.level().isClientSide()) target.discard();
                reChange = true;
                break;
            }
        }

        if (reChange && entity instanceof PureLoveCannonEntity _pureLove) {
            _pureLove.getEntityData().set(PureLoveCannonEntity.DATA_move, true);
        }
    }

    @Unique
    private static void playSound(LevelAccessor world, double x, double y, double z, String sound, float volume, float pitch) {
        if (world instanceof Level _level) {
            SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(sound));
            if (event != null) {
                if (!_level.isClientSide())
                    _level.playSound(null, BlockPos.containing(x, y, z), event, SoundSource.NEUTRAL, volume, pitch);
                else _level.playLocalSound(x, y, z, event, SoundSource.NEUTRAL, volume, pitch, false);
            }
        }
    }

    @Unique
    private static void runCommand(Entity entity, Vec3 pos, String command) {
        if (!entity.level().isClientSide() && entity.getServer() != null) {
            ServerLevel serverLevel = (ServerLevel) entity.level();
            CommandSourceStack source = new CommandSourceStack(CommandSource.NULL, pos, Vec2.ZERO, serverLevel, 4, "", Component.literal(""), serverLevel.getServer(), null).withSuppressedOutput();
            serverLevel.getServer().getCommands().performPrefixedCommand(source, command);
        }
    }
}
