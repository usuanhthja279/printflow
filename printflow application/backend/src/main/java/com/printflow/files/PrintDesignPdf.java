package com.printflow.files;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Component;

/** Exports validated structured editor data to a static, one-page PDF. */
@Component
public class PrintDesignPdf {
    private static final float PT_PER_MM=72f/25.4f;
    private static final long MAX_IMAGE_PIXELS=12_500_000L;

    static java.io.InputStream openBundledFont(){return PrintDesignPdf.class.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf");}

    public byte[] create(PrintDesign.Document design,Function<UUID,byte[]> imageResolver)throws IOException {
        validate(design);
        try(var document=new PDDocument();var output=new ByteArrayOutputStream();var fontStream=openBundledFont()){
            PDFont font=fontStream==null?new PDType1Font(Standard14Fonts.FontName.HELVETICA):PDType0Font.load(document,fontStream);
            float bleed=(float)design.bleedMm()*PT_PER_MM;
            float pageWidth=(float)(design.widthMm()+2*design.bleedMm())*PT_PER_MM;
            float pageHeight=(float)(design.heightMm()+2*design.bleedMm())*PT_PER_MM;
            var page=new PDPage(new PDRectangle(pageWidth,pageHeight));document.addPage(page);
            document.getDocumentCatalog().setLanguage("en-IN");
            document.getDocumentInformation().setTitle("J.D.Printex artwork proof");
            document.getDocumentInformation().setSubject("Static export from a versioned local print-design draft");
            Map<UUID,PDImageXObject> images=new HashMap<>();Set<UUID> seen=new HashSet<>();long totalPixels=0,totalBytes=0;
            for(var element:design.elements())if("IMAGE".equals(element.type())&&seen.add(element.assetId())){
                byte[] bytes=imageResolver.apply(element.assetId());
                if(bytes==null||bytes.length==0||bytes.length>ArtifactPolicy.MAX_BYTES)throw new IllegalArgumentException("An image layer is unavailable or too large");
                totalBytes+=bytes.length;if(totalBytes>ArtifactPolicy.MAX_BYTES)throw new IllegalArgumentException("The combined image layers exceed the export limit");
                BufferedImage image=ImageIO.read(new ByteArrayInputStream(bytes));
                if(image==null||(long)image.getWidth()*image.getHeight()>MAX_IMAGE_PIXELS)throw new IllegalArgumentException("An image layer cannot be safely decoded");
                totalPixels+=(long)image.getWidth()*image.getHeight();if(totalPixels>25_000_000L)throw new IllegalArgumentException("The combined image layers exceed the safe pixel limit");
                images.put(element.assetId(),LosslessFactory.createFromImage(document,image));
            }
            try(var canvas=new PDPageContentStream(document,page)){
                canvas.setNonStrokingColor(Color.WHITE);canvas.addRect(0,0,pageWidth,pageHeight);canvas.fill();
                for(var element:design.elements())draw(canvas,pageHeight,bleed,font,element,images);
            }
            document.save(output);byte[] result=output.toByteArray();
            if(result.length>ArtifactPolicy.MAX_BYTES)throw new IllegalArgumentException("The exported proof exceeds the 20 MiB file limit");
            return result;
        }
    }

    private void draw(PDPageContentStream canvas,float pageHeight,float bleed,PDFont font,PrintDesign.Element element,Map<UUID,PDImageXObject> images)throws IOException{
        float x=(float)element.xMm()*PT_PER_MM+bleed,y=(float)element.yMm()*PT_PER_MM+bleed,w=(float)element.widthMm()*PT_PER_MM,h=(float)element.heightMm()*PT_PER_MM;
        float left=x,top=y,rotation=element.rotationDegrees();
        // SVG uses a y-down coordinate system, so positive angles turn clockwise.
        // PDF uses y-up coordinates: negate the angle to preserve that visual direction.
        if(rotation!=0){canvas.saveGraphicsState();float cx=x+w/2,cy=pageHeight-(y+h/2);double angle=Math.toRadians(-rotation),cos=Math.cos(angle),sin=Math.sin(angle);canvas.transform(new Matrix((float)cos,(float)sin,(float)-sin,(float)cos,(float)(cx-cos*cx+sin*cy),(float)(cy-sin*cx-cos*cy)));}
        try{
            if("TEXT".equals(element.type())){
                // x/y in the document are the top-left in mm. Convert once to PDF points;
                // the previous implementation multiplied this point value by PT_PER_MM a
                // second time, which moved text far from its SVG position.
                float topPt=top;
                float ascent=font.getFontDescriptor().getAscent()/1000f*element.fontSizePt();
                float baseline=pageHeight-topPt-ascent;
                float lineHeight=element.fontSizePt()*1.25f;
                int visibleLines=Math.max(1,(int)Math.floor(h/lineHeight));
                var lines=wrap(font,element.text(),element.fontSizePt(),w);
                canvas.saveGraphicsState();canvas.addRect(left,pageHeight-(topPt+h),w,h);canvas.clip();
                canvas.beginText();canvas.setFont(font,element.fontSizePt());canvas.setNonStrokingColor(color(element.fill()));
                canvas.newLineAtOffset(left,baseline);
                for(int i=0;i<Math.min(lines.size(),visibleLines);i++){if(i>0)canvas.newLineAtOffset(0,-lineHeight);canvas.showText(lines.get(i));}
                canvas.endText();
                canvas.restoreGraphicsState();
            }else if("IMAGE".equals(element.type())){
                PDImageXObject image=images.get(element.assetId());if(image==null)throw new IllegalArgumentException("Image layer reference was not resolved");
                canvas.drawImage(image,left,pageHeight-(top+h),w,h);
            }else{
                canvas.setLineWidth((float)element.strokeMm()*PT_PER_MM);canvas.setStrokingColor(color(element.stroke()));canvas.setNonStrokingColor(color(element.fill()));
                if("ELLIPSE".equals(element.type()))ellipse(canvas,left,pageHeight-(top+h),w,h);
                else canvas.addRect(left,pageHeight-(top+h),w,h);
                if(element.strokeMm()>0)canvas.fillAndStroke();else canvas.fill();
            }
        }finally{if(rotation!=0)canvas.restoreGraphicsState();}
    }

    private java.util.List<String> wrap(PDFont font,String text,float fontSize,float maxWidthPt){
        try{
            var result=new java.util.ArrayList<String>();
            if(font instanceof PDType0Font embedded){for(int offset=0;offset<text.length();){int cp=text.codePointAt(offset);if(!embedded.hasGlyph(cp))throw new IllegalArgumentException("Text contains a character unsupported by the print proof font");offset+=Character.charCount(cp);}}
            String[] paragraphs=(text==null?"":text).split("\\R",-1);
            for(String paragraph:paragraphs){
                if(paragraph.isEmpty()){result.add("");continue;}
                String line="";
                for(String word:paragraph.split("\\s+")){
                    String candidate=line.isEmpty()?word:line+" "+word;
                    if(font.getStringWidth(candidate)*fontSize/1000f<=maxWidthPt){line=candidate;continue;}
                    if(!line.isEmpty()){result.add(line);line="";}
                    String part="";
                    for(int offset=0;offset<word.length();){int cp=word.codePointAt(offset);String character=new String(Character.toChars(cp));String next=part+character;
                        if(!part.isEmpty()&&font.getStringWidth(next)*fontSize/1000f>maxWidthPt){result.add(part);part=character;}else part=next;
                        offset+=Character.charCount(cp);
                    }
                    line=part;
                }
                result.add(line);
            }
            return result;
        }catch(IOException unsupported){throw new IllegalArgumentException("Text contains a character unsupported by the print proof font",unsupported);}
    }

    private void ellipse(PDPageContentStream stream,float x,float y,float w,float h)throws IOException{
        float k=.55228475f,rx=w/2,ry=h/2,cx=x+rx,cy=y+ry;
        stream.moveTo(cx+rx,cy);stream.curveTo(cx+rx,cy+k*ry,cx+k*rx,cy+ry,cx,cy+ry);stream.curveTo(cx-k*rx,cy+ry,cx-rx,cy+k*ry,cx-rx,cy);stream.curveTo(cx-rx,cy-k*ry,cx-k*rx,cy-ry,cx,cy-ry);stream.curveTo(cx+k*rx,cy-ry,cx+rx,cy-k*ry,cx+rx,cy);
    }

    private Color color(String value){if(value==null||!value.matches("#[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Use a six-digit RGB color");return new Color(Integer.parseInt(value.substring(1),16));}
    private void validate(PrintDesign.Document design){
        if(design==null||design.schemaVersion()!=1||design.elements()==null||design.elements().size()>100)throw new IllegalArgumentException("Unsupported print-design document");
        if(design.widthMm()<40||design.heightMm()<40||design.widthMm()>1000||design.heightMm()>1000||design.bleedMm()<0||design.bleedMm()>20||design.safeMarginMm()<0||design.safeMarginMm()>Math.min(design.widthMm(),design.heightMm())/2.0)throw new IllegalArgumentException("Canvas size, bleed or safe margin is outside the supported range");
        Set<String> ids=new HashSet<>();for(var e:design.elements()){
            if(e==null||e.id()==null||!ids.add(e.id())||e.type()==null||!Set.of("TEXT","RECTANGLE","ELLIPSE","IMAGE").contains(e.type())||e.widthMm()<1||e.heightMm()<1||Math.abs(e.xMm())>2000||Math.abs(e.yMm())>2000||e.rotationDegrees()<-360||e.rotationDegrees()>360)throw new IllegalArgumentException("A design layer is invalid");
            if("IMAGE".equals(e.type())){if(e.assetId()==null)throw new IllegalArgumentException("Choose a scanned image asset for every image layer");}
            else if(e.assetId()!=null)throw new IllegalArgumentException("Only image layers can reference an asset");
            if("TEXT".equals(e.type())&&(e.text()==null||e.text().isBlank()||e.text().length()>2000))throw new IllegalArgumentException("Text layers need readable text");
            color(e.fill());color(e.stroke());
        }
    }
}
