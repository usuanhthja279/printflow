package com.printflow.collaboration;

import com.printflow.identity.StaffPrincipal;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
final class CollaborationChannelInterceptor implements ChannelInterceptor {
    private static final Pattern SUBSCRIBE=Pattern.compile("/user/queue/collaboration/([0-9a-fA-F-]{36})");
    private static final Pattern APP=Pattern.compile("/app/collaboration/([0-9a-fA-F-]{36})/(cursor|heartbeat)");
    private final CollaborationBroker broker;
    private final com.printflow.files.DesignProofs proofs;
    private final com.printflow.identity.StaffDirectory directory;
    CollaborationChannelInterceptor(CollaborationBroker broker,com.printflow.files.DesignProofs proofs,com.printflow.identity.StaffDirectory directory){this.broker=broker;this.proofs=proofs;this.directory=directory;}
    @Override public Message<?> preSend(Message<?> message,MessageChannel channel) {
        StompHeaderAccessor accessor=StompHeaderAccessor.wrap(message);
        StompCommand command=accessor.getCommand();
        if(command==StompCommand.CONNECT){
            StaffPrincipal staff=staff(accessor);
            if(!valid(accessor,staff))throw new AccessDeniedException("Collaboration session is no longer valid");
            return message;
        }
        if(command==StompCommand.SUBSCRIBE){
            StaffPrincipal staff=staff(accessor); Matcher match=SUBSCRIBE.matcher(nullToEmpty(accessor.getDestination()));
            if(!match.matches()||!valid(accessor,staff))throw new AccessDeniedException("Project collaboration subscription denied");
            UUID project=parse(match.group(1));HttpSession session=httpSession(accessor);
            authorize(staff,project);broker.join(accessor.getSessionId(),staff,session,project);
            return message;
        }
        if(command==StompCommand.MESSAGE){
            StaffPrincipal staff=staff(accessor);Matcher match=APP.matcher(nullToEmpty(accessor.getDestination()));
            if(!match.matches()||!valid(accessor,staff))throw new AccessDeniedException("Collaboration message denied");
            UUID project=parse(match.group(1));authorize(staff,project);
            if(!broker.subscribed(accessor.getSessionId(),project))throw new AccessDeniedException("Join the authorized project before sending collaboration updates");
            return message;
        }
        if(command==StompCommand.UNSUBSCRIBE||command==StompCommand.DISCONNECT||command==StompCommand.ACK||command==StompCommand.NACK)return message;
        if(command!=null)throw new AccessDeniedException("Unsupported collaboration operation");
        return message;
    }
    private StaffPrincipal staff(StompHeaderAccessor a){Object p=a.getUser();if(p instanceof org.springframework.security.core.Authentication auth&&auth.getPrincipal() instanceof StaffPrincipal s)return s;throw new AccessDeniedException("Authentication required");}
    private boolean valid(StompHeaderAccessor a,StaffPrincipal staff){HttpSession session=httpSession(a);if(session==null)return false;try{Object context=session.getAttribute("SPRING_SECURITY_CONTEXT");return context instanceof org.springframework.security.core.context.SecurityContext sc&&sc.getAuthentication()!=null&&sc.getAuthentication().getPrincipal() instanceof StaffPrincipal actual&&actual.id().equals(staff.id())&&directory.current(staff)&&(!staff.role().equals("OWNER")||com.printflow.identity.OwnerMfaController.verified(session));}catch(IllegalStateException expired){return false;}}
    private HttpSession httpSession(StompHeaderAccessor a){Object s=a.getSessionAttributes()==null?null:a.getSessionAttributes().get(CollaborationHandshakeInterceptor.HTTP_SESSION);return s instanceof HttpSession h?h:null;}
    private void authorize(StaffPrincipal staff,UUID id){try{proofs.authorizeCollaboration(staff,id);}catch(RuntimeException denied){throw new AccessDeniedException("Project collaboration access denied");}}
    private UUID parse(String value){try{return UUID.fromString(value);}catch(IllegalArgumentException invalid){throw new AccessDeniedException("Invalid project");}}
    private String nullToEmpty(String value){return value==null?"":value;}
}
