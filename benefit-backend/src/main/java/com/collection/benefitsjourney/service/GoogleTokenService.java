package com.collection.benefitsjourney.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Service 
public class GoogleTokenService {
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenService(
            @Value("${google.client-id}") String clientId) {

        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleIdToken.Payload verify(String idToken) {

        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);

            if (googleIdToken == null) {
                throw new IllegalArgumentException(
                        "Google ID Token 無效或已過期");
            }

            GoogleIdToken.Payload payload =
                    googleIdToken.getPayload();

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new IllegalArgumentException(
                        "Google Email 尚未通過驗證");
            }

            return payload;

        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException(
                    "Google ID Token 驗證失敗", e);
        }
    }
}
