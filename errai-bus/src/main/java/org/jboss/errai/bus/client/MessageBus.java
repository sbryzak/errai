package org.jboss.errai.bus.client;

public interface MessageBus {
    public static final String WS_SESSION_ID = "WSSessionID";

    public void sendGlobal(CommandMessage message);

    public void send(CommandMessage message);
    public void send(String subject, CommandMessage message);

    public void send(CommandMessage message, boolean fireListeners);
    public void send(String subject, CommandMessage message, boolean fireListener);

    public void conversationWith(CommandMessage message, MessageCallback callback);

    public void subscribe(String subject, MessageCallback receiver);
    public void unsubscribeAll(String subject);

    public boolean isSubscribed(String subject);

    public void addGlobalListener(MessageListener listener);
    public void addSubscribeListener(SubscribeListener listener);
    public void addUnsubscribeListener(UnsubscribeListener listener);
}