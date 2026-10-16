# Employee Management REST API

A Spring Boot REST API for managing employees, built with Java, Spring Data JPA and MySQL.

## Tech Stack
Java, Spring Boot, Spring Data JPA, MySQL, Maven

## Setup
1. Install MySQL and create the database:
```sql
   CREATE DATABASE employee_db;
```
2. Set these environment variables before running:
    - `DB_USERNAME` - your MySQL username
    - `DB_PASSWORD` - your MySQL password
3. Run the application (`EmpManageApplication`). It starts on `http://localhost:8080`.
   The `employees` table is created automatically.

## Endpoints
| Method | URL | Description | Success |
|---|---|---|---|
| POST | /api/employees | Create employee | 201 |
| GET | /api/employees | List all employees | 200 |
| GET | /api/employees/{id} | Get employee by ID | 200 |
| PUT | /api/employees/{id} | Update employee | 200 |
| DELETE | /api/employees/{id} | Delete employee | 204 |
| GET | /api/employees/department/{department} | Search by department | 200 |

## Sample Request Body
```json
{
  "fullName": "Amit Sharma",
  "email": "amit@example.com",
  "department": "IT",
  "salary": 50000,
  "joiningDate": "2024-01-15"
}
```

## Error Responses
- 400: validation failure (invalid email, negative salary, missing fields, bad date format)
- 404: employee not found
- 409: email already exists