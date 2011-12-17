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

package org.jboss.errai.marshalling.server.marshallers;

import org.jboss.errai.common.client.protocols.SerializationParts;
import org.jboss.errai.marshalling.client.api.Marshaller;
import org.jboss.errai.marshalling.client.api.MarshallingSession;
import org.jboss.errai.marshalling.client.api.annotations.ServerMarshaller;
import org.jboss.errai.marshalling.server.util.ServerMarshallUtil;

import java.util.Map;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
@ServerMarshaller
public class ServerBooleanMarshaller implements Marshaller<Object, Boolean> {
  @Override
  public Class<Boolean> getTypeHandled() {
    return Boolean.class;
  }

  @Override
  public String getEncodingType() {
    return "json";
  }

  @Override
  public Boolean demarshall(Object o, MarshallingSession ctx) {
    if (o instanceof Map) {
      Object v = ((Map) o).get(SerializationParts.NUMERIC_VALUE);
      if (v instanceof Boolean) {
        return (Boolean) v;
      }
      else {
        return Boolean.parseBoolean(String.valueOf(v));
      }
    }
    else {
      return (Boolean) o;
    }
  }

  @Override
  public String marshall(Boolean o, MarshallingSession ctx) {
    return o.toString();
  }

  @Override
  public boolean handles(Object o) {
    return ServerMarshallUtil.handles(o, getTypeHandled());
  }
}