package com.printflow.collaboration;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.util.matcher.MessageMatcher;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocketSecurity
public class CollaborationConfiguration implements WebSocketMessageBrokerConfigurer {
    private final CollaborationChannelInterceptor interceptor;
    private final CollaborationBroker broker;
    private final List<String> allowedOrigins;

    public CollaborationConfiguration(CollaborationChannelInterceptor interceptor, CollaborationBroker broker,
            @Value("${app.collaboration.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}") String origins) {
        this.interceptor=interceptor;
        this.broker=broker;
        this.allowedOrigins=Arrays.stream(origins.split(",")).map(String::trim).filter(s->!s.isBlank()).toList();
    }

    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/collaboration/ws").addInterceptors(new CollaborationHandshakeInterceptor()).setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        registry.enableSimpleBroker("/queue");
    }

    @Override public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(interceptor);
    }

    @Override public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(8192).setSendTimeLimit(5000).setSendBufferSizeLimit(64*1024);
        registration.addDecoratorFactory(new WebSocketHandlerDecoratorFactory() {
            @Override public WebSocketHandler decorate(WebSocketHandler handler) {
                return new WebSocketHandlerDecorator(handler) {
                    @Override public void afterConnectionEstablished(org.springframework.web.socket.WebSocketSession session) throws Exception {
                        broker.attachTransport(session);
                        super.afterConnectionEstablished(session);
                    }
                    @Override public void afterConnectionClosed(org.springframework.web.socket.WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
                        broker.detachTransport(session.getId());
                        super.afterConnectionClosed(session,status);
                    }
                };
            }
        });
    }

    @Bean AuthorizationManager<Message<?>> collaborationMessages(MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        messages.nullDestMatcher().authenticated()
                .simpDestMatchers("/app/collaboration/**").hasAnyRole("CUSTOMER","DESIGNER","SALES","OWNER")
                .simpSubscribeDestMatchers("/user/queue/collaboration/**").hasAnyRole("CUSTOMER","DESIGNER","SALES","OWNER")
                .simpTypeMatchers(SimpMessageType.CONNECT, SimpMessageType.UNSUBSCRIBE, SimpMessageType.DISCONNECT).authenticated()
                .anyMessage().denyAll();
        return messages.build();
    }
}
