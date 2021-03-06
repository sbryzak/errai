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

package org.jboss.errai.bus.server.util;

import org.jboss.errai.bus.client.api.Message;
import org.jboss.errai.bus.server.api.QueueSession;

import java.util.Collection;

public class SessionContext implements Context {
  private QueueSession session;

  public static SessionContext get(QueueSession session) {
    return new SessionContext(session);
  }

  public static SessionContext get(Message message) {
    return new SessionContext(message.getResource(QueueSession.class, "Session"));
  }

  private SessionContext(QueueSession session) {
    if (session == null) {
      throw new RuntimeException("no session");
    }
    this.session = session;
  }

  public void setAttribute(Enum<?> key, Object value) {
    session.setAttribute(key.toString(), value);
  }

  public <T> T getAttribute(Class<T> type, Enum<?> key) {
    return session.getAttribute(type, key.toString());
  }

  public void setAttribute(Class<?> typeIndexed, Object value) {
    if (session.hasAttribute(typeIndexed.getName())) {
      throw new IllegalStateException("The type-indexed property already exists: " + typeIndexed.getName());
    }

    session.setAttribute(typeIndexed.getName(), value);
  }

  public <T> T getAttribute(Class<T> type, Class<?> typeIndexed) {
    return session.getAttribute(type, typeIndexed.getName());
  }

  public <T> T getAttribute(Class<T> type) {
    return getAttribute(type, type);
  }

  public void setAttribute(String param, Object value) {
    session.setAttribute(param, value);
  }

  public <T> T getAttribute(Class<T> type, String param) {
    return session.getAttribute(type, param);
  }

  public Collection<String> getAttributeNames() {
    return session.getAttributeNames();
  }

  public boolean removeAttribute(Enum key) {
    return session.removeAttribute(key.toString());
  }

  public boolean removeAttribute(Class<?> typeIndexed) {
    return session.removeAttribute(typeIndexed.getName());
  }

  public boolean removeAttribute(String param) {
    return session.removeAttribute(param);
  }

  public QueueSession getSession() {
    return session;
  }
}
