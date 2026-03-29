package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.entity.BulletNailEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.procedures.DamageFixProcedure;
import net.mcreator.jujutsucraft.procedures.KugisakiNailProcedure;
import net.mcreator.jujutsucraft.procedures.RotateEntityProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KugisakiNailProcedure.class, priority = -10000)
public abstract class KugisakiNailProcedureMixin {

    /**
     * @author Satushi
     * @reason Buffs Nails and make them don't cost always every shot
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();

        if (entity == null) return;

        if (entity instanceof LivingEntity _liv) {
            _liv.swing(InteractionHand.MAIN_HAND, true);
        }

        ItemStack mainHandItem = (entity instanceof LivingEntity _liv) ? _liv.getMainHandItem() : ItemStack.EMPTY;

        if (mainHandItem.getItem() == JujutsucraftModItems.HAMMER.get()) {
            if (world instanceof Level _level) {
                SoundEvent anvilSound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.anvil.place"));
                if (!_level.isClientSide()) {
                    _level.playSound(null, BlockPos.containing(x, y, z), anvilSound, SoundSource.NEUTRAL, 0.5F, 1.5F);
                } else {
                    _level.playLocalSound(x, y, z, anvilSound, SoundSource.NEUTRAL, 0.5F, 1.5F, false);
                }
            }

            // Addon Buff: Only 1% chance to consume a nail when using Hammer
            if (Math.random() < 0.01 && entity instanceof Player _player) {
                ItemStack nail = new ItemStack((ItemLike) JujutsucraftModItems.NAIL.get());
                _player.getInventory().clearOrCountMatchingItems(p -> nail.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
            }
        }

        if (entity instanceof Mob _mob) {
            LivingEntity _target = _mob.getTarget();
            if (_target != null) {
                double x_pos = _target.getX();
                double y_pos = _target.getY() + _target.getBbHeight() * 0.5;
                double z_pos = _target.getZ();
                RotateEntityProcedure.execute(x_pos, y_pos, z_pos, entity);
            }
        }

        entity.getPersistentData().putDouble("Damage", 0.5);
        DamageFixProcedure.execute(entity);

        Level worldLevel = entity.level();
        if (!worldLevel.isClientSide()) {
            BulletNailEntity _nail = new BulletNailEntity((EntityType<? extends BulletNailEntity>) JujutsucraftModEntities.BULLET_NAIL.get(), worldLevel);
            _nail.setOwner(entity);
            _nail.setBaseDamage(entity.getPersistentData().getDouble("Damage"));
            _nail.setKnockback(0);
            _nail.setSilent(true);
            _nail.setCritArrow(true);
            _nail.pickup = AbstractArrow.Pickup.ALLOWED;
            _nail.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
            _nail.shoot(entity.getLookAngle().x, entity.getLookAngle().y, entity.getLookAngle().z, 3.0F, 0.0F);
            worldLevel.addFreshEntity(_nail);
        }

        entity.getPersistentData().putDouble("skill", 0.0);
    }
}
