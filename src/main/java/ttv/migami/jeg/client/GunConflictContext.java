package ttv.migami.jeg.client;

/**
 * Minimal stand-in for Controllable's binding context. Without Controllable the
 * contexts are no-ops, but the rest of the code can continue to reference them.
 */
public enum GunConflictContext
{
    IN_GAME_HOLDING_WEAPON;
}
