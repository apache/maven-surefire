Surefire need maven 3 to be build.
But in order to test it tests you can do:
* -DmavenHomeUsed= path to a maven 2.x home
* or -Pmaven-2.2.1 this profile will download a maven 2.2.1 distrib and use it for integration tests.

Deploying web site
-------------------
You can use the script deploySite.sh
Without any profile the site will be deploy to http://maven.apache.org/surefire-archives/surefire-${project.version}
sh ./deploySite.sh -Preporting

To deploy main version http://maven.apache.org/surefire
Use
sh ./deploySite.sh -Preporting -P site-release

Note you can add arguments to the script to pass your svn credentials:
-Dusername=
-Dpassword=

