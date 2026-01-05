package com.ka.identity_service.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/*
*file này sẽ tạo các annotation custome
* -Trong interface này có 3 thuộc tính cơ bản cho một annotation dành cho validation
* */
//Targer chính là tham số cho biêt annotation được applyở đâu
@Target({ElementType.FIELD})
//Retention: Annotation này sẽ được xử lí lúc nào
@Retention(RetentionPolicy.RUNTIME)
//Constraint: Class chịu trách nhiệm validate cho annotation này -> Rất quan trọng
@Constraint(
        validatedBy = {DobValidator.class}
)
//Trong annotation thì khai báo rất khắt khe
public @interface DobConstraint {
    String message() default "Invalid date of birth";

    //Tham số này là một hàm không có logic -> Đợi truyền từ ngoài vào
    int min();

    Class<?>[] groups() default {};

    //Đây là property customize
    Class<? extends Payload>[] payload() default {};
}
