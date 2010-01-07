package org.apache.maven.surefire.its;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

/**
 * Helper class to assist in using verifier with a staged local repository.
 *
 * @author Stephen Connolly
 * @since 05-Jan-2010 07:36:22
 */
public final class StagedLocalRepoHelper
{
    private StagedLocalRepoHelper()
    {
        throw new IllegalAccessError( "Helper class" );
    }

    private static String toUrl( String filename )
    {
        /*
        * NOTE: Maven fails to properly handle percent-encoded "file:" URLs (WAGON-111) so don't use File.toURI() here
        * as-is but use the decoded path component in the URL.
        */
        String url = "file://" + new File( filename ).toURI().getPath();
        if ( url.endsWith( "/" ) )
        {
            url = url.substring( 0, url.length() - 1 );
        }
        return url;
    }


    public static void createStagedSettingsXml( File originalSettingsXml, File stagedLocalRepo, File stagedSettingsXml )
        throws IOException
    {
        Random entropy = new Random();
        SettingsXpp3Reader reader = new SettingsXpp3Reader();
        SettingsXpp3Writer writer = new SettingsXpp3Writer();
        try
        {
            Settings settings = reader.read( new XmlStreamReader( originalSettingsXml ) );

            String localRepo = System.getProperty( "maven.repo.local" );

            if ( localRepo == null )
            {
                localRepo = settings.getLocalRepository();
            }

            if ( localRepo == null )
            {
                localRepo = System.getProperty( "user.home" ) + "/.m2/repository";
            }

            File repoDir = new File( localRepo );

            if ( !repoDir.exists() )
            {
                repoDir.mkdirs();
            }

            // normalize path
            localRepo = repoDir.getAbsolutePath();

            Profile profile = new Profile();
            do
            {
                profile.setId( "stagedLocalRepo" + entropy.nextLong() );
            }
            while ( settings.getProfilesAsMap().containsKey( profile.getId() ) );
            Repository repository = new Repository();
            repository.setId( profile.getId() + entropy.nextLong() );
            RepositoryPolicy policy = new RepositoryPolicy();
            policy.setEnabled( true );
            policy.setChecksumPolicy( "ignore" );
            policy.setUpdatePolicy( "never" );
            repository.setReleases( policy );
            repository.setSnapshots( policy );
            repository.setLayout( "default" );
            repository.setName( "Original Local Repository" );
            repository.setUrl( toUrl( localRepo ) );
            profile.addPluginRepository( repository );
            profile.addRepository( repository );
            settings.addProfile( profile );
            settings.addActiveProfile( profile.getId() );
            settings.setLocalRepository( stagedLocalRepo.getAbsolutePath() );
            writer.write( new FileWriter( stagedSettingsXml ), settings );
        }
        catch ( XmlPullParserException e )
        {
            IOException ioe = new IOException( e.getMessage() );
            ioe.initCause( e );
            throw ioe;
        }
    }
}
