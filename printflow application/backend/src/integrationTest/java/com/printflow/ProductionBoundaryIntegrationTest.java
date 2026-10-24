package com.printflow;

import com.printflow.enquiry.DemoEnquiryController;
import com.printflow.identity.LocalStaffBootstrap;
import com.printflow.identity.LocalOwnerBootstrap;
import com.printflow.identity.LocalMailController;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("prod")
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"APP_ARTIFACT_ROOT=.local/prod-boundary-artifacts","APP_ARTIFACT_SCANNER=NONE",
    "server.address=127.0.0.1","app.owner.bootstrap.enabled=true","app.staff.bootstrap.enabled=true","app.demo.enabled=true"})
class ProductionBoundaryIntegrationTest {
    @LocalServerPort int port;
    @Autowired ApplicationContext context;
    HttpClient client=HttpClient.newHttpClient();
    HttpResponse<String> get(String path)throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void productionExcludesBootstrapAndDemoEvenIfEnabled()throws Exception {
        assertEquals(0,context.getBeanNamesForType(LocalStaffBootstrap.class).length);
        assertEquals(0,context.getBeanNamesForType(LocalOwnerBootstrap.class).length);
        assertEquals(0,context.getBeanNamesForType(LocalMailController.class).length);
        assertEquals(0,context.getBeanNamesForType(DemoEnquiryController.class).length);
        assertEquals(200,get("/actuator/health").statusCode());
        assertEquals(404,get("/api/demo/enquiries").statusCode());
        assertEquals(401,get("/api/v1/staff/enquiries").statusCode());
    }
    @Test void productionDefaultsDisableIntakeAndUseSecureCookies()throws Exception {
        String body="{\"customerName\":\"Production Synthetic\",\"email\":\"prod@example.test\",\"phone\":\"9000000000\",\"productCode\":\"WEDDING_CARDS\",\"quantity\":1}";
        var response=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/public/enquiries"))
            .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(503,response.statusCode());
        var token=get("/api/v1/auth/csrf");assertEquals(200,token.statusCode());
        String cookie=token.headers().firstValue("set-cookie").orElseThrow();
        assertTrue(cookie.contains("Secure"));assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("SameSite=Strict"));
        assertTrue(token.headers().firstValue("cache-control").orElse("").contains("no-store"));
    }
}
