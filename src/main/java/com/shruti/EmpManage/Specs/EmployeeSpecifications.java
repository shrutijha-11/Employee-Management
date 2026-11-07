package com.shruti.EmpManage.Specs;

import com.shruti.EmpManage.Entity.Employee;
import org.springframework.data.jpa.domain.Specification;
import com.shruti.EmpManage.Entity.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    // partial, case-insensitive match on the full name
    public static Specification<Employee> nameContains(String name) {
        String pattern = "%" + escapeLike(name.toLowerCase()) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("fullName")), pattern, '\\');
    }

    // case-insensitive exact match on the department name
    public static Specification<Employee> departmentIs(String departmentName) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("department").get("name")), departmentName.toLowerCase());
    }

    // salary >= min (the limit is included)
    public static Specification<Employee> salaryAtLeast(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<BigDecimal>get("salary"), min);
    }

    // salary <= max (the limit is included)
    public static Specification<Employee> salaryAtMost(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<BigDecimal>get("salary"), max);
    }

    // joiningDate >= from (the date is included)
    public static Specification<Employee> joinedOnOrAfter(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDate>get("joiningDate"), from);
    }

    // joiningDate <= to (the date is included)
    public static Specification<Employee> joinedOnOrBefore(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("joiningDate"), to);
    }

    public static Specification<Employee> statusIs(EmployeeStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    // so a typed % or _ is searched literally instead of acting as a wildcard
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}