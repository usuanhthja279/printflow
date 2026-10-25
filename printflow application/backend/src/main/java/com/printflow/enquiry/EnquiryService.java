package com.printflow.enquiry;

import com.printflow.catalog.CatalogController;
import com.printflow.enquiry.EnquiryDtos.*;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnquiryService {
    private final com.printflow.catalog.ProductTemplates templates;
    private final EnquiryRepository repository;
    private final com.printflow.identity.CustomerMail mail;
    private final EnquiryEventRepository events;private final com.printflow.identity.ProjectNotifications notifications;
    private final com.printflow.identity.MfaSecrets encryption;private final tools.jackson.databind.json.JsonMapper json;private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    @org.springframework.beans.factory.annotation.Autowired
    public EnquiryService(EnquiryRepository repository, EnquiryEventRepository events,com.printflow.catalog.ProductTemplates templates,com.printflow.identity.CustomerMail mail,com.printflow.identity.ProjectNotifications notifications,com.printflow.identity.MfaSecrets encryption,tools.jackson.databind.json.JsonMapper json,org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.notifications=notifications;this.mail=mail;this.repository=repository; this.events=events;this.templates=templates;this.encryption=encryption;this.json=json;this.jdbc=jdbc;
    }
    EnquiryService(EnquiryRepository repository,EnquiryEventRepository events,com.printflow.catalog.ProductTemplates templates,com.printflow.identity.CustomerMail mail,com.printflow.identity.ProjectNotifications notifications){this(repository,events,templates,mail,notifications,null,null,null);}
    @Transactional
    public CreatedEnquiry create(CreateEnquiry request) {
        return create(request,null);
    }
    @Transactional
    public CreatedEnquiry create(CreateEnquiry request,String idempotencyKey) {
        if (CatalogController.PRODUCTS.stream().noneMatch(p -> p.code().equals(request.productCode())))
            throw new IllegalArgumentException("Unknown product code");
        String keyHash=null,requestHash=null;
        if(idempotencyKey!=null&&!idempotencyKey.isBlank()){
            if(!idempotencyKey.matches("[A-Za-z0-9_-]{20,100}"))throw new IllegalArgumentException("Invalid idempotency key");
            keyHash=TokenUtil.sha256(idempotencyKey);
            try{requestHash=TokenUtil.sha256(json.writeValueAsString(java.util.Arrays.asList(request.customerName(),request.email(),request.phone(),request.productCode(),request.quantity(),request.size(),request.paperGsm(),request.printSides(),request.finish(),request.notes(),request.privacyConsent(),request.privacyVersion(),request.requestedDate(),request.intendedUse(),request.whatsappConsent())));}catch(Exception e){throw new IllegalStateException("Could not fingerprint enquiry request",e);}
        }
        var template=templates.capture(request);
        String token = TokenUtil.newToken();
        UUID publicId=UUID.randomUUID();
        if(keyHash!=null){
            int inserted=jdbc.update("insert into enquiry_request_idempotency(key_hash,request_hash,public_id,tracking_token_ciphertext) values(?,?,?,?) on conflict do nothing",keyHash,requestHash,publicId,encryption.encrypt(token));
            if(inserted==0){var existing=jdbc.query("select request_hash,public_id,tracking_token_ciphertext,created_at from enquiry_request_idempotency where key_hash=?",(rs,n)->new Object[]{rs.getString(1),rs.getObject(2,UUID.class),rs.getString(3),rs.getTimestamp(4).toInstant()},keyHash).getFirst();if(!requestHash.equals(existing[0]))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"This submission key was already used for a different enquiry");return new CreatedEnquiry((UUID)existing[1],encryption.decrypt(existing[2].toString()),"NEW",(java.time.Instant)existing[3]);}
        }
        Enquiry enquiry = new Enquiry();
        enquiry.id=UUID.randomUUID(); enquiry.publicId=publicId;
        enquiry.trackingTokenSha256=TokenUtil.sha256(token);
        enquiry.customerName=request.customerName().trim();
        enquiry.email=request.email().trim(); enquiry.phone=request.phone().trim();
        enquiry.requestedDate=request.requestedDate();
        enquiry.productCode=request.productCode(); enquiry.quantity=request.quantity();enquiry.intendedUse=request.intendedUse();
        enquiry.privacyVersion=Boolean.TRUE.equals(request.privacyConsent())?request.privacyVersion():null;
        enquiry.size=request.size(); enquiry.paperGsm=request.paperGsm();
        enquiry.printSides=request.printSides(); enquiry.finish=request.finish(); enquiry.notes=request.notes();
        if(template!=null){enquiry.templateRevision=template.revision();enquiry.templateSnapshot=templates.snapshot(template);}
        enquiry.status=EnquiryStatus.NEW; enquiry.createdAt=Instant.now(); enquiry.updatedAt=enquiry.createdAt;
        repository.save(enquiry);
        events.save(EnquiryEvent.of(enquiry, null, EnquiryStatus.NEW, "CREATED"));
        repository.flush();
        if(Boolean.TRUE.equals(request.whatsappConsent()))jdbc.update("insert into whatsapp_contact_consents(id,enquiry_id,purpose,channel,consent_version,granted_at) values(?,?,'SERVICE_UPDATES','WEBSITE','2026-10',now())",UUID.randomUUID(),enquiry.id);
        mail.queueProject(null,enquiry.id,enquiry.email,enquiry.publicId.toString(),"ENQUIRY",enquiry.id,"Your printing enquiry has been received. Our team will review it and contact you if more information or artwork is needed.");
        notifications.team(enquiry.id,"NEW_ENQUIRY",enquiry.id,"A new enquiry has arrived. Open the staff workspace to review and accept or reject it.");
        return new CreatedEnquiry(enquiry.publicId, token, enquiry.status.name(), enquiry.createdAt);
    }
    @Transactional(readOnly=true)
    public TrackingView track(UUID publicId, String token) {
        Enquiry enquiry=repository.findByPublicId(publicId).orElseThrow(NoSuchElementException::new);
        if (!TokenUtil.matches(token,enquiry.trackingTokenSha256)) throw new NoSuchElementException();
        List<EventView> history=events.findByEnquiryIdOrderByCreatedAtAsc(enquiry.id).stream()
            .map(e -> new EventView(e.toStatus,e.createdAt)).toList();
        return new TrackingView(enquiry.publicId,enquiry.productCode,enquiry.quantity,
            enquiry.status.name(),enquiry.createdAt,history,enquiry.closureKind,enquiry.closureMessage);
    }
    @Transactional(readOnly=true)
    public List<DemoEnquiry> demoList() {
        return repository.findTop100ByOrderByCreatedAtDesc().stream().map(this::toDemo).toList();
    }
    @Transactional
    public DemoEnquiry demoUpdate(UUID id, EnquiryStatus next) {
        Enquiry enquiry=repository.findForUpdate(id).orElseThrow(NoSuchElementException::new);
        updateStatus(enquiry,next);
        return toDemo(enquiry);
    }
    void updateStatus(Enquiry enquiry, EnquiryStatus next) {
        EnquiryStatus prev=enquiry.status;
        if (prev==next) return;
        if (prev==EnquiryStatus.CLOSED || next.ordinal()<prev.ordinal())
            throw new IllegalArgumentException("Invalid status transition");
        if (prev!=next) {
            enquiry.status=next;enquiry.updatedAt=Instant.now();
            events.save(EnquiryEvent.of(enquiry,prev,next,"STATUS_CHANGED"));
        }
    }
    private DemoEnquiry toDemo(Enquiry e) {
        return new DemoEnquiry(e.id,e.publicId,e.customerName,e.email,e.phone,e.productCode,e.quantity,
            e.size,e.paperGsm,e.printSides,e.finish,e.notes,e.status.name(),e.createdAt);
    }
}
