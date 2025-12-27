package org.example.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(firebaseTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // Existing public endpoint
                        .requestMatchers("/health").permitAll()
                        // Require authentication for all /protected/** routes
                        .requestMatchers("/protected/**").authenticated()
                        // Permit all other requests
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public OncePerRequestFilter firebaseTokenFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {

                String authorizationHeader = request.getHeader("Authorization");

                if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                    String token = authorizationHeader.substring(7);

                    try {
                        // Verify the token
                        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifySessionCookie(token);

                        // Extract user ID
                        String userId = decodedToken.getUid();

                        // Create authentication object with user ID as principal
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );

                        // Set the authentication in SecurityContext
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                    } catch (FirebaseAuthException e) {
                        logger.error("Firebase authentication failed: ",e);
                        // Token invalid
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
                        return; // Stop filter chain
                    }
                } else if (request.getRequestURI().startsWith("/protected/")) {
                    logger.warn("Missing Authorization header for protected endpoint" + request.getRequestURI());

                    // Missing token for protected endpoint
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Missing token\"}");
                    return; // Stop filter chain
                }

                filterChain.doFilter(request, response);
            }
        };
    }
}