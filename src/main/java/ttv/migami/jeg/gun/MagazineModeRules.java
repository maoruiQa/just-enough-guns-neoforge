package ttv.migami.jeg.gun;

import java.util.Set;

/** Server recipe IDs, shared by recipe loading and cached crafting lookups. */
public final class MagazineModeRules {
    public static final Set<String> CAPACITY_RECIPES = Set.of("jeg:extended_mag", "jeg:drum_mag");
    public static final Set<String> MAGAZINE_RECIPES = Set.of(
            "jeg:pistol_magazine", "jeg:machine_gun_magazine", "jeg:magazine_loader",
            "jeg:smg_magazine", "jeg:smg_extended_magazine", "jeg:smg_drum_magazine",
            "jeg:rifle_magazine", "jeg:rifle_extended_magazine", "jeg:rifle_drum_magazine",
            "jeg:shotgun_magazine", "jeg:shotgun_extended_magazine", "jeg:shotgun_drum_magazine");

    private MagazineModeRules() {}

    public static boolean allows(String recipeId, boolean magazineFeed) {
        return !(magazineFeed ? CAPACITY_RECIPES : MAGAZINE_RECIPES).contains(recipeId);
    }

    public static void main(String[] args) {
        for (String id : CAPACITY_RECIPES) {
            assert !allows(id, true) : id;
            assert allows(id, false) : id;
        }
        for (String id : MAGAZINE_RECIPES) {
            assert allows(id, true) : id;
            assert !allows(id, false) : id;
        }
        assert CAPACITY_RECIPES.size() + MAGAZINE_RECIPES.size() == 14;
        assert allows("jeg:assault_rifle", true) && allows("jeg:assault_rifle", false);
        assert allows("other:extended_mag", true);
        System.out.println("Magazine mode: all 14 recipes passed both modes");
    }
}
