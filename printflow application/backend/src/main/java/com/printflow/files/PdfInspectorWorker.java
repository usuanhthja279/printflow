package com.printflow.files;
/** Resource-limited child process: inspect only, never execute PDF actions or render external content. */
public final class PdfInspectorWorker {
 public static void main(String[] args){try(var doc=org.apache.pdfbox.Loader.loadPDF(new java.io.File(args[0]),org.apache.pdfbox.io.IOUtils.createTempFileOnlyStreamCache())){int pages=doc.getNumberOfPages();if(doc.isEncrypted()||pages<1||pages>100||(doc.getDocumentCatalog().getAcroForm()!=null&&doc.getDocumentCatalog().getAcroForm().hasXFA()))System.exit(2);System.out.print(pages);}catch(Throwable invalid){System.exit(2);}}
}
