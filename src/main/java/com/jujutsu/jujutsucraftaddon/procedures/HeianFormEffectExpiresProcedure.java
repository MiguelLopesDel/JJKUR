package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public class HeianFormEffectExpiresProcedure {

    public static void execute(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) return;

        if (livingEntity instanceof Player player) {
            player.getInventory().clearContent();
        } else {
            livingEntity.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
            livingEntity.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            livingEntity.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }

        MobEffect heianEffect = JujutsucraftaddonModMobEffects.HEIAN_FORM.get();
        for (MobEffectInstance effectInstance : new ArrayList<>(livingEntity.getActiveEffects())) {
            if (effectInstance.getEffect() != heianEffect) {
                livingEntity.removeEffect(effectInstance.getEffect());
            }
        }
    }
}