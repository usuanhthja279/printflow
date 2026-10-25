package com.printflow.files;

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

/** Versioned, renderer-independent representation of a one-page print design. */
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
            UUID assetId) {}

    public record Document(
            @Min(1) @Max(1) int schemaVersion,
            @NotBlank @Size(max=80) String templateId,
            @Min(40) @Max(1000) int widthMm,
            @Min(40) @Max(1000) int heightMm,
            @DecimalMin("0.0") @DecimalMax("20.0") double bleedMm,
            @DecimalMin("0.0") @DecimalMax("50.0") double safeMarginMm,
            @NotNull @Size(max=100) List<@NotNull @Valid Element> elements) {}

    public record SaveRequest(@Min(0) long expectedRevision,@NotNull @Valid Document document) {}
    public record ExportRequest(@Min(1) long expectedDraftRevision,@Min(0) int expectedProofRevision,@NotBlank @Size(max=2000) String summary) {}
    public record Draft(long revision,Document document,String updatedAt) {}
}
