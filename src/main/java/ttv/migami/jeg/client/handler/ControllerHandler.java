package ttv.migami.jeg.client.handler;

/**
 * Stub controller handler that keeps the existing code paths compiling when Controllable
 * is not on the classpath. All methods simply return `false` or no-op.
 */
public final class ControllerHandler
{
    private ControllerHandler() {}

    public static void init() {}

    public static boolean isAiming()
    {
        return false;
    }

    public static boolean isShooting()
    {
        return false;
    }
}
