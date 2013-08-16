/*-
 * Copyright (c) 2013 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fedoraproject.maven.repository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.fedoraproject.maven.config.Configuration;
import org.fedoraproject.maven.config.Configurator;
import org.fedoraproject.maven.config.Repository;
import org.fedoraproject.maven.config.RepositoryConfigurator;
import org.fedoraproject.maven.model.Artifact;

/**
 * @author Mikolaj Izdebski
 */
public class NestedRepositoriesTest
    extends PlexusTestCase
{
    private final Artifact POM = new Artifact( "JPP/sisu", "sisu-plexus", "1.2.3", "pom" );

    private final Artifact POM2 = new Artifact( "JPP/plexus", "utils", "3.0.14", "pom" );

    private final Artifact JAR = new Artifact( "JPP/sisu", "sisu-plexus", "1.2.3", "jar" );;

    private final Artifact JAR2 = new Artifact( "JPP/plexus", "utils", "3.0.14", "jar" );;

    private org.fedoraproject.maven.repository.Repository base;

    private org.fedoraproject.maven.repository.Repository addon;

    private void configureBaseEffectivePomRepository( Configuration configuration )
    {
        Repository repo = new Repository();
        repo.setId( "base-effective-pom" );
        repo.setType( "flat" );
        repo.addArtifactType( "pom" );
        repo.addProperty( "root", "usr/share/maven-effective-poms" );
        configuration.addRepository( repo );
    }

    private void configureBasePomRepository( Configuration configuration )
    {
        Repository repo = new Repository();
        repo.setId( "base-pom" );
        repo.setType( "flat" );
        repo.addArtifactType( "pom" );
        repo.addProperty( "root", "usr/share/maven-poms" );
        configuration.addRepository( repo );
    }

    private void configureBaseJniRepository( Configuration configuration )
    {
        Repository repo = new Repository();
        repo.setId( "base-jni" );
        repo.setType( "jpp" );
        repo.addArtifactType( "jar" );
        repo.addProperty( "root", "usr/lib/java" );
        configuration.addRepository( repo );
    }

    private void configureBaseJarRepository( Configuration configuration )
    {
        Repository repo = new Repository();
        repo.setId( "base-jar" );
        repo.setType( "jpp" );
        repo.addArtifactType( "jar" );
        repo.addProperty( "root", "usr/share/java" );
        configuration.addRepository( repo );
    }

    private void configureBaseRepository( Configuration configuration )
    {
        configureBaseEffectivePomRepository( configuration );
        configureBasePomRepository( configuration );
        configureBaseJniRepository( configuration );
        configureBaseJarRepository( configuration );

        Repository repo = new Repository();
        repo.setId( "base" );
        repo.setType( "compound" );
        Xpp3Dom child1 = new Xpp3Dom( "repository" );
        child1.setValue( "base-effective-pom" );
        Xpp3Dom child2 = new Xpp3Dom( "repository" );
        child2.setValue( "base-pom" );
        Xpp3Dom child3 = new Xpp3Dom( "repository" );
        child3.setValue( "base-jni" );
        Xpp3Dom child4 = new Xpp3Dom( "repository" );
        child4.setValue( "base-jar" );
        Xpp3Dom childreen = new Xpp3Dom( "repositories" );
        childreen.addChild( child1 );
        childreen.addChild( child2 );
        childreen.addChild( child3 );
        childreen.addChild( child4 );
        Xpp3Dom config = new Xpp3Dom( "configuration" );
        config.addChild( childreen );
        repo.setConfiguration( config );
        configuration.addRepository( repo );
    }

    private void configureAddonPrefixRepository( Configuration configuration )
    {
        Repository repo = new Repository();
        repo.setId( "addon-prefix" );
        repo.setType( "compound" );
        repo.addProperty( "prefix", "opt/rh/addon" );
        Xpp3Dom child = new Xpp3Dom( "repository" );
        child.setValue( "base" );
        Xpp3Dom childreen = new Xpp3Dom( "repositories" );
        childreen.addChild( child );
        Xpp3Dom config = new Xpp3Dom( "configuration" );
        config.addChild( childreen );
        repo.setConfiguration( config );
        configuration.addRepository( repo );
    }

    private void configureAddonRepository( Configuration configuration )
    {
        configureAddonPrefixRepository( configuration );

        Repository repo = new Repository();
        repo.setId( "addon" );
        repo.setType( "compound" );
        Xpp3Dom child1 = new Xpp3Dom( "repository" );
        child1.setValue( "addon-prefix" );
        Xpp3Dom child2 = new Xpp3Dom( "repository" );
        child2.setValue( "base" );
        Xpp3Dom childreen = new Xpp3Dom( "repositories" );
        childreen.addChild( child1 );
        childreen.addChild( child2 );
        Xpp3Dom config = new Xpp3Dom( "configuration" );
        config.addChild( childreen );
        repo.setConfiguration( config );
        configuration.addRepository( repo );
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        Configurator configurator = lookup( Configurator.class );
        RepositoryConfigurator repositoryConfigurator = lookup( RepositoryConfigurator.class );

        Configuration configuration = configurator.getDefaultConfiguration();
        configureBaseRepository( configuration );
        configureAddonRepository( configuration );

        base = repositoryConfigurator.configureRepository( "base" );
        addon = repositoryConfigurator.configureRepository( "addon" );
    }

    /**
     * Test resolution of versioned POM artifacts from base repository.
     * 
     * @throws Exception
     */
    public void testBasePomVersioned()
        throws Exception
    {
        Iterator<Path> it = base.getArtifactPaths( POM ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-effective-poms/JPP.sisu-sisu-plexus-1.2.3.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-poms/JPP.sisu-sisu-plexus-1.2.3.pom" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of versionless POM artifacts from base repository.
     * 
     * @throws Exception
     */
    public void testBasePomVersionless()
        throws Exception
    {
        Iterator<Path> it = base.getArtifactPaths( POM.clearVersion() ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-effective-poms/JPP.sisu-sisu-plexus.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-poms/JPP.sisu-sisu-plexus.pom" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of versioned JAR artifacts from base repository.
     * 
     * @throws Exception
     */
    public void testBaseJarVersioned()
        throws Exception
    {
        Iterator<Path> it = base.getArtifactPaths( JAR ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/lib/java/sisu/sisu-plexus-1.2.3.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/java/sisu/sisu-plexus-1.2.3.jar" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of versionless JAR artifacts from base repository.
     * 
     * @throws Exception
     */
    public void testBaseJarVersionless()
        throws Exception
    {
        Iterator<Path> it = base.getArtifactPaths( JAR.clearVersion() ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/lib/java/sisu/sisu-plexus.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/java/sisu/sisu-plexus.jar" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of versioned POM artifacts from addon repository.
     * 
     * @throws Exception
     */
    public void testAddonPomVersioned()
        throws Exception
    {
        Iterator<Path> it = addon.getArtifactPaths( POM ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/maven-effective-poms/JPP.sisu-sisu-plexus-1.2.3.pom" ),
                      it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/maven-poms/JPP.sisu-sisu-plexus-1.2.3.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-effective-poms/JPP.sisu-sisu-plexus-1.2.3.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-poms/JPP.sisu-sisu-plexus-1.2.3.pom" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of versionless POM artifacts from addon repository.
     * 
     * @throws Exception
     */
    public void testAddonPomVersionless()
        throws Exception
    {
        Iterator<Path> it = addon.getArtifactPaths( POM.clearVersion() ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/maven-effective-poms/JPP.sisu-sisu-plexus.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/maven-poms/JPP.sisu-sisu-plexus.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-effective-poms/JPP.sisu-sisu-plexus.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-poms/JPP.sisu-sisu-plexus.pom" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of versioned JAR artifacts from addon repository.
     * 
     * @throws Exception
     */
    public void testAddonJarVersioned()
        throws Exception
    {
        Iterator<Path> it = addon.getArtifactPaths( JAR ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/lib/java/sisu/sisu-plexus-1.2.3.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/java/sisu/sisu-plexus-1.2.3.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/lib/java/sisu/sisu-plexus-1.2.3.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/java/sisu/sisu-plexus-1.2.3.jar" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of versionless JAR artifacts from addon repository.
     * 
     * @throws Exception
     */
    public void testAddonJarVersionless()
        throws Exception
    {
        Iterator<Path> it = addon.getArtifactPaths( JAR.clearVersion() ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/lib/java/sisu/sisu-plexus.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/java/sisu/sisu-plexus.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/lib/java/sisu/sisu-plexus.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/java/sisu/sisu-plexus.jar" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of alternative versioned POM artifacts from base repository.
     * 
     * @throws Exception
     */
    public void testAlternativeBasePomVersioned()
        throws Exception
    {
        Iterator<Path> it = base.getArtifactPaths( Arrays.asList( POM, POM2 ) ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-effective-poms/JPP.sisu-sisu-plexus-1.2.3.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-effective-poms/JPP.plexus-utils-3.0.14.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-poms/JPP.sisu-sisu-plexus-1.2.3.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-poms/JPP.plexus-utils-3.0.14.pom" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of alternative versionless POM artifacts from base repository.
     * 
     * @throws Exception
     */
    public void testAlternativeBasePomVersionless()
        throws Exception
    {
        Iterator<Path> it =
            base.getArtifactPaths( Arrays.asList( POM.clearVersion(), POM2.clearVersion() ) ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-effective-poms/JPP.sisu-sisu-plexus.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-effective-poms/JPP.plexus-utils.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-poms/JPP.sisu-sisu-plexus.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-poms/JPP.plexus-utils.pom" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of alternative versioned JAR artifacts from base repository.
     * 
     * @throws Exception
     */
    public void testAlternativeBaseJarVersioned()
        throws Exception
    {
        Iterator<Path> it = base.getArtifactPaths( Arrays.asList( JAR, JAR2 ) ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/lib/java/sisu/sisu-plexus-1.2.3.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/lib/java/plexus/utils-3.0.14.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/java/sisu/sisu-plexus-1.2.3.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/java/plexus/utils-3.0.14.jar" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of alternative versionless JAR artifacts from base repository.
     * 
     * @throws Exception
     */
    public void testAlternativeBaseJarVersionless()
        throws Exception
    {
        Iterator<Path> it =
            base.getArtifactPaths( Arrays.asList( JAR.clearVersion(), JAR2.clearVersion() ) ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/lib/java/sisu/sisu-plexus.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/lib/java/plexus/utils.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/java/sisu/sisu-plexus.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/java/plexus/utils.jar" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of alternative versioned POM artifacts from addon repository.
     * 
     * @throws Exception
     */
    public void testAlternativeAddonPomVersioned()
        throws Exception
    {
        Iterator<Path> it = addon.getArtifactPaths( Arrays.asList( POM, POM2 ) ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/maven-effective-poms/JPP.sisu-sisu-plexus-1.2.3.pom" ),
                      it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/maven-effective-poms/JPP.plexus-utils-3.0.14.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/maven-poms/JPP.sisu-sisu-plexus-1.2.3.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/maven-poms/JPP.plexus-utils-3.0.14.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-effective-poms/JPP.sisu-sisu-plexus-1.2.3.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-effective-poms/JPP.plexus-utils-3.0.14.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-poms/JPP.sisu-sisu-plexus-1.2.3.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-poms/JPP.plexus-utils-3.0.14.pom" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of alternative versionless POM artifacts from addon repository.
     * 
     * @throws Exception
     */
    public void testAlternativeAddonPomVersionless()
        throws Exception
    {
        Iterator<Path> it =
            addon.getArtifactPaths( Arrays.asList( POM.clearVersion(), POM2.clearVersion() ) ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/maven-effective-poms/JPP.sisu-sisu-plexus.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/maven-effective-poms/JPP.plexus-utils.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/maven-poms/JPP.sisu-sisu-plexus.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/maven-poms/JPP.plexus-utils.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-effective-poms/JPP.sisu-sisu-plexus.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-effective-poms/JPP.plexus-utils.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-poms/JPP.sisu-sisu-plexus.pom" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/maven-poms/JPP.plexus-utils.pom" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of alternative versioned JAR artifacts from addon repository.
     * 
     * @throws Exception
     */
    public void testAlternativeAddonJarVersioned()
        throws Exception
    {
        Iterator<Path> it = addon.getArtifactPaths( Arrays.asList( JAR, JAR2 ) ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/lib/java/sisu/sisu-plexus-1.2.3.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/lib/java/plexus/utils-3.0.14.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/java/sisu/sisu-plexus-1.2.3.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/java/plexus/utils-3.0.14.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/lib/java/sisu/sisu-plexus-1.2.3.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/lib/java/plexus/utils-3.0.14.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/java/sisu/sisu-plexus-1.2.3.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/java/plexus/utils-3.0.14.jar" ), it.next() );
        assertFalse( it.hasNext() );
    }

    /**
     * Test resolution of alternative versionless JAR artifacts from addon repository.
     * 
     * @throws Exception
     */
    public void testAlternativeAddonJarVersionless()
        throws Exception
    {
        Iterator<Path> it =
            addon.getArtifactPaths( Arrays.asList( JAR.clearVersion(), JAR2.clearVersion() ) ).iterator();
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/lib/java/sisu/sisu-plexus.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/lib/java/plexus/utils.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/java/sisu/sisu-plexus.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "opt/rh/addon/usr/share/java/plexus/utils.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/lib/java/sisu/sisu-plexus.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/lib/java/plexus/utils.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/java/sisu/sisu-plexus.jar" ), it.next() );
        assertTrue( it.hasNext() );
        assertEquals( Paths.get( "usr/share/java/plexus/utils.jar" ), it.next() );
        assertFalse( it.hasNext() );
    }
}