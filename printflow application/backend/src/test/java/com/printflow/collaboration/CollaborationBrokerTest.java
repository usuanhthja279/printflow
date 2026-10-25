package com.printflow.collaboration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import org.mockito.ArgumentCaptor;

import com.printflow.files.DesignProofs;
import com.printflow.identity.StaffDirectory;
import com.printflow.identity.StaffPrincipal;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;

class CollaborationBrokerTest {
    private final SimpMessagingTemplate messaging=mock(SimpMessagingTemplate.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<SimpMessagingTemplate> messagingProvider=mock(ObjectProvider.class);
    private final DesignProofs proofs=mock(DesignProofs.class);
    private final StaffDirectory directory=mock(StaffDirectory.class);
    private final ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
    private final HttpSession session=mock(HttpSession.class);
    private final StaffPrincipal customer=new StaffPrincipal(UUID.randomUUID(),"customer@example.test","hash","CUSTOMER",true,false,1);
    private final UUID project=UUID.randomUUID();
    private final UUID proof=UUID.randomUUID();
    private CollaborationBroker broker;

    @BeforeEach void setup(){
        when(messagingProvider.getObject()).thenReturn(messaging);
        broker=new CollaborationBroker(messagingProvider,proofs,directory,events);
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

    @Test void replaysTheRemotePointerWhenAParticipantReannouncesTheirView(){
        StaffPrincipal designer=new StaffPrincipal(UUID.randomUUID(),"designer@example.test","hash","DESIGNER",true,false,1);
        HttpSession designerSession=mock(HttpSession.class);
        when(designerSession.getAttribute("SPRING_SECURITY_CONTEXT")).thenReturn(new SecurityContextImpl(new UsernamePasswordAuthenticationToken(designer,"n/a")));
        when(directory.current(designer)).thenReturn(true);
        broker.join("socket-2",designer,designerSession,project);
        clearInvocations(messaging);
        broker.cursor("socket-2",designer,project,new CollaborationBroker.CursorPacket(proof,2,0.2,0.8,false));
        clearInvocations(messaging);

        // A customer's reconnect/page replay carries proof and page, without pointer coordinates.
        broker.cursor("socket-1",customer,project,new CollaborationBroker.CursorPacket(proof,2,null,null,false));

        verify(messaging).convertAndSendToUser(eq(customer.getUsername()),eq("/queue/collaboration/"+project),argThat(event->event instanceof CollaborationBroker.Event e&&e.type().equals("cursor.moved")&&e.data().get("participantId") instanceof String&&"Designer".equals(e.data().get("participant"))&&Boolean.TRUE.equals(e.data().get("samePage"))&&Double.valueOf(0.2).equals(e.data().get("x"))&&Double.valueOf(0.8).equals(e.data().get("y"))),anyMap());
        assertTrue(broker.subscribed("socket-2",project));
    }

    @Test void sameRoleParticipantsKeepDistinctSessionScopedCursorIdentities(){
        StaffPrincipal salesOne=new StaffPrincipal(UUID.randomUUID(),"sales-one@example.test","hash","SALES",true,false,1);
        StaffPrincipal salesTwo=new StaffPrincipal(UUID.randomUUID(),"sales-two@example.test","hash","SALES",true,false,1);
        HttpSession firstSession=mock(HttpSession.class),secondSession=mock(HttpSession.class);
        when(firstSession.getAttribute("SPRING_SECURITY_CONTEXT")).thenReturn(new SecurityContextImpl(new UsernamePasswordAuthenticationToken(salesOne,"n/a")));
        when(secondSession.getAttribute("SPRING_SECURITY_CONTEXT")).thenReturn(new SecurityContextImpl(new UsernamePasswordAuthenticationToken(salesTwo,"n/a")));
        when(directory.current(salesOne)).thenReturn(true);when(directory.current(salesTwo)).thenReturn(true);
        broker.join("sales-socket-1",salesOne,firstSession,project);broker.join("sales-socket-2",salesTwo,secondSession,project);
        broker.cursor("socket-1",customer,project,new CollaborationBroker.CursorPacket(proof,1,0.5,0.5,false));clearInvocations(messaging);
        broker.cursor("sales-socket-1",salesOne,project,new CollaborationBroker.CursorPacket(proof,1,0.2,0.3,false));
        broker.cursor("sales-socket-2",salesTwo,project,new CollaborationBroker.CursorPacket(proof,1,0.7,0.8,false));
        @SuppressWarnings("unchecked") ArgumentCaptor<Object> sent=ArgumentCaptor.forClass(Object.class);
        verify(messaging,atLeastOnce()).convertAndSendToUser(eq(customer.getUsername()),eq("/queue/collaboration/"+project),sent.capture(),anyMap());
        var teamCursors=sent.getAllValues().stream().filter(value->value instanceof CollaborationBroker.Event e&&e.type().equals("cursor.moved")&&"J.D.Printex team".equals(e.data().get("participant"))&&Boolean.TRUE.equals(e.data().get("samePage"))).map(value->((CollaborationBroker.Event)value).data().get("participantId")).distinct().toList();
        assertTrue(teamCursors.size()>=2,"same-label staff cursors must have separate participant IDs");
    }
}
