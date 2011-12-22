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

package org.jboss.errai.marshalling.client.api.json.impl.gwt;

import com.google.gwt.json.client.JSONBoolean;
import org.jboss.errai.marshalling.client.api.json.EJBoolean;

/**
 * @author Mike Brock
 */
public class GWTJSONBoolean implements EJBoolean {
  final JSONBoolean bool;

  public GWTJSONBoolean(JSONBoolean bool) {
    this.bool = bool;
  }

  @Override
  public boolean booleanValue() {
    return bool.booleanValue();
  }
}