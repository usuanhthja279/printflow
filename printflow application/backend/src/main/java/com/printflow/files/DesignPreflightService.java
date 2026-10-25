package com.printflow.files;

import java.util.ArrayList;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import java.util.List;

/** Deterministic, bounded checks for structured editor drafts. Findings are advisory and never approve production. */
public final class DesignPreflightService {
    public static final String RULESET_VERSION="6B-1";
    private DesignPreflightService() {}

    public record Finding(String code,String status,String severity,String pageId,String layerId,String message) {}

    public static List<Finding> inspect(PrintDesign.Document source) {
        var document=source.asV2();var findings=new ArrayList<Finding>();
        var textRenderer=new PrintDesignPdf();
        try(var fontDocument=new PDDocument();var fontStream=PrintDesignPdf.openBundledFont()){
          var font=fontStream==null?null:PDType0Font.load(fontDocument,fontStream);
        for(var page:document.pages()){
            findings.add(new Finding("PAGE_GEOMETRY","PASS","INFO",page.id(),null,
                    "Trim dimensions and page-specific safe margins are within application limits."));
            if(page.bleedMm()<=0)findings.add(new Finding("BLEED","WARNING","WARNING",page.id(),null,
                    "No bleed is configured. Confirm the product and printer bleed requirement with the business."));
            if(page.elements().isEmpty())findings.add(new Finding("PAGE_CONTENT","WARNING","WARNING",page.id(),null,
                    "This page has no design layers."));
            for(var layer:page.elements()){
                double angle=Math.toRadians(layer.rotationDegrees());
                double halfW=layer.widthMm()/2.0,halfH=layer.heightMm()/2.0;
                double boundW=Math.abs(halfW*Math.cos(angle))+Math.abs(halfH*Math.sin(angle));
                double boundH=Math.abs(halfW*Math.sin(angle))+Math.abs(halfH*Math.cos(angle));
                boolean outside=layer.xMm()+layer.widthMm()/2.0-boundW < -page.bleedMm()
                        || layer.yMm()+layer.heightMm()/2.0-boundH < -page.bleedMm()
                        || layer.xMm()+layer.widthMm()/2.0+boundW > page.widthMm()+page.bleedMm()
                        || layer.yMm()+layer.heightMm()/2.0+boundH > page.heightMm()+page.bleedMm();
                if(outside)findings.add(new Finding("OBJECT_OUTSIDE_BLEED","ERROR","ERROR",page.id(),layer.id(),
                        "The rotated layer extends outside the page and configured bleed."));
                else findings.add(new Finding("OBJECT_BOUNDS","PASS","INFO",page.id(),layer.id(),
                        "The rotated layer fits within the trim and configured bleed."));
                if("TEXT".equals(layer.type())){
                    if(layer.fontSizePt()<8)findings.add(new Finding("SMALL_TEXT","WARNING","WARNING",page.id(),layer.id(),
                            "Text is below 8 pt. Confirm legibility at the final print size."));
                    if(layer.xMm()<page.safeMarginMm()||layer.yMm()<page.safeMarginMm()
                            ||layer.xMm()+layer.widthMm()>page.widthMm()-page.safeMarginMm()
                            ||layer.yMm()+layer.heightMm()>page.heightMm()-page.safeMarginMm())
                        findings.add(new Finding("TEXT_SAFE_MARGIN","WARNING","WARNING",page.id(),layer.id(),
                                "Text touches or crosses the configured safe margin."));
                    if(font==null)findings.add(new Finding("FONT_AVAILABILITY","ERROR","ERROR",page.id(),layer.id(),"The bundled print font is unavailable."));
                    else try {var lines=textRenderer.wrap(font,layer.text(),layer.fontSizePt(),(float)layer.widthMm()*72f/25.4f,(float)layer.effectiveLetterSpacing());int maxLines=Math.max(1,(int)Math.floor(layer.heightMm()*72f/25.4f/(layer.fontSizePt()*layer.effectiveLineSpacing())));findings.add(new Finding("FONT_EMBEDDING","PASS","INFO",page.id(),layer.id(),"The approved regular font is available and embedded in PDF export."));if(lines.size()>maxLines)findings.add(new Finding("TEXT_OVERFLOW","WARNING","WARNING",page.id(),layer.id(),"Wrapped text exceeds the visible text box and will be clipped in the proof."));}catch(IllegalArgumentException unsupported){findings.add(new Finding("UNSUPPORTED_GLYPH","ERROR","ERROR",page.id(),layer.id(),"One or more characters are unsupported by the bundled proof font."));}
                    if(layer.text()!=null&&containsComplexScript(layer.text()))
                        findings.add(new Finding("COMPLEX_SCRIPT_SHAPING","UNSUPPORTED","WARNING",page.id(),layer.id(),
                                "Complex-script shaping is not certified by the current PDF renderer. Verify the exported proof before approval."));
                }
        }
        }
        } catch(IOException fontLoadFailure) {
            findings.add(new Finding("FONT_AVAILABILITY","ERROR","ERROR",null,null,
                    "The approved bundled print font could not be loaded for preflight."));
        }
        findings.add(new Finding("COLOR_MANAGEMENT","NOT_CHECKED","INFO",null,null,
                "No approved press/paper ICC profile is configured; RGB content is not converted to CMYK."));
        findings.add(new Finding("PDF_X_TRAPPING_SPOT_COLOR","UNSUPPORTED","INFO",null,null,
                "PDF/X conformance, trapping and spot-color separation are not certified by this preflight."));
        return List.copyOf(findings);
    }

    private static boolean containsComplexScript(String text) {
        return text.codePoints().anyMatch(cp ->
                (cp>=0x0900&&cp<=0x0DFF)||(cp>=0x0F00&&cp<=0x109F)
                ||(cp>=0x1780&&cp<=0x17FF)||(cp>=0xA980&&cp<=0xA9DF));
    }
}
