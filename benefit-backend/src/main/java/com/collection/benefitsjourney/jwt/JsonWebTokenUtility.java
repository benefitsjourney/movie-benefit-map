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

    // 建立 JWT
    public String createToken(String data) {

        Instant now = Instant.now();
        Instant expireTime = now.plusSeconds(this.expire * 60);

        try {

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

            signedJWT.sign(
                    new MACSigner(
                            secret.getBytes(StandardCharsets.UTF_8)
                    )
            );

            return signedJWT.serialize();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // 驗證 JWT
    public String validateToken(String token) {

        try {

            SignedJWT signedJWT = SignedJWT.parse(token);

            JWTClaimsSet claimsSet =
                    signedJWT.getJWTClaimsSet();

            JWSVerifier verifier =
                    new MACVerifier(
                            secret.getBytes(StandardCharsets.UTF_8)
                    );

            boolean signatureValid =
                    signedJWT.verify(verifier);

            boolean notExpired =
                    new Date().before(
                            claimsSet.getExpirationTime()
                    );

            boolean correctIssuer =
                    issuer.equals(
                            claimsSet.getIssuer()
                    );

            if (signatureValid
                    && notExpired
                    && correctIssuer) {

                return claimsSet.getSubject();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}