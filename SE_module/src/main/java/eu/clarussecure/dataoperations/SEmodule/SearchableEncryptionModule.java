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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.crypto.SecretKey;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.clarussecure.dataoperations.AttributeNamesUtilities;
import eu.clarussecure.dataoperations.Criteria;
import eu.clarussecure.dataoperations.DataOperation;
import eu.clarussecure.dataoperations.DataOperationCommand;
import eu.clarussecure.dataoperations.DataOperationResult;

public class SearchableEncryptionModule implements DataOperation {
	private static Logger logger = Logger.getLogger(SearchableEncryptionModule.class);

	// AKKA fix: any protection module must load the security policy
	// Data extracted from the security policy
	// fully qualified name->pattern
	private Map<String, Pattern> attributePatterns = new HashMap<>();
	// fully qualified name->type
	private Map<String, String> attributeTypes = new HashMap<>();
	// fully qualified name->data type
	private Map<String, String> dataTypes = new HashMap<>();
	// fully qualified name->index
	private Map<String, Integer> attributeIndexes = new HashMap<>();
	// type->protectionModule
	private Map<String, String> typesProtection = new HashMap<>();
	// type->idKey
	private Map<String, String> typesDataIDs = new HashMap<>();

	public SearchableEncryptionModule(Document policy) {
		// First, get the types of each attribute and build the map
		NodeList nodes = policy.getElementsByTagName("attribute");
		for (int i = 0; i < nodes.getLength(); i++) {
			// Get the node and the list of its attributes
			Node node = nodes.item(i);
			NamedNodeMap attributes = node.getAttributes();
			// Extract the required attributes
			String attributeName = attributes.getNamedItem("name").getNodeValue();
			String fqAttributeName = AttributeNamesUtilities.fullyQualified(attributeName);
			Pattern attributePattern = Pattern.compile(AttributeNamesUtilities.escapeRegex(fqAttributeName));
			String attributeType = attributes.getNamedItem("attribute_type").getNodeValue();
			String dataType = attributes.getNamedItem("data_type").getNodeValue();
			// Add the information to the map
			this.attributePatterns.put(fqAttributeName, attributePattern);
			this.attributeTypes.put(fqAttributeName, attributeType);
			this.dataTypes.put(fqAttributeName, dataType);
			this.attributeIndexes.put(fqAttributeName, i);
		}
		// Second, get the protection of each attribute type and their idKeys
		nodes = policy.getElementsByTagName("attribute_type");
		for (int i = 0; i < nodes.getLength(); i++) {
			// Get the node and the list of its attributes
			Node node = nodes.item(i);
			NamedNodeMap attributes = node.getAttributes();
			// Extract the required attributes
			String attributeType = attributes.getNamedItem("type").getNodeValue();
			String typeProtection = attributes.getNamedItem("protection").getNodeValue();
			// Add the information to the map
			this.typesProtection.put(attributeType, typeProtection.toLowerCase());
			// Get the idKey only if the protection module is "encryption" or
			// "simple"
			if (typeProtection.equals("encryption") || typeProtection.equals("searchable")) {
				String dataID = attributes.getNamedItem("id_key").getNodeValue();
				this.typesDataIDs.put(attributeType, dataID);
			}
		}
		// Third, initialize the keystore
		String ksName = "clarus_keystore";
		// String ksName = System.getProperty("javax.net.ssl.keyStore",
		// System.getProperty("java.home") + "/lib/security/jssecacerts");
		if (!ksName.equals(System.getProperty("java.home") + "/lib/security/jssecacerts")) {
			try {
				Files.deleteIfExists(FileSystems.getDefault().getPath(ksName));
				KeyManagementUtils.procedureKeyGen();
			} catch (IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public List<DataOperationCommand> post(String[] attributeNames, String[][] contents) {
		// AKKA fix: filter attribute names and content according to the security policy
		//        /**
		//         * Generates keying material
		//         * Builds the dictionary of distinct keywords
		//         * Creates the secure search index
		//         * Encrypts the data
		//         */
		//        List<DataOperationCommand> SEcommand = null;
		//        try {
		//            SEcommand = Store.store_with_SE(attributeNames, contents);
		//        } catch (Exception e) {
		//            System.out.println("[FAILURE]: Searchable Encryption post");
		//            System.out.println(e);
		//            System.exit(1);
		//        }
		// First, filter attribute names and content to encrypt
		List<String> allFqAttributeNames = Arrays.stream(attributeNames).map(an -> attributePatterns.entrySet().stream()
				.filter(e -> e.getValue().matcher(an).matches()).findFirst().map(Map.Entry::getKey).orElse(null))
				.collect(Collectors.toList());
		List<Boolean> encryptedAttributeFlags = allFqAttributeNames.stream().map(fqan -> attributeTypes.get(fqan))
				.map(type -> typesProtection.get(type)).map(typeProtection -> Stream.of("encryption", "searchable")
						.anyMatch(protection -> protection.equals(typeProtection)))
						.collect(Collectors.toList());
		List<String> attributeNamesToProcess = IntStream.range(0, attributeNames.length)
				.filter(i -> encryptedAttributeFlags.get(i)).mapToObj(i -> attributeNames[i])
				.collect(Collectors.toList());
		List<Integer> encryptedAttributeIndexes = Arrays.stream(attributeNames)
				.map(an -> attributeNamesToProcess.indexOf(an)).collect(Collectors.toList());
		// only the last part of the attribute name is encrypted
		String[] attributeNamesToEncrypt = attributeNamesToProcess.stream().map(an -> an.split("/"))
				.map(parts -> parts[parts.length - 1]).toArray(String[]::new);
		// filter content to encrypt and replace null values
		String[][] contentToEncrypt = Arrays.stream(contents)
				.map(row -> IntStream.range(0, row.length).filter(c -> encryptedAttributeIndexes.get(c) != -1)
						.mapToObj(c -> row[c]).map(v -> v == null ? "clarus_null" : v).toArray(String[]::new))
						.toArray(String[][]::new);
		// index of the attribute name (use to build salt)
		int[] indexes = IntStream.range(0, allFqAttributeNames.size()).filter(i -> encryptedAttributeFlags.get(i))
				.mapToObj(i -> allFqAttributeNames.get(i)).map(fqan -> attributeIndexes.get(fqan))
				.mapToInt(Integer::intValue).toArray();

		// Second, perform encryption if necessary (only on attributes and
		// content that must be encrypted)
		List<DataOperationCommand> SEcommand = null;
		if (attributeNamesToEncrypt.length > 0) {
			/**
			 * Generates keying material Builds the dictionary of distinct
			 * keywords Creates the secure search index Encrypts the data
			 */
			try {
				SEcommand = Store.store_with_SE(attributeNamesToEncrypt, contentToEncrypt, indexes);
			} catch (Exception e) {
				logger.info("[FAILURE]: Searchable Encryption post");
				logger.info(e);
				System.exit(1);
			}
		} else {
			SEcommand = Collections.singletonList(new SearchableEncryptionCommand());
		}

		// Third, add attributes and contents that must not be encrypted in the
		// response
		SearchableEncryptionCommand postQuery = SearchableEncryptionCommand.class.cast(SEcommand.get(0));
		String[] clearAttributeNames = new String[encryptedAttributeFlags.size()
		                                          + (attributeNamesToEncrypt.length > 0 ? 1 : 0)];
		String[] protectedAttributeNames = new String[encryptedAttributeFlags.size()
		                                              + (attributeNamesToEncrypt.length > 0 ? 1 : 0)];
		String[][] protectedContents = new String[contents.length][protectedAttributeNames.length];
		Map<String, String> mapping = new HashMap<>();
		String[] parts = null;
		for (int i = 0; i < attributeNames.length; i++) {
			clearAttributeNames[i] = attributeNames[i];
			if (encryptedAttributeFlags.get(i)) {
				parts = clearAttributeNames[i].split("/");
				parts[parts.length - 1] = postQuery.getProtectedAttributeNames()[encryptedAttributeIndexes.get(i)];
				protectedAttributeNames[i] = mergeAttributeName(parts);
			} else {
				protectedAttributeNames[i] = clearAttributeNames[i];
			}
			mapping.put(clearAttributeNames[i], protectedAttributeNames[i]);
			for (int r = 0; r < protectedContents.length; r++) {
				protectedContents[r][i] = encryptedAttributeFlags.get(i)
						? postQuery.getProtectedContents()[r][encryptedAttributeIndexes.get(i)] : contents[r][i];
			}
		}
		if (attributeNamesToEncrypt.length > 0) {
			// add additional attributes
			parts[parts.length - 1] = "?attribute1?";
			clearAttributeNames[clearAttributeNames.length - 1] = mergeAttributeName(parts);
			parts[parts.length - 1] = postQuery
					.getProtectedAttributeNames()[postQuery.getProtectedAttributeNames().length - 1];
			protectedAttributeNames[protectedAttributeNames.length - 1] = mergeAttributeName(parts);
			for (int r = 0; r < protectedContents.length; r++) {
				protectedContents[r][protectedAttributeNames.length
				                     - 1] = postQuery.getProtectedContents()[r][postQuery.getProtectedAttributeNames().length - 1];
			}
			mapping.put(clearAttributeNames[clearAttributeNames.length - 1],
					protectedAttributeNames[protectedAttributeNames.length - 1]);
		}
		postQuery.setAttributeNames(clearAttributeNames);
		postQuery.setProtectedAttributeNames(protectedAttributeNames);
		postQuery.setProtectedContents(protectedContents);
		postQuery.setMapping(mapping);
		return SEcommand;
	}

	@Override
	public List<DataOperationCommand> get(String[] attributeNames, Criteria[] criteria) {
		// AKKA fix: filter attribute names and criteria according to the security policy
		//        /**
		//         * Generates one or several SE queries
		//         */
		//        List<DataOperationCommand> SEquery = null;
		//        try {
		//            SEquery = Query.search_with_SE(attributeNames, criteria);
		//        } catch (Exception e) {
		//            System.exit(1);
		//
		//        }
		// First, filter attribute names and criteria to encrypt

		List<String> allFqAttributeNames = Arrays.stream(attributeNames).map(an -> attributePatterns.entrySet().stream()
				.filter(e -> e.getValue().matcher(an).matches()).findFirst().map(Map.Entry::getKey).orElse(null))
				.collect(Collectors.toList());

		List<Boolean> encryptedAttributeFlags = allFqAttributeNames.stream().map(fqan -> attributeTypes.get(fqan))
				.map(type -> typesProtection.get(type)).map(typeProtection -> Stream.of("encryption", "searchable")
						.anyMatch(protection -> protection.equals(typeProtection)))
						.collect(Collectors.toList());
		List<String> attributeNamesToProcess = IntStream.range(0, attributeNames.length)
				.filter(i -> encryptedAttributeFlags.get(i)).mapToObj(i -> attributeNames[i])
				.collect(Collectors.toList());

		List<Integer> encryptedAttributeIndexes = Arrays.stream(attributeNames)
				.map(an -> attributeNamesToProcess.indexOf(an)).collect(Collectors.toList());
		// only the last part of the attribute name is encrypted
		String[] attributeNamesToEncrypt = attributeNamesToProcess.stream().map(an -> an.split("/"))
				.map(parts -> parts[parts.length - 1]).toArray(String[]::new);
		// index of the attribute name (use to build salt)
		int[] indexes = IntStream.range(0, allFqAttributeNames.size()).filter(i -> encryptedAttributeFlags.get(i))
				.mapToObj(i -> allFqAttributeNames.get(i)).map(fqan -> attributeIndexes.get(fqan))
				.mapToInt(Integer::intValue).toArray();
		List<String> allFqCriteriaAttributeNames = Arrays.stream(criteria).map(Criteria::getAttributeName)
				.map(an -> attributePatterns.entrySet().stream().filter(e -> e.getValue().matcher(an).matches())
						.findFirst().map(Map.Entry::getKey).orElse(null))
						.collect(Collectors.toList());

		List<Boolean> encryptedCriteriaFlags = IntStream.range(0, criteria.length)
				.mapToObj(
						i -> (criteria[i].getOperator().equals("=") || criteria[i].getOperator().equals(">")
								|| criteria[i].getOperator().equals(">=") || criteria[i].getOperator().equals("<")
								|| criteria[i].getOperator().equals("<="))
								&& Stream.of("encryption", "searchable")
								.anyMatch(protection -> protection.equals(typesProtection
										.get(attributeTypes.get(allFqCriteriaAttributeNames.get(i))))))
										.collect(Collectors.toList());
		List<Criteria> criteriaToProcess = IntStream.range(0, criteria.length)
				.filter(i -> encryptedCriteriaFlags.get(i)).mapToObj(i -> criteria[i]).collect(Collectors.toList());
		List<Integer> encryptedCriteriaIndexes = Arrays.stream(criteria).map(c -> criteriaToProcess.indexOf(c))
				.collect(Collectors.toList());
		// only the last part of the criteria attribute name is encrypted
		Criteria[] criteriaToEncrypt = criteriaToProcess.stream().map(c -> {
			String[] parts = c.getAttributeName().split("/");
			return new Criteria(parts[parts.length - 1], c.getOperator(), c.getValue());
		}).toArray(Criteria[]::new);

		// Second, perform encryption if necessary (only on attributes and on
		// criteria that must be encrypted)
		List<DataOperationCommand> SEquery = null;
		if (attributeNamesToEncrypt.length > 0) {
			/**
			 * Generates one or several SE queries
			 */
			try {
				SEquery = Query.search_with_SE(attributeNamesToEncrypt, criteriaToEncrypt, indexes);
			} catch (Exception e) {
				System.exit(1);
			}
		} else {
			SEquery = Collections.singletonList(new SearchableEncryptionCommand());
		}

		// Third, add attributes and criteria that must not be encrypted in the
		// response
		SearchableEncryptionCommand searchQuery = SearchableEncryptionCommand.class.cast(SEquery.get(0));
		String[] clearAttributeNames = new String[encryptedAttributeFlags.size()
		                                          + (attributeNamesToEncrypt.length > 0 ? 1 : 0)];
		String[] protectedAttributeNames = new String[encryptedAttributeFlags.size()
		                                              + (attributeNamesToEncrypt.length > 0 ? 1 : 0)];
		Map<String, String> mapping = new HashMap<>();
		String[] parts = null;
		for (int i = 0; i < attributeNames.length; i++) {
			clearAttributeNames[i] = attributeNames[i];
			if (encryptedAttributeFlags.get(i)) {
				parts = clearAttributeNames[i].split("/");
				parts[parts.length - 1] = searchQuery.getProtectedAttributeNames()[encryptedAttributeIndexes.get(i)];
				protectedAttributeNames[i] = mergeAttributeName(parts);
			} else {
				protectedAttributeNames[i] = clearAttributeNames[i];
			}
			mapping.put(clearAttributeNames[i], protectedAttributeNames[i]);
		}
		if (attributeNamesToEncrypt.length > 0) {
			// add additional attributes
			parts[parts.length - 1] = "?attribute1?";
			clearAttributeNames[clearAttributeNames.length - 1] = mergeAttributeName(parts);
			parts[parts.length - 1] = searchQuery
					.getProtectedAttributeNames()[searchQuery.getProtectedAttributeNames().length - 1];
			protectedAttributeNames[protectedAttributeNames.length - 1] = mergeAttributeName(parts);
			mapping.put(clearAttributeNames[clearAttributeNames.length - 1],
					protectedAttributeNames[protectedAttributeNames.length - 1]);
		}
		Criteria[] protectedCriteria = new Criteria[encryptedCriteriaFlags.size()];
		for (int i = 0; i < protectedCriteria.length; i++) {
			if (encryptedCriteriaFlags.get(i)) {
				parts = criteria[i].getAttributeName().split("/");
				Criteria newCriteria = searchQuery.getCriteria()[encryptedCriteriaIndexes.get(i)];
				parts[parts.length - 1] = newCriteria.getAttributeName();
				newCriteria.setAttributeName(mergeAttributeName(parts));
				protectedCriteria[i] = newCriteria;
			} else {
				protectedCriteria[i] = criteria[i];
			}
		}
		/*ArrayList<Criteria> protectedCriteria = new ArrayList<Criteria>();
        int kk = 0;
        System.out.println("encryptedCriteriaFlags.size()=" + encryptedCriteriaFlags.size());

        for (int i = 0; i < encryptedCriteriaFlags.size(); i++) {
            System.out.println("i=" + i + "and k=" + kk);
            if (encryptedCriteriaFlags.get(i)) {
                parts = criteria[i].getAttributeName().split("/");
                System.out.println("obfuscated criteria number =" + encryptedCriteriaIndexes.get(i).intValue() + kk);
                Criteria newCriteria = searchQuery.getCriteria()[encryptedCriteriaIndexes.get(i).intValue() + kk];
                System.out.println(newCriteria.getAttributeName() + newCriteria.getOperator() + newCriteria.getValue());
                if (newCriteria.getOperator() == "(") {
                    System.out.println("RANGE QUERY CRITERIA");
                    protectedCriteria.add(kk, newCriteria);
                    kk++;
                    System.out.println("i=" + i + "and k=" + kk);
                    newCriteria = searchQuery.getCriteria()[encryptedCriteriaIndexes.get(i).intValue() + kk];
                    System.out.println(
                            newCriteria.getAttributeName() + newCriteria.getOperator() + newCriteria.getValue());
                    while (newCriteria.getOperator() != ")") {
                        if (newCriteria.getOperator().toUpperCase() != "OR"
                                && newCriteria.getOperator().toUpperCase() != "AND") {
                            parts[parts.length - 1] = newCriteria.getAttributeName();
                            newCriteria.setAttributeName(mergeAttributeName(parts));
                        }
                        protectedCriteria.add(kk, newCriteria);
                        kk++;
                        System.out.println("i=" + i + "and k=" + kk);
                        newCriteria = searchQuery.getCriteria()[encryptedCriteriaIndexes.get(i).intValue() + kk];
                        System.out.println(
                                newCriteria.getAttributeName() + newCriteria.getOperator() + newCriteria.getValue());

                    }
                    i++;
                } else {
                    parts[parts.length - 1] = newCriteria.getAttributeName();
                    newCriteria.setAttributeName(mergeAttributeName(parts));
                    protectedCriteria.add(kk, newCriteria);
                    kk++;
                    System.out.println("i=" + i + "and k=" + kk);

                }
            } else {
                protectedCriteria.add(kk, criteria[i]);
            }
        }*/
		searchQuery.setAttributeNames(clearAttributeNames);
		searchQuery.setProtectedAttributeNames(protectedAttributeNames);
		searchQuery.setMapping(mapping);
		searchQuery.setCriteria(protectedCriteria);
		//searchQuery.setCriteria(protectedCriteria.toArray(new Criteria[protectedCriteria.size()]));
		return SEquery;
	}

	@Override
	public List<DataOperationResult> get(List<DataOperationCommand> promise, List<String[][]> contents) {
		// AKKA fix: filter contents according to the security policy
		//        /**
		//         * Decrypts the retrieved content
		//         */
		//        List<DataOperationResult> SEresult = null;
		//        try {
		//            try {
		//                SEresult = Retrieve.decrypt_result(promise, contents);
		//            } catch (SQLException e) {
		//                e.printStackTrace();
		//            }
		//        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableEntryException
		//                | IOException e) {
		//            e.printStackTrace();
		//        }
		String[] encryptedAttributeNames = promise.get(0).getProtectedAttributeNames();
		Map<String, String> mapping = promise.get(0).getMapping();
		String[][] encryptedContents = contents.get(0);
		// First, filter attribute names and content to decrypt
		List<Boolean> encryptedAttributeFlags = Arrays.stream(encryptedAttributeNames)
				.map(ean -> mapping.containsValue(ean) && !mapping.containsKey(ean)).collect(Collectors.toList());
		List<String> encryptedAttributeNamesToProcess = IntStream.range(0, encryptedAttributeFlags.size())
				.filter(i -> encryptedAttributeFlags.get(i)).mapToObj(i -> encryptedAttributeNames[i])
				.collect(Collectors.toList());
		List<Integer> encryptedAttributeIndexes = Arrays.stream(encryptedAttributeNames)
				.map(an -> encryptedAttributeNamesToProcess.indexOf(an)).collect(Collectors.toList());
		// only the last part of the attribute name is decrypted
		String[] attributeNamesToDecrypt = encryptedAttributeNamesToProcess.stream().map(an -> an.split("/"))
				.map(parts -> parts[parts.length - 1]).toArray(String[]::new);
		// filter content to decrypt and replace null values
		String[][] contentToDecrypt = Arrays.stream(encryptedContents).map(row -> IntStream.range(0, row.length)
				.filter(c -> encryptedAttributeIndexes.get(c) != -1).mapToObj(c -> row[c]).toArray(String[]::new))
				.toArray(String[][]::new);
		// index of the attribute name (use to build salt)
		int[] indexes = encryptedAttributeNamesToProcess.stream()
				.map(ean -> mapping.entrySet().stream().filter(e -> e.getValue().equals(ean)).map(Map.Entry::getKey)
						.findFirst().filter(an -> !an.endsWith("?attribute1?"))
						.map(an -> attributePatterns.entrySet().stream().filter(e -> e.getValue().matcher(an).matches())
								.findFirst().map(Map.Entry::getKey).orElse(null))
								.orElse(null))
								.filter(fqan -> fqan != null).map(fqan -> attributeIndexes.get(fqan)).mapToInt(Integer::intValue)
								.toArray();

		// Second, perform decryption if necessary (only on attributes and
		// contents that must be decrypted)
		List<DataOperationResult> SEresult = null;
		if (attributeNamesToDecrypt.length > 0) {
			/**
			 * Decrypts the retrieved content
			 */
			try {
				try {
					SEresult = Retrieve.decrypt_result(attributeNamesToDecrypt, contentToDecrypt, indexes);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableEntryException
					| IOException e) {
				e.printStackTrace();
			}
		} else {
			SEresult = Collections.singletonList(new SearchableEncryptionResponse());
		}

		// Third, add attributes and contents that must not be decrypted in the
		// response
		SearchableEncryptionResponse searchResponse = SearchableEncryptionResponse.class.cast(SEresult.get(0));
		String[] clearAttributeNames = new String[encryptedAttributeFlags.size() - 1];
		String[][] clearContents = new String[encryptedContents.length][encryptedAttributeFlags.size() - 1];
		for (int i = 0; i < clearAttributeNames.length; i++) {
			if (encryptedAttributeFlags.get(i)) {
				String[] parts = encryptedAttributeNames[i].split("/");
				parts[parts.length - 1] = searchResponse.getAttributeNames()[encryptedAttributeIndexes.get(i)];
				clearAttributeNames[i] = mergeAttributeName(parts);
			} else {
				clearAttributeNames[i] = encryptedAttributeNames[i];
			}
			for (int r = 0; r < clearContents.length; r++) {
				if (encryptedAttributeFlags.get(i)) {
					clearContents[r][i] = searchResponse.getContents()[r][encryptedAttributeIndexes.get(i)];
					if ("clarus_null".equals(clearContents[r][i])) {
						clearContents[r][i] = null;
					}
				} else {
					clearContents[r][i] = encryptedContents[r][i];
				}
			}
		}
		searchResponse.setAttributeNames(clearAttributeNames);
		searchResponse.setContents(clearContents);
		return SEresult;
	}

	@Override
	public List<Map<String, String>> head(String[] attributeNames) {
		// AKKA fix: resolve the attribute names according to the security policy
		String[] resolvedAttributes = AttributeNamesUtilities.resolveOperationAttributeNames(attributeNames,
				new ArrayList<>(this.attributeTypes.keySet()));
		List<Map<String, String>> output = new ArrayList<>();
		Map<String, String> se_mapping = new HashMap<>();
		logger.info("Loading search keys");
		String ksName = "clarus_keystore";
		char[] ksPassword = KeyManagementUtils.askPassword(ksName);
		KeyStore myKS;
		try {
			myKS = KeyManagementUtils.loadKeyStore(ksName, ksPassword);
			// AKKA fix: filter the attribute names according to the security policy
			//            SecretKey encryption_Key = KeyManagementUtils.loadSecretKey(myKS, "encKey", ksPassword);
			//
			//            for (int i = 0; i < attributeNames.length; i++) {
			//                SecretKey newSK = KeyManagementUtils.hashAESKey(encryption_Key, Integer.toString(i + 1));
			//                se_mapping.put(attributeNames[0], Encryptor.encrypt(attributeNames[i], newSK));
			boolean additionalAttribute = false;
			String[] parts = null;
			for (int i = 0; i < resolvedAttributes.length; i++) {
				String resolvedAttributeName = resolvedAttributes[i];
				String fqAttributeName = attributePatterns.entrySet().stream()
						.filter(e -> e.getValue().matcher(resolvedAttributeName).matches()).findFirst()
						.map(Map.Entry::getKey).orElse(null);
				// don't map attributes that are not supposed to be covered
				if (fqAttributeName != null) {
					String attributeType = attributeTypes.get(fqAttributeName);
					String typeProtection = typesProtection.get(attributeType);
					if (typeProtection.equals("encryption") || typeProtection.equals("searchable")) {
						additionalAttribute = true;
						// encrypt attribute name
						String keyAlias = typesDataIDs.get(attributeType);
						SecretKey encryption_Key = KeyManagementUtils.loadSecretKey(myKS, keyAlias, ksPassword);
						int index = attributeIndexes.get(fqAttributeName);
						SecretKey newSK = KeyManagementUtils.hashAESKey(encryption_Key, Integer.toString(index + 1));
						parts = resolvedAttributeName.split("/");
						parts[parts.length - 1] = Encryptor.encrypt(parts[parts.length - 1], newSK, true);
						String encryptedAttributeName = mergeAttributeName(parts);
						se_mapping.put(resolvedAttributeName, encryptedAttributeName);
					} else {
						// attribute name is not encrypted
						se_mapping.put(resolvedAttributeName, resolvedAttributeName);
					}
				}
			}
			if (additionalAttribute) {
				// add additional attributes
				parts[parts.length - 1] = "?attribute1?";
				String fqAttributeName = mergeAttributeName(parts);
				parts[parts.length - 1] = "rowID";
				String encryptedAttributeName = mergeAttributeName(parts);
				se_mapping.put(fqAttributeName, encryptedAttributeName);
			}
		} catch (Exception e) {
			logger.info("[FAILURE]");
			e.printStackTrace();
		}
		output.add(se_mapping);
		return output;
	}

	// AKKA fix: utility method to build an attribute name from the different parts: [[dataset]/data]/attribute
	private String mergeAttributeName(String[] parts) {
		if (parts.length == 3) {
			return String.format("%s/%s/%s", parts[0], parts[1], parts[2]);
		} else if (parts.length == 2) {
			return String.format("%s/%s", parts[0], parts[1]);
		} else if (parts.length == 1) {
			return parts[0];
		}
		return null;
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
