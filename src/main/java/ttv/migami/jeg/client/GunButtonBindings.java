package ttv.migami.jeg.client;

/**
 * Dummy controller bindings used when Controllable is not present. Each binding simply
 * reports the default controller button identifier (for logging/UI) but no runtime
 * interaction is performed.
 */
public final class GunButtonBindings
{
    private GunButtonBindings() {}

    public static final DummyBinding SHOOT = new DummyBinding();
    public static final DummyBinding AIM = new DummyBinding();
    public static final DummyBinding RELOAD = new DummyBinding();
    public static final DummyBinding OPEN_ATTACHMENTS = new DummyBinding();
    public static final DummyBinding STEADY_AIM = new DummyBinding();

    public static void register() {}

    public static final class DummyBinding
    {
        public int getButton()
        {
            return -1;
        }
    }
}
