package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModGameRules;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AIPurpleProcedure.class, priority = -10000)
public abstract class AIPurpleProcedureMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity == null) return;

        boolean big = entity.getPersistentData().getBoolean("explode");
        double power = 1.0 + entity.getPersistentData().getDouble("cnt6") * 0.1;
        // JJKUR uses cnt6 to increase range, v43 uses x3 multiplier for big. We combine for the ultimate purple.
        double range = ReturnEntitySizeProcedure.execute(entity) * (big ? 3.0 : 1.0) + entity.getPersistentData().getDouble("cnt6");

        if (big && entity.getPersistentData().getDouble("cnt3") == 0.0) {
            entity.getPersistentData().putDouble("cnt3", 1.0);
        }

        if (entity.getPersistentData().getDouble("NameRanged_ranged") != 0.0) {
            if (entity.getPersistentData().getDouble("cnt3") == 0.0) {
                // Phase 0: Sync with Owner
                if (LogicOwnerExistProcedure.execute(world, entity)) {
                    Entity owner = GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().getString("OWNER_UUID"));
                    if (owner != null && entity.getPersistentData().getDouble("NameRanged_ranged") == owner.getPersistentData().getDouble("NameRanged")) {
                        entity.getPersistentData().putDouble("x_power", owner.getPersistentData().getDouble("x_power"));
                        entity.getPersistentData().putDouble("y_power", owner.getPersistentData().getDouble("y_power"));
                        entity.getPersistentData().putDouble("z_power", owner.getPersistentData().getDouble("z_power"));
                        entity.getPersistentData().putDouble("cnt3", 1.0);
                    }
                }
                if (Math.random() < 0.05) playSound(world, x, y, z, "jujutsucraft:electric_shock", 2.0F, (float) (0.5 + Math.random()));
                if (!entity.isAlive()) entity.getPersistentData().putDouble("cnt3", 1.0);
            }

            if (entity.getPersistentData().getDouble("cnt3") != 0.0) {
                entity.getPersistentData().putDouble("cnt1", entity.getPersistentData().getDouble("cnt1") + 1.0);
                double cnt1 = entity.getPersistentData().getDouble("cnt1");

                if (cnt1 < 10.0) {
                    // Phase 1: Charging
                    entity.setDeltaMovement(Vec3.ZERO);
                    
                    // Smooth attribute scaling (v43 logic with JJKUR 400 limit)
                    if (entity instanceof LivingEntity _liv) {
                        double currentSize = _liv.getAttribute(JujutsucraftModAttributes.SIZE.get()).getBaseValue();
                        double targetSize = entity.getPersistentData().getDouble("size");
                        if (currentSize < targetSize) {
                            _liv.getAttribute(JujutsucraftModAttributes.SIZE.get()).setBaseValue(Math.min(currentSize + (targetSize - 5.0) * 0.2, targetSize));
                        }
                    }

                    if (big) {
                        entity.teleportTo(entity.getX(), entity.getY() + 1.0, entity.getZ());
                        if (world instanceof ServerLevel _level) {
                            _level.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY(), entity.getZ(), (int)range, range, range, range, 0.05);
                        }
                    } else if (LogicOwnerExistProcedure.execute(world, entity)) {
                        Entity owner = GetEntityFromUUIDProcedure.execute(world, entity.getPersistentData().getString("OWNER_UUID"));
                        if (owner != null) {
                            double yaw = Math.toRadians(owner.getYRot() + 90.0F);
                            double pitch = Math.toRadians(owner.getXRot());
                            double dis = (2.0 + owner.getBbWidth() + Math.max(entity.getPersistentData().getDouble("size"), 5.0) * 0.2) * 0.75;
                            teleportWithSync(entity, owner.getX() + Math.cos(yaw) * Math.cos(pitch) * dis, owner.getY() + owner.getBbHeight() * 0.5 + Math.sin(pitch) * -1.0 * dis, owner.getZ() + Math.sin(yaw) * Math.cos(pitch) * dis);
                        }
                    }
                } else {
                    // Phase 2: Active
                    BulletDomainHit2Procedure.execute(world, entity);
                    
                    if (big && entity instanceof LivingEntity _liv) {
                        double currentSize = _liv.getAttribute(JujutsucraftModAttributes.SIZE.get()).getBaseValue();
                        if (currentSize < 400.0) { // JJKUR 200% Purple Cap
                            _liv.getAttribute(JujutsucraftModAttributes.SIZE.get()).setBaseValue(Math.min(currentSize + 15.0, 400.0));
                        }
                        if (cnt1 == 10.0) playSound(world, x, y, z, "jujutsucraft:electric_shock", 4.0F, 0.75F);
                        if (cnt1 % 4.0 == 1.0) playSound(world, x, y, z, "jujutsucraft:electric_shock", 4.0F, (float) (0.5 + Math.random()));
                        
                        // Circle Particles (v43)
                        for (int i = 0; i < (int)(36.0 * range); i++) {
                            ParticleGeneratorCircleProcedure.execute(world, 1.0, Mth.nextDouble(RandomSource.create(), -45.0, 45.0) * Math.random() * Math.random() * Math.random(), 0.0, 1.0, 12.0, entity.getX(), entity.getX(), entity.getY(), entity.getY(), Mth.nextDouble(RandomSource.create(), 0.0, 360.0), entity.getZ(), entity.getZ(), "jujutsucraft:particle_thunder_purple");
                            ParticleGeneratorCircleProcedure.execute(world, 1.0, Mth.nextDouble(RandomSource.create(), -45.0, 45.0) * Math.random() * Math.random() * Math.random(), 0.0, 1.0, 12.0, entity.getX(), entity.getX(), entity.getY(), entity.getY(), Mth.nextDouble(RandomSource.create(), 0.0, 360.0), entity.getZ(), entity.getZ(), "cloud");
                        }
                    }

                    if (!big && world instanceof ServerLevel _level) {
                        _level.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_THUNDER_PURPLE.get(), entity.getX(), entity.getY(), entity.getZ(), (int) (5.0 * range), range, range, range, 0.05);
                    }

                    double xp = entity.getPersistentData().getDouble("x_power") * 2.0;
                    double yp = entity.getPersistentData().getDouble("y_power") * 2.0;
                    double zp = entity.getPersistentData().getDouble("z_power") * 2.0;
                    double dist = Math.sqrt(xp * xp + yp * yp + zp * zp);
                    if (dist > 0.0) { xp /= dist; yp /= dist; zp /= dist; }

                    double damage = Math.max(80.0 * Math.pow(0.99, entity.getPersistentData().getDouble("cnt_life")), 55.0) * power;

                    for (int i = 0; i < (int) Math.round(Math.max(dist, 1.0)); i++) {
                        double ex = entity.getX(); double ey = entity.getY(); double ez = entity.getZ();

                        if (!big && cnt1 >= 12.0) handleExplosion(world, ex, ey, ez, (float) (2.0 * range));

                        if (!big || cnt1 <= 30.0) {
                            entity.getPersistentData().putDouble("Damage", damage);
                            entity.getPersistentData().putDouble("Range", 8.0 * range);
                            if (big) {
                                entity.getPersistentData().putDouble("knockback", 2.0);
                            } else {
                                entity.getPersistentData().putDouble("knockback", entity.getPersistentData().getDouble("cnt6") >= 2.0 ? 1.0 : 0.1);
                            }
                            entity.getPersistentData().putDouble("effectConfirm", 2.0);
                            if (big) entity.getPersistentData().putBoolean("betrayal", true);
                            RangeAttackProcedure.execute(world, ex, ey, ez, entity);
                        }

                        if (!big || cnt1 % 4.0 == 1.0) {
                            entity.getPersistentData().putDouble("BlockRange", 3.0 * range);
                            entity.getPersistentData().putDouble("BlockDamage", 18.0 * power);
                            entity.getPersistentData().putBoolean("noParticle", true);
                            entity.getPersistentData().putBoolean("noEffect", true);
                            entity.getPersistentData().putBoolean("ExtinctionBlock", true);
                            BlockDestroyAllDirectionProcedure.execute(world, ex, ey, ez, entity);
                        }

                        if (big) break;
                        if (entity.getPersistentData().getBoolean("Stop")) { entity.getPersistentData().putBoolean("Stop", false); break; }

                        if (world instanceof ServerLevel _level) {
                            _level.sendParticles((SimpleParticleType) JujutsucraftModParticleTypes.PARTICLE_THUNDER_PURPLE.get(), ex, ey, ez, (int) (5.0 * range), range, range, range, 0.05);
                        }

                        teleportWithSync(entity, entity.getX() + xp, entity.getY() + yp, entity.getZ() + zp);
                    }

                    entity.setDeltaMovement(big ? Vec3.ZERO : new Vec3(entity.getPersistentData().getDouble("x_power"), entity.getPersistentData().getDouble("y_power"), entity.getPersistentData().getDouble("z_power")));
                    entity.getPersistentData().putDouble("cnt_life", entity.getPersistentData().getDouble("cnt_life") + 1.0);
                    double cntLife = entity.getPersistentData().getDouble("cnt_life");

                    if (entity instanceof LivingEntity _livSize && _livSize.getAttribute(JujutsucraftModAttributes.SIZE.get()).getBaseValue() >= 80.0) {
                        if (cntLife >= 500.0 && !entity.level().isClientSide()) entity.discard();
                    } else {
                        if (cnt1 >= (big ? 80.0 : 30.0) && !entity.level().isClientSide()) entity.discard();
                    }
                }
            }
        }
    }

    @Unique
    private static void teleportWithSync(Entity entity, double x, double y, double z) {
        entity.teleportTo(x, y, z);
        if (entity instanceof ServerPlayer _sp) {
            _sp.connection.teleport(x, y, z, entity.getYRot(), entity.getXRot());
        }
    }

    @Unique
    private static void handleExplosion(LevelAccessor world, double x, double y, double z, float power) {
        if (world instanceof Level _level && !_level.isClientSide()) {
            boolean griefing = _level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            boolean pvp = _level.getGameRules().getBoolean(JujutsucraftModGameRules.JUJUTSUPVP);
            _level.explode(null, x, y, z, (griefing && pvp) ? power : 0.0F, (griefing && pvp) ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        }
    }

    @Unique
    private static void playSound(LevelAccessor world, double x, double y, double z, String sound, float volume, float pitch) {
        if (world instanceof Level _level) {
            SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(sound));
            if (event != null) {
                if (!_level.isClientSide()) _level.playSound(null, BlockPos.containing(x, y, z), event, SoundSource.NEUTRAL, volume, pitch);
                else _level.playLocalSound(x, y, z, event, SoundSource.NEUTRAL, volume, pitch, false);
            }
        }
    }
}
