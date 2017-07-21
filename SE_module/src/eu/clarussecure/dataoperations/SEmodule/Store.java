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
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;


import eu.clarussecure.dataoperations.DataOperationCommand;
import eu.clarussecure.dataoperations.Mapping;

public class Store {
	private static SecretKey encryption_Key;
	private static SecretKey prfKey;
	private static SecretKey permKey;
	private static String[][] contents;
	private static String[] attributes;
	private static String[][] encrypted_content;
	private static String[] encrypted_attributes;
	private static String[] extraProtectedAttributeNames = {"index"};
	private static Mapping mapping = new Mapping();
	private static ProgressBar bar;
	
	private static ArrayList<Object> index;



	public static final List<DataOperationCommand> store_with_SE(String[] attributeNames,
			String[][] data_contents) throws Exception {

		List<DataOperationCommand> myList = new ArrayList<DataOperationCommand>();
		SearchableEncryptionCommand SE_post_query = new SearchableEncryptionCommand();


		System.out.println("Step 01: Generate Keying material");
		//Generate keys and store them in key store
		SecretKey[] keys = new SecretKey[3];
		keys = KeyManagementUtils.procedureKeyGen();
		encryption_Key = keys[0];
		prfKey = keys[1];
		permKey = keys[2];


		System.out.println("\nStep 02: Shuffling rows");
		int[] permuted = new int[data_contents.length];
		permuted = Permutation.permute_array(data_contents.length);
		contents = new String[data_contents.length][attributeNames.length];
		for(int i=0; i<data_contents.length; i++){
			contents[i] = data_contents[permuted[i]].clone();
		}
		attributes = attributeNames;

		System.out.println("\nStep 03: Generate secure index");
		//ArrayList<Object> index = BuildIndex.buildIndex(attributes, contents, prfKey, permKey);	
		index = BuildIndex.buildIndex(attributes, contents, prfKey, permKey);	

		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		ObjectOutputStream byte_index = new ObjectOutputStream(byteOut);
		byte_index.writeObject(index);
	
		InputStream is_index = new ByteArrayInputStream(byteOut.toByteArray());
		byte_index.close();
		byteOut.close();


		encrypted_attributes = new String[attributes.length+1];
		encrypted_content = new String[contents.length][contents[0].length+1];

		
		System.out.println("\n\nStep 04: Encrypt attributes and data");

		// Encrypt attributes
		SecretKey newSK;
		for (int i = 0; i < attributeNames.length; i++) {
			newSK = KeyManagementUtils.hashAESKey(encryption_Key,
					Integer.toString(i + 1));
			encrypted_attributes[i] = Encryptor.encrypt(attributes[i], newSK);
		}
		encrypted_attributes[attributeNames.length]="rowID";
		
		// Encrypt data
		bar = new ProgressBar();
		bar.update(0, contents.length);
		for (int i = 0; i < contents.length; i++) {
			for (int j = 0; j < contents[0].length; j++) {
				newSK = KeyManagementUtils.hashAESKey(encryption_Key,
						Integer.toString(i + j + 1));
				encrypted_content[i][j] = Encryptor.encrypt(contents[i][j], newSK);
			}
			encrypted_content[i][contents[0].length]=Integer.toString(i+1);
			bar.update(i, contents.length);
		}
		System.out.println("Plain content:\n" + Arrays.deepToString(contents).replaceAll("],", "]," + System.getProperty("line.separator")) + "\n");
		System.out.println(encrypted_content.length + " rows have been encrypted\n");


		/**
		 * Creating output
		 */
		// Output is a SearchableEncryptionCommand object
		SE_post_query.setProtectedAttributeNames(encrypted_attributes);
		SE_post_query
		.setExtraProtectedAttributeNames(extraProtectedAttributeNames);
		InputStream[] extraBinaryContent = {is_index};
		SE_post_query.setExtraBinaryContent(extraBinaryContent);
		for(int i=0; i<encrypted_attributes.length-1; i++){
			mapping.put(attributes[i], encrypted_attributes[i]);
		}
		SE_post_query.setMapping(mapping);
		SE_post_query.setProtectedContents(encrypted_content);
		myList.add(SE_post_query);
		return myList;

	}

	public static ArrayList<Object> getIndex(){
		return index;
	}


}
