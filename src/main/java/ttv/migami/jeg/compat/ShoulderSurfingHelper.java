package ttv.migami.jeg.compat;

/**
 * Optional integration stub. Shoulder Surfing adds third-person utilities, but the
 * mod is not a hard dependency, so the helper simply reports "not shoulder surfing"
 * when the API is unavailable.
 */
public final class ShoulderSurfingHelper
{
    private ShoulderSurfingHelper() {}

    public static boolean isShoulderSurfing()
    {
        return false;
    }

    public static void changePerspective(String perspective)
    {
        // no-op without Shoulder Surfing present
    }

    public static void applyCameraRecoil(float startProgress, float endProgress, float cameraRecoil, int recoilRand)
    {
        // no-op without Shoulder Surfing present
    }
}
