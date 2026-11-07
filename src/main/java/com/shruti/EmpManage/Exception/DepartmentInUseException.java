package com.shruti.EmpManage.Exception;

public class DepartmentInUseException extends RuntimeException {
    public DepartmentInUseException(Long id) {
        super("Department " + id + " cannot be deleted because employees still belong to it");
    }
}
