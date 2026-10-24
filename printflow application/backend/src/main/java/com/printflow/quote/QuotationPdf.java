package com.printflow.quote;
import java.io.*;import java.time.LocalDate;import java.util.*;
import org.apache.pdfbox.pdmodel.*;import org.apache.pdfbox.pdmodel.common.PDRectangle;import org.apache.pdfbox.pdmodel.font.*;import org.springframework.stereotype.Component;
/** Immutable text-only quote. Never parses customer files, HTML or external resources. */
@Component public class QuotationPdf {
 private final tools.jackson.databind.json.JsonMapper json;public QuotationPdf(tools.jackson.databind.json.JsonMapper json){this.json=json;}
 public byte[] create(UUID quote,UUID enquiry,int version,String customer,Quotations.Issue input,LocalDate requested,List<Map<String,Object>> items,String snapshot){
  try(var doc=new PDDocument();var stream=getClass().getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf");var output=new ByteArrayOutputStream()){
   PDFont font=stream==null?new PDType1Font(Standard14Fonts.FontName.HELVETICA):PDType0Font.load(doc,stream);var layout=new Layout(doc,font,version);
   layout.block("PREPARED FOR",customer);layout.block("DOCUMENT REFERENCES","Quotation: "+quote+"\nEnquiry: "+enquiry);
   layout.grid("CUSTOMER REQUESTED",requested.toString(),"PROPOSED DELIVERY / COLLECTION",input.proposedDate().toString());
   layout.block("SCOPE OF WORK",input.scope());
   if(input.amendmentOf()!=null)layout.block("CHANGE AGREEMENT","Amends: "+input.amendmentOf()+"\n"+input.amendmentReason()+"\nEarlier agreement remains effective until this version is accepted.");
   layout.heading("PRINT ITEMS & PRICING");int n=0;for(var item:items){layout.item(++n,item);}
   layout.block("TOTAL INCLUDING ENTERED TAX: INR "+money(input.totalPaise()),"Prices and tax as entered. This quotation is not a tax invoice.");
   layout.block("COMMERCIAL TERMS",input.terms());
   @SuppressWarnings("unchecked") Map<String,Object> specs=json.readValue(snapshot,Map.class);var labels=new LinkedHashMap<String,String>();labels.put("productCode","Product");labels.put("quantity","Copies");labels.put("size","Size");labels.put("paperGsm","Paper GSM");labels.put("printSides","Print sides");labels.put("finish","Finish");labels.put("notes","Instructions");StringBuilder captured=new StringBuilder();labels.forEach((k,label)->{if(specs.get(k)!=null&&!specs.get(k).toString().isBlank())captured.append(label).append(": ").append(specs.get(k).toString().replace('_',' ')).append("\n");});layout.block("PRINT SPECIFICATIONS AT ISSUE",captured.toString());
   if(Boolean.TRUE.equals(input.acceptedOffline()))layout.block("PHONE / EMAIL AGREEMENT",input.acceptanceSource()+": "+input.acceptanceEvidence());
   layout.block("ACCEPTANCE & NEXT STEP","Review and accept this exact version in your customer workspace unless the agreement is already recorded above. Current status, withdrawal and amendments are shown in the workspace. No expiry. Printing requires separate proof approval and production release.");
   layout.finish();doc.getDocumentInformation().setTitle("J.D.Printex quotation v"+version);doc.getDocumentInformation().setAuthor("J.D.Printex");doc.save(output);return output.toByteArray();
  }catch(IllegalArgumentException e){throw new IllegalArgumentException("Quotation PDF contains unsupported characters or excessive text. Use the English print specification for this release.",e);}catch(IOException e){throw new IllegalStateException("Could not generate quotation document",e);}
 }
 private static String money(Number paise){return java.math.BigDecimal.valueOf(paise.longValue(),2).toPlainString();}
 private static class Layout {
  final PDDocument doc;final PDFont font;final int version;PDPageContentStream c;float y;int lines=0;
  Layout(PDDocument d,PDFont f,int v)throws IOException{doc=d;font=f;version=v;page();}
  void page()throws IOException{if(c!=null)c.close();if(doc.getNumberOfPages()>=110)throw new IllegalArgumentException("Document too long");var p=new PDPage(PDRectangle.A4);doc.addPage(p);c=new PDPageContentStream(doc,p);fill(0.09f,0.22f,0.24f);c.addRect(0,752,596,90);c.fill();text(40,798,23,"J.D.Printex",true);text(40,777,9,"Wedding | Hospitality | Promotional printing",true);text(396,801,12,"QUOTATION v"+version,true);text(396,781,10,"jdprintex.in | INR",true);y=728;}
  void fill(float r,float g,float b)throws IOException{c.setNonStrokingColor(r,g,b);}
  void text(float x,float yy,float size,String value,boolean white)throws IOException{if(white)fill(1,1,1);else fill(.09f,.22f,.24f);c.beginText();c.setFont(font,size);c.newLineAtOffset(x,yy);c.showText(value);c.endText();}
  void room(float height)throws IOException{if(y-height<65)page();}
  List<String> wrap(String value,float width,float size)throws IOException{var result=new ArrayList<String>();value=value.replace("₹","INR ").replace('\t',' ').replace('\r',' ');for(String para:value.split("\n",-1)){StringBuilder row=new StringBuilder();for(int offset=0;offset<para.length();){int cp=para.codePointAt(offset);offset+=Character.charCount(cp);if(Character.isISOControl(cp))continue;String ch=new String(Character.toChars(cp));if(font.getStringWidth(row+ch)/1000*size>width){result.add(row.toString());row.setLength(0);}row.append(ch);}result.add(row.toString());}lines+=result.size();if(lines>6000)throw new IllegalArgumentException("Too much text");return result;}
  void heading(String title)throws IOException{room(35);text(40,y,11,title,false);y-=20;}
  void box(float x,float bottom,float width,float height)throws IOException{fill(.96f,.975f,.97f);c.addRect(x,bottom,width,height);c.fill();c.setStrokingColor(.82f,.87f,.85f);c.setLineWidth(.5f);c.addRect(x,bottom,width,height);c.stroke();}
  void block(String title,String body)throws IOException{var rows=wrap(body,486,10);int i=0;while(i<rows.size()){room(68);int count=Math.min(rows.size()-i,Math.max(1,(int)((y-65-38)/14)));float h=38+count*14;box(40,y-h,516,h);text(54,y-19,10,title+(i>0?" (continued)":""),false);float line=y-38;for(int j=0;j<count;j++)text(54,line-j*14,10,rows.get(i+j),false);i+=count;y-=h+12;}}
  void grid(String left,String value,String right,String other)throws IOException{room(66);box(40,y-54,252,54);box(304,y-54,252,54);text(52,y-18,8,left,false);text(52,y-39,12,value,false);text(316,y-18,8,right,false);text(316,y-39,12,other,false);y-=66;}
  void item(int n,Map<String,Object> item)throws IOException{block("ITEM "+n,item.get("description").toString());grid("QUANTITY / UNIT PRICE",item.get("quantity")+" x INR "+money((Number)item.get("unitPaise")),"TAX / LINE TOTAL",(((Number)item.get("taxBps")).doubleValue()/100)+"% | INR "+money((Number)item.get("totalPaise")));@SuppressWarnings("unchecked") var specs=(Map<String,String>)item.get("specifications");StringBuilder details=new StringBuilder();specs.forEach((k,v)->{if(!v.isBlank()&&!v.equals("UNSURE"))details.append(k.replaceAll("([a-z])([A-Z])","$1 $2")).append(": ").append(v.replace('_',' ')).append("    ");});if(!details.isEmpty())block("ITEM SPECIFICATION",details.toString());}
  void finish()throws IOException{c.close();c=null;int pages=doc.getNumberOfPages();for(int n=0;n<pages;n++){try(var footer=new PDPageContentStream(doc,doc.getPage(n),PDPageContentStream.AppendMode.APPEND,true,true)){footer.setNonStrokingColor(.35f,.45f,.45f);footer.beginText();footer.setFont(font,9);footer.newLineAtOffset(40,32);footer.showText("J.D.Printex | Quotation v"+version+" | Page "+(n+1)+" / "+pages);footer.endText();}}}
 }
}
