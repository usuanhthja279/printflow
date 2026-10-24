package com.printflow.files;
import java.io.InputStream;
import java.util.UUID;
/** No public bucket or upload endpoint is provided by Phase 1. Implementations must enforce ownership,
 * opaque object keys, bounded streaming, private storage, quarantine/scan and expiring authorized reads. */
public interface PrivateArtifactStore {
 UUID quarantine(UUID actorId,UUID enquiryId,String displayName,String mediaType,long declaredSize,InputStream boundedContent);
 InputStream openClean(UUID actorId,UUID artifactId);
}
