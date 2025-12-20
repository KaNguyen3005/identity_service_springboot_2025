package com.ka.identity_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IdentityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityServiceApplication.class, args);
	}

}
//| type         | Ý nghĩa                                                         |
//| ------------ | --------------------------------------------------------------- |
//| feat         | Thêm tính năng mới                                              |
//| fix          | Sửa bug                                                         |
//| docs         | Cập nhật tài liệu (README…)                                     |
//| style        | Thay đổi format, không ảnh hưởng logic (format code, prettier…) |
//| refactor     | Tái cấu trúc code, không sửa bug / không thêm tính năng         |
//| perf         | Tối ưu hiệu năng                                                |
//| test         | Thêm / sửa test                                                 |
//| chore        | Việc linh tinh (config, build, CI/CD…)                          |
//| revert       | Rollback commit                                                 |