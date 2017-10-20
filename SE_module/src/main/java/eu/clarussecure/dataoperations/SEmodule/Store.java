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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import eu.clarussecure.dataoperations.DataOperationCommand;

import org.apache.log4j.Logger;

public class Store {
    private static SecretKey encryption_Key;
    private static SecretKey prfKey;
    private static SecretKey permKey;
    private static String[][] contents;
    private static String[] attributes;
    private static String[][] encrypted_content;
    private static String[] encrypted_attributes;
    private static String[] extraProtectedAttributeNames = { Constants.tableName + "/" + Constants.indexName };
    private static Map<String, String> mapping = new HashMap<>();

    private static ArrayList<Object> index;

    private static Logger logger = Logger.getLogger(Store.class);

    public static Map<String, String> num_Attr = new HashMap<String, String>();
    public static Map<String, String> ranges = new HashMap<String, String>();

    // AKKA fix: new 'indexes' parameter to compute salts
    //public static final List<DataOperationCommand> store_with_SE(String[] attributeNames, String[][] data_contents)
    //    throws Exception {
    public static final List<DataOperationCommand> store_with_SE(String[] attributeNames, String[][] data_contents,
            int[] indexes) throws Exception {

        List<DataOperationCommand> myList = new ArrayList<DataOperationCommand>();
        SearchableEncryptionCommand SE_post_query = new SearchableEncryptionCommand();

        logger.info("Step 01: Generate Keying material");
        //Generate keys and store them in key store
        SecretKey[] keys = new SecretKey[3];
        // AKKA fix: don't generate secret keys here, just load them
        //keys = KeyManagementUtils.procedureKeyGen();
        keys = KeyManagementUtils.loadSecretKeys();
        encryption_Key = keys[0];
        prfKey = keys[1];
        permKey = keys[2];

        logger.info("\nStep 02: Shuffling rows");
        int[] permuted = new int[data_contents.length];
        permuted = Permutation.permute_array(data_contents.length);
        contents = new String[data_contents.length][attributeNames.length];
        for (int i = 0; i < data_contents.length; i++) {
            contents[i] = data_contents[permuted[i]].clone();
        }
        attributes = attributeNames;

        System.out.println("\nStep 03: Range configuration");
        int num_attr = RangeUtils.checkContent(attributes, contents);
        if (num_attr > 0) {
            System.out.println(num_attr + " numerical attributes found!");
            RangeUtils.askUserForRangeFeature();
        } else {
            System.out.println("Your database does not contain numerical values. No range configuration.");
        }

        if (!Store.ranges.isEmpty()) {
            System.out.println("\nStoring range configuration in a file");
            Map[] config = new Map[2];
            config[0] = Store.num_Attr;
            config[1] = Store.ranges;
            storeConfig(config);
            System.out.println("[OK] File created");
            System.out.println("\n\nStep 04: Update the database with the range configuration");
            String[][] updatedDB = RangeUtils.updateDB(attributes, contents);
            String[] attributes_with_range = new String[updatedDB[0].length];
            attributes_with_range = updatedDB[0].clone();
            String[][] contents_with_range = new String[updatedDB.length - 1][updatedDB[0].length];
            for (int i = 0; i < updatedDB.length - 1; i++) {
                contents_with_range[i] = updatedDB[i + 1].clone();
            }

            System.out.println("\nStep 05: Generate secure index");
            index = BuildIndex.buildIndex(attributes_with_range, contents_with_range, prfKey, permKey);

        } else {
            System.out.println("\nStep 04: Generate secure index");
            index = BuildIndex.buildIndex(attributes, contents, prfKey, permKey);
        }

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream byte_index = new ObjectOutputStream(byteOut);
        byte_index.writeObject(index);

        InputStream is_index = new ByteArrayInputStream(byteOut.toByteArray());
        byte_index.close();
        byteOut.close();

        encrypted_attributes = new String[attributes.length + 1];
        encrypted_content = new String[contents.length][contents[0].length + 1];

        if (!Store.ranges.isEmpty()) {
            System.out.println("\n\nStep 06: Encrypt attributes and data0m");
        } else {
            System.out.println("\n\nStep 05: Encrypt attributes and data");
        }

        // Encrypt attributes
        SecretKey newSK;
        for (int i = 0; i < attributeNames.length; i++) {
            // AKKA fix: compute salts according to the 'indexes' parameter
            //newSK = KeyManagementUtils.hashAESKey(encryption_Key, Integer.toString(i + 1));
            newSK = KeyManagementUtils.hashAESKey(encryption_Key, Integer.toString(indexes[i] + 1));
            // AKKA fix: encode attribute to be URL and filename safe (without / character)
            //encrypted_attributes[i] = Encryptor.encrypt(attributes[i], newSK);
            encrypted_attributes[i] = Encryptor.encrypt(attributes[i], newSK, true);
        }
        encrypted_attributes[attributeNames.length] = "rowID";

        // Encrypt data
        for (int i = 0; i < contents.length; i++) {
            for (int j = 0; j < contents[0].length; j++) {
                // AKKA fix: compute salts according to the 'indexes' parameter
                //newSK = KeyManagementUtils.hashAESKey(encryption_Key, Integer.toString(i + j + 1));
                newSK = KeyManagementUtils.hashAESKey(encryption_Key, Integer.toString(i + 1 + indexes[j]));
                encrypted_content[i][j] = Encryptor.encrypt(contents[i][j], newSK);
            }
            encrypted_content[i][contents[0].length] = Integer.toString(i + 1);
        }
        logger.info("Plain content:\n"
                + Arrays.deepToString(contents).replaceAll("],", "]," + System.getProperty("line.separator")) + "\n");
        logger.info(encrypted_content.length + " rows have been encrypted\n");

        /**
         * Creating output
         */
        // Output is a SearchableEncryptionCommand object
        // AKKA fix: save clear attribute names (proxy needs them)
        SE_post_query.setAttributeNames(attributeNames);
        SE_post_query.setProtectedAttributeNames(encrypted_attributes);
        SE_post_query.setExtraProtectedAttributeNames(extraProtectedAttributeNames);
        InputStream[] extraBinaryContent = { is_index };
        SE_post_query.setExtraBinaryContent(extraBinaryContent);
        for (int i = 0; i < encrypted_attributes.length - 1; i++) {
            mapping.put(attributes[i], encrypted_attributes[i]);
        }
        SE_post_query.setMapping(mapping);
        SE_post_query.setProtectedContents(encrypted_content);
        myList.add(SE_post_query);
        return myList;

    }

    public static ArrayList<Object> getIndex() {
        return index;
    }

    public static void storeConfig(Map[] config) throws FileNotFoundException, IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(Constants.tableName + ".config"));
        out.writeObject(config);
        out.close();
    }

}
