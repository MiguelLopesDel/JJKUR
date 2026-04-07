package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.entity.DismantleEntity;
import com.jujutsu.jujutsucraftaddon.entity.DismantleVariantEntity;
import com.jujutsu.jujutsucraftaddon.entity.WorldSlashFinalEntity;
import com.jujutsu.jujutsucraftaddon.entity.WorldSlashVariantEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModEntities;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.BiFunction;

public class PassiveSukunaProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(baseVars -> {
            entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                boolean hasWSEffect = (entity instanceof LivingEntity living && living.hasEffect(JujutsucraftaddonModMobEffects.WORLD_SLASH_EFFECT.get()));

                if (!hasWSEffect) {
                    handleNormalState(world, x, y, z, entity, baseVars, addonVars);
                } else {
                    handleEnhancedState(world, x, y, z, entity, baseVars, addonVars);
                }
            });
        });
    }

    private static void handleNormalState(LevelAccessor world, double x, double y, double z, Entity entity, JujutsucraftModVariables.PlayerVariables base, JujutsucraftaddonModVariables.PlayerVariables addon) {
        if (addon.Mode.equals("Dismantle")) {
            if (base.PlayerCursePower <= 100 || base.BodyItem.getCount() < 5.0) return;

            if (entity.onGround()) {
                executeDismantle(world, x, y, z, entity, addon.OutputLevel, false, false);
            } else {
                playClientAnimation(entity, "dismantleback" + Mth.nextInt(RandomSource.create(), 1, 2));
                executeDismantle(world, x, y, z, entity, addon.OutputLevel, true, false);
            }
            consumeEnergy(entity, 50);

        } else if (addon.Mode.equals("Cleave")) {
            if (base.PlayerCursePower <= 500 || base.BodyItem.getCount() < 14.0) return;

            if (!entity.onGround()) playClientAnimation(entity, "dismantleback2");
            executeCleave(world, x, y, z, entity, addon.OutputLevel, false);
            consumeEnergy(entity, 500);
        }
    }

    private static void handleEnhancedState(LevelAccessor world, double x, double y, double z, Entity entity, JujutsucraftModVariables.PlayerVariables base, JujutsucraftaddonModVariables.PlayerVariables addon) {
        ItemStack chest = (entity instanceof LivingEntity living ? living.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY);
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(chest.getItem());
        String armorName = registryName != null ? registryName.toString() : "";
        boolean hasArmor = armorName.equals("jujutsucraft:sukuna_body_chestplate") || chest.getItem() == JujutsucraftaddonModItems.SUKUNA_ARMOR_THREE_CHESTPLATE.get();

        if (!hasArmor) return;

        if (addon.Mode.equals("Dismantle")) {
            if (base.PlayerCursePower <= 100 || base.BodyItem.getCount() < 5.0) return;

            if (entity.onGround()) {
                executeDismantle(world, x, y, z, entity, addon.OutputLevel, false, true);
            } else {
                playClientAnimation(entity, "dismantleback" + Mth.nextInt(RandomSource.create(), 1, 2));
                executeDismantle(world, x, y, z, entity, addon.OutputLevel, true, true);
            }
            consumeEnergy(entity, 50);

        } else if (addon.Mode.equals("Cleave")) {
            if (!(entity instanceof ServerPlayer && world instanceof ServerLevel)) return;
            if (base.PlayerCursePower <= 500 || base.BodyItem.getCount() < 14.0) return;

            if (!entity.onGround()) playClientAnimation(entity, "dismantleback2");
            executeCleave(world, x, y, z, entity, addon.OutputLevel, true);
            consumeEnergy(entity, 500);
        }
    }

    private static void executeDismantle(LevelAccessor world, double x, double y, double z, Entity entity, double output, boolean isAir, boolean isWS) {
        String sound = isAir ? "jujutsucraftaddon:kai" : "jujutsucraftaddon:dismantle";
        BiFunction<Level, Entity, AbstractArrow> factory = (lvl, shooter) -> {
            if (isWS) return new WorldSlashVariantEntity(JujutsucraftaddonModEntities.WORLD_SLASH_VARIANT.get(), (LivingEntity) shooter, lvl);
            return isAir ? new DismantleVariantEntity(JujutsucraftaddonModEntities.DISMANTLE_VARIANT.get(), (LivingEntity) shooter, lvl)
                         : new DismantleEntity(JujutsucraftaddonModEntities.DISMANTLE.get(), (LivingEntity) shooter, lvl);
        };

        if (Math.random() < 0.04) {
            spawnSphere(world, x, y, z, entity, (int) output - 1, output, factory, sound);
        } else {
            spawnProjectile(world, entity.getX(), entity.getEyeY() - 0.1, entity.getZ(), entity, output, factory);
            playSound(world, x, y, z, sound);
        }
        applyAnimationEffect(entity);
    }

    private static void executeCleave(LevelAccessor world, double x, double y, double z, Entity entity, double output, boolean isWS) {
        int radius = (int) (output * 2) - 1;
        
        BiFunction<Level, Entity, AbstractArrow> f1 = (lvl, shooter) -> isWS ? new WorldSlashVariantEntity(JujutsucraftaddonModEntities.WORLD_SLASH_VARIANT.get(), (LivingEntity) shooter, lvl) 
                                                                            : new DismantleVariantEntity(JujutsucraftaddonModEntities.DISMANTLE_VARIANT.get(), (LivingEntity) shooter, lvl);
        BiFunction<Level, Entity, AbstractArrow> f2 = (lvl, shooter) -> isWS ? new WorldSlashFinalEntity(JujutsucraftaddonModEntities.WORLD_SLASH_FINAL.get(), (LivingEntity) shooter, lvl) 
                                                                            : new DismantleEntity(JujutsucraftaddonModEntities.DISMANTLE.get(), (LivingEntity) shooter, lvl);

        spawnSphere(world, x, y, z, entity, radius, output, f1, "jujutsucraftaddon:kai");
        spawnSphere(world, x, y, z, entity, radius, output, f2, "jujutsucraftaddon:dismantle");
        applyAnimationEffect(entity);
    }

    private static void spawnSphere(LevelAccessor world, double x, double y, double z, Entity entity, int radius, double output, BiFunction<Level, Entity, AbstractArrow> factory, String sound) {
        if (!(world instanceof ServerLevel)) return;
        for (int i = -radius; i <= radius; i++) {
            for (int xi = -radius; xi <= radius; xi++) {
                for (int zi = -radius; zi <= radius; zi++) {
                    double dist = (double)(xi * xi) / (radius * radius) + (double)(i * i) / (radius * radius) + (double)(zi * zi) / (radius * radius);
                    if (dist <= 1.0) {
                        spawnProjectile(world, x + xi, entity.getEyeY(), z + zi, entity, output, factory);
                    }
                }
            }
        }
        playSound(world, x, y, z, sound);
    }

    private static void spawnProjectile(LevelAccessor world, double px, double py, double pz, Entity owner, double output, BiFunction<Level, Entity, AbstractArrow> factory) {
        if (!(world instanceof ServerLevel sLevel)) return;
        com.jujutsu.jujutsucraftaddon.JujutsucraftaddonMod.logCall("PassiveSukuna_Projectile");
        AbstractArrow arrow = factory.apply(sLevel, owner);
        arrow.setOwner(owner);
        arrow.setBaseDamage(10 * output);
        arrow.setKnockback(0);
        arrow.setSilent(true);
        arrow.setPierceLevel((byte) 1);
        arrow.setPos(px, py, pz);
        arrow.shoot(owner.getLookAngle().x, owner.getLookAngle().y, owner.getLookAngle().z, 10, 0);
        sLevel.addFreshEntity(arrow);
    }

    private static void playSound(LevelAccessor world, double x, double y, double z, String soundPath) {
        if (world instanceof Level lvl) {
            ResourceLocation res = new ResourceLocation(soundPath);
            if (!world.isClientSide()) lvl.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(res), SoundSource.NEUTRAL, 1, 1);
            else lvl.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(res), SoundSource.NEUTRAL, 1, 1, false);
        }
    }

    private static void playClientAnimation(Entity entity, String animName) {
        if (entity.level().isClientSide() && entity instanceof AbstractClientPlayer player) {
            var anim = (ModifierLayer) PlayerAnimationAccess.getPlayerAssociatedData(player).get(new ResourceLocation("jujutsucraftaddon", "player_animation"));
            if (anim != null && !anim.isActive()) {
                anim.setAnimation(new KeyframeAnimationPlayer(PlayerAnimationRegistry.getAnimation(new ResourceLocation("jujutsucraftaddon", animName))));
            }
        }
    }

    private static void applyAnimationEffect(Entity entity) {
        if (entity instanceof LivingEntity living && !living.level().isClientSide()) {
            living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.ANIMATION.get(), 1, 1, false, false));
        }
    }

    private static void consumeEnergy(Entity entity, double amount) {
        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
            cap.PlayerCursePower -= amount;
            cap.syncPlayerVariables(entity);
        });
    }
}
