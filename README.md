# Searchable Encryption (SE) module
[![Build Status](https://travis-ci.org/clarus-proxy/searchableencryption-module.svg?branch=master)](https://travis-ci.org/clarus-proxy/searchableencryption-module)


Instructions to install and test the module.



**Author**: Monir Azraoui

**Contact**: name.surname(at)eurecom(dot)fr




---
## Introduction

It allows
- single keyword search queries
- range queries
- boolean queries.

The implementation has two core projects:
- the SE module that should be run by the CLARUS proxy.
It has three main functions:
> POST: It takes the clear content of a database, extracts the keywords, 
> builds a secure search index and encrypts the content of the database.
> GET(out): It generates a search query, i.e. generates a trapdoor for 
> the searched keyword(s). GET(in): It decrypts the results of a search 
> query.
- the search function run by the Cloud.

---
## Packages architecture

-  `SE_module`:

It contains the source code of a Maven project implementing the SE module.


-  `cloud_search_with_SE`:

It contains the source code of a Maven project implementing the Search function at the Cloud (an run by the PostgreSQL server).


- `demo_SE_module`:

It contains the source code of a project for demo. It simulates the operations of the CLARUS proxy calling the SE module.
It requires the jars of the SE module to run, and an external jar called "postgresql-42.0.0.jar".

- `JARs`:

The folder contains the JAR of the SE module and the JAR for the cloud search function.


---
## Installation

To run and test the demo program, please follow the below instructions.


---
### 1. Prerequisites
 - C compiler

> sudo apt-get install g++

- JDK 1.8

> sudo apt-get install openjdk-8-jdk

- Maven

> sudo apt-get install maven

- Postgresql

> sudo apt-get install postgresql-common

> sudo apt-get install postgresql

> sudo apt-get install postgresql-server-dev-X.Y (current version X.Y = 9.5)

> sudo apt-get install libecpg-dev

> sudo apt-get install libkrb5-dev

- PL/Java (build)


-- Download the latest release of PL/Java (today - July 20th 2017: release 1.5.0) from:
https://github.com/tada/pljava/releases 
(see the "Download" section and choose the zip or tar.gz archive)

-- Extract the archive to [your_folder]

-- Open a terminal and run:

> cd [your_folder]/pljava-1_5_0

> mvn clean install

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

- PL/Java (install)

-- Run in a terminal:

> sudo java -jar pljava-packaging/target/pljva-pg9.5-amd64-Linux-gpp.jar

The 4 following parameters can change in the path according to your system:

1. 9.5 is the postgresql version

1. amd64 is the architecture of the system

1. Linux is the OS

1. gpp is the link

- PL/Java (Configure) 
-- Launch postgresql:

> sudo -u postgres psql

The command line will start with "**postgres=#**"

--  Set the following configuration variable that locate your jvm library

> SET pljava.libjvm_location TO '[jvm_path]';

Replace **[jvm_path]** by the path of your jvm (can be located using the command [locate libjvm.so])

Don't forget the ";" at the end of the command.

For ex: [jvm_path] = /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/amd64/server/libjvm.so

-- Make the previous setting persistent by running the command in postgresql:

> ALTER DATABASE postgres SET pljava.libjvm_location FROM CURRENT;

Don't forget the ";" at the end of the command.

-- Then run

> CREATE EXTENSION pljava;

> GRANT USAGE ON LANGUAGE java TO PUBLIC;

Don't forget the ";" at the end of the command.


---
### 2. Install the Search function in PostgreSQL

`cloud_search_with_SE-1.jar` is the JAR that implements the search function that is executed by the PostgreSQL server. 

-- Launch the PostgreSQL application

> sudo -u postgres psql

-- Install the JAR

> select sqlj.install_jar('file://[path_to_the.jar]', '[alias]', true);

`[alias]` is just a name to refer to the jar. For ex: "search".

`[path_to_the.jar]` is the path the `cloud_search_with_SE-1.jar`. For example `'file:///home/eurecom/cloud_search_with_SE-1.jar'`. 

Don't forget the ";" at the end of the command.


-- Set the classpath

> select sqlj.set_classpath('public', '[alias]'); 

Don't forget the ";" at the end of the command.


-- You can check in PostgreSQL that the function has been stored using the following command

> \df


---
### 2. Run the demo
The demo will:
* parse a csv file (given as input to the demo program)
* call POST method from the SE module to create a search index and encrypt the data
* store the data and the index in a table in Postgresql
* call GET(out) method to search for a keyword (here pat_last2='GARCIA' is hardcoded)
* run the corresponding protected SQL query
* get the result from the Postgres server who processed the query using the function Search (in the JAR)
* call GET(in) method to decrypt the search results. 


These instructions have been tested on a Ubuntu 16.04 machine that runs **Eclipse Luna**.

In Eclipse:

* File > Import... > General > Existing Projects into Workspace
* In **Select root directory**, click on **Browse** and find the folder `demo_SE_module`
* Click **Finish**
* In the **Project Explorer**, Right click on `demo_SE_module` > Properties > Java Build Path > Libraries > Add External Jars
* Find the `postgresql-42.0.0` jar in the SE_module folder and click *OK*
* Find the `SE_module` jar in the SE_module folder and click *OK* and again on **OK**. 
* In the **Project Explorer**, Right click on `demo_SE_module` > Run Configurations > Java Application 
* In the **Main** tab, search the **Main class* called `demo_main`
* In the **Arguments** tab, in the **Program arguments** field, type the path to a test CSV file of your choice (there is a test file called lab_simple2000.csv in the git folder)
* Click on **Run**


Output of the demo program:
* The list of clear rows, read from the csv file
* The number of rows that have been encrypted
* POST OK
* Message about the upload of the encrypted data and the index to the PostgreSQL server
* The SQL query select * from [table] where pat_last2='GARCIA'
* The process of loading keys from the keystore and the generation of the trapdoor
* GET(out) OK
* The protected SQL query which contains the clause "rowID IN..."
* The search
* GET(in) OK
* The decrypted search results. 


You can check in PostgreSQL that two tables have been created using the command

> \d

* one table ends with the keyword "_encrypted" 
* the other table ends with the keyword "_index"


