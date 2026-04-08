package com.jujutsu.jujutsucraftaddon.procedures;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.slyrienmodern.com.util.SmoothChasingValue;

public class ModernJujutsuDustFix {

    public static SmoothChasingValue redirectDustTarget(SmoothChasingValue instance, float originalTarget, GuiGraphics gui, Entity player, float delta, boolean isEditing) {
        if (isEditing) {
            instance.target(originalTarget);
            return instance;
        }

        var cap = player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null);
        if (cap.isPresent()) {
            String overlay2 = cap.resolve().get().OVERLAY2;
            
            if (overlay2 != null && overlay2.contains("■")) {
                instance.target(calculatePercentageFromOverlay(overlay2));
                return instance;
            }
        }

        double dustAmount = player.getPersistentData().getDouble("dust_amount");
        instance.target((float) (dustAmount / 200.0));
        return instance;
    }

    private static float calculatePercentageFromOverlay(String overlay) {
        if (overlay == null || overlay.isEmpty()) return 0.0F;
        
        int redCount = 0;
        String clean = overlay.replace("§l", "").replace("§4", "");
        
        int splitIndex = clean.indexOf("§r§7");
        if (splitIndex != -1) {
            String filledPart = clean.substring(0, splitIndex);
            for (char c : filledPart.toCharArray()) {
                if (c == '■') redCount++;
            }
        } else {
            for (char c : clean.toCharArray()) {
                if (c == '■') redCount++;
            }
        }
        
        return Math.min(redCount / 10.0F, 1.0F);
    }
}
