/*******************************************************************************
 * Copyright (c) 2017, EURECOM
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *     - Neither the name of EURECOM nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contact: Monir AZRAOUI, Melek ÖNEN, Refik MOLVA
 * name.surname(at)eurecom(dot)fr
 *
*******************************************************************************/
package eu.clarussecure.dataoperations.SEmodule;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;

public class Permutation {

    private static BigInteger a;
    private static BigInteger b;

    public static int[] permute_array(int n) {
        do {
            a = new BigInteger("" + ThreadLocalRandom.current().nextInt(1, n));
        } while (!coprime(a, BigInteger.valueOf(n)));

        b = new BigInteger("" + ThreadLocalRandom.current().nextInt(1, n));
        int[] perm = new int[n];
        for (int i = 0; i < n; i++) {
            perm[i] = (int) ((a.longValue() * i + b.longValue()) % n);
        }
        return perm;
    }

    private static boolean coprime(BigInteger a, BigInteger b) {
        // test if two integers are coprime
        return gcd(a, b) == 1;
    }

    private static int gcd(BigInteger a, BigInteger b) {
        // compute the gcd of a and b
        BigInteger d = a.gcd(b);
        return d.intValue();
    }

}
