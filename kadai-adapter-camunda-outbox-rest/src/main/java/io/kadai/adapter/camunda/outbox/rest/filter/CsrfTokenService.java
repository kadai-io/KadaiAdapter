package io.kadai.adapter.camunda.outbox.rest.filter;

import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class CsrfTokenService {

  private static final byte TOKEN_BYTES = 16;
  private static final byte SIGNATURE_BYTES = 32;

  private final SecureRandom secureRandom;
  private final byte[] secret = new byte[TOKEN_BYTES];

  public CsrfTokenService() throws NoSuchAlgorithmException {
    secureRandom = SecureRandom.getInstanceStrong();
    secureRandom.nextBytes(secret);
  }

  public String createRandomToken() {
    try {
      byte[] tokenAndSignature = new byte[TOKEN_BYTES + SIGNATURE_BYTES];

      secureRandom.nextBytes(tokenAndSignature);

      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(tokenAndSignature, 0, TOKEN_BYTES);
      messageDigest.update(secret);

      messageDigest.digest(tokenAndSignature, TOKEN_BYTES, SIGNATURE_BYTES);

      return Base64.getEncoder().encodeToString(tokenAndSignature);
    } catch (NoSuchAlgorithmException | DigestException e) {
      throw new IllegalStateException(e);
    }
  }

  public boolean validateToken(String encodedTokenAndSignature) {
    try {
      byte[] tokenAndSignature;

      try {
        tokenAndSignature = Base64.getDecoder().decode(encodedTokenAndSignature);
      } catch (IllegalArgumentException e) {
        return false;
      }

      if (tokenAndSignature.length != TOKEN_BYTES + SIGNATURE_BYTES) {
        return false;
      }

      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(tokenAndSignature, 0, TOKEN_BYTES);
      messageDigest.update(secret);
      byte[] expectedSignature = messageDigest.digest();

      ByteBuffer tokenBuffer = ByteBuffer.wrap(tokenAndSignature, TOKEN_BYTES, SIGNATURE_BYTES);
      ByteBuffer expectedBuffer = ByteBuffer.wrap(expectedSignature, 0, SIGNATURE_BYTES);

      return tokenBuffer.equals(expectedBuffer);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
