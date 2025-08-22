package com.acc.somsomparty.global.filter;

import com.acc.somsomparty.global.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

//@Component
//public class JwtExceptionFilter extends OncePerRequestFilter {
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException {
//        response.setCharacterEncoding("utf-8");
//
//        try{
//            filterChain.doFilter(request, response);
//        } catch (CustomException e){
//            response.setStatus(e.getErrorCode().getHttpStatus().value());  // 401 Unauthorized
//            response.getWriter().write(e.getMessage());  // CustomException 메시지 전달
//            return;
//        }
//    }
//}