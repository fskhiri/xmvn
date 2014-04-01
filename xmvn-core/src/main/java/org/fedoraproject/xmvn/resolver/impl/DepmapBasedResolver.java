/*-
 * Copyright (c) 2012-2014 Red Hat, Inc.
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
package org.fedoraproject.xmvn.resolver.impl;

import static org.fedoraproject.xmvn.utils.FileUtils.followSymlink;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fedoraproject.xmvn.artifact.Artifact;
import org.fedoraproject.xmvn.artifact.DefaultArtifact;
import org.fedoraproject.xmvn.repository.Repository;
import org.fedoraproject.xmvn.repository.RepositoryConfigurator;
import org.fedoraproject.xmvn.repository.RepositoryPath;
import org.fedoraproject.xmvn.resolver.DependencyMap;
import org.fedoraproject.xmvn.resolver.ResolutionRequest;
import org.fedoraproject.xmvn.resolver.ResolutionResult;
import org.fedoraproject.xmvn.resolver.Resolver;
import org.fedoraproject.xmvn.utils.ArtifactUtils;
import org.fedoraproject.xmvn.utils.AtomicFileCounter;
import org.fedoraproject.xmvn.utils.FileUtils;

/**
 * Default implementation of XMvn {@code Resolver} interface.
 * <p>
 * <strong>WARNING</strong>: This class is part of internal implementation of XMvn and it is marked as public only for
 * technical reasons. This class is not part of XMvn API. Client code using XMvn should <strong>not</strong> reference
 * it directly.
 * 
 * @author Mikolaj Izdebski
 */
@Named( "depmap" )
@Singleton
@Deprecated
public class DepmapBasedResolver
    implements Resolver
{
    private final Logger logger = LoggerFactory.getLogger( DepmapBasedResolver.class );

    private final RepositoryConfigurator repositoryConfigurator;

    private Repository bisectRepo;

    private final Repository systemRepo;

    private final DependencyMap depmap;

    private AtomicFileCounter bisectCounter;

    private static final RpmDb rpmdb = new RpmDb();

    @Inject
    public DepmapBasedResolver( RepositoryConfigurator repositoryConfigurator, DependencyMap depmap )
    {
        this.repositoryConfigurator = repositoryConfigurator;
        this.depmap = depmap;

        initializeBisect();

        systemRepo = repositoryConfigurator.configureRepository( "resolve" );
    }

    private void initializeBisect()
    {
        try
        {
            String bisectCounterPath = System.getProperty( "xmvn.bisect.counter" );
            if ( StringUtils.isEmpty( bisectCounterPath ) )
            {
                logger.debug( "Bisection build is not enabled" );
                return;
            }
            bisectCounter = new AtomicFileCounter( bisectCounterPath );

            bisectRepo = repositoryConfigurator.configureRepository( "bisect" );

            logger.info( "Enabled XMvn bisection build" );
        }
        catch ( IOException e )
        {
            logger.error( "Unable to initialize XMvn bisection build", e );
            throw new RuntimeException( e );
        }
    }

    private DefaultResolutionResult tryResolveFromBisectRepo( Artifact artifact )
    {
        try
        {
            if ( bisectCounter == null || bisectRepo == null || bisectCounter.tryDecrement() == 0 )
                return null;

            Path artifactPath = bisectRepo.getPrimaryArtifactPath( artifact ).getPath();
            if ( Files.exists( artifactPath ) )
                return new DefaultResolutionResult( artifactPath, bisectRepo );

            return new DefaultResolutionResult();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    /**
     * Translate artifact to list of JPP artifacts (there may be more than one JPP artifacts if there are multiple
     * depmap entries for the same artifact, like when two packages install the same artifact, but different version).
     * Depmaps are always versionless, so use versionless artifact. Returned JPP artifacts are also versionless.
     * 
     * @param artifact Maven artifact to translate
     * @return list of versionless JPP artifacts corresponding to given artifact
     */
    private List<Artifact> getJppArtifactList( Artifact artifact )
    {
        Artifact versionlessArtifact = artifact.setVersion( null );
        Set<Artifact> jppArtifacts = new LinkedHashSet<>( depmap.translate( versionlessArtifact ) );

        // For POM artifacts besides standard mapping we need to use backwards-compatible mappings too. We set extension
        // to "jar", translate the artifact using depmaps and set extensions back to "pom".
        if ( artifact.getExtension().equals( "pom" ) )
        {
            Artifact jarArtifact =
                new DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(), null, artifact.getClassifier(),
                                     null );
            for ( Artifact jppJarArtifact : depmap.translate( jarArtifact ) )
            {
                Artifact jppPomArtifact =
                    new DefaultArtifact( jppJarArtifact.getGroupId(), jppJarArtifact.getArtifactId(), "pom",
                                         jppJarArtifact.getClassifier(), null );
                jppArtifacts.add( jppPomArtifact );
            }
        }

        return new ArrayList<>( jppArtifacts );
    }

    private DefaultResolutionResult tryResolveFromJavaHome( List<Artifact> jppArtifacts )
    {
        String javaHome = System.getProperty( "java.home" );
        if ( javaHome == null )
            return null;

        Path javaHomeDir = followSymlink( Paths.get( javaHome ) );

        for ( Artifact jppArtifact : jppArtifacts )
        {
            if ( jppArtifact.getGroupId().equals( "JAVA_HOME" ) )
            {
                Path artifactPath =
                    javaHomeDir.resolve( Paths.get( jppArtifact.getArtifactId() + "." + jppArtifact.getExtension() ) );
                artifactPath = followSymlink( artifactPath );
                if ( Files.exists( artifactPath ) )
                    return new DefaultResolutionResult( artifactPath );
            }
        }

        return null;
    }

    private DefaultResolutionResult tryResolveFromConfiguredRepos( List<Artifact> jppArtifacts, String requestedVersion )
    {
        List<String> versionList = Arrays.asList( requestedVersion, Artifact.DEFAULT_VERSION );
        Set<String> orderedVersionSet = new LinkedHashSet<>( versionList );

        for ( String version : orderedVersionSet )
        {
            for ( ListIterator<Artifact> it = jppArtifacts.listIterator(); it.hasNext(); )
                it.set( it.next().setVersion( version ) );

            for ( RepositoryPath repoPath : systemRepo.getArtifactPaths( jppArtifacts, true ) )
            {
                Path artifactPath = repoPath.getPath();
                logger.debug( "Checking artifact path: {}", artifactPath );
                if ( Files.exists( artifactPath ) )
                {
                    DefaultResolutionResult result = new DefaultResolutionResult( artifactPath );
                    result.setCompatVersion( version );
                    result.setRepository( repoPath.getRepository() );
                    return result;
                }
            }
        }

        return null;
    }

    @Override
    public ResolutionResult resolve( ResolutionRequest request )
    {
        Artifact artifact = request.getArtifact();
        logger.debug( "Trying to resolve artifact {}", artifact );

        List<Artifact> jppArtifacts = getJppArtifactList( artifact );
        logger.debug( "JPP artifacts considered during resolution: {}", ArtifactUtils.collectionToString( jppArtifacts ) );
        jppArtifacts.add( artifact );

        DefaultResolutionResult result = tryResolveFromBisectRepo( artifact );
        if ( result == null )
            result = tryResolveFromJavaHome( jppArtifacts );
        if ( result == null )
            result = tryResolveFromConfiguredRepos( jppArtifacts, artifact.getVersion() );

        if ( result == null )
        {
            logger.info( "Failed to resolve artifact: {}", artifact );
            return new DefaultResolutionResult();
        }

        Path artifactPath = result.getArtifactPath();
        if ( artifactPath != null )
        {
            artifactPath = FileUtils.followSymlink( artifactPath );
            logger.debug( "Artifact {} was resolved to {}", artifact, artifactPath );
            if ( request.isProviderNeeded() )
                result.setProvider( rpmdb.lookupPath( artifactPath ) );
        }

        return result;
    }
}