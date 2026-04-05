package com.jujutsu.jujutsucraftaddon.init;

import com.jujutsu.jujutsucraftaddon.JujutsucraftaddonMod;
import com.jujutsu.jujutsucraftaddon.client.screens.*;
import com.jujutsu.jujutsucraftaddon.network.*;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class JujutsucraftaddonModKeyMappings {

    private static long WORLD_SLASH_KEY_LASTPRESS = 0;
    private static long ANIMATION_KEYBIND_LASTPRESS = 0;
    private static long BURNOUT_KEY_LASTPRESS = 0;

    public static final KeyMapping WORLD_SLASH_KEY = new KeyMapping("key.jujutsucraftaddon.world_slash_key", GLFW.GLFW_KEY_F, "Jujutsu Kaisen Ultimate") {
        private boolean isDownOld = false;

        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            LocalPlayer player = Minecraft.getInstance().player;
            if (isDownOld != isDown) {
                if (isDown) {
                    JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new WorldSlashKeyMessage(0, 0));
                    WorldSlashKeyMessage.pressAction(player, 0, 0);
                    WORLD_SLASH_KEY_LASTPRESS = System.currentTimeMillis();
                } else {
                    int dt = (int) (System.currentTimeMillis() - WORLD_SLASH_KEY_LASTPRESS);
                    JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new WorldSlashKeyMessage(1, dt));
                    WorldSlashKeyMessage.pressAction(player, 1, dt);
                }
            }
            isDownOld = isDown;
        }
    };

    public static final KeyMapping ALTAR_SELECTOR = new KeyMapping("key.jujutsucraftaddon.altar", GLFW.GLFW_KEY_PERIOD, "Jujutsu Kaisen Ultimate") {
        private boolean isDownOld = false;

        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (isDownOld != isDown && isDown && player != null) {
                if (player.isShiftKeyDown()) {
                    mc.execute(() -> {
                        if (mc.screen == null) mc.setScreen(new AltarShortcut());
                    });
                } else {
                    player.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                        if (!addonVars.Clans.equals("Kenjaku") && !player.hasEffect(JujutsucraftaddonModMobEffects.MANIFESTATION.get())) {
                            player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
                                double activeTech = vars.SecondTechnique ? vars.PlayerCurseTechnique2 : vars.PlayerCurseTechnique;
                                mc.execute(() -> {
                                    if (mc.screen != null) return;
                                    if (activeTech == TechniqueIDs.YUTA) mc.setScreen(new AltarSelectorScreen());
                                    else if (activeTech == TechniqueIDs.MAHITO) mc.setScreen(new MahitoScreen());
                                    else if (activeTech == TechniqueIDs.WUKONG) mc.setScreen(new WukongScreen());
                                    else if (activeTech == TechniqueIDs.TODO) mc.setScreen(new TodoScreen());
                                    else if (activeTech == TechniqueIDs.CHOSO) mc.setScreen(new ChosoScreen());
                                    else if (activeTech == TechniqueIDs.MEGUMI) mc.setScreen(new MegumiScreen());
                                    else if (activeTech == TechniqueIDs.MAKI) mc.setScreen(new HRScreen());
                                    else if (activeTech == TechniqueIDs.ITADORI) mc.setScreen(new ItadoriScreen());
                                    else if (activeTech == TechniqueIDs.HAKARI) mc.setScreen(new HakariScreen());
                                    else if (activeTech == TechniqueIDs.MAHORAGA) mc.setScreen(new MahoragaScreen());
                                    else if (activeTech == TechniqueIDs.GOJO) mc.setScreen(new GojoScreen());
                                    else if (activeTech == TechniqueIDs.SUKUNA) mc.setScreen(new SukunaScreen());
                                    else if (activeTech == TechniqueIDs.KASHIMO) mc.setScreen(new KashimoScreen());
                                    else if (activeTech == TechniqueIDs.URAUME) mc.setScreen(new UraumeScreen());
                                });
                            });
                        }
                    });
                }
            }
            isDownOld = isDown;
        }
    };

    public static final KeyMapping KEY_WHEEL = new KeyMapping("key.jujutsucraftaddon.key_wheel", GLFW.GLFW_KEY_C, "Jujutsu Kaisen Ultimate") {
        private boolean isDownOld = false;

        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (isDownOld != isDown && isDown && player != null) {
                mc.execute(() -> {
                    if (mc.screen == null) {
                        mc.setScreen(player.isShiftKeyDown() ? new GreatScreen() : new MasteryScreen());
                    }
                });
            }
            isDownOld = isDown;
        }
    };

    public static final KeyMapping ANIMATION_KEYBIND = new KeyMapping("key.jujutsucraftaddon.animation_keybind", GLFW.GLFW_KEY_B, "Jujutsu Kaisen Ultimate") {
        private boolean isDownOld = false;

        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            LocalPlayer player = Minecraft.getInstance().player;
            if (isDownOld != isDown) {
                if (isDown) {
                    JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new AnimationKeybindMessage(0, 0));
                    AnimationKeybindMessage.pressAction(player, 0, 0);
                    ANIMATION_KEYBIND_LASTPRESS = System.currentTimeMillis();
                } else {
                    int dt = (int) (System.currentTimeMillis() - ANIMATION_KEYBIND_LASTPRESS);
                    JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new AnimationKeybindMessage(1, dt));
                    AnimationKeybindMessage.pressAction(player, 1, dt);
                }
            }
            isDownOld = isDown;
        }
    };

    public static final KeyMapping TEST_ANIMATION = new KeyMapping("key.jujutsucraftaddon.test_animation", GLFW.GLFW_KEY_KP_DECIMAL, "Jujutsu Kaisen Ultimate") {
        private boolean isDownOld = false;

        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            if (isDownOld != isDown && isDown) {
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new TestAnimationMessage(0, 0));
                TestAnimationMessage.pressAction(Minecraft.getInstance().player, 0, 0);
            }
            isDownOld = isDown;
        }
    };

    public static final KeyMapping BURNOUT_KEY = new KeyMapping("key.jujutsucraftaddon.burnout_key", GLFW.GLFW_KEY_UNKNOWN, "Jujutsu Kaisen Ultimate") {
        private boolean isDownOld = false;

        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            LocalPlayer player = Minecraft.getInstance().player;
            if (isDownOld != isDown) {
                if (isDown) {
                    JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new BurnoutKeyMessage(0, 0));
                    BurnoutKeyMessage.pressAction(player, 0, 0);
                    BURNOUT_KEY_LASTPRESS = System.currentTimeMillis();
                } else {
                    int dt = (int) (System.currentTimeMillis() - BURNOUT_KEY_LASTPRESS);
                    JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new BurnoutKeyMessage(1, dt));
                    BurnoutKeyMessage.pressAction(player, 1, dt);
                }
            }
            isDownOld = isDown;
        }
    };

    public static final KeyMapping WATER_WALKING = new KeyMapping("key.jujutsucraftaddon.water_walking", GLFW.GLFW_KEY_UNKNOWN, "Jujutsu Kaisen Ultimate") {
        private boolean isDownOld = false;

        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            if (isDownOld != isDown && isDown) {
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new WaterWalkingMessage(0, 0));
                WaterWalkingMessage.pressAction(Minecraft.getInstance().player, 0, 0);
            }
            isDownOld = isDown;
        }
    };

    public static final KeyMapping RELEASE_TECHNIQUE = new KeyMapping("key.jujutsucraftaddon.release_technique", GLFW.GLFW_KEY_MINUS, "Jujutsu Kaisen Ultimate") {
        private boolean isDownOld = false;

        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            if (isDownOld != isDown && isDown) {
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new ReleaseTechniqueMessage(0, 0));
                ReleaseTechniqueMessage.pressAction(Minecraft.getInstance().player, 0, 0);
            }
            isDownOld = isDown;
        }
    };

    public static final KeyMapping PASSIVE_KEYBIND = new KeyMapping("key.jujutsucraftaddon.passive_keybind", GLFW.GLFW_KEY_B, "Jujutsu Kaisen Ultimate") {
        private boolean isDownOld = false;

        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            if (isDownOld != isDown && isDown) {
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PassiveKeybindMessage(0, 0));
                PassiveKeybindMessage.pressAction(Minecraft.getInstance().player, 0, 0);
            }
            isDownOld = isDown;
        }
    };

    public static final KeyMapping Z_TARGET_KEY = new KeyMapping("key.z_target", GLFW.GLFW_KEY_UNKNOWN, "key.categories.gameplay");

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(WORLD_SLASH_KEY);
        event.register(ALTAR_SELECTOR);
        event.register(KEY_WHEEL);
        event.register(Z_TARGET_KEY);
        event.register(ANIMATION_KEYBIND);
        event.register(TEST_ANIMATION);
        event.register(BURNOUT_KEY);
        event.register(WATER_WALKING);
        event.register(PASSIVE_KEYBIND);
        event.register(RELEASE_TECHNIQUE);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class KeyEventListener {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (Minecraft.getInstance().screen == null) {
                WORLD_SLASH_KEY.consumeClick();
                ALTAR_SELECTOR.consumeClick();
                KEY_WHEEL.consumeClick();
                ANIMATION_KEYBIND.consumeClick();
                TEST_ANIMATION.consumeClick();
                BURNOUT_KEY.consumeClick();
                Z_TARGET_KEY.consumeClick();
                WATER_WALKING.consumeClick();
                PASSIVE_KEYBIND.consumeClick();
                RELEASE_TECHNIQUE.consumeClick();
            }
        }
    }
}
