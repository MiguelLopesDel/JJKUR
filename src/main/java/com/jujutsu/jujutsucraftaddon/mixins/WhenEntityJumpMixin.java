package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.WhenPlayerJumpProcedure;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WhenPlayerJumpProcedure.class, priority = -10000)
public abstract class WhenEntityJumpMixin {

    /**
     * @author Satushi
     * @reason Refactored for v43. Adds Wukong set bonuses for jump and fall control.
     */
    @Inject(at = @At("HEAD"), method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/entity/Entity;)V", remap = false)
    private static void execute(Event event, Entity entity, CallbackInfo ci) {
        if (entity instanceof LivingEntity _liv && !worldSideCheck(_liv)) {
            
            // Wukong Leggings: Double Jump
            ItemStack legs = _liv.getItemBySlot(EquipmentSlot.LEGS);
            if (legs.getItem() == JujutsucraftaddonModItems.WUKONG_SET_LEGGINGS.get().asItem()) {
                _liv.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.DOUBLE_JUMP_EFFECT.get(), 120, 4, false, false));
            }

            // Wukong Boots: Slow Falling
            ItemStack boots = _liv.getItemBySlot(EquipmentSlot.FEET);
            if (boots.getItem() == JujutsucraftaddonModItems.WUKONG_SET_BOOTS.get().asItem()) {
                _liv.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 4, false, false));
            }
        }
    }

    private static boolean worldSideCheck(Entity entity) {
        return entity.level().isClientSide();
    }
}
