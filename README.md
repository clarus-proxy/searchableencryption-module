# Searchable Encryption (SE) module


Instructions to install, test and update the (standalone) module for Searchable Encryption.



**Author**: Monir Azraoui

**Contact**: name.surname(at)eurecom(dot)fr

**Last modification**: November 8, 2017


---
## Introduction

The SE module allows
- single keyword search queries
- range queries
- boolean queries.

The implementation has two core projects:
- the SE module that should be run by the CLARUS proxy.
It has three main functions:
* POST: It takes the clear content of a database, extracts the keywords, 
 builds a secure search index and encrypts the content of the database.
* GET(out): It generates a search query, i.e. generates a trapdoor for 
 the searched keyword(s). 
* GET(in): It decrypts the results of a search 
 query.
- the search function run by the Cloud.

---
## Project architecture

- `JARs`:

The folder contains the **final** JARs of the SE module and the JAR for the cloud search function.


-  `SE_module`:

It contains the source code of a Maven project implementing the SE module.


-  `cloud_search_with_SE`:

It contains the source code of a Maven project implementing the Search function at the Cloud (an run by the PostgreSQL server).


- `demo_SE_module`:

It contains the source code of a project for demo. It simulates the operations of the CLARUS proxy calling the SE module.


---
## Installation

To install all the prerequisites and the different elements of the SE module (the demo program, and the search function run by the cloud server), please run the following instruction:

---
### 1. Prerequisites
- Update apt-get repo list

> sudo apt-get update

- Install C compiler

> sudo apt-get install g++

- Install JDK 1.8 (Java)

> sudo apt-get install openjdk-8-jdk

- Install Maven
Maven is a tool to automate project and dependencies management.

> sudo apt-get install maven

- Install Postgresql
PostgreSQL is an opensource relational database management system.

> sudo apt-get install postgresql-common

> sudo apt-get install postgresql

> sudo apt-get install postgresql-server-dev-X.Y (current version X.Y = 9.5)

> sudo apt-get install libecpg-dev

> sudo apt-get install libkrb5-dev

- Install and Build PL/Java 
PL/Java is a free add-on module that brings Java function to the PostgreSQL server.

> sudo git clone https://github.com/tada/pljava

> sudo chmod 777 pljava/

> sudo cd pljava/

> sudo mvn clean install 

At the end of the execution you should have:

> [INFO] PostgreSQL PL/Java ......................... SUCCESS

> [INFO] PL/Java API ....................................... SUCCESS

> [INFO] PL/Java backend Java code ............ SUCCESS

> [INFO] PL/Java backend native code ........... SUCCESS

> [INFO] PL/Java Deploy ............................... SUCCESS

> [INFO] PL/Java Ant tasks ................................. SUCCESS

> [INFO] PL/Java examples .................................. SUCCESS

> [INFO] PL/Java packaging ................................. SUCCESS


If one of the above line does not end with "SUCCESS", something went wrong. 
Please investigate.

If during the execution of [mvn clean install] some lines of the output
begin with "ERROR" but at the end all of the above lines are SUCCESS, 
don't worry. It's fine. 

- Install PL/Java (cont.)

> sudo java -jar pljava-packaging/target/pljva-pg9.5-amd64-Linux-gpp.jar

- Configure PL/Java 

mvn install:install-file -DgroupId=org.postgresql -DartifactId=pljava-api -Dversion=1.6.0-SNAPSHOT -Dpackaging=jar -Dfile=pljava-api/target/pljava-api-1.6.0-SNAPSHOT.jar -DgeneratePom=true

mvn install:install-file -DgroupId=org.postgresql -DartifactId=pljava -Dversion=1.6.0-SNAPSHOT -Dpackaging=jar -Dfile=pljava/target/pljava-1.6.0-SNAPSHOT.jar -DgeneratePom=true

   - Launch postgresql:

> sudo -u postgres createuser "user";

> sudo -u postgres psql


The command line will start with "**postgres=#**"

   - Set the following configuration variable that locate your jvm library

> SET pljava.libjvm_location TO '[jvm_path]';

Replace **[jvm_path]** by the path of your jvm (can be located using the command [locate libjvm.so])

Don't forget the ";" at the end of the command.

For ex: [jvm_path] = /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/amd64/server/libjvm.so

   - Make the previous setting persistent by running the command in postgresql:

> ALTER DATABASE postgres SET pljava.libjvm_location FROM CURRENT;

Don't forget the ";" at the end of the command.

   - Then run

> CREATE EXTENSION IF NOT EXISTS pljava;

> GRANT USAGE ON LANGUAGE java TO PUBLIC;

Don't forget the ";" at the end of the command.


---
### 2. Install the Search function in PostgreSQL

`cloud_search_with_SE-1.jar` is the JAR that implements the search function that is executed by the PostgreSQL server. 

- Launch the PostgreSQL application (if you exited it before)

> sudo -u postgres psql

- Install the JAR

> select sqlj.install_jar('file://[path_to_the.jar]', '[alias]', true);

`[alias]` is just a name to refer to the jar. For ex: "search_with_SE".

`[path_to_the.jar]` is the path the `cloud_search_with_SE-1.jar`. For example `'file:///home/user/Desktop/SE_module/JARs/cloud_search_with_SE-1.jar'`. 

Don't forget the ";" at the end of the command.

So for example, you will run:

> select sqlj.install_jar('file:///home/user/Desktop/SE_module/JARs/cloud_search_with_SE-1.jar', 'search_with_SE', true);

- Set the classpath

> select sqlj.set_classpath('public', '[alias]'); 

Don't forget the ";" at the end of the command.

So for example, you will run:

> select sqlj.set_classpath('public', 'search_with_SE');

- You can check in PostgreSQL that the function has been stored using the following command

> \df

- Some further commands to execute in PostgreSQL:

> alter user "user" with encrypted password '123';

> grant all privileges on database postgres to "user";

> grant all privileges on schema sqlj to "user";

- Quit PostgreSQL

> \q

---
##  Run the demo
The demo will:
* parse a csv file (given as input to the demo program)
* call POST method from the SE module to create a search index and encrypt the data
* store the data and the index in 2 different tables in Postgresql
* call GET(out) method to search for a keyword (gemerate a trapdoor for the searched keyword)
* run the corresponding protected SQL query
* get the result from the Postgres server who processed the query using the function Search (in the JAR `cloud_search_with_SE`)
* call GET(in) method to decrypt the search results. 


To run the demo program, execute the following instructions:

> java -cp JARs/demo_SE_module.jar demo.test.demo_main [path_to_CSV_file] 

Example: 

> java -cp JARs/demo_SE_module.jar demo.test.demo_main lab.csv

#### Troubleshootings
- You can find a step by step demo storyline with screenshots in the file "step_by_step_demo.pdf" in the gitlab. 

- If you get no error, you can check in PostgreSQL, the 2 tables that have been created during the upload of the encrypted database using the following command:

> sudo -u postgres psql

> \d

You should have two tables ending with `_encrypted` and `_ index`.

- If during the execution of the above `java -cp` instruction you get an error with `Permission denied`, you probably need to be `sudo`

