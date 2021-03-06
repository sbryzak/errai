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

package org.jboss.errai.bus.server.io;

import org.jboss.errai.bus.client.api.Message;
import org.jboss.errai.bus.client.api.MessageCallback;
import org.jboss.errai.bus.client.api.base.MessageDeliveryFailure;
import org.jboss.errai.bus.client.framework.MessageBus;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import static org.jboss.errai.bus.client.api.base.MessageBuilder.createConversation;
import static org.mvel2.DataConversion.canConvert;
import static org.mvel2.DataConversion.convert;

/**
 * <tt>ConversationalEndpointCallback</tt> creates a conversation that invokes an endpoint function
 */
public class ConversationalEndpointCallback implements MessageCallback {
  private ServiceInstanceProvider serviceProvider;
  private Class[] targetTypes;
  private Method method;
  private MessageBus bus;

  /**
   * Initializes the service, method and bus
   * 
   * @param genericSvc
   *          - the service the bus is subscribed to
   * @param method
   *          - the endpoint function
   * @param bus
   *          - the bus to send the messages on
   */
  public ConversationalEndpointCallback(ServiceInstanceProvider genericSvc, Method method, MessageBus bus) {
    this.serviceProvider = genericSvc;
    this.targetTypes = (this.method = method).getParameterTypes();
    this.bus = bus;
  }

  /**
   * Callback function. Creates the conversation and invokes the endpoint using the message specified
   * 
   * @param message
   *          - the message to initiate the conversation
   */
  @SuppressWarnings({ "unchecked" })
  public void callback(Message message) {
    Object[] parms = message.get(Object[].class, "MethodParms");

    if ((parms == null && targetTypes.length != 0) || (parms.length != targetTypes.length)) {
      throw new MessageDeliveryFailure("wrong number of arguments sent to endpoint. (received: "
              + (parms == null ? 0 : parms.length) + "; required: " + targetTypes.length + ")");
    }
    for (int i = 0; i < parms.length; i++) {
      if (parms[i] != null && !targetTypes[i].isAssignableFrom(parms[i].getClass())) {
        if (canConvert(targetTypes[i], parms[i].getClass())) {
          parms[i] = convert(parms[i], targetTypes[i]);
        }
        else if (targetTypes[i].isArray()) {
          if (parms[i] instanceof Collection) {
            Collection c = (Collection) parms[i];
            parms[i] = c.toArray((Object[]) Array.newInstance(targetTypes[i].getComponentType(), c.size()));

          }
          else if (parms[i].getClass().isArray()) {

            int length = Array.getLength(parms[i]);
            Class toComponentType = parms[i].getClass().getComponentType();
            Object parmValue = parms[i];
            Object newArray = Array.newInstance(targetTypes[i].getComponentType(), length);

            for (int x = 0; x < length; x++) {
              Array.set(newArray, x, convert(Array.get(parmValue, x), toComponentType));
            }

            parms[i] = newArray;
          }

        }
        else {
          throw new MessageDeliveryFailure("type mismatch in method parameters");
        }
      }
    }

    try {
      Object methReply = method.invoke(serviceProvider.get(message), parms);

      if (method.getReturnType().equals(void.class)) {
        createConversation(message)
            .subjectProvided()
            .noErrorHandling().sendNowWith(bus);
      }
      else {
        createConversation(message)
              .subjectProvided()
              .with("MethodReply", methReply)
              .noErrorHandling().sendNowWith(bus);
      }
    }
    catch (MessageDeliveryFailure e) {
      throw e;
    }
    catch (InvocationTargetException e) {
      throw new MessageDeliveryFailure("error invoking endpoint", e.getCause());
    }
    catch (Exception e) {
      throw new MessageDeliveryFailure("error invoking endpoint", e);
    }
  }
}