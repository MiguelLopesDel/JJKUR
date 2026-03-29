package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.CopiedCursedTechniqueRightclickedProcedure;
import net.mcreator.jujutsucraft.procedures.LocateRikaProcedure;
import net.mcreator.jujutsucraft.procedures.StartCursedTechniqueProcedure;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CopiedCursedTechniqueRightclickedProcedure.class, priority = -10000)
public abstract class CopiedCursedTechniqueMixin {

    /**
     * @author Satushi / Audit Fixed (Parity v43)
     * @reason Restores mandatory state resets, activation conditions (Rika/Domain), failed checks, and effects for v43.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack, CallbackInfo ci) {
        if (entity == null) return;

        // Cancelamento imediato para assumir autoridade total sobre o evento
        ci.cancel();

        // RESTAURADO: Reset inicial obrigatório (Classe Alvo Linha 25)
        entity.getPersistentData().putBoolean("PRESS_Z", false);

        double skillTagValue = itemstack.getOrCreateTag().getDouble("skill");
        if (skillTagValue > 0.0 && !itemstack.getOrCreateTag().getBoolean("Used")) {

            JujutsucraftModVariables.PlayerVariables pVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, (Direction) null).orElse(new JujutsucraftModVariables.PlayerVariables());

            // Validação de Técnica 5.0 (Cópia do Yuta)
            if (pVars.PlayerCurseTechnique != 5.0 && pVars.PlayerCurseTechnique2 != 5.0) {
                if (entity instanceof Player _player && !_player.level().isClientSide()) {
                    _player.displayClientMessage(Component.literal(Component.translatable("jujutsu.message.dont_use").getString()), true);
                }
                return;
            }

            // RESTAURADO: Checagem de Rika ou Domínio (Base) com checagem de Falha + Addon InfusedDomain
            boolean canUseBase = (LocateRikaProcedure.execute(world, entity)
                    || (entity instanceof LivingEntity _liv && _liv.hasEffect((MobEffect) JujutsucraftModMobEffects.DOMAIN_EXPANSION.get())))
                    && !entity.getPersistentData().getBoolean("Failed");

            boolean canUseAddon = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables()).InfusedDomain;

            if (!canUseBase && !canUseAddon) {
                if (entity instanceof Player _player && !_player.level().isClientSide()) {
                    _player.displayClientMessage(Component.literal(Component.translatable("jujutsu.message.dont_use").getString()), true);
                }
                return;
            }

            if (entity.getPersistentData().getDouble("skill") == 0.0) {
                // Lógica de InfusedDomain (Aprendizado Permanente do Addon)
                JujutsucraftaddonModVariables.PlayerVariables aVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());
                if (aVars.InfusedDomain) {
                    double newTechnique = (double) ((int) (skillTagValue / 100));
                    entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                        capability.PlayerCurseTechnique = newTechnique;
                        capability.PlayerCurseTechnique2 = newTechnique;
                        capability.syncPlayerVariables(entity);
                    });

                    if (entity instanceof LivingEntity _living && !_living.level().isClientSide())
                        _living.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.MANIFESTATION.get(), 6000, 0, false, false));

                    if (entity instanceof Player) {
                        itemstack.shrink(1);
                    }
                }

                itemstack.getOrCreateTag().putBoolean("used_item", true);
                StartCursedTechniqueProcedure.execute(world, x, y, z, entity);

                // REAFIRMAÇÃO DE ESTADO (Classe Alvo Linhas 101-106)
                double currentSkill = entity.getPersistentData().getDouble("skill");
                if (currentSkill > 0.0) {
                    // Garante que o NBT da entidade esteja sincronizado com a skill iniciada
                    entity.getPersistentData().putDouble("skill", currentSkill);

                    // APLICAÇÃO OBRIGATÓRIA do efeito CURSED_TECHNIQUE para o motor do mod base
                    if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
                        _entity.addEffect(new MobEffectInstance((MobEffect) JujutsucraftModMobEffects.CURSED_TECHNIQUE.get(), Integer.MAX_VALUE, 0, false, false));
                    }

                    entity.getPersistentData().putBoolean("PRESS_Z", true);
                    if (entity instanceof Player _player) {
                        if (!_player.level().isClientSide()) {
                            _player.displayClientMessage(itemstack.getDisplayName(), true);
                        }
                        _player.getCooldowns().addCooldown(itemstack.getItem(), 10);
                        if (itemstack.getItem() != JujutsucraftModItems.COPIED_CURSED_TECHNIQUE.get()) {
                            itemstack.getOrCreateTag().putBoolean("Used", true);
                        }
                    }
                }
                itemstack.getOrCreateTag().putBoolean("used_item", false);
            }
        }
    }
}
