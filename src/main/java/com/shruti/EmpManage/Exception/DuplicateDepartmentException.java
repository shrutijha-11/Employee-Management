package com.shruti.EmpManage.Exception;

public class DuplicateDepartmentException extends RuntimeException {
    public DuplicateDepartmentException(String name) {
        super("A department named '" + name + "' already exists");
    }
}
