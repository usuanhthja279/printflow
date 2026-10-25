package com.printflow.files;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

class PrintDesignPdfTest {
    private final PrintDesignPdf exporter=new PrintDesignPdf();

    @Test void exportsTrimAndBleedDimensionsAndTextInLayerOrder() throws Exception {
        var first=text("heading","First layer",12,22);
        var second=text("caption","Second layer",12,55);
        var doc=new PrintDesign.Document(1,"A4_FLYER",210,297,3,5,List.of(first,second));
        byte[] pdf=exporter.create(doc,id->null);
        try(var rendered=Loader.loadPDF(pdf)){
            var page=rendered.getPage(0).getMediaBox();
            assertEquals((210+6)*72f/25.4f,page.getWidth(),.02f);
            assertEquals((297+6)*72f/25.4f,page.getHeight(),.02f);
            String text=new PDFTextStripper().getText(rendered);
            assertTrue(text.indexOf("First layer")<text.indexOf("Second layer"));
        }
    }

    @Test void v1DraftConvertsDeterministicallyAndV2ExportsPageOrderAndPageSizes() throws Exception {
        var layer=text("heading","Page one",8,8);
        var legacy=new PrintDesign.Document(1,"BLANK",90,54,3,5,List.of(layer));
        var converted=legacy.asV2();
        assertEquals(2,converted.schemaVersion());
        assertEquals("page-1",converted.pages().getFirst().id());
        assertEquals(List.of(layer),converted.pages().getFirst().elements());
        var second=new PrintDesign.Page("inside","Inside",148,210,2,6,List.of(text("inside-title","Inside cover",10,12)));
        var multi=PrintDesign.Document.v2("BOOK_COVER",List.of(converted.pages().getFirst(),second),"inside");
        byte[] bytes=exporter.create(multi,id->null);
        try(var pdf=Loader.loadPDF(bytes)){
            assertEquals(2,pdf.getNumberOfPages());
            assertEquals((90+6)*72f/25.4f,pdf.getPage(0).getMediaBox().getWidth(),.02f);
            assertEquals((148+4)*72f/25.4f,pdf.getPage(1).getMediaBox().getWidth(),.02f);
            assertEquals((148+4)*72f/25.4f,pdf.getPage(1).getBleedBox().getWidth(),.02f);
            assertTrue(new PDFTextStripper().getText(pdf).contains("Inside cover"));
        }
    }

    @Test void rejectsMultipageDocumentsOverServerPageLimit() {
        var pages=java.util.stream.IntStream.range(0,101).mapToObj(i->new PrintDesign.Page("p"+i,"Page",90,54,3,5,List.of())).toList();
        var tooMany=PrintDesign.Document.v2("BLANK",pages,"p0");
        assertThrows(IllegalArgumentException.class,()->exporter.create(tooMany,id->null));
    }
    @Test void rejectsMalformedV2DocumentsInsteadOfSilentlyFlatteningThem() {
        var page=new PrintDesign.Page("front","Front",90,54,3,5,List.of(text("title","Front",5,8)));
        var mismatched=new PrintDesign.Document(2,"BUSINESS_CARD",90,54,3,5,List.of(),List.of(page),"front");
        assertThrows(IllegalArgumentException.class,()->exporter.create(mismatched,id->null));
        var duplicate=new PrintDesign.Document(2,"BUSINESS_CARD",90,54,3,5,page.elements(),List.of(page,page),"front");
        assertThrows(IllegalArgumentException.class,()->exporter.create(duplicate,id->null));
        var legacyWithV2Fields=new PrintDesign.Document(1,"BUSINESS_CARD",90,54,3,5,page.elements(),List.of(page),"front");
        assertThrows(IllegalArgumentException.class,()->exporter.create(legacyWithV2Fields,id->null));
    }
    @Test void boundedImageDecoderRejectsOversizedPngHeaderBeforePixelDecode() throws Exception {
        var image=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);var output=new ByteArrayOutputStream();ImageIO.write(image,"png",output);
        byte[] bytes=output.toByteArray();int width=12001;bytes[16]=(byte)(width>>>24);bytes[17]=(byte)(width>>>16);bytes[18]=(byte)(width>>>8);bytes[19]=(byte)width;
        java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(bytes,12,17);long value=crc.getValue();for(int i=0;i<4;i++)bytes[29+i]=(byte)(value>>>(24-i*8));
        assertThrows(IllegalArgumentException.class,()->PrintDesignPdf.readBoundedImage(bytes));
    }
    @Test void positiveAndNegativeLetterSpacingAreIncludedInWidthAndTextWrapping() throws Exception {
        var font=new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
        String text="PRINT SAFE";float plain=PrintDesignPdf.textWidth(font,text,12,0),positive=PrintDesignPdf.textWidth(font,text,12,1.5f),negative=PrintDesignPdf.textWidth(font,text,12,-1.5f);
        assertTrue(positive>plain);assertTrue(negative<plain);
        assertTrue(exporter.wrap(font,text,12,(plain+positive)/2,1.5f).size()>exporter.wrap(font,text,12,(plain+positive)/2,0).size());
        assertTrue(exporter.wrap(font,text,12,(plain+negative)/2,-1.5f).size()<=exporter.wrap(font,text,12,(plain+negative)/2,0).size());
    }
    @Test void imageFitCalculationsPreserveContainAndCenterCropCoverGeometry() {
        var contain=PrintDesignPdf.imageFit(100,100,1600,800,"CONTAIN");
        assertEquals(100,contain.width(),.01f);assertEquals(50,contain.height(),.01f);
        assertEquals(0,contain.x(),.01f);assertEquals(25,contain.y(),.01f);
        var cover=PrintDesignPdf.imageFit(100,100,1600,800,"COVER");
        assertEquals(200,cover.width(),.01f);assertEquals(100,cover.height(),.01f);
        assertEquals(-50,cover.x(),.01f);assertEquals(0,cover.y(),.01f);
        assertThrows(IllegalArgumentException.class,()->PrintDesignPdf.imageFit(100,100,1,1,"INVALID"));
    }
    @Test void embedsOnlyResolvedImageLayersAndRejectsInvalidLayerReferences() throws Exception {
        UUID asset=UUID.randomUUID();var image=new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB);
        image.setRGB(2,2,0xff33aa77);var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);
        var layer=new PrintDesign.Element("image","IMAGE",5,5,35,25,0,null,12,"#ffffff","#183b43",0,asset);
        byte[] pdf=exporter.create(new PrintDesign.Document(1,"BLANK",90,54,3,5,List.of(layer)),id->asset.equals(id)?bytes.toByteArray():null);
        try(var rendered=Loader.loadPDF(pdf)){
            assertTrue(rendered.getPage(0).getResources().getXObjectNames().iterator().hasNext());
        }
        assertThrows(IllegalArgumentException.class,()->exporter.create(new PrintDesign.Document(1,"BLANK",90,54,3,5,List.of(layer)),id->null));
        var duplicate=text("same","one",1,1);
        assertThrows(IllegalArgumentException.class,()->exporter.create(new PrintDesign.Document(1,"BLANK",90,54,3,5,List.of(duplicate,duplicate)),id->null));
    }

    @Test void rejectsGlyphsOutsideTheSharedPrintFontWithAClientReadableValidationError(){
        var emoji=text("unsupported","Print safely 😀",10,10,70,20,12,0);
        var design=new PrintDesign.Document(1,"BLANK",90,54,3,5,List.of(emoji));
        var error=assertThrows(IllegalArgumentException.class,()->exporter.create(design,id->null));
        assertTrue(error.getMessage().contains("unsupported by the print proof font"));
    }

    @Test void textBaselineWrapAndRasterBoundsMatchTheMillimetreEditorCanvas() throws Exception {
        var top=text("top","TOP LINE THAT WRAPS\nSecond line",12,8,34,42,14,0);
        var bottom=text("bottom","BOTTOM",12,266,60,20,10,0);
        var doc=new PrintDesign.Document(1,"A4_FLYER",210,297,3,5,List.of(top,bottom));
        byte[] pdfBytes=exporter.create(doc,id->null);var fixtureDir=java.nio.file.Path.of("build","test-output");java.nio.file.Files.createDirectories(fixtureDir);java.nio.file.Files.write(fixtureDir.resolve("editor-layout-comparison.pdf"),pdfBytes);
        try(var pdf=Loader.loadPDF(pdfBytes)){
            var positions=new Positions();positions.setSortByPosition(true);positions.getText(pdf);
            assertTrue(positions.rows.stream().anyMatch(p->p.text.startsWith("TOP")));
            assertTrue(positions.rows.stream().anyMatch(p->p.text.startsWith("BOTTOM")));
            var topPosition=positions.rows.stream().filter(p->p.text.startsWith("TOP")).findFirst().orElseThrow();
            var bottomPosition=positions.rows.stream().filter(p->p.text.startsWith("BOTTOM")).findFirst().orElseThrow();
            float expectedTop=(8+3)*72f/25.4f+14*.905f;
            float expectedBottom=(266+3)*72f/25.4f+10*.905f;
            assertEquals(expectedTop,topPosition.y,.8f,"PDF baseline must follow the SVG top-left text box");
            assertEquals(expectedBottom,bottomPosition.y,.8f,"bottom-positioned text must retain trim-relative mm coordinates");
            long distinctLines=positions.rows.stream().filter(p->p.y<100).map(p->Math.round(p.y)).distinct().count();
            assertTrue(distinctLines>=3,"explicit and width-based wrapping should produce distinct, clipped lines: "+positions.rows);

            // At 72 dpi, one raster pixel is one PDF point. Compare the text's first
            // painted pixel with the editor's trim+bleed mm origin and SVG font ascent.
            var raster=new PDFRenderer(pdf).renderImage(0);
            int expectedX=Math.round((12+3)*72f/25.4f),expectedY=Math.round((8+3)*72f/25.4f);
            int minX=raster.getWidth(),minY=raster.getHeight(),maxX=-1,maxY=-1;
            for(int y=Math.max(0,expectedY-3);y<Math.min(raster.getHeight(),expectedY+50);y++)for(int x=Math.max(0,expectedX-3);x<Math.min(raster.getWidth(),expectedX+100);x++){
                int rgb=raster.getRGB(x,y)&0xffffff;if(((rgb>>16)&255)<150&&((rgb>>8)&255)<150&&(rgb&255)<150){minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
            }
            assertTrue(maxX>=minX&&maxY>=minY,"PDF raster should paint the editor's text layer");
            assertTrue(Math.abs(minX-expectedX)<=2,"raster x origin should match the editor's mm coordinate");
            // Raster glyph ink starts below the text-box top; the baseline itself is
            // checked above using PDFTextStripper. This compares the painted glyph top
            // to the SVG text box's top edge, allowing the font's cap-height inset.
            assertTrue(minY>=expectedY&&minY<=expectedY+5,"PDF raster ink should begin inside the editor text box; minY="+minY+", svgTop="+expectedY);
            assertTrue(maxX<Math.round((12+34+3)*72f/25.4f)+1,"wrapped text raster must be clipped to its editor text box");
        }
    }

    @Test void supportsBusinessCardAndA4TrimAndBleedCoordinatesWithRotatedText() throws Exception {
        for(var size:List.of(new int[]{90,54},new int[]{210,297})){
            var rotated=text("rotated","ROTATED",5,5,50,18,12,35);
            var doc=new PrintDesign.Document(1,size[0]==90?"BUSINESS_CARD":"A4_FLYER",size[0],size[1],3,5,List.of(rotated));
            try(var pdf=Loader.loadPDF(exporter.create(doc,id->null))){
                var page=pdf.getPage(0).getMediaBox();
                assertEquals((size[0]+6)*72f/25.4f,page.getWidth(),.02f);
                assertEquals((size[1]+6)*72f/25.4f,page.getHeight(),.02f);
                var raster=new PDFRenderer(pdf).renderImage(0);assertTrue(hasDarkPixel(raster));
                var positions=new Positions();positions.getText(pdf);assertTrue(positions.rows.stream().anyMatch(p->p.text.equals("ROTATED")));
                assertTrue(positions.rows.stream().filter(p->p.text.equals("ROTATED")).allMatch(p->Math.abs(Math.abs(p.direction)-35)<.1),"PDF text rotation matrix must match SVG clockwise rotation: "+positions.rows);
            }
        }
    }

    private static boolean hasDarkPixel(BufferedImage image){for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int c=image.getRGB(x,y);if(((c>>16)&255)<120&&((c>>8)&255)<120&&(c&255)<120)return true;}return false;}

    private static final class Positions extends PDFTextStripper {
        private final java.util.List<Row> rows=new java.util.ArrayList<>();
        Positions()throws java.io.IOException{}
        @Override protected void writeString(String text,java.util.List<TextPosition> chars){if(!chars.isEmpty()){var m=chars.getFirst().getTextMatrix();rows.add(new Row(text,chars.getFirst().getYDirAdj(),(float)Math.toDegrees(Math.atan2(m.getShearY(),m.getScaleX()))));}}
    }
    private record Row(String text,float y,float direction){}

    private static PrintDesign.Element text(String id,String text,double x,double y){
        return text(id,text,x,y,60,12,12,0);
    }
    private static PrintDesign.Element text(String id,String text,double x,double y,double w,double h,int size,int rotation){return new PrintDesign.Element(id,"TEXT",x,y,w,h,rotation,text,size,"#183b43","#183b43",0,null);}
}
