package com.printflow.catalog;
import com.printflow.identity.StaffPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
public class TemplateController {
 private final ProductTemplates templates;
 public TemplateController(ProductTemplates templates){this.templates=templates;}
 @GetMapping("/api/public/templates/{code}") public ProductTemplates.Template get(@PathVariable String code){var t=templates.get(code);if(!t.active())throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND);return t;}
 @GetMapping("/api/v1/owner/templates") public ResponseEntity<List<ProductTemplates.Template>> list(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(templates.list(true));}
 @PutMapping("/api/v1/owner/templates/{code}") public ResponseEntity<ProductTemplates.Template> edit(@AuthenticationPrincipal StaffPrincipal owner,@PathVariable String code,@Valid @RequestBody ProductTemplates.Edit edit){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(templates.edit(owner,code,edit));}
}
