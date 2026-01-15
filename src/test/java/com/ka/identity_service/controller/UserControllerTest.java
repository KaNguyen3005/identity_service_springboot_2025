package com.ka.identity_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ka.identity_service.dto.request.UserCreationRequest;
import com.ka.identity_service.dto.response.UserResponse;
import com.ka.identity_service.service.UserService;
import com.ka.identity_service.service.UserServiceTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("/test.properties")
public class UserControllerTest {

    // MockMvc dùng để giả lập việc gửi HTTP request vào Controller
    // mà không cần chạy server thật
    @Autowired
    private MockMvc mockMvc;

    // @MockBean: tạo một bean giả (mock) của UserService
    // Bean thật trong Spring Context sẽ bị thay thế bằng mock này
    @MockBean
    private UserService userService;

    // Đối tượng request giả lập dữ liệu gửi lên API
    private UserCreationRequest request;

    // Đối tượng response giả lập dữ liệu service trả về
    private UserResponse userResponse;

    // Ngày sinh mẫu
    private LocalDate dob;

    // @BeforeEach: chạy trước mỗi test case
    // Khởi tạo dữ liệu test mẫu
    @BeforeEach
    public void initData(){
        dob = LocalDate.of(1990,1,1);

        // Dữ liệu gửi vào API tạo user
        request = UserCreationRequest.builder()
                .username("john12")
                .firstName("john")
                .lastName("Doe")
                .password("12345678")
                .dob(dob)
                .build();

        // Dữ liệu service trả về sau khi tạo user thành công
        userResponse = UserResponse.builder()
                .id("4055f7be-defd-4c5c-8195-ab7108ba1121")
                .username("john12")
                .firstName("john")
                .lastName("Doe")
                .dob(dob)
                .build();
    }

    // Test case: kiểm tra API tạo user khi request hợp lệ
    @Test
    void createUser_validRequest_success() throws Exception {
        // ===== GIVEN =====
        // ObjectMapper dùng để convert object Java -> JSON string
        ObjectMapper objectMapper = new ObjectMapper();

        // Đăng ký module hỗ trợ kiểu LocalDate (Java 8 Time)
        objectMapper.registerModule(new JavaTimeModule());

        // Chuyển request object thành JSON để gửi trong body HTTP
        String content = objectMapper.writeValueAsString(request);

        // Giả lập hành vi của userService.createUser(...)
        // Khi controller gọi service.createUser(...)
        // thì mock sẽ trả về userResponse thay vì gọi logic thật
        Mockito.when(userService.createUser(ArgumentMatchers.any())).thenReturn(userResponse);

        // ===== WHEN & THEN =====
        // Thực hiện request POST /users
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/users")                       // URL API
                        .contentType(MediaType.APPLICATION_JSON_VALUE) // Kiểu dữ liệu gửi đi là JSON
                        .content(content))                    // Body JSON
                // Kiểm tra HTTP status trả về là 200 OK
                .andExpect(MockMvcResultMatchers.status().isOk())
                // Kiểm tra trong JSON response có field "code" = 1000
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("result.id").value("4055f7be-defd-4c5c-8195-ab7108ba1121"));

    }
    @Test
    void createUser_usernameInvalid_fail() throws Exception {
        // ===== GIVEN =====
        request.setUsername("joh");

        // ObjectMapper dùng để convert object Java -> JSON string
        ObjectMapper objectMapper = new ObjectMapper();

        // Đăng ký module hỗ trợ kiểu LocalDate (Java 8 Time)
        objectMapper.registerModule(new JavaTimeModule());

        // Chuyển request object thành JSON để gửi trong body HTTP
        String content = objectMapper.writeValueAsString(request);

        // Giả lập hành vi của userService.createUser(...)
        // Khi controller gọi service.createUser(...)
        // thì mock sẽ trả về userResponse thay vì gọi logic thật
        Mockito.when(userService.createUser(ArgumentMatchers.any()))
                .thenReturn(userResponse);

        // ===== WHEN & THEN =====
        // Thực hiện request POST /users
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/users")                       // URL API
                        .contentType(MediaType.APPLICATION_JSON_VALUE) // Kiểu dữ liệu gửi đi là JSON
                        .content(content))                    // Body JSON
                // Kiểm tra HTTP status trả về là 200 OK
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                // Kiểm tra trong JSON response có field "code" = 1000
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1002))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("Username must be at least 4 characters"));

    }
}
