package com.ka.identity_service.service;

import com.ka.identity_service.dto.request.UserCreationRequest;
import com.ka.identity_service.dto.response.UserResponse;
import com.ka.identity_service.entity.User;
import com.ka.identity_service.exception.AppException;
import com.ka.identity_service.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test tầng Service cho UserService.
 *
 * @SpringBootTest: load toàn bộ Spring Context → test gần giống chạy thật.
 *
 * @MockBean UserRepository:
 *  - Repository thật sẽ bị thay bằng mock
 *  - Không truy cập database thật
 */
@SpringBootTest
@TestPropertySource("/test.properties")
public class UserServiceTest {

    // Inject UserService thật từ Spring Context
    @Autowired
    private UserService userService;

    // Mock UserRepository để chủ động điều khiển kết quả DB
    @MockBean
    private UserRepository userRepository;

    // Request giả lập dữ liệu client gửi lên API
    private UserCreationRequest request;

    // Response giả lập dữ liệu service trả về
    private UserResponse userResponse;

    // Ngày sinh mẫu dùng chung
    private LocalDate dob;

    // Entity User giả lập dữ liệu lưu trong DB
    private User user;

    /**
     * @BeforeEach
     * Hàm này chạy trước MỖI test case
     * Dùng để khởi tạo dữ liệu mẫu tránh lặp code
     */
    @BeforeEach
    public void initData() {
        dob = LocalDate.of(1990, 1, 1);

        // Giả lập dữ liệu người dùng gửi lên khi tạo user
        request = UserCreationRequest.builder()
                .username("kakaka2")
                .firstName("john")
                .lastName("Doe")
                .password("12345678")
                .dob(dob)
                .build();

        // Giả lập dữ liệu service sẽ trả về sau khi tạo thành công
        userResponse = UserResponse.builder()
                .id("4055f7be-defd-4c5c-8195-ab7108ba1121")
                .username("john12")
                .firstName("john")
                .lastName("Doe")
                .dob(dob)
                .build();

        // Giả lập entity User được lưu trong database
        user = User.builder()
                .id("4055f7be-defd-4c5c-8195-ab7108ba1121")
                .username("john12")
                .firstName("john")
                .lastName("Doe")
                .password("12345678")
                .dob(dob)
                .build();
    }

    /**
     * Test case: tạo user thành công
     * Điều kiện:
     *  - Username chưa tồn tại trong DB
     *  - Lưu user thành công
     */
    @Test
    void createUser_validRequest_success() {

        // ===== GIVEN =====
        // Giả lập: kiểm tra username chưa tồn tại
        when(userRepository.existsByUsername(anyString()))
                .thenReturn(false);

        // Giả lập: khi save user → trả về entity user
        when(userRepository.save(any()))
                .thenReturn(user);

        // ===== WHEN =====
        // Gọi service thật
        var response = userService.createUser(request);

        // ===== THEN =====
        // Kiểm tra kết quả trả về đúng như mong đợi
        Assertions.assertThat(response.getId())
                .isEqualTo("4055f7be-defd-4c5c-8195-ab7108ba1121");

        Assertions.assertThat(response.getUsername())
                .isEqualTo("john12");
    }

    /**
     * Test case: tạo user thất bại vì username đã tồn tại
     * Điều kiện:
     *  - existsByUsername trả về true
     *  - Service phải ném AppException
     */
    @Test
    void createUser_userExisted_fail() {

        // ===== GIVEN =====
        // Giả lập: username đã tồn tại trong DB
        when(userRepository.existsByUsername(anyString()))
                .thenReturn(true);

        // ===== WHEN =====
        // Gọi service và kỳ vọng ném exception
        var exception = assertThrows(AppException.class,
                () -> userService.createUser(request));

        // ===== THEN =====
        // Kiểm tra mã lỗi đúng như thiết kế
        Assertions.assertThat(exception.getErrorcode().getCode())
                .isEqualTo(1001);
    }

    @Test
// Giả lập một Security Context với username là "john12" để vượt qua lớp bảo mật (Spring Security)
    @WithMockUser(username = "john12")
    void getMyInfo_valid_success() {
        // GIVEN: Thiết lập hành vi giả lập cho repository
        // Khi gọi hàm findByUsername với bất kỳ chuỗi nào, trả về một Optional chứa đối tượng user đã tạo sẵn
        when(userRepository.findByUsername(anyString()))
                .thenReturn(Optional.of(user));

        // WHEN: Thực hiện gọi hàm nghiệp vụ cần kiểm thử
        var response = userService.getMyInfo();

        // THEN: Kiểm tra kết quả trả về (Assertion)
        // Xác nhận username trong kết quả phải khớp với "john12"
        Assertions.assertThat(response.getUsername()).isEqualTo("john12");
        // Xác nhận ID người dùng khớp với giá trị mong đợi
        Assertions.assertThat(response.getId()).isEqualTo("4055f7be-defd-4c5c-8195-ab7108ba1121");
    }

    @Test
// Giả lập Security Context với username "john12"
    @WithMockUser(username = "john12")
    void getMyInfo_userNotFound_error() {
        // GIVEN: Thiết lập tình huống không tìm thấy người dùng trong cơ sở dữ liệu
        // Khi tìm kiếm theo username, trả về kết quả rỗng (null/empty)
        when(userRepository.findByUsername(anyString()))
                .thenReturn(Optional.ofNullable(null));

        // WHEN & THEN: Thực thi hành động và kiểm tra ngoại lệ
        // Mong đợi hệ thống tung ra một AppException khi không tìm thấy thông tin
        var exception = assertThrows(AppException.class,
                () -> userService.getMyInfo());

        // Xác nhận rằng mã lỗi (Error Code) trả về đúng là 1005 (thường là USER_NOT_EXISTED)
        Assertions.assertThat(exception.getErrorcode().getCode()).isEqualTo(1005);
    }
}
