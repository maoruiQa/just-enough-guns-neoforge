package ttv.migami.jeg.client;

public final class ClientUiConfig {
    private static boolean showCrosshair = true;
    private static boolean showHitFeedback = true;
    private static boolean hideMedals = false;

    private ClientUiConfig() {}

    public static void update(boolean showCrosshair, boolean showHitFeedback, boolean hideMedals) {
        ClientUiConfig.showCrosshair = showCrosshair;
        ClientUiConfig.showHitFeedback = showHitFeedback;
        ClientUiConfig.hideMedals = hideMedals;
    }

    public static boolean showCrosshair() {
        return showCrosshair;
    }

    public static boolean showHitFeedback() {
        return showHitFeedback;
    }

    public static boolean hideMedals() {
        return hideMedals;
    }
}
