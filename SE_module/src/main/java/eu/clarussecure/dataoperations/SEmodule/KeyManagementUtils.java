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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.SecretKeyEntry;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

public class KeyManagementUtils {

    public static SecretKey[] procedureKeyGen()
            throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        /**
         * Overall Procedure for key generation:
         * 1) Generate the encryption key and 2 additional keys for index generation
         * 2) Create the key store
         * 3) Store generated keys in the key store
         *
         * IN: -
         * OUT: Array of 3 keys
         */

        SecretKey encryption_Key;
        SecretKey prfKey;
        SecretKey permKey;
        SecretKey[] output = new SecretKey[3];

        String keyType = "AES";
        String keyLength = "128";

        encryption_Key = generateKey(keyType, keyLength);
        System.out.println("	Generated Encryption Key: " + convertAESKeyToString(encryption_Key));
        output[0] = encryption_Key;

        /*
         * Generate sub-keys
         */
        prfKey = generateKey(keyType, keyLength);
        permKey = generateKey(keyType, keyLength);
        output[1] = prfKey;
        output[2] = permKey;
        System.out.println("	Generated Pseudo Random Function (PRF) key: " + convertAESKeyToString(prfKey));

        System.out.println("	Generated cuckoo hash table  initial (Pi) key: " + convertAESKeyToString(permKey));

        /*
         * Save keys in a Java keystore
         */
        String ksName = "clarus_keystore";
        char[] ksPassword = askPassword(ksName);
        // create keystore
        createKeyStore(ksName, ksPassword);
        // load keystore
        KeyStore myKS = loadKeyStore(ksName, ksPassword);
        storeSecretKey(myKS, encryption_Key, "encKey", ksPassword);
        storeSecretKey(myKS, prfKey, "y_Key", ksPassword);
        storeSecretKey(myKS, permKey, "z_Key", ksPassword);
        storeKeyStore(ksName, myKS, ksPassword);
        return output;
    }

    // AKKA fix: new function to load the secret keys (used by store_with_SE)
    public static SecretKey[] loadSecretKeys() throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
            IOException, UnrecoverableEntryException {
        /*
         * Load keys from a Java keystore
         */
        String ksName = "clarus_keystore";
        char[] ksPassword = askPassword(ksName);
        // load keystore
        KeyStore myKS = loadKeyStore(ksName, ksPassword);
        SecretKey encryption_Key = loadSecretKey(myKS, "encKey", ksPassword);
        SecretKey prfKey = loadSecretKey(myKS, "y_Key", ksPassword);
        SecretKey permKey = loadSecretKey(myKS, "z_Key", ksPassword);
        SecretKey[] output = new SecretKey[] { encryption_Key, prfKey, permKey };
        return output;
    }

    private static SecretKey generateKey(String type, String size) throws NoSuchAlgorithmException {
        /**
         * Generate a key with a specific algorithm and keySize
         */
        KeyGenerator keyGenerator = KeyGenerator.getInstance(type);
        keyGenerator.init(Integer.parseInt(size));
        return keyGenerator.generateKey();
    }

    public static SecretKey hashAESKey(SecretKey masterKey, String salt) throws Exception {

        /**
         * Convert the master key to string and shuffle all its bytes
         */

        char[] hex = Hex.encodeHex(masterKey.getEncoded());
        String aesKeyString = String.valueOf(hex);
        aesKeyString = aesKeyString.substring(0, 16);
        aesKeyString = Encryptor.Xor(aesKeyString, salt);

        SecretKey key = new SecretKeySpec(aesKeyString.getBytes(), "AES");
        return key;
    }

    private static void createKeyStore(String ksName, char[] password)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        // Initialize KeyStore from java
        KeyStore ks = KeyStore.getInstance("JCEKS");
        // create an empty keystore
        ks.load(null, password);
        FileOutputStream fos = new FileOutputStream(ksName);
        ks.store(fos, password);
        System.out.println("New keystore created");
        fos.close();
    }

    private static void storeKeyStore(String ksName, KeyStore ks, char[] password)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        FileOutputStream fos = new FileOutputStream(ksName);
        ks.store(fos, password);
        System.out.println("Keystore saved!");
        fos.close();
    }

    public static KeyStore loadKeyStore(String ksName, char[] password)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore ks = KeyStore.getInstance("JCEKS");
        InputStream is = new FileInputStream(ksName);
        try {
            ks.load(is, password);
        } catch (IOException e) {
            System.out.println("Wrong Password...");
            System.exit(1);
        }
        System.out.println("Keystore loaded!");
        return ks;
    }

    private static void storeSecretKey(KeyStore ks, SecretKey key, String alias, char[] password)
            throws KeyStoreException {
        // char [] password = askPassword(alias);
        SecretKeyEntry skEntry = new SecretKeyEntry(key);
        ProtectionParameter protParam = new PasswordProtection(password);
        ks.setEntry(alias, skEntry, protParam);
        String keyName = getKeyName(alias);
        System.out.println(keyName + " inserted in the keystore");

    }

    public static SecretKey loadSecretKey(KeyStore ks, String alias, char[] password)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException {
        SecretKey key;
        ProtectionParameter protParam = new PasswordProtection(password);
        SecretKeyEntry skEntry = (SecretKeyEntry) ks.getEntry(alias, protParam);
        key = skEntry.getSecretKey();
        String keyName = getKeyName(alias);
        System.out.println(keyName + " loaded from the keystore");
        return key;
    }

    public static char[] askPassword(String myObject) {
        if (Constants.passwd == null) {
            Scanner pwd_input = new Scanner(System.in);
            System.out.println("Please enter a password for " + myObject + ": ");
            String read_pwd = pwd_input.nextLine();
            char[] password = read_pwd.toCharArray();
            Constants.passwd = password;
        }
        return Constants.passwd;
    }

    private static String getKeyName(String alias) {
        return ((alias == "encKey") ? "Encryption Key" : ((alias == "y_Key") ? "PRF key" : "Pi key"));
    }

    public static String convertAESKeyToString(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static String convertAESKeyToHexa(Key key) {
        return String.valueOf(Hex.encodeHex(key.getEncoded()));
    }

}
