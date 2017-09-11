package eu.clarussecure.dataoperations.SEmodule;

import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;

import eu.clarussecure.dataoperations.Criteria;
import eu.clarussecure.dataoperations.DataOperationCommand;

public class Query {

	static List<DataOperationCommand> search_with_SE(
			String[] attributeNames, Criteria[] criteria)
					throws Exception {

		List<DataOperationCommand> myList = new ArrayList<DataOperationCommand>();
		SearchableEncryptionCommand SE_search_query = new SearchableEncryptionCommand();

		String keyword;
		String[] trap = null;

		System.out.println("Loading search keys");
		String ksName = "clarus_keystore";
		char[] ksPassword = KeyManagementUtils.askPassword(ksName);
		KeyStore myKS = KeyManagementUtils.loadKeyStore(ksName, ksPassword);
		SecretKey y_Key = KeyManagementUtils.loadSecretKey(myKS, "y_Key", ksPassword);
		SecretKey z_Key = KeyManagementUtils.loadSecretKey(myKS, "z_Key", ksPassword);
		SecretKey encryption_Key = KeyManagementUtils.loadSecretKey(myKS, "encKey", ksPassword);

		System.out.println("\nGenerating trapdoors...\n");
		for (int i = 0; i < criteria.length; i++) {
			// extract keyword from criteria
			keyword = criteria[i].getAttributeName()
					+ criteria[i].getOperator() + criteria[i].getValue();

			// generate trapdoor from that keyword
			try {
				trap = generateTrapdoor(keyword, y_Key, z_Key);
			} catch (IOException e) {
				System.out
				.println("[FAILURE:] Trapdoor generation for keyword "
						+ keyword);
			}
		}

		// Encrypt attributes
		String[] encrypted_attributes=new String[attributeNames.length];
		SecretKey newSK;
		for (int i = 0; i < attributeNames.length; i++) {
			newSK = KeyManagementUtils.hashAESKey(encryption_Key,
					Integer.toString(i + 1));
			encrypted_attributes[i] = Encryptor.encrypt(attributeNames[i], newSK);
		}


		// Output is a SearchableEncryptionCommand object
		SE_search_query.setProtectedAttributeNames(encrypted_attributes);
		Criteria[] myCriteria = new Criteria[criteria.length];
		String query = "(select * from search_with_SE((select index from " + 
				Constants.tableName + Constants.indexName
				+ "),ARRAY['" + trap[0] + "', '" +  trap[1] + "']))";
		Criteria trapdoor = new Criteria("rowID", "IN", query);
		myCriteria[0] = trapdoor;
		SE_search_query.criteria = myCriteria;
		myList.add(SE_search_query);

		return myList;
	}
	
	public static String[] generateTrapdoor(String keyword, SecretKey prfKey, SecretKey permKey) throws Exception{
		String [] trapdoor = new String[2];
		trapdoor[0] = Base64.getEncoder().encodeToString(Encryptor.prf(keyword, prfKey));//XORkey
		trapdoor[1] = Encryptor.encrypt(keyword, permKey);//posInT
		return trapdoor;
	}
	
	
}