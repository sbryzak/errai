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

package org.jboss.errai.bus.server.servlet;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jboss.errai.common.client.api.ResourceProvider;
import org.jboss.errai.bus.client.api.base.DefaultErrorCallback;
import org.jboss.errai.bus.client.framework.MarshalledMessage;
import org.jboss.errai.bus.client.framework.MessageBus;
import org.jboss.errai.bus.client.protocols.BusCommands;
import org.jboss.errai.bus.server.ServerMessageBusImpl;
import org.jboss.errai.bus.server.api.ServerMessageBus;
import org.jboss.errai.bus.server.api.SessionProvider;
import org.jboss.errai.bus.server.service.ErraiService;
import org.jboss.errai.bus.server.service.ErraiServiceConfigurator;
import org.jboss.errai.bus.server.service.ErraiServiceConfiguratorImpl;
import org.jboss.errai.bus.server.service.ErraiServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;

/**
 * The <tt>AbstractErraiServlet</tt> provides a starting point for creating Http-protocol gateway between the server
 * bus and the client buses.
 */
public abstract class AbstractErraiServlet extends HttpServlet {
  /* New and configured errai service */
  protected ErraiService service;

  /* A default Http session provider */
  protected SessionProvider<HttpSession> sessionProvider;

  protected volatile ClassLoader contextClassLoader;

 // protected Logger log = LoggerFactory.getLogger(getClass());

  public enum ConnectionPhase {
    NORMAL, CONNECTING, DISCONNECTING, UNKNOWN
  }

  public static ConnectionPhase getConnectionPhase(HttpServletRequest request) {
    if (request.getHeader("phase") == null) return ConnectionPhase.NORMAL;
    else {
      String phase = request.getHeader("phase");
      if ("connection".equals(phase)) {
        return ConnectionPhase.CONNECTING;
      }
      else if ("disconnect".equals(phase)) {
        return ConnectionPhase.DISCONNECTING;
      }

      return ConnectionPhase.UNKNOWN;
    }
  }


  @Override
  public void init(ServletConfig config) throws ServletException {

    final ServletContext context = config.getServletContext();
    service = (ErraiService) context.getAttribute("errai");
    if (null == service) {
      synchronized (context) {
        // Build or lookup service
        String serviceLocatorClass = config.getInitParameter("service-locator");
        if (serviceLocatorClass != null) {
          // locate externally created service instance, i.e. CDI
          try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> aClass = loader.loadClass(serviceLocatorClass);
            ServiceLocator locator = (ServiceLocator) aClass.newInstance();
            this.service = locator.locateService();
          }
          catch (Exception e) {
            throw new RuntimeException("Failed to create service", e);
          }
        }
        else {
          // create a service instance manually
          this.service = buildService();
        }

        contextClassLoader = Thread.currentThread().getContextClassLoader();

        service.getConfiguration().getResourceProviders()
                .put("errai.experimental.classLoader", new ResourceProvider<ClassLoader>() {
                  public ClassLoader get() {
                    return contextClassLoader;
                  }
                });

        service.getConfiguration().getResourceProviders()
                .put("errai.experimental.servletContext", new ResourceProvider<ServletContext>() {
                  public ServletContext get() {
                    return context;
                  }
                });

        // store it in servlet context
        config.getServletContext().setAttribute("errai", service);
      }
    }

    sessionProvider = service.getSessionProvider();
  }

  @Override
  public void destroy() {
    service.stopService();
  }

  @SuppressWarnings({"unchecked"})
  protected ErraiService<HttpSession> buildService() {
    return (ErraiService<HttpSession>) Guice.createInjector(new AbstractModule() {
      @SuppressWarnings({"unchecked"})
      public void configure() {
        bind(ErraiService.class).to(ErraiServiceImpl.class);
        bind(ErraiServiceConfigurator.class).to(ErraiServiceConfiguratorImpl.class);
        bind(MessageBus.class).to(ServerMessageBusImpl.class);
        bind(ServerMessageBus.class).to(ServerMessageBusImpl.class);
        //  bind(new TypeLiteral<ErraiService<HttpSession>>() {}).to(new TypeLiteral<ErraiServiceImpl<HttpSession>>() {});

      }
    }).getInstance(ErraiService.class);
  }


  /**
   * Writes the message to the output stream
   *
   * @param stream - the stream to write to
   * @param m      - the message to write to the stream
   * @throws java.io.IOException - is thrown if any input/output errors occur while writing to the stream
   */
  public static void writeToOutputStream(OutputStream stream, MarshalledMessage m) throws IOException {
    stream.write('[');

    if (m.getMessage() == null) {
      stream.write('n');
      stream.write('u');
      stream.write('l');
      stream.write('l');
    }
    else {
      for (byte b : ((String) m.getMessage()).getBytes()) {
        stream.write(b);
      }
    }
    stream.write(']');

  }


  protected void writeExceptionToOutputStream(HttpServletResponse httpServletResponse
          , final
  Throwable t) throws IOException {
    httpServletResponse.setHeader("Cache-Control", "no-cache");
    httpServletResponse.addHeader("Payload-Size", "1");
    httpServletResponse.setContentType("application/json");
    OutputStream stream = httpServletResponse.getOutputStream();

    stream.write('[');

    writeToOutputStream(stream, new MarshalledMessage() {
      public String getSubject() {
        return DefaultErrorCallback.CLIENT_ERROR_SUBJECT;
      }

      public Object getMessage() {
        StringBuilder b = new StringBuilder("{\"ErrorMessage\":\"").append(t.getMessage()).append("\"," +
                "\"AdditionalDetails\":\"");
        for (StackTraceElement e : t.getStackTrace()) {
          b.append(e.toString()).append("<br/>");
        }

        return b.append("\"}").toString();
      }
    });

    stream.write(']');
    stream.close();
  }

  protected void sendDisconnectWithReason(OutputStream stream, final String reason) throws IOException {
    writeToOutputStream(stream, new MarshalledMessage() {
      public String getSubject() {
        return "ClientBus";
      }

      public Object getMessage() {
        return reason != null ? "{\"ToSubject\":\"ClientBus\", \"CommandType\":\"" + BusCommands.Disconnect + "\"," +
                "\"Reason\":\"" + reason + "\"}"
                : "{\"CommandType\":\"" + BusCommands.Disconnect + "\"}";
      }
    });
  }


  protected void sendDisconnectDueToSessionExpiry(OutputStream stream) throws IOException {
    writeToOutputStream(stream, new MarshalledMessage() {
      public String getSubject() {
        return "ClientBus";
      }

      public Object getMessage() {
        return "{\"ToSubject\":\"ClientBus\", \"CommandType\":\"" + BusCommands.SessionExpired + "\"}";
      }
    });
  }
}
