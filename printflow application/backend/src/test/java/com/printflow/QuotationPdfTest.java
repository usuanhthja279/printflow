package com.printflow;
import com.printflow.quote.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class QuotationPdfTest {
 @Test void longDocumentsPaginateAndRemainTextOnly()throws Exception{
  var renderer=new QuotationPdf(tools.jackson.databind.json.JsonMapper.builder().build());var date=LocalDate.of(2026,11,10);
  var issue=new Quotations.Issue(0,"Specification detail ".repeat(120),10000,"Reviewed terms ".repeat(100),date,date,false,null,null,null,null,null);
  var items=List.of(Map.<String,Object>of("description","Synthetic item (round brackets) \\ test","quantity",1,"unitPaise",10000,"taxBps",0,"totalPaise",10000,"specifications",Map.of("paper","250 GSM")));
  byte[] bytes=renderer.create(UUID.randomUUID(),UUID.randomUUID(),3,"Synthetic customer",issue,date,items,"{\"productCode\":\"WEDDING_CARDS\",\"quantity\":250}");
  try(var document=org.apache.pdfbox.Loader.loadPDF(bytes)){assertTrue(document.getNumberOfPages()>1);String text=new org.apache.pdfbox.text.PDFTextStripper().getText(document);assertTrue(text.contains("Page 1 /"));assertTrue(text.contains("TOTAL INCLUDING ENTERED TAX: INR 100.00"));assertTrue(text.contains("Copies: 250"));assertNull(document.getDocumentCatalog().getOpenAction());assertNull(document.getDocumentCatalog().getAcroForm());}
 }
 @Test void sampleGridQuotation()throws Exception{var renderer=new QuotationPdf(tools.jackson.databind.json.JsonMapper.builder().build());var date=LocalDate.of(2026,10,28);var input=new Quotations.Issue(0,"Shaadi wedding cards with gloss finish",5900000,"50% advance. Balance on collection. Please check names and dates before proof approval.",date.minusDays(1),date,false,null,null,null,null,null);var items=List.of(Map.<String,Object>of("description","Wedding cards and matching envelopes","quantity",10000,"unitPaise",500,"taxBps",1800,"totalPaise",5900000,"specifications",Map.of("paper","250 GSM","size","A5","finish","Gloss")));byte[] bytes=renderer.create(UUID.randomUUID(),UUID.randomUUID(),1,"Synthetic preview customer",input,date.minusDays(1),items,"{\"productCode\":\"WEDDING_CARDS\",\"quantity\":10000}");var output=java.nio.file.Path.of("build/reports/quotation-preview.pdf");java.nio.file.Files.createDirectories(output.getParent());java.nio.file.Files.write(output,bytes);try(var pdf=org.apache.pdfbox.Loader.loadPDF(bytes)){assertTrue(pdf.getNumberOfPages()<=2);}}

}
