package com.printflow.customer;
import com.printflow.identity.StaffPrincipal;import jakarta.validation.Valid;import java.io.IOException;import java.util.*;import org.springframework.http.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/production") public class ProductionController {
 private final ProductionQueue queue;private final FulfilmentController fulfilment;
 public ProductionController(ProductionQueue queue,FulfilmentController fulfilment){this.queue=queue;this.fulfilment=fulfilment;}
 @GetMapping("/queue") public ResponseEntity<?> board(@AuthenticationPrincipal StaffPrincipal u){return response(queue.board(u));}
 @GetMapping("/enquiries/{id}") public ResponseEntity<?> view(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id){return response(queue.view(u,id));}
 @PostMapping("/enquiries/{id}") @Transactional(rollbackFor=Exception.class) public ResponseEntity<?> change(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id,@Valid @RequestBody ProductionQueue.Change input)throws IOException{queue.change(u,id,input);if(input.action().equals("READY")){var current=(Map<?,?>)fulfilment.view(u,id).getBody();fulfilment.act(u,id,new FulfilmentController.Action(((Number)current.get("revision")).longValue(),"READY",input.note(),true));}return view(u,id);}
 private ResponseEntity<?> response(Object data){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(data);}
}
