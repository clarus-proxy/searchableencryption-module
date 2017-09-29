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
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import eu.clarussecure.dataoperations.Criteria;
import eu.clarussecure.dataoperations.DataOperationCommand;
import eu.clarussecure.dataoperations.DataOperationResult;
import eu.clarussecure.dataoperations.SEmodule.Constants;
import eu.clarussecure.dataoperations.SEmodule.SearchableEncryptionCommand;
import eu.clarussecure.dataoperations.SEmodule.SearchableEncryptionModule;
import eu.clarussecure.dataoperations.SEmodule.SearchableEncryptionResponse;


public class demo_main {
	@SuppressWarnings("unchecked")
	private static Logger logger = Logger.getLogger(demo_main.class);

	public static void main(String[] args) throws Exception {

		/**
		 * Parses csv file (demo) Outputs encrypted list of attributes and
		 * encrypted data content
		 */

		if (args.length < 1) {
			logger.info("You must provide csv file path...");
			return;
		}

		String name = (args[0].substring(args[0].lastIndexOf("/")+1)).split(".csv")[0];
		Constants.tableName = name;

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(new File("/home/eurecom/Desktop/lab_simple.xml"));
		logger.info("Loading security policy");


		Connection connection = DBmanager.establishConnection();

		/**
		 * Simulating the Protocol Module
		 */
		logger.info("Parsing the dataset....");

		String[] attributeNames;
		String[][] content;

		attributeNames = parse_attributes(args[0]);
		logger.info("Attribute names:\n" + Arrays.deepToString(attributeNames).replaceAll("],","]," + System.getProperty("line.separator")) + "\n");

		content = parse_csv(args[0]);
		logger.info(content.length + " rows in the database");

		// Declare instance of SE module
		SearchableEncryptionModule SE_module = new SearchableEncryptionModule(document);

		// Call "post" method of the SE module
		List<DataOperationCommand> post_query;
		System.out.println("**********************\n"
				+ "******** POST ********\n" 
				+ "**********************");
		/**
		 * Call to the SE module
		 */
		post_query = SE_module.post(attributeNames, content);
		logger.info("POST OK\n");

		/**
		 * Simulating Protocol Module for storing the encrypted database to the server
		 */
		SearchableEncryptionCommand myPostQuery = (SearchableEncryptionCommand) post_query.get(0);
		String[][] encrypted_content = myPostQuery.getProtectedContents();
		logger.info("Upload encrypted database to to the PostgreSQL server\n");

		DBmanager.storeInPostgres(Constants.tableName+encryptedDB, post_query.get(0).getProtectedAttributeNames(), encrypted_content, connection);

		logger.info("Upload search index to the PostgreSQL server\n");
		logger.info(encrypted_content.length + " rows in the database");
		DBmanager.storeIndex(Constants.tableName+Constants.indexName, post_query.get(0).getExtraBinaryContent()[0], connection);




		while(true){

			// Call "get outbound" (client search with SE)
			System.out.println("*********************\n"
					+ "****** SEARCH *******\n" 
					+ "*********************");
			List<DataOperationCommand> search_query;
			/**
			 * Simulating Protocol module
			 */
			// Prompt user for keyword (SQL "WHERE" condition - simple)
			//Scanner keyword_input = new Scanner(System.in);
			//System.out.println("Please complete the statement:\n");
			//System.out.println("SELECT * FROM "+Constants.tableName +" WHERE ");
			//String where_statement = keyword_input.nextLine();
			String where_statement = "lab_simple/lab_simple/pat_name=SANDRA OR lab_simple/lab_simple/pat_name=RAUL";
			where_statement= "(" + where_statement + ");";
			System.out.println("Executing SQL query: SELECT * FROM " + Constants.tableName + " WHERE " + where_statement + "\n");

			// Create Criteria
			Criteria[] criteria = create_criteria(where_statement);
			int kk = 0;
			for (int i = 0; i < criteria.length; i++) {
				if (criteria[i].getAttributeName() != null) {
					kk++;
				}
			}
			Criteria[] kw_only_criteria = new Criteria[kk];
			int jj = 0;
			for (int i = 0; i < criteria.length; i++) {
				if (criteria[i].getAttributeName() != null) {
					kw_only_criteria[jj] = criteria[i];
					jj++;
				}
			}
			//System.out.println(kw_only_criteria[0].getAttributeName() + " " +kw_only_criteria[0].getOperator() + " " + kw_only_criteria[0].getValue() );

			/**
			 * Call to the SE module
			 */
			search_query = SE_module.get(attributeNames, kw_only_criteria);


			/**
			 * Simulating Protocol Module for creating the obfuscated SQL query
			 */
			ArrayList<String> conditions = new ArrayList<String>();
			for(int i=0; i<search_query.size();i++){
				Criteria[] search_criteria = search_query.get(i).getCriteria();
				String where_condition;
				int k = 0;
				for(int j=0;j<criteria.length; j++){
					if(criteria[j].getAttributeName()==null){
						where_condition = " " + criteria[j].getOperator() + " ";
						conditions.add(where_condition);
					}else{
						where_condition = search_criteria[k].getAttributeName().split("/")[2] + " " +
								search_criteria[k].getOperator() + " " + search_criteria[k].getValue();
						conditions.add(where_condition);
						k++;
					}
				}

			}


			System.out.println("Protected SQL query executed by the PostgresSQL server:");
			String sql_search_query = "select * from "+name+encryptedDB+" where ";
			System.out.println( sql_search_query+ "\n");
			for(int i=0; i<conditions.size(); i++){
				System.out.println(conditions.get(i) + "\n");
				sql_search_query += conditions.get(i);
			}

			System.out.println(sql_search_query);
			/**
			 * TEST of cloud search with PL/JAVA in Postgres
			 */
			String[][] search_results = DBmanager.search_and_retrieve(sql_search_query, connection);
			if(search_results.length>0){
				System.out.println("Retrieved encrypted results from the PostgreSQL database:");

				System.out.println();

				System.out.println(search_results.length + " rows retrieved from the database\n\n");


				List<String[][]> encrypted_results = new ArrayList<String[][]>();
				encrypted_results.add(search_results);

				// Call "get inbound" (decrypt with SE)
				System.out.println("**********************\n"
						+ "****** DECRYPT *******\n" 
						+ "**********************");

				/**
				 * Simulating the Protocol Module for creating Data Operation Result
				 */
				List<DataOperationResult> se_response;

				// Call "get"
				se_response = SE_module.get(search_query, encrypted_results);
				SearchableEncryptionResponse output_get;
				String[][] decrypted_content = null;
				for(int k = 0; k < se_response.size(); k++){
					output_get = (SearchableEncryptionResponse) se_response.get(k);
					decrypted_content = output_get.getContents();
				}
				System.out.println("Decrypted content:");

				System.out.println(Arrays.deepToString(decrypted_content).replaceAll("],","]," + System.getProperty("line.separator")) + "\n");
				System.out.println(search_results.length + " rows retrieved from the database\n\n");}
			else{
				System.out.println("KEYWORD NOT FOUND \n\n");
			}



			boolean again = true;
			while(again){
				again=false;
				System.out.println("Do you want to submit another search query? (Y/N)");
				Scanner sc = new Scanner(System.in);
				String answer = sc.nextLine();


				switch (answer.toUpperCase())
				{
				case "Y":
					continue;

				case "N":  
					System.out.println("*************************\n"
							+ "******** Bye ! ********\n" 
							+ "*************************");
					connection.close();
					System.exit(0);  

				default:
					System.out.println("ERROR");
					again=true;
					break;             
				}
			}

		}
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
		File csv_file = new File(csv_path);
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(csv_file), "ISO-8859-1"));
		LinkedList<String> lines = new LinkedList<String>();
		String line = "";
		while ((line = in.readLine()) != null) {
			lines.add(line);
		}
		in.close();
		line = lines.pollFirst();
		int recordSize = line.split(",").length;
		String[][] content = new String[lines.size()][recordSize];
		for (int i = 0; i < lines.size(); i++) {
			content[i] = lines.get(i).split(",");
		}
		return content;
	}

	/*	SQL interpretation methods
	 */

	private static String identifyOperator(String keyword){
		String[] operators = new String[]{">=","<=","=",">","<","%"}; //Be careful, the order matter !!
		String operator = null;
		int i=0;
		while(operator==null && i<operators.length){
			if (keyword.contains(operators[i])) 
				operator = operators[i];
			++i;
		}
		return operator;
	}

	private static Criteria[] create_criteria(String query){
		StringTokenizer tokens = new StringTokenizer(query,"(); ",true);
		ArrayList<Criteria> criteriaList = new ArrayList<Criteria>();
		String operator;
		String condition[];
		Criteria myCriteria;

		for (;tokens.hasMoreTokens();) {
			String token = tokens.nextToken();
			System.out.println(token);

			//Ignore space
			if (token.charAt(0) == ' ')
				;
			else if (token.equals(";"))
				;

			else if ( token.equals("(") || token.equals(")") ){
				//opening or closing brackets
				myCriteria = new Criteria(null, token, null);
				criteriaList.add(myCriteria);
			}

			// Logical Operator (or, and)
			else if(token.toUpperCase().equals("OR") || token.toUpperCase().equals("AND")){  
				myCriteria = new Criteria(null, token.toUpperCase(), null);
				criteriaList.add(myCriteria);
			}
			// search criterion of the form attribute=value
			else {
				operator = identifyOperator(token);
				if(operator==null){
					System.out.println("ERROR: keyword without arithmetic operator!\nKeyword: "+token);
					System.exit(1);
					//}
				}
				condition = token.split(operator);

				//in case the value contains spaces or quotes
				String regExp = "'.*(\')*.*[^']";
				String value = condition[1];
				while(value.matches(regExp)||value.equals("'")){
					token = tokens.nextToken();
					value = value.concat(token);
				}
				value = value.replaceAll("\\\\'", "'");
				myCriteria = new Criteria(condition[0], operator, value);
				criteriaList.add(myCriteria);
			}
		}
		return criteriaList.toArray(new Criteria[criteriaList.size()]);	  
	}




}
