package com.printflow.files;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
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

    private static PrintDesign.Element text(String id,String text,double x,double y){
        return new PrintDesign.Element(id,"TEXT",x,y,60,12,0,text,12,"#183b43","#183b43",0,null);
    }
}
