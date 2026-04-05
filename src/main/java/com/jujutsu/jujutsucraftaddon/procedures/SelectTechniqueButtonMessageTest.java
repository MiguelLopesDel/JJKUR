package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.JujutsucraftaddonMod;
import net.mcreator.jujutsucraft.world.inventory.SelectTechniqueMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SelectTechniqueButtonMessageTest {
    private final int buttonID;
    private final int x;
    private final int y;
    private final int z;
    private final HashMap<String, String> textstate;

    public SelectTechniqueButtonMessageTest(FriendlyByteBuf buffer) {
        this.buttonID = buffer.readInt();
        this.x = buffer.readInt();
        this.y = buffer.readInt();
        this.z = buffer.readInt();
        this.textstate = readTextState(buffer);
    }

    public SelectTechniqueButtonMessageTest(int buttonID, int x, int y, int z, HashMap<String, String> textstate) {
        this.buttonID = buttonID;
        this.x = x;
        this.y = y;
        this.z = z;
        this.textstate = textstate;
    }

    public static void buffer(SelectTechniqueButtonMessageTest message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.buttonID);
        buffer.writeInt(message.x);
        buffer.writeInt(message.y);
        buffer.writeInt(message.z);
        writeTextState(message.textstate, buffer);
    }

    public static void handler(SelectTechniqueButtonMessageTest message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Player entity = context.getSender();
            if (entity != null) {
                handleButtonAction(entity, message.buttonID, message.x, message.y, message.z, message.textstate);
            }
        });
        context.setPacketHandled(true);
    }

    public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z, HashMap<String, String> textstate) {
        Level world = entity.level();
        if (!world.hasChunkAt(new BlockPos(x, y, z))) return;

        HashMap guistate = SelectTechniqueMenu.guistate;
        for (Map.Entry<String, String> entry : textstate.entrySet()) {
            guistate.put(entry.getKey(), entry.getValue());
        }

        if (buttonID == 100) {
            SelectWukongProcedure.execute(world, x, y, z, entity);
        } else if (buttonID == 101) {
            SelectJinWooProcedure.execute(world, x, y, z, entity);
        }
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        JujutsucraftaddonMod.addNetworkMessage(SelectTechniqueButtonMessageTest.class, SelectTechniqueButtonMessageTest::buffer, SelectTechniqueButtonMessageTest::new, SelectTechniqueButtonMessageTest::handler);
    }

    public static void writeTextState(HashMap<String, String> map, FriendlyByteBuf buffer) {
        buffer.writeInt(map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            buffer.writeComponent(Component.literal(entry.getKey()));
            buffer.writeComponent(Component.literal(entry.getValue()));
        }
    }

    public static HashMap<String, String> readTextState(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < size; ++i) {
            String key = buffer.readComponent().getString();
            String value = buffer.readComponent().getString();
            map.put(key, value);
        }
        return map;
    }
}
