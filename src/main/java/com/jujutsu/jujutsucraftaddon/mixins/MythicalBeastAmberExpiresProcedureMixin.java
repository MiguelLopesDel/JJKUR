package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.JujutsucraftMod;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.procedures.MythicalBeastAmberEffectEffectExpiresProcedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MythicalBeastAmberEffectEffectExpiresProcedure.class, priority = -10000)
public abstract class MythicalBeastAmberExpiresProcedureMixin {

    /**
     * @author Satushi
     * @reason Adds survival chance for Kashimo and syncs with v43 damage reduction
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, Entity entity, double amplifier, CallbackInfo ci) {
        ci.cancel();

        if (entity == null) return;

        double num_level = amplifier + 1.0;
        if (num_level > 0.0 && entity instanceof LivingEntity _liv) {
            if (_liv.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                double currentBase = _liv.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
                // v43 uses 1.0 multiplier
                _liv.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(currentBase - num_level * 1.0);
            }
        }

        JujutsucraftMod.queueServerWork(1, () -> {
            if (entity instanceof LivingEntity _liv && _liv.hasEffect(JujutsucraftModMobEffects.MYTHICAL_BEAST_AMBER_EFFECT.get())) {
                return;
            }

            if (!entity.level().isClientSide() && entity.getServer() != null) {
                CommandSourceStack source = new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), 
                    entity.level() instanceof ServerLevel ? (ServerLevel) entity.level() : null, 4, 
                    entity.getName().getString(), entity.getDisplayName(), entity.level().getServer(), entity);
                
                entity.getServer().getCommands().performPrefixedCommand(source, "clear @s jujutsucraft:mythical_beast_amber_head");
                entity.getServer().getCommands().performPrefixedCommand(source, "clear @s jujutsucraft:mythical_beast_amber_helmet");
            }

            JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftaddonModVariables.PlayerVariables());

            boolean shouldDie = true;
            if ("Kashimo".equals(addonVars.Clans)) {
                // Addon Buff: 50% chance to survive the expiration
                if (Math.random() < 0.5) {
                    shouldDie = false;
                }
            }

            if (shouldDie) {
                if (entity instanceof LivingEntity _liv) {
                    _liv.setHealth(0.0F);
                }
                DamageSource generic = new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC));
                entity.hurt(generic, 1.0F);
            }
        });
    }
}
