// Khai báo package chứa class cấu hình Spring Security
package com.ka.identity_service.configuration;

// Import các annotation và class cần thiết cho cấu hình bảo mật
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;

// Đánh dấu đây là class cấu hình (Configuration) của Spring
@Configuration

// Bật Spring Security cho application
@EnableWebSecurity

// Bật bảo mật ở tầng method (cho phép dùng @PreAuthorize, @PostAuthorize, ...)
@EnableMethodSecurity
public class WebSecurityConfig {

    // Danh sách các endpoint public (không cần đăng nhập)
    // Chỉ áp dụng cho method POST (config ở dưới)
    private final String[] PUBLIC_ENDPOINTS = {
            "/user",
            "/auth/token",
            "/auth/introspect",
            "/auth/logout"
    };

    // Lấy secret key dùng để ký và verify JWT từ file application.properties / yml
    @Value("${jwt.signerKey}")
    private String signerKey;

    // Bean cấu hình chuỗi filter bảo mật của Spring Security
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

        // Cấu hình phân quyền cho các request HTTP
        httpSecurity.authorizeHttpRequests(request ->
                request
                        // Cho phép gọi POST vào các endpoint public mà không cần token
                        .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()

                        // Ví dụ phân quyền theo role / authority
                        // .requestMatchers(HttpMethod.GET, "/users")
                        // .hasAuthority("ROLE_ADMIN") // hoặc hasRole("ADMIN")

                        // Tất cả các request còn lại đều phải xác thực (có JWT hợp lệ)
                        .anyRequest().authenticated()
        );

        // Cấu hình application hoạt động như một OAuth2 Resource Server
        // Tức là server sẽ:
        // - Nhận JWT từ client
        // - Verify JWT
        // - Trích xuất thông tin user + quyền
        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2
                        // Cấu hình xử lý JWT
                        .jwt(jwtConfigurer ->
                                jwtConfigurer
                                        // Decoder dùng để verify và decode JWT
                                        .decoder(jwtDecoder())

                                        // Converter dùng để convert JWT -> Authentication
                                        // (trích xuất role, permission từ token)
                                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        // Xử lý khi xác thực thất bại (401 Unauthorized)
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
        );

        // Disable CSRF
        // Thường dùng cho REST API (stateless, không dùng session)
        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        // Build và trả về SecurityFilterChain
        return httpSecurity.build();
    }

    /*
     * Bean dùng để convert JWT thành Authentication object
     * Nhiệm vụ chính:
     * - Lấy scope / authority từ JWT
     * - Convert thành GrantedAuthority cho Spring Security
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(){

        // Converter dùng để lấy authority từ claim trong JWT
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        // Mặc định Spring sẽ tự thêm prefix "ROLE_"
        // Ở đây set prefix = "" vì trong JWT đã có ROLE_ sẵn
        // Ví dụ: ROLE_ADMIN, USER_CREATE
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        // Converter chính để tạo Authentication từ JWT
        JwtAuthenticationConverter jwtAuthenticationConverter =
                new JwtAuthenticationConverter();

        // Gán converter authority vào JWT Authentication Converter
        jwtAuthenticationConverter
                .setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

    /*
     * Bean dùng để decode và verify JWT token
     * Kiểm tra:
     * - Chữ ký token
     * - Thuật toán ký (HS512)
     */
    @Bean
    JwtDecoder jwtDecoder(){

        // Tạo secret key từ signerKey
        // HS512 là thuật toán HMAC sử dụng secret key
        SecretKeySpec secretKeySpec =
                new SecretKeySpec(signerKey.getBytes(), "HS512");

        // Tạo JwtDecoder dùng Nimbus
        // Decoder này sẽ:
        // - Verify chữ ký
        // - Decode payload JWT
        return NimbusJwtDecoder
                .withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    /*
     * Bean mã hóa mật khẩu
     * BCrypt là thuật toán được Spring Security khuyến nghị
     * Strength = 10 (độ phức tạp)
     */
    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(10);
    }
}
