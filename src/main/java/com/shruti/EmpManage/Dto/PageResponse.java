package com.shruti.EmpManage.Dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int currentPage,
        int pageSize,
        long totalRecords,
        int totalPages)
{
}
