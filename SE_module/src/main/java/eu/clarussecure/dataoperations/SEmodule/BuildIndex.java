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

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.crypto.SecretKey;

import org.apache.commons.lang.RandomStringUtils;

public class BuildIndex {

    private static Map<String, LinkedList<String>> dictionary = new HashMap<String, LinkedList<String>>();
    private static int numberOfElements = 0;

    public static ArrayList<Object> buildIndex(String[] attributes, String[][] contents, SecretKey prfKey,
            SecretKey permKey) throws Exception {
        Map<String, LinkedList<String>> dictionary = BuildIndex.CreateDictionary(attributes, contents);
        int numberOfElements = BuildIndex.getNumberOfElements();
        int sizeOfArrayA = (int) Math.pow(2, Math.ceil(Math.log(numberOfElements) / Math.log(2)));
        ArrayList<Object> index = BuildIndex.BuildAandT(dictionary, sizeOfArrayA, prfKey, permKey);
        return index;

    }

    public static Map<String, LinkedList<String>> CreateDictionary(String[] attributes, String[][] contents) {

        /**
         * Create a dictionary of distinct keywords
         * Each entry of the dictionary corresponds to a particular keyword
         * and lists the ID of records that contain this keyword (as a linked list)
         */
        System.out.println("Create dictionary");
        int col = attributes.length;
        int row = contents.length;
        ProgressBar bar = new ProgressBar();
        bar.update(0, row);
        for (int r = 0; r < row; r++) {
            String rowID = "row" + String.valueOf(r + 1);
            String[] record = contents[r];
            for (int c = 0; c < col; c++) {
                String keyword = attributes[c] + "='" + record[c] + "'";
                if (!dictionary.containsKey(keyword)) {
                    //the keyword not yet inserted in the dictionary
                    //create a linked list and insert the current ID
                    LinkedList<String> ll = new LinkedList<String>();
                    ll.add(rowID);
                    numberOfElements++;
                    //insert in the dictionary
                    dictionary.put(keyword, ll);
                } else {
                    //the keyword already in the dictionary
                    //update the linked list with the current ID
                    LinkedList<String> ll = dictionary.get(keyword);
                    ll.add(rowID);
                    numberOfElements++;
                    dictionary.put(keyword, ll);
                }
            }
            bar.update(r, row);
        }
        return dictionary;
    }

    public static ArrayList<Object> BuildAandT(Map<String, LinkedList<String>> dictionary, int sizeOfA,
            SecretKey prfKey, SecretKey permKey) throws Exception {
        System.out.println("Create index from dictionary");
        int counter = 0;
        String[] arrayA = new String[sizeOfA];
        CuckooHashMap<String, String> T = new CuckooHashMap<String, String>();
        int[] permuted = new int[sizeOfA];
        String k_ij;
        String node;
        String encrypted_node;

        permuted = Permutation.permute_array(sizeOfA);
        Set<Entry<String, LinkedList<String>>> dict = dictionary.entrySet();
        Iterator<Entry<String, LinkedList<String>>> it = dict.iterator();
        ProgressBar bar = new ProgressBar();
        int r = 0;
        bar.update(r, dict.size());
        while (it.hasNext()) {
            Entry<String, LinkedList<String>> e = it.next();
            //generate first key for encrypting first node
            String k_i0 = RandomStringUtils.randomAlphabetic(16);

            //define info about first node to be stored in T
            String firstNodeInformation = permuted[counter] + "||" + k_i0;
            String XorKey = Base64.getEncoder().encodeToString(Encryptor.prf(e.getKey(), prfKey));

            String encryptedFirstNodeInfo = Encryptor.Xor(firstNodeInformation, XorKey);

            // store it in cuckoo table T
            T.put(Encryptor.encrypt(e.getKey(), permKey), encryptedFirstNodeInfo);

            //fill array A
            Iterator<String> it_ll = e.getValue().iterator();
            String current_key = k_i0;
            for (int i = 0; i < e.getValue().size(); i++) {
                if (i == e.getValue().size() - 1) {
                    node = it_ll.next() + "||" + "-||-1";
                    encrypted_node = Encryptor.encrypt(current_key, node);
                    arrayA[permuted[counter]] = encrypted_node;
                    counter++;

                } else {
                    k_ij = RandomStringUtils.randomAlphabetic(16);
                    node = it_ll.next() + "||" + k_ij + "||" + permuted[counter + 1];
                    encrypted_node = Encryptor.encrypt(current_key, node);
                    arrayA[permuted[counter]] = encrypted_node;
                    current_key = k_ij;
                    counter++;
                }
            }
            r++;
            bar.update(r, dict.size());
        }
        // Fill remaining A's entries with random stuff
        for (int i = counter + 1; i < sizeOfA; i++) {
            String key = RandomStringUtils.randomAlphabetic(16);
            arrayA[permuted[i]] = Encryptor.encrypt(key, key);
        }

        ArrayList<Object> output = new ArrayList<Object>();
        output.add(arrayA);
        output.add(T);
        return output;
    }

    public static int getNumberOfElements() {
        return numberOfElements;
    }
}
