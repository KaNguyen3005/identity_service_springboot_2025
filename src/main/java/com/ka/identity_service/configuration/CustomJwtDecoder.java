package com.ka.identity_service.configuration;

import com.ka.identity_service.dto.request.IntrospectRequest;
import com.ka.identity_service.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.util.Objects;
// Custom JwtDecoder
// Class này dùng để override cách Spring Security decode JWT
// Mục đích chính:
// - Gọi introspect để kiểm tra token có bị logout (blacklist) hay không
// - Sau đó mới decode JWT như bình thường
public class CustomJwtDecoder implements JwtDecoder {

    // Secret key dùng để verify chữ ký JWT
    // Lấy từ application.properties / application.yml
    @Value("${jwt.signerKey}")
    private String signerKey;

    // Service dùng để introspect token
    // Thường dùng để:
    // - Verify token
    // - Kiểm tra token có nằm trong blacklist hay không
    @Autowired
    private AuthenticationService authenticationService;

    // NimbusJwtDecoder dùng để decode JWT
    // Khởi tạo lazy (chỉ tạo khi cần)
    private NimbusJwtDecoder nimbusJwtDecoder = null;

    // Override method decode của JwtDecoder
    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            // Gọi introspect để kiểm tra token
            // Nếu token:
            // - Không hợp lệ
            // - Hết hạn
            // - Đã bị logout
            // → method này sẽ throw exception
            authenticationService.introspect(
                    IntrospectRequest.builder()
                            .token(token)
                            .build()
            );
        }
        catch (JOSEException | ParseException e) {
            // Nếu có lỗi trong quá trình introspect
            // Chuyển exception sang JwtException để Spring Security hiểu
            throw new JwtException(e.getMessage());
        }

        // Khởi tạo NimbusJwtDecoder nếu chưa có
        // Tránh tạo lại decoder nhiều lần
        if (Objects.isNull(nimbusJwtDecoder)) {

            // Tạo secret key từ signerKey
            // Thuật toán HMAC HS512
            SecretKeySpec secretKeySpec =
                    new SecretKeySpec(signerKey.getBytes(), "HS512");

            // Tạo NimbusJwtDecoder với secret key và thuật toán HS512
            nimbusJwtDecoder = NimbusJwtDecoder
                    .withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();
        }

        // Log decoder (chỉ dùng cho debug, không nên dùng trong production)
        System.out.println(nimbusJwtDecoder);

        // Decode JWT:
        // - Verify chữ ký
        // - Parse payload
        // - Trả về đối tượng Jwt cho Spring Security sử dụng
        return nimbusJwtDecoder.decode(token);
    }
}
