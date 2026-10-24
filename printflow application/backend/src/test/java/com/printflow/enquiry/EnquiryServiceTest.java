package com.printflow.enquiry;

import com.printflow.enquiry.EnquiryDtos.CreateEnquiry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnquiryServiceTest {
    @Test void createsAndPersistsNewEnquiry() {
        EnquiryRepository repository=mock(EnquiryRepository.class);
        EnquiryEventRepository events=mock(EnquiryEventRepository.class);
        EnquiryService service=new EnquiryService(repository,events,mock(com.printflow.catalog.ProductTemplates.class),mock(com.printflow.identity.CustomerMail.class),mock(com.printflow.identity.ProjectNotifications.class));
        var result=service.create(new CreateEnquiry("Example", "hello@example.com", "9876543210",
            "BROCHURES",5000,"A4",170,"DOUBLE","MATTE","Needed next week"));
        assertEquals("NEW",result.status());
        assertNotNull(result.publicId());
        assertTrue(result.trackingToken().length()>30);
        verify(repository).save(any(Enquiry.class));
        verify(events).save(any(EnquiryEvent.class));
    }
    @Test void rejectsUnrecognisedProduct() {
        EnquiryService service=new EnquiryService(mock(EnquiryRepository.class),mock(EnquiryEventRepository.class),mock(com.printflow.catalog.ProductTemplates.class),mock(com.printflow.identity.CustomerMail.class),mock(com.printflow.identity.ProjectNotifications.class));
        assertThrows(IllegalArgumentException.class,()->service.create(new CreateEnquiry("Example",
            "hello@example.com","9876543210","BOGUS",5000,null,null,null,null,null)));
    }
    @Test void repeatedStatusUpdatesAreIdempotentIncludingClosed() {
        for (EnquiryStatus status : EnquiryStatus.values()) {
            EnquiryRepository repository=mock(EnquiryRepository.class);
            EnquiryEventRepository events=mock(EnquiryEventRepository.class);
            EnquiryService service=new EnquiryService(repository,events,mock(com.printflow.catalog.ProductTemplates.class),mock(com.printflow.identity.CustomerMail.class),mock(com.printflow.identity.ProjectNotifications.class));
            Enquiry enquiry=new Enquiry();
            enquiry.id=UUID.randomUUID(); enquiry.status=status;
            when(repository.findForUpdate(enquiry.id)).thenReturn(Optional.of(enquiry));

            assertEquals(status.name(),service.demoUpdate(enquiry.id,status).status());
            assertNull(enquiry.updatedAt);
            verifyNoInteractions(events);
        }
    }
    @Test void statusTransitionsAllowForwardChangesAndRejectReopeningOrBackwardsChanges() {
        for (EnquiryStatus previous : EnquiryStatus.values()) {
            for (EnquiryStatus next : EnquiryStatus.values()) {
                if (previous==next) continue;
                EnquiryRepository repository=mock(EnquiryRepository.class);
                EnquiryEventRepository events=mock(EnquiryEventRepository.class);
                EnquiryService service=new EnquiryService(repository,events,mock(com.printflow.catalog.ProductTemplates.class),mock(com.printflow.identity.CustomerMail.class),mock(com.printflow.identity.ProjectNotifications.class));
                Enquiry enquiry=new Enquiry();
                enquiry.id=UUID.randomUUID(); enquiry.status=previous;
                when(repository.findForUpdate(enquiry.id)).thenReturn(Optional.of(enquiry));

                if (next.ordinal()<previous.ordinal()) {
                    assertThrows(IllegalArgumentException.class,()->service.demoUpdate(enquiry.id,next));
                    assertEquals(previous,enquiry.status);
                    verifyNoInteractions(events);
                } else {
                    assertEquals(next.name(),service.demoUpdate(enquiry.id,next).status());
                    assertNotNull(enquiry.updatedAt);
                    verify(events).save(argThat(event -> event.enquiry==enquiry
                        && previous.name().equals(event.fromStatus) && next.name().equals(event.toStatus)));
                }
            }
        }
    }
    @Test void wrongOrMissingTrackingTokenDoesNotReadHistory() {
        EnquiryRepository repository=mock(EnquiryRepository.class);
        EnquiryEventRepository events=mock(EnquiryEventRepository.class);
        EnquiryService service=new EnquiryService(repository,events,mock(com.printflow.catalog.ProductTemplates.class),mock(com.printflow.identity.CustomerMail.class),mock(com.printflow.identity.ProjectNotifications.class));
        Enquiry enquiry=new Enquiry(); enquiry.publicId=UUID.randomUUID();
        enquiry.trackingTokenSha256=TokenUtil.sha256(TokenUtil.newToken());
        when(repository.findByPublicId(enquiry.publicId)).thenReturn(Optional.of(enquiry));

        assertThrows(NoSuchElementException.class,()->service.track(enquiry.publicId,"wrong-token"));
        assertThrows(NoSuchElementException.class,()->service.track(enquiry.publicId,null));
        verifyNoInteractions(events);
    }
}
