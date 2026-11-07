package com.shruti.EmpManage.Mapper;

import com.shruti.EmpManage.Dto.EmployeeHistoryResponse;
import com.shruti.EmpManage.Entity.EmployeeHistory;

public class EmployeeHistoryMapper {

    private EmployeeHistoryMapper() {
    }

    public static EmployeeHistoryResponse toResponse(EmployeeHistory history) {
        return new EmployeeHistoryResponse(
                history.getId(),
                history.getEmployeeId(),
                history.getFieldName(),
                history.getOldValue(),
                history.getNewValue(),
                history.getChangedAt()
        );
    }
}