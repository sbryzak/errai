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

package org.jboss.errai.bus.client.json;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import org.jboss.errai.bus.client.api.Message;
import org.jboss.errai.bus.client.api.base.CommandMessage;
import org.jboss.errai.bus.client.framework.MarshalledMessage;
import org.jboss.errai.bus.client.util.BusTools;
import org.jboss.errai.marshalling.client.api.MarshallerFramework;
import org.jboss.errai.marshalling.client.api.json.impl.gwt.GWTJSON;
import org.jboss.errai.marshalling.client.protocols.ErraiProtocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JSONUtilCli {
  public static List<MarshalledMessage> decodePayload(String value) {
    if (value == null || value.trim().length() == 0) return Collections.emptyList();

    /**
     * We have to do a two-stage decoding of the message.  We cannot fully decode the message here, as we
     * cannot be sure the destination endpoint exists within this Errai bundle.  So we extract the ToSubject
     * field and send the unparsed JSON object onwards.
     *
     */


    JSONValue val = null;

    try {
      val = JSONParser.parseStrict(value);
    }
    catch (ClassCastException e) {
      if (!GWT.isProdMode()) {
        System.out.println("*** working around devmode bug ***");
        val = JSONParser.parseStrict(value);
      }
    }

    if (val == null) {
      return Collections.emptyList();
    }
    JSONArray arr = val.isArray();
    if (arr == null) {
      throw new RuntimeException("unrecognized payload" + val.toString());
    }
    ArrayList<MarshalledMessage> list = new ArrayList<MarshalledMessage>();
    unwrap(list, arr);
    return list;

  }

  private static void unwrap(List<MarshalledMessage> messages, JSONArray val) {
    for (int i = 0; i < val.size(); i++) {
      JSONValue v = val.get(i);
      if (v.isArray() != null) {
        unwrap(messages, v.isArray());
      }
      else {
        messages.add(new MarshalledMessageImpl((JSONObject) v));
      }
    }
  }

  public static class MarshalledMessageImpl implements MarshalledMessage {
    public final JSONObject o;

    public MarshalledMessageImpl(JSONObject o) {
      this.o = o;
    }

    public Object getMessage() {
      return o;
    }

    public String getSubject() {
      return o.get("ToSubject").isString().stringValue();
    }
  }

  @SuppressWarnings({"unchecked"})
  public static Map<String, Object> decodePayload(Object value) {
    if (value == null) {
      return null;
    }
    if (!(value instanceof JSONObject)) {
      throw new RuntimeException("bad payload: " + value);
    }

    return ErraiProtocol.decodePayload(GWTJSON.wrap((JSONObject) value));
  }

  public static Message decodeCommandMessage(Object value) {
    return CommandMessage.createWithParts(decodePayload(value));
  }
}
