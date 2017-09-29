package eu.clarussecure.dataoperations.SEmodule;

import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;

import org.apache.log4j.Logger;

import eu.clarussecure.dataoperations.Criteria;
import eu.clarussecure.dataoperations.DataOperationCommand;

public class Query {

    private static Logger logger = Logger.getLogger(Query.class);

    // AKKA fix: new 'indexes' parameter to compute salts
    //static List<DataOperationCommand> search_with_SE(String[] attributeNames, Criteria[] criteria) throws Exception {
    static List<DataOperationCommand> search_with_SE(String[] attributeNames, Criteria[] criteria, int[] indexes)
            throws Exception {

        List<DataOperationCommand> myList = new ArrayList<DataOperationCommand>();
        SearchableEncryptionCommand SE_search_query = new SearchableEncryptionCommand();
        ArrayList<Criteria> myCriteria = new ArrayList<Criteria>();

        String keyword;
        String[] trap = null;

        logger.info("Loading search keys");
        String ksName = "clarus_keystore";
        char[] ksPassword = KeyManagementUtils.askPassword(ksName);
        KeyStore myKS = KeyManagementUtils.loadKeyStore(ksName, ksPassword);
        SecretKey y_Key = KeyManagementUtils.loadSecretKey(myKS, "y_Key", ksPassword);
        SecretKey z_Key = KeyManagementUtils.loadSecretKey(myKS, "z_Key", ksPassword);
        SecretKey encryption_Key = KeyManagementUtils.loadSecretKey(myKS, "encKey", ksPassword);

        logger.info("\nGenerating trapdoors...\n");
        logger.info("Number of criteria = " + criteria.length);
        for (int tt = 0; tt < criteria.length; tt++) {
            logger.info(
                    criteria[tt].getAttributeName() + "" + criteria[tt].getOperator() + " " + criteria[tt].getValue());
        }
        for (int i = 0; i < criteria.length; i++) {
            /*// extract keyword from criteria
            keyword = criteria[i].getAttributeName() + criteria[i].getOperator() + criteria[i].getValue();
            // generate trapdoor from that keyword
            try {
            trap = generateTrapdoor(keyword, y_Key, z_Key);
            } catch (IOException e) {
            logger.info("[FAILURE:] Trapdoor generation for keyword " + keyword);
            }
            }*/

            // extract keyword from criteria
            keyword = criteria[i].getAttributeName() + criteria[i].getOperator() + criteria[i].getValue();

            // generate trapdoor from that keyword
            try {
                trap = generateTrapdoor(keyword, y_Key, z_Key);
            } catch (IOException e) {
                logger.info("[FAILURE:] Trapdoor generation for keyword " + keyword);
            }
            String query = "(select * from search_with_SE((select " + "index" + " from " + Constants.tableName
                    + Constants.indexName + "),ARRAY['" + trap[0] + "', '" + trap[1] + "']))";
            Criteria trapdoor = new Criteria("rowID", "IN", query);
            myCriteria.add(trapdoor);
        }

        // Encrypt attributes
        // AKKA fix: declare the 'rowID' in the encrypted attributes (proxy has to be aware of added attributes)
        String[] encrypted_attributes = new String[attributeNames.length + 1];
        SecretKey newSK;
        for (int i = 0; i < attributeNames.length; i++) {
            // AKKA fix: compute salts according to the 'indexes' parameter
            //newSK = KeyManagementUtils.hashAESKey(encryption_Key, Integer.toString(i + 1));
            newSK = KeyManagementUtils.hashAESKey(encryption_Key, Integer.toString(indexes[i] + 1));
            // AKKA fix: encode attribute to be URL and filename safe (without / character)
            //encrypted_attributes[i] = Encryptor.encrypt(attributeNames[i], newSK);
            encrypted_attributes[i] = Encryptor.encrypt(attributeNames[i], newSK, true);
        }
        // AKKA fix: declare the 'rowID' in the encrypted attributes (proxy has to be aware of added attributes)
        encrypted_attributes[attributeNames.length] = "rowID";

        // Output is a SearchableEncryptionCommand object
        SE_search_query.setProtectedAttributeNames(encrypted_attributes);
        // AKKA fix: verify if criteria exists
        if (criteria.length > 0) {
            // AKKA fix: build query using the Constant.tableName
            //String query = "(select * from search_with_SE((select index from " + Constants.tableName + Constants.indexName
            //        + "),ARRAY['" + trap[0] + "', '" + trap[1] + "']))";
            //String query = "(select * from search_with_SE((select " + "index" + " from " + Constants.tableName
            //        + Constants.indexName + "),ARRAY['" + trap[0] + "', '" + trap[1] + "']))";
            //Criteria trapdoor = new Criteria("rowID", "IN", query);
            //myCriteria.add(trapdoor);
        }

        //AKKA fix: use setter method instead of access field
        //SE_search_query.criteria = myCriteria;
        SE_search_query.setCriteria(myCriteria.toArray(new Criteria[myCriteria.size()]));

        myList.add(SE_search_query);

        return myList;
    }

    private static String[] generateTrapdoor(String keyword, SecretKey prfKey, SecretKey permKey) throws Exception {
        String[] trapdoor = new String[2];
        trapdoor[0] = Base64.getEncoder().encodeToString(Encryptor.prf(keyword, prfKey));//XORkey
        trapdoor[1] = Encryptor.encrypt(keyword, permKey);//posInT
        return trapdoor;
    }

}
