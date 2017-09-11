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

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import eu.clarussecure.dataoperations.*;

public class SearchableEncryptionModule implements DataOperation {
    @Override
    public List<DataOperationCommand> post(String[] attributeNames, String[][] contents) {
        /**
         * Generates keying material
         * Builds the dictionary of distinct keywords
         * Creates the secure search index
         * Encrypts the data
         */
        List<DataOperationCommand> SEcommand = null;
        try {
            SEcommand = Store.store_with_SE(attributeNames, contents);
        } catch (Exception e) {
            System.out.println("[FAILURE]: Searchable Encryption post");
            System.out.println(e);
            System.exit(1);
        }
        return SEcommand;
    }

    @Override
    public List<DataOperationCommand> get(String[] attributeNames, Criteria[] criteria) {
        /**
         * Generates one or several SE queries
         */
        List<DataOperationCommand> SEquery = null;
        try {
            SEquery = Query.search_with_SE(attributeNames, criteria);
        } catch (Exception e) {
            System.exit(1);

        }
        return SEquery;
    }

    @Override
    public List<DataOperationResult> get(List<DataOperationCommand> promise, List<String[][]> contents) {
        /**
         * Decrypts the retrieved content
         */
        List<DataOperationResult> SEresult = null;
        try {
            try {
                SEresult = Retrieve.decrypt_result(promise, contents);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableEntryException
                | IOException e) {
            e.printStackTrace();
        }
        return SEresult;
    }

    @Override
    public List<Map<String, String>> head(String[] attributeNames) {
        List<Map<String, String>> output = new ArrayList<>();
        Map<String, String> se_mapping = new HashMap<>();
        System.out.println("Loading search keys");
        String ksName = "clarus_keystore";
        char[] ksPassword = KeyManagementUtils.askPassword(ksName);
        KeyStore myKS;
        try {
            myKS = KeyManagementUtils.loadKeyStore(ksName, ksPassword);
            SecretKey encryption_Key = KeyManagementUtils.loadSecretKey(myKS, "encKey", ksPassword);

            for (int i = 0; i < attributeNames.length; i++) {
                SecretKey newSK = KeyManagementUtils.hashAESKey(encryption_Key, Integer.toString(i + 1));
                se_mapping.put(attributeNames[0], Encryptor.encrypt(attributeNames[i], newSK));
            }
        } catch (Exception e) {
            System.out.println("[FAILURE]");
            e.printStackTrace();
        }
        output.add(se_mapping);
        return output;
    }

    @Override
    public List<DataOperationCommand> put(String[] attributeNames, Criteria[] criteria, String[][] contents) {
        return null;
    }

    @Override
    public List<DataOperationCommand> delete(String[] attributeNames, Criteria[] criteria) {
        return null;
    }

}
