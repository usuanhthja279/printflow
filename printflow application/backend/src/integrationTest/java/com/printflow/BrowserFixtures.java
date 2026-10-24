package com.printflow;
import com.printflow.identity.*;import com.printflow.enquiry.TokenUtil;
import java.nio.file.*;import java.util.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.security.crypto.password.PasswordEncoder;import tools.jackson.databind.json.JsonMapper;
/** Explicit native-only synthetic browser fixture task; never a production provisioning path. */
public class BrowserFixtures {
 public static void main(String[] args)throws Exception {
  String url=System.getenv("DB_URL");if(url==null||!url.startsWith("jdbc:postgresql://127.0.0.1:5434/")||"true".equalsIgnoreCase(System.getenv("APP_MAIL_ENABLED")))throw new IllegalStateException("Browser fixtures require native loopback DEV and disabled real delivery");
  Path output=Path.of("..",".local","browser-fixtures.json").toAbsolutePath().normalize();
  {
   var jdbc=new JdbcTemplate(new DriverManagerDataSource(url,System.getenv("DB_USERNAME"),System.getenv("DB_PASSWORD")));var json=JsonMapper.builder().build();
   jdbc.update("update staff_users set enabled=false where username like 'browser-phase1-%'");
   jdbc.update("update mail_outbox set payload=null,state='EXPIRED' where recipient like 'browser-phase1-%'");
   if(args.length>0&&args[0].equals("cleanup")){jdbc.update("update mail_outbox set state='HELD',payload=null where state<>'SENT' and enquiry_id in (select id from enquiries where email like 'browser-phase1-%@example.test' and customer_name like 'Synthetic%')");jdbc.update("update enquiries set status='CLOSED',closure_kind='REMOVED',closure_message='Synthetic browser verification archived.',revision=revision+1,updated_at=now() where email like 'browser-phase1-%@example.test' and customer_name like 'Synthetic%' and coalesce(closure_kind,'')<>'REMOVED'");Files.deleteIfExists(output);System.out.println("Synthetic browser accounts disabled and private fixture file removed.");return;}
   UUID id=UUID.randomUUID();String username="browser-phase1-owner-"+id,password=TokenUtil.newToken();String seed=Totp.secret();
   jdbc.update("insert into staff_users(id,username,password_hash,role,totp_secret) values(?,?,?,'OWNER',?)",id,username,new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12).encode(password),new MfaSecrets(System.getenv("APP_MFA_ENCRYPTION_KEY")).encrypt(seed));
   Map<String,String> codes=new HashMap<>();
   for(String project:List.of("desktop","mobile")){String code=UUID.randomUUID().toString().replace("-","");codes.put(project,code);jdbc.update("insert into owner_recovery_codes(staff_id,code_hash) values(?,?)",id,TokenUtil.sha256(code));}
   UUID designerId=UUID.randomUUID();String designerUsername="browser-phase1-designer-"+designerId+"@example.test",designerPassword=TokenUtil.newToken();
   jdbc.update("insert into staff_users(id,username,password_hash,role) values(?,?,?,'DESIGNER')",designerId,designerUsername,new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12).encode(designerPassword));
   jdbc.update("update staff_users set routing_pool='TEST',public_name='Synthetic test designer',created_at='2000-01-01T00:00:00Z' where id=?",designerId);
   Map<String,Object> fixture=new LinkedHashMap<>(Map.of("username",username,"password",password,"recoveryCodes",codes,"designerId",designerId.toString(),"designerUsername",designerUsername,"designerPassword",designerPassword));
   for(var role:Map.of("sales","SALES","printer","OPERATOR","delivery","DELIVERY").entrySet()){UUID staffId=UUID.randomUUID();String staffName="browser-phase1-"+role.getKey()+"-"+staffId,staffPassword=TokenUtil.newToken();jdbc.update("insert into staff_users(id,username,password_hash,role) values(?,?,?,?)",staffId,staffName,new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12).encode(staffPassword),role.getValue());fixture.put(role.getKey()+"Id",staffId.toString());fixture.put(role.getKey()+"Username",staffName);fixture.put(role.getKey()+"Password",staffPassword);}
   Files.createDirectories(output.getParent());Files.writeString(output,json.writeValueAsString(fixture));
   System.out.println("Synthetic browser owner prepared privately; real owner/authenticator unchanged.");
  }
 }
}
