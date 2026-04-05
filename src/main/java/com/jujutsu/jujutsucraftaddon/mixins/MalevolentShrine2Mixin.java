package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.ItadoriShinjukuEntity;
import com.jujutsu.jujutsucraftaddon.entity.MalevolentShrineEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModEntities;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.entity.EntityMalevolentShrine2Entity;
import net.mcreator.jujutsucraft.entity.EntityMalevolentShrineEntity;
import net.mcreator.jujutsucraft.entity.EntityTreeEntity;
import net.mcreator.jujutsucraft.entity.SukunaFushiguroEntity;
import net.mcreator.jujutsucraft.entity.SukunaPerfectEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.DomainExpansionCreateBarrierProcedure;
import net.mcreator.jujutsucraft.procedures.MalevolentShrineProcedure;
import net.mcreator.jujutsucraft.procedures.PlayAnimationProcedure;
import net.mcreator.jujutsucraft.procedures.RotateEntityProcedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Mixin(value = MalevolentShrineProcedure.class, priority = -10000)
public abstract class MalevolentShrine2Mixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void onExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        double radius = JujutsucraftModVariables.MapVariables.get(world).DomainExpansionRadius;
        int amplifier = 0;

        if (entity instanceof LivingEntity living) {
            MobEffectInstance effect = living.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get());
            if (effect != null) {
                amplifier = effect.getAmplifier();
            }
        }

        double totalRadius = radius * (amplifier > 0 ? 6 : 2);
        entity.getPersistentData().putDouble("select", 1.0);
        DomainExpansionCreateBarrierProcedure.execute(world, x, y, z, entity);

        double cnt1 = entity.getPersistentData().getDouble("cnt1");
        if (cnt1 > 0) {
            handleShrinePhases(world, x, y, z, entity, totalRadius, radius, cnt1);
        }

        PlayAnimationProcedure.execute(world, entity);
    }

    private static void handleShrinePhases(LevelAccessor world, double x, double y, double z, Entity entity, double totalRadius, double baseRadius, double cnt1) {
        if (cnt1 == 34.0) {
            if (isItadoriSpecialDomain(entity)) {
                spawnItadoriSoulTrees(world, entity, baseRadius);
            } else {
                handleMalevolentShrineSpawning(world, entity, baseRadius);
            }
        } else if (cnt1 == 1.0) {
            handleInitialBlindness(world, entity, totalRadius);
        }
    }

    private static boolean isItadoriSpecialDomain(Entity entity) {
        if (entity instanceof ItadoriShinjukuEntity) return true;
        if (entity instanceof ServerPlayer player) {
            boolean hasEnchained = player.getAdvancements().getOrStartProgress(Objects.requireNonNull(player.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:enchained")))).isDone();
            boolean hasSoulResearch = player.getAdvancements().getOrStartProgress(Objects.requireNonNull(player.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:soul_research")))).isDone();
            String clan = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables()).Clans;
            return hasEnchained && hasSoulResearch && clan.equals("Itadori");
        }
        return false;
    }

    private static void spawnItadoriSoulTrees(LevelAccessor world, Entity entity, double baseRadius) {
        if (entity.getPersistentData().getDouble("NameRanged") == 0.0) {
            entity.getPersistentData().putDouble("NameRanged", Math.random());
        }

        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(Math.random() * 720.0);
            double dist = 0;
            for (int j = 0; j < 32; j++) {
                dist = Math.random();
                if (dist > 0.5) {
                    dist *= (baseRadius - 4.0);
                    break;
                }
            }

            double px = entity.getPersistentData().getDouble("x_pos_doma") + Math.sin(angle) * dist;
            double py = findGroundY(world, px, entity.getPersistentData().getDouble("y_pos_doma") + 1.0, entity.getPersistentData().getDouble("z_pos_doma") + Math.cos(angle) * dist);
            double pz = entity.getPersistentData().getDouble("z_pos_doma") + Math.cos(angle) * dist;

            if (world instanceof ServerLevel serverLevel) {
                String cmd = String.format("summon jujutsucraft:entity_tree %f %f %f {NoAI:1b,Invulnerable:1b,Rotation:[%fF,%fF]}", 
                    px, py, pz, (float)(Math.random() * 360.0), (float)((Math.random() - 0.5) * 30.0));
                serverLevel.getServer().getCommands().performPrefixedCommand(createCommandSource(serverLevel, px, py, pz), cmd);

                List<EntityTreeEntity> trees = world.getEntitiesOfClass(EntityTreeEntity.class, AABB.ofSize(new Vec3(px, py, pz), 1.0, 1.0, 1.0), e -> true);
                trees.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(px, py, pz))).ifPresent(tree -> {
                    tree.getPersistentData().putDouble("NameRanged_ranged", entity.getPersistentData().getDouble("NameRanged"));
                    tree.getPersistentData().putString("OWNER_UUID", entity.getStringUUID());
                    tree.getAttribute(JujutsucraftModAttributes.SIZE.get()).setBaseValue(2.0 + Math.random());
                });
            }
        }
    }

    private static void handleMalevolentShrineSpawning(LevelAccessor world, Entity entity, double baseRadius) {
        float originalYaw = entity.getYRot();
        float originalPitch = entity.getXRot();

        Entity target = (entity instanceof net.minecraft.world.entity.Mob _mob) ? _mob.getTarget() : null;
        if (target instanceof LivingEntity) {
            RotateEntityProcedure.execute(target.getX(), target.getY(), target.getZ(), entity);
        } else {
            RotateEntityProcedure.execute(entity.getPersistentData().getDouble("x_pos_doma"), entity.getPersistentData().getDouble("y_pos_doma"), entity.getPersistentData().getDouble("z_pos_doma"), entity);
        }
        syncRotation(entity, entity.getYRot(), 0.0F);

        if (world instanceof ServerLevel serverLevel) {
            serverLevel.getServer().getCommands().performPrefixedCommand(createCommandSource(serverLevel, entity.position().x, entity.position().y, entity.position().z), "effect give @s slowness 1 10 true");
        }

        double spawnX = entity.getPersistentData().getDouble("x_pos_doma");
        double spawnY = entity.getPersistentData().getDouble("y_pos_doma");
        double spawnZ = entity.getPersistentData().getDouble("z_pos_doma");

        for (int i = 0; i < 100 && !world.getBlockState(BlockPos.containing(spawnX, spawnY - 1.0, spawnZ)).canOcclude(); i++) {
            spawnY--;
            if (spawnY <= 0) { spawnY = 0; break; }
        }

        double offset = baseRadius - 5.0;
        double rad = Math.toRadians(entity.getYRot() + 90.0F);
        double finalX = spawnX - Math.cos(rad) * offset;
        double finalZ = spawnZ - Math.sin(rad) * offset;

        if (world instanceof ServerLevel serverLevel) {
            boolean isLowHealth = (entity instanceof LivingEntity living) && living.getHealth() <= living.getMaxHealth() * 0.5;
            boolean isHeian = (entity instanceof SukunaPerfectEntity) || 
                             (entity instanceof LivingEntity livingEnt && livingEnt.hasEffect(JujutsucraftaddonModMobEffects.HEIAN_FORM.get())) ||
                             (entity instanceof SukunaFushiguroEntity fushi && fushi.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode));

            EntityType<?> type;
            if (isLowHealth) {
                type = (EntityType<?>) JujutsucraftModEntities.ENTITY_MALEVOLENT_SHRINE_2.get();
            } else {
                type = isHeian ? (EntityType<?>) JujutsucraftaddonModEntities.MALEVOLENT_SHRINE.get() : (EntityType<?>) JujutsucraftModEntities.ENTITY_MALEVOLENT_SHRINE.get();
            }

            Entity shrine = type.create(serverLevel, null, null, BlockPos.containing(finalX, spawnY, finalZ), MobSpawnType.MOB_SUMMONED, false, false);
            if (shrine != null) {
                shrine.setYRot(entity.getDirection() != Direction.NORTH && entity.getDirection() != Direction.SOUTH ? 90 : 0);
                shrine.setXRot(0.0F);
                syncRotation(shrine, shrine.getYRot(), 0.0F);

                if (entity.getPersistentData().getDouble("NameRanged") == 0.0) {
                    entity.getPersistentData().putDouble("NameRanged", Math.random());
                }
                shrine.getPersistentData().putDouble("NameRanged_ranged", entity.getPersistentData().getDouble("NameRanged"));
                shrine.getPersistentData().putString("OWNER_UUID", entity.getStringUUID());

                if (shrine.getServer() != null) {
                    String nbt = isLowHealth ? "{NoAI:1b,Invulnerable:1b}" : "{Invulnerable:1b}";
                    shrine.getServer().getCommands().performPrefixedCommand(
                        new CommandSourceStack(CommandSource.NULL, shrine.position(), shrine.getRotationVector(), serverLevel, 4, shrine.getName().getString(), shrine.getDisplayName(), shrine.getServer(), shrine),
                        "data merge entity @s " + nbt
                    );
                }

                serverLevel.addFreshEntity(shrine);
                playSound(world, finalX, spawnY, finalZ, "jujutsucraft:piano_horror", 3.0F, 0.75F);
            }
        }

        entity.getPersistentData().putDouble("y_pos_doma", spawnY + 1.0);
        syncRotation(entity, originalYaw, originalPitch);
    }

    private static void handleInitialBlindness(LevelAccessor world, Entity entity, double totalRadius) {
        if (entity instanceof LivingEntity living) living.swing(InteractionHand.MAIN_HAND, true);

        Vec3 center = new Vec3(entity.getPersistentData().getDouble("x_pos_doma"), entity.getPersistentData().getDouble("y_pos_doma"), entity.getPersistentData().getDouble("z_pos_doma"));
        List<Entity> targets = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(totalRadius / 2.0), e -> true);
        
        for (Entity target : targets) {
            if (target instanceof LivingEntity livingTarget && !target.level().isClientSide() && target.getServer() != null) {
                target.getServer().getCommands().performPrefixedCommand(createCommandSource((ServerLevel)target.level(), target.getX(), target.getY(), target.getZ()), "effect give @s blindness 2 0 true");
            }
        }

        playSound(world, entity.getX(), entity.getY(), entity.getZ(), "jujutsucraft:wind_chime", 3.0F, 1.0F);
        playSound(world, entity.getX(), entity.getY(), entity.getZ(), "jujutsucraft:slow_motion_end", 3.0F, 1.0F);
    }

    private static double findGroundY(LevelAccessor world, double x, double y, double z) {
        double currentY = y;
        for (int i = 0; i < 16; i++) {
            if (world.getBlockState(BlockPos.containing(x, currentY, z)).canOcclude()) currentY++;
            else if (!world.getBlockState(BlockPos.containing(x, currentY - 1.0, z)).canOcclude()) currentY--;
            else break;
        }
        return Math.floor(currentY);
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

    private static CommandSourceStack createCommandSource(ServerLevel level, double x, double y, double z) {
        return new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, level, 4, "", Component.literal(""), level.getServer(), null).withSuppressedOutput();
    }

    private static void playSound(LevelAccessor world, double x, double y, double z, String soundId, float vol, float pitch) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(soundId));
        if (sound != null && world instanceof Level level) {
            if (!level.isClientSide()) level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.NEUTRAL, vol, pitch);
            else level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, vol, pitch, false);
        }
    }
}
