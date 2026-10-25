package com.printflow.files;

import java.util.List;
import java.util.UUID;

/** Provider boundary. Implementations receive only structured document metadata and are never workflow authorities. */
public interface DesignAssistantProvider {
    record Suggestion(String id,String provider,String title,String rationale,String pageId,String layerId,String field,String before,String after) {}
    List<Suggestion> suggest(PrintDesign.Document document);
}
