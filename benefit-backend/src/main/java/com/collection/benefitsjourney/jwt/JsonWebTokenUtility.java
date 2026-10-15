package com.collection.benefitsjourney.jwt;

import java.time.Instant;
import java.util.Date;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;


@Component
public class JsonWebTokenUtility {

    @Value("${jwt.token.expire}")
    private long expire;

    @Value("${jwt.token.secret}")
    private String secret;

    private String issuer = "BenefitsJourney";

    // 檢查 Secret 是否符合 HS512 的長度要求
    private byte[] getSecretBytes() {

        byte[] secretBytes =
                secret.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < 64) {
            throw new IllegalStateException(
                    "HS512 的 JWT Secret 至少需要 64 bytes");
        }

        return secretBytes;
    }

    // 建立 JWT
    public String createToken(String data) {

        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT subject 不可為空");
        }

        if (expire <= 0) {
            throw new IllegalStateException(
                    "JWT 過期時間必須大於 0");
        }

        try {
            byte[] secretBytes = getSecretBytes();

            Instant now = Instant.now();
            Instant expireTime =
                    now.plusSeconds(Math.multiplyExact(expire, 60L));

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expireTime))
                    .subject(data)
                    .build();

        SignedJWT signedJWT = new SignedJWT(
                        new JWSHeader(JWSAlgorithm.HS512),
                        claimsSet
                );

        signedJWT.sign(new MACSigner(secretBytes));

        return signedJWT.serialize();

        } catch (Exception e) {
                throw new IllegalStateException(
                        "建立 JWT 失敗", e);
                }
        }

        // 驗證 JWT
        public String validateToken(String token) {

                if (token == null || token.isBlank()) {
                return null;
                }

        try {
            byte[] secretBytes = getSecretBytes();

            SignedJWT signedJWT = SignedJWT.parse(token);

            // 確認簽章演算法
            if (!JWSAlgorithm.HS512.equals(
                    signedJWT.getHeader().getAlgorithm())) {
                return null;
            }

            // 驗證簽章
            JWSVerifier verifier =
                    new MACVerifier(secretBytes);

            if (!signedJWT.verify(verifier)) {
                return null;
            }

            JWTClaimsSet claimsSet =
                    signedJWT.getJWTClaimsSet();

            // 驗證簽發者
            if (!issuer.equals(claimsSet.getIssuer())) {
                return null;
            }

            // 驗證到期時間
            Date expirationTime =
                    claimsSet.getExpirationTime();

            if (expirationTime == null
                    || !new Date().before(expirationTime)) {
                return null;
            }

            // 驗證 subject
            String subject = claimsSet.getSubject();

            if (subject == null || subject.isBlank()) {
                return null;
            }

            return subject;

        } catch (Exception e) {
            // Token 格式錯誤或驗證失敗
            return null;
        }
    }
}