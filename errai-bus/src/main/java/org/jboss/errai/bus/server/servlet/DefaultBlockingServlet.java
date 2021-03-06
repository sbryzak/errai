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

import static org.jboss.errai.bus.server.io.MessageFactory.createCommandMessage;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.errai.bus.client.framework.ClientMessageBus;
import org.jboss.errai.bus.server.api.MessageQueue;
import org.jboss.errai.bus.server.api.QueueSession;
import org.jboss.errai.bus.server.service.ErraiServiceConfigurator;

/**
 * The default DefaultBlockingServlet which provides the HTTP-protocol gateway between the server bus and the client buses.
 */
public class DefaultBlockingServlet extends AbstractErraiServlet {


  /**
   * Called by the server (via the <tt>service</tt> method) to allow a servlet to handle a GET request by supplying
   * a response
   *
   * @param httpServletRequest  - object that contains the request the client has made of the servlet
   * @param httpServletResponse - object that contains the response the servlet sends to the client
   * @throws IOException      - if an input or output error is detected when the servlet handles the GET request
   * @throws ServletException - if the request for the GET could not be handled
   */
  @Override
  protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
          throws ServletException, IOException {
    pollForMessages(sessionProvider.getSession(httpServletRequest.getSession(),
            httpServletRequest.getHeader(ClientMessageBus.REMOTE_QUEUE_ID_HEADER)),
            httpServletRequest, httpServletResponse, ErraiServiceConfigurator.LONG_POLLING);
  }

  /**
   * Called by the server (via the <code>service</code> method) to allow a servlet to handle a POST request, by
   * sending the request
   *
   * @param httpServletRequest  - object that contains the request the client has made of the servlet
   * @param httpServletResponse - object that contains the response the servlet sends to the client
   * @throws IOException      - if an input or output error is detected when the servlet handles the request
   * @throws ServletException - if the request for the POST could not be handled
   */
  @Override
  protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
          throws ServletException, IOException {
    final QueueSession session = sessionProvider.getSession(httpServletRequest.getSession(),
            httpServletRequest.getHeader(ClientMessageBus.REMOTE_QUEUE_ID_HEADER));

    service.store(createCommandMessage(session, httpServletRequest));

    pollForMessages(session, httpServletRequest, httpServletResponse, false);
  }

  private void pollForMessages(QueueSession session, HttpServletRequest httpServletRequest,
                               HttpServletResponse httpServletResponse, boolean wait) throws IOException {
    try {
      // note about caching: clients now include a uniquifier in a request parameter called "z"
      // so no-cache headers are now unnecessary.
      httpServletResponse.setContentType("application/json");

      final MessageQueue queue = service.getBus().getQueue(session);

      ServletOutputStream outputStream = httpServletResponse.getOutputStream();
      if (queue == null) {
        switch (getConnectionPhase(httpServletRequest)) {
          case CONNECTING:
          case DISCONNECTING:
            return;
        }

        sendDisconnectDueToSessionExpiry(outputStream);
        return;
      }

      queue.heartBeat();

      queue.poll(wait, outputStream);

      outputStream.close();
    }
    catch (final Throwable t) {
      t.printStackTrace();
      writeExceptionToOutputStream(httpServletResponse, t);
    }
  }
}