package com.jujutsu.jujutsucraftaddon.network;

import com.jujutsu.jujutsucraftaddon.util.JJKUVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerVariablesSyncHandler {

    static void handleMessage(JujutsucraftaddonModVariables.PlayerVariablesSyncMessage message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity targetEntity;
        if (message.targetId == -1) {
            targetEntity = Minecraft.getInstance().player;
        } else {
            targetEntity = level.getEntity(message.targetId);
        }
        if (targetEntity != null) {
            JJKUVariables.get(targetEntity).ifPresent(cap -> JujutsucraftaddonModVariables.PlayerVariablesSyncMessage.syncData(message, cap));
        }
    }
}
