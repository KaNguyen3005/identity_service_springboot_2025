package com.ka.identity_service.service;

import com.ka.identity_service.dto.request.AuthenticationRequest;
import com.ka.identity_service.dto.request.IntrospectRequest;
import com.ka.identity_service.dto.request.LogoutRequest;
import com.ka.identity_service.dto.response.AuthenticationResponse;
import com.ka.identity_service.dto.response.IntrospectResponse;
import com.ka.identity_service.entity.InvalidatedToken;
import com.ka.identity_service.entity.User;
import com.ka.identity_service.exception.AppException;
import com.ka.identity_service.exception.ErrorCode;
import com.ka.identity_service.repository.InvalidatedTokenRepository;
import com.ka.identity_service.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    // Hàm dùng để introspect JWT token
    // Mục đích: kiểm tra token có hợp lệ hay không (đúng chữ ký, chưa hết hạn, chưa bị logout...)
    public IntrospectResponse introspect(IntrospectRequest request)
            throws JOSEException, ParseException {

        // Lấy JWT token từ request gửi lên
        // Thường token này được client gửi khi cần kiểm tra trạng thái đăng nhập
        var token = request.getToken();

        // Xác thực token
        // Bên trong verifyToken thường sẽ:
        // - Verify chữ ký JWT
        // - Kiểm tra token hết hạn (exp)
        // - Kiểm tra token có nằm trong blacklist hay không
        // Nếu token không hợp lệ → method này sẽ throw exception
        boolean isValid = true;
        try {
            verifyToken(token);
        }
        catch(AppException e){
            isValid = false;
        }

        // Nếu chạy được tới đây nghĩa là token hợp lệ
        // Trả về response với valid = true
        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }


    public AuthenticationResponse authenticate(AuthenticationRequest request){
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if(!authenticated)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        // Khởi tạo đối tượng JWTClaimsSet bằng Builder pattern
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()

                // Subject (sub): định danh chính của token, thường là username hoặc userId
                .subject(user.getUsername())

                // Issuer (iss): đơn vị phát hành token
                .issuer("kaakaa.com")

                // Issue Time (iat): thời điểm token được tạo
                .issueTime(new Date())

                // Expiration Time (exp): thời điểm token hết hạn
                // Ở đây token sẽ hết hạn sau 1 giờ kể từ thời điểm hiện tại
                .expirationTime(new Date(
                        Instant.now()
                                .plus(1, ChronoUnit.HOURS)
                                .toEpochMilli()
                ))

                // JWT ID (jti): định danh duy nhất của token
                // Thường dùng để kiểm soát token (ví dụ: blacklist, logout)
                // Tạo một UUID (Universally Unique Identifier) ngẫu nhiên
                // UUID này có xác suất trùng lặp cực kỳ thấp
                // toString() chuyển UUID thành chuỗi theo định dạng chuẩn: 8-4-4-4-12
                // Ví dụ: "550e8400-e29b-41d4-a716-446655440000"
                .jwtID(UUID.randomUUID().toString())


                // Custom claim: scope
                // Lưu thông tin quyền hạn / role của user
                .claim("scope", buildScope(user))

                // Xây dựng đối tượng JWTClaimsSet
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Can not create token", e);
            throw new RuntimeException(e);
        }
    }

    // Hàm dùng để build chuỗi scope (authorities) đưa vào JWT token
// Scope này sẽ chứa ROLE và PERMISSION của user
    private String buildScope(User user){
        // StringJoiner dùng để nối các chuỗi với nhau, ngăn cách bằng dấu space
        // Ví dụ kết quả: "ROLE_ADMIN USER_CREATE USER_DELETE"
        StringJoiner stringJoiner = new StringJoiner(" ");

        // Kiểm tra user có role hay không
        // Tránh NullPointerException khi user chưa được gán role
        if(!CollectionUtils.isEmpty(user.getRoles())){

            // Duyệt từng role của user
            user.getRoles().forEach(role -> {
                // Thêm role vào scope
                // Spring Security yêu cầu role phải có tiền tố "ROLE_"
                // Ví dụ: ADMIN -> ROLE_ADMIN
                stringJoiner.add("ROLE_" + role.getName());
                // Kiểm tra role có permission hay không
                if(!CollectionUtils.isEmpty(role.getPermissions()))
                    // Duyệt từng permission của role

                    role.getPermissions()
                            // Thêm permission vào scope
                            // Permission không cần tiền tố "ROLE_"
                            // Ví dụ: USER_CREATE, USER_DELETE
                            .forEach(permission -> stringJoiner.add(permission.getName()));
            });
        }
        // Trả về chuỗi scope hoàn chỉnh để nhét vào JWT token
        return stringJoiner.toString();
    }

    // Hàm xử lý logout
    // Mục đích: vô hiệu hóa JWT hiện tại bằng cách lưu token vào blacklist
    // Sau khi logout, token này sẽ không còn sử dụng được dù chưa hết hạn
    public void logout(LogoutRequest request) throws ParseException, JOSEException {

        // Verify JWT token để đảm bảo:
        // - Token hợp lệ
        // - Chữ ký đúng
        // - Token chưa hết hạn
        // Nếu không hợp lệ → exception được throw ra
        var signedToken = verifyToken(request.getToken());

        // Lấy JWT ID (jti) từ token
        // jti là định danh duy nhất của mỗi JWT
        // Dùng để nhận diện token khi cần blacklist
        String jti = signedToken.getJWTClaimsSet().getJWTID();

        // Lấy thời gian hết hạn của token
        // Thời điểm này dùng để biết khi nào có thể xóa token khỏi blacklist
        Date expiryTime = signedToken.getJWTClaimsSet().getExpirationTime();

        // Tạo entity InvalidatedToken
        // Entity này đại diện cho một token đã bị logout (bị vô hiệu hóa)
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                // Lưu jti làm khóa chính
                .id(jti)
                // Lưu thời gian hết hạn của token
                .expiryTime(expiryTime)
                .build();

        // Lưu token đã bị vô hiệu hóa vào database (hoặc Redis)
        // Mỗi request sau này sẽ kiểm tra:
        // - jti có tồn tại trong bảng invalidated_token hay không
        // Nếu có → từ chối truy cập
        invalidatedTokenRepository.save(invalidatedToken);
    }


    // Hàm dùng để verify (xác thực) JWT token
    // Trả về SignedJWT nếu token hợp lệ
    // Nếu token không hợp lệ → throw exception
    private SignedJWT verifyToken(String token) throws JOSEException, ParseException {

        // Tạo verifier để kiểm tra chữ ký JWT
        // MACVerifier dùng cho JWT ký bằng thuật toán HMAC (HS256, HS512, ...)
        // SIGNER_KEY là secret key dùng để ký và verify token
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        // Parse chuỗi token (String) thành đối tượng SignedJWT
        // Nếu token sai format → ParseException
        SignedJWT signedJWT = SignedJWT.parse(token);

        // Lấy thời gian hết hạn (exp) từ payload của JWT
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        // Verify chữ ký của JWT
        // true  → chữ ký hợp lệ (token không bị chỉnh sửa)
        // false → chữ ký không hợp lệ
        boolean verified = signedJWT.verify(verifier);

        // Kiểm tra 2 điều kiện bắt buộc:
        // 1. Chữ ký hợp lệ (verified == true)
        // 2. Token chưa hết hạn (expiryTime sau thời điểm hiện tại)
        // Nếu 1 trong 2 điều kiện sai → token không hợp lệ
        if(!(verified && expiryTime.after(new Date())))
            // Ném exception xác thực thất bại
            // Thường sẽ map sang HTTP 401 Unauthorized
            throw new AppException(ErrorCode.UNAUTHENTICATED);


        if(invalidatedTokenRepository
                .existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        // Nếu token hợp lệ → trả về SignedJWT
        // Object này có thể dùng tiếp để:
        // - Lấy username (sub)
        // - Lấy scope / role / permission
        return signedJWT;
    }
}
