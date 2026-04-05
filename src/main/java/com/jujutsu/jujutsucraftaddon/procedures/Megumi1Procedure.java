package com.jujutsu.jujutsucraftaddon.procedures;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class Megumi1Procedure {

    public static void execute(Entity entity, Entity sourceentity) {
        if (entity == null || sourceentity == null) return;

        ItemStack helmet = (entity instanceof LivingEntity living) ? living.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY;
        ResourceLocation helmetKey = ForgeRegistries.ITEMS.getKey(helmet.getItem());

        if (helmetKey == null || !helmetKey.toString().equals("jujutsucraft:mahoraga_wheel_helmet")) return;

        double entitySkillDomain = entity.getPersistentData().getDouble("skill_domain");
        if (entitySkillDomain != 0) return;

        double sourceSkillDomain = sourceentity.getPersistentData().getDouble("skill_domain");
        String domainKey = "domain" + new java.text.DecimalFormat("##.##").format(sourceSkillDomain);

        if (helmet.getOrCreateTag().getDouble(domainKey) >= 100) {
            if (!entity.level().isClientSide() && entity.getServer() != null) {
                CommandSourceStack stack = new CommandSourceStack(
                    CommandSource.NULL, 
                    entity.position(), 
                    entity.getRotationVector(), 
                    entity.level() instanceof ServerLevel level ? level : null, 
                    4, 
                    entity.getName().getString(), 
                    entity.getDisplayName(), 
                    entity.getServer(), 
                    entity
                );
                
                String command = "execute as @s unless entity @e[nbt={ForgeData:{Mahoraga:1d}},distance=..50] run summon jujutsucraft:eight_handled_sword_divergent_sila_divine_general_mahoraga ~ ~ ~ {ForgeData:{Mahoraga:1d},Attributes:[{Name:\"jujutsucraft:size\",Base:2}]}";
                entity.getServer().getCommands().performPrefixedCommand(stack, command);
            }
        }
    }
}
