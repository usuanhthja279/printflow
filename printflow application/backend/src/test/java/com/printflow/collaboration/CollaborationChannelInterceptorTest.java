package com.printflow.collaboration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import com.printflow.files.DesignProofs;
import com.printflow.identity.StaffDirectory;
import com.printflow.identity.StaffPrincipal;
import jakarta.servlet.http.HttpSession;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompDecoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;

class CollaborationChannelInterceptorTest {
    private final CollaborationBroker broker=mock(CollaborationBroker.class);
    private final DesignProofs proofs=mock(DesignProofs.class);
    private final StaffDirectory directory=mock(StaffDirectory.class);
    private final HttpSession session=mock(HttpSession.class);
    private final MessageChannel channel=mock(MessageChannel.class);
    private final StaffPrincipal customer=new StaffPrincipal(UUID.randomUUID(),"customer@example.test","hash","CUSTOMER",true,false,1);
    private final UUID project=UUID.randomUUID();
    private CollaborationChannelInterceptor interceptor;

    @BeforeEach void setup(){
        interceptor=new CollaborationChannelInterceptor(broker,proofs,directory);
        var auth=new UsernamePasswordAuthenticationToken(customer,"n/a",customer.getAuthorities());
        when(session.getAttribute("SPRING_SECURITY_CONTEXT")).thenReturn(new SecurityContextImpl(auth));
        when(directory.current(customer)).thenReturn(true);
    }

    @Test void acceptsAnAuthorizedClientSendDecodedFromARealStompFrame(){
        Message<?> frame=decodeClientFrame("SEND","/app/collaboration/"+project+"/cursor");
        assertEquals(StompCommand.SEND,StompHeaderAccessor.wrap(frame).getCommand());
        when(broker.subscribed("browser-session",project)).thenReturn(true);

        assertSame(frame,interceptor.preSend(frame,channel));

        verify(proofs).authorizeCollaboration(customer,project);
        verify(broker).subscribed("browser-session",project);
    }

    @Test void rejectsARealSendFrameForAnUnauthorizedProject(){
        UUID otherProject=UUID.randomUUID();
        Message<?> frame=decodeClientFrame("SEND","/app/collaboration/"+otherProject+"/cursor");
        doThrow(new AccessDeniedException("unassigned")).when(proofs).authorizeCollaboration(customer,otherProject);

        assertThrows(AccessDeniedException.class,()->interceptor.preSend(frame,channel));

        verify(broker,never()).subscribed(eq("browser-session"),eq(otherProject));
    }

    @Test void rejectsARealSendFrameOutsideTheValidatedApplicationDestinations(){
        Message<?> frame=decodeClientFrame("SEND","/topic/collaboration/"+project);

        assertThrows(AccessDeniedException.class,()->interceptor.preSend(frame,channel));

        verify(proofs,never()).authorizeCollaboration(customer,project);
    }

    @Test void rejectsARealSubscriptionForAnotherProjectsQueue(){
        UUID otherProject=UUID.randomUUID();
        Message<?> frame=decodeClientFrame("SUBSCRIBE","/user/queue/collaboration/"+otherProject);
        doThrow(new AccessDeniedException("unassigned")).when(proofs).authorizeCollaboration(customer,otherProject);

        assertThrows(AccessDeniedException.class,()->interceptor.preSend(frame,channel));

        verify(broker,never()).join("browser-session",customer,session,otherProject);
    }

    @Test void rejectsClientSendAfterTheHttpSessionExpires(){
        Message<?> frame=decodeClientFrame("SEND","/app/collaboration/"+project+"/heartbeat");
        when(session.getAttribute("SPRING_SECURITY_CONTEXT")).thenThrow(new IllegalStateException("expired"));

        assertThrows(AccessDeniedException.class,()->interceptor.preSend(frame,channel));
    }

    @Test void requiresRecentMfaForOwnerCollaboration(){
        StaffPrincipal owner=new StaffPrincipal(UUID.randomUUID(),"owner@example.test","hash","OWNER",true,false,1);
        var auth=new UsernamePasswordAuthenticationToken(owner,"n/a",owner.getAuthorities());
        when(session.getAttribute("SPRING_SECURITY_CONTEXT")).thenReturn(new SecurityContextImpl(auth));
        when(directory.current(owner)).thenReturn(true);
        Message<?> frame=decodeClientFrame("CONNECT",null,owner);

        assertThrows(AccessDeniedException.class,()->interceptor.preSend(frame,channel));
    }

    private Message<?> decodeClientFrame(String command,String destination){
        return decodeClientFrame(command,destination,customer);
    }
    private Message<?> decodeClientFrame(String command,String destination,StaffPrincipal principal){
        String protocolHeaders=switch(command){
            case "CONNECT" -> "accept-version:1.2\nhost:localhost\nheart-beat:0,0\n";
            case "SUBSCRIBE" -> "id:subscription-1\nack:auto\n";
            default -> "";
        };
        String body=command.equals("SEND")?"{}":"";
        String wire=command+"\n"+protocolHeaders+(destination==null?"":"destination:"+destination+"\n")+(command.equals("SEND")?"content-type:application/json\n":"")+"\n"+body+"\u0000";
        var decoded=new StompDecoder().decode(ByteBuffer.wrap(wire.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1,decoded.size());
        StompHeaderAccessor accessor=StompHeaderAccessor.wrap(decoded.getFirst());
        accessor.setUser(new UsernamePasswordAuthenticationToken(principal,"n/a",principal.getAuthorities()));
        accessor.setSessionId("browser-session");
        HashMap<String,Object> attributes=new HashMap<>();
        attributes.put(CollaborationHandshakeInterceptor.HTTP_SESSION,session);
        accessor.setSessionAttributes(attributes);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(decoded.getFirst().getPayload(),accessor.getMessageHeaders());
    }
}
