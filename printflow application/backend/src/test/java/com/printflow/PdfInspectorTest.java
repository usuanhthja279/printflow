package com.printflow;
import com.printflow.files.PdfInspector;import java.nio.file.*;import org.junit.jupiter.api.Test;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;
class PdfInspectorTest{
 @TempDir Path root;
 @Test void boundedChildCountsPagesAndRejectsMalformedAndExcessPages()throws Exception{var inspector=new PdfInspector();Path valid=root.resolve("valid.pdf"),large=root.resolve("large.pdf"),broken=root.resolve("broken.pdf");try(var pdf=new org.apache.pdfbox.pdmodel.PDDocument()){pdf.addPage(new org.apache.pdfbox.pdmodel.PDPage());pdf.addPage(new org.apache.pdfbox.pdmodel.PDPage());pdf.save(valid.toFile());for(int n=2;n<101;n++)pdf.addPage(new org.apache.pdfbox.pdmodel.PDPage());pdf.save(large.toFile());}assertEquals(2,inspector.pages(valid));Files.writeString(broken,"%PDF-broken");assertThrows(org.springframework.web.server.ResponseStatusException.class,()->inspector.pages(broken));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->inspector.pages(large));}
}
