package com.printflow.files;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Versioned, renderer-independent print design schema. */
public final class PrintDesign {
    private PrintDesign() {}

    public record Element(
            @NotBlank @Size(max=80) String id,
            @NotBlank @Pattern(regexp="TEXT|RECTANGLE|ELLIPSE|IMAGE") String type,
            @DecimalMin("-100.0") @DecimalMax("2000.0") double xMm,
            @DecimalMin("-100.0") @DecimalMax("2000.0") double yMm,
            @DecimalMin("1.0") @DecimalMax("2000.0") double widthMm,
            @DecimalMin("1.0") @DecimalMax("2000.0") double heightMm,
            @Min(-360) @Max(360) int rotationDegrees,
            @Size(max=2000) String text,
            @Min(6) @Max(144) int fontSizePt,
            @NotBlank @Pattern(regexp="#[0-9a-fA-F]{6}") String fill,
            @NotBlank @Pattern(regexp="#[0-9a-fA-F]{6}") String stroke,
            @DecimalMin("0.0") @DecimalMax("20.0") double strokeMm,
            UUID assetId,
            @Pattern(regexp="LIBERATION_SANS") String fontFamily,
            @Pattern(regexp="REGULAR") String fontStyle,
            @Pattern(regexp="LEFT|CENTER|RIGHT") String textAlignment,
            @DecimalMin("0.8") @DecimalMax("2.0") Double lineSpacing,
            @DecimalMin("-2.0") @DecimalMax("10.0") Double letterSpacingPt,
            @Pattern(regexp="STRETCH|CONTAIN|COVER") String imageFit,
            @DecimalMin("0.0") @DecimalMax("1.0") Double opacity) {
        public Element(String id,String type,double xMm,double yMm,double widthMm,double heightMm,int rotationDegrees,String text,int fontSizePt,String fill,String stroke,double strokeMm,UUID assetId) {
            this(id,type,xMm,yMm,widthMm,heightMm,rotationDegrees,text,fontSizePt,fill,stroke,strokeMm,assetId,"LIBERATION_SANS","REGULAR","LEFT",1.25,0.0,"STRETCH",1.0);
        }
        public double effectiveLineSpacing(){return lineSpacing==null||lineSpacing<=0?1.25:lineSpacing;}
        public double effectiveLetterSpacing(){return letterSpacingPt==null?0.0:letterSpacingPt;}
        public String effectiveTextAlignment(){return textAlignment==null?"LEFT":textAlignment;}
        public String effectiveImageFit(){return imageFit==null?"STRETCH":imageFit;}
        public double effectiveOpacity(){return opacity==null?1.0:opacity;}
    }

    public record Page(
            @NotBlank @Size(max=80) String id,
            @NotBlank @Size(max=120) String name,
            @Min(40) @Max(1000) int widthMm,
            @Min(40) @Max(1000) int heightMm,
            @DecimalMin("0.0") @DecimalMax("20.0") double bleedMm,
            @DecimalMin("0.0") @DecimalMax("50.0") double safeMarginMm,
            @NotNull @Size(max=100) List<@NotNull @Valid Element> elements) {}

    /**
     * V1 fields remain as a compatibility projection of the active page.
     * V2 persists the complete page list and active page identifier. This lets
     * older stored V1 JSON be read and deterministically migrated on first read.
     */
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Document(
            @Min(1) @Max(2) int schemaVersion,
            @NotBlank @Size(max=80) String templateId,
            @Min(40) @Max(1000) int widthMm,
            @Min(40) @Max(1000) int heightMm,
            @DecimalMin("0.0") @DecimalMax("20.0") double bleedMm,
            @DecimalMin("0.0") @DecimalMax("50.0") double safeMarginMm,
            @NotNull @Size(max=100) List<@NotNull @Valid Element> elements,
            @Size(max=100) List<@NotNull @Valid Page> pages,
            @Size(max=80) String activePageId) {
        public Document(int schemaVersion,String templateId,int widthMm,int heightMm,double bleedMm,double safeMarginMm,List<Element> elements) {
            this(schemaVersion,templateId,widthMm,heightMm,bleedMm,safeMarginMm,elements,null,null);
        }
        public static Document v2(String templateId,List<Page> pages,String activePageId) {
            Page active=pages.stream().filter(p->p.id().equals(activePageId)).findFirst().orElse(pages.getFirst());
            return new Document(2,templateId,active.widthMm(),active.heightMm(),active.bleedMm(),active.safeMarginMm(),active.elements(),pages,active.id());
        }
        public Document asV2() {
            if(schemaVersion==2 && pages!=null && !pages.isEmpty()) return this;
            Page first=new Page("page-1","Page 1",widthMm,heightMm,bleedMm,safeMarginMm,elements);
            return v2(templateId,List.of(first),first.id());
        }
        public List<Page> normalizedPages() { return asV2().pages(); }
    }

    public record SaveRequest(@Min(0) long expectedRevision,@NotNull @Valid Document document) {}
    public record ExportRequest(@Min(1) long expectedDraftRevision,@Min(0) int expectedProofRevision,@NotBlank @Size(max=2000) String summary) {}
    public record Draft(long revision,Document document,String updatedAt) {}
}
