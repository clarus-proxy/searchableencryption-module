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


package search_with_SE;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import org.postgresql.pljava.annotation.Function;

import eu.clarussecure.dataoperations.SEmodule.CuckooHashMap;

public class Cloud {
	@SuppressWarnings("unchecked")
	@Function
	public static Iterator<String> search_with_SE(Object index_bytes, String[] trapdoor ) throws Exception{
		if(index_bytes==null||trapdoor==null)
			return null;
		
		
		ArrayList<Object> indexDB = new ArrayList<>();

		byte[] test_index = (byte[])index_bytes;

		ByteArrayInputStream bis_index = new ByteArrayInputStream(test_index);
		ObjectInputStream ois = new ObjectInputStream(bis_index);
		indexDB = (ArrayList<Object>)ois.readObject();
		ois.close();
		bis_index.close();

		// Declare list of docIds (i.e. the search results)
		ArrayList<String> docIDs = new ArrayList<String>();

		// Declare the 2 data structures of the index
		// arrayA of linked list nodes
		// lookupT is the Cuckoo hash table
		String[] arrayA;

		CuckooHashMap<String, String> lookupT = new CuckooHashMap<String, String>();

		// Reconstruct the 2 data structures from the byte array of index
		arrayA = (String[]) indexDB.get(0);
		lookupT =(CuckooHashMap<String, String>) indexDB.get(1);

		// Instantiate the two values of the trapdoors
		//ArrayList<String> list = new ArrayList<String>(Arrays.asList(trapdoor));
		String posInT = trapdoor[1];
		String XORkey = trapdoor[0];

		// Read from lookup table T the entry in position [TrapKey]
		String teta = lookupT.get(posInT);
		if (teta == null) {
			docIDs.add("Word not found");
		} else {
			//Decrypt the entry in table T with TrapValue
			String alphaK = Xor(teta, XORkey);

			// Read address in arrayA of first node and the key to decrypt it
			Vector<String> alphaArray = new Vector<String>(Arrays.asList(alphaK.split(Constants.SEPARATOR.getValue())));
			boolean availableElement = true;
			int nextPosition = Integer.parseInt(alphaArray.get(0));
			String strKey = alphaArray.get(1);
			do {

				//Get the node from the A array
				String node = arrayA[nextPosition];

				//Decrypt the node - nodeData : < docID || nextKey || number >
				//nodePointer : < Position of next Node>	 
				//node.decrypt(strKey);
				String decrypted_node = Encryptor.decrypt(strKey, node);				
				alphaArray = new Vector<String>(Arrays.asList(decrypted_node.split(Constants.SEPARATOR.getValue())));
				docIDs.add(alphaArray.get(0));
				strKey = alphaArray.get(1);
				nextPosition = Integer.parseInt(alphaArray.get(2));
				if(nextPosition==-1){
					availableElement = false;
				}
			} while (availableElement == true);

		}

		try{
			for (int i=0;i<docIDs.size();i++){
				String temp = docIDs.get(i).split("-")[0];
				String temp2 = temp.split("row")[1];
				docIDs.set(i, temp2);
			}
		}catch(Exception e){
			return new ArrayList<String>().iterator();
		}
		return docIDs.iterator();
	}//End of search

	


	/** Implementation
	 * of a XOR encryption function
	 **/
	public static String Xor(String a, String XorKey){

		char[] key = XorKey.toCharArray();
		StringBuilder output = new StringBuilder();

		int aL = a.length();
		int kL = key.length;

		for(int i = 0; i < aL; i++) {
			output.append((char) (a.charAt(i) ^ key[i % kL]));
		}
		return output.toString();
	}//End of Xor
}

