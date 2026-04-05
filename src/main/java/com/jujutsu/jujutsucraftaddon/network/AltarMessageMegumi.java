package com.jujutsu.jujutsucraftaddon.network;

import com.jujutsu.jujutsucraftaddon.JujutsucraftaddonMod;
import com.jujutsu.jujutsucraftaddon.procedures.RemoveCE;
import com.jujutsu.jujutsucraftaddon.util.TechniqueIDs;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.KeyStartTechniqueOnKeyPressedProcedure;
import net.mcreator.jujutsucraft.procedures.StartCursedTechniqueProcedure;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class AltarMessageMegumi {
    private final int page;

    public AltarMessageMegumi(int page) {
        this.page = page;
    }

    public AltarMessageMegumi(FriendlyByteBuf buffer) {
        this.page = buffer.readInt();
    }

    public static void buffer(AltarMessageMegumi message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.page);
    }

    public static void handler(AltarMessageMegumi message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> pressAction(context.getSender(), message.page));
        context.setPacketHandled(true);
    }

    public static void pressAction(Player entity, int page) {
        if (entity == null) return;
        Level world = entity.level();
        if (!world.hasChunkAt(entity.blockPosition())) return;

        entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
            boolean megumiIn1 = vars.PlayerCurseTechnique == TechniqueIDs.MEGUMI;
            boolean megumiIn2 = vars.PlayerCurseTechnique2 == TechniqueIDs.MEGUMI;

            if (!(megumiIn1 || megumiIn2) || vars.PlayerCursePower < 500) return;

            if (entity instanceof ServerPlayer _plr) {
                Advancement _adv = _plr.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft:sorcerer_grade_special"));
                if (_adv == null || !_plr.getAdvancements().getOrStartProgress(_adv).isDone()) return;

                vars.SecondTechnique = megumiIn2;
                CompoundTag nbt = entity.getPersistentData();
                double skillId = -1;
                double selectId = -1;
                String skillName = "";
                boolean useKeyStart = true;

                switch (page) {
                    case 0 -> { skillId = 607; selectId = 7; skillName = "Divine Dog: Totality"; useKeyStart = false; }
                    case 1 -> { skillId = 608; selectId = 8; skillName = "Nue"; }
                    case 2 -> { skillId = 609; selectId = 9; skillName = "Great Serpent"; }
                    case 3 -> { skillId = 610; selectId = 10; skillName = "Toad"; }
                    case 4 -> { skillId = 611; selectId = 11; skillName = "Max Elephant"; }
                    case 5 -> { skillId = 612; selectId = 12; skillName = "Rabbit Escape"; }
                    case 6 -> { skillId = 613; selectId = 13; skillName = "Round Deer"; }
                    case 7 -> { skillId = 614; selectId = 14; skillName = "Piercing Ox"; }
                    case 8 -> { skillId = 615; selectId = 15; skillName = "Tiger Funeral"; }
                    case 9 -> {
                        if (nbt.getDouble("TenShadowsTechnique13") > -1) {
                            skillId = 617; selectId = 17; skillName = "Merged Beast Agito";
                        }
                    }
                    case 10 -> {
                        if (nbt.getDouble("TenShadowsTechnique14") > -1) {
                            skillId = 618; selectId = 18; skillName = "Mahoraga"; nbt.putDouble("cnt9", 1.0);
                        }
                    }
                    case 11 -> { skillId = 1007; selectId = 7; skillName = Component.translatable("jujutsu.technique.choso3").getString(); }
                    case 12 -> { skillId = 608; selectId = 8; skillName = Component.translatable("entity.jujutsucraft.nue_totality").getString(); }
                }

                if (skillId != -1) {
                    vars.PlayerSelectCurseTechnique = selectId;
                    vars.PlayerSelectCurseTechniqueName = skillName;
                    vars.syncPlayerVariables(entity);

                    nbt.putDouble("skill", skillId);
                    if (useKeyStart) {
                        KeyStartTechniqueOnKeyPressedProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
                    } else {
                        StartCursedTechniqueProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
                    }

                    if (!world.isClientSide()) {
                        entity.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.COOLDOWN_TIME.get(), 120));
                    }
                    RemoveCE.execute(entity, world);
                }
            }
        });
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        JujutsucraftaddonMod.addNetworkMessage(AltarMessageMegumi.class, AltarMessageMegumi::buffer, AltarMessageMegumi::new, AltarMessageMegumi::handler);
    }
}
