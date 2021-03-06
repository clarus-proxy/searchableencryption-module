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

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

//import org.apache.commons.codec.binary.Base64;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

public class Encryptor {
    public static String encrypt(String key, String value) {
        return encrypt(key, value, false);
    }

    // AKKA fix: new safe parameter to specify if encoded value must be URL and filename safe (without / character)
    public static String encrypt(String key, String value, boolean safe) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            IvParameterSpec iv = new IvParameterSpec(skeySpec.getEncoded());

            Cipher cipher = Cipher.getInstance(Constants.transformation);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            // AKKA fix: use an encoder according to the safe parameter
            //return Base64.getEncoder().encodeToString(encrypted);
            Encoder encoder = safe ? Base64.getUrlEncoder() : Base64.getEncoder();
            return encoder.encodeToString(encrypted);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static String encrypt(String Data, Key key) throws Exception {
        return encrypt(Data, key, false);
    }

    // AKKA fix: new safe parameter to specify if encoded value must be URL and filename safe (without / character)
    public static String encrypt(String Data, Key key, boolean safe) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(key.getEncoded());
        SecretKeySpec skeySpec = new SecretKeySpec(key.getEncoded(), "AES");
        Cipher c = Cipher.getInstance(Constants.transformation);
        c.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        byte[] encVal = c.doFinal(Data.getBytes(Constants.charset));
        // AKKA fix: use an encoder according to the safe parameter
        //String encryptedValue = Base64.getEncoder().encodeToString(encVal);
        Encoder encoder = safe ? Base64.getUrlEncoder() : Base64.getEncoder();
        String encryptedValue = encoder.encodeToString(encVal);
        return encryptedValue;
    }

    public static String decrypt(String key, String encrypted) {
        return decrypt(key, encrypted, false);
    }

    // AKKA fix: new safe parameter to specify if encoded value is URL and filename safe (without / character)
    public static String decrypt(String key, String encrypted, boolean safe) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            IvParameterSpec iv = new IvParameterSpec(skeySpec.getEncoded());

            Cipher cipher = Cipher.getInstance(Constants.transformation);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            // AKKA fix: use a decoder according to the safe parameter
            //byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            Decoder decoder = safe ? Base64.getUrlDecoder() : Base64.getDecoder();
            byte[] original = cipher.doFinal(decoder.decode(encrypted));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static String decrypt(String encryptedData, Key key) throws Exception {
        return decrypt(encryptedData, key, false);
    }

    // AKKA fix: new safe parameter to specify if encoded value is URL and filename safe (without / character)
    public static String decrypt(String encryptedData, Key key, boolean safe) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(key.getEncoded());
        SecretKeySpec skeySpec = new SecretKeySpec(key.getEncoded(), "AES");
        Cipher c = Cipher.getInstance(Constants.transformation);
        c.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        // AKKA fix: use a decoder according to the safe parameter
        //byte[] decordedValue = Base64.getDecoder().decode(encryptedData);
        Decoder decoder = safe ? Base64.getUrlDecoder() : Base64.getDecoder();
        byte[] decordedValue = decoder.decode(encryptedData);
        byte[] decValue = c.doFinal(decordedValue);
        String decryptedValue = new String(decValue);
        return decryptedValue;
    }

    public static byte[] prf(String a, SecretKey key) {
        Mac mac;
        try {
            mac = Mac.getInstance(Constants.prf);
            mac.init(key);

            byte[] b = a.getBytes(Constants.charset);
            return mac.doFinal(b);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String Xor(String a, String XorKey) {

        char[] key = XorKey.toCharArray();
        StringBuilder output = new StringBuilder();

        int aL = a.length();
        int kL = key.length;

        for (int i = 0; i < aL; i++) {
            output.append((char) (a.charAt(i) ^ key[i % kL]));
        }
        return output.toString();
    }

}
