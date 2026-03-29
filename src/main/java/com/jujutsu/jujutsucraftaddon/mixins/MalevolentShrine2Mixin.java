package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.ItadoriShinjukuEntity;
import com.jujutsu.jujutsucraftaddon.entity.MalevolentShrineEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
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

@Mixin(value = MalevolentShrineProcedure.class, priority = -10000)
public abstract class MalevolentShrine2Mixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity == null) return;

        double radius = JujutsucraftModVariables.MapVariables.get(world).DomainExpansionRadius;
        int amplifier = 0;
        if (entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())) {
            amplifier = _liv.getEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()).getAmplifier();
        }

        double dis = radius * (amplifier > 0 ? 6.0 : 2.0);
        entity.getPersistentData().putDouble("select", 1.0);
        DomainExpansionCreateBarrierProcedure.execute(world, x, y, z, entity);

        double cnt1 = entity.getPersistentData().getDouble("cnt1");
        if (cnt1 <= 0.0) {
            PlayAnimationProcedure.execute(world, entity);
            return;
        }

        // Addon Condition: Itadori Shinjuku Tree Domain
        JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());
        boolean isItadoriSpecial = entity instanceof ItadoriShinjukuEntity || 
            ("Itadori".equals(addonVars.Clans) && entity instanceof ServerPlayer _sp && 
             _sp.getAdvancements().getOrStartProgress(_sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:enchained"))).isDone() && 
             _sp.getAdvancements().getOrStartProgress(_sp.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:soul_research"))).isDone());

        if (isItadoriSpecial) {
            if (cnt1 == 34.0) {
                if (entity.getPersistentData().getDouble("NameRanged") == 0.0) {
                    entity.getPersistentData().putDouble("NameRanged", Math.random());
                }

                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(Math.random() * 720.0);
                    double dist = 0.0;
                    for (int j = 0; j < 32; j++) {
                        dist = Math.random();
                        if (dist > 0.5) {
                            dist *= (radius - 4.0);
                            break;
                        }
                    }
                    
                    double x_tree = entity.getPersistentData().getDouble("x_pos_doma") + Math.sin(angle) * dist;
                    double y_tree = entity.getPersistentData().getDouble("y_pos_doma") + 1.0;
                    double z_tree = entity.getPersistentData().getDouble("z_pos_doma") + Math.cos(angle) * dist;

                    for (int j = 0; j < 16; j++) {
                        if (world.getBlockState(BlockPos.containing(x_tree, y_tree, z_tree)).canOcclude()) y_tree++;
                        else if (!world.getBlockState(BlockPos.containing(x_tree, y_tree - 1.0, z_tree)).canOcclude()) y_tree--;
                        else {
                            y_tree = Math.floor(y_tree);
                            break;
                        }
                    }

                    if (world instanceof ServerLevel _level) {
                        double rot = Math.random() * 360.0;
                        _level.getServer().getCommands().performPrefixedCommand(
                            new CommandSourceStack(CommandSource.NULL, new Vec3(x_tree, y_tree, z_tree), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
                            "summon jujutsucraft:entity_tree ~ ~ ~ {NoAI:1b,Invulnerable:1b,Rotation:[" + rot + "F," + (Math.random() - 0.5) * 30.0 + "F]}"
                        );

                        final double fx = x_tree;
                        final double fy = y_tree;
                        final double fz = z_tree;
                        List<EntityTreeEntity> trees = _level.getEntitiesOfClass(EntityTreeEntity.class, new AABB(fx - 0.5, fy - 0.5, fz - 0.5, fx + 0.5, fy + 0.5, fz + 0.5));
                        if (!trees.isEmpty()) {
                            EntityTreeEntity tree = trees.stream().min(Comparator.comparingDouble(t -> t.distanceToSqr(fx, fy, fz))).get();
                            tree.getPersistentData().putDouble("NameRanged_ranged", entity.getPersistentData().getDouble("NameRanged"));
                            tree.getPersistentData().putString("OWNER_UUID", entity.getStringUUID());
                            tree.getAttribute(JujutsucraftModAttributes.SIZE.get()).setBaseValue(2.0 + Math.random());
                        }
                    }
                }
            }
        } else {
            if (cnt1 == 1.0) {
                if (entity instanceof LivingEntity _liv) _liv.swing(InteractionHand.MAIN_HAND, true);

                Vec3 center = new Vec3(entity.getPersistentData().getDouble("x_pos_doma"), entity.getPersistentData().getDouble("y_pos_doma"), entity.getPersistentData().getDouble("z_pos_doma"));
                for (Entity targetFound : world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(dis / 2.0), e -> true)) {
                    if (targetFound instanceof LivingEntity _target && !targetFound.level().isClientSide() && targetFound.getServer() != null) {
                        targetFound.getServer().getCommands().performPrefixedCommand(
                            new CommandSourceStack(CommandSource.NULL, targetFound.position(), targetFound.getRotationVector(), (ServerLevel)targetFound.level(), 4, targetFound.getName().getString(), targetFound.getDisplayName(), targetFound.level().getServer(), targetFound),
                            "effect give @s blindness 2 0 true"
                        );
                    }
                }

                if (world instanceof Level _level) {
                    SoundEvent chime = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:wind_chime"));
                    SoundEvent slowEnd = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:slow_motion_end"));
                    _level.playSound(null, BlockPos.containing(x, y, z), chime, SoundSource.NEUTRAL, 3.0F, 1.0F);
                    _level.playSound(null, BlockPos.containing(x, y, z), slowEnd, SoundSource.NEUTRAL, 3.0F, 1.0F);
                }
            }

            if (cnt1 == 34.0) {
                float yaw = entity.getYRot();
                float pitch = entity.getXRot();

                LivingEntity targetEntity = null;
                if (entity instanceof Mob _mob) targetEntity = _mob.getTarget();

                if (targetEntity != null) {
                    RotateEntityProcedure.execute(targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(), entity);
                } else {
                    RotateEntityProcedure.execute(entity.getPersistentData().getDouble("x_pos_doma"), entity.getPersistentData().getDouble("y_pos_doma"), entity.getPersistentData().getDouble("z_pos_doma"), entity);
                }

                entity.setXRot(0.0F);
                entity.setYBodyRot(entity.getYRot());
                entity.setYHeadRot(entity.getYRot());
                entity.yRotO = entity.getYRot();
                entity.xRotO = entity.getXRot();
                if (entity instanceof LivingEntity _liv) {
                    _liv.yBodyRotO = _liv.getYRot();
                    _liv.yHeadRotO = _liv.getYRot();
                    if (!_liv.level().isClientSide() && _liv.getServer() != null) {
                        _liv.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _liv.position(), _liv.getRotationVector(), (ServerLevel)_liv.level(), 4, _liv.getName().getString(), _liv.getDisplayName(), _liv.level().getServer(), _liv), "effect give @s slowness 1 10 true");
                    }
                }

                double x_doma = entity.getPersistentData().getDouble("x_pos_doma");
                double y_doma = entity.getPersistentData().getDouble("y_pos_doma");
                double z_doma = entity.getPersistentData().getDouble("z_pos_doma");
                entity.getPersistentData().putDouble("x_pos", x_doma);
                entity.getPersistentData().putDouble("z_pos", z_doma);

                for (int k = 0; k < 100; k++) {
                    if (!world.getBlockState(BlockPos.containing(x_doma, y_doma - 1.0, z_doma)).canOcclude()) {
                        y_doma -= 1.0;
                        if (y_doma <= 0.0) {
                            y_doma = 0.0;
                            break;
                        }
                    } else break;
                }
                entity.getPersistentData().putDouble("y_pos", y_doma);

                double yawRad = Math.toRadians(entity.getYRot() + 90.0F);
                double distShrine = radius - 5.0;
                double x_shrine = x_doma - Math.cos(yawRad) * distShrine;
                double y_shrine = y_doma;
                double z_shrine = z_doma - Math.sin(yawRad) * distShrine;

                if (entity.getPersistentData().getDouble("NameRanged") == 0.0) {
                    entity.getPersistentData().putDouble("NameRanged", Math.random());
                }

                if (world instanceof ServerLevel _level) {
                    boolean isPerfect = entity instanceof SukunaPerfectEntity || (entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftaddonModMobEffects.HEIAN_FORM.get())) || (entity instanceof SukunaFushiguroEntity _fushi && _fushi.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode));
                    boolean isLowHealth = false;
                    EntityType<?> shrineType;

                    if (isPerfect) {
                        shrineType = (EntityType<?>)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("jujutsucraftaddon:malevolent_shrine"));
                    } else {
                        if (entity instanceof LivingEntity _liv && _liv.getHealth() <= _liv.getMaxHealth() * 0.5) {
                            shrineType = (EntityType<?>)JujutsucraftModEntities.ENTITY_MALEVOLENT_SHRINE_2.get();
                            isLowHealth = true;
                        } else {
                            shrineType = (EntityType<?>)JujutsucraftModEntities.ENTITY_MALEVOLENT_SHRINE.get();
                        }
                    }
                    
                    if (shrineType != null) {
                        Entity shrine = shrineType.create(_level, null, null, BlockPos.containing(x_shrine, y_shrine, z_shrine), MobSpawnType.MOB_SUMMONED, false, false);
                        if (shrine != null) {
                            shrine.setInvulnerable(true);
                            if (isLowHealth && shrine instanceof Mob _shrineMob) _shrineMob.setNoAi(true);
                            
                            int shrineRot = (entity.getDirection() != Direction.NORTH && entity.getDirection() != Direction.SOUTH) ? 90 : 0;
                            shrine.setYRot(shrineRot);
                            shrine.setXRot(0.0F);
                            if (shrine instanceof LivingEntity _sLiv) {
                                _sLiv.setYBodyRot(shrineRot);
                                _sLiv.setYHeadRot(shrineRot);
                                _sLiv.yBodyRotO = _sLiv.yHeadRotO = shrineRot;
                            }
                            shrine.getPersistentData().putDouble("NameRanged_ranged", entity.getPersistentData().getDouble("NameRanged"));
                            shrine.getPersistentData().putString("OWNER_UUID", entity.getStringUUID());
                            
                            _level.addFreshEntity(shrine);
                            _level.playSound(null, BlockPos.containing(x_shrine, y_shrine, z_shrine), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("jujutsucraft:piano_horror")), SoundSource.NEUTRAL, 3.0F, 0.75F);
                        }
                    }
                }

                entity.getPersistentData().putDouble("y_pos_doma", y_shrine + 1.0);
                entity.setYRot(yaw);
                entity.setXRot(pitch);
            }
        }
        entity.yRotO = entity.getYRot();
        entity.xRotO = entity.getXRot();
        if (entity instanceof LivingEntity _liv) {
            _liv.yBodyRotO = _liv.yHeadRotO = _liv.getYRot();
        }
        PlayAnimationProcedure.execute(world, entity);
    }
}
