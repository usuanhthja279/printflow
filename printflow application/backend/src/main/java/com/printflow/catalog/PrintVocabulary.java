package com.printflow.catalog;
import com.printflow.identity.StaffPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
@RestController
public class PrintVocabulary {
 public record Edit(@Min(1) int expectedRevision,@NotNull @Size(min=1,max=40) List<@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{0,39}") String> choices){}
 private final JdbcTemplate db;private final JsonMapper json;
 public PrintVocabulary(JdbcTemplate db,JsonMapper json){this.db=db;this.json=json;}
 @GetMapping({"/api/public/print-vocabulary","/api/v1/owner/print-vocabulary"}) public ResponseEntity<?> list(){var rows=db.queryForList("select category,choices::text as choices,revision from print_vocabulary order by category");return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(rows);}
 @PutMapping("/api/v1/owner/print-vocabulary/{category}") @Transactional public ResponseEntity<?> edit(@AuthenticationPrincipal StaffPrincipal user,@PathVariable String category,@Valid @RequestBody Edit input){if(!"OWNER".equals(user.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);if(!input.choices().contains("UNSURE")||new HashSet<>(input.choices()).size()!=input.choices().size())throw new IllegalArgumentException("Keep UNSURE and unique option codes");String choices=json.writeValueAsString(input.choices());if(db.update("update print_vocabulary set choices=?::jsonb,revision=revision+1 where category=? and revision=?",choices,category,input.expectedRevision())!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Vocabulary changed; refresh first");db.update("insert into vocabulary_events(id,category,actor_id,revision,choices) values(?,?,?,?,?::jsonb)",UUID.randomUUID(),category,user.id(),input.expectedRevision()+1,choices);return list();}
 public void validate(Map<String,String> specifications){for(var row:db.queryForList("select category,choices::text as choices from print_vocabulary")){String value=specifications.get(row.get("category").toString());if(value!=null&&!json.readValue(row.get("choices").toString(),List.class).contains(value))throw new IllegalArgumentException("Choose a configured "+row.get("category")+" option");}}
}
