/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.utbot.examples.thirdparty.numbers;

/**
 * Some useful, arithmetics related, additions to the built-in functions in
 * {@link Math}.
 */
public final class ArithmeticUtils {

    /**
     * Negative exponent exception message part 1.
     */
    private static final String NEGATIVE_EXPONENT_1 = "negative exponent ({";
    /**
     * Negative exponent exception message part 2.
     */
    private static final String NEGATIVE_EXPONENT_2 = "})";

    /**
     * Raise an int to an int power.
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>{@code k^0} returns {@code 1} (including {@code k=0})
     *   <li>{@code k^1} returns {@code k} (including {@code k=0})
     *   <li>{@code 0^0} returns {@code 1}
     *   <li>{@code 0^e} returns {@code 0}
     *   <li>{@code 1^e} returns {@code 1}
     *   <li>{@code (-1)^e} returns {@code -1 or 1} if {@code e} is odd or even
     * </ul>
     *
     * @param k Number to raise.
     * @param e Exponent (must be positive or zero).
     * @return \( k^e \)
     * @throws IllegalArgumentException if {@code e < 0}.
     * @throws ArithmeticException      if the result would overflow.
     */
    public int pow(final int k,
                   final int e) {
        if (e < 0) {
            throw new IllegalArgumentException(NEGATIVE_EXPONENT_1 + e + NEGATIVE_EXPONENT_2);
        }

        if (k == 0) {
            return e == 0 ? 1 : 0;
        }

        if (k == 1) {
            return 1;
        }

        if (k == -1) {
            return (e & 1) == 0 ? 1 : -1;
        }

        if (e >= 31) {
            throw new ArithmeticException("integer overflow");
        }

        int exp = e;
        int result = 1;
        int k2p = k;
        while (true) {
            if ((exp & 0x1) != 0) {
                result = Math.multiplyExact(result, k2p);
            }

            exp >>= 1;
            if (exp == 0) {
                break;
            }

            k2p = Math.multiplyExact(k2p, k2p);
        }

        return result;
    }
}