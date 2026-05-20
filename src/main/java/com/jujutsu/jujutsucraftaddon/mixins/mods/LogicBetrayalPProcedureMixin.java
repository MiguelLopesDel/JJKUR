package com.jujutsu.jujutsucraftaddon.mixins.mods;

import net.mcreator.jujutsucraft.procedures.LogicBetrayalProcedure;
import net.mcreator.jujutsucrafts.procedures.LogicBetrayalPProcedure;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = LogicBetrayalPProcedure.class, remap = false)
public abstract class LogicBetrayalPProcedureMixin {

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static boolean execute(Entity entity, Entity entityiterator) {
        return entity != null && entityiterator != null ? LogicBetrayalProcedure.execute(entity.level(), entity, entityiterator) : false;
    }
}
