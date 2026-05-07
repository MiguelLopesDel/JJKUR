package com.jujutsu.jujutsucraftaddon.procedures;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

public class GojoDomainForceProcedure {
    public static void execute(CommandContext<CommandSourceStack> arguments) {
        try {
            Entity target = EntityArgument.getEntity(arguments, "target");
            if (target instanceof LivingEntity livingTarget) {
                force(livingTarget);
            }
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void force(LivingEntity livingTarget) {
        livingTarget.getPersistentData().putDouble("skill", 220.0);

        MobEffect cursedTechnique = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "cursed_technique"));
        if (cursedTechnique != null) {
            livingTarget.addEffect(new MobEffectInstance(cursedTechnique, 99999, 0, false, false));
        }

        MobEffect cooldown = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "cooldown_time"));
        MobEffect cooldownCombat = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "cooldown_time_combat"));

        if (cooldown != null) livingTarget.removeEffect(cooldown);
        if (cooldownCombat != null) livingTarget.removeEffect(cooldownCombat);
    }
}
