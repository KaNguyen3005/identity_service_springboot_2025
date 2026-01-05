package com.ka.identity_service.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class DobValidator implements ConstraintValidator<DobConstraint, LocalDate> {

    private int min;

    /*
    * isValid là hàm xử lí xem data có đúng hay không
    * */
    @Override
    public boolean isValid(LocalDate localDate, ConstraintValidatorContext constraintValidatorContext) {
        //Bestpractice: mỗi annotation chỉ nên chịu trách nhiệm một validation thôi
        if(Objects.isNull(localDate))
            return false;
        //Dùng để định nghĩa đơn vị thời gian
        //Trong trường hợp này xac định ngày hiện tại và localDate là bao nhiêu năm
        long years = ChronoUnit.YEARS.between(localDate, LocalDate.now());

        return years >= min;
    }


    /*
    * Khi constraint được khởi tạo -> Có thể lấy thông số của annotation đó
    * */
    @Override
    public void initialize(DobConstraint constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        min = constraintAnnotation.min();
    }
}
