package com.shruti.EmpManage.Service;

import com.shruti.EmpManage.Dto.*;
import com.shruti.EmpManage.Entity.Department;
import com.shruti.EmpManage.Entity.Employee;
import com.shruti.EmpManage.Entity.EmployeeHistory;
import com.shruti.EmpManage.Entity.EmployeeStatus;
import com.shruti.EmpManage.Exception.*;
import com.shruti.EmpManage.Mapper.EmployeeHistoryMapper;
import com.shruti.EmpManage.Mapper.EmployeeMapper;
import com.shruti.EmpManage.Repository.DepartmentRepo;
import com.shruti.EmpManage.Repository.EmployeeHistoryRepo;
import com.shruti.EmpManage.Repository.EmployeeRepo;
import com.shruti.EmpManage.Specs.EmployeeSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shruti.EmpManage.Dto.DashboardResponse;
import com.shruti.EmpManage.Dto.DepartmentSummaryResponse;
import com.shruti.EmpManage.Dto.TopDepartmentResponse;
import com.shruti.EmpManage.Dto.JoiningMonthReportResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
public class EmployeeService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("fullName", "email", "department", "salary", "joiningDate");

    private final EmployeeRepo employeeRepository;
    private final DepartmentRepo departmentRepository;
    private final EmployeeHistoryRepo historyRepository;

    public EmployeeService(EmployeeRepo employeeRepository,
                           DepartmentRepo departmentRepository,
                           EmployeeHistoryRepo historyRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.historyRepository = historyRepository;
    }

    // ---------- create ----------

    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        if (employeeRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicatePhoneException(request.phoneNumber());
        }
        Department department = findDepartmentByNameOrThrow(request.department());

        Employee manager = resolveManager(request.managerId(), null);

        Employee employee = EmployeeMapper.toEntity(request);
        employee.setDepartment(department);
        employee.setManager(manager);
        employee.setStatus(EmployeeStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        // creating an employee does not write history
        return EmployeeMapper.toResponse(employeeRepository.save(employee));
    }

    // ---------- list, filter, read ----------

    public PageResponse<EmployeeResponse> getAllEmployees(int page, int size, String sortBy,
                                                          String direction, String status) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);

        Page<Employee> result;
        if (status == null || status.isBlank()) {
            result = employeeRepository.findAll(pageable);
        } else {
            result = employeeRepository.findByStatus(parseStatus(status), pageable);
        }
        return toPageResponse(result);
    }

    public PageResponse<EmployeeResponse> filterEmployees(EmployeeFilterRequest filter, int page, int size,
                                                          String sortBy, String direction) {
        validateFilter(filter);
        Pageable pageable = buildPageable(page, size, sortBy, direction);

        // start with "no restriction", then add only the filters that were sent
        Specification<Employee> spec = Specification.unrestricted();
        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and(EmployeeSpecifications.nameContains(filter.name().trim()));
        }
        if (filter.department() != null && !filter.department().isBlank()) {
            spec = spec.and(EmployeeSpecifications.departmentIs(filter.department().trim()));
        }
        if (filter.minSalary() != null) {
            spec = spec.and(EmployeeSpecifications.salaryAtLeast(filter.minSalary()));
        }
        if (filter.maxSalary() != null) {
            spec = spec.and(EmployeeSpecifications.salaryAtMost(filter.maxSalary()));
        }
        if (filter.joinedFrom() != null) {
            spec = spec.and(EmployeeSpecifications.joinedOnOrAfter(filter.joinedFrom()));
        }
        if (filter.joinedTo() != null) {
            spec = spec.and(EmployeeSpecifications.joinedOnOrBefore(filter.joinedTo()));
        }

        return toPageResponse(employeeRepository.findAll(spec, pageable));
    }

    public EmployeeResponse getEmployeeById(Long id) {
        return EmployeeMapper.toResponse(findEmployeeOrThrow(id));
    }

    // ---------- update (version check + history) ----------

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee existing = findEmployeeOrThrow(id);

        if (!existing.getVersion().equals(request.version())) {
            throw new VersionConflictException(id, request.version(), existing.getVersion());
        }
        if (employeeRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new DuplicateEmailException(request.email());
        }
        if (employeeRepository.existsByPhoneNumberAndIdNot(request.phoneNumber(), id)) {
            throw new DuplicatePhoneException(request.phoneNumber());
        }
        Department department = findDepartmentByNameOrThrow(request.department());
        Employee manager = resolveManager(request.managerId(), id);

        // compare old and new values BEFORE changing the entity
        LocalDateTime now = LocalDateTime.now();
        List<EmployeeHistory> changes = new ArrayList<>();
        addChange(changes, id, "fullName", existing.getFullName(), request.fullName(), now);
        addChange(changes, id, "email", existing.getEmail(), request.email(), now);
        addChange(changes, id, "department", existing.getDepartment().getName(), department.getName(), now);
        addChange(changes, id, "salary", formatSalary(existing.getSalary()), formatSalary(request.salary()), now);
        addChange(changes, id, "joiningDate", existing.getJoiningDate().toString(),
                request.joiningDate().toString(), now);
        addChange(changes, id, "phoneNumber", existing.getPhoneNumber(), request.phoneNumber(), now);
        addChange(changes, id, "address", existing.getAddress(), request.address(), now);
        addChange(changes, id, "managerId", managerIdText(existing.getManager()), managerIdText(manager), now);
        EmployeeMapper.updateEntity(existing, request);
        existing.setDepartment(department);
        existing.setUpdatedAt(now);
        existing.setManager(manager);

        // saveAndFlush so the version is increased before we build the response
        Employee saved = employeeRepository.saveAndFlush(existing);
        historyRepository.saveAll(changes);
        return EmployeeMapper.toResponse(saved);
    }

    @Transactional
    public EmployeeResponse updateStatus(Long id, StatusUpdateRequest request) {
        Employee existing = findEmployeeOrThrow(id);
        EmployeeStatus newStatus = parseStatus(request.status());

        LocalDateTime now = LocalDateTime.now();
        List<EmployeeHistory> changes = new ArrayList<>();
        addChange(changes, id, "status", existing.getStatus().name(), newStatus.name(), now);

        existing.setStatus(newStatus);
        existing.setUpdatedAt(now);

        Employee saved = employeeRepository.saveAndFlush(existing);
        historyRepository.saveAll(changes);
        return EmployeeMapper.toResponse(saved);
    }

    @Transactional
    public List<EmployeeResponse> bulkUpdateStatus(BulkStatusRequest request) {
        // 1. every check that returns 400 comes first
        EmployeeStatus newStatus = parseStatus(request.status());
        List<Long> ids = request.ids();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new InvalidRequestException("ids must not contain repeated values");
        }

        // 2. every id must exist, otherwise 404 and nothing changes
        Map<Long, Employee> byId = new HashMap<>();
        for (Employee employee : employeeRepository.findAllById(ids)) {
            byId.put(employee.getId(), employee);
        }
        for (Long id : ids) {
            if (!byId.containsKey(id)) {
                throw new EmployeeNotFoundException(id);
            }
        }

        // 3. only now change anything, and only the employees whose status really differs
        LocalDateTime now = LocalDateTime.now();
        List<Employee> changed = new ArrayList<>();
        List<EmployeeHistory> changes = new ArrayList<>();
        for (Long id : ids) {
            Employee employee = byId.get(id);
            if (employee.getStatus() == newStatus) {
                continue;
            }
            addChange(changes, id, "status", employee.getStatus().name(), newStatus.name(), now);
            employee.setStatus(newStatus);
            employee.setUpdatedAt(now);
            changed.add(employee);
        }

        // flush so the new versions are set before the response is built
        employeeRepository.saveAllAndFlush(changed);
        historyRepository.saveAll(changes);

        return ids.stream()
                .map(id -> EmployeeMapper.toResponse(byId.get(id)))
                .toList();
    }

    public List<EmployeeHistoryResponse> getEmployeeHistory(Long id) {
        findEmployeeOrThrow(id);   // 404 if the employee does not exist
        return historyRepository.findByEmployeeIdOrderByChangedAtDescIdAsc(id).stream()
                .map(EmployeeHistoryMapper::toResponse)
                .toList();
    }

    public PageResponse<EmployeeHistoryResponse> getEmployeeHistoryPage(Long id, int page, int size) {
        findEmployeeOrThrow(id);
        if (page < 0) {
            throw new InvalidRequestException("page must be 0 or greater");
        }
        if (size < 1) {
            throw new InvalidRequestException("size must be at least 1");
        }
        Page<EmployeeHistory> result = historyRepository
                .findByEmployeeIdOrderByChangedAtDescIdAsc(id, PageRequest.of(page, size));
        return new PageResponse<>(
                result.getContent().stream().map(EmployeeHistoryMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        long total = employeeRepository.count();
        long active = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long inactive = employeeRepository.countByStatus(EmployeeStatus.INACTIVE);

        Double average = employeeRepository.findAverageSalary();
        double averageSalary = average == null ? 0.0 : Math.round(average * 100.0) / 100.0;

        // most employees first; on a tie, the department name in alphabetical order
        TopDepartmentResponse top = employeeRepository.getDepartmentSummary().stream()
                .sorted(Comparator.comparing(DepartmentSummaryResponse::employeeCount).reversed()
                        .thenComparing(DepartmentSummaryResponse::department))
                .findFirst()
                .map(d -> new TopDepartmentResponse(d.department(), d.employeeCount()))
                .orElse(null);

        return new DashboardResponse(total, active, inactive, averageSalary, top);
    }

    @Transactional(readOnly = true)
    public JoiningMonthReportResponse getJoiningMonthReport(Integer year, Integer month) {
        if (year == null || month == null) {
            throw new InvalidRequestException("year and month are required");
        }
        if (month < 1 || month > 12) {
            throw new InvalidRequestException("month must be between 1 and 12");
        }
        if (year < 1 || year > 9999) {
            throw new InvalidRequestException("year must be between 1 and 9999");
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        List<EmployeeResponse> employees = employeeRepository
                .findByJoiningDateBetweenOrderByJoiningDateDescIdAsc(yearMonth.atDay(1), yearMonth.atEndOfMonth())
                .stream()
                .map(EmployeeMapper::toResponse)
                .toList();

        return new JoiningMonthReportResponse(year, month, (long) employees.size(), employees);
    }

    // ---------- delete ----------

    public void deleteEmployee(Long id) {
        Employee existing = findEmployeeOrThrow(id);
        if (employeeRepository.existsByManagerId(id)) {
            throw new EmployeeIsManagerException(id);
        }
        employeeRepository.delete(existing);
    }

    // ---------- simple searches ----------

    public List<EmployeeResponse> getEmployeesByDepartment(String department) {
        return employeeRepository.findByDepartmentNameIgnoreCase(department).stream()
                .map(EmployeeMapper::toResponse)
                .toList();
    }

    public List<EmployeeResponse> searchByName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("name must not be blank");
        }
        return employeeRepository.findByFullNameContainingIgnoreCase(name.trim()).stream()
                .map(EmployeeMapper::toResponse)
                .toList();
    }

    public List<EmployeeResponse> getEmployeesBySalaryRange(BigDecimal min, BigDecimal max) {
        if (min.signum() < 0 || max.signum() < 0) {
            throw new InvalidRequestException("min and max must not be negative");
        }
        if (min.compareTo(max) > 0) {
            throw new InvalidRequestException("min must not be greater than max");
        }
        return employeeRepository.findBySalaryBetween(min, max).stream()
                .map(EmployeeMapper::toResponse)
                .toList();
    }

    // ---------- summary ----------

    public List<DepartmentSummaryResponse> getDepartmentSummary() {
        return employeeRepository.getDepartmentSummary().stream()
                .map(s -> new DepartmentSummaryResponse(
                        s.department(),
                        s.employeeCount(),
                        Math.round(s.averageSalary() * 100.0) / 100.0))
                .toList();
    }

    // ---------- CSV export ----------

    public String exportEmployeesCsv(String department, String status) {
        Specification<Employee> spec = Specification.unrestricted();
        if (department != null && !department.isBlank()) {
            spec = spec.and(EmployeeSpecifications.departmentIs(department.trim()));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and(EmployeeSpecifications.statusIs(parseStatus(status)));
        }

        List<Employee> employees = employeeRepository.findAll(spec, Sort.by("id"));

        StringBuilder csv = new StringBuilder("id,fullName,email,department,salary,joiningDate,status,createdAt,updatedAt,phoneNumber,address\n");

        for (Employee e : employees) {
            csv.append(String.join(",",
                            String.valueOf(e.getId()),
                            csvText(e.getFullName()),
                            csvText(e.getEmail()),
                            csvText(e.getDepartment().getName()),
                            e.getSalary().toPlainString(),
                            e.getJoiningDate().toString(),
                            e.getStatus().name(),
                            e.getCreatedAt().toString(),
                            e.getUpdatedAt().toString(),
                            csvText(e.getPhoneNumber()),
                            csvText(e.getAddress())))
                    .append("\n");
        }
        return csv.toString();
    }

    // ---------- helpers ----------

    private void addChange(List<EmployeeHistory> changes, Long employeeId, String field,
                           String oldValue, String newValue, LocalDateTime at) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new EmployeeHistory(employeeId, field, oldValue, newValue, at));
        }
    }

    // 50000 and 50000.00 must count as the same salary
    private String formatSalary(BigDecimal salary) {
        return salary.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void validateFilter(EmployeeFilterRequest filter) {
        if ((filter.minSalary() != null && filter.minSalary().signum() < 0)
                || (filter.maxSalary() != null && filter.maxSalary().signum() < 0)) {
            throw new InvalidRequestException("minSalary and maxSalary must not be negative");
        }
        if (filter.minSalary() != null && filter.maxSalary() != null
                && filter.minSalary().compareTo(filter.maxSalary()) > 0) {
            throw new InvalidRequestException("minSalary must not be greater than maxSalary");
        }
        if (filter.joinedFrom() != null && filter.joinedTo() != null
                && filter.joinedFrom().isAfter(filter.joinedTo())) {
            throw new InvalidRequestException("joinedFrom must not be after joinedTo");
        }
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        if (page < 0) {
            throw new InvalidRequestException("page must be 0 or greater");
        }
        if (size < 1 || size > 100) {
            throw new InvalidRequestException("size must be between 1 and 100");
        }
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new InvalidRequestException(
                    "sortBy must be one of: fullName, email, department, salary, joiningDate");
        }
        if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
            throw new InvalidRequestException("direction must be 'asc' or 'desc'");
        }
        // "department" is a link, so sort by the department's name
        String sortProperty = sortBy.equals("department") ? "department.name" : sortBy;
        return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortProperty));
    }

    private PageResponse<EmployeeResponse> toPageResponse(Page<Employee> result) {
        return new PageResponse<>(
                result.getContent().stream().map(EmployeeMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // quotes values containing commas, quotes or line breaks; and neutralises values that
    // a spreadsheet would run as a formula (starting with = + - @)
    private String csvText(String value) {
        String text = value == null ? "" : value;
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            text = "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private Department findDepartmentByNameOrThrow(String name) {
        return departmentRepository.findByNameIgnoreCase(name.trim())
                .orElseThrow(() -> new InvalidRequestException(
                        "Department '" + name.trim() + "' does not exist"));
    }

    private EmployeeStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("status must be ACTIVE or INACTIVE");
        }
        try {
            return EmployeeStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("status must be ACTIVE or INACTIVE");
        }
    }
    private Employee resolveManager(Long managerId, Long employeeId) {
        if (managerId == null) {
            return null;
        }
        if (managerId.equals(employeeId)) {
            throw new InvalidRequestException("An employee cannot be their own manager");
        }
        return employeeRepository.findById(managerId)
                .orElseThrow(() -> new EmployeeNotFoundException(managerId));
    }

    private String managerIdText(Employee manager) {
        return manager == null ? null : String.valueOf(manager.getId());
    }
}