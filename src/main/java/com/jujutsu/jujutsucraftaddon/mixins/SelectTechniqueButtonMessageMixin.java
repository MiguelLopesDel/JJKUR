package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.CustomCursedTechniqueChangerRightclickedProcedureProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.SelectJinWooProcedure;
import com.jujutsu.jujutsucraftaddon.procedures.SelectWukongProcedure;
import net.mcreator.jujutsucraft.network.SelectTechniqueButtonMessage;
import net.mcreator.jujutsucraft.world.inventory.SelectTechniqueMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(value = SelectTechniqueButtonMessage.class, priority = -10000)
public abstract class SelectTechniqueButtonMessageMixin {

    /**
     * @author Satushi
     * @reason Consolidated Mixin to handle custom technique selections (Wukong, JinWoo, and Addon Changer)
     */
    @Inject(method = "handleButtonAction", at = @At("HEAD"), remap = false)
    private static void onHandleButtonAction(Player entity, int buttonID, int x, int y, int z, HashMap<String, String> textstate, CallbackInfo ci) {
        if (entity == null) return;
        
        Level world = entity.level();
        
        // Sync textstate to Menu's guistate (v43 standard)
        textstate.forEach(SelectTechniqueMenu.guistate::put);

        // JJKUR Custom Button Actions
        switch (buttonID) {
            case 1000 -> CustomCursedTechniqueChangerRightclickedProcedureProcedure.execute(world, x, y, z, entity);
            case 100 -> SelectWukongProcedure.execute(world, x, y, z, entity);
            case 101 -> SelectJinWooProcedure.execute(world, x, y, z, entity);
        }
    }
}
