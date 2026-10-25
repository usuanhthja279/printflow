package com.printflow.collaboration;

import com.printflow.identity.StaffPrincipal;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class CollaborationController {
    private final CollaborationBroker broker;
    public CollaborationController(CollaborationBroker broker){this.broker=broker;}
    @MessageMapping("/collaboration/{project}/heartbeat")
    public void heartbeat(@DestinationVariable UUID project,@AuthenticationPrincipal StaffPrincipal user,org.springframework.messaging.Message<?> message){broker.heartbeat(session(message),user,project);}
    @MessageMapping("/collaboration/{project}/cursor")
    public void cursor(@DestinationVariable UUID project,@AuthenticationPrincipal StaffPrincipal user,org.springframework.messaging.Message<CollaborationBroker.CursorPacket> message){broker.cursor(session(message),user,project,message.getPayload());}
    private String session(org.springframework.messaging.Message<?> message){return org.springframework.messaging.simp.SimpMessageHeaderAccessor.getSessionId(message.getHeaders());}
}
