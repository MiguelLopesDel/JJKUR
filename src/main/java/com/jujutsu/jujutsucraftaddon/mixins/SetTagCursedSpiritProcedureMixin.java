package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.PartialRikaEntity;
import net.mcreator.jujutsucraft.entity.*;
import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.procedures.SetTagCursedSpritProcedure;
import net.mcreator.jujutsucraft.procedures.SetTagProcedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SetTagCursedSpritProcedure.class, priority = -10000)
public abstract class SetTagCursedSpiritProcedureMixin {

    /**
     * @author Satushi
     * @reason Refactored for clean code and compatibility with Partial Rika and custom entity tags.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        SetTagProcedure.execute(world, entity);
        entity.getPersistentData().putBoolean("CursedSpirit", true);

        // 1. Handle Silent Grade 16
        if (entity instanceof CursedSpiritGrade16Entity) {
            if (!entity.level().isClientSide() && entity.getServer() != null) {
                entity.getServer().getCommands().performPrefixedCommand(
                    new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(), 
                    (ServerLevel) entity.level(), 4, entity.getName().getString(), entity.getDisplayName(), 
                    entity.level().getServer(), entity), "data merge entity @s {Silent:1b}"
                );
            }
        }

        // 2. Handle Step Height for specific spirits
        if (entity instanceof CursedSpiritGrade21Entity || entity instanceof CursedSpiritGrade22Entity || 
            entity instanceof CursedSpiritGrade23Entity || entity instanceof CursedSpiritGrade25Entity || 
            entity instanceof CursedSpiritGrade26Entity || entity instanceof CursedSpiritGrade27Entity || 
            entity instanceof CursedSpiritGrade28Entity || entity instanceof CursedSpiritGrade13Entity) {
            entity.setMaxUpStep(entity.getStepHeight() * 2.0F);
        }

        // 3. Handle Size for Rika (Including Partial Rika)
        if (entity instanceof RikaEntity || entity instanceof Rika2Entity || entity instanceof PartialRikaEntity) {
            if (entity instanceof LivingEntity _liv) {
                if (_liv.getAttributes().hasAttribute((Attribute) JujutsucraftModAttributes.SIZE.get())) {
                    _liv.getAttribute((Attribute) JujutsucraftModAttributes.SIZE.get()).setBaseValue(2.5);
                }
            }
        }
    }
}
