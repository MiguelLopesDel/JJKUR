package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.configuration.JogoatConfiguration;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TestProcedureeProcedure {
    private static class CTRange {
        final String CT;
        final double startRange;
        final double endRange;

        public CTRange(String CT, double startRange, double endRange) {
            this.CT = CT;
            this.startRange = startRange;
            this.endRange = endRange;
        }
    }

    private static final List<CTRange> CTs = new ArrayList<>();
    private static double totalSum = 0.0;

    public static void calculateCTRanges() {
        CTs.clear();
        totalSum = 0.0;

        addCTRange("SUKUNA", JogoatConfiguration.SUKUNA.get());
        addCTRange("GOJO", JogoatConfiguration.GOJO.get());
        addCTRange("OGI", JogoatConfiguration.OGI.get());
        addCTRange("MAKI", JogoatConfiguration.MAKI.get());
        addCTRange("DHRUV", JogoatConfiguration.DHRUV.get());
        addCTRange("MAHORAGA", JogoatConfiguration.MAHORAGA.get());
        addCTRange("TSUKUMO", JogoatConfiguration.TSUKUMO.get());
        addCTRange("YAGA", JogoatConfiguration.YAGA.get());
        addCTRange("KUROURUSHI", JogoatConfiguration.KUROURUSHI.get());
        addCTRange("ISHIGORI", JogoatConfiguration.ISHIGORI.get());
        addCTRange("NISHIMIYA", JogoatConfiguration.NISHIMIYA.get());
        addCTRange("TODO", JogoatConfiguration.TODO.get());
        addCTRange("DAGON", JogoatConfiguration.DAGON.get());
        addCTRange("MIGUEL", JogoatConfiguration.MIGUEL.get());
        addCTRange("OKKOTSU", JogoatConfiguration.OKKOTSU.get());
        addCTRange("MEIMEI", JogoatConfiguration.MEIMEI.get());
        addCTRange("YOROZU", JogoatConfiguration.YOROZU.get());
        addCTRange("URAUME", JogoatConfiguration.URAUME.get());
        addCTRange("GETO", JogoatConfiguration.GETO.get());
        addCTRange("CHOSO", JogoatConfiguration.CHOSO.get());
        addCTRange("TAKAKOURO", JogoatConfiguration.TAKAKOURO.get());
        addCTRange("ITADORI", JogoatConfiguration.ITADORI.get());
        addCTRange("KUSAKABE", JogoatConfiguration.KUSAKABE.get());
        addCTRange("JUNPE", JogoatConfiguration.JUNPE.get());
        addCTRange("JOGO", JogoatConfiguration.JOGO.get());
        addCTRange("TAKABA", JogoatConfiguration.TAKABA.get());
        addCTRange("HIGURUMA", JogoatConfiguration.HIGURUMA.get());
        addCTRange("ANGEL", JogoatConfiguration.ANGEL.get());
        addCTRange("HAKARI", JogoatConfiguration.HAKARI.get());
        addCTRange("MAHITO", JogoatConfiguration.MAHITO.get());
        addCTRange("CHOJURO", JogoatConfiguration.CHOJURO.get());
        addCTRange("NANAMI", JogoatConfiguration.NANAMI.get());
        addCTRange("FUSHIGURO", JogoatConfiguration.FUSHIGURO.get());
        addCTRange("SMALLPOXDEITY", JogoatConfiguration.SMALLPOXDEITY.get());
        addCTRange("INUMAKI", JogoatConfiguration.INUMAKI.get());
        addCTRange("KUGISAKI", JogoatConfiguration.KUGISAKI.get());
        addCTRange("HANAMI", JogoatConfiguration.HANAMI.get());
        addCTRange("KASHIMO", JogoatConfiguration.KASHIMO.get());
        addCTRange("JINICHI", JogoatConfiguration.JINICHI.get());
        addCTRange("NAOYA", JogoatConfiguration.NAOYA.get());
    }

    public static String selectRandomCT() {
        RandomSource random = RandomSource.create();
        double randomValue = random.nextDouble() * totalSum;
        for (CTRange ctRange : CTs) {
            if (randomValue >= ctRange.startRange && randomValue < ctRange.endRange) {
                return ctRange.CT;
            }
        }
        return null;
    }

    private static void addCTRange(String CT, double value) {
        CTs.add(new CTRange(CT, totalSum, totalSum + value));
        totalSum += value;
    }

    public static void execute(LevelAccessor world, double x, double y, double z, Player entity, HashMap<?, ?> guistate) {
        if (entity == null || guistate == null) return;

        calculateCTRanges();
        String selectedCT = selectRandomCT();
        if (selectedCT == null) return;

        switch (selectedCT) {
            case "SUKUNA" -> SelectSukunaProcedure.execute(world, x, y, z, entity);
            case "GOJO" -> SelectGojoProcedure.execute(world, x, y, z, entity);
            case "OGI" -> SelectOgiProcedure.execute(world, x, y, z, entity);
            case "MAKI" -> SelectMakiProcedure.execute(world, x, y, z, entity);
            case "DHRUV" -> SelectDhruvLakdawallaProcedure.execute(world, x, y, z, entity);
            case "MAHORAGA" -> SelectMahoragaProcedure.execute(world, x, y, z, entity);
            case "TSUKUMO" -> SelectTsukumoProcedure.execute(world, x, y, z, entity);
            case "YAGA" -> SelectYagaProcedure.execute(world, x, y, z, entity);
            case "KUROURUSHI" -> SelectKurourushiProcedure.execute(world, x, y, z, entity);
            case "ISHIGORI" -> SelectIshigoriProcedure.execute(world, x, y, z, entity);
            case "NISHIMIYA" -> SelectNishimiyaProcedure.execute(world, x, y, z, entity);
            case "TODO" -> SelectTodoProcedure.execute(world, x, y, z, entity);
            case "DAGON" -> SelectDagonProcedure.execute(world, x, y, z, entity);
            case "MIGUEL" -> SelectMiguelProcedure.execute(world, x, y, z, entity);
            case "OKKOTSU" -> SelectOkkotsuProcedure.execute(world, x, y, z, entity);
            case "MEIMEI" -> SelectMeiMeiProcedure.execute(world, x, y, z, entity);
            case "YOROZU" -> SelectYorozuProcedure.execute(world, x, y, z, entity);
            case "URAUME" -> SelectUraumeProcedure.execute(world, x, y, z, entity);
            case "GETO" -> SelectGetoProcedure.execute(world, x, y, z, entity);
            case "CHOSO" -> SelectChosoProcedure.execute(world, x, y, z, entity);
            case "TAKAKOURO" -> SelectTakakoUroProcedure.execute(world, x, y, z, entity);
            case "ITADORI" -> SelectItadoriProcedure.execute(world, x, y, z, entity);
            case "KUSAKABE" -> SelectKusakabeProcedure.execute(world, x, y, z, entity);
            case "JUNPE" -> SelectJunpeProcedure.execute(world, x, y, z, entity);
            case "JOGO" -> SelectJogoProcedure.execute(world, x, y, z, entity);
            case "TAKABA" -> SelectTakabaProcedure.execute(world, x, y, z, entity);
            case "HIGURUMA" -> SelectHigurumaProcedure.execute(world, x, y, z, entity);
            case "ANGEL" -> SelectAngelProcedure.execute(world, x, y, z, entity);
            case "HAKARI" -> SelectHakariProcedure.execute(world, x, y, z, entity);
            case "MAHITO" -> SelectMahitoProcedure.execute(world, x, y, z, entity);
            case "CHOJURO" -> SelectChojuroProcedure.execute(world, x, y, z, entity);
            case "NANAMI" -> SelectNanamiProcedure.execute(world, x, y, z, entity);
            case "FUSHIGURO" -> SelectFushiguroProcedure.execute(world, x, y, z, entity);
            case "SMALLPOXDEITY" -> SelectSmallpoxDeityProcedure.execute(world, x, y, z, entity);
            case "INUMAKI" -> SelectInumakiProcedure.execute(world, x, y, z, entity);
            case "KUGISAKI" -> SelectKugisakiProcedure.execute(world, x, y, z, entity);
            case "HANAMI" -> SelectHanamiProcedure.execute(world, x, y, z, entity);
            case "KASHIMO" -> SelectKashimoProcedure.execute(world, x, y, z, entity);
            case "JINICHI" -> SelectJinichiProcedure.execute(world, x, y, z, entity);
            case "NAOYA" -> SelectNaoyaProcedure.execute(world, x, y, z, entity);
        }

        RandomizedSelectionProcedure.execute(entity);

        ItemStack stackToRemove = new ItemStack(JujutsucraftaddonModItems.RANDOM_CT_CHANGER.get());
        entity.getInventory().clearOrCountMatchingItems(p -> stackToRemove.getItem() == p.getItem(), 1, entity.inventoryMenu.getCraftSlots());
    }
}
