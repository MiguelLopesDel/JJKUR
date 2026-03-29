package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UroCounterProcedure.class, priority = -10000)
public class UroCounterProcedureMixin {

    /**
     * @author Satushi
     * @reason Refactored for v43. Implements dynamic counter radius and preserves Wukong Set visual bypass.
     */
    @Inject(method = "execute", at = @At("HEAD"), remap = false, cancellable = true)
    private static void execute(LevelAccessor world, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        // 1. Initial Neutralization Check
        if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.NEUTRALIZATION.get())) {
            return;
        }

        double x_pos = 0, y_pos = 0, z_pos = 0, yaw, pitch, dis, speed, num1, num2, num3, x_power, y_power, z_power;
        boolean logic_a = false, logic_b = false, success = false;

        // 2. Rotate to Target (v43 standard)
        Entity target = (entity instanceof Mob _mob) ? _mob.getTarget() : null;
        if (target instanceof LivingEntity) {
            RotateEntityProcedure.execute(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), entity);
        }

        yaw = entity.getYRot();
        pitch = entity.getXRot();
        dis = 6.0;

        // 3. Search for Refraction Target
        for (int i = 0; i < 8; i++) {
            ClipContext clip = new ClipContext(entity.getEyePosition(1.0F), entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(dis)), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity);
            Vec3 center = Vec3.atCenterOf(entity.level().clip(clip).getBlockPos());
            
            for (Entity found : world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(6.0), e -> true)) {
                if (entity != found && LogicAttackProcedure.execute(world, entity, found) && !found.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo")))) {
                    success = true;
                    x_pos = found.getX();
                    y_pos = found.getY() + found.getBbHeight() * 0.5;
                    z_pos = found.getZ();
                    break;
                }
            }
            if (success) break;
            dis += 6.0;
        }

        // 4. Projectile Deflection Logic (v43 Dynamic Radius)
        double counterRadius = 12.0F + Math.max(entity.getBbWidth() * 2.0F, entity.getBbHeight() * 2.0F) * 2.0F;
        Vec3 entityCenter = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());

        for (Entity found : world.getEntitiesOfClass(Entity.class, new AABB(entityCenter, entityCenter).inflate(counterRadius / 2.0), e -> true)) {
            boolean isAmmo = found.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo"))) || (found instanceof Projectile);
            boolean noMove = found.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo_no_move")));
            
            if (isAmmo && !noMove) {
                // Size safety check (v43)
                if (found.getBbWidth() * found.getBbWidth() + found.getBbHeight() <= (entity.getBbWidth() * entity.getBbWidth() + entity.getBbHeight()) * 8.0F) {
                    logic_a = true;
                    found.getPersistentData().putBoolean("betrayal", true);
                    if (found instanceof Projectile _proj && entity instanceof LivingEntity _livOwner) {
                        _proj.setOwner(_livOwner);
                    }

                    // Logic B determination (Counter-Attack direction)
                    if (entity instanceof Player _p) {
                        logic_b = _p.isShiftKeyDown();
                    } else {
                        int guardDur = (entity instanceof LivingEntity _livGuard && _livGuard.hasEffect((MobEffect) JujutsucraftModMobEffects.GUARD.get())) ? _livGuard.getEffect((MobEffect) JujutsucraftModMobEffects.GUARD.get()).getDuration() : 0;
                        logic_b = entity.getPersistentData().getDouble("cnt_uro") < 15.0 && guardDur < (guardDur > 10 ? 18 : 8);
                    }

                    speed = 3.0;
                    num1 = entity.getYRot() % 360.0F;
                    num2 = found.getYRot() % 360.0F;
                    num3 = Math.abs(num1 - num2);
                    found.getPersistentData().putString("OWNER_UUID", entity.getStringUUID());

                    if (logic_b || (num3 > 135.0 && num3 < 315.0)) {
                        RotateEntityProcedure.execute(entity.getX(), entity.getY() + entity.getBbHeight() * 0.9, entity.getZ(), found);
                        found.setYRot(found.getYRot() + (float)(GetDistanceIteratorProcedure.execute(entity, found) > 5.0 ? 45 : 90));
                        found.setXRot(found.getXRot());
                        found.setYBodyRot(found.getYRot());
                        found.setYHeadRot(found.getYRot());
                        found.yRotO = found.getYRot();
                        found.xRotO = found.getXRot();
                        if (found instanceof LivingEntity _livFound) {
                            _livValueRotation(_livFound, found.getYRot());
                        }

                        if (entity instanceof LivingEntity _livEnt && !entity.level().isClientSide()) {
                            _livEnt.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.GUARD.get(), 20, 0, false, false));
                        }
                    } else {
                        RotateEntityProcedure.execute(x_pos, y_pos, z_pos, found);
                    }

                    x_power = found.getLookAngle().x * speed;
                    y_power = found.getLookAngle().y * speed;
                    z_power = found.getLookAngle().z * speed;
                    found.setDeltaMovement(new Vec3(x_power, y_power, z_power));
                    found.getPersistentData().putDouble("x_power", x_power);
                    found.getPersistentData().putDouble("y_power", y_power);
                    found.getPersistentData().putDouble("z_power", z_power);
                }
            }
        }

        // 5. Player/Mob Counter cooldown and visuals
        if (entity instanceof Player _p) {
            double currentCnt = entity.getPersistentData().getDouble("cnt_uro") + 1.0;
            entity.getPersistentData().putDouble("cnt_uro", (currentCnt > 0.0) ? -5.0 : currentCnt);
            if (currentCnt <= 0.0) logic_a = false;
        } else {
            if (logic_a) {
                entity.getPersistentData().putDouble("cnt_uro", Math.max(entity.getPersistentData().getDouble("cnt_uro"), 1.0));
                if (entity instanceof LivingEntity _livC && !entity.level().isClientSide()) {
                    _livC.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get(), 5, 0));
                    _livC.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 9));
                    _livC.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_BACK_STEP.get(), 5, 9));
                    _livC.removeEffect((MobEffect) JujutsucraftModMobEffects.FLY_EFFECT.get());
                }
            }
            if (entity.getPersistentData().getDouble("cnt_uro") > 0.0) {
                double progress = entity.getPersistentData().getDouble("cnt_uro") + 1.0;
                entity.getPersistentData().putDouble("cnt_uro", (progress > 100.0) ? 0.0 : progress);
            }
        }

        // 6. Final Visuals & Wukong Set Bypass (JJKUR Feature)
        boolean isWukong = (entity instanceof LivingEntity _entGetArmor && _entGetArmor.getItemBySlot(EquipmentSlot.CHEST).getItem() == JujutsucraftaddonModItems.WUKONG_SET_CHESTPLATE.get().asItem());

        if (logic_a) {
            if (entity instanceof LivingEntity _livV && !entity.level().isClientSide()) {
                _livV.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 9));
            }

            if (!isWukong) {
                if (entity instanceof LivingEntity _livA) {
                    if (_livA.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get())) {
                        _livA.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_1.get()).setBaseValue(-8.0);
                    }
                    if (_livA.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get())) {
                        _livA.getAttribute((Attribute) JujutsucraftModAttributes.ANIMATION_2.get()).setBaseValue(1.0);
                    }
                }
                PlayAnimationProcedure.execute(world, entity);
            }
        }

        if (!isWukong) {
            entity.setYRot((float) yaw);
            entity.setXRot((float) pitch);
            entity.setYBodyRot(entity.getYRot());
            entity.setYHeadRot(entity.getYRot());
            entity.yRotO = entity.getYRot();
            entity.xRotO = entity.getXRot();
            if (entity instanceof LivingEntity _livFinal) {
                _livValueRotation(_livFinal, entity.getYRot());
            }
        }
    }

    private static void _livValueRotation(LivingEntity entity, float yaw) {
        entity.yBodyRotO = yaw;
        entity.yHeadRotO = yaw;
    }
}
