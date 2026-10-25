package com.printflow.collaboration;

import com.printflow.files.DesignProofs;
import com.printflow.identity.StaffDirectory;
import com.printflow.identity.StaffPrincipal;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

/** Project-scoped, in-memory collaboration state. Permanent records stay in their existing REST/database flows. */
@Service
public class CollaborationBroker {
    public record CursorPacket(UUID proofRevisionId,Integer page,Double x,Double y,Boolean hidden) { }
    public record ProjectChange(UUID projectId,String type,UUID proofRevisionId) { }
    public record Event(int version,UUID eventId,String type,UUID projectId,UUID proofRevisionId,Instant at,Map<String,Object> data) { }
    private static final long IDLE_MILLIS=45_000;private static final int MAX_CONNECTIONS=2000;private static final int MAX_PROJECT_CONNECTIONS=50;
    private final ConcurrentMap<String,Connection> sessions=new ConcurrentHashMap<>();
    private final ConcurrentMap<String,WebSocketSession> transports=new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messages;
    private final DesignProofs proofs;
    private final StaffDirectory directory;
    private final ApplicationEventPublisher events;

    public CollaborationBroker(SimpMessagingTemplate messages,DesignProofs proofs,StaffDirectory directory,ApplicationEventPublisher events){this.messages=messages;this.proofs=proofs;this.directory=directory;this.events=events;}

    public void attachTransport(WebSocketSession session){transports.put(session.getId(),session);}
    public void detachTransport(String sessionId){transports.remove(sessionId);leave(sessionId);}
    public void join(String sessionId,StaffPrincipal user,HttpSession httpSession,UUID project){
        authorize(user,httpSession,project);
        if(sessions.size()>=MAX_CONNECTIONS||projectConnections(project).stream().map(c->c.user.id()).distinct().count()>=MAX_PROJECT_CONNECTIONS)throw new org.springframework.security.access.AccessDeniedException("Collaboration capacity is temporarily full");
        sessions.put(sessionId,new Connection(sessionId,user,httpSession,project));
    }
    public boolean subscribed(String sessionId,UUID project){Connection c=sessions.get(sessionId);return c!=null&&c.project.equals(project);}
    public void heartbeat(String sessionId,StaffPrincipal user,UUID project){Connection c=connection(sessionId,user,project);long now=System.currentTimeMillis();if(now-c.lastHeartbeat<1000)return;c.lastHeartbeat=now;c.lastSeen=now;broadcastPresence(project);}

    public void cursor(String sessionId,StaffPrincipal user,UUID project,CursorPacket packet){
        Connection sender=connection(sessionId,user,project);
        if(packet==null||packet.hidden()==null)throw new IllegalArgumentException("Invalid cursor update");
        if(Boolean.TRUE.equals(packet.hidden())){
            if((packet.proofRevisionId()==null)!=(packet.page()==null)||packet.x()!=null||packet.y()!=null)throw new IllegalArgumentException("Invalid cursor context");
            if(packet.proofRevisionId()!=null){proofs.authorizeProofCursor(user,project,packet.proofRevisionId(),packet.page());sender.cursor=new Cursor(packet.proofRevisionId(),packet.page(),null,null,System.currentTimeMillis());}
            else if(sender.cursor!=null)sender.cursor=new Cursor(sender.cursor.proof,sender.cursor.page,null,null,System.currentTimeMillis());
            else sender.cursor=null;
        }else{
            boolean coordinatesAbsent=packet.x()==null&&packet.y()==null;
            if(packet.proofRevisionId()==null||packet.page()==null||packet.page()<1||packet.page()>100||!coordinatesAbsent&&(!unit(packet.x())||!unit(packet.y())))throw new IllegalArgumentException("Invalid artwork cursor coordinates");
            proofs.authorizeProofCursor(user,project,packet.proofRevisionId(),packet.page());long now=System.currentTimeMillis();if(now-sender.cursorWindowStarted>=1000){sender.cursorWindowStarted=now;sender.cursorPackets=0;}if(++sender.cursorPackets>12)return;sender.lastSeen=now;sender.cursor=new Cursor(packet.proofRevisionId(),packet.page(),packet.x(),packet.y(),now);
        }
        for(Connection target:projectConnections(project)){
            if(target.user.id().equals(user.id()))continue;
            Cursor current=sender.cursor;boolean same=current!=null&&current.x!=null&&current.y!=null&&target.cursor!=null&&target.cursor.proof.equals(current.proof)&&target.cursor.page==current.page;
            Map<String,Object> data=new HashMap<>();data.put("participant",label(user.role()));data.put("samePage",same);data.put("page",current==null?null:current.page);data.put("proofRevisionId",current==null?null:current.proof);
            if(same){data.put("x",current.x);data.put("y",current.y);}
            send(target,new Event(1,UUID.randomUUID(),"cursor.moved",project,current==null?null:current.proof,Instant.now(),data));
        }
    }

    public void presence(String sessionId,StaffPrincipal user,UUID project){connection(sessionId,user,project);broadcastPresence(project);}
    public void changed(UUID project,String type,UUID proof){events.publishEvent(new ProjectChange(project,type,proof));}
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void committed(ProjectChange change){
        Event event=new Event(1,UUID.randomUUID(),change.type(),change.projectId(),change.proofRevisionId(),Instant.now(),Map.of());
        for(Connection target:projectConnections(change.projectId()))send(target,event);
    }

    @Scheduled(fixedDelay=10_000)
    public void expireAndReauthorize(){
        long now=System.currentTimeMillis();var affected=new java.util.HashSet<UUID>();
        sessions.forEach((id,c)->{
            boolean valid=now-c.lastSeen<=IDLE_MILLIS;
            try{if(valid)authorize(c.user,c.httpSession,c.project);}catch(RuntimeException denied){valid=false;}
            if(!valid){affected.add(c.project);sessions.remove(id,c);WebSocketSession socket=transports.remove(id);if(socket!=null&&socket.isOpen())try{socket.close(CloseStatus.POLICY_VIOLATION);}catch(Exception ignored){} }
            else if(c.cursor!=null&&now-c.cursor.updated>IDLE_MILLIS)c.cursor=null;
        });
        affected.forEach(this::broadcastPresence);
    }
    public void leave(String sessionId){Connection c=sessions.remove(sessionId);if(c!=null)broadcastPresence(c.project);}

    private Connection connection(String id,StaffPrincipal user,UUID project){Connection c=sessions.get(id);if(c==null||!c.project.equals(project)||!c.user.id().equals(user.id()))throw new org.springframework.security.access.AccessDeniedException("Collaboration membership expired");authorize(user,c.httpSession,project);return c;}
    private void authorize(StaffPrincipal user,HttpSession session,UUID project){
        try{Object context=session.getAttribute("SPRING_SECURITY_CONTEXT");if(!(context instanceof org.springframework.security.core.context.SecurityContext sc)||sc.getAuthentication()==null||!(sc.getAuthentication().getPrincipal() instanceof StaffPrincipal current)||!current.id().equals(user.id())||!directory.current(user)||user.role().equals("OWNER")&&!com.printflow.identity.OwnerMfaController.verified(session))throw new SecurityException("Session expired");proofs.authorizeCollaboration(user,project);}
        catch(IllegalStateException expired){throw new SecurityException("Session expired",expired);}
    }
    private void broadcastPresence(UUID project){List<Connection> room=projectConnections(project);var participants=room.stream().collect(java.util.stream.Collectors.toMap(c->c.user.id(),c->c,(a,b)->a));
        for(Connection recipient:room){List<Map<String,Object>> visible=new ArrayList<>();participants.values().stream().sorted(Comparator.comparing(c->label(c.user.role()))).forEach(person->{Map<String,Object> row=new HashMap<>();row.put("label",person.user.id().equals(recipient.user.id())?"You":label(person.user.role()));row.put("role",person.user.role());row.put("self",person.user.id().equals(recipient.user.id()));Cursor cursor=person.cursor;row.put("viewingPage",cursor==null?null:cursor.page);row.put("proofRevisionId",cursor==null?null:cursor.proof);visible.add(row);});send(recipient,new Event(1,UUID.randomUUID(),"presence.snapshot",project,null,Instant.now(),Map.of("participants",visible)));}
    }
    private List<Connection> projectConnections(UUID project){var room=new ArrayList<Connection>();sessions.values().forEach(c->{if(c.project.equals(project))room.add(c);});return room;}
    private void send(Connection target,Object event){
        try{authorize(target.user,target.httpSession,target.project);SimpMessageHeaderAccessor headers=SimpMessageHeaderAccessor.create();headers.setSessionId(target.sessionId);headers.setLeaveMutable(true);messages.convertAndSendToUser(target.user.getUsername(),"/queue/collaboration/"+target.project,event,headers.getMessageHeaders());}
        catch(RuntimeException revoked){sessions.remove(target.sessionId,target);WebSocketSession socket=transports.remove(target.sessionId);if(socket!=null&&socket.isOpen())try{socket.close(CloseStatus.POLICY_VIOLATION);}catch(Exception ignored){}}
    }
    private static String label(String role){return role.equals("DESIGNER")?"Designer":role.equals("CUSTOMER")?"Customer":"J.D.Printex team";}
    private static boolean unit(Double v){return v!=null&&Double.isFinite(v)&&v>=0&&v<=1;}
    private static final class Connection{final String sessionId;final StaffPrincipal user;final HttpSession httpSession;final UUID project;volatile long lastSeen=System.currentTimeMillis(),lastHeartbeat=0,cursorWindowStarted=0;volatile int cursorPackets=0;volatile Cursor cursor;Connection(String s,StaffPrincipal u,HttpSession h,UUID p){sessionId=s;user=u;httpSession=h;project=p;}}
    private record Cursor(UUID proof,int page,Double x,Double y,long updated){}
}
