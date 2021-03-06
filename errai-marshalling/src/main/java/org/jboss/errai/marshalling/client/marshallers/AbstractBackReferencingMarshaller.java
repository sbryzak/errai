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

package org.jboss.errai.marshalling.client.marshallers;

import org.jboss.errai.common.client.protocols.SerializationParts;
import org.jboss.errai.marshalling.client.api.MarshallingSession;

/**
 * @author Mike Brock
 */
public abstract class AbstractBackReferencingMarshaller<C> extends AbstractJSONMarshaller<C> {

  @Override
  public final String marshall(C o, MarshallingSession ctx) {
    if (o == null) {
      return "null";
    }

    final boolean isNew = !ctx.isEncoded(o);
    final String objId = ctx.getObjectHash(o);

    final StringBuilder buf = new StringBuilder("{\"").append(SerializationParts.ENCODED_TYPE).append("\":\"")
            .append(o.getClass().getName()).append("\",\"").append(SerializationParts.OBJECT_ID).append("\":\"")
            .append(objId).append("\"");

    if (!isNew) {
      return buf.append("}").toString();
    }
    else {
      doMarshall(buf.append(",\"").append(SerializationParts.QUALIFIED_VALUE).append("\":"), o, ctx);
      return buf.append("}").toString();
    }
  }

  public abstract void doMarshall(StringBuilder buf, C o, MarshallingSession ctx);
}
