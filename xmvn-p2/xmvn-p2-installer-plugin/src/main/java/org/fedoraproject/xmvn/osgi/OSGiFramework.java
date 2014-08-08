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
package org.fedoraproject.xmvn.osgi;

import org.osgi.framework.BundleContext;

/**
 * Allows Eclipse Equinox OSGi framework to be embedded in the running JVM.
 * 
 * @author Mikolaj Izdebski
 */
public interface OSGiFramework
{
    /**
     * Obtain bundle context of embedded OSGi framework. This causes the framework to be launched if it is not running
     * yet.
     * 
     * @return bundle context of embedded OSGi framework
     */
    BundleContext getBundleContext();
}