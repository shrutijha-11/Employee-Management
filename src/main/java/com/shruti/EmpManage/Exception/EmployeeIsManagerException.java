package com.shruti.EmpManage.Exception;

public class EmployeeIsManagerException extends RuntimeException {
    public EmployeeIsManagerException(Long id)
    {
        super("Employee " + id + " cannot be deleted because they still manage other employees");
    }
}
