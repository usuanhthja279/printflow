package com.printflow.shared;
import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.*;
import org.springframework.core.Ordered;import org.springframework.core.annotation.Order;import org.springframework.stereotype.Component;import org.springframework.web.filter.OncePerRequestFilter;
@Component @Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestBodyLimit extends OncePerRequestFilter {
 private static final int MAX=131072;
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
  if(!request.getRequestURI().startsWith("/api/")){chain.doFilter(request,response);return;}
  final boolean photo=request.getMethod().equals("POST")&&request.getRequestURI().matches("/api/v1/design/enquiries/[0-9a-fA-F-]{36}/fulfilment/photos");
  final int limit=photo?5*1024*1024:request.getMethod().equals("POST")&&request.getRequestURI().matches("/api/v1/design/enquiries/[0-9a-fA-F-]{36}/artifacts")?20*1024*1024:MAX;
  if(request.getContentLengthLong()>limit){response.setStatus(413);response.setHeader("Cache-Control","no-store");response.setContentType("application/json");response.getWriter().write("{\"code\":\"BODY_TOO_LARGE\"}");return;}
  var wrapped=new HttpServletRequestWrapper(request){
   private ServletInputStream stream;
   @Override public ServletInputStream getInputStream()throws IOException {
    if(stream==null){var original=super.getInputStream();stream=new ServletInputStream(){int count;
     @Override public boolean isFinished(){return original.isFinished();}@Override public boolean isReady(){return original.isReady();}@Override public void setReadListener(ReadListener listener){original.setReadListener(listener);}
     @Override public int read()throws IOException {int b=original.read();if(b>=0&&++count>limit)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE,"Request body limit exceeded");return b;}
     @Override public int read(byte[] b,int off,int length)throws IOException {int n=original.read(b,off,Math.min(length,limit-count+1));if(n>0){count+=n;if(count>limit)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE,"Request body limit exceeded");}return n;}
    };}return stream;
   }
   @Override public BufferedReader getReader()throws IOException {return new BufferedReader(new InputStreamReader(getInputStream(),java.nio.charset.StandardCharsets.UTF_8));}
  };
  chain.doFilter(wrapped,response);
 }
}
