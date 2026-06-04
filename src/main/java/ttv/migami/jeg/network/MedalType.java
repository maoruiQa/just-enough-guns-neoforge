package ttv.migami.jeg.network;

public enum MedalType {
    MULTIKILL_SINGLE_KILL("multikill_kill_single", "medal.jeg.multikill_kill_single"),
    MULTIKILL_DOUBLE_KILL("multikill_kill_double", "medal.jeg.multikill_double_kill"),
    MULTIKILL_TRIPLE_KILL("multikill_kill_triple", "medal.jeg.multikill_triple_kill"),
    MULTIKILL_QUAD_KILL("multikill_kill_quad", "medal.jeg.multikill_quad_kill"),
    MULTIKILL_PENTA_KILL("multikill_kill_penta", "medal.jeg.multikill_penta_kill"),
    MULTIKILL_KILLING_SPREE("multikill_killing_spree", "medal.jeg.multikill_killing_spree"),
    COMBAT_HEADSHOT("combat_headshot", "medal.jeg.combat_headshot"),
    COMBAT_KINGSLAYER("combat_kingslayer", "medal.jeg.combat_kingslayer"),
    COMBAT_JUST_ENOUGH_AMMO("combat_just_enough_ammo", "medal.jeg.combat_just_enough_ammo"),
    COMBAT_HUSH("combat_hush", "medal.jeg.combat_hush"),
    GEAR_BOOM("gear_boom", "medal.jeg.gear_boom"),
    GEAR_BBQ("gear_bbq", "medal.jeg.gear_bbq");

    private final String texturePath;
    private final String translationKey;

    MedalType(String texturePath, String translationKey) {
        this.texturePath = texturePath;
        this.translationKey = translationKey;
    }

    public String texturePath() {
        return texturePath;
    }

    public String translationKey() {
        return translationKey;
    }

    public static MedalType byOrdinal(int ordinal) {
        MedalType[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return MULTIKILL_SINGLE_KILL;
        }
        return values[ordinal];
    }
}
