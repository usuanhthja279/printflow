package com.printflow.customer;

import java.io.*;import java.time.LocalDate;import java.util.*;
import org.apache.pdfbox.pdmodel.*;import org.apache.pdfbox.pdmodel.common.PDRectangle;import org.apache.pdfbox.pdmodel.font.*;import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Commercial invoice from an immutable accepted-quotation snapshot; no inferred GST registration. */
@Component public class InvoicePdf {
 private final JsonMapper json;public InvoicePdf(JsonMapper json){this.json=json;}
 public byte[] create(long number,LocalDate date,Map<String,Object> snapshot){
  try(var doc=new PDDocument();var fontStream=getClass().getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf");var out=new ByteArrayOutputStream()){
   PDFont font=fontStream==null?new PDType1Font(Standard14Fonts.FontName.HELVETICA):PDType0Font.load(doc,fontStream);var page=new Layout(doc,font,number);
   page.block("INVOICE INV-"+number,"Issued: "+date+" | Currency: INR\nCommercial invoice - not a GST tax invoice");
   page.block("BILL TO",snapshot.get("customer")+"\n"+snapshot.get("email"));
   page.block("ACCEPTED AGREEMENT","Quotation v"+snapshot.get("version")+" | "+snapshot.get("id")+"\n"+snapshot.get("scope"));
   @SuppressWarnings("unchecked") List<Map<String,Object>> items=json.readValue(snapshot.get("line_items").toString(),List.class);int i=0;
   for(var item:items)page.block("ITEM "+(++i),item.get("description")+"\nQuantity: "+item.get("quantity")+" | Unit: INR "+Payments.money(((Number)item.get("unitPaise")).longValue())+"\nEntered tax: "+java.math.BigDecimal.valueOf(((Number)item.get("taxBps")).longValue(),2).toPlainString()+"% | Line total: INR "+Payments.money(((Number)item.get("totalPaise")).longValue()));
   if(items.isEmpty())page.block("AGREED LOT",snapshot.get("scope").toString());
   page.block("TOTAL INCLUDING ENTERED TAX","INR "+Payments.money(((Number)snapshot.get("total_paise")).longValue()));
   @SuppressWarnings("unchecked") var policy=(Map<String,Object>)snapshot.get("policy");
   page.block("PAYMENT AGREEMENT","Required advance: INR "+Payments.money(((Number)policy.get("advance_paise")).longValue())+"\n"+(policy.get("credit_due")==null?"Remaining balance due before dispatch.":"Approved balance credit until "+policy.get("credit_due"))+"\n"+policy.get("reason"));
   page.block("COMMERCIAL TERMS",snapshot.get("terms").toString());
   page.block("PAYMENT STATUS","This invoice is not a receipt or confirmation of payment. See verified receipts and the current balance in your customer workspace. Tax rates and prices above are copied from the accepted quotation; no tax registration or statutory tax invoice status is asserted.");
   page.finish();doc.getDocumentInformation().setTitle("J.D.Printex invoice INV-"+number);doc.save(out);return out.toByteArray();
  }catch(IOException|IllegalArgumentException e){throw new IllegalArgumentException("Invoice text could not be rendered. Review unsupported characters in the accepted agreement.",e);}
 }
 private static class Layout {
  final PDDocument doc;final PDFont font;final long number;PDPageContentStream stream;float y;int count;
  Layout(PDDocument d,PDFont f,long n)throws IOException{doc=d;font=f;number=n;page();}
  void text(float x,float y,int size,String s)throws IOException{stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(x,y);stream.showText(s);stream.endText();}
  void page()throws IOException{if(stream!=null)stream.close();if(doc.getNumberOfPages()>=100)throw new IllegalArgumentException("Invoice too long");var p=new PDPage(PDRectangle.A4);doc.addPage(p);stream=new PDPageContentStream(doc,p);stream.setNonStrokingColor(.06f,.23f,.25f);stream.addRect(0,758,596,84);stream.fill();stream.setNonStrokingColor(1f,1f,1f);text(42,800,24,"J.D.Printex");text(42,778,10,"PRINTING | COMMERCIAL INVOICE | INR");stream.setNonStrokingColor(.06f,.23f,.25f);y=730;}
  void block(String title,String value)throws IOException{if(y<125)page();text(42,y,11,title);y-=20;for(String para:value.replace("₹","INR ").replace('\r',' ').replace('\t',' ').split("\n",-1)){StringBuilder line=new StringBuilder();for(int at=0;at<para.length();){int cp=para.codePointAt(at);at+=Character.charCount(cp);if(Character.isISOControl(cp))continue;String ch=new String(Character.toChars(cp));if(font.getStringWidth(line+ch)/1000*10>505){row(line.toString());line.setLength(0);}line.append(ch);}row(line.toString());}y-=18;}
  void row(String s)throws IOException{if(++count>5000)throw new IllegalArgumentException("Invoice too long");if(y<65)page();text(42,y,10,s);y-=15;}
  void finish()throws IOException{stream.close();for(int i=0;i<doc.getNumberOfPages();i++)try(var footer=new PDPageContentStream(doc,doc.getPage(i),PDPageContentStream.AppendMode.APPEND,true,true)){footer.beginText();footer.setFont(font,9);footer.newLineAtOffset(42,30);footer.showText("J.D.Printex | INV-"+number+" | Page "+(i+1)+" / "+doc.getNumberOfPages());footer.endText();}}
 }
}
