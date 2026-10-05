package com.collection.benefitsjourney.jwt;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.annotation.PostConstruct;

@Component
public class JsonWebTokenUtility {
	@Value("${jwt.token.expire}")
	private long expire;
	private String issuer = "ispan";

	private byte[] signingKey;		// 用在簽章
	@PostConstruct
	public void generateSigningKey() {
		// 需要長度是512-bit的金鑰以便使用HS512演算法製作簽章
		signingKey = new byte[64];

		// TODO：可以使用其他方式產生金鑰內容
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(signingKey);
	}

	public String createToken(String data) {
		Instant now = Instant.now();
		Instant expire = now.plusSeconds(this.expire * 60);
		try {
			// 建立JWT主體
			JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
					.issuer(issuer)
					.issueTime(Date.from(now))
					.expirationTime(Date.from(expire))
					.subject(data)
					.build();

			// 建立JWS(使用HS512簽章的JWT)：header+主體
			SignedJWT signedJWT = new SignedJWT(
					new JWSHeader(JWSAlgorithm.HS512),
					claimsSet);

			// 使用HMAC signer簽章
			signedJWT.sign(new MACSigner(signingKey));

			// 產生Token
			return signedJWT.serialize();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String validateToken(String token) {
		try {
			// 解析JWS
			SignedJWT signedJWT = SignedJWT.parse(token);

			// 取得JWT主體
			JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

			// 建立HMAC verifier
			JWSVerifier verifier = new MACVerifier(signingKey);

			// 驗證簽章 + 驗證過期時間
			if (signedJWT.verify(verifier) && new Date().before(claimsSet.getExpirationTime())) {
				String subject = claimsSet.getSubject();
				return subject;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private SecretKey secretKey;	// 用在加密
	@PostConstruct
	public void generateSecretKey() {
        // 取得A256GCM演算法所需要的金鑰長度
		int length = EncryptionMethod.A256GCM.cekBitLength();
        try {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(length);
			secretKey = keyGen.generateKey();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new ExceptionInInitializerError(e);
		}
	}

	public String createEncryptedToken(String data) {
		Instant now = Instant.now();
		Instant expire = now.plusSeconds(this.expire * 60);
		try {
			// 建立JWT主體
			JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
					.issuer(issuer)
					.issueTime(Date.from(now))
					.expirationTime(Date.from(expire))
					.subject(data)
					.build();

			// 建立JWS(使用HS512簽章的JWT)：header+主體
			SignedJWT signedJWT = new SignedJWT(
					new JWSHeader(JWSAlgorithm.HS512),
					claimsSet);

			// 使用HMAC signer簽章
			signedJWT.sign(new MACSigner(signingKey));

			// 建立JWE Payload
			Payload payload= new Payload(signedJWT);

			// 建立JWE 
			JWEObject jweObject = new JWEObject(
				new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM),
				payload);

			// 使用Encrypter加密
			jweObject.encrypt(new DirectEncrypter(secretKey));

			// 產生Token
			return jweObject.serialize();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String validateEncryptedToken(String token) {
		try {
			// 解析JWE
			JWEObject jweObject = JWEObject.parse(token);

			// 使用Decrypter解密
			jweObject.decrypt(new DirectDecrypter(secretKey));

			// 取得JWE Payload
			Payload payload = jweObject.getPayload();

			// 取得JWS
			SignedJWT signedJWT = payload.toSignedJWT();

			// 取得JWT主體
			JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

			// 建立HMAC verifier
			JWSVerifier verifier = new MACVerifier(signingKey);

			// 驗證簽章 + 驗證過期時間
			if (signedJWT.verify(verifier) && new Date().before(claimsSet.getExpirationTime())) {
				String subject = claimsSet.getSubject();
				return subject;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
