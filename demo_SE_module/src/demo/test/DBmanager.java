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

import static eu.clarussecure.dataoperations.SEmodule.Constants.port;
import static eu.clarussecure.dataoperations.SEmodule.Constants.remoteServer;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

public class DBmanager {

	public static Connection establishConnection() throws SQLException{
		try {

			Class.forName("org.postgresql.Driver");

		} catch (ClassNotFoundException e) {

			System.out.println("Where is your PostgreSQL JDBC Driver? "
					+ "Include in your library path!");
			e.printStackTrace();
			return null;

		}
		Connection connection = null;
		try {

			connection = DriverManager.getConnection(
					"jdbc:postgresql://"+remoteServer+":"+port+"/postgres", "user",
					"123");
		} catch (SQLException e) {
			System.out.println("Connection Failed! Check the address of the remote server and the port for postgresql");
			return null;
		}
		return connection;
	}

	public static void storeInPostgres(String tableName, String[] attributesNames, String[][] contents, Connection connection) throws SQLException{
		if (connection != null) {
			Statement statement = connection.createStatement();
			createTable(tableName, attributesNames, statement);
			fillTable(tableName, contents, statement);

			//statement.close();
			//connection.close();
		} else {
			System.out.println("Failed to make connection!");

		}
	}

	private static void createTable(String tableName, String[] attributesNames, Statement statement){

		try {
			statement.executeUpdate("DROP TABLE IF EXISTS " + tableName+";");
		} catch (SQLException e1) {
			System.out.println("Failed to delete existing table!");
		}
		int nbAtt = attributesNames.length;
		String query = "CREATE TABLE " + tableName + "( ";
		for(int i=0; i<nbAtt-1; ++i){
			query += "U&\""+attributesNames[i]+ "\" TEXT, ";
		}
		query += attributesNames[nbAtt-1] + " TEXT);";
		try {
			statement.executeUpdate(query);
		} catch (SQLException e) {
			System.out.println("Failed to create table!");
			e.printStackTrace();
		}
	}

	private static void fillTable(String tableName, String[][] contents, Statement statement){

		String content = "";

		int contentLength = contents.length;
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO " + tableName + " VALUES\n");
		for(int i=0; i<contentLength-1;++i){
			content = Arrays.toString(contents[i]);
			content = content.substring(1, content.length()-1).replaceAll(", " , ",'").replaceAll("," , "', ").replace("\\" , "\\\\");
			sb.append("('"+content+"'),");			
		}
		content = Arrays.toString(contents[contentLength-1]);
		content = content.substring(1, content.length()-1).replaceAll(", " , ",'").replaceAll("," , "', ").replace("\\" , "\\\\");		
		sb.append("('"+content+"');");		
		String sql = sb.toString();

		try {
			statement.executeUpdate(sql);

		} catch (SQLException e) {
			System.out.println("Failed to insert rows ");
		}
	}
	
	// Create a table in the DB that stores the index as binary data (bytea)
	public static void storeIndex(String tableName, InputStream is_index, Connection connection) throws SQLException{
		Statement statement= null;
		if (connection != null){
			statement = connection.createStatement();
		}
		else{
			System.out.println("Failed to establish connection§"); 
			System.exit(1);
		}
		statement.executeUpdate("DROP TABLE IF EXISTS "+tableName);
		statement.executeUpdate("CREATE TABLE "+tableName+"(name char(30), index bytea)");
		String sql = "INSERT INTO "+tableName+" (name, index) VALUES(?,?)";
		PreparedStatement pstmt = connection.prepareStatement(sql);
		pstmt.setString(1, tableName);
		pstmt.setBinaryStream(2, is_index);
		pstmt.executeUpdate();
		pstmt.close();

		//statement.close();
		//connection.close();

	}

	
	//search_and_retrieve_encrypted_results
	public static String[][] search_and_retrieve(String sql_query, Connection connection) throws SQLException, FileNotFoundException{
		System.out.println("Search using PostgreSQL");
		Statement statement= null;
		if (connection != null) {
			statement = connection.createStatement();
		}
		else{
			System.out.println("Failed to establish connection§"); 
			System.exit(1);
		}

		ResultSet resultSet = statement.executeQuery(sql_query);
		ResultSetMetaData rsmd = resultSet.getMetaData();
		int columnsCount = rsmd.getColumnCount();

		ArrayList<String[]> contents_list = new ArrayList<String[]>();
		while (resultSet.next()) {
			String[] row = new String[columnsCount];
			for(int c=0; c<columnsCount;++c) row[c] = resultSet.getString(c+1);
			contents_list.add(row);
		}

		int rowsCount = contents_list.size();
		String[][] contents = new String[rowsCount][columnsCount];
		contents = contents_list.toArray(contents);
		statement.close();
		connection.close();
		return contents;
	}	
}
