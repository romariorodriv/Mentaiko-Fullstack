package com.mentaiko.backend.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mentaiko.backend.entity.User;
import com.mentaiko.backend.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.debug("JWT authentication skipped for path={} because bearer token is absent", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);

        try {
            String email = jwtService.extractSubject(token);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
                if (user != null && user.isActive() && jwtService.isTokenValid(token, user)) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT authentication succeeded for user id={} path={}", user.getId(), request.getRequestURI());
                } else if (user == null) {
                    log.warn("JWT authentication rejected for path={} because token subject does not match an existing user",
                            request.getRequestURI());
                } else if (!user.isActive()) {
                    log.warn("JWT authentication rejected for user id={} path={} because user is inactive",
                            user.getId(), request.getRequestURI());
                } else {
                    log.warn("JWT authentication rejected for user id={} path={} because token is invalid or expired",
                            user.getId(), request.getRequestURI());
                }
            }
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            log.warn("JWT authentication rejected for path={} because token could not be parsed", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}