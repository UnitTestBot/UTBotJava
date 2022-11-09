/*
 The MIT License

 Copyright (c) 2010-2021 Paul R. Holser, Jr.

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package org.utbot.quickcheck.generator.java.nio.charset;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.concrete.UtModelConstructor;
import org.utbot.framework.plugin.api.*;
import org.utbot.framework.plugin.api.util.IdUtilKt;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.nio.charset.Charset.availableCharsets;
import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.framework.plugin.api.util.IdUtilKt.getStringClassId;

/**
 * Produces values of type {@link Charset}.
 */
public class CharsetGenerator extends Generator<Charset> {
    public CharsetGenerator() {
        super(Charset.class);
    }

    @Override
    public UtModel generate(
            SourceOfRandomness random,
            GenerationStatus status) {

        final UtModelConstructor modelConstructor = UtModelGenerator.getUtModelConstructor();

        final String charsetName = random.choose(availableCharsets().keySet());
        final UtModel charsetNameModel = modelConstructor.construct(charsetName, getStringClassId());

        final Method charsetForName;
        try {
            charsetForName = Charset.class.getMethod("forName", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        final ClassId charsetClassId = classIdForType(Charset.class);
        final ExecutableId charsetForNameId = IdUtilKt.getExecutableId(charsetForName);

        final int modelId = modelConstructor.computeUnusedIdAndUpdate();

        return new UtAssembleModel(
                modelId,
                charsetClassId,
                charsetForNameId.getName() + "#" + modelId,
                new UtExecutableCallModel(null, charsetForNameId, List.of(charsetNameModel)),
                null,
                (a) -> List.of()
        );
    }
}
