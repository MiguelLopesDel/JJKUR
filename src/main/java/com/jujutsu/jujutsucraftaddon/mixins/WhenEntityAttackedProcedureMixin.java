package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModEnchantments;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.AnimationDodgeProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.SukunaAttackAnimationsProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.SwapTodoTarget;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.entity.FushiguroTojiBugEntity;
import net.mcreator.jujutsucraft.entity.FushiguroTojiEntity;
import net.mcreator.jujutsucraft.entity.TodoAoiEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.WhenEntityAttacked2Procedure;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoEntity;

@Mixin(value = WhenEntityAttacked2Procedure.class, priority = -10000)
public abstract class WhenEntityAttackedProcedureMixin {

    /**
     * @author Satushi / SURGICAL COMPATIBILITY
     * @reason Refactored to only inject Addon dodges/animations and let v43 base logic handle damage/defense.
     * This fixes incompatibilities with Takaba, Kirin, and v43 Guard Success system.
     */
    @Inject(at = @At("HEAD"), method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;D)V", remap = false, cancellable = true)
    private static void execute(Event event, LevelAccessor world, DamageSource damagesource, Entity entity, Entity immediatesourceentity, Entity sourceentity, double amount, CallbackInfo ci) {
        if (damagesource == null || entity == null || sourceentity == null) return;

        SukunaAttackAnimationsProcedure.execute(sourceentity, entity, world);
        handleTojiAnimations(sourceentity, amount);

        if (checkAndApplyAddonDodges(event, world, entity, sourceentity, amount)) {
            ci.cancel();
        }
    }

    private static boolean checkAndApplyAddonDodges(Event event, LevelAccessor world, Entity entity, Entity sourceentity, double amount) {
        if (!(entity instanceof LivingEntity living)) return false;

        JujutsucraftModVariables.PlayerVariables baseVars = living.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
        JujutsucraftaddonModVariables.PlayerVariables addonVars = living.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);

        if (baseVars == null || addonVars == null) return false;

        if (baseVars.PlayerCurseTechnique == TechniqueIDs.TODO && baseVars.PlayerCursePower >= 1000.0 && Math.random() < 1.0 / 60.0) {
            SwapTodoTarget.execute(entity, world, sourceentity);
            AnimationDodgeProcedure.execute(world, entity);
            cancelOriginalEvent(event);
            return true;
        }

        if (baseVars.PlayerCurseTechnique == TechniqueIDs.URO && Math.random() < (addonVars.Clans.equals("Fujiwara") ? 1.0 / 50.0 : 1.0 / 120.0)) {
            applyDodgeRigor(world, entity, 3810.0);
            cancelOriginalEvent(event);
            return true;
        }

        if (baseVars.PlayerCurseTechnique == TechniqueIDs.MAKI && baseVars.PlayerCursePower == 0.0 && Math.random() < (addonVars.Clans.equals("Rejected Zenin") ? 1.0 / 40.0 : 1.0 / 80.0)) {
            AnimationDodgeProcedure.execute(world, entity);
            cancelOriginalEvent(event);
            return true;
        }

        ItemStack chest = living.getItemBySlot(EquipmentSlot.CHEST);
        int uiLevel = EnchantmentHelper.getItemEnchantmentLevel(JujutsucraftaddonModEnchantments.ULTRA_INSTINCT.get(), chest);
        if (uiLevel > 0 && Math.random() < uiLevel / 100.0) {
            AnimationDodgeProcedure.execute(world, entity);
            cancelOriginalEvent(event);
            return true;
        }

        ItemStack feet = living.getItemBySlot(EquipmentSlot.FEET);
        if (feet.getItem() == JujutsucraftaddonModItems.WUKONG_SET_BOOTS.get().asItem() && Math.random() < 1.0 / 100.0) {
            if (!(entity instanceof GeoEntity)) {
                AnimationDodgeProcedure.execute(world, entity);
                cancelOriginalEvent(event);
                return true;
            }
        }

        return false;
    }

    private static void handleTojiAnimations(Entity sourceentity, double amount) {
        if (sourceentity instanceof FushiguroTojiBugEntity _tojiBug && amount >= 100) {
            _tojiBug.setAnimation("playful" + Mth.nextInt(RandomSource.create(), 1, 4));
        } else if (sourceentity instanceof FushiguroTojiEntity _toji && amount >= 100) {
            ItemStack sourceHand = (sourceentity instanceof LivingEntity _liv) ? _liv.getMainHandItem() : ItemStack.EMPTY;
            if (sourceHand.getItem() == JujutsucraftModItems.PLAYFUL_CLOUD.get()) {
                _toji.setAnimation("playful" + Mth.nextInt(RandomSource.create(), 1, 4));
            }
        }
    }

    private static void applyDodgeRigor(LevelAccessor world, Entity entity, double skill) {
        CompoundTag nbt = entity.getPersistentData();
        nbt.putDouble("skill", skill);
        nbt.putDouble("cnt6", 4.0);
        double yaw = Math.toRadians(entity.getYRot() + 90.0F);
        double pitch = Math.toRadians(entity.getXRot());
        double dist = 2.0 + entity.getBbWidth();
        nbt.putDouble("x_pos", entity.getX() + Math.cos(yaw) * Math.cos(pitch) * dist);
        nbt.putDouble("y_pos", entity.getY() + (double) entity.getBbHeight() * 0.75 + Math.sin(pitch) * -1.0 * (double) (2.0 + entity.getBbWidth()));
        nbt.putDouble("z_pos", entity.getZ() + Math.sin(yaw) * Math.cos(pitch) * dist);
        if (entity instanceof LivingEntity _liv && !_liv.level().isClientSide()) _liv.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), 60, 1, false, false));
        AnimationDodgeProcedure.execute(world, entity);
    }

    private static void cancelOriginalEvent(Event event) {
        if (event != null && event.isCancelable()) {
            event.setCanceled(true);
        }
    }
}
