package eu.clarussecure.dataoperations.SEmodule;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

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

        List<DataOperationCommand> myList = new ArrayList<>();
        SearchableEncryptionCommand SE_search_query = new SearchableEncryptionCommand();
        ArrayList<Criteria> myCriteria = new ArrayList<>();

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
        for (Criteria criterion : criteria) {
            logger.info(criterion.getAttributeName() + "" + criterion.getOperator() + "" + criterion.getValue());
        }
        for (int i = 0; i < criteria.length; i++) {
            if (!(criteria[i].getAttributeName() == null || criteria[i].getValue() == null)) {
                //test if it is a range query 
                if (!Store.ranges.isEmpty() && (Store.ranges.containsKey(criteria[i].getAttributeName()))
                        && (">=".equals(criteria[i].getOperator()) || ">".equals(criteria[i].getOperator())
                                || "<=".equals(criteria[i].getOperator()) || "<".equals(criteria[i].getOperator()))) {
                    //range query !!!
                    Map<String, String[]> ListTrapdoorForRange = new HashMap<>();
                    myCriteria.add(new Criteria("", "(", ""));

                    // Montimage fix: This line generates an ArrayIndexOutOfBounds
                    // when the criteria is single (attrib<max || attrib>min)
                    // Adding a "check if next array element exists" fix
                    // Montimage fix: String comparison is made with "equals" rather
                    // than "=="
                    if ((i + 1) < criteria.length
                            && criteria[i].getAttributeName().equals(criteria[i + 1].getAttributeName())) {
                        //type attribute >= value_min AND attribute <= value_max
                        if ((">=".equals(criteria[i].getOperator()) || ">".equals(criteria[i].getOperator()))
                                && ("<".equals(criteria[i + 1].getOperator())
                                        || "<=".equals(criteria[i + 1].getOperator()))) {
                            // generate trapdoors for that range
                            ListTrapdoorForRange = generateTrapdoorforRange(criteria[i], criteria[i + 1], y_Key, z_Key);
                            i = i + 1;
                        }
                        //type attribute <= value_max AND attribute >= value_min
                        else if (("<=".equals(criteria[i].getOperator()) || "<".equals(criteria[i].getOperator()))
                                && (">".equals(criteria[i + 1].getOperator())
                                        || ">=".equals(criteria[i + 1].getOperator()))) {

                            // generate trapdoors for that range
                            ListTrapdoorForRange = generateTrapdoorforRange(criteria[i + 1], criteria[i], y_Key, z_Key);
                            i = i + 1;
                        }
                    } else if ("<=".equals(criteria[i].getOperator()) || "<".equals(criteria[i].getOperator())) {
                        // generate trapdoors for that range
                        Criteria fake = new Criteria(null, null, null);
                        ListTrapdoorForRange = generateTrapdoorforRange(fake, criteria[i], y_Key, z_Key);
                    } else if (">=".equals(criteria[i].getOperator()) || ">".equals(criteria[i].getOperator())) {
                        // generate trapdoors for that range
                        Criteria fake = new Criteria(null, null, null);
                        ListTrapdoorForRange = generateTrapdoorforRange(criteria[i], fake, y_Key, z_Key);
                    }

                    SortedSet<String> keywords = new TreeSet<>(ListTrapdoorForRange.keySet());
                    for (Iterator<String> it = keywords.iterator(); it.hasNext();) {
                        keyword = it.next();
                        System.out.println("Trapdoor for keyword" + keyword);
                        trap = ListTrapdoorForRange.get(keyword);
                        System.out.println("[" + trap[0] + ", " + trap[1] + "\n");

                        // Montimage fix: Use Constants.indexName instead "index" as 
                        // the index table column name
                        // Montimage fix: Use only Constants.tableName as the index
                        // table name
                        String query = "(select * from search_with_SE((select " + Constants.indexName + " from "
                                + Constants.tableName + "),ARRAY['" + trap[0] + "', '" + trap[1] + "']))";
                        Criteria trapdoor = new Criteria("rowID", "IN", query);
                        myCriteria.add(trapdoor);
                        if (it.hasNext()) {
                            myCriteria.add(new Criteria("", "OR", ""));
                        }
                    }
                    myCriteria.add(new Criteria("", ")", ""));
                    //System.out.println("[\033[1;34m" + nb_traps+" trapdoors generated !!\u001B[0m]\n");
                }

                else {
                    // extract keyword from criteria
                    keyword = criteria[i].getAttributeName() + criteria[i].getOperator() + criteria[i].getValue();

                    // generate trapdoor from that keyword
                    try {
                        trap = generateTrapdoor(keyword, y_Key, z_Key);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("[FAILURE:] Trapdoor generation for keyword " + keyword);
                    }

                    System.out.println("Trapdoor for keyword " + keyword);
                    System.out.println("[" + trap[0] + ", " + trap[1] + "]\n");

                    // Montimage fix: Use Constants.indexName instead "index" as 
                    // the index table column name
                    // Montimage fix: Use only Constants.tableName as the index
                    // table name
                    String query = "(select * from search_with_SE((select " + Constants.indexName + " from "
                            + Constants.tableName + "),ARRAY['" + trap[0] + "', '" + trap[1] + "']))";

                    Criteria trapdoor = new Criteria("rowID", "IN", query);
                    myCriteria.add(trapdoor);
                }
            }

            else {
                myCriteria.add(new Criteria("", criteria[i].getOperator(), ""));
            }
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

    private static Map<String, String[]> generateTrapdoorforRange(Criteria rangeInf, Criteria rangeSup,
            SecretKey prfKey, SecretKey permKey) throws Exception {
        Map<String, String[]> ListTrapdoorForRange = new HashMap<>();
        String a;
        if (rangeInf.getAttributeName() != null) {
            a = rangeInf.getAttributeName();
        } else {
            a = rangeSup.getAttributeName();
        }
        Map<String, String>[] config = loadConfig(Constants.tableName + ".config");
        Map<String, String> minmax = config[0];
        Map<String, String> range_config = config[1];
        String min = minmax.get(a).split(":")[0];
        String max = minmax.get(a).split(":")[1];
        if (rangeInf.getAttributeName() == null && rangeInf.getOperator() == null && rangeInf.getValue() == null) {
            rangeInf.setAttributeName(a);
            rangeInf.setOperator(">=");
            rangeInf.setValue(min);
        }
        if (rangeSup.getAttributeName() == null && rangeSup.getOperator() == null && rangeSup.getValue() == null) {
            rangeSup.setAttributeName(a);
            rangeSup.setOperator("<=");
            rangeSup.setValue(max);
        }
        int ll = Integer.valueOf(minmax.get(a).split(":")[2]);
        int initial = Integer.valueOf(range_config.get(a).split(":")[0]);
        int range = Integer.valueOf(range_config.get(a).split(":")[1]);

        /*  Example: attribute age. Range added in the search index of length 10. So of the form [0-9], [10, 19], etc...
         * More formally, if we note range_length = r and initial value = v then the interval are of the form
         * [v + ir ; v + (i+1)r - 1] with i=0, 1, 2, ... 
         * Range search query if for ex, 18 <= age <= 55. (a <= attribute <= b)
         * first expand [18-55] into intervals that have been added in the search index [20-29], [30, 39], [40, 49]
         * then you have remaining "discrete" values : 18, 19, 50, 51, 52, 53, 54, 55
         * for the range, generate trapdoor of keyword = "RANGE_age='20-29', "RANGE_age='30-39 etc
         * for the discrete value, generate trapdoor of keyword age='18', age='19', age='50', etc...		 * 		
         */

        /*
         * to test whether my range_query min is one of the intervals' min, check if (range_query_min - initial)/range_length is an integer
         * same to test whether my range_query max is one the intervals' max
         */
        int inf_discrete;
        int sup_discrete;
        double x = (((double) (Integer.valueOf(rangeInf.getValue())) - (double) initial) / (double) range);
        double y = (((double) (Integer.valueOf(rangeSup.getValue()) + 1) - (double) initial) / (double) range);

        // Montimage fix: String comparison using "equals" instead of "=="
        if (x % 1 == 0 && ">=".equals(rangeInf.getOperator())) {
            inf_discrete = Integer.valueOf(rangeInf.getValue());
        } else if (x % 1 == 0 && ">".equals(rangeInf.getOperator())) {
            inf_discrete = ((int) (x + 1)) * range + initial;
        } else {
            inf_discrete = ((int) Math.ceil(x)) * range + initial;
        }
        if (y % 1 == 0 && "<=".equals(rangeSup.getOperator())) {
            sup_discrete = Integer.valueOf(rangeSup.getValue());
        } else if (y % 1 == 0 && "<".equals(rangeSup.getOperator())) {
            sup_discrete = ((int) (y - 1)) * range + initial;
        } else {
            sup_discrete = ((int) Math.floor(y)) * range + initial;
        }

        if (inf_discrete > Integer.valueOf(rangeInf.getValue())) {
            for (int ii = 0; ii < inf_discrete - Integer.valueOf(rangeInf.getValue()); ii++) {
                //compute trapdoor for rangeInf (18) to inf_discrete (20)
                if (ii == 0 && !">=".equals(rangeInf.getOperator()))
                    continue;
                int val = Integer.valueOf(rangeInf.getValue()) + ii;
                String keyword = a + "=" + String.format("%0" + ll + "d", val);
                ListTrapdoorForRange.put(keyword, generateTrapdoor(keyword, prfKey, permKey));
            }
        }
        int nb_ranges = (sup_discrete + 1 - inf_discrete) / range;
        for (int ii = 0; ii < nb_ranges; ii++) {
            int val1 = inf_discrete + ii * range;
            int val2 = inf_discrete + (ii + 1) * range - 1;
            //System.out.println("trapdoor for [" + "RANGE_" + a+"="+ String.format("%0"+ll+"d",val1) + "-" + String.format("%0"+ll+"d",val2) +"]");
            String keyword = "RANGE_" + a + "=" + String.format("%0" + ll + "d", val1) + "-"
                    + String.format("%0" + ll + "d", val2);
            ListTrapdoorForRange.put(keyword, generateTrapdoor(keyword, prfKey, permKey));
        }

        if (Integer.valueOf(rangeSup.getValue()) > sup_discrete) {
            for (int ii = 0; ii < Integer.valueOf(rangeSup.getValue()) - sup_discrete; ii++) {
                //compute trapdoor for sup_discrete (49) to rangeSup (55)
                int val = sup_discrete + ii;
                //System.out.println("trapdoor for [" + a+"="+ String.format("%0"+ll+"d",val) + "]");
                String keyword = a + "=" + String.format("%0" + ll + "d", val);
                ListTrapdoorForRange.put(keyword, generateTrapdoor(keyword, prfKey, permKey));
            }
        }
        //System.out.println("trapdoor for [" + a+"="+ String.format("%0"+ll+"d",Integer.valueOf(rangeSup.getValue())) + "]");
        if ("<=".equals(rangeSup.getOperator()) && Integer.valueOf(rangeSup.getValue()) != sup_discrete) {
            String keyword = a + "="+ String.format("%0" + ll + "d", Integer.valueOf(rangeSup.getValue()));
            ListTrapdoorForRange.put(keyword, generateTrapdoor(keyword, prfKey, permKey));
        }

        return ListTrapdoorForRange;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String>[] loadConfig(String filename)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        Map<String, String>[] config = new Map[2];
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            config = (Map<String, String>[]) in.readObject();
        }
        return config;
    }

}
