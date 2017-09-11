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
import java.util.List;

import javax.crypto.SecretKey;

import eu.clarussecure.dataoperations.DataOperationCommand;
import eu.clarussecure.dataoperations.DataOperationResult;

public class Retrieve {

    public static List<DataOperationResult> decrypt_result(List<DataOperationCommand> promise,
            List<String[][]> contents) throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
            IOException, UnrecoverableEntryException, SQLException {

        // Declare output
        List<DataOperationResult> output = new ArrayList<DataOperationResult>();
        SearchableEncryptionResponse SE_search_response = new SearchableEncryptionResponse();
        ;

        // Load encryption key
        String ksName = "clarus_keystore";
        char[] ksPassword = KeyManagementUtils.askPassword(ksName);
        KeyStore myKS = KeyManagementUtils.loadKeyStore(ksName, ksPassword);
        SecretKey encryption_Key = KeyManagementUtils.loadSecretKey(myKS, "encKey", ksPassword);

        // Decrypt attributes
        String[] encrypted_attribute_names = promise.get(0).getProtectedAttributeNames();
        String[] decrypted_attribute_names = new String[encrypted_attribute_names.length - 1];
        SecretKey newSK;

        for (int i = 0; i < decrypted_attribute_names.length; i++) {
            try {
                newSK = KeyManagementUtils.hashAESKey(encryption_Key, Integer.toString(i + 1));
                decrypted_attribute_names[i] = Encryptor.decrypt(encrypted_attribute_names[i], newSK);
            } catch (Exception e) {
                System.out.println("Decryption failure");
                e.printStackTrace();
            }

        }

        String[][] encrypted_retrieved_results = contents.get(0);
        String[][] decrypted_content = new String[encrypted_retrieved_results.length][encrypted_retrieved_results[0].length
                - 1];

        // Decrypt data
        int row_number;
        /*		for (int i = 0; i < encrypted_retrieved_results.length; i++) {
        			// Retrieve the row_numbers of the retrieved (encrypted rows)
        			// They can be find in the last column of encrypted_retrieved_results matrix
        			row_number[i] = Integer.parseInt(encrypted_retrieved_results[i][encrypted_retrieved_results[0].length - 1])-1;
        		}*/
        ProgressBar bar = new ProgressBar();
        bar.update(0, encrypted_retrieved_results.length);
        for (int i = 0; i < encrypted_retrieved_results.length; i++) {
            for (int j = 0; j < encrypted_retrieved_results[0].length - 1; j++) {
                row_number = Integer.parseInt(encrypted_retrieved_results[i][encrypted_retrieved_results[0].length - 1])
                        - 1;

                try {
                    newSK = KeyManagementUtils.hashAESKey(encryption_Key, Integer.toString(row_number + j + 1));
                    decrypted_content[i][j] = Encryptor.decrypt(encrypted_retrieved_results[i][j], newSK);
                } catch (Exception e) {
                    System.out.println("Decryption failure");
                }
            }
            bar.update(i, encrypted_retrieved_results.length);
        }
        SE_search_response.setContents(decrypted_content);
        SE_search_response.setAttributeNames(decrypted_attribute_names);
        output.add(SE_search_response);

        return output;
    }
}
