package com.printflow.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(12); }
    @Bean SecurityFilterChain security(HttpSecurity http,StaffDirectory directory,OidcProviders providers,SocialAccounts social) throws Exception {
        LoginThrottle throttle=new LoginThrottle(Clock.systemUTC());
        http.addFilterBefore(new OncePerRequestFilter(){
            @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
                    throws ServletException,IOException {
                if(request.getMethod().equals("POST")&&request.getServletPath().equals("/api/v1/auth/session")) {
                    String name=request.getParameter("username");
                    String password=request.getParameter("password");
                    if(name==null||name.length()>140||!throttle.allow(request.getRemoteAddr(),name.trim().toLowerCase(Locale.ROOT))) {
                        response.setHeader("Retry-After","900"); json(response,429,"LOGIN_RATE_LIMITED");return;
                    }
                    if(password==null||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72){json(response,401,"INVALID_CREDENTIALS");return;}
                }
                if(request.getMethod().equals("POST")&&request.getServletPath().equals("/api/v1/auth/password")) {
                    var auth=SecurityContextHolder.getContext().getAuthentication();
                    if(auth!=null&&auth.getPrincipal() instanceof StaffPrincipal staff&&!throttle.allow(request.getRemoteAddr(),"password:"+staff.id())) {json(response,429,"PASSWORD_RATE_LIMITED");return;}
                }
                chain.doFilter(request,response);
            }
        },UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(new OncePerRequestFilter(){
            @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
                    throws ServletException,IOException {
                var authentication=SecurityContextHolder.getContext().getAuthentication();
                if(authentication!=null&&authentication.getPrincipal() instanceof StaffPrincipal staff&&!directory.current(staff)) {
                    SecurityContextHolder.clearContext();
                    var session=request.getSession(false);if(session!=null)session.invalidate();
                    json(response,401,"SESSION_INVALIDATED");return;
                }
                if(authentication!=null&&authentication.getPrincipal() instanceof StaffPrincipal staff) {
                    String path=request.getServletPath();
                    if(path.startsWith("/api/v1/")&&!path.startsWith("/api/v1/auth/")&&staff.mustChangePassword()) {json(response,403,"PASSWORD_CHANGE_REQUIRED");return;}
                    if(path.startsWith("/api/v1/")&&!path.startsWith("/api/v1/auth/")&&staff.role().equals("OWNER")&&!OwnerMfaController.verified(request.getSession(false))) {json(response,403,"OWNER_MFA_REQUIRED");return;}
                    if(path.equals("/api/v1/auth/password")&&staff.role().equals("OWNER")&&!OwnerMfaController.verified(request.getSession(false))) {json(response,403,"OWNER_MFA_REQUIRED");return;}
                }
                chain.doFilter(request,response);
            }
        },AnonymousAuthenticationFilter.class);
        http.csrf(csrf->csrf.ignoringRequestMatchers("/api/public/payments/razorpay/webhook","/api/public/whatsapp/webhook","/api/public/enquiries","/api/demo/**"))
            .requestCache(cache->cache.requestCache(new NullRequestCache()))
            .authorizeHttpRequests(auth->auth
                .requestMatchers("/actuator/health","/api/public/**","/api/demo/**","/api/v1/auth/csrf","/error").permitAll()
                .requestMatchers("/collaboration/ws").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/v1/auth/session").permitAll()
                .requestMatchers("/api/v1/auth/**").authenticated()
                .requestMatchers("/api/v1/staff/contacts/**","/api/v1/staff/contacts").hasAnyRole("OWNER","SALES","DESIGNER","ESTIMATOR","SUPERVISOR","OPERATOR","INVENTORY","ACCOUNTS","DELIVERY")
                .requestMatchers("/api/v1/production/**").hasAnyRole("OWNER","OPERATOR")
                .requestMatchers("/api/v1/design/enquiries/*/fulfilment","/api/v1/design/enquiries/*/fulfilment/**").hasAnyRole("CUSTOMER","OWNER","SALES","DESIGNER","OPERATOR","DELIVERY")
                .requestMatchers("/api/v1/design/**").hasAnyRole("CUSTOMER","OWNER","SALES","DESIGNER")
                .requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER")
                .requestMatchers("/api/v1/owner/**").hasRole("OWNER")
                .requestMatchers("/api/v1/staff/**").hasAnyRole("SALES","OWNER")
                .anyRequest().denyAll())
            .formLogin(form->form.loginProcessingUrl("/api/v1/auth/session")
                .successHandler((req,res,auth)->{req.getSession().removeAttribute(OwnerMfaController.VERIFIED);req.getSession().removeAttribute("MFA_PENDING");json(res,200,"SIGNED_IN");})
                .failureHandler((req,res,error)->json(res,401,"INVALID_CREDENTIALS")))
            .logout(logout->logout.logoutUrl("/api/v1/auth/logout").invalidateHttpSession(true)
                .deleteCookies("JSESSIONID").logoutSuccessHandler((req,res,auth)->json(res,200,"SIGNED_OUT")))
            .exceptionHandling(errors->errors
                .authenticationEntryPoint((req,res,error)->json(res,401,"AUTHENTICATION_REQUIRED"))
                .accessDeniedHandler((req,res,error)->json(res,403,"ACCESS_DENIED")))
            .sessionManagement(session->session.sessionFixation(fixation->fixation.changeSessionId()));
        if(providers.enabled()) {
            var resolver=new org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver(providers,"/api/public/sso/authorize");
            resolver.setAuthorizationRequestCustomizer(org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers.withPkce());
            http.oauth2Login(oauth->oauth.clientRegistrationRepository(providers)
                .authorizationEndpoint(endpoint->endpoint.authorizationRequestResolver(resolver))
                .redirectionEndpoint(endpoint->endpoint.baseUri("/api/public/sso/callback/*"))
                .authorizedClientRepository(new org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository(){
                    @Override public <T extends org.springframework.security.oauth2.client.OAuth2AuthorizedClient> T loadAuthorizedClient(String id,org.springframework.security.core.Authentication auth,HttpServletRequest req){return null;}
                    @Override public void saveAuthorizedClient(org.springframework.security.oauth2.client.OAuth2AuthorizedClient client,org.springframework.security.core.Authentication auth,HttpServletRequest req,HttpServletResponse res){}
                    @Override public void removeAuthorizedClient(String id,org.springframework.security.core.Authentication auth,HttpServletRequest req,HttpServletResponse res){}
                })
                .successHandler((req,res,auth)->{
                    try {
                        var token=(org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken)auth;
                        String target=social.callback(token.getAuthorizedClientRegistrationId(),(org.springframework.security.oauth2.core.oidc.user.OidcUser)token.getPrincipal(),req,res);
                        res.setStatus(302);res.setHeader("Cache-Control","no-store");res.setHeader("Location",target);
                    } catch(Exception failure) {
                        SecurityContextHolder.clearContext();req.getSession().removeAttribute("SPRING_SECURITY_CONTEXT");
                        res.setStatus(302);res.setHeader("Cache-Control","no-store");res.setHeader("Location","/customer?provider_error=signin_failed");
                    }
                })
                .failureHandler((req,res,error)->{var session=req.getSession(false);if(session!=null){session.removeAttribute("SSO_LINK");session.removeAttribute("SSO_PENDING");}res.setStatus(302);res.setHeader("Cache-Control","no-store");res.setHeader("Location","/customer?provider_error=signin_failed");}));
        }
        return http.build();
    }
    private static void json(HttpServletResponse response,int status,String code)throws IOException {
        response.setStatus(status);response.setContentType("application/json");response.setHeader("Cache-Control","no-store");
        response.getWriter().write("{\"code\":\""+code+"\"}");
    }
}
