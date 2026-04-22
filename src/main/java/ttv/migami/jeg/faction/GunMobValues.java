package ttv.migami.jeg.faction;

public class GunMobValues {
    public static boolean enabled = true;
    public static boolean elitesEnabled = true;
    public static double eliteChance = 0.3D;
    public static int minDays = 4;
    public static int initialChance = 1;
    public static int chanceIncrement = 1;
    public static int maxChance = 50;

    public static void init() {
        // TODO: Implement configuration support when server config is added
        // For now using default values
    }
}