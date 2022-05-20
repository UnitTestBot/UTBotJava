package org.utbot.framework.modifications

/**
 * Restrictions on demanded modificators
 */
enum class AnalysisMode {

    /**
     * Search for all field modificators
     */
    AllModificators,

    /**
     * Search setters and possible direct accesses
     */
    SettersAndDirectAccessors,

    /**
     * Search constructors only
     */
    Constructors,
}
