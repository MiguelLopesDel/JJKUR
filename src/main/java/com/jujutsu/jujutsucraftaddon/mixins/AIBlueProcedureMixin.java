package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.SetCustomizedProcedure;
import net.mcreator.jujutsucraft.entity.BlackHoleEntity;
import net.mcreator.jujutsucraft.entity.BlueEntity;
import net.mcreator.jujutsucraft.entity.EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModGameRules;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
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
import java.util.List;
import java.util.Optional;

@Mixin(value = AIBlueProcedure.class, priority = -10000)
public abstract class AIBlueProcedureMixin {

    /**
     * @author Satushi
     * @reason Keep addon Blue behavior compatible with new base structure. Owner is never pulled.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) {
            return;
        }

        AIBlueRedProcedure.execute(world, entity);
        String ownerUuid = entity.getPersistentData().getString("OWNER_UUID");
        Entity owner = LogicOwnerExistProcedure.execute(world, entity)
                ? GetEntityFromUUIDProcedure.execute(world, ownerUuid)
                : null;

        if (entity instanceof BlackHoleEntity blackHole) {
            double maxSize = entity.getPersistentData().getDouble("Ult") != 0 ? 120.0 : 64.0;
            AttributeInstance sizeAttr = blackHole.getAttribute(JujutsucraftModAttributes.SIZE.get());
            if (sizeAttr != null) {
                double currentSize = sizeAttr.getBaseValue();
                if (currentSize < maxSize) {
                    sizeAttr.setBaseValue(Math.min(currentSize + 0.8, maxSize));
                }

                double particleDis = sizeAttr.getBaseValue() * 10.0;
                if (world instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SQUID_INK, x, y, z, (int) particleDis, particleDis * 0.05, particleDis * 0.05, particleDis * 0.05, 1.0 + particleDis * 0.02);
                    serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, x, y, z, (int) particleDis, particleDis * 0.05, particleDis * 0.05, particleDis * 0.05, 1.0 + particleDis * 0.02);
                }
            }

            if (owner instanceof LivingEntity) {
                owner.setDeltaMovement(Vec3.ZERO);
                owner.teleportTo(x, y, z);
                if (owner instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.teleport(x, y, z, owner.getYRot(), owner.getXRot());
                }
            }
        }

        boolean started = entity instanceof BlueEntity blue && blue.getEntityData().get(BlueEntity.DATA_flag_start)
                || !(entity instanceof BlueEntity);

        if (started) {
            updateCircleMovement(entity, owner);

            double cnt6 = 1.0 + entity.getPersistentData().getDouble("cnt6") * 0.1;
            entity.getPersistentData().putDouble("cnt1", entity.getPersistentData().getDouble("cnt1") + 1.0);

            if (entity.getPersistentData().getDouble("cnt2") == 0.0) {
                entity.getPersistentData().putDouble("cnt2", 1.0);
                playBlueStartSound(world, x, y, z, cnt6, 1.0F);
                playBlueStartSound(world, x, y, z, cnt6, 0.5F);
                entity.getPersistentData().putDouble("cnt_bullet_hit", 15.0);
                BulletDomainHit2Procedure.execute(world, entity);
            }

            double xPos = entity.getX();
            double yPos = entity.getY();
            double zPos = entity.getZ();

            if (entity.getPersistentData().getDouble("cnt1") % 2.0 == 1.0) {
                int amplifier = 0;
                if (entity instanceof LivingEntity living && living.hasEffect(MobEffects.DAMAGE_BOOST)) {
                    var effect = living.getEffect(MobEffects.DAMAGE_BOOST);
                    amplifier = effect != null ? effect.getAmplifier() : 0;
                }
                applyAddonBlockDestroy(world, entity, xPos, yPos, zPos, cnt6, amplifier);
            }

            boolean crushScaled = false;
            if (world instanceof ServerLevel) {
                crushScaled = applyAttractionPhases(world, x, y, z, entity, owner, ownerUuid, cnt6);
            }

            entity.getPersistentData().putDouble("Damage", 13.0 * cnt6);
            entity.getPersistentData().putDouble("Range", 4.0 * cnt6);
            RangeAttackProcedure.execute(world, xPos, yPos, zPos, entity);

            // Addon extra chip damage around Blue/Black Hole.
            entity.getPersistentData().putDouble("Damage", 0.5 * cnt6);
            entity.getPersistentData().putDouble("Range", Math.min(45.0 * cnt6, 75.0));
            applyAddonAreaDamage(world, xPos, yPos, zPos, entity);

            if (crushScaled) {
                playCrushSound(world, x, y, z);
            }

            if (shouldExpire(entity, cnt6)) {
                if (entity instanceof BlackHoleEntity) {
                    cleanupOwnerOnBlackHoleEnd(owner);
                }
                if (!entity.level().isClientSide()) {
                    entity.discard();
                }
            }

            // Official Trail Particles from Base Mod (v43)
            double rangeParticles = ReturnEntitySizeProcedure.execute(entity);
            for (int i = 0; i < 8; i++) {
                double px = entity.getX() + (Math.random() - 0.5) * 48.0 * rangeParticles;
                double py = entity.getY() + (Math.random() - 0.5) * 48.0 * rangeParticles;
                double pz = entity.getZ() + (Math.random() - 0.5) * 48.0 * rangeParticles;
                double dx = entity.getX() - px;
                double dy = entity.getY() - py;
                double dz = entity.getZ() - pz;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist != 0.0 && world instanceof ServerLevel serverLevel) {
                    dx /= dist;
                    dy /= dist;
                    dz /= dist;
                    serverLevel.getServer().getCommands().performPrefixedCommand(
                            new CommandSourceStack(CommandSource.NULL, new Vec3(px, py, pz), Vec2.ZERO, serverLevel, 4, "", Component.literal(""), serverLevel.getServer(), null)
                                    .withSuppressedOutput(),
                            "particle minecraft:enchanted_hit ~ ~ ~ " + (dx * 10000.0) + " " + (dy * 10000.0) + " " + (dz * 10000.0) + " 0.0025 0 force"
                    );
                }
            }
        }

        if (!entity.isAlive()) {
            if (entity instanceof BlackHoleEntity) {
                cleanupOwnerOnBlackHoleEnd(owner);
            }
            if (!entity.level().isClientSide()) {
                entity.discard();
            }
        }

        SetCustomizedProcedure.execute(world, x, y, z, entity);
    }

    @Unique
    private static void updateCircleMovement(Entity entity, Entity owner) {
        if (!entity.getPersistentData().getBoolean("circle")) {
            entity.setDeltaMovement(Vec3.ZERO);
            return;
        }

        if (entity.getPersistentData().getDouble("NameRanged_ranged") != 0.0
                && owner instanceof LivingEntity
                && entity.getPersistentData().getDouble("NameRanged_ranged") == owner.getPersistentData().getDouble("NameRanged")) {
            BlockPos ownerPos = owner.level().clip(new ClipContext(
                    owner.getEyePosition(1.0F),
                    owner.getEyePosition(1.0F).add(owner.getViewVector(1.0F).scale(0.0)),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    owner
            )).getBlockPos();
            RotateEntityProcedure.execute(ownerPos.getX(), ownerPos.getY(), ownerPos.getZ(), entity);
        }

        entity.setYRot(entity.getYRot() + 90.0F);
        entity.setXRot(entity.getXRot());
        entity.setYBodyRot(entity.getYRot());
        entity.setYHeadRot(entity.getYRot());
        entity.yRotO = entity.getYRot();
        entity.xRotO = entity.getXRot();
        if (entity instanceof LivingEntity living) {
            living.yBodyRotO = living.getYRot();
            living.yHeadRotO = living.getYRot();
        }

        entity.getPersistentData().putBoolean("free", true);
        BlockPos lookPos = entity.level().clip(new ClipContext(
                entity.getEyePosition(1.0F),
                entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(24.0)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                entity
        )).getBlockPos();
        GetPowerForwardProcedure.execute(lookPos.getX(), lookPos.getY(), lookPos.getZ(), entity);
        entity.setDeltaMovement(new Vec3(
                entity.getPersistentData().getDouble("x_power") * 0.4,
                entity.getPersistentData().getDouble("y_power") * 0.4,
                entity.getPersistentData().getDouble("z_power") * 0.4
        ));
    }

    @Unique
    private static void applyAddonBlockDestroy(LevelAccessor world, Entity entity, double x, double y, double z, double cnt6, int amplifier) {
        double range = Math.min(amplifier, 30) * 0.0333;
        boolean ult = entity.getPersistentData().getDouble("Ult") != 0.0;
        double ultScale = ult ? 2.0 : 1.0;

        // In Ult mode, the range/amplifier itself is doubled, and then the final damage is doubled again (double-scaling)
        double effectiveRange = range * ultScale;

        entity.getPersistentData().putDouble("knockback", -1.0);
        entity.getPersistentData().putDouble("BlockRange", Math.min(7.0 * cnt6, entity.getPersistentData().getDouble("cnt1") * 0.5) * ultScale);
        entity.getPersistentData().putDouble("BlockDamage", 5.0 * (effectiveRange + 0.01) * cnt6 * ultScale);
        entity.getPersistentData().putBoolean("noParticle", entity instanceof BlackHoleEntity);
        BlockDestroyAllDirectionProcedure.execute(world, x, y, z, entity);

        entity.getPersistentData().putDouble("BlockRange", Math.min(9.0 * cnt6, entity.getPersistentData().getDouble("cnt1")) * ultScale);
        entity.getPersistentData().putDouble("BlockDamage", 2.5 * (effectiveRange + 0.01) * cnt6 * ultScale);
        entity.getPersistentData().putBoolean("noParticle", entity instanceof BlackHoleEntity);
        BlockDestroyAllDirectionProcedure.execute(world, x, y, z, entity);
    }

    @Unique
    private static boolean applyAttractionPhases(LevelAccessor world, double x, double y, double z, Entity entity, Entity owner, String ownerUuid, double cnt6) {
        boolean logicB = false;
        double powerAttenuation = 1.0;
        double xKnockback, yKnockback, zKnockback;

        for (int i = 0; i < 5; i++) {
            double range = Math.min(45.0 * powerAttenuation * cnt6, 75.0);
            double knockback = Math.max(-5.0 * (1.2 - powerAttenuation) * cnt6, -8.0);
            entity.getPersistentData().putDouble("Range", range);
            entity.getPersistentData().putDouble("knockback", knockback);

            Vec3 center = new Vec3(x, y, z);
            List<Entity> entities = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(range / 2.0), e -> true)
                    .stream()
                    .sorted(Comparator.comparingDouble(target -> target.distanceToSqr(center)))
                    .toList();

            for (Entity target : entities) {
                if (target == entity) {
                    continue;
                }
                boolean isOwner = ownerUuid != null && !ownerUuid.isEmpty() && ownerUuid.equals(target.getStringUUID());
                if (isOwner && target.isShiftKeyDown()) {
                    continue;
                }
                if (!canAffectTarget(world, entity, owner, target)) {
                    continue;
                }
                if (isMahoragaAdapted(entity, target)) {
                    continue;
                }

                xKnockback = target.getX() - entity.getX();
                yKnockback = target.getY() - entity.getY();
                zKnockback = target.getZ() - entity.getZ();
                double dis = Math.sqrt(xKnockback * xKnockback + yKnockback * yKnockback + zKnockback * zKnockback);

                if (dis < Math.max(entity.getBbWidth(), 1.0F)
                        && entity.getPersistentData().getDouble("NameRanged_ranged") != target.getPersistentData().getDouble("NameRanged")) {
                    if (!(target instanceof LivingEntity)) {
                        runEntityCommand(target, "kill @s");
                    }
                    if (!target.isAlive() && !target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("forge:not_living")))) {
                        if (target.getBbHeight() > 0.25) {
                            runEntityCommand(target, "scale add pehkui:height -0.025 @s");
                            logicB = true;
                        }
                        if (target.getBbWidth() > 0.25) {
                            runEntityCommand(target, "scale add pehkui:width -0.025 @s");
                            logicB = true;
                        }
                    }
                    xKnockback = 0.0;
                    yKnockback = 0.0;
                    zKnockback = 0.0;
                } else {
                    xKnockback = xKnockback / dis * knockback;
                    yKnockback = yKnockback / dis * knockback;
                    zKnockback = zKnockback / dis * knockback;

                    if (!(xKnockback * 1.1 < target.getDeltaMovement().x()) && !(xKnockback * 0.9 > target.getDeltaMovement().x())) {
                        xKnockback = target.getDeltaMovement().x();
                    } else {
                        xKnockback = target.getDeltaMovement().x() + xKnockback * 0.05;
                    }

                    if (!(yKnockback * 1.1 < target.getDeltaMovement().y()) && !(yKnockback * 0.9 > target.getDeltaMovement().y())) {
                        yKnockback = target.getDeltaMovement().y();
                    } else {
                        yKnockback = target.getDeltaMovement().y() + yKnockback * 0.05;
                    }

                    if (!(zKnockback * 1.1 < target.getDeltaMovement().z()) && !(zKnockback * 0.9 > target.getDeltaMovement().z())) {
                        zKnockback = target.getDeltaMovement().z();
                    } else {
                        zKnockback = target.getDeltaMovement().z() + zKnockback * 0.05;
                    }

                    if (target.onGround()) {
                        yKnockback = Math.max(yKnockback, 0.5 * (1.2 - powerAttenuation) * cnt6);
                    }
                }
                applyEntityKnockback(target, xKnockback, Math.min(yKnockback, 1.5), zKnockback);
            }

            entity.getPersistentData().putDouble("knockback", 0.0);
            powerAttenuation *= 0.75;
        }

        return logicB;
    }

    @Unique
    private static boolean canAffectTarget(LevelAccessor world, Entity entity, Entity owner, Entity target) {
        if (target instanceof Player player) {
            // Owner is always affected by their own Blue (unless shifting, handled before this call)
            // Other players are only affected if Jujutsu PvP is enabled.
            if (target != owner && !world.getLevelData().getGameRules().getBoolean(JujutsucraftModGameRules.JUJUTSUPVP)) {
                return false;
            }
            if (player.getAbilities().instabuild || target.isSpectator()) {
                return false;
            }
            return true;
        }
        if (target == owner) {
            if (target.isShiftKeyDown()) return false;
            if (target instanceof LivingEntity living && living.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()))
                return false;
        }
        return entity.getPersistentData().getDouble("NameRanged_ranged") != target.getPersistentData().getDouble("NameRanged");
    }

    @Unique
    private static boolean isMahoragaAdapted(Entity entity, Entity target) {
        boolean mahoragaCandidate = target instanceof EightHandledSwordDivergentSilaDivineGeneralMahoragaEntity;
        if (target instanceof Player player) {
            JujutsucraftModVariables.PlayerVariables vars = player
                    .getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftModVariables.PlayerVariables());
            mahoragaCandidate = vars.PlayerCurseTechnique == 16.0 || vars.PlayerCurseTechnique2 == 16.0;
        }

        if (!mahoragaCandidate) {
            return false;
        }

        ItemStack helmet = target instanceof LivingEntity living ? living.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY;
        if (target instanceof Player player && player.getCooldowns().isOnCooldown(helmet.getItem())) {
            return false;
        }

        if (helmet.getItem() == JujutsucraftModItems.MAHORAGA_WHEEL_HELMET.get()
                || helmet.getItem() == JujutsucraftModItems.MAHORAGA_BODY_HELMET.get()) {
            CompoundTag tag = helmet.getOrCreateTag();
            return tag.getDouble("skill" + Math.round(entity.getPersistentData().getDouble("skill"))) >= 100.0;
        }

        return false;
    }

    @Unique
    private static void applyAddonAreaDamage(LevelAccessor world, double x, double y, double z, Entity entity) {
        Vec3 center = new Vec3(x, y, z);
        List<Entity> entities = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(entity.getPersistentData().getDouble("Range") / 2.0), e -> true)
                .stream()
                .sorted(Comparator.comparingDouble(target -> target.distanceToSqr(center)))
                .toList();

        for (Entity target : entities) {
            if (entity != target && LogicAttackProcedure.execute(world, entity, target)) {
                target.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)),
                        (float) entity.getPersistentData().getDouble("Damage"));
            }
        }
    }

    @Unique
    private static boolean shouldExpire(Entity entity, double cnt6) {
        if (entity.getPersistentData().getBoolean("circle")) {
            return entity.getPersistentData().getDouble("cnt1") > 120.0;
        }
        return entity.getPersistentData().getDouble("cnt1") > 60.0 * cnt6;
    }

    @Unique
    private static void cleanupOwnerOnBlackHoleEnd(Entity owner) {
        if (!(owner instanceof LivingEntity)) {
            return;
        }

        owner.getPersistentData().putDouble("skill", 0.0);
        ((LivingEntity) owner).removeEffect(JujutsucraftModMobEffects.STAR_RAGE.get());

        if (owner instanceof Player player && player.getAbilities().instabuild) {
            return;
        }

        runEntityCommand(owner, "kill @s");
    }

    @Unique
    private static void playBlueStartSound(LevelAccessor world, double x, double y, double z, double cnt6, float pitch) {
        if (!(world instanceof Level level)) {
            return;
        }

        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("block.end_gateway.spawn"));
        if (sound == null) {
            return;
        }

        if (!level.isClientSide()) {
            level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.NEUTRAL, (float) (1.5 + cnt6), pitch);
        } else {
            level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, (float) (1.5 + cnt6), pitch, false);
        }
    }

    @Unique
    private static void playCrushSound(LevelAccessor world, double x, double y, double z) {
        if (!(world instanceof Level level)) {
            return;
        }

        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("jujutsucraft:crush"));
        if (sound == null) {
            return;
        }

        if (!level.isClientSide()) {
            level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.NEUTRAL, 0.25F, 1.0F);
        } else {
            level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, 0.25F, 1.0F, false);
        }
    }

    @Unique
    private static void runEntityCommand(Entity entity, String command) {
        if (!(entity.level() instanceof ServerLevel serverLevel) || entity.getServer() == null) {
            return;
        }

        entity.getServer().getCommands().performPrefixedCommand(
                new CommandSourceStack(
                        CommandSource.NULL,
                        entity.position(),
                        entity.getRotationVector(),
                        serverLevel,
                        4,
                        entity.getName().getString(),
                        entity.getDisplayName(),
                        serverLevel.getServer(),
                        entity
                ),
                command
        );
    }

    @Unique
    private static void applyEntityKnockback(Entity target, double x, double y, double z) {
        EntityVectorProcedure.execute(target, x, y, z);
    }
}

