package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import net.mcreator.jujutsucraft.entity.DagonEntity;
import net.mcreator.jujutsucraft.entity.UroTakakoEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuardEffectStartedappliedProcedure.class, priority = -10000)
public abstract class GuardEffectStartAppliedMixin {

    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity != null) {
            double num1 = 0.0;
            double num2 = 0.0;
            double x_pos = 0.0;
            double y_pos = 0.0;
            double z_pos = 0.0;
            double old_skill = 0.0;

            if (entity.isAlive()) {
                if (!(entity instanceof Player)) {
                    entity.setShiftKeyDown(true);
                }

                if (LogicStartPassiveProcedure.execute(entity)) {
                    LivingEntity _livEnt = (entity instanceof LivingEntity) ? (LivingEntity) entity : null;

                    if (_livEnt != null && !_livEnt.hasEffect((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get())) {
                        JujutsucraftModVariables.PlayerVariables pVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
                        num1 = pVars.PlayerCurseTechnique;
                        num2 = pVars.PlayerCurseTechnique2;
                        old_skill = entity.getPersistentData().getDouble("skill");

                        if (_livEnt.hasEffect((MobEffect) JujutsucraftModMobEffects.SUKUNA_EFFECT.get())) {
                            entity.getPersistentData().putDouble("skill", 105.0);
                            int amp = _livEnt.hasEffect((MobEffect) JujutsucraftModMobEffects.GUARD.get()) ? _livEnt.getEffect((MobEffect) JujutsucraftModMobEffects.GUARD.get()).getAmplifier() : 0;
                            entity.getPersistentData().putDouble("Damage", 15.0 + amp * 3.0);
                            entity.getPersistentData().putDouble("Range", entity.getBbWidth() + 32.0F);
                            entity.getPersistentData().putDouble("effect", 2.0);
                            entity.getPersistentData().putDouble("knockback", 0.25);
                            entity.getPersistentData().putDouble("projectile_type", 1.0);
                            entity.getPersistentData().putBoolean("onlyRanged", true);
                            RangeAttackProcedure.execute(world, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), entity);
                            entity.getPersistentData().putBoolean("onlyRanged", false);
                        }

                        if (num1 == 8.0 || num2 == 8.0 || (entity instanceof DagonEntity _dagon && (Integer) _dagon.getEntityData().get(DagonEntity.DATA_form) > 0)) {
                            entity.getPersistentData().putDouble("skill", 805.0);
                            int amp = _livEnt.hasEffect((MobEffect) JujutsucraftModMobEffects.GUARD.get()) ? _livEnt.getEffect((MobEffect) JujutsucraftModMobEffects.GUARD.get()).getAmplifier() : 0;
                            entity.getPersistentData().putDouble("Damage", (6.0 + amp * 3.0) * 0.25);
                            
                            double randYaw = Math.toRadians(Math.random() * 360.0);
                            double radius = entity.getBbWidth() + 1.5;

                            for (int index0 = 0; index0 < 72; index0++) {
                                if (Math.random() < 0.5) {
                                    x_pos = entity.getX() + Math.sin(randYaw) * radius;
                                    y_pos = entity.getY() + entity.getBbHeight() * 0.5;
                                    z_pos = entity.getZ() + Math.cos(randYaw) * radius;
                                    if (world instanceof ServerLevel _level) {
                                        _level.getServer().getCommands().performPrefixedCommand(
                                                (new CommandSourceStack(CommandSource.NULL, new Vec3(x_pos, y_pos, z_pos), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)).withSuppressedOutput(),
                                                "particle jujutsucraft:particle_water_no_gravity ~ ~ ~ 0.1 0.1 0.1 0 1 force");
                                    }
                                }
                                randYaw += Math.toRadians(Math.random() * 10.0);
                            }

                            entity.getPersistentData().putDouble("Range", (entity.getBbWidth() + 1.5) * 2.0 + 3.0);
                            entity.getPersistentData().putDouble("knockback", 0.25);
                            entity.getPersistentData().putDouble("projectile_type", 1.0);
                            entity.getPersistentData().putBoolean("onlyRanged", true);
                            RangeAttackProcedure.execute(world, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), entity);
                            entity.getPersistentData().putBoolean("onlyRanged", false);
                            entity.getPersistentData().putDouble("knockback", 0.25);
                            entity.getPersistentData().putDouble("effect", 1.0);
                            KnockbackProcedure.execute(world, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), entity);
                            entity.getPersistentData().putDouble("knockback", 0.0);
                            entity.getPersistentData().putDouble("effect", 0.0);
                        }

                        entity.getPersistentData().putDouble("skill", old_skill);

                        boolean isWukong = _livEnt.getItemBySlot(EquipmentSlot.CHEST).getItem() == JujutsucraftaddonModItems.WUKONG_SET_CHESTPLATE.get().asItem();
                        if (num1 == 38.0 || num2 == 38.0 || isWukong || entity instanceof UroTakakoEntity) {
                            UroCounterProcedure.execute(world, entity);
                        }
                    }
                }

                if (entity.getPersistentData().getDouble("skill") == 0.0) {
                    GuardSetDamageProcedure.execute(entity);
                }
            }
        }
    }
}
