package com.printflow.whatsapp;

import com.printflow.catalog.ProductTemplates;
import com.printflow.identity.StaffPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
public class WhatsappService {
    public record MessageView(UUID id,String direction,String type,String text,String status,Instant at,String providerErrorCode) {}
    public record ConversationView(UUID id,String phone,String contactName,String handoffStatus,UUID assignedStaffId,boolean suppressed,Instant lastInboundAt,Instant updatedAt,String lastMessage,List<MessageView> messages) {}
    public record Outbound(UUID id,String to,String body,int attempts) {}
    private final JdbcTemplate db;private final JsonMapper json;private final WhatsappEncryption cipher;private final ProductTemplates products;private final WhatsappSettings settings;private final String publicBase;
    public WhatsappService(JdbcTemplate db,JsonMapper json,WhatsappEncryption cipher,ProductTemplates products,WhatsappSettings settings,@Value("${APP_PUBLIC_BASE_URL:http://127.0.0.1:3000}") String publicBase){this.db=db;this.json=json;this.cipher=cipher;this.products=products;this.settings=settings;this.publicBase=publicBase.replaceAll("/$","");}

    @Transactional public void acceptWebhook(byte[] raw){
        Map<String,Object> root;
        try{root=json.readValue(raw,new TypeReference<Map<String,Object>>(){});}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid webhook JSON");}
        boolean handled=false;
        for(Object entryValue:list(root.get("entry"))){Map<String,Object> entry=map(entryValue);for(Object changeValue:list(entry.get("changes"))){Map<String,Object> change=map(changeValue),value=map(change.get("value"));
            for(Object messageValue:list(value.get("messages"))){Map<String,Object> message=map(messageValue);String id=string(message.get("id")),from=string(message.get("from")),type=string(message.get("type"));String text=messageText(message,type);String name=contactName(value,from);long at=epoch(message.get("timestamp"));if(id!=null){inbound(id,from,name,type==null?"unknown":type,text,at,false);handled=true;}}
            for(Object statusValue:list(value.get("statuses"))){Map<String,Object> status=map(statusValue);String id=string(status.get("id")),state=string(status.get("status"));if(id!=null&&state!=null){status(id,state,epoch(status.get("timestamp")),firstError(status));handled=true;}}
        }}
        if(!handled){String eventId="unknown:"+sha256(new String(raw,StandardCharsets.UTF_8));recordEvent(eventId,sha256(raw),"UNRECOGNIZED");}
    }

    @Transactional public void receiveSimulated(String sender,String text){
        if(!settings.simulatorEnabled())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        String digits=normalize(sender);if(text==null||text.length()>4096)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Message is too long");
        String id="sim:"+UUID.randomUUID();inbound(id,digits,null,"text",text,Instant.now().getEpochSecond(),true);
    }

    private void inbound(String providerId,String sender,String contactName,String type,String text,long timestamp,boolean simulated){
        if(providerId.length()>220){recordEvent("oversize:"+sha256(providerId),sha256(providerId),"REJECTED");return;}
        if(sender==null||!sender.matches("[0-9]{7,15}")){recordEvent("invalid-contact:"+sha256(providerId),sha256(providerId),"IGNORED");return;}
        String eventId=(simulated?"sim-message:":"message:")+providerId;
        if(!recordEvent(eventId,sha256((providerId+"|"+sender+"|"+type+"|"+Objects.toString(text,"")).getBytes(StandardCharsets.UTF_8)),"INBOUND"))return;
        UUID conversation=conversation(sender,contactName);
        String safeText=text==null?"":text.strip();if(safeText.length()>4096)safeText=safeText.substring(0,4096);
        String messageType=Set.of("text","interactive").contains(type)?type:"unsupported";
        db.update("insert into whatsapp_messages(id,conversation_id,provider_message_id,idempotency_key,direction,message_type,body_ciphertext,delivery_status,provider_status_at) values(?,?,?,?, 'INBOUND',?,?, 'RECEIVED',to_timestamp(?)) on conflict do nothing",UUID.randomUUID(),conversation,simulated?null:providerId,eventId,"unsupported".equals(messageType)?messageType:type,cipher.encrypt(safeText),timestamp);
        db.update("update whatsapp_conversations set last_inbound_at=greatest(coalesce(last_inbound_at,'epoch'::timestamptz),to_timestamp(?)),updated_at=now(),revision=revision+1 where id=?",timestamp,conversation);
        respond(conversation,safeText);
    }

    private UUID conversation(String phone,String name){
        String hash=sha256(phone);
        db.update("insert into whatsapp_conversations(id,phone_hash,phone_ciphertext,contact_name_ciphertext) values(?,?,?,?) on conflict(phone_hash) do nothing",UUID.randomUUID(),hash,cipher.encrypt("+"+phone),name==null||name.isBlank()?null:cipher.encrypt(name.substring(0,Math.min(140,name.length()))));
        return db.queryForObject("select id from whatsapp_conversations where phone_hash=? for update",UUID.class,hash);
    }

    private void respond(UUID id,String inbound){
        Map<String,Object> conversation=db.queryForMap("select bot_state,state_data,handoff_status,suppressed_at,last_inbound_at from whatsapp_conversations where id=? for update",id);
        String state=conversation.get("bot_state").toString(),control=conversation.get("handoff_status").toString(),input=inbound.trim(),upper=input.toUpperCase(Locale.ROOT);
        Map<String,Object> data=readData(conversation.get("state_data"));
        if(conversation.get("suppressed_at")!=null&&!Set.of("STOP","UNSUBSCRIBE","CANCEL").contains(upper)){
            db.update("update whatsapp_conversations set handoff_status='HUMAN',bot_state='HANDOFF',updated_at=now(),revision=revision+1 where id=? and handoff_status<>'HUMAN'",id);
            db.update("insert into whatsapp_handoff_requests(id,conversation_id,reason) select ?,?,'Inbound support from an opted-out contact' where not exists(select 1 from whatsapp_handoff_requests where conversation_id=? and status in ('OPEN','ASSIGNED'))",UUID.randomUUID(),id,id);
            return;
        }
        if(Set.of("STOP","UNSUBSCRIBE","CANCEL").contains(upper)){
            db.update("update whatsapp_conversations set suppressed_at=coalesce(suppressed_at,now()),updated_at=now(),revision=revision+1 where id=?",id);
            db.update("insert into whatsapp_contact_consents(id,conversation_id,purpose,channel,consent_version,granted,granted_at,withdrawn_at) values(?,?,'SERVICE_UPDATES','WHATSAPP','1.0',false,now(),now())",UUID.randomUUID(),id);
            queue(id,"You are unsubscribed from business-initiated WhatsApp updates. You can still message us whenever you need help.",true);return;
        }
        if("HANDOFF".equals(control))return;
        if(Set.of("HELP","MENU").contains(upper)){db.update("update whatsapp_conversations set bot_state='MENU',state_data='{}'::jsonb where id=?",id);queue(id,greeting(),false);return;}
        if("START".equals(upper)&&"AWAITING_CONSENT".equals(state)){
            db.update("insert into whatsapp_contact_consents(id,conversation_id,purpose,channel,consent_version,granted,granted_at) values(?,?,'SERVICE_UPDATES','WHATSAPP','1.0',true,now())",UUID.randomUUID(),id);
            db.update("update whatsapp_conversations set bot_state='MENU',state_data='{}'::jsonb where id=?",id);queue(id,"You opted in to WhatsApp service updates. You can opt out any time by replying STOP.\n\n"+greeting(),false);return;
        }
        switch(state){
            case "QUOTE_CATEGORY" -> {List<String> groups=groups();int choice=number(input,groups.size());if(choice<1){queue(id,"Reply with a category number, or MENU to return to the main menu.\n"+groupMenu(groups),false);return;}data.put("category",groups.get(choice-1));db.update("update whatsapp_conversations set bot_state='QUOTE_PRODUCT',state_data=?::jsonb where id=?",json(data),id);queue(id,productMenu(groups.get(choice-1)),false);}
            case "QUOTE_PRODUCT" -> {List<ProductTemplates.Template> choices=products.list(false).stream().filter(p->group(p.code()).equals(data.get("category"))).toList();int choice=number(input,choices.size());if(choice<1){queue(id,"Reply with a product number from this list.\n"+productMenu(Objects.toString(data.get("category"),"CUSTOM")),false);return;}var p=choices.get(choice-1);data.put("product",p.code());db.update("update whatsapp_conversations set bot_state='QUOTE_QUANTITY',state_data=?::jsonb where id=?",json(data),id);queue(id,"About how many do you need? An estimate is fine. Reply with a number, or ‘not sure’.",false);}
            case "QUOTE_QUANTITY" -> {String digits=input.replaceAll("[, ]","");if(!digits.matches("[0-9]{1,8}")&&!Set.of("NOT SURE","UNSURE").contains(upper)){queue(id,"Please enter an approximate quantity, or reply ‘not sure’.",false);return;}data.put("quantity",digits.matches("[0-9]{1,8}")?digits:"Not sure");db.update("update whatsapp_conversations set bot_state='QUOTE_ARTWORK',state_data=?::jsonb where id=?",json(data),id);queue(id,"Do you have artwork ready? Reply: 1 Yes, 2 Not yet, 3 I need design help.",false);}
            case "QUOTE_ARTWORK" -> {String artwork=Set.of("1","YES","READY").contains(upper)?"Ready":Set.of("2","NO","NOT YET").contains(upper)?"Not yet":Set.of("3","DESIGN").contains(upper)?"Design assistance":"";if(artwork.isBlank()){queue(id,"Please reply 1, 2 or 3 so we can guide you.",false);return;}data.put("artwork",artwork);db.update("update whatsapp_conversations set bot_state='QUOTE_DATE',state_data=?::jsonb where id=?",json(data),id);queue(id,"When would you like the print? Reply with a preferred date, or ‘skip’ if you are not sure.",false);}
            case "QUOTE_DATE" -> {data.put("date",Set.of("SKIP","NOT SURE","UNSURE").contains(upper)?"Not specified":input.substring(0,Math.min(100,input.length())));db.update("update whatsapp_conversations set bot_state='QUOTE_DETAILS',state_data=?::jsonb where id=?",json(data),id);queue(id,"Any other requirements we should know? Reply with a short note, or ‘skip’.",false);}
            case "QUOTE_DETAILS" -> {data.put("notes",Set.of("SKIP","NONE").contains(upper)?"None":input.substring(0,Math.min(1000,input.length())));String code=Objects.toString(data.get("product"),"CUSTOM");db.update("update whatsapp_conversations set bot_state='MENU',state_data='{}'::jsonb where id=?",id);queue(id,"Thanks — your outline is ready for our team. It does not create an order or set a price. To submit a secure quotation request, continue here: "+publicBase+"/request-quote?product="+code+"\n\nWe’ll ask for your contact details and privacy consent on the website. No private order details are shared in WhatsApp.",false);}
            case "AWAITING_CONSENT" -> queue(id,"To opt in to service updates, reply START. To leave preferences unchanged, reply MENU. Reply STOP any time to withdraw.",false);
            default -> menu(id,upper);
        }
    }

    private void menu(UUID id,String input){switch(input){case "1"->{var groups=groups();db.update("update whatsapp_conversations set bot_state='QUOTE_CATEGORY',state_data='{}'::jsonb where id=?",id);queue(id,"Let’s outline your print request. Choose a category:\n"+groupMenu(groups),false);}case "2"->queue(id,servicesText(),false);case "3"->queue(id,"For private order or enquiry status, use the secure customer workspace or token-based tracker: "+publicBase+"/track. We cannot verify account identity from a phone number alone.",false);case "4"->{db.update("update whatsapp_conversations set bot_state='HANDOFF',handoff_status='HUMAN',updated_at=now(),revision=revision+1 where id=?",id);db.update("insert into whatsapp_handoff_requests(id,conversation_id,reason) values(?,?,'Customer requested to speak with the team')",UUID.randomUUID(),id);queue(id,"A J.D.Printex team member has been asked to review this conversation. Please do not send passwords or payment details here.",false);}case "5"->{db.update("update whatsapp_conversations set bot_state='AWAITING_CONSENT' where id=?",id);queue(id,"Privacy: we use this conversation to respond to your request. Your WhatsApp permission is separate from website privacy consent.\n\nReply START to opt in to service updates. Reply STOP at any time to withdraw.",false);}default->queue(id,greeting(),false);}}

    private String greeting(){return "Welcome to J.D.Printex.\nHow can we help with your printing?\n\n1. Request a print quotation\n2. Explore printing services\n3. Enquire about an existing order\n4. Talk to our team\n5. Privacy and communication preferences\n\nReply HELP or MENU to see these options again.";}
    private String servicesText(){StringBuilder out=new StringBuilder("Explore our enabled print services:\n");for(var p:products.list(false))out.append("• ").append(p.name()).append(" — ").append(p.description()).append("\n").append(publicBase).append("/products/").append(p.code()).append("\n");return out.toString().trim();}
    private List<String> groups(){return products.list(false).stream().map(p->group(p.code())).distinct().toList();}
    private String groupMenu(List<String> groups){StringBuilder out=new StringBuilder();for(int i=0;i<groups.size();i++)out.append(i+1).append(". ").append(groups.get(i)).append("\n");return out.toString().trim();}
    private String productMenu(String category){List<ProductTemplates.Template> rows=products.list(false).stream().filter(p->group(p.code()).equals(category)).toList();StringBuilder out=new StringBuilder("Choose a product:\n");for(int i=0;i<rows.size();i++)out.append(i+1).append(". ").append(rows.get(i).name()).append("\n");return out.toString().trim();}
    private String group(String code){return switch(code){case "WEDDING_CARDS"->"Wedding & celebrations";case "HOTEL_PRINT"->"Hotels & hospitality";case "BROCHURES"->"Pamphlets & promotion";case "OFFSET_GLOSS","CARDS"->"Business & offset print";case "BOOKS"->"Books & catalogues";case "PACKAGING"->"Packaging";default->"Custom printing";};}
    private int number(String value,int max){try{int n=Integer.parseInt(value.trim());return n>=1&&n<=max?n:0;}catch(Exception e){return 0;}}
    private void queue(UUID conversation,String body,boolean operationalAck){
        var inbound=db.queryForList("select last_inbound_at,suppressed_at from whatsapp_conversations where id=?",conversation).stream().findFirst().orElseThrow();
        var last=(java.sql.Timestamp)inbound.get("last_inbound_at");
        WhatsappOutboundPolicy.requireEligible(inbound.get("suppressed_at")!=null,last==null?null:last.toInstant(),Instant.now(),operationalAck);
        db.update("insert into whatsapp_messages(id,conversation_id,idempotency_key,direction,message_type,body_ciphertext,delivery_status,next_attempt_at) values(?,?,?,'OUTBOUND',?,?, 'QUEUED',now()) on conflict(idempotency_key) do nothing",UUID.randomUUID(),conversation,"out:"+UUID.randomUUID(),operationalAck?"OPT_OUT_ACK":"text",cipher.encrypt(body));db.update("update whatsapp_conversations set last_outbound_at=now(),updated_at=now() where id=?",conversation);
    }

    private void status(String providerMessageId,String rawStatus,long at,String error){if(!Set.of("sent","delivered","read","failed").contains(rawStatus))return;String next=switch(rawStatus){case "sent"->"SENT";case "delivered"->"DELIVERED";case "read"->"READ";default->"FAILED";};String eventId="status:"+providerMessageId+":"+rawStatus+":"+at;if(!recordEvent(eventId,sha256(eventId),"STATUS"))return;db.update("update whatsapp_messages set delivery_status=?,provider_status_at=to_timestamp(?),provider_error_code=? where provider_message_id=? and (provider_status_at is null or provider_status_at<to_timestamp(?) or (provider_status_at=to_timestamp(?) and (case delivery_status when 'SENT' then 1 when 'DELIVERED' then 2 when 'READ' then 3 when 'FAILED' then 4 else 0 end)<(case ? when 'SENT' then 1 when 'DELIVERED' then 2 when 'READ' then 3 when 'FAILED' then 4 else 0 end)))",next,at,error,providerMessageId,at,at,next);}
    private boolean recordEvent(String id,String hash,String type){return db.update("insert into whatsapp_events(id,provider_event_id,event_hash,event_type,state,processed_at) values(?,?,?,?, 'PROCESSED',now()) on conflict(provider_event_id) do nothing",UUID.randomUUID(),id,hash,type)==1;}
    private Map<String,Object> readData(Object raw){try{return json.readValue(raw.toString(),new TypeReference<Map<String,Object>>(){});}catch(Exception e){return new LinkedHashMap<>();}}
    private String json(Map<String,Object> data){try{return json.writeValueAsString(data);}catch(Exception e){throw new IllegalStateException("Could not save conversation state");}}
    private static String messageText(Map<String,Object> message,String type){if("text".equals(type))return string(map(message.get("text")).get("body"));if("interactive".equals(type)){Map<String,Object> i=map(message.get("interactive"));String b=string(map(i.get("button_reply")).get("id"));if(b==null)b=string(map(i.get("list_reply")).get("id"));if(b==null)b=string(map(i.get("button_reply")).get("title"));if(b==null)b=string(map(i.get("list_reply")).get("title"));return b;}return null;}
    private static String contactName(Map<String,Object> value,String sender){for(Object c:list(value.get("contacts"))){Map<String,Object> contact=map(c);if(Objects.equals(string(contact.get("wa_id")),sender))return string(map(contact.get("profile")).get("name"));}return null;}
    private static String firstError(Map<String,Object> status){List<Object> errors=list(status.get("errors"));return errors.isEmpty()?null:Objects.toString(map(errors.getFirst()).get("code"),null);}
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object value){return value instanceof Map<?,?>?(Map<String,Object>)value:Map.of();}
    @SuppressWarnings("unchecked") private static List<Object> list(Object value){return value instanceof List<?>?(List<Object>)value:List.of();}
    private static String string(Object value){return value==null?null:value.toString();}
    private static long epoch(Object value){try{return Long.parseLong(value.toString());}catch(Exception e){return Instant.now().getEpochSecond();}}
    private static String normalize(String phone){return phone==null?"":phone.replaceAll("[^0-9]","");}
    private static String sha256(String value){return sha256(value.getBytes(StandardCharsets.UTF_8));}
    private static String sha256(byte[] value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));}catch(Exception e){throw new IllegalStateException(e);}}

    @Transactional(readOnly=true) public List<ConversationView> list(StaffPrincipal actor){boolean owner="OWNER".equals(actor.role());return db.query("select c.id,c.phone_ciphertext,c.contact_name_ciphertext,c.handoff_status,c.assigned_staff_id,c.suppressed_at,c.last_inbound_at,c.updated_at,(select m.body_ciphertext from whatsapp_messages m where m.conversation_id=c.id order by m.created_at desc,m.id desc limit 1) last_body from whatsapp_conversations c where (? or c.assigned_staff_id=? or (c.handoff_status='HUMAN' and c.assigned_staff_id is null)) order by c.updated_at desc limit 100",(rs,n)->new ConversationView(rs.getObject("id",UUID.class),cipher.decrypt(rs.getString("phone_ciphertext")),decrypt(rs.getString("contact_name_ciphertext")),rs.getString("handoff_status"),rs.getObject("assigned_staff_id",UUID.class),rs.getTimestamp("suppressed_at")!=null,toInstant(rs,"last_inbound_at"),rs.getTimestamp("updated_at").toInstant(),decrypt(rs.getString("last_body")),List.of()),owner,actor.id());}
    @Transactional(readOnly=true) public ConversationView detail(StaffPrincipal actor,UUID id){var c=lockedConversation(actor,id,false);List<MessageView> messages=db.query("select id,direction,message_type,body_ciphertext,delivery_status,created_at,provider_error_code from whatsapp_messages where conversation_id=? order by created_at,id limit 300",(rs,n)->new MessageView(rs.getObject("id",UUID.class),rs.getString("direction"),rs.getString("message_type"),decrypt(rs.getString("body_ciphertext")),rs.getString("delivery_status"),rs.getTimestamp("created_at").toInstant(),rs.getString("provider_error_code")),id);return new ConversationView(c.id(),c.phone(),c.contactName(),c.handoffStatus(),c.assignedStaffId(),c.suppressed(),c.lastInboundAt(),c.updatedAt(),c.lastMessage(),messages);}
    @Transactional public ConversationView claim(StaffPrincipal actor,UUID id){var c=lockedConversation(actor,id,true);if(!"OWNER".equals(actor.role())&&c.assignedStaffId()!=null&&!actor.id().equals(c.assignedStaffId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Conversation is assigned to another staff member");if(c.suppressed())throw new ResponseStatusException(HttpStatus.CONFLICT,"This contact opted out of business-initiated messages");db.update("update whatsapp_conversations set assigned_staff_id=?,handoff_status='HUMAN',bot_state='HANDOFF',updated_at=now(),revision=revision+1 where id=?",actor.id(),id);db.update("update whatsapp_handoff_requests set assigned_staff_id=?,status='ASSIGNED' where conversation_id=? and status in ('OPEN','ASSIGNED')",actor.id(),id);return lockedConversation(actor,id,false);}
    @Transactional public void resume(StaffPrincipal actor,UUID id){var c=lockedConversation(actor,id,true);if(!"OWNER".equals(actor.role())&&!actor.id().equals(c.assignedStaffId()))throw new ResponseStatusException(HttpStatus.NOT_FOUND);db.update("update whatsapp_conversations set handoff_status='BOT',bot_state='MENU',state_data='{}'::jsonb,assigned_staff_id=null,updated_at=now(),revision=revision+1 where id=?",id);db.update("update whatsapp_handoff_requests set status='CLOSED',closed_at=now() where conversation_id=? and status in ('OPEN','ASSIGNED')",id);}
    @Transactional public void reply(StaffPrincipal actor,UUID id,String text){if(text==null||text.isBlank()||text.length()>4000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Message must contain 1 to 4000 characters");var c=lockedConversation(actor,id,true);if(!"OWNER".equals(actor.role())&&!actor.id().equals(c.assignedStaffId()))throw new ResponseStatusException(HttpStatus.NOT_FOUND);WhatsappOutboundPolicy.requireEligible(c.suppressed(),c.lastInboundAt(),Instant.now(),false);queue(id,text,false);}
    private ConversationView lockedConversation(StaffPrincipal actor,UUID id,boolean lock){boolean owner="OWNER".equals(actor.role());String sql="select c.id,c.phone_ciphertext,c.contact_name_ciphertext,c.handoff_status,c.assigned_staff_id,c.suppressed_at,c.last_inbound_at,c.updated_at,(select m.body_ciphertext from whatsapp_messages m where m.conversation_id=c.id order by m.created_at desc,m.id desc limit 1) last_body from whatsapp_conversations c where c.id=? and (? or c.assigned_staff_id=? or (c.handoff_status='HUMAN' and c.assigned_staff_id is null))"+(lock?" for update":"");return db.query(sql,(rs,n)->new ConversationView(rs.getObject("id",UUID.class),cipher.decrypt(rs.getString("phone_ciphertext")),decrypt(rs.getString("contact_name_ciphertext")),rs.getString("handoff_status"),rs.getObject("assigned_staff_id",UUID.class),rs.getTimestamp("suppressed_at")!=null,toInstant(rs,"last_inbound_at"),rs.getTimestamp("updated_at").toInstant(),decrypt(rs.getString("last_body")),List.of()),id,owner,actor.id()).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
    private static Instant toInstant(java.sql.ResultSet rs,String col)throws java.sql.SQLException{var value=rs.getTimestamp(col);return value==null?null:value.toInstant();}
    private String decrypt(String value){return value==null?null:cipher.decrypt(value);}
    public Map<String,Object> contact(){return Map.of("available",settings.publicContactAvailable(),"number",settings.publicContactAvailable()?settings.publicNumber():"","url",settings.publicContactAvailable()?"https://wa.me/"+settings.publicNumber():"");}
    public boolean simulatorEnabled(){return settings.simulatorEnabled();}
}
