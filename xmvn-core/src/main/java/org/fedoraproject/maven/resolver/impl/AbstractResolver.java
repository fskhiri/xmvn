/*-
 * Copyright (c) 2012-2013 Red Hat, Inc.
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
package org.fedoraproject.maven.resolver.impl;

import java.io.File;

import org.fedoraproject.maven.model.Artifact;
import org.fedoraproject.maven.resolver.ResolutionRequest;
import org.fedoraproject.maven.resolver.ResolutionResult;
import org.fedoraproject.maven.resolver.Resolver;

/**
 * Base class for several resolves implemented in this package.
 * 
 * @author Mikolaj Izdebski
 */
abstract class AbstractResolver
    implements Resolver
{
    @Override
    public abstract ResolutionResult resolve( ResolutionRequest request );

    @Deprecated
    @Override
    public File resolve( String groupId, String artifactId, String version, String extension )
    {
        Artifact artifact = new Artifact( groupId, artifactId, version, extension );
        return resolve( artifact );
    }

    @Deprecated
    @Override
    public File resolve( Artifact artifact )
    {
        ResolutionRequest request = new ResolutionRequest( artifact );
        ResolutionResult result = resolve( request );
        return result.getArtifactFile();
    }
}