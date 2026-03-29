package com.jujutsu.jujutsucraftaddon.mixins;

import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.KugisakiNailProcedure;
import net.mcreator.jujutsucraft.procedures.KugisakiRightClickProcedure;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(value = KugisakiRightClickProcedure.class, priority = -10000)
public abstract class KugisakiRightClickProcedureMixin {

    /**
     * @author Satushi
     * @reason Modify Kugisaki Right Click to buff it and makes more damage
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack, CallbackInfo ci) {
        ci.cancel();

        if (entity == null) return;

        // 1. Condition Check: Not using another skill and not in combat cooldown
        boolean canUse = entity.getPersistentData().getDouble("skill") == 0.0;
        if (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get())) {
            canUse = false;
        }

        if (!canUse) {
            if (entity instanceof Player _player && !_player.level().isClientSide()) {
                _player.displayClientMessage(Component.literal(Component.translatable("jujutsu.message.dont_use").getString()), true);
            }
            return;
        }

        // 2. Resource Check: Nails and Curse Power
        AtomicReference<Boolean> hasNail = new AtomicReference<>(false);
        entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (handler.getStackInSlot(i).getItem() == JujutsucraftModItems.NAIL.get()) {
                    hasNail.set(true);
                    break;
                }
            }
        });

        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
            boolean isCreative = entity instanceof Player _player && _player.getAbilities().instabuild;
            boolean hasPower = (vars.PlayerCursePower + vars.PlayerCursePowerChange) >= 10.0;

            if ((hasNail.get() && hasPower) || isCreative) {
                // 3. Execution: Deduct power and fire nail
                if (!isCreative) {
                    vars.PlayerCursePowerChange -= 10.0;
                    vars.syncPlayerVariables(entity);
                }

                KugisakiNailProcedure.execute(world, x, y, z, entity);

                if (entity instanceof Player _player) {
                    _player.getCooldowns().addCooldown(itemstack.getItem(), 3);
                }
            } else {
                // 4. Failure message
                if (entity instanceof Player _player && !_player.level().isClientSide()) {
                    _player.displayClientMessage(Component.literal(Component.translatable("jujutsu.message.dont_use").getString()), true);
                }
            }
        });
    }
}
