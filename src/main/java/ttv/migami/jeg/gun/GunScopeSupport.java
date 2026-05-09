package ttv.migami.jeg.gun;

public final class GunScopeSupport {
    private static volatile boolean boltActionRifleScopeEnabled = true;

    private GunScopeSupport() {
    }

    public static boolean isBoltActionRifleScopeEnabled() {
        return boltActionRifleScopeEnabled;
    }

    public static void setBoltActionRifleScopeEnabled(boolean enabled) {
        boltActionRifleScopeEnabled = enabled;
    }
}
