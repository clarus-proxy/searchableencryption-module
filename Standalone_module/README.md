# Searchable Encryption (SE) module


Instructions to install, test and update the module for Searchable Encryption.



**Author**: Monir Azraoui

**Contact**: name.surname(at)eurecom(dot)fr

**Last modification**: October 30, 2017


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


-  `SE_module_benchmarks`:

It contains the source code used for benchmarking the SE module. It also contains the benchmarks results.


-  `SQL_scripts`:

It contains some test files to be used as inputs of the demo program.


-  `classpath`:

It contains some JARs files to be included to the classpath of the program.



-  `cloud_search_with_SE`:

It contains the source code of a Maven project implementing the Search function at the Cloud (an run by the PostgreSQL server).


- `demo_SE_module`:

It contains the source code of a project for demo. It simulates the operations of the CLARUS proxy calling the SE module.


---
## Installation

To install all the prerequisites and the different elements of the SE module (the demo program, and the search function run by the cloud server), please run the following instruction:

> sudo bash sudo_script_setup_VM

During the execution of the above command, you will be asked your username and password to access the EURECOM's GITLAB. 

At some points, many lines indicate [ERROR], DO NOT WORRY! :)

If everything seems OK, please go to the section 'Run the demo'.

Below, we give the installation instructions that are executed by the script `sudo_script_setup_VM` (if you want to execute all the instructions one by one instead of running the script or if you want to check the correctness of the script).

---
### 1. Prerequisites
- Update apt-get repo list

> sudo apt-get update

- Install Git

> sudo apt-get install -y git

- Checkout SE module project from EURECOM gitlab

> sudo cd ~/Desktop

> sudo git clone https://gitlab.eurecom.fr/monir.azraoui/SE_module_final

> sudo chmod 777 -R SE_module_final/

- Install C compiler

> sudo apt-get install g++

- Install JDK 1.8 (Java)

> sudo apt-get install openjdk-8-jdk

- Install Maven

> sudo apt-get install maven

- Install Postgresql

> sudo apt-get install postgresql-common

> sudo apt-get install postgresql

> sudo apt-get install postgresql-server-dev-X.Y (current version X.Y = 9.5)

> sudo apt-get install libecpg-dev

> sudo apt-get install libkrb5-dev

- Install and Build PL/Java 

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

The 4 following parameters can change in the path according to your system:

1. 9.5 is the postgresql version

1. amd64 is the architecture of the system

1. Linux is the OS

1. gpp is the link

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

`[path_to_the.jar]` is the path the `cloud_search_with_SE-1.jar`. For example `'file:///home/eurecom/Desktop/SE_module_final/JARs/cloud_search_with_SE-1.jar'`. 

Don't forget the ";" at the end of the command.

So for example, you will run:

> select sqlj.install_jar('file:///home/eurecom/Desktop/SE_module_final/JARs/cloud_search_with_SE-1.jar', 'search_with_SE', true);

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

- Add some JARs in the local Maven repository:

> mvn install:install-file -DgroupId=commons-lang -DartifactId=commons-lang -Dversion=2.6 -Dpackaging=jar -Dfile=SE_module_final/classpath/commons-lang-2.6.jar -DgeneratePom=true

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

> cd ~/Desktop/SE_module_final/ 

> java -cp JARs/demo_SE_module.jar demo.test.demo_main [path_to_CSV_file] 

Example: 

> java -cp JARs/demo_SE_module.jar demo.test.demo_main /home/eurecom/Desktop/clarus/searchable_encryption/DB_csv_or_sql/lab.csv

#### Troubleshootings
- You can find a step by step demo storyline with screenshots in the file "step_by_step_demo.pdf" in the gitlab. 

- If you get no error, you can check in PostgreSQL, the 2 tables that have been created during the upload of the encrypted database using the following command:

> sudo -u postgres psql

> \d

You should have two tables ending with `_encrypted` and `_ index`.

- If during the execution of the above `java -cp` instruction you get an error with `Permission denied`, you probably need to be `sudo`


---
##  Update the source code

These instructions have been tested on a Ubuntu 16.04 machine that runs **Eclipse Luna**.

### 1. Prerequisites and initial setting

- Add some PL/Java related JARs into the local maven repository

> cd ~/Desktop/pljava

> mvn install:install-file -DgroupId=org.postgresql -DartifactId=pljava-api -Dversion=1.6.0-SNAPSHOT -Dpackaging=jar -Dfile=pljava-api/target/pljava-api-1.6.0-SNAPSHOT.jar -DgeneratePom=true

> mvn install:install-file -DgroupId=org.postgresql -DartifactId=pljava -Dversion=1.6.0-SNAPSHOT -Dpackaging=jar -Dfile=pljava/target/pljava-1.6.0-SNAPSHOT.jar -DgeneratePom=true


- Download Eclipse Luna (Eclipse IDE for Java EE Developers). It may take some time (15-20 minutes).

```
http://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/lunasr2
```

- Extract the archive you download in the Desktop.

- Open a terminal and run:

> mkdir ~/Desktop/mkdir

> cd ~/Desktop/eclipse

> ./eclipse &



- In Eclipse:

  * Select the folder `workspace` just created with the above `mkdir` command as the current workspace. 

  * `File > Import... > Maven > Existing Maven Projects` then `Next` 
  
  * In the field **Root directory**, click on **Browse** and find the folder `SE_module_final`. Then `OK`.

  * Untick the 3 projects that start with `SE_module_benchmarks`. Then `Finish`.
  
  * In the **Project Explorer**, Right click on one of the project: `Maven > Update project...`. Then `Select all` and `OK`.

  * Right click on `SE_module` project then `Run As > Maven build...`. In the field `Goals` write **clean package install**. Then click `Run`.

  * Do not worry if in the project names, some yellow triangles with an exclamation mark appear. They are just warning, we can ignore them.

- To test that the configuration is OK do:
 
  * Right click on `demo_SE_module > Run as > Run configurations... > Java Applications`. In the field `Main class`, click on `Search` and select `demo_main` as the main class. Then go to the `Arguments` tab, and in the field `Program arguments`, write (or copy-paste)  the entire path to a test file (for example lab.csv). Then click `Run`.
  
### 2. Update instructions (in Eclipse)

A schema describing the architecture and the workflow of the code can be found in the file `SE_module_demo_workflow.pdf` in the gitlab project folder. 


- What you need to understand:

  - The project `demo_SE_module` depends on the project `SE_module`. When you modify the code from `SE_module`, you have to tell to `demo_SE_module` that some changes occured. 

  - The project `cloud_search_with_SE` is independent. But since it is installed in the PostgreSQL server, you have to tell to the PostgreSQL server that some changes occured.

  - The instructions here list what to do when you make some changes in one of the project.

- What to do after you make some changes in the project `cloud_search_with_SE`:

  - In the Project Explorer of Eclipse, right-click on `cloud_search_with_SE`, then `Maven > Update project`.

  - Then again right-click on `cloud_search_with_SE`, then `Run as > Maven build...`. In the field `Goals`, write **clean package**, then click `Run`.

  - You will see (either in the Project Explorer, or in the Ubuntu's Files Explorer) that the previous command creates a JAR file in the folder `target` with the name `cloud_search_with_SE-1.jar`. This newly updated JAR needs to be updated in the PostgreSQL server.
For simplicity, you can first copy-paste this jar in the `JARs` folder of the local `SE_module_final` gitlab folder. 

  - Open a Terminal in Ubuntu and run : 

> sudo -u postgres psql
The command line starts with **postgres=#**.

> select sqlj.remove_jar('search_with_se', true);

> select sqlj.install_jar('file://[path_to cloud_search_with_SE-1.jar]', 'search_with_se', true);

For example:

> select sqlj.install_jar('file:///home/eurecom/Desktop/SE_module_final/JARs/cloud_search_with_SE-1.jar', 'search_with_se', true);

Then:

> select sqlj.set_classpath('public', 'search_with_se');

The cloud function of search is updated in PostgreSQL!



- What to do after you make some changes in the project `SE_module`:

  - In the Project Explorer of Eclipse, right-click on `SE_module`, then `Maven > Update project`.

  - Then again right-click on `SE_module`, then `Run as > Maven build...`. In the field `Goals`, write **clean package install**, then click `Run`. This creates a new Jar (that can be found in the folder `target`) and installs this Jar in the local Maven repository, such that other projects that use this Jar can invoke it.


- What to do after you make some changes in the project `demo_SE_module`:

  - In the Project Explorer of Eclipse, right-click on `demo_SE_module`, then `Maven > Update project`.

  - Then again right-click on `demo_SE_module`, then `Run as > Maven build...`. In the field `Goals`, write **clean package**, then click `Run`. This creates a new Jar (that can be found in the folder `target`) whose name ends with "jar-with-dependencies". This is the JAR that you tested in section "Run the demo".

  
