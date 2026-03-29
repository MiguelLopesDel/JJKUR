package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import net.mcreator.jujutsucraft.entity.FushiguroTojiBugEntity;
import net.mcreator.jujutsucraft.entity.FushiguroTojiEntity;
import net.mcreator.jujutsucraft.entity.SukunaFushiguroEntity;
import net.mcreator.jujutsucraft.entity.SukunaPerfectEntity;
import net.mcreator.jujutsucraft.procedures.SetTagProcedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SetTagProcedure.class, priority = -10000)
public abstract class SetTagSocererMixin {

    /**
     * @author Satushi
     * @reason Injects custom Addon clothing and awakening events without breaking v43 lore system.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false)
    private static void onExecuteHead(LevelAccessor world, Entity entity, CallbackInfo ci) {
        if (entity == null) return;

        // 1. Sukuna Fushiguro Custom Clothing
        if (entity instanceof SukunaFushiguroEntity _sukuna && !_sukuna.getEntityData().get(SukunaFushiguroEntity.DATA_perfect_mode)) {
            _sukuna.setItemSlot(EquipmentSlot.CHEST, new ItemStack(JujutsucraftaddonModItems.SUKUNA_COAT_BLACK.get()));
        }

        // 2. Sukuna Perfect Awakening Event
        if (entity instanceof SukunaPerfectEntity _perfect) {
            if (!"heianform".equals(_perfect.animationprocedure)) {
                _perfect.setAnimation("heianform");

                if (!_perfect.level().isClientSide()) {
                    _perfect.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.ANIMATION_HEIAN.get(), 40, 1, false, false));
                }

                // Broadcast Calamity Message and Effects
                Vec3 center = _perfect.position();
                for (Entity target : world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(15.0), e -> true)) {
                    if (!_perfect.level().isClientSide() && _perfect.getServer() != null) {
                        _perfect.getServer().getCommands().performPrefixedCommand(
                            new CommandSourceStack(CommandSource.NULL, center, _perfect.getRotationVector(), 
                            (ServerLevel) _perfect.level(), 4, _perfect.getName().getString(), _perfect.getDisplayName(), 
                            _perfect.level().getServer(), _perfect), "particle jjkueffects:red_awakening_2 ~ ~-1 ~ 0 0 0 1 1 force"
                        );
                    }

                    if (target instanceof Player _player && !_player.level().isClientSide()) {
                        _player.displayClientMessage(Component.literal("§lLike a Calamity, The Strongest Sorcerer from History, Awakens"), false);
                    }
                }
            }
        }

        // 3. Toji Awakening Event (Bug Entity)
        if (entity instanceof FushiguroTojiBugEntity _tojiBug) {
            if (!"tojiawakening".equals(_tojiBug.animationprocedure)) {
                _tojiBug.setAnimation("tojiawakening");
            }
            if (!_tojiBug.level().isClientSide()) {
                _tojiBug.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 2, false, false));
            }

            Vec3 center = _tojiBug.position();
            for (Entity target : world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(15.0), e -> true)) {
                if (target != _tojiBug) {
                    if (target instanceof LivingEntity _livTarget && !_livTarget.level().isClientSide()) {
                        _livTarget.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.QUAKE.get(), 380, 1, false, false));
                    }
                    if (target instanceof Player _player && !_player.level().isClientSide()) {
                        _player.displayClientMessage(Component.literal("§lThe One Who Left It All Behind... And His Overwhelming Intensity!"), false);
                    }
                }
            }
        } 
        // Standard Toji Regeneration
        else if (entity instanceof FushiguroTojiEntity _toji) {
            if (!_toji.level().isClientSide()) {
                _toji.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 2, false, false));
            }
        }
    }
}
