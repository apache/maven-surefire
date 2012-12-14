Surefire needs Maven 3 to be built.
But in order to test it tests, you can do:
* -DmavenHomeUsed= path to a Maven 2.x home
* or -Pmaven-2.2.1, this profile will download a Maven 2.2.1 distrib and use it for integration tests.

Deploying web site
------------------
You can use the deploySite.sh script
Without any profile the site will be deployed to http://maven.apache.org/surefire-archives/surefire-${project.version}
sh ./deploySite.sh -Preporting

To deploy main version http://maven.apache.org/surefire, use
sh ./deploySite.sh -Preporting -Psite-release

Note you can add arguments to the script to pass your svn credentials:
-Dusername=
-Dpassword=

Workflow for site when releasing
--------------------------------
Once release staged, you can publish a staged site.
cd target/checkout
sh ./deploySite.sh -Preporting
content will be in http://maven.apache.org/surefire-archives/surefire-${project.version}

If something goes wrong when publishing the site to svnpubsub (like https://jira.codehaus.org/browse/MSCMPUB-6),
you can avoid rebuilding the site before publishing with
mvn -Preporting scm-publish:publish-scm

Once vote passed, redeploy main site:
cd target/checkout (or use the version tag)
sh ./deploySite.sh -Preporting -Psite-release



