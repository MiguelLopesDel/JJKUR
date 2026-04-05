package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class TestAnimationOnKeyPressedProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
        JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());

        double techniqueId = baseVars.SecondTechnique ? baseVars.PlayerCurseTechnique2 : baseVars.PlayerCurseTechnique;

        if (addonVars.Clans.equals("Kenjaku")) {
            handleKenjakuLogic(entity, addonVars);
        }

        if (techniqueId == TechniqueIDs.SUKUNA) {
            handleSukunaLogic(entity, addonVars);
        } else if (techniqueId == TechniqueIDs.MEGUMI) {
            handleMegumiLogic(entity, addonVars);
        } else if (techniqueId == TechniqueIDs.KUSAKABE) {
            handleKusakabeLogic(entity, addonVars);
        } else if (techniqueId == TechniqueIDs.GOJO) {
            handleGojoLogic(entity, addonVars);
        } else if (techniqueId == TechniqueIDs.KASHIMO) {
            handleKashimoLogic(entity, addonVars);
        }

        if (baseVars.PlayerCurseTechnique2 == TechniqueIDs.MEGUMI) {
            handleMegumiHidingLogic(world, x, y, z, entity);
        }

        if (entity.isShiftKeyDown()) {
            PassiveKeybindOnKeyReleased1Procedure.execute(world, x, y, z, entity);
        }
    }

    private static void handleKenjakuLogic(Entity entity, JujutsucraftaddonModVariables.PlayerVariables addon) {
        if (!entity.isShiftKeyDown()) {
            String current = addon.tag1;
            if (!current.equals("One") && !current.equals("Two") && !current.equals("Three")) {
                addon.tag1 = "One";
                sendActionMessage(entity, "Swapped For First Body");
            } else if (current.equals("One")) {
                addon.tag1 = "Two";
                sendActionMessage(entity, "Swapped For Second Body");
            } else if (current.equals("Two")) {
                addon.tag1 = "Three";
                sendActionMessage(entity, "Swapped For Third Body");
            } else {
                addon.tag1 = "";
                sendActionMessage(entity, "Swapped For No Body");
            }
        } else {
            if (addon.tag2.equals("True")) {
                addon.AgitoBeast = false;
                addon.tag2 = "False";
                sendActionMessage(entity, "No Body");
            } else {
                addon.AgitoBeast = true;
                addon.tag2 = "True";
                sendActionMessage(entity, "Copying Body");
            }
        }
        addon.syncPlayerVariables(entity);
    }

    private static void handleSukunaLogic(Entity entity, JujutsucraftaddonModVariables.PlayerVariables addon) {
        if (entity.isShiftKeyDown()) {
            if (addon.Mode.equals("Dismantle")) addon.Mode = "Cleave";
            else if (addon.Mode.equals("Cleave")) addon.Mode = "Base";
            else addon.Mode = "Dismantle";
            sendActionMessage(entity, addon.Mode + " Mode");
        } else {
            if (addon.Moveset < 5) addon.Moveset += 1;
            else addon.Moveset = 0;
            
            String msg = switch ((int) addon.Moveset) {
                case 1 -> "Cleave Web";
                case 2 -> "Dismantle Net";
                case 3 -> "WS: Dismantle Barrage";
                case 4 -> "WS: Dismantle Impact";
                case 5 -> "WS: Cleave Ground";
                default -> "None";
            };
            sendActionMessage(entity, msg);
        }
        addon.syncPlayerVariables(entity);
    }

    private static void handleMegumiLogic(Entity entity, JujutsucraftaddonModVariables.PlayerVariables addon) {
        if (addon.Mode.equals("Shikigami Offensive")) addon.Mode = "Shikigami Defensive";
        else if (addon.Mode.equals("Shikigami Defensive")) addon.Mode = "Base";
        else addon.Mode = "Shikigami Offensive";
        
        String msg = addon.Mode.equals("Shikigami Defensive") ? "Shikigami Guard Mode" : addon.Mode + " Mode";
        sendActionMessage(entity, msg);
        addon.syncPlayerVariables(entity);
    }

    private static void handleKusakabeLogic(Entity entity, JujutsucraftaddonModVariables.PlayerVariables addon) {
        if (addon.Mode.equals("New Shadow Style: Defensive")) addon.Mode = "New Shadow Style: Offensive";
        else addon.Mode = "New Shadow Style: Defensive";
        sendActionMessage(entity, addon.Mode + " Mode");
        addon.syncPlayerVariables(entity);
    }

    private static void handleGojoLogic(Entity entity, JujutsucraftaddonModVariables.PlayerVariables addon) {
        if (entity.isShiftKeyDown()) {
            if (addon.Mode.equals("Tatical Mode")) addon.Mode = "Passive Mode";
            else if (addon.Mode.equals("Passive Mode")) addon.Mode = "Combat Mode";
            else addon.Mode = "Tatical Mode";
            sendActionMessage(entity, addon.Mode);
        } else {
            if (addon.Moveset < 5) addon.Moveset += 1;
            else addon.Moveset = 0;

            String msg = switch ((int) addon.Moveset) {
                case 1 -> "Maximum Output: Blue";
                case 2 -> "Maximum Output: Red";
                case 3 -> "200% Purple";
                case 4 -> "Unlimited Purple";
                case 5 -> "Blue Barrage";
                default -> "None";
            };
            sendActionMessage(entity, msg);
        }
        addon.syncPlayerVariables(entity);
    }

    private static void handleKashimoLogic(Entity entity, JujutsucraftaddonModVariables.PlayerVariables addon) {
        if (addon.Mode.equals("Thunder Blitz")) addon.Mode = "Thunder God";
        else if (addon.Mode.equals("Thunder God")) addon.Mode = "Estrategic Mode";
        else addon.Mode = "Thunder Blitz";
        sendActionMessage(entity, addon.Mode + " Mode");
        addon.syncPlayerVariables(entity);
    }

    private static void handleMegumiHidingLogic(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity.isShiftKeyDown()) {
            final Vec3 _center = new Vec3(x, y, z);
            List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(10.0), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
            for (Entity iterator : _entfound) {
                if (iterator != entity && iterator.getPersistentData().getString("OWNER_UUID").equals(entity.getStringUUID())) {
                    if (entity instanceof LivingEntity _liv && !_liv.hasEffect(JujutsucraftaddonModMobEffects.DASH_COOLDOWN.get())) {
                        if (iterator instanceof LivingEntity _target && !_target.level().isClientSide()) {
                            _target.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.HIDING.get(), 100, 1, false, false));
                        }
                        sendActionMessage(entity, "Lovely...");
                    } else {
                        sendActionMessage(entity, "Skill On Cooldown");
                    }
                }
            }
        }
    }

    private static void sendActionMessage(Entity entity, String text) {
        if (entity instanceof Player _player && !_player.level().isClientSide()) {
            _player.displayClientMessage(Component.literal(text), true);
        }
    }
}
