package com.acc.somsomparty.global.util;
//
//import com.acc.somsomparty.global.exception.CustomException;
//import com.acc.somsomparty.global.exception.error.ErrorCode;
//import com.auth0.jwk.Jwk;
//import com.auth0.jwk.JwkProvider;
//import com.auth0.jwk.JwkProviderBuilder;
//import com.auth0.jwt.JWT;
//import com.auth0.jwt.algorithms.Algorithm;
//import com.auth0.jwt.interfaces.DecodedJWT;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.security.interfaces.RSAPublicKey;
//import java.util.concurrent.TimeUnit;

//@Component
//public class JwtUtil {
//    private final JwkProvider jwkProvider;
//    private final String jwksUrl;
//
//    private final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
//
//    public JwtUtil(@Value("${aws.auth.cognito.jwks-url}") String jwksUrl) {
//        this.jwksUrl = jwksUrl;
//        this.jwkProvider = new JwkProviderBuilder(jwksUrl)
//                .cached(10, 1, TimeUnit.HOURS)
//                .build();
//    }
//
//    public DecodedJWT verifyToken(String token) {
//        try {
//            logger.info("Starting token verification process");
//
//            // 디코드 및 kid 추출
//            DecodedJWT jwt = JWT.decode(token);
//            logger.debug("Token decoded successfully. Key ID: {}", jwt.getKeyId());
//
//            // JWK 가져오기
//            String kid = jwt.getKeyId();
//            Jwk jwk = jwkProvider.get(kid);
//            logger.debug("JWK fetched successfully for Key ID: {}", kid);
//
//            // RSA 공개 키 및 검증
//            RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();
//            jwt = JWT.require(Algorithm.RSA256(publicKey, null))
//                    .withIssuer(jwksUrl)
//                    .build()
//                    .verify(token);
//
//            logger.info("Token verification successful");
//
//            return jwt;
//        } catch (Exception e) {
//            logger.error("Token verification failed: {}", e.getMessage(), e);
//            throw new CustomException(ErrorCode.TOKEN_VERIFICATION_FAILED);
//        }
//    }
//
//    public String getUsernameFromToken(String token) {
//        DecodedJWT decodedJWT = verifyToken(token);
//        return decodedJWT.getClaim("username").asString();
//    }
//}