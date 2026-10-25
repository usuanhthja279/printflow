package com.printflow.collaboration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.printflow.files.DesignProofs;
import com.printflow.identity.StaffDirectory;
import com.printflow.identity.StaffPrincipal;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;

class CollaborationBrokerTest {
    private final SimpMessagingTemplate messaging=mock(SimpMessagingTemplate.class);
    private final DesignProofs proofs=mock(DesignProofs.class);
    private final StaffDirectory directory=mock(StaffDirectory.class);
    private final ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
    private final HttpSession session=mock(HttpSession.class);
    private final StaffPrincipal customer=new StaffPrincipal(UUID.randomUUID(),"customer@example.test","hash","CUSTOMER",true,false,1);
    private final UUID project=UUID.randomUUID();
    private final UUID proof=UUID.randomUUID();
    private CollaborationBroker broker;

    @BeforeEach void setup(){
        broker=new CollaborationBroker(messaging,proofs,directory,events);
        when(session.getAttribute("SPRING_SECURITY_CONTEXT")).thenReturn(new SecurityContextImpl(new UsernamePasswordAuthenticationToken(customer,"n/a")));
        when(directory.current(customer)).thenReturn(true);
        broker.join("socket-1",customer,session,project);
    }

    @Test void acceptsOnlyFiniteNormalizedCoordinatesForAnAuthorizedRevision(){
        broker.cursor("socket-1",customer,project,new CollaborationBroker.CursorPacket(proof,2,0.15,0.85,false));
        verify(proofs).authorizeProofCursor(customer,project,proof,2);
        assertThrows(IllegalArgumentException.class,()->broker.cursor("socket-1",customer,project,new CollaborationBroker.CursorPacket(proof,1,1.01,0.5,false)));
        assertThrows(IllegalArgumentException.class,()->broker.cursor("socket-1",customer,project,new CollaborationBroker.CursorPacket(proof,0,0.5,0.5,false)));
        assertThrows(IllegalArgumentException.class,()->broker.cursor("socket-1",customer,project,new CollaborationBroker.CursorPacket(proof,1,Double.NaN,0.5,false)));
    }

    @Test void rejectsEventsForAProjectThatWasNotJoined(){
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->broker.heartbeat("socket-1",customer,UUID.randomUUID()));
    }

    @Test void rechecksTheAccountAndProjectOnEachHeartbeat(){
        assertDoesNotThrow(()->broker.heartbeat("socket-1",customer,project));
        verify(proofs,org.mockito.Mockito.atLeastOnce()).authorizeCollaboration(customer,project);
    }
}
