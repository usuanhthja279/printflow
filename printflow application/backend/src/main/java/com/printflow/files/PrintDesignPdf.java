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
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Component;

/** Exports validated structured editor data to a static multipage PDF. */
@Component
public class PrintDesignPdf {
    private static final float PT_PER_MM=72f/25.4f;
    private static final long MAX_IMAGE_PIXELS=12_500_000L;
    private static final int MAX_IMAGE_DIMENSION=12000;

    static java.io.InputStream openBundledFont(){return PrintDesignPdf.class.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf");}

    public byte[] create(PrintDesign.Document source,Function<UUID,byte[]> imageResolver)throws IOException {
        if(source==null)throw new IllegalArgumentException("Design document is required");
        PrintDesign.Document design=source.asV2();
        validate(design);
        try(var document=new PDDocument();var output=new ByteArrayOutputStream();var fontStream=openBundledFont()){
            if(fontStream==null)throw new IllegalStateException("The licensed bundled print font is unavailable");PDFont font=PDType0Font.load(document,fontStream);
            document.getDocumentCatalog().setLanguage("en-IN");
            document.getDocumentInformation().setTitle("J.D.Printex artwork proof");
            document.getDocumentInformation().setSubject("Static multipage export from a versioned local print-design draft");
            Map<UUID,PDImageXObject> images=new HashMap<>();Set<UUID> seen=new HashSet<>();long totalPixels=0,totalBytes=0;
            for(var page:design.pages())for(var element:page.elements())if("IMAGE".equals(element.type())&&seen.add(element.assetId())){
                byte[] bytes=imageResolver.apply(element.assetId());
                if(bytes==null||bytes.length==0||bytes.length>ArtifactPolicy.MAX_BYTES)throw new IllegalArgumentException("An image layer is unavailable or too large");
                totalBytes+=bytes.length;if(totalBytes>ArtifactPolicy.MAX_BYTES)throw new IllegalArgumentException("The combined image layers exceed the export limit");
                BufferedImage image=readBoundedImage(bytes);
                totalPixels+=(long)image.getWidth()*image.getHeight();if(totalPixels>25_000_000L)throw new IllegalArgumentException("The combined image layers exceed the safe pixel limit");
                images.put(element.assetId(),LosslessFactory.createFromImage(document,image));
            }
            for(var designPage:design.pages()){
                float bleed=(float)designPage.bleedMm()*PT_PER_MM;
                float pageWidth=(float)(designPage.widthMm()+2*designPage.bleedMm())*PT_PER_MM;
                float pageHeight=(float)(designPage.heightMm()+2*designPage.bleedMm())*PT_PER_MM;
                var page=new PDPage(new PDRectangle(pageWidth,pageHeight));document.addPage(page);
                page.setTrimBox(new PDRectangle(bleed,bleed,pageWidth-2*bleed,pageHeight-2*bleed));
                page.setBleedBox(new PDRectangle(0,0,pageWidth,pageHeight));
                page.setCropBox(new PDRectangle(0,0,pageWidth,pageHeight));
                page.setMediaBox(new PDRectangle(pageWidth,pageHeight));
                try(var canvas=new PDPageContentStream(document,page)){
                    canvas.setNonStrokingColor(Color.WHITE);canvas.addRect(0,0,pageWidth,pageHeight);canvas.fill();
                    for(var element:designPage.elements())draw(canvas,pageHeight,bleed,font,element,images);
                }
            }
            document.save(output);byte[] result=output.toByteArray();
            if(result.length>ArtifactPolicy.MAX_BYTES)throw new IllegalArgumentException("The exported proof exceeds the 20 MiB file limit");
            return result;
        }
    }

    record ImageFit(float x,float y,float width,float height) {}
    static ImageFit imageFit(float frameWidth,float frameHeight,int sourceWidth,int sourceHeight,String mode){
        if(frameWidth<=0||frameHeight<=0||sourceWidth<=0||sourceHeight<=0)throw new IllegalArgumentException("Image and frame dimensions must be positive");
        if("STRETCH".equals(mode)||mode==null)return new ImageFit(0,0,frameWidth,frameHeight);
        double scale="CONTAIN".equals(mode)?Math.min(frameWidth/sourceWidth,frameHeight/sourceHeight):"COVER".equals(mode)?Math.max(frameWidth/sourceWidth,frameHeight/sourceHeight):Double.NaN;
        if(Double.isNaN(scale))throw new IllegalArgumentException("Unsupported image fitting mode");
        float width=(float)(sourceWidth*scale),height=(float)(sourceHeight*scale);
        return new ImageFit((frameWidth-width)/2,(frameHeight-height)/2,width,height);
    }
    private void draw(PDPageContentStream canvas,float pageHeight,float bleed,PDFont font,PrintDesign.Element element,Map<UUID,PDImageXObject> images)throws IOException{
        float x=(float)element.xMm()*PT_PER_MM+bleed,y=(float)element.yMm()*PT_PER_MM+bleed,w=(float)element.widthMm()*PT_PER_MM,h=(float)element.heightMm()*PT_PER_MM;
        float left=x,top=y,rotation=element.rotationDegrees();boolean alpha=element.effectiveOpacity()<0.999;
        if(alpha){canvas.saveGraphicsState();var state=new PDExtendedGraphicsState();state.setNonStrokingAlphaConstant((float)element.effectiveOpacity());state.setStrokingAlphaConstant((float)element.effectiveOpacity());canvas.setGraphicsStateParameters(state);}
        if(rotation!=0){canvas.saveGraphicsState();float cx=x+w/2,cy=pageHeight-(y+h/2);double angle=Math.toRadians(-rotation),cos=Math.cos(angle),sin=Math.sin(angle);canvas.transform(new Matrix((float)cos,(float)sin,(float)-sin,(float)cos,(float)(cx-cos*cx+sin*cy),(float)(cy-sin*cx-cos*cy)));}
        try{
            if("TEXT".equals(element.type())){
                float ascent=font.getFontDescriptor().getAscent()/1000f*element.fontSizePt();
                float baseline=pageHeight-top-ascent;
                float lineHeight=(float)(element.fontSizePt()*element.effectiveLineSpacing());
                int visibleLines=Math.max(1,(int)Math.floor(h/lineHeight));
                var lines=wrap(font,element.text(),element.fontSizePt(),w,(float)element.effectiveLetterSpacing());
                canvas.saveGraphicsState();canvas.addRect(left,pageHeight-(top+h),w,h);canvas.clip();
                canvas.beginText();canvas.setFont(font,element.fontSizePt());canvas.setNonStrokingColor(color(element.fill()));canvas.setCharacterSpacing((float)element.effectiveLetterSpacing());
                for(int i=0;i<Math.min(lines.size(),visibleLines);i++){
                    String line=lines.get(i);float textWidth=(float)(font.getStringWidth(line)*element.fontSizePt()/1000f+Math.max(0,line.codePointCount(0,line.length())-1)*element.effectiveLetterSpacing());
                    float shift=switch(element.effectiveTextAlignment()){case "CENTER"->(w-textWidth)/2;case "RIGHT"->w-textWidth;default->0;};
                    canvas.setTextMatrix(Matrix.getTranslateInstance(left+shift,baseline-i*lineHeight));canvas.showText(line);
                }
                canvas.endText();canvas.restoreGraphicsState();
            }else if("IMAGE".equals(element.type())){
                PDImageXObject image=images.get(element.assetId());if(image==null)throw new IllegalArgumentException("Image layer reference was not resolved");
                canvas.saveGraphicsState();canvas.addRect(left,pageHeight-(top+h),w,h);canvas.clip();
                ImageFit fit=imageFit(w,h,image.getWidth(),image.getHeight(),element.effectiveImageFit());
                float drawW=fit.width(),drawH=fit.height(),drawX=left+fit.x(),drawY=top+fit.y();
                canvas.drawImage(image,drawX,pageHeight-(drawY+drawH),drawW,drawH);canvas.restoreGraphicsState();
            }else{
                canvas.setLineWidth((float)element.strokeMm()*PT_PER_MM);canvas.setStrokingColor(color(element.stroke()));canvas.setNonStrokingColor(color(element.fill()));
                if("ELLIPSE".equals(element.type()))ellipse(canvas,left,pageHeight-(top+h),w,h);
                else canvas.addRect(left,pageHeight-(top+h),w,h);
                if(element.strokeMm()>0)canvas.fillAndStroke();else canvas.fill();
            }
        }finally{if(rotation!=0)canvas.restoreGraphicsState();if(alpha)canvas.restoreGraphicsState();}
    }
    java.util.List<String> wrap(PDFont font,String text,float fontSize,float maxWidthPt){return wrap(font,text,fontSize,maxWidthPt,0);}
    java.util.List<String> wrap(PDFont font,String text,float fontSize,float maxWidthPt,float letterSpacingPt){
        try{
            var result=new java.util.ArrayList<String>();
            if(font instanceof PDType0Font embedded){for(int offset=0;offset<text.length();){int cp=text.codePointAt(offset);if(!embedded.hasGlyph(cp))throw new IllegalArgumentException("Text contains a character unsupported by the print proof font");offset+=Character.charCount(cp);}}
            String[] paragraphs=(text==null?"":text).split("\\R",-1);
            for(String paragraph:paragraphs){
                if(paragraph.isEmpty()){result.add("");continue;}
                String line="";
                for(String word:paragraph.split("\\s+")){
                    String candidate=line.isEmpty()?word:line+" "+word;
                    if(textWidth(font,candidate,fontSize,letterSpacingPt)<=maxWidthPt){line=candidate;continue;}
                    if(!line.isEmpty()){result.add(line);line="";}
                    String part="";
                    for(int offset=0;offset<word.length();){int cp=word.codePointAt(offset);String character=new String(Character.toChars(cp));String next=part+character;
                        if(!part.isEmpty()&&textWidth(font,next,fontSize,letterSpacingPt)>maxWidthPt){result.add(part);part=character;}else part=next;
                        offset+=Character.charCount(cp);
                    }
                    line=part;
                }
                result.add(line);
            }
            return result;
        }catch(IOException unsupported){throw new IllegalArgumentException("Text contains a character unsupported by the print proof font",unsupported);}
    }

    static float textWidth(PDFont font,String text,float fontSize,float letterSpacingPt)throws IOException{
        int characters=text.codePointCount(0,text.length());
        return (float)(font.getStringWidth(text)*fontSize/1000f+Math.max(0,characters-1)*letterSpacingPt);
    }

    static BufferedImage readBoundedImage(byte[] bytes)throws IOException{
        if(bytes==null||bytes.length==0)throw new IllegalArgumentException("Image is empty");
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
            if(input==null)throw new IllegalArgumentException("Image cannot be safely decoded");
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IllegalArgumentException("Image cannot be safely decoded");
            var reader=readers.next();try{
                reader.setInput(input,true,true);int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<=0||height<=0||width>MAX_IMAGE_DIMENSION||height>MAX_IMAGE_DIMENSION
                        ||(long)width*height>MAX_IMAGE_PIXELS)throw new IllegalArgumentException("Image dimensions exceed the safe decode limit");
                var image=reader.read(0);if(image==null||image.getWidth()!=width||image.getHeight()!=height)
                    throw new IllegalArgumentException("Image cannot be safely decoded");
                return image;
            }finally{reader.dispose();}
        }
    }

    private void ellipse(PDPageContentStream stream,float x,float y,float w,float h)throws IOException{
        float k=.55228475f,rx=w/2,ry=h/2,cx=x+rx,cy=y+ry;
        stream.moveTo(cx+rx,cy);stream.curveTo(cx+rx,cy+k*ry,cx+k*rx,cy+ry,cx,cy+ry);stream.curveTo(cx-k*rx,cy+ry,cx-rx,cy+k*ry,cx-rx,cy);stream.curveTo(cx-rx,cy-k*ry,cx-k*rx,cy-ry,cx,cy-ry);stream.curveTo(cx+k*rx,cy-ry,cx+rx,cy-k*ry,cx+rx,cy);
    }

    private Color color(String value){if(value==null||!value.matches("#[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Use a six-digit RGB color");return new Color(Integer.parseInt(value.substring(1),16));}
    private void validate(PrintDesign.Document source){
        if(source==null||source.schemaVersion()<1||source.schemaVersion()>2)throw new IllegalArgumentException("Unsupported print-design document");
        PrintDesign.Document design=source.asV2();
        if(design.pages()==null||design.pages().isEmpty()||design.pages().size()>100)throw new IllegalArgumentException("A design must contain between 1 and 100 pages");
        int totalLayers=0;Set<String> pageIds=new HashSet<>();
        for(var page:design.pages()){
            if(page==null||page.id()==null||!pageIds.add(page.id())||page.name()==null||page.name().isBlank()||page.widthMm()<40||page.heightMm()<40||page.widthMm()>1000||page.heightMm()>1000||page.bleedMm()<0||page.bleedMm()>20||page.safeMarginMm()<0||page.safeMarginMm()>Math.min(page.widthMm(),page.heightMm())/2.0)throw new IllegalArgumentException("A page size, bleed or safe margin is outside the supported range");
            if(page.elements()==null||page.elements().size()>100)throw new IllegalArgumentException("A page may contain at most 100 layers");
            totalLayers+=page.elements().size();if(totalLayers>500)throw new IllegalArgumentException("A design may contain at most 500 layers");
            Set<String> ids=new HashSet<>();for(var e:page.elements()){
                if(e==null||e.id()==null||!ids.add(e.id())||e.type()==null||!Set.of("TEXT","RECTANGLE","ELLIPSE","IMAGE").contains(e.type())||e.widthMm()<1||e.heightMm()<1||Math.abs(e.xMm())>2000||Math.abs(e.yMm())>2000||e.rotationDegrees()<-360||e.rotationDegrees()>360)throw new IllegalArgumentException("A design layer is invalid");
                if("IMAGE".equals(e.type())){if(e.assetId()==null)throw new IllegalArgumentException("Choose a scanned image asset for every image layer");}
                else if(e.assetId()!=null)throw new IllegalArgumentException("Only image layers can reference an asset");
                if("TEXT".equals(e.type())&&(e.text()==null||e.text().isBlank()||e.text().length()>2000))throw new IllegalArgumentException("Text layers need readable text");
                if(e.fontFamily()!=null&&!"LIBERATION_SANS".equals(e.fontFamily()))throw new IllegalArgumentException("The selected font is not available for print export");
                if(e.fontStyle()!=null&&!"REGULAR".equals(e.fontStyle()))throw new IllegalArgumentException("Only the licensed regular print font is available");
                if(e.textAlignment()!=null&&!Set.of("LEFT","CENTER","RIGHT").contains(e.textAlignment()))throw new IllegalArgumentException("Unsupported text alignment");
                if(e.imageFit()!=null&&!Set.of("STRETCH","CONTAIN","COVER").contains(e.imageFit()))throw new IllegalArgumentException("Unsupported image fitting mode");
                color(e.fill());color(e.stroke());
            }
        }
    }}
