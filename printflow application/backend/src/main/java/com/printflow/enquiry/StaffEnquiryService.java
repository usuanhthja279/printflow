package com.printflow.enquiry;

import com.printflow.enquiry.EnquiryDtos.CreateEnquiry;
import com.printflow.identity.StaffPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaffEnquiryService {
    public enum Source { WEBSITE,WALK_IN,PHONE,EMAIL,WHATSAPP,SOCIAL,REFERRAL,OTHER }
    public enum Priority { LOW,NORMAL,HIGH }
    public record LeadView(UUID id,UUID publicId,String customerName,String email,String phone,
        String productCode,int quantity,String notes,String status,String source,String priority,
        UUID assignedStaffId,String nextAction,Instant followUpAt,long revision,Instant createdAt,java.time.LocalDate requestedDate,String closureKind,String closureMessage) {}
    public record FollowUp(@NotNull Long revision,@NotNull Priority priority,
        @Size(max=500) String nextAction,Instant followUpAt) {}
    public record StatusChange(@NotNull Long revision,@NotNull EnquiryStatus status) {}
    public record OwnerAssignment(@NotNull Long revision,UUID staffId) {}
    public record Assignment(@NotNull Long revision,@NotNull Boolean claim) {}
    public record AssistedEnquiry(@NotNull @Valid CreateEnquiry enquiry,@NotNull Source source) {}
    public record CreatedLead(LeadView lead,String trackingToken) {}
    public record Activity(UUID id,UUID actorId,String action,String details,Instant createdAt) {}
    private final EnquiryRepository repository;
    private final EnquiryService enquiries;
    private final JdbcTemplate jdbc;
    public StaffEnquiryService(EnquiryRepository repository,EnquiryService enquiries,JdbcTemplate jdbc) {
        this.repository=repository;this.enquiries=enquiries;this.jdbc=jdbc;
    }
    @Transactional(readOnly=true) public List<LeadView> list(StaffPrincipal staff,String search,String filter,UUID before) {return list(staff,search,filter,before,"ALL");}
    @Transactional(readOnly=true) public List<LeadView> list(StaffPrincipal staff,String search,String filter,UUID before,String status) {
        if(!java.util.Set.of("ALL","NEW","REVIEWING","QUOTED","IN_PROGRESS","CLOSED").contains(status))throw new IllegalArgumentException("Invalid status filter");
        if(search.length()>140||!java.util.Set.of("ALL","MINE","UNASSIGNED","OVERDUE").contains(filter))throw new IllegalArgumentException("Invalid queue filters");
        String referenceSearch=search.matches("(?i)^PF-[0-9A-F]{1,8}$")||search.matches("(?i)^[0-9A-F]{8}(-[0-9A-F]{4}){3}-[0-9A-F]{12}$")?search.replaceAll("(?i)^PF-","").replace("-","").toUpperCase(java.util.Locale.ROOT):"";
        var ids=jdbc.query("select e.id from enquiries e left join product_templates t on t.code=e.product_code where coalesce(e.closure_kind,'')<>'REMOVED' and (?='ALL' or e.status=?) and (? or (e.status<>'NEW' and e.assigned_staff_id=?)) and (lower(e.customer_name||' '||e.email||' '||e.phone||' '||e.product_code||' '||coalesce(t.name,'')) like ? or upper(replace(e.public_id::text,'-','')) like ?) and (?='ALL' or ?='MINE' and e.assigned_staff_id=? or ?='UNASSIGNED' and e.assigned_staff_id is null or ?='OVERDUE' and e.status<>'CLOSED' and e.follow_up_at<now()) and (?::uuid is null or ((e.status='CLOSED')::int,-extract(epoch from e.created_at),e.id)> (select (status='CLOSED')::int,-extract(epoch from created_at),id from enquiries where id=?)) order by (e.status='CLOSED') asc,e.created_at desc,e.id asc limit 100",(rs,n)->rs.getObject(1,UUID.class),status,status,staff.role().equals("OWNER"),staff.id(),"%"+search.toLowerCase(java.util.Locale.ROOT)+"%",(referenceSearch.isEmpty()?"%!%":"%"+referenceSearch+"%"),filter,filter,staff.id(),filter,filter,before,before);
        var found=repository.findAllById(ids).stream().collect(java.util.stream.Collectors.toMap(e->e.id,e->e));
        return ids.stream().filter(found::containsKey).map(id->view(found.get(id))).toList();
    }
    @Transactional public CreatedLead create(StaffPrincipal staff,AssistedEnquiry input) {
        var created=enquiries.create(input.enquiry());
        Enquiry lead=repository.findByPublicId(created.publicId()).orElseThrow();
        lead.source=input.source().name();lead.assignedStaffId=staff.id();
        audit(lead,staff,"STAFF_CREATED","Manual enquiry; source="+lead.source);
        repository.flush();
        return new CreatedLead(view(lead),created.trackingToken());
    }
    @Transactional public LeadView assign(StaffPrincipal staff,UUID id,Assignment input) {
        Enquiry lead=editable(staff,id,input.revision());
        UUID next=input.claim()?staff.id():null;
        if (!java.util.Objects.equals(lead.assignedStaffId,next)) {
            lead.assignedStaffId=next;lead.updatedAt=Instant.now();
            audit(lead,staff,input.claim()?"CLAIMED":"UNASSIGNED","Assignment changed by responsible sales staff");
        }
        repository.flush();return view(lead);
    }
    @Transactional public LeadView reassign(StaffPrincipal owner,UUID id,OwnerAssignment input) {
        if(!owner.role().equals("OWNER"))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Enquiry lead=editable(owner,id,input.revision());
        if(input.staffId()!=null&&!Integer.valueOf(1).equals(jdbc.queryForObject("select count(*) from staff_users where id=? and enabled and role in ('OWNER','SALES')",Integer.class,input.staffId())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose an enabled owner or sales account");
        if(!java.util.Objects.equals(lead.assignedStaffId,input.staffId())) {
            String previous=String.valueOf(lead.assignedStaffId);lead.assignedStaffId=input.staffId();lead.updatedAt=Instant.now();
            audit(lead,owner,"OWNER_REASSIGNED",previous+" -> "+input.staffId());
        }
        repository.flush();return view(lead);
    }
    @Transactional public LeadView followUp(StaffPrincipal staff,UUID id,FollowUp input) {
        Enquiry lead=owned(staff,id,input.revision());
        if (lead.status==EnquiryStatus.CLOSED) throw new ResponseStatusException(HttpStatus.CONFLICT,"Closed enquiries cannot have follow-ups changed");
        String action=input.nextAction()==null?null:input.nextAction().trim();
        if ((action==null||action.isEmpty())!=(input.followUpAt()==null))
            throw new IllegalArgumentException("Provide both a next action and follow-up time, or clear both");
        String details="Previous: "+lead.priority+" / "+safe(lead.nextAction)+" / "+lead.followUpAt
            +"; New: "+input.priority()+" / "+safe(action)+" / "+input.followUpAt();
        lead.priority=input.priority().name();lead.nextAction=action==null||action.isEmpty()?null:action;
        lead.followUpAt=input.followUpAt();lead.updatedAt=Instant.now();
        audit(lead,staff,"FOLLOW_UP_UPDATED",details);
        repository.flush();return view(lead);
    }
    @Transactional public LeadView status(StaffPrincipal staff,UUID id,StatusChange input) {
        if(input.status()!=EnquiryStatus.REVIEWING)throw new ResponseStatusException(HttpStatus.CONFLICT,"Quotation status is set by issuing a quotation; close using a reasoned resolution");
        Enquiry lead=owned(staff,id,input.revision());
        if(lead.status==EnquiryStatus.NEW)throw new ResponseStatusException(HttpStatus.CONFLICT,"Owner/designer must accept intake first");
        EnquiryStatus previous=lead.status;
        enquiries.updateStatus(lead,input.status());
        if(previous!=lead.status) {
            if(lead.status==EnquiryStatus.CLOSED){lead.nextAction=null;lead.followUpAt=null;}
            audit(lead,staff,"STATUS_CHANGED",previous+" -> "+lead.status);
        }
        repository.flush();return view(lead);
    }
    @Transactional(readOnly=true) public List<Activity> history(StaffPrincipal staff,UUID id) {
        Enquiry lead=repository.findById(id).orElseThrow(NoSuchElementException::new);
        requireVisible(staff,lead);
        return jdbc.query("select id,actor_id,action,details,created_at from lead_activities where enquiry_id=? order by created_at,id",
            (rs,row)->new Activity(rs.getObject("id",UUID.class),rs.getObject("actor_id",UUID.class),
                rs.getString("action"),rs.getString("details"),rs.getTimestamp("created_at").toInstant()),id);
    }
    private Enquiry editable(StaffPrincipal staff,UUID id,long revision) {
        Enquiry lead=repository.findForUpdate(id).orElseThrow(NoSuchElementException::new);
        requireVisible(staff,lead);
        if(lead.revision!=revision)throw new ResponseStatusException(HttpStatus.CONFLICT,"Enquiry changed; refresh before saving");
        return lead;
    }
    private Enquiry owned(StaffPrincipal staff,UUID id,long revision) {
        Enquiry lead=editable(staff,id,revision);
        if(!staff.id().equals(lead.assignedStaffId))throw new ResponseStatusException(HttpStatus.CONFLICT,"Claim this enquiry first");
        return lead;
    }
    private void requireVisible(StaffPrincipal staff,Enquiry lead) {
        if(!staff.role().equals("OWNER")&&(lead.status==EnquiryStatus.NEW||!staff.id().equals(lead.assignedStaffId)))throw new NoSuchElementException();
    }
    private void audit(Enquiry lead,StaffPrincipal staff,String action,String details) {
        jdbc.update("insert into lead_activities(id,enquiry_id,actor_id,action,details) values(?,?,?,?,?)",
            UUID.randomUUID(),lead.id,staff.id(),action,details);
    }
    private String safe(String value){return value==null?"(none)":value;}
    private LeadView view(Enquiry e) {
        return new LeadView(e.id,e.publicId,e.customerName,e.email,e.phone,e.productCode,e.quantity,e.notes,
            e.status.name(),e.source,e.priority,e.assignedStaffId,e.nextAction,e.followUpAt,e.revision,e.createdAt,e.requestedDate,e.closureKind,e.closureMessage);
    }
}
