package org.utbot.examples.annotations.lombok;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EnumWithAnnotations {
    ENUM_CONSTANT("Constant_1");
    private final String constant;
}
