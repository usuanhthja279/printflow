package com.printflow.collaboration;

import com.printflow.identity.OwnerMfaController;
import com.printflow.identity.StaffPrincipal;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.ServletServerHttpRequest;

final class CollaborationHandshakeInterceptor implements HandshakeInterceptor {
    static final String STAFF="collab.staff";
    static final String HTTP_SESSION="collab.httpSession";
    @Override public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Map<String,Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servlet)) return false;
        var principal=servlet.getServletRequest().getUserPrincipal();
        if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof StaffPrincipal staff) || !staff.isEnabled() || staff.mustChangePassword()) return false;
        HttpSession session=servlet.getServletRequest().getSession(false);
        if (session==null || staff.role().equals("OWNER")&&!OwnerMfaController.verified(session)) return false;
        attributes.put(STAFF,staff);
        attributes.put(HTTP_SESSION,session);
        return true;
    }
    @Override public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Exception exception) { }
}
