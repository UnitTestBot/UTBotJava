package org.utbot.greyboxfuzzer.quickcheck.generator

enum class GenerationState {
    REGENERATE, CACHE, MODIFY, MODIFYING_CHAIN
}