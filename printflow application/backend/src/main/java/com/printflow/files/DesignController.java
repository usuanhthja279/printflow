package com.printflow.files;
import com.printflow.identity.StaffPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/design")
public class DesignController {
 private final DesignProofs service;
 public DesignController(DesignProofs service){this.service=service;}
 @GetMapping("/enquiries/{id}/samples") public ResponseEntity<?> samples(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id){return response(service.samples(user,id));}
 @GetMapping("/enquiries/{id}/samples/{code}/preview") public ResponseEntity<?> sample(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@PathVariable String code)throws IOException{return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options","nosniff").body(service.samplePreview(user,id,code));}
 @PostMapping("/enquiries/{id}/samples/{code}") public ResponseEntity<?> select(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@PathVariable String code)throws IOException{return response(service.selectSample(user,id,code));}
 @PostMapping("/enquiries/{id}/references/{file}/feedback") public ResponseEntity<?> referenceFeedback(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@PathVariable UUID file,@Valid @RequestBody DesignProofs.ReferenceInput input)throws IOException{return response(service.referenceFeedback(user,id,file,input));}
 @GetMapping("/enquiries/{id}/reference-feedback/{feedback}/document") public ResponseEntity<?> review(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@PathVariable UUID feedback)throws IOException{var copy=service.referenceCopy(user,id,feedback);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF).contentLength(copy.size()).header("Content-Disposition","inline").header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox; default-src 'none'").body(new InputStreamResource(copy.stream()));}
 @GetMapping("/queue") public ResponseEntity<?> queue(@AuthenticationPrincipal StaffPrincipal user){return response(service.designerQueue(user));}
 @GetMapping("/enquiries/{id}") public ResponseEntity<?> workspace(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id){return response(service.workspace(user,id));}
 @PostMapping("/enquiries/{id}/designer") public ResponseEntity<?> assign(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@Valid @RequestBody DesignProofs.Assignment input){service.assign(user,id,input);return response(Map.of("code","DESIGNER_ASSIGNED"));}
 @PostMapping("/enquiries/{id}/artifacts") public ResponseEntity<?> upload(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@RequestHeader("X-File-Name") String name,@RequestHeader("Content-Type") String mime,@RequestParam String kind,HttpServletRequest request)throws IOException{
  if(name.length()>2160)throw new IllegalArgumentException("Filename too long");return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(service.upload(user,id,URLDecoder.decode(name,StandardCharsets.UTF_8),mime,kind,request.getInputStream()));
 }
 @PostMapping("/enquiries/{id}/artifacts/{file}/scan") public ResponseEntity<?> scan(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@PathVariable UUID file)throws IOException{return response(service.rescan(user,id,file));}
 @GetMapping("/enquiries/{id}/artifacts/{file}/download") public ResponseEntity<?> download(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@PathVariable UUID file)throws IOException{
  var download=service.download(user,id,file);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(download.size()).header("Content-Disposition",ContentDisposition.attachment().filename(download.filename(),StandardCharsets.UTF_8).build().toString()).header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox; default-src 'none'").body(new InputStreamResource(download.stream()));
 }
 @GetMapping("/enquiries/{id}/artifacts/{file}/preview") public ResponseEntity<?> preview(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@PathVariable UUID file)throws IOException{
  var download=service.preview(user,id,file);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(download.mediaType())).header("Content-Disposition","inline").contentLength(download.size()).header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox; default-src 'none'").body(new InputStreamResource(download.stream()));
 }
 @PostMapping("/enquiries/{id}/proofs") public ResponseEntity<?> publish(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@Valid @RequestBody DesignProofs.Publish input)throws IOException{return response(service.publish(user,id,input));}
 @PostMapping("/enquiries/{id}/proofs/{proof}/comments") public ResponseEntity<?> comment(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@PathVariable UUID proof,@Valid @RequestBody DesignProofs.Comment input)throws IOException{return response(service.comment(user,id,proof,input));}
 @PostMapping("/enquiries/{id}/proofs/{proof}/decision") public ResponseEntity<?> decision(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@PathVariable UUID proof,@Valid @RequestBody DesignProofs.Decision input)throws IOException{return response(service.decide(user,id,proof,input));}
 @PostMapping("/enquiries/{id}/tasks") public ResponseEntity<?> task(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@Valid @RequestBody DesignProofs.Task input){return response(service.saveTask(user,id,input));}
 @PostMapping("/enquiries/{id}/comments/{comment}/resolve") public ResponseEntity<?> resolve(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@PathVariable UUID comment,@Valid @RequestBody DesignProofs.Resolution input){return response(service.resolveComment(user,id,comment,input));}
 @PostMapping("/enquiries/{id}/proofs/{proof}/offline-approval") public ResponseEntity<?> offline(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id,@PathVariable UUID proof,@Valid @RequestBody DesignProofs.Offline input)throws IOException{return response(service.offlineApproval(user,id,proof,input));}
 private ResponseEntity<?> response(Object body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
}
