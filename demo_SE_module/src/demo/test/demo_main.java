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

package demo.test;

import static eu.clarussecure.dataoperations.SEmodule.Constants.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import eu.clarussecure.dataoperations.Criteria;
import eu.clarussecure.dataoperations.DataOperationCommand;
import eu.clarussecure.dataoperations.DataOperationResult;
import eu.clarussecure.dataoperations.SEmodule.Constants;
import eu.clarussecure.dataoperations.SEmodule.ProgressBar;
import eu.clarussecure.dataoperations.SEmodule.SearchableEncryptionCommand;
import eu.clarussecure.dataoperations.SEmodule.SearchableEncryptionModule;
import eu.clarussecure.dataoperations.SEmodule.SearchableEncryptionResponse;


public class demo_main {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {

		/**
		 * Parses csv file (demo) Outputs encrypted list of attributes and
		 * encrypted data content
		 */

		if (args.length < 1) {
			System.out.print("You must provide csv file path...");
			return;
		}

		String name = (args[0].substring(args[0].lastIndexOf("/")+1)).split(".csv")[0];
		Constants.tableName = name;

		Connection connection = DBmanager.establishConnection();

		System.out.println("*************************\n"
				+ "******** HELLO ! ********\n" 
				+ "*************************");

		/**
		 * Simulating the Protocol Module
		 */
		System.out.println("Parsing the dataset....");

		String[] attributeNames;
		String[][] content;

		attributeNames = parse_attributes(args[0]);
		System.out.println("Attribute names:\n" + Arrays.deepToString(attributeNames).replaceAll("],","]," + System.getProperty("line.separator")) + "\n");

		content = parse_csv(args[0]);
		System.out.println(content.length + " rows in the database");

		// Declare instance of SE module
		SearchableEncryptionModule SE_module = new SearchableEncryptionModule();

		// Call "post" method of the SE module
		List<DataOperationCommand> post_query;
		System.out.println("**********************\n"
				+ "******** POST ********\n" 
				+ "**********************");
		/**
		 * Call to the SE module
		 */
		post_query = SE_module.post(attributeNames, content);
		System.out.println("POST OK\n");

		/**
		 * Simulating Protocol Module for storing the encrypted database to the server
		 */
		SearchableEncryptionCommand myPostQuery = (SearchableEncryptionCommand) post_query.get(0);
		String[][] encrypted_content = myPostQuery.getProtectedContents();
		System.out.println("Upload encrypted database to the server\n");
		DBmanager.storeInPostgres(Constants.tableName+encryptedDB, post_query.get(0).getProtectedAttributeNames(), encrypted_content, connection);

		System.out.println("Upload search index to the server\n");
		System.out.println(encrypted_content.length + " rows in the database");
		DBmanager.storeIndex(Constants.tableName+Constants.indexName, post_query.get(0).getExtraBinaryContent()[0], connection);





		// Call "get outbound" (client search with SE)
		System.out.println("**********************\n"
				+ "***** GET (out) ******\n" 
				+ "**********************");
		List<DataOperationCommand> search_query;
		/**
		 * Simulating Protocol module
		 */
		// Prompt user for keyword (SQL "WHERE" condition - simple)
		//Scanner keyword_input = new Scanner(System.in);
		//System.out.println("Please complete the statement:\n");
		//System.out.println("SELECT * FROM "+Constants.tableName +" WHERE ");
		//String read_keyword = keyword_input.nextLine();
		String read_keyword = "pat_last2='GARCIA'";
		System.out.println("Executing SQL query: SELECT * FROM " + Constants.tableName + " WHERE " +read_keyword);
		// Create Criteria
		String condition[] = read_keyword.split("=");
		Criteria myCriteria = new Criteria(condition[0], "=", condition[1]);
		Criteria[] criteria = { myCriteria };

		/**
		 * Call to the SE module
		 */
		search_query = SE_module.get(attributeNames, criteria);
		System.out.println("GET (outbound) OK\n");


		/**
		 * Simulating Protocol Module for creating the obfuscated SQL query
		 */
		ArrayList<String> conditions = new ArrayList<String>();
		for(int i=0; i<search_query.size();i++){
			Criteria[] search_criteria = search_query.get(i).getCriteria();
			String where_condition;
			for(int j=0;j<search_criteria.length; j++){
				where_condition = search_criteria[j].getAttributeName() + " " +
						search_criteria[j].getOperator() + " " + search_criteria[j].getValue();
				conditions.add(where_condition);
			}
		}
		String sql_search_query = "select * from "+name+encryptedDB+" where ";
		for(int i=0; i<conditions.size(); i++){
			sql_search_query += conditions.get(i);
		}


		/**
		 * TEST of cloud search with PL/JAVA in Postgres
		 */
		System.out.println(sql_search_query);
		String[][] search_results = DBmanager.search_and_retrieve(sql_search_query, connection);


		List<String[][]> encrypted_results = new ArrayList<String[][]>();
		encrypted_results.add(search_results);

		// Call "get inbound" (decrypt with SE)
		System.out.println("**********************\n"
				+ "***** GET (in) *******\n" 
				+ "**********************");

		/**
		 * Simulating the Protocol Module for creating Data Operation Result
		 */
		List<DataOperationResult> se_response;

		// Call "get"
		se_response = SE_module.get(search_query, encrypted_results);
		System.out.println("GET INBOUND OK\n");


		SearchableEncryptionResponse output_get;
		String[][] decrypted_content = null;
		for(int k = 0; k < se_response.size(); k++){
			output_get = (SearchableEncryptionResponse) se_response.get(k);
			decrypted_content = output_get.getContents();
		}
		System.out.println("Decrypted content:\n" + Arrays.deepToString(decrypted_content).replaceAll("],","]," + System.getProperty("line.separator")) + "\n");


	}





	/*	Parsing methods
	 */

	private static String[] parse_attributes(String csv_path)
			throws IOException {
		File csv_file = new File(csv_path);
		BufferedReader br = new BufferedReader(new FileReader(csv_file));
		String attributesLine = br.readLine();
		String[] attributeNames = attributesLine.split(",");

		br.close();
		return attributeNames;
	}

	private static String[][] parse_csv(String csv_path) throws IOException {
		ProgressBar bar = new ProgressBar();
		File csv_file = new File(csv_path);
		BufferedReader in = new BufferedReader(new FileReader(csv_file));
		LinkedList<String> lines = new LinkedList<String>();
		String line = "";
		while ((line = in.readLine()) != null) {
			lines.add(line);
		}
		in.close();
		line = lines.pollFirst();
		int recordSize = line.split(",").length;
		String[][] content = new String[lines.size()][recordSize];
		bar.update(0, lines.size());
		for (int i = 0; i < lines.size(); i++) {
			content[i] = lines.get(i).split(",");
			bar.update(i, lines.size());
		}
		return content;
	}

}
