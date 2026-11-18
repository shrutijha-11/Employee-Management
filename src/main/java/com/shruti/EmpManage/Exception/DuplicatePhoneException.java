package com.shruti.EmpManage.Exception;

public class DuplicatePhoneException extends RuntimeException {
    public DuplicatePhoneException(String phoneNumber) {
        super("An employee with phone number '" + phoneNumber + "' already exists");
    }
}
