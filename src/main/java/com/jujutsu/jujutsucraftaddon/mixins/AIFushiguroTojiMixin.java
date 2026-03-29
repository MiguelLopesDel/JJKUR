package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModParticleTypes;
import com.jujutsu.jujutsucraftaddon.procedures.DomainBreakerTojiProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.SixEyesCutProcedure;
import net.mcreator.jujutsucraft.entity.FushiguroTojiBugEntity;
import net.mcreator.jujutsucraft.entity.FushiguroTojiEntity;
import net.mcreator.jujutsucraft.entity.GojoSatoruEntity;
import net.mcreator.jujutsucraft.entity.GojoSatoruSchoolDaysEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.AIActiveProcedure;
import net.mcreator.jujutsucraft.procedures.AIFushiguroTojiProcedure;
import net.mcreator.jujutsucraft.procedures.CalculateAttackProcedure;
import net.mcreator.jujutsucraft.procedures.ResetCounterProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AIFushiguroTojiProcedure.class, priority = -10000)
public class AIFushiguroTojiMixin {

    @Inject(method = "execute", at = @At("HEAD"), remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        if (entity.isAlive()) {
            // Walking on water logic (Original Addon)
            BlockPos belowPos = entity.blockPosition().below();
            boolean isJustAboveWater = (world.getBlockState(BlockPos.containing(entity.getX(), entity.getY() - 1, entity.getZ())).getBlock() instanceof LiquidBlock);
            if (isJustAboveWater && !entity.isInWater()) {
                if (entity.getDeltaMovement().y() <= 0) {
                    entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                    entity.setOnGround(true);
                    entity.setPos(entity.getX(), belowPos.getY() + 1.0, entity.getZ());
                }
            }

            if (entity instanceof LivingEntity _liv) {
                // Active AI check
                if (!_liv.hasEffect(JujutsucraftaddonModMobEffects.QUAKE.get())) {
                    AIActiveProcedure.execute(world, x, y, z, entity);
                }

                // Addon Strength Buff (Amp 29)
                if (!_liv.hasEffect(MobEffects.DAMAGE_BOOST)) {
                    if (!_liv.level().isClientSide()) {
                        _liv.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 29, false, false));
                    }
                }

                // Resistance Buff (Amp 3)
                if (!_liv.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                    if (!_liv.level().isClientSide()) {
                        _liv.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3, false, false));
                    }
                }

                // Physical Gifted Buff (Amp 4)
                if (!_liv.hasEffect(JujutsucraftModMobEffects.PHYSICAL_GIFTED_EFFECT.get())) {
                    if (!_liv.level().isClientSide()) {
                        _liv.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.PHYSICAL_GIFTED_EFFECT.get(), Integer.MAX_VALUE, 4, false, false));
                    }
                }

                LivingEntity target = (_liv instanceof Mob _mob) ? _mob.getTarget() : null;

                if (target != null) {
                    entity.getPersistentData().putDouble("cnt_x", entity.getPersistentData().getDouble("cnt_x") + 1.0);

                    if (entity.getPersistentData().getDouble("cnt_x") > 10.0 && entity.getPersistentData().getDouble("skill") == 0.0 &&
                            !_liv.hasEffect(JujutsucraftaddonModMobEffects.QUAKE.get())) {

                        // Domain Breaker (Inverted Spear)
                        if (target.hasEffect(JujutsucraftModMobEffects.DOMAIN_EXPANSION.get()) &&
                                (_liv.getMainHandItem().getItem() == JujutsucraftModItems.INVERTED_SPEAR_OF_HEAVEN.get())) {
                            DomainBreakerTojiProcedure.execute(world, x, y, z, entity);
                        }

                        // Six Eyes Cut (Armoury Chestplate)
                        if (_liv.getItemBySlot(EquipmentSlot.CHEST).getItem() == JujutsucraftModItems.CURSED_SPIRIT_ARMOURY_CHESTPLATE.get()) {
                            if (Math.random() < (1.0 / 90.0)) {
                                SixEyesCutProcedure.execute(world, x, y, z, entity);
                            }
                        }

                        // Playful Cloud Special (Toji Bug Entity)
                        if (_liv.getMainHandItem().getItem() == JujutsucraftModItems.PLAYFUL_CLOUD.get() && entity instanceof FushiguroTojiBugEntity) {
                            if (Math.random() < (1.0 / 80.0)) {
                                executeSpecialAttack(world, entity, target);
                            }
                        }

                        // Split Soul Katana Special
                        if (_liv.getMainHandItem().getItem() == JujutsucraftModItems.SPLIT_SOUL_KATANA.get()) {
                            if (Math.random() < (1.0 / 80.0)) {
                                if (entity instanceof FushiguroTojiEntity _toji) {
                                    if (!_toji.animationprocedure.equals("splitsoulpierce")) {
                                        _toji.setAnimation("splitsoulpierce");
                                    }
                                }
                                executeSpecialAttack(world, entity, target);
                            }
                        }

                        entity.getPersistentData().putDouble("cnt_x", 5.0);
                        ResetCounterProcedure.execute(entity);

                        // v43 Signature Update (Removed x, y, z)
                        CalculateAttackProcedure.execute(world, entity);

                        // Armoury switching
                        if (entity instanceof FushiguroTojiEntity) {
                            handleWeaponSwitching(world, entity, target);
                        }
                    }
                } else {
                    entity.getPersistentData().putDouble("cnt_x", 0.0);
                }
            }
        }
    }

    @Unique
    private static void executeSpecialAttack(LevelAccessor world, Entity entity, LivingEntity target) {
        entity.teleportTo(target.getX(), target.getY() + 1, target.getZ() - 1);
        if (entity instanceof ServerPlayer _serverPlayer) {
            _serverPlayer.connection.teleport(target.getX(), target.getY() + 1, target.getZ() - 1, entity.getYRot(), entity.getXRot());
        }
        if (world instanceof ServerLevel _level) {
            _level.sendParticles(JujutsucraftaddonModParticleTypes.BLOOD_RED.get(), target.getX(), target.getY(), target.getZ(), 2, 0, 0, 0, 1);
        }
        if (!target.level().isClientSide()) {
            target.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.RCT_CUT.get(), 60, 1, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 1, false, false));
            target.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.UNSTABLE.get(), 60, 1, false, false));
        }
        target.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("jujutsucraft:damage_curse")))), 40);
    }

    @Unique
    private static void handleWeaponSwitching(LevelAccessor world, Entity entity, LivingEntity target) {
        if (!entity.getPersistentData().getBoolean("Armoury")) {
            entity.getPersistentData().putBoolean("Armoury", true);
            ItemStack chestplate = new ItemStack(JujutsucraftModItems.CURSED_SPIRIT_ARMOURY_CHESTPLATE.get());
            if (entity instanceof Player _player) {
                _player.getInventory().armor.set(2, chestplate);
                _player.getInventory().setChanged();
            } else if (entity instanceof LivingEntity _liv) {
                _liv.setItemSlot(EquipmentSlot.CHEST, chestplate);
            }
            entity.getPersistentData().putBoolean("HasWeapon1", true);
            entity.getPersistentData().putBoolean("HasWeapon2", true);
            entity.getPersistentData().putBoolean("HasWeapon3", true);
        }

        if (entity.getPersistentData().getDouble("skill") != 0.0) {
            ItemStack armor = (entity instanceof LivingEntity _liv) ? _liv.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY;
            if (armor.getItem() == JujutsucraftModItems.CURSED_SPIRIT_ARMOURY_CHESTPLATE.get()) {
                CompoundTag nbt = entity.getPersistentData();
                LivingEntity _liv = (LivingEntity) entity;

                nbt.putBoolean("HasWeapon1", nbt.getBoolean("HasWeapon1") || _liv.getMainHandItem().getItem() == JujutsucraftModItems.PLAYFUL_CLOUD.get());
                nbt.putBoolean("HasWeapon2", nbt.getBoolean("HasWeapon2") || _liv.getMainHandItem().getItem() == JujutsucraftModItems.INVERTED_SPEAR_OF_HEAVEN.get());
                nbt.putBoolean("HasWeapon3", nbt.getBoolean("HasWeapon3") || _liv.getMainHandItem().getItem() == JujutsucraftModItems.SPLIT_SOUL_KATANA.get());

                ItemStack setItemA = ItemStack.EMPTY;
                if (target.hasEffect(JujutsucraftModMobEffects.INFINITY_EFFECT.get()) || target instanceof GojoSatoruSchoolDaysEntity || target instanceof GojoSatoruEntity) {
                    if (_liv.getMainHandItem().getItem() != JujutsucraftModItems.INVERTED_SPEAR_OF_HEAVEN.get() && nbt.getBoolean("HasWeapon2")) {
                        nbt.putBoolean("HasWeapon2", false);
                    }
                    setItemA = new ItemStack(JujutsucraftModItems.INVERTED_SPEAR_OF_HEAVEN.get());
                } else {
                    for (int i = 0; i < 16; i++) {
                        double rnd = Math.ceil(Math.random() * 3.0);
                        if (rnd == 1.0 && nbt.getBoolean("HasWeapon1")) {
                            nbt.putBoolean("HasWeapon1", false);
                            setItemA = new ItemStack(JujutsucraftModItems.PLAYFUL_CLOUD.get());
                        } else if (rnd == 2.0 && nbt.getBoolean("HasWeapon2")) {
                            if (!target.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:no_curse_power")))) {
                                if (!(target instanceof Player _p && ((JujutsucraftModVariables.PlayerVariables) _p.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables())).PlayerCursePowerFormer < 100.0)) {
                                    nbt.putBoolean("HasWeapon2", false);
                                    setItemA = new ItemStack(JujutsucraftModItems.INVERTED_SPEAR_OF_HEAVEN.get());
                                }
                            }
                        } else if (rnd == 3.0 && nbt.getBoolean("HasWeapon3")) {
                            nbt.putBoolean("HasWeapon3", false);
                            setItemA = new ItemStack(JujutsucraftModItems.SPLIT_SOUL_KATANA.get());
                        }
                        if (!setItemA.isEmpty()) break;
                    }
                }

                if (!setItemA.isEmpty()) {
                    _liv.setItemInHand(InteractionHand.MAIN_HAND, setItemA);
                    if (_liv instanceof Player _p) _p.getInventory().setChanged();
                }
            }
        }
    }
}
