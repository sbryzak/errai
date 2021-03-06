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

package org.jboss.errai.ioc.client.api;

import org.jboss.errai.bus.client.api.MessageCallback;
import org.jboss.errai.common.client.protocols.MessageParts;

/**
 * An interface that can be used as the injection point for a convenience object that sends single-payload messages to a
 * given subject.
 * <p>
 * Injection of a sender must be qualified with {@link ToSubject} and optionally {@link ReplyTo}.
 * <p>
 * Example:
 * 
 * <pre>
 *  {@code @Inject}
 *  {@code @ToSubject("ListCapitializationService")}
 *  {@code @ReplyTo("ClientListService")}
 *  {@code Sender<String> listSender;}
 * </pre>
 * 
 * @author Mike Brock
 * @author Jonathan Fuerth <jfuerth@redhat.com>
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public interface Sender<T> {

  /**
   * Sends the message to the subject specified by {@link ToSubject}.
   * 
   * @param value
   *          the payload of the message stored in {@link MessageParts#Value}
   */
  public void send(T value);

  /**
   * Sends the message to the subject specified by {@link ToSubject}.
   *
   * @param value
   *          the payload of the message stored in {@link MessageParts#Value}
   *
   * @param replyTo
   *          a callback to be registered to handle a reply from the remote service.
   */
  public void send(T value, MessageCallback replyTo);


}
