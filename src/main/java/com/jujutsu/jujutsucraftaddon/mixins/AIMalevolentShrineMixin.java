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

import java.util.UUID;
import java.util.function.Consumer;

@Mixin(value = AIMalevolentShrineProcedure.class, priority = -10000)//ok
public abstract class AIMalevolentShrineMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void onExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        entity.setDeltaMovement(new Vec3(0.0, Math.min(entity.getDeltaMovement().y(), 0.0), 0.0));

        if (entity instanceof EntityMalevolentShrineEntity || entity instanceof MalevolentShrineEntity) {
            handleShrineInitialization(world, x, y, z, entity);
        }

        handleShrineCoreLogic(world, x, y, z, entity);
    }

    private static void handleShrineInitialization(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (!entity.getPersistentData().getBoolean("flag_start")) {
            entity.getPersistentData().putBoolean("flag_start", true);

            float originalYaw = entity.getYRot();
            float originalPitch = entity.getXRot();

            if (world instanceof ServerLevel serverLevel) {
                spawnPeripheralSkulls(serverLevel, entity);
            }

            syncRotation(entity, originalYaw, originalPitch);

            if (world instanceof ServerLevel serverLevel) {
                serverLevel.getServer().getCommands().performPrefixedCommand(
                        new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, serverLevel, 4, "", Component.literal(""), serverLevel.getServer(), null).withSuppressedOutput(),
                        "particle dust 0.251 0.000 0.000 4 ~ ~ ~ 4 0 4 1 30 force"
                );
            }
        }
    }

    private static void spawnPeripheralSkulls(ServerLevel level, Entity shrine) {
        for (int i = 0; i < 18; i++) {
            double angle = Math.toRadians(shrine.getYRot() + 90.0F);
            double offset = shrine.getBbWidth() - 2.5;
            double px = shrine.getX() + Math.cos(angle) * offset;
            double py = shrine.getY();
            double pz = shrine.getZ() + Math.sin(angle) * offset;

            BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos(px, py, pz);
            for (int j = 0; j < 100 && !level.getBlockState(mpos.below()).canOcclude(); j++) {
                mpos.move(0, -1, 0);
            }

            Entity skull = ((EntityType<?>) JujutsucraftModEntities.ENTITY_SKULL.get()).create(level, null, null, mpos, MobSpawnType.MOB_SUMMONED, false, false);
            if (skull != null) {
                skull.setYRot(level.getRandom().nextFloat() * 360.0F);

                if (level.getServer() != null) {
                    level.getServer().getCommands().performPrefixedCommand(
                            new CommandSourceStack(CommandSource.NULL, skull.position(), skull.getRotationVector(), level, 4, skull.getName().getString(), skull.getDisplayName(), level.getServer(), skull),
                            "data merge entity @s {Invulnerable:1b}"
                    );
                }

                syncRotation(skull, shrine.getYRot(), 0);
                skull.getPersistentData().putDouble("NameRanged_ranged", shrine.getPersistentData().getDouble("NameRanged_ranged"));
                skull.getPersistentData().putString("OWNER_UUID", shrine.getPersistentData().getString("OWNER_UUID"));
                level.addFreshEntity(skull);
            }

            shrine.setYRot(shrine.getYRot() + 20.0F);
        }
    }

    private static void handleShrineCoreLogic(LevelAccessor world, double x, double y, double z, Entity shrine) {
        boolean ownerIsValid = false;
        String ownerUuidStr = shrine.getPersistentData().getString("OWNER_UUID");

        if (!ownerUuidStr.isEmpty() && LogicOwnerExistProcedure.execute(world, shrine)) {
            Entity owner = GetEntityFromUUIDProcedure.execute(world, ownerUuidStr);
            if (owner != null) {
                double ownerNameRanged = owner.getPersistentData().getDouble("NameRanged");
                double shrineNameRanged = shrine.getPersistentData().getDouble("NameRanged_ranged");

                if (shrineNameRanged == ownerNameRanged) {
                    ownerIsValid = true;

                    if (owner instanceof LivingEntity livingOwner) {
                        if (!livingOwner.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
                            if (owner.getPersistentData().getDouble("select") == 0.0) {
                                shrine.getPersistentData().putBoolean("flag", true);
                            }
                        }
                    }

                    handleBrainBreakSounds(world, x, y, z, shrine, owner);
                }
            }
        }

        if (!ownerIsValid) {
            shrine.getPersistentData().putBoolean("flag", true);
        }

        if (shrine.getPersistentData().getBoolean("flag") && !shrine.level().isClientSide()) {
            shrine.discard();
        }
    }

    private static void handleBrainBreakSounds(LevelAccessor world, double x, double y, double z, Entity shrine, Entity owner) {
        if (shrine instanceof EntityMalevolentShrineEntity && owner.getPersistentData().getDouble("brokenBrain") >= 1.0 && owner.getPersistentData().getDouble("cnt1") >= 45.0) {
            if (!shrine.getPersistentData().getBoolean("flag_a")) {
                shrine.getPersistentData().putBoolean("flag_a", true);

                playSound(world, x, y, z, "jujutsucraft:stone_crash", 2.0F, 1.0F);
                playSound(world, x, y, z, "block.end_gateway.spawn", 2.0F, 0.8F);

                if (world instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 100, 1.0, 1.0, 1.0, 0.5);
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 100, 1.0, 1.0, 1.0, 0.5);
                }
            }
        }
    }

    private static void syncRotation(Entity ent, float yaw, float pitch) {
        ent.setYRot(yaw);
        ent.setXRot(pitch);
        ent.setYBodyRot(yaw);
        ent.setYHeadRot(yaw);
        ent.yRotO = yaw;
        ent.xRotO = pitch;
        if (ent instanceof LivingEntity living) {
            living.yBodyRotO = yaw;
            living.yHeadRotO = yaw;
        }
    }

    private static void playSound(LevelAccessor world, double x, double y, double z, String soundId, float volume, float pitch) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(soundId));
        if (sound == null) return;

        if (world instanceof Level level) {
            if (!level.isClientSide()) {
                level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.NEUTRAL, volume, pitch);
            } else {
                level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, volume, pitch, false);
            }
        }
    }
}
