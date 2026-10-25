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

    public byte[] create(PrintDesign.Document design,Function<UUID,byte[]> imageResolver)throws IOException {
        validate(design);
        try(var document=new PDDocument();var output=new ByteArrayOutputStream();var fontStream=getClass().getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")){
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
        if(rotation!=0){canvas.saveGraphicsState();float cx=x+w/2,cy=pageHeight-(y+h/2);canvas.transform(Matrix.getRotateInstance((float)Math.toRadians(rotation),cx,cy));}
        try{
            if("TEXT".equals(element.type())){
                canvas.beginText();canvas.setFont(font,element.fontSizePt());canvas.setNonStrokingColor(color(element.fill()));
                String[] lines=element.text()==null?new String[]{""}:element.text().split("\\R",-1);
                float baseline=pageHeight-top*PT_PER_MM-element.fontSizePt();
                canvas.newLineAtOffset(left,baseline);
                for(int i=0;i<lines.length;i++){if(i>0)canvas.newLineAtOffset(0,-element.fontSizePt()*1.25f);canvas.showText(lines[i]);}
                canvas.endText();
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
