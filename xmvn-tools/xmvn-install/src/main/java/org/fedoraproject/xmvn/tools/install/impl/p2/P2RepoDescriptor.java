/*-
 * Copyright (c) 2014 Red Hat, Inc.
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
package org.fedoraproject.xmvn.tools.install.impl.p2;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Mikolaj Izdebski
 */
public class P2RepoDescriptor
{
    private String repoId;

    private final Set<Path> plugins = new LinkedHashSet<>();

    private final Set<Path> features = new LinkedHashSet<>();

    public String getRepoId()
    {
        return repoId;
    }

    public void setTargetPackage( String repoId )
    {
        this.repoId = repoId;
    }

    public void addPlugin( Path bundlePath )
    {
        plugins.add( bundlePath );
    }

    public Collection<Path> getPlugins()
    {
        return Collections.unmodifiableSet( plugins );
    }

    public void addFeature( Path bundlePath )
    {
        features.add( bundlePath );
    }

    public Collection<Path> getFeatures()
    {
        return Collections.unmodifiableSet( features );
    }
}
