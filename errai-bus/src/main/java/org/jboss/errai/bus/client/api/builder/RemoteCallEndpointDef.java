/*
 * Copyright 2011 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.bus.client.api.builder;

/**
 * This interface, <tt>RemoteCallEndpointDef</tt> is a template for creating a remote call endpoint. It ensures that
 * the endpoint is constructed properly
 */
public interface RemoteCallEndpointDef {

  /**
   * Sets the endpoint for a message using the specified name
   *
   * @param endPointName - name of endpoint
   * @return an instance of <tt>RemoteCallResponseDef</tt>
   */
  public RemoteCallResponseDef endpoint(String endPointName);

  /**
   * Sets the endpoint for a message using the specified name
   *
   * @param endPointName - name of endpoint
   * @param args         - the parameters for the endpoint function
   * @return an instance of <tt>RemoteCallResponseDef</tt>
   */
  public RemoteCallResponseDef endpoint(String endPointName, Object... args);
}
