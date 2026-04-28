package ttv.migami.jeg.client;

public final class ClientUiConfig {
    private static boolean showCrosshair = true;
    private static boolean showHitFeedback = true;

    private ClientUiConfig() {}

    public static void update(boolean showCrosshair, boolean showHitFeedback) {
        ClientUiConfig.showCrosshair = showCrosshair;
        ClientUiConfig.showHitFeedback = showHitFeedback;
    }

    public static boolean showCrosshair() {
        return showCrosshair;
    }

    public static boolean showHitFeedback() {
        return showHitFeedback;
    }
}
