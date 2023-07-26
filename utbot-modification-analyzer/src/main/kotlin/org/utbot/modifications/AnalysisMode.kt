package org.utbot.modifications

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

    // TODO?: add all modificators without constructors (methods only)
}
