//package com.acc.somsomparty.global.filter;
//
//import com.acc.somsomparty.global.exception.CustomException;
//import com.acc.somsomparty.global.exception.error.ErrorCode;
//import com.acc.somsomparty.global.util.JwtUtil;
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.Collections;
//import java.util.List;
//
//@Component
//public class JwtAuthFilter implements Filter {
//    private final JwtUtil jwtUtil;
//
//    public JwtAuthFilter(JwtUtil jwtUtil) {
//        this.jwtUtil = jwtUtil;
//    }
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//        HttpServletRequest httpRequest = (HttpServletRequest) request;
//        HttpServletResponse httpResponse = (HttpServletResponse) response;
//
//        String authorizationHeader = httpRequest.getHeader("Authorization");
//
//        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
//            String token = authorizationHeader.substring(7);
//
//            try {
//                String username = jwtUtil.getUsernameFromToken(token);
//
//                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
//                        new SimpleGrantedAuthority("USER")
//                );
//
//                UserDetails userDetails = User.builder()
//                        .username(username)
//                        .password("")
//                        .authorities(authorities)
//                        .build();
//
//                Authentication auth = new UsernamePasswordAuthenticationToken(
//                        userDetails, null, authorities
//                );
//
//                SecurityContextHolder.getContext().setAuthentication(auth);
//
//            } catch (Exception e) {
//                SecurityContextHolder.clearContext();
//                throw new CustomException(ErrorCode.TOKEN_VERIFICATION_FAILED);
//            }
//        }
//        chain.doFilter(httpRequest, httpResponse);
//    }
//}