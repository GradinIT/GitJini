package net.jini.space.config.modifiers;

/**
 * Defines modifiers for space operations.
 */
public enum WriteModifier {
    NONE,
    WRITE_ONLY,
    UPDATE_ONLY,
    UPDATE_OR_WRITE,
    RETURN_PREV_ON_UPDATE,
    ONE_WAY,
    MEMORY_ONLY_SEARCH,
    PARTIAL_UPDATE;
}
