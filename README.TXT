Surefire needs Maven 3 to be built.
But in order to test it tests, you can do:
* -DmavenHomeUsed= path to a Maven 2.x home
* or -Pmaven-2.2.1, this profile will download a Maven 2.2.1 distrib and use it for integration tests.
In order to run tests for a release check during the vote the following memory requirements are needed:
export MAVEN_OPTS="-Xmx2048m -Xms1024m -XX:MaxPermSize=512m -Djava.awt.headless=true"

Deploying web site
------------------

see http://maven.apache.org/developers/website/deploy-component-reference-documentation.html