package com.printflow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tools.jackson.databind.json.JsonMapper;

/** Local-only synthetic fixture for the genuine two-browser Batch 3B acceptance run. */
public final class CollaborationBrowserFixture {
    private static final Path OUTPUT=Path.of("..",".local","batch3b-real-acceptance","credentials.json").toAbsolutePath().normalize();
    private static final String PASSWORD="Local-Collaboration-Test-2026!";

    public static void main(String[] args) throws Exception {
        String url=System.getenv("DB_URL");
        if(url==null||!url.startsWith("jdbc:postgresql://127.0.0.1:5432/")||"true".equalsIgnoreCase(System.getenv("APP_MAIL_ENABLED")))
            throw new IllegalStateException("Requires the native loopback PostgreSQL 5432 database and disabled mail delivery");
        JdbcTemplate db=new JdbcTemplate(new DriverManagerDataSource(url,System.getenv("DB_USERNAME"),System.getenv("DB_PASSWORD")));
        if(args.length>0&&args[0].equals("unassign")){
            Map<?,?> f=JsonMapper.builder().build().readValue(Files.readString(OUTPUT),Map.class);
            UUID project=UUID.fromString(f.get("projectId").toString());
            db.update("update enquiries set designer_id=null,designer_assignment_mode='MANUAL',revision=revision+1,updated_at=now() where id=? and email like 'batch3b-customer-%@example.test'",project);
            System.out.println("Synthetic project assignment removed.");return;
        }
        if(args.length>0&&args[0].equals("expire-customer")){
            Map<?,?> f=JsonMapper.builder().build().readValue(Files.readString(OUTPUT),Map.class);
            db.update("update staff_users set enabled=false where id=?",UUID.fromString(f.get("customerId").toString()));
            System.out.println("Synthetic customer session revoked.");return;
        }
        if(args.length>0&&args[0].equals("cleanup")){
            if(!Files.exists(OUTPUT)){System.out.println("No Batch 3B local fixture exists.");return;}
            Map<?,?> f=JsonMapper.builder().build().readValue(Files.readString(OUTPUT),Map.class);
            UUID project=UUID.fromString(f.get("projectId").toString());
            db.update("update enquiries set status='CLOSED',closure_kind='REMOVED',closure_message='Synthetic Batch 3B acceptance fixture archived.',revision=revision+1,updated_at=now() where id=? and email like 'batch3b-customer-%@example.test' and status<>'CLOSED'",project);
            db.update("update staff_users set enabled=false where id in (?,?)",UUID.fromString(f.get("customerId").toString()),UUID.fromString(f.get("designerId").toString()));
            db.update("update staff_users set enabled=false where id in (select assigned_staff_id from enquiries where id=? and assigned_staff_id is not null)",project);
            if(f.get("ownerId")!=null)db.update("update staff_users set enabled=false where id=?",UUID.fromString(f.get("ownerId").toString()));
            Files.deleteIfExists(OUTPUT);Files.deleteIfExists(OUTPUT.getParent().resolve("proof-sample.png"));System.out.println("Synthetic Batch 3B fixture closed and local credentials removed.");return;
        }
        UUID customer=UUID.randomUUID(),designer=UUID.randomUUID(),sales=UUID.randomUUID(),owner=UUID.randomUUID(),project=UUID.randomUUID(),quote=UUID.randomUUID();
        String email="batch3b-customer-"+project+"@example.test",designerLogin="batch3b-designer-"+designer+"@example.test",salesLogin="batch3b-sales-"+sales+"@example.test";
        String hash=new BCryptPasswordEncoder(10).encode(PASSWORD);
        db.update("insert into staff_users(id,username,password_hash,role) values(?,?,?,'CUSTOMER')",customer,email,hash);
        db.update("insert into customer_profiles(user_id,name,phone,email_verified,privacy_version,consent_at) values(?,?,'9000000000',true,'2026-10-25',now())",customer,"Synthetic Batch 3B Customer");
        db.update("insert into staff_users(id,username,password_hash,role,public_name,routing_pool) values(?,?,?,'DESIGNER','Synthetic Batch 3B Designer','TEST')",designer,designerLogin,hash);
        db.update("insert into staff_users(id,username,password_hash,role) values(?,?,?,'SALES')",sales,salesLogin,hash);
        String ownerLogin="batch3b-owner-"+owner+"@example.test";
        db.update("insert into staff_users(id,username,password_hash,role) values(?,?,?,'OWNER')",owner,ownerLogin,hash);
        db.update("insert into enquiries(id,public_id,tracking_token_sha256,customer_name,email,phone,product_code,quantity,status,assigned_staff_id,customer_user_id,designer_id,designer_assignment_mode,created_at,updated_at) values(?,?,?,'Synthetic Batch 3B Customer',?,'9000000000','WEDDING_CARDS',250,'IN_PROGRESS',?,?,?,'MANUAL',now(),now())",project,project,"0".repeat(64),email,sales,customer,designer);
        db.update("insert into designer_allocations(id,enquiry_id,designer_id) values(?,?,?)",UUID.randomUUID(),project,designer);
        db.update("insert into quote_versions(id,enquiry_id,version,scope,total_paise,terms,requested_date,proposed_date,valid_until,specification_snapshot,issued_by,state,responded_at,acceptance_source,acceptance_recorded_by) values(?,?,1,'250 synthetic wedding cards',100000,'Synthetic test terms',current_date,current_date+10,current_date+3,'{}'::jsonb,?,'ACCEPTED',now(),'PORTAL',?)",quote,project,sales,customer);
        Files.createDirectories(OUTPUT.getParent());
        createProofSample(OUTPUT.getParent().resolve("proof-sample.png"));
        Map<String,Object> fixture=new LinkedHashMap<>();fixture.put("projectId",project.toString());fixture.put("customerId",customer.toString());fixture.put("customerUsername",email);fixture.put("customerPassword",PASSWORD);fixture.put("designerId",designer.toString());fixture.put("designerUsername",designerLogin);fixture.put("designerPassword",PASSWORD);fixture.put("salesId",sales.toString());fixture.put("ownerId",owner.toString());fixture.put("ownerUsername",ownerLogin);fixture.put("ownerPassword",PASSWORD);fixture.put("createdAt",Instant.now().toString());
        Files.writeString(OUTPUT,JsonMapper.builder().build().writeValueAsString(fixture));
        System.out.println("Synthetic local customer/designer project prepared; credentials saved under ignored .local.");
        System.out.println("Fixture project ID: "+project);
    }

    private static void createProofSample(Path file)throws Exception{
        BufferedImage image=new BufferedImage(1200,800,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(247,244,234));g.fillRect(0,0,1200,800);
            g.setColor(new Color(26,64,70));g.fillRoundRect(85,75,1030,650,34,34);
            g.setColor(new Color(227,192,130));g.setStroke(new java.awt.BasicStroke(4));g.drawRoundRect(112,102,976,596,24,24);
            g.setColor(new Color(226,180,126));g.setFont(new Font("Serif",Font.ITALIC,48));g.drawString("With love and joy",345,230);
            g.setColor(Color.WHITE);g.setFont(new Font("Serif",Font.BOLD,72));g.drawString("Aarav & Meera",285,365);
            g.setColor(new Color(232,210,163));g.setFont(new Font("SansSerif",Font.PLAIN,27));g.drawString("invite you to celebrate their wedding",310,432);
            g.drawString("Sunday · 15 November · Jaipur",340,495);
            g.setColor(new Color(194,130,103));for(int i=0;i<7;i++){double r=12+i*10;g.fill(new Ellipse2D.Double(200-r,590-r,r*2,r*2));g.fill(new Ellipse2D.Double(1000-r,210-r,r*2,r*2));}
        }finally{g.dispose();}
        ImageIO.write(image,"png",file.toFile());
    }
}
