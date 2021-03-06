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

package org.jboss.errai.bus.client.framework;

import static org.jboss.errai.bus.client.json.JSONUtilCli.decodePayload;
import static org.jboss.errai.bus.client.protocols.BusCommands.RemoteSubscribe;
import static org.jboss.errai.common.client.protocols.MessageParts.PriorityProcessing;
import static org.jboss.errai.common.client.protocols.MessageParts.ReplyTo;
import static org.jboss.errai.common.client.protocols.MessageParts.Subject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.api.Message;
import org.jboss.errai.bus.client.api.MessageCallback;
import org.jboss.errai.bus.client.api.MessageListener;
import org.jboss.errai.bus.client.api.PreInitializationListener;
import org.jboss.errai.bus.client.api.SessionExpirationListener;
import org.jboss.errai.bus.client.api.SubscribeListener;
import org.jboss.errai.bus.client.api.UnsubscribeListener;
import org.jboss.errai.bus.client.api.base.Capabilities;
import org.jboss.errai.bus.client.api.base.DefaultErrorCallback;
import org.jboss.errai.bus.client.api.base.MessageBuilder;
import org.jboss.errai.bus.client.api.base.NoSubscribersToDeliverTo;
import org.jboss.errai.bus.client.api.base.TransportIOException;
import org.jboss.errai.bus.client.json.JSONUtilCli;
import org.jboss.errai.bus.client.protocols.BusCommands;
import org.jboss.errai.bus.client.util.BusTools;
import org.jboss.errai.common.client.framework.Assert;
import org.jboss.errai.common.client.protocols.MessageParts;
import org.jboss.errai.marshalling.client.api.MarshallerFramework;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * The default client <tt>MessageBus</tt> implementation.  This bus runs in the browser and automatically federates
 * with the server immediately upon initialization.
 *
 * @author Mike Brock
 */
public class ClientMessageBusImpl implements ClientMessageBus {
  private String clientId;
  private String sessionId;

  /* The encoded URL to be used for the bus */
  String OUT_SERVICE_ENTRY_POINT = "out.erraiBus";
  String IN_SERVICE_ENTRY_POINT = "in.erraiBus";

  /* ArrayList of all subscription listeners */
  private List<SubscribeListener> onSubscribeHooks;

  /* ArrayList of all unsubscription listeners */
  private List<UnsubscribeListener> onUnsubscribeHooks;

  /* Used to build the HTTP POST request */
  private RequestBuilder sendBuilder;

  private volatile boolean cometChannelOpen = true;
  private volatile boolean webSocketOpen = false;
  private String webSocketUrl;
  private String webSocketToken;
  private Object webSocketChannel;

  public final MessageCallback remoteCallback = new RemoteMessageCallback();

  private RequestCallback receiveCommCallback = new NoPollRequestCallback();

  /* Map of subjects to subscriptions  */
//  private Map<String, List<Object>> subscriptions;

  // TODO remove "shadow" from everything in this class that has "shadow" in its name
  private Map<String, List<MessageCallback>> shadowSubscriptions =
          new HashMap<String, List<MessageCallback>>();

  private Map<String, MessageCallback> remotes;

  private List<SessionExpirationListener> sessionExpirationListeners
          = new ArrayList<SessionExpirationListener>();

  private List<PreInitializationListener> preInitializationListeners
          = new ArrayList<PreInitializationListener>();

  /* Map of subjects to references registered in this session */
  private Map<String, List<Object>> registeredInThisSession = new HashMap<String, List<Object>>();

  /* A list of {@link Runnable} initialization tasks to be executed after the bus has successfully finished it's
* initialization and is now communicating with the remote bus. */
  private List<Runnable> postInitTasks = new ArrayList<Runnable>();
  private List<Message> deferredMessages = new ArrayList<Message>();

  /* True if the client's message bus has been initialized */
  private boolean initialized = false;
  private boolean reinit = false;
  private boolean postInit = false;

  /* Default is 2 -- one for the RPC proxies,  one for the server connection  */
  private int initVotesRequired = 2;

  /**
   * The unique ID that will sent with the next request.
   * <p/>
   * IMPORTANT: only access this member via {@link #getNextRequestNumber()}.
   */
  private int txNumber = 0;
  private int rxNumber = 0;


  private boolean disconnected = false;

  static {
    MarshallerFramework.initializeDefaultSessionProvider();
  }

  private List<MessageInterceptor> interceptorStack = new LinkedList<MessageInterceptor>();

  private LogAdapter logAdapter = new LogAdapter() {
    @Override
    public void warn(String message) {
      GWT.log("WARN: " + message, null);
    }

    @Override
    public void info(String message) {
      GWT.log("INFO: " + message, null);
    }

    @Override
    public void debug(String message) {
      GWT.log("DEBUG: " + message, null);
    }

    @Override
    public void error(String message, Throwable t) {
      showError(message, t);
    }
  };

  private BusErrorDialog errorDialog = new BusErrorDialog();

  public ClientMessageBusImpl() {
    init();
  }

  private void createRequestBuilders() {
    sendBuilder = getSendBuilder();

    logAdapter.debug("Connecting Errai at URL " + sendBuilder.getUrl());
  }

  private RequestBuilder getSendBuilder() {
    String endpoint = OUT_SERVICE_ENTRY_POINT;

    RequestBuilder builder = new RequestBuilder(
            RequestBuilder.POST,
            URL.encode(endpoint) + "?z=" + getNextRequestNumber()
    );

    builder.setHeader("Content-Type", "application/json");
    builder.setHeader(ClientMessageBus.REMOTE_QUEUE_ID_HEADER, clientId);

    return builder;
  }

  private RequestBuilder getRecvBuilder() {
    String endpoint = IN_SERVICE_ENTRY_POINT;

    RequestBuilder builder = new RequestBuilder(
            RequestBuilder.GET,
            URL.encode(endpoint) + "?z=" + getNextRequestNumber()
    );

    builder.setHeader("Content-Type", "application/json");
    builder.setHeader(ClientMessageBus.REMOTE_QUEUE_ID_HEADER, clientId);
    return builder;
  }

  /**
   * Removes all subscriptions attached to the specified subject
   *
   * @param subject - the subject to have all it's subscriptions removed
   */
  @Override
  public void unsubscribeAll(String subject) {
    fireAllUnSubscribeListeners(subject);
    removeShadowSubscription(subject);
  }

  /**
   * Add a subscription for the specified subject
   *
   * @param subject  - the subject to add a subscription for
   * @param callback - function called when the message is dispatched
   */
  @Override
  public Subscription subscribe(final String subject, final MessageCallback callback) {
    return _subscribe(subject, callback, false);
  }

  @Override
  public Subscription subscribeLocal(final String subject, final MessageCallback callback) {
    return _subscribe(subject, callback, true);
  }

  private Subscription _subscribe(final String subject, final MessageCallback callback, final boolean local) {
    if ("ServerBus".equals(subject) && shadowSubscriptions.containsKey("ServerBus")) return null;

    if (!postInit) {
      final DeferredSubscription detachedSubscripton = new DeferredSubscription();

      postInitTasks.add(new Runnable() {
        @Override
        public void run() {
          detachedSubscripton.attachSubscription(_subscribe(subject, callback, local));
        }
      });

      return detachedSubscripton;
    }

    fireAllSubscribeListeners(subject, local, directSubscribe(subject, callback));

    return new Subscription() {
      @Override
      public void remove() {
        List<MessageCallback> cbs = shadowSubscriptions.get(subject);
        if (cbs != null) {
          cbs.remove(callback);
        }
      }
    };
  }


  private boolean directSubscribe(final String subject, final MessageCallback callback) {
    boolean isNew = !isSubscribed(subject);

    addShadowSubscription(subject, new MessageCallback() {
      @Override
      public void callback(Message message) {
        try {
          executeInterceptorStack(true, message);
          callback.callback(message);
        }
        catch (Exception e) {
          logError("receiver '" + subject + "' threw an exception", decodeCommandMessage(message), e);
        }
      }
    });

    return isNew;
  }

  /**
   * Fire listeners to notify that a new subscription has been registered on the bus.
   *
   * @param subject - new subscription registered
   * @param local   -
   * @param isNew   -
   */
  private void fireAllSubscribeListeners(String subject, boolean local, boolean isNew) {
    Iterator<SubscribeListener> iter = onSubscribeHooks.iterator();
    SubscriptionEvent evt = new SubscriptionEvent(false, false, local, isNew, 1, "InBrowser", subject);

    while (iter.hasNext()) {
      iter.next().onSubscribe(evt);

      if (evt.isDisposeListener()) {
        iter.remove();
        evt.setDisposeListener(false);
      }
    }
  }

  /**
   * Fire listeners to notify that a subscription has been unregistered from the bus
   *
   * @param subject - subscription unregistered
   */
  private void fireAllUnSubscribeListeners(String subject) {
    Iterator<UnsubscribeListener> iter = onUnsubscribeHooks.iterator();
    SubscriptionEvent evt = new SubscriptionEvent(false, "InBrowser", 0, false, subject);

    while (iter.hasNext()) {
      iter.next().onUnsubscribe(evt);
      if (evt.isDisposeListener()) {
        iter.remove();
        evt.setDisposeListener(false);
      }
    }
  }

  private static int conversationCounter = 0;

  /**
   * Have a single two-way conversation
   *
   * @param message  - The message to be sent in the conversation
   * @param callback - The function to be called when the message is received
   */
  @Override
  public void conversationWith(final Message message, final MessageCallback callback) {
    final String tempSubject = "temp:Reply:" + (++conversationCounter);

    message.set(ReplyTo, tempSubject);

    subscribe(tempSubject, new MessageCallback() {
      @Override
      public void callback(Message message) {
        unsubscribeAll(tempSubject);
        callback.callback(message);
      }
    });

    send(message);
  }

  /**
   * Globally send message to all receivers.
   *
   * @param message - The message to be sent.
   */
  @Override
  public void sendGlobal(Message message) {
    send(message);
  }

  /**
   * Sends the specified message, and notifies the listeners.
   *
   * @param message       - the message to be sent
   * @param fireListeners - true if the appropriate listeners should be fired
   */
  @Override
  public void send(Message message, boolean fireListeners) {
    // TODO: fire listeners?
    send(message);
  }

  /**
   * Sends the message using it's encoded subject. If the bus has not been initialized, it will be added to
   * <tt>postInitTasks</tt>.
   *
   * @param message -
   * @throws RuntimeException - if message does not contain a ToSubject field or if the message's callback throws
   *                          an error.
   */
  @Override
  public void send(final Message message) {
    executeInterceptorStack(false, message);

    message.commit();
    try {
      if (message.hasPart(MessageParts.ToSubject)) {
        if (!initialized) {
          deferredMessages.add(message);
        }
        else {
          if (!hasListeners(message.getSubject())) {
            throw new NoSubscribersToDeliverTo(message.getSubject());
          }

          directStore(message);
        }
      }
      else {
        throw new RuntimeException("Cannot send message using this method" +
                " if the message does not contain a ToSubject field.");
      }
    }
    catch (RuntimeException e) {
      callErrorHandler(message, e);
      throw e;
    }
  }

  private boolean hasListeners(String subject) {
    return shadowSubscriptions.containsKey(subject) || remotes.containsKey(subject);
  }

  private void callErrorHandler(final Message message, final Throwable t) {
    if (message.getErrorCallback() != null) {
      message.getErrorCallback().error(message, t);
    }
    logError(t.getMessage(), "none", t);
  }

  private void directStore(final Message message) {
    String subject = message.getSubject();

    if (remotes.containsKey(subject)) {
      remotes.get(subject).callback(message);
    }
    else if (shadowSubscriptions.containsKey(subject)) {
      deliverToShadowSubscriptions(subject, message);
    }
    else {
      throw new NoSubscribersToDeliverTo(subject);
    }
  }


  /**
   * Add message to the queue that remotely transmits messages to the server.
   * All messages in the queue are then sent.
   *
   * @param message -
   */
  private void encodeAndTransmit(Message message) {
    transmitRemote(BusTools.encodeMessage(message), message);
  }

  private void addShadowSubscription(String subject, MessageCallback reference) {
    if (!shadowSubscriptions.containsKey(subject)) {
      shadowSubscriptions.put(subject, new ArrayList<MessageCallback>());
    }

    if (!shadowSubscriptions.get(subject).contains(reference)) {
      shadowSubscriptions.get(subject).add(reference);
    }
  }

  private void removeShadowSubscription(String subject) {
    shadowSubscriptions.remove(subject);
  }

  // TODO delete this method
  private void resubscribeShadowSubcriptions() {
    for (Map.Entry<String, List<MessageCallback>> entry : shadowSubscriptions.entrySet()) {
      for (MessageCallback callback : entry.getValue()) {
        _subscribe(entry.getKey(), callback, false);
      }
    }
  }

  private void deliverToShadowSubscriptions(String subject, Message message) {
    for (MessageCallback cb : shadowSubscriptions.get(subject)) {
      cb.callback(message);
    }
  }

  /**
   * Checks if subject is already listed in the subscriptions map
   *
   * @param subject - subject to look for
   * @return true if the subject is already subscribed
   */
  @Override
  public boolean isSubscribed(String subject) {
    return shadowSubscriptions.containsKey(subject);
  }

  /**
   * Retrieve all registrations that have occured during the current capture context.
   * <p/>
   * The Map returned has the subject of the registrations as the key, and Sets of registration objects as the
   * value of the Map.
   *
   * @return A map of registrations captured in the current capture context.
   */
  @Override
  public Map<String, List<Object>> getCapturedRegistrations() {
    return registeredInThisSession;
  }

  /**
   * Marks the beginning of a new capture context.<p/>  From this point, the message is called forward, all
   * registration events which occur will be captured.
   */
  @Override
  public void beginCapture() {
    registeredInThisSession = new HashMap<String, List<Object>>();
  }

  /**
   * End the current capturing context.
   */
  @Override
  public void endCapture() {
    registeredInThisSession = null;
  }

  /**
   * Transmits JSON string containing message, using the <tt>sendBuilder</tt>
   *
   * @param message   - JSON string representation of message
   * @param txMessage - Message reference.
   */
  private void transmitRemote(final String message, final Message txMessage) {
    if (message == null) return;
    //System.out.println("TX: " + message);

    if (webSocketOpen) {
      if (ClientWebSocketChannel.transmitToSocket(webSocketChannel, message)) {
        return;
      }
      else {
        log("websocket channel is closed. falling back to comet");

        //disconnected.
        webSocketOpen = false;
        webSocketChannel = null;
        cometChannelOpen = true;

        if (receiveCommCallback instanceof LongPollRequestCallback) {
          ((LongPollRequestCallback) receiveCommCallback).schedule();
        }
      }
    }

    try {
      sendBuilder.sendRequest(message, new RequestCallback() {

        @Override
        public void onResponseReceived(Request request, Response response) {
          switch (response.getStatusCode()) {
            case 1:
            case 404:
            case 408:
            case 502:
            case 503:
            case 504: {
              // Sending the message failed.
              // Although the response may still be valid
              // Handle it gracefully
              //noinspection ThrowableInstanceNeverThrown

              TransportIOException tioe = new TransportIOException(response.getText(), response.getStatusCode(),
                      "Failure communicating with server");

              callErrorHandler(txMessage, tioe);
              return;
            }
          }

          /**
           * If the server bus returned us some client-destined messages
           * in response to our send, handle them now.
           */
          try {
            procIncomingPayload(response);
          }
          catch (Throwable e) {
            callErrorHandler(txMessage, e);
          }
        }

        @Override
        public void onError(Request request, Throwable exception) {
          if (txMessage.getErrorCallback() == null || txMessage.getErrorCallback().error(txMessage, exception)) {
            logError("Failed to communicate with remote bus", "", exception);
          }
        }
      });
    }
    catch (Exception e) {
      callErrorHandler(txMessage, e);
    }
  }

  boolean pollActive = false;

  private void performPoll() {
    try {
      if (pollActive || !cometChannelOpen) return;
      pollActive = true;
      getRecvBuilder().sendRequest(null, receiveCommCallback);
    }
    catch (RequestTimeoutException e) {
      statusCode = 1;
      receiveCommCallback.onError(null, e);
    }
    catch (Throwable t) {
      DefaultErrorCallback.INSTANCE.error(null, t);
    }
    finally {
      pollActive = false;
    }
  }

  @Override
  public void voteForInit() {
    if (--initVotesRequired == 0) {
      log("received final vote for initialization ...");

      postInit = true;
      log("executing " + postInitTasks.size() + " post init task(s)");

      Iterator<Runnable> postInitTasksIter = postInitTasks.iterator();
      while (postInitTasksIter.hasNext()) {
        postInitTasksIter.next().run();
        postInitTasksIter.remove();
      }

//      for (Runnable postInitTask : postInitTasks) {
//        try {
//          postInitTask.run();
//        }
//        catch (Throwable t) {
//          t.printStackTrace();
//          throw new RuntimeException("error running task", t);
//        }
//      }

      sendAllDeferred();
      postInitTasks.clear();

      setInitialized(true);

      log("bus federation complete. now operating normally.");
    }
    else {
      log("waiting for " + initVotesRequired + " more votes to initialize");
    }
  }

  @Override
  public void stop(boolean sendDisconnect) {
    try {
      if (sendDisconnect) {
        sendBuilder.setHeader("phase", "disconnect");

        Message m = MessageBuilder.createMessage()
                .toSubject("ServerBus")
                .command(BusCommands.Disconnect).getMessage();

        encodeAndTransmit(m);
      }

      unsubscribeAll("ClientBus");
      shadowSubscriptions.clear();
    }
    finally {
      this.remotes.clear();
      this.disconnected = true;
      this.initialized = false;
      this.sendBuilder = null;
      this.initVotesRequired = 1; // just to reconnect the bus.
      this.postInitTasks.clear();
    }
  }

  public class RemoteMessageCallback implements MessageCallback {
    @Override
    public void callback(Message message) {
      encodeAndTransmit(message);
    }
  }

  private void initFields() {
    initialized = false;
    disconnected = false;

    clientId = String.valueOf(com.google.gwt.user.client.Random.nextInt(99999))
            + "-" + (System.currentTimeMillis() % com.google.gwt.user.client.Random.nextInt(99999));

    IN_SERVICE_ENTRY_POINT = "in." + clientId + ".erraiBus";
    OUT_SERVICE_ENTRY_POINT = "out." + clientId + ".erraiBus";

    onSubscribeHooks = new ArrayList<SubscribeListener>();
    onUnsubscribeHooks = new ArrayList<UnsubscribeListener>();
    //subscriptions = new HashMap<String, List<Object>>();
    remotes = new HashMap<String, MessageCallback>();
  }

  public void setInitialized(boolean initialized) {
    this.initialized = initialized;
  }

  public boolean isReinit() {
    return this.reinit;
  }

  private void setReinit(boolean reinit) {
    this.reinit = reinit;
  }

  /**
   * Initializes the message bus, by subscribing to the ClientBus (to receive subscription messages) and the
   * ClientErrorBus to dispatch errors when called.
   */
  @Override
  public void init() {
    declareDebugFunction();

    if (sendBuilder == null) {
      if (!GWT.isScript()) {   // Hosted Mode
        initFields();
        createRequestBuilders();

        if (isReinit()) {
          setReinit(true);
          init();
          setReinit(false);
          return;
        }
        else {
          init();
          return;
        }
      }
      else {
        initFields();
        createRequestBuilders();
      }
    }

    if (sendBuilder == null) {
      return;
    }

    if (reinit) {
      resubscribeShadowSubcriptions();
    }

    /**
     * Fire initialization listeners now.
     */
    for (PreInitializationListener listener : preInitializationListeners) {
      listener.beforeInitialization();
    }

    remoteSubscribe("ServerEchoService");
    directSubscribe("ClientBus", new MessageCallback() {
      @Override
      @SuppressWarnings({"unchecked"})
      public void callback(final Message message) {
        switch (BusCommands.valueOf(message.getCommandType())) {
          case RemoteSubscribe:
            if (message.hasPart("SubjectsList")) {
              for (String subject : (List<String>) message.get(List.class, "SubjectsList")) {
                remoteSubscribe(subject);
              }
            }
            else {
              String subject = message.get(String.class, Subject);
              remoteSubscribe(subject);
            }
            break;

          case RemoteUnsubscribe:
            unsubscribeAll(message.get(String.class, Subject));
            break;

          case CapabilitiesNotice:
            log("received capabilities notice from server. supported capabilities of remote: "
                    + message.get(String.class, "Flags"));

            String[] capabilites = message.get(String.class, "Flags").split(",");

            for (String capability : capabilites) {
              switch (Capabilities.valueOf(capability)) {
                case WebSockets:
                  webSocketUrl = message.get(String.class, MessageParts.WebSocketURL);
                  webSocketToken = message.get(String.class, MessageParts.WebSocketToken);

                  log("attempting web sockets connection at URL: " + webSocketUrl);

                  Object o = ClientWebSocketChannel.attemptWebSocketConnect(ClientMessageBusImpl.this, webSocketUrl);

                  if (o instanceof String) {
                    log("could not use web sockets. reason: " + o);
                  }
                  break;
                case LongPollAvailable:

                  log("initializing long poll subsystem");
                  receiveCommCallback = new LongPollRequestCallback();
                  break;
                case NoLongPollAvailable:
                  receiveCommCallback = new ShortPollRequestCallback();
                  if (message.hasPart("PollFrequency")) {
                    POLL_FREQUENCY = message.get(Integer.class, "PollFrequency");
                  }
                  else {
                    POLL_FREQUENCY = 500;
                  }
                  break;
              }
            }

            break;

          case RemoteMonitorAttach:
            break;

          case FinishStateSync:
            if (isInitialized()) {
              return;
            }

            log("received FinishStateSync message. preparing to bring up the federation");

            List<String> subjects = new ArrayList<String>();
            for (String s : shadowSubscriptions.keySet()) {
              if (s.startsWith("local:")) continue;
              if (!remotes.containsKey(s)) subjects.add(s);
            }

            sessionId = message.get(String.class, MessageParts.ConnectionSessionKey);

            remoteSubscribe("ServerBus");

            MessageBuilder.createMessage()
                    .toSubject("ServerBus")
                    .command(RemoteSubscribe)
                    .with("SubjectsList", subjects)
                    .with(PriorityProcessing, "1")
                    .noErrorHandling()
                    .sendNowWith(ClientMessageBusImpl.this);


            MessageBuilder.createMessage()
                    .toSubject("ServerBus")
                    .command(BusCommands.FinishStateSync)
                    .with(PriorityProcessing, "1")
                    .noErrorHandling().sendNowWith(ClientMessageBusImpl.this);

            /**
             * ... also send RemoteUnsubscribe signals.
             */

            addSubscribeListener(new SubscribeListener() {
              @Override
              public void onSubscribe(SubscriptionEvent event) {
                if (event.isLocalOnly() || event.getSubject().startsWith("local:")
                        || remotes.containsKey(event.getSubject())) {
                  return;
                }

                if (event.isNew()) {
                  MessageBuilder.getMessageProvider().get().command(RemoteSubscribe)
                          .toSubject("ServerBus")
                          .set(Subject, event.getSubject())
                          .set(PriorityProcessing, "1")
                          .sendNowWith(ClientMessageBusImpl.this);
                }
              }
            });

            addUnsubscribeListener(new UnsubscribeListener() {
              @Override
              public void onUnsubscribe(SubscriptionEvent event) {
                MessageBuilder.getMessageProvider().get().command(BusCommands.RemoteUnsubscribe)
                        .toSubject("ServerBus")
                        .set(Subject, event.getSubject())
                        .set(PriorityProcessing, "1")
                        .sendNowWith(ClientMessageBusImpl.this);
              }
            });

            subscribe(DefaultErrorCallback.CLIENT_ERROR_SUBJECT, new MessageCallback() {
              @Override
              public void callback(Message message) {
                String errorTo = message.get(String.class, MessageParts.ErrorTo);
                if (errorTo == null) {
                  logError(message.get(String.class, "ErrorMessage"),
                          message.get(String.class, "AdditionalDetails"), null);
                }
                else {
                  message.toSubject(errorTo);
                  message.sendNowWith(ClientMessageBusImpl.this);
                }
              }
            });

            voteForInit();
            break;

          case SessionExpired:
            if (isReinit()) {
              showError("Session was terminated and could not be re-established", null);
              return;
            }

            if (!isInitialized()) return;

            for (SessionExpirationListener listener : sessionExpirationListeners) {
              listener.onSessionExpire();
            }

            stop(false);

            setReinit(true);

            init();
            setReinit(false);

            break;

          case WebsocketChannelVerify:
            log("received verification token for websocket connection");
            MessageBuilder.createMessage()
                    .toSubject("ServerBus")
                    .command(BusCommands.WebsocketChannelVerify)
                    .copy(MessageParts.WebSocketToken, message)
                    .done().sendNowWith(ClientMessageBusImpl.this);
            break;

          case WebsocketChannelOpen:

            cometChannelOpen = false;
            webSocketOpen = true;

            // send final message to open the channel
            ClientWebSocketChannel.transmitToSocket(webSocketChannel, getWebSocketNegotiationString());

            log("web socket channel successfully negotiated. comet channel deactivated.");

            break;

          case WebsocketNegotiationFailed:
            webSocketChannel = null;
            logError("failed to connect to websocket: server rejected request",
                    message.get(String.class, MessageParts.ErrorMessage), null);
            break;

          case Disconnect:
            stop(false);

            if (message.hasPart("Reason")) {
              logError("The bus was disconnected by the server", "Reason: "
                      + message.get(String.class, "Reason"), null);
            }
            break;
        }
      }
    });

    /**
     * Send initial message to connect to the queue, to establish an HTTP session. Otherwise, concurrent
     * requests will result in multiple sessions being created.  Which is bad.  Avoid this at all costs.
     * Please.
     */
    if (!sendInitialMessage()) {
      logError("Could not connect to remote bus", "", null);
    }
  }

  private void remoteSubscribe(String subject) {
    remotes.put(subject, remoteCallback);
    //  addShadowSubscription(subject, remoteCallback);
  }

  private void sendAllDeferred() {
    log("transmitting deferred messages now ...");

    for (Iterator<Message> iter = deferredMessages.iterator(); iter.hasNext(); ) {
      Message m = iter.next();
      if (m.hasPart(MessageParts.PriorityProcessing)) {
        directStore(m);
        iter.remove();
      }
    }

    for (Iterator<Message> iter = deferredMessages.iterator(); iter.hasNext(); ) {
      directStore(iter.next());
      iter.remove();
    }
  }

  /**
   * Sends the initial message to connect to the queue, to estabish an HTTP session. Otherwise, concurrent
   * requests will result in multiple sessions being created.
   *
   * @return true if initial message was sent successfully.
   */
  private boolean sendInitialMessage() {
    try {

      log("sending initial handshake to remote bus");

      String initialMessage = "{\"CommandType\":\"ConnectToQueue\",\"ToSubject\":\"ServerBus\"," +
              " \"PriorityProcessing\":\"1\"}";

      RequestBuilder initialRequest = getSendBuilder();
      initialRequest.setHeader("phase", "connection");

      initialRequest.sendRequest(initialMessage, new RequestCallback() {
        @Override
        public void onResponseReceived(Request request, Response response) {
          try {
            log("received response from initial handshake.");
            procIncomingPayload(response);
            initializeMessagingBus();
          }
          catch (Exception e) {
            e.printStackTrace();
            logError("Error attaching to bus", e.getMessage() + "<br/>Message Contents:<br/>"
                    + response.getText(), e);
          }
        }

        @Override
        public void onError(Request request, Throwable exception) {
          logError("Could not connect to remote bus", "", exception);
        }
      });
    }
    catch (RequestException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  /**
   * Returns true if client message bus is initialized.
   *
   * @return true if client message bus is initialized.
   */
  @Override
  public boolean isInitialized() {
    return this.initialized;
  }

  int maxRetries = 5;
  int retries = 0;
  int timeout = 2000;
  int statusCode = 0;
  DialogBox timeoutDB;
  Label timeoutMessage;

  private void createConnectAttemptGUI() {
    timeoutDB = new DialogBox();
    timeoutMessage = new Label();
    timeoutDB.add(timeoutMessage);
    RootPanel.get().add(timeoutDB);
    timeoutDB.show();
    timeoutDB.center();
  }

  private void clearConnectAttemptGUI() {
    timeoutDB.hide();
    RootPanel.get().remove(timeoutDB);
    timeoutDB = null;
    timeoutMessage = null;
    retries = 0;
  }

  protected class LongPollRequestCallback implements RequestCallback {
    @Override
    public void onError(Request request, Throwable throwable) {
      switch (statusCode) {
        case 1:
        case 408:
        case 502:
        case 504:
          if (retries != maxRetries) {
            if (timeoutDB == null) {
              createConnectAttemptGUI();
            }

            logAdapter.warn("Attempting reconnection -- Retries: " + (maxRetries - retries));
            timeoutMessage.setText("Connection Interrupted -- Retries: " + (maxRetries - retries));
            retries++;
            new Timer() {
              @Override
              public void run() {
                performPoll();
              }
            }.schedule(timeout);

            statusCode = 0;
            return;
          }
          else {
            timeoutMessage.setText("Connection re-attempt failed!");
          }
      }

      DefaultErrorCallback.INSTANCE.error(null, throwable);
    }

    @Override
    public void onResponseReceived(Request request, Response response) {
      if (response.getStatusCode() != 200) {
        switch (statusCode = response.getStatusCode()) {
          case 200:
          case 300:
          case 301:
          case 302:
          case 303:
          case 304:
          case 305:
          case 307:
            break;
          default:
            onError(request, new TransportIOException("Unexpected response code", statusCode, response.getStatusText()));
            return;
        }
      }

      if (retries != 0) {
        clearConnectAttemptGUI();
      }

      try {
        procIncomingPayload(response);
        schedule();
      }
      catch (Throwable e) {
        logError("Errai MessageBus Disconnected Due to Fatal Error",
                response.getText(), e);
      }
    }

    public void schedule() {
      if (!cometChannelOpen) return;
      new Timer() {
        @Override
        public void run() {
          performPoll();
        }
      }.schedule(25);
    }
  }

  protected class NoPollRequestCallback extends LongPollRequestCallback {
    @Override
    public void schedule() {
      performPoll();
    }
  }

  protected class ShortPollRequestCallback extends LongPollRequestCallback {
    public ShortPollRequestCallback() {
    }

    @Override
    public void schedule() {
      new Timer() {
        @Override
        public void run() {
          performPoll();
        }
      }.schedule(POLL_FREQUENCY);
    }
  }

  public static int POLL_FREQUENCY = 250;

  /**
   * Initializes the message bus by setting up the <tt>recvBuilder</tt> to accept responses. Also, initializes the
   * incoming timer to ensure the client's polling with the server is active.
   */
  private void initializeMessagingBus() {
    if (disconnected) {
      return;
    }

    RpcProxyLoader loader = GWT.create(RpcProxyLoader.class);
    loader.loadProxies(ClientMessageBusImpl.this);

    final Timer initialPollTimer = new Timer() {
      @Override
      public void run() {
        performPoll();
      }
    };

    initialPollTimer.schedule(10);

    voteForInit();
  }

  /**
   * Add runnable tasks to be run after the message bus is initialized.
   *
   * @param run a {@link Runnable} task.
   */
  @Override
  public void addPostInitTask(Runnable run) {
    if (isInitialized()) {
      run.run();
      return;
    }
    postInitTasks.add(run);
  }

  @Override
  public void addSessionExpirationListener(SessionExpirationListener listener) {
    sessionExpirationListeners.add(Assert.notNull(listener));
  }

  @Override
  public void addPreInitializationListener(PreInitializationListener listener) {
    preInitializationListeners.add(Assert.notNull(listener));
  }

  /**
   * Do-nothing function, should eventually be able to add a global listener to receive all messages. Though global
   * message dispatches the message to all listeners attached.
   *
   * @param listener - listener to accept all messages dispatched
   */
  @Override
  public void addGlobalListener(MessageListener listener) {
  }

  /**
   * Adds a subscription listener, so it is possible to add subscriptions to the client.
   *
   * @param listener - subscription listener
   */
  @Override
  public void addSubscribeListener(SubscribeListener listener) {
    this.onSubscribeHooks.add(Assert.notNull(listener));
  }

  /**
   * Adds an unsubscription listener, so it is possible for applications to remove subscriptions from the client
   *
   * @param listener - unsubscription listener
   */
  @Override
  public void addUnsubscribeListener(UnsubscribeListener listener) {
    this.onUnsubscribeHooks.add(listener);
  }

  private static String decodeCommandMessage(Message msg) {
    StringBuilder decode = new StringBuilder(
            "<table><thead style='font-weight:bold;'><tr><td>Field</td><td>Value</td></tr></thead><tbody>");

    for (Map.Entry<String, Object> entry : msg.getParts().entrySet()) {
      decode.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>");
    }

    return decode.append("</tbody></table>").toString();
  }


  private void logError(String message, String additionalDetails, Throwable e) {
    logAdapter.error(message + " -- Additional Details: " + additionalDetails, e);
  }

  private void showError(String message, Throwable e) {
    errorDialog.addError(message, "", e);

    if (isNativeJavaScriptLoggerSupported()) {
      nativeLog(message);
    }
  }

  /**
   * Process the incoming payload and push all the incoming messages onto the bus.
   *
   * @param response -
   * @throws Exception -
   */
  private void procIncomingPayload(Response response) throws Exception {
    procPayload(response.getText());
  }

  public void procPayload(String text) {
    // System.out.println("RX:" + text);
    try {
      for (MarshalledMessage m : decodePayload(text)) {
        rxNumber++;
        _store(m.getSubject(), JSONUtilCli.decodeCommandMessage(m.getMessage()));
      }
    }
    catch (RuntimeException e) {
      e.printStackTrace();
      logError("Error delivering message into bus", text, e);
    }
  }

  @Override
  public void attachMonitor(BusMonitor monitor) {
  }

  @Override
  public void setLogAdapter(LogAdapter logAdapter) {
    this.logAdapter = logAdapter;
  }

  @Override
  public LogAdapter getLogAdapter() {
    return logAdapter;
  }

  private boolean executeInterceptorStack(boolean inbound, Message message) {
    boolean validMessage = true;
    for (MessageInterceptor intcp : interceptorStack) {
      if (inbound)
        validMessage = intcp.processInbound(message);
      else
        validMessage = intcp.processOutbound(message);

      if (!validMessage) // brute force for now
        throw new RuntimeException("Interceptor " + intcp.getClass() + " invalidates message");

    }

    return validMessage;
  }

  public void addInterceptor(MessageInterceptor interceptor) {
    interceptorStack.add(interceptor);
  }

  public void _store(String subject, Message msg) {
    if (shadowSubscriptions.containsKey(subject)) {
      for (MessageCallback cb : shadowSubscriptions.get(subject)) {
        cb.callback(msg);
      }
    }
  }

  public int getNextRequestNumber() {
    if (txNumber == Integer.MAX_VALUE) {
      txNumber = 0;
    }
    return txNumber++;
  }

  private String getWebSocketNegotiationString() {
    return "{\"" + MessageParts.CommandType.name() + "\":\"" + BusCommands.ConnectToQueue.name()
            + "\", \"" + MessageParts.ConnectionSessionKey + "\":\"" + sessionId + "\"" +
            ",\"" + MessageParts.WebSocketToken + "\":\"" + webSocketToken + "\"}";
  }

  public void attachWebSocketChannel(Object o) {
    log("web socket opened. sending negotiation message.");
    ClientWebSocketChannel.transmitToSocket(o, getWebSocketNegotiationString());
    webSocketChannel = o;
  }

  /**
   * The built-in, default error dialog.
   */
  class BusErrorDialog extends DialogBox {
    boolean showErrors = !GWT.isProdMode();
    Panel contentPanel = new AbsolutePanel();

    public BusErrorDialog() {
      setModal(true);

      VerticalPanel panel = new VerticalPanel();

      HorizontalPanel titleBar = new HorizontalPanel();
      titleBar.getElement().getStyle().setProperty("backgroundColor", "darkgrey");
      titleBar.getElement().getStyle().setWidth(100, Style.Unit.PCT);
      titleBar.getElement().getStyle().setProperty("borderBottom", "1px solid black");
      titleBar.getElement().getStyle().setProperty("marginBottom", "5px");

      Label titleBarLabel = new Label("An Error Occurred in the Bus");
      titleBarLabel.getElement().getStyle().setFontSize(10, Style.Unit.PT);
      titleBarLabel.getElement().getStyle().setFontWeight(Style.FontWeight.BOLDER);
      titleBarLabel.getElement().getStyle().setColor("white");

      titleBar.add(titleBarLabel);
      titleBar.setCellVerticalAlignment(titleBarLabel, HasVerticalAlignment.ALIGN_MIDDLE);

      HorizontalPanel buttonPanel = new HorizontalPanel();

      CheckBox showFurtherErrors = new CheckBox();
      showFurtherErrors.setValue(showErrors);
      showFurtherErrors.setText("Show further errors");
      showFurtherErrors.getElement().getStyle().setFontSize(10, Style.Unit.PT);
      showFurtherErrors.getElement().getStyle().setColor("white");

      showFurtherErrors.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
        @Override
        public void onValueChange(ValueChangeEvent<Boolean> booleanValueChangeEvent) {
          showErrors = booleanValueChangeEvent.getValue();
        }
      });

      Button disconnectFromServer = new Button("Disconnect Bus");
      disconnectFromServer.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if (Window.confirm("Are you sure you want to disconnect and de-federate the local bus from the server bus? "
                  + "This will permanently kill your session. You will need to refresh to reconnect. OK will proceed. Click "
                  + "Cancel to abort this operation")) {
            stop(true);
          }
        }
      });

      Button clearErrors = new Button("Clear Log");
      clearErrors.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          contentPanel.clear();
        }
      });

      Button closeButton = new Button("Dismiss Error");
      closeButton.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          errorDialog.hide();
        }
      });

      buttonPanel.add(showFurtherErrors);
      buttonPanel.add(disconnectFromServer);
      buttonPanel.add(clearErrors);
      buttonPanel.add(closeButton);

      buttonPanel.setCellVerticalAlignment(showFurtherErrors, HasVerticalAlignment.ALIGN_MIDDLE);

      titleBar.add(buttonPanel);
      titleBar.setCellHorizontalAlignment(buttonPanel, HasHorizontalAlignment.ALIGN_RIGHT);

      panel.add(titleBar);

      Style s = panel.getElement().getStyle();

      s.setProperty("border", "1px");
      s.setProperty("borderStyle", "solid");
      s.setProperty("borderColor", "black");
      s.setProperty("backgroundColor", "#ede0c3");

      resize();

      panel.add(contentPanel);
      add(panel);
    }

    public void addError(String message, String additionalDetails, Throwable e) {
      if (!showErrors) return;


      contentPanel.add(new HTML("<strong style='background:red;color:white;'>" + message + "</strong>"));

      StringBuilder buildTrace = new StringBuilder("<tt style=\"font-size:11px;\"><pre>");
      if (e != null) {
        e.printStackTrace();
        buildTrace.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("<br/>");
        for (StackTraceElement ste : e.getStackTrace()) {
          buildTrace.append("  ").append(ste.toString()).append("<br/>");
        }
      }
      buildTrace.append("</pre>");

      contentPanel.add(new HTML(buildTrace.toString() + "<br/><strong>Additional Details:</strong>" + additionalDetails + "</tt>"));

      if (!isShowing()) {
        resize();
        show();
        center();
        setModal(true);
      }
    }

    private void resize() {
      contentPanel.setWidth(Window.getClientWidth() * 0.90 + "px");
      contentPanel.setHeight(Window.getClientHeight() * 0.90 + "px");
      contentPanel.getElement().getStyle().setProperty("overflow", "auto");
    }
  }

  private static void log(String message) {
    if (isNativeJavaScriptLoggerSupported()) {
      nativeLog("[erraibus] " + message);
    }
  }

  private static native boolean isNativeJavaScriptLoggerSupported() /*-{
    return ((window.console != null) &&
            (window.console.firebug == null) &&
            (window.console.log != null) &&
            (typeof(window.console.log) == 'function'));
  }-*/;

  private static native void nativeLog(String message) /*-{
    window.console.log(message);
  }-*/;


  private static native void declareDebugFunction() /*-{
    $wnd.errai_status = function () {
      @org.jboss.errai.bus.client.framework.ClientMessageBusImpl::_displayStatusToLog()();
    };

    $wnd.errai_list_services = function () {
      @org.jboss.errai.bus.client.framework.ClientMessageBusImpl::_listAvailableServicesToLog()();
    };

    $wnd.errai_show_error_console = function () {
      @org.jboss.errai.bus.client.framework.ClientMessageBusImpl::_showErrorConsole()();
    }

  }-*/;

  /**
   * Debugging functions.
   */
  private static void _displayStatusToLog() {
    ClientMessageBusImpl bus = (ClientMessageBusImpl) ErraiBus.get();

    nativeLog("ErraiBus Status");
    nativeLog("------------------------------------------------");
    nativeLog("Bus State              : " + (bus.initialized ? "Online/Federated" : "Disconnected"));
    nativeLog("");
    nativeLog("Comet Channel          : " + (bus.cometChannelOpen ? "Active" : "Offline"));
    nativeLog("  Endpoint (RX)        : " + (bus.getRecvBuilder().getUrl()));
    nativeLog("  Endpoint (TX)        : " + (bus.getSendBuilder().getUrl()));
    nativeLog("");
    nativeLog("WebSocket Channel      : " + (bus.webSocketOpen ? "Active" : "Offline"));
    nativeLog("  Endpoint (RX/TX)     : " + (bus.webSocketUrl));
    nativeLog("");
    nativeLog("Total TXs              : " + (bus.txNumber));
    nativeLog("Total RXs              : " + (bus.rxNumber));
    nativeLog("");
    nativeLog("Endpoints");
    nativeLog("  Remote (total)       : " + (bus.remotes.size()));
    nativeLog("  Local (total)        : " + (bus.shadowSubscriptions.size()));
    nativeLog("------------------------------------------------");
  }

  private static void _listAvailableServicesToLog() {
    ClientMessageBusImpl bus = (ClientMessageBusImpl) ErraiBus.get();

    nativeLog("Service and Routing Table");
    nativeLog("------------------------------------------------");
    nativeLog("[REMOTES]");

    for (String remoteName : bus.remotes.keySet()) {
      nativeLog(remoteName);
    }

    nativeLog("[LOCALS]");

    for (String localName : bus.shadowSubscriptions.keySet()) {
      nativeLog(localName + " (" + bus.shadowSubscriptions.get(localName).size() + ")");
    }

    nativeLog("------------------------------------------------");
  }

  private static void _showErrorConsole() {
    ClientMessageBusImpl bus = (ClientMessageBusImpl) ErraiBus.get();
    bus.errorDialog.show();
  }
}
