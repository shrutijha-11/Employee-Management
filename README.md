# Employee Management REST API

A backend Employee Management System built using **Java, Spring Boot, Spring Data JPA, and MySQL**. This project provides RESTful APIs for managing employee records, handling employee-related operations, and retrieving filtered employee data.

The application follows a layered architecture to maintain clean, modular, and maintainable code.

## 🚀 Features

### Employee Management
- Create new employee records.
- Retrieve all employees.
- Retrieve employee details by ID.
- Update employee information.
- Delete employee records.
- Handle duplicate employee email validation.
- Handle employee not found exceptions.

### Advanced Features
- Pagination for employee records.
- Search and filter employees.
- Filter employees based on salary.
- Employee summary and statistics.
- DTO-based request and response handling.
- Automatic creation and management of timestamps.
- Employee data export functionality.

### Department Management
- Department-related operations.
- Department service layer.
- Employee and department management.

### Additional Features
- Global exception handling.
- Request validation using Jakarta Validation.
- RESTful API design.
- Swagger UI for API documentation.
- Unit testing using JUnit 5 and Mockito.

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Programming Language |
| Spring Boot 4 | Backend Framework |
| Spring Web MVC | REST API Development |
| Spring Data JPA | Database Operations |
| Hibernate | ORM |
| MySQL | Database |
| Maven | Dependency Management |
| JUnit 5 | Unit Testing |
| Mockito | Mock Testing |
| Swagger / OpenAPI | API Documentation |
| H2 Database | Testing Database |
| IntelliJ IDEA | Development Environment |
| Postman | API Testing |
| Git & GitHub | Version Control |

## 🏗️ Project Architecture

The application follows a layered architecture.

```text
Client (Postman)
       |
       v
Controller Layer
       |
       v
Service Layer
       |
       v
Repository Layer
       |
       v
MySQL Database
```

### Project Structure

```text
Employee-Management/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/shruti/EmpManage/
│   │   │       ├── Controller/
│   │   │       ├── Entity/
│   │   │       ├── Exception/
│   │   │       ├── Repository/
│   │   │       ├── Service/
│   │   │       └── DTO/
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│       └── java/
│           └── com/shruti/EmpManage/
│
├── pom.xml
└── README.md
```

## ⚙️ Installation and Setup

### Prerequisites

Make sure you have installed:

- Java JDK 21
- Apache Maven
- MySQL Server
- IntelliJ IDEA or any Java IDE
- Postman (for API testing)

### 1. Clone the Repository

```bash
git clone https://github.com/shrutijha-11/Employee-Management.git
```

### 2. Open the Project

Open the cloned project in IntelliJ IDEA.

### 3. Create MySQL Database

Open MySQL Workbench and execute:

```sql
CREATE DATABASE employee_management;
```

### 4. Configure Database

Open:

`src/main/resources/application.properties`

Configure your database connection:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/employee_management
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

Use your own MySQL credentials. Do not upload database passwords to GitHub.

### 5. Run the Application

Run the main Spring Boot application from IntelliJ IDEA.

Alternatively, use Maven:

```bash
mvn spring-boot:run
```

The application will start at:

```text
http://localhost:8080
```

## 📖 API Documentation

This project uses Swagger UI for interactive API documentation.

After starting the application, access:

```text
http://localhost:8080/swagger-ui/index.html
```

Swagger allows you to explore available endpoints and test API requests directly from your browser.

## 🧪 Testing

Unit testing is implemented using:

- JUnit 5
- Mockito
- Spring Test

Run the test suite using:

```bash
mvn clean test
```

The tests cover service-layer functionality and help verify application behaviour.

## 🎯 Key Learning Outcomes

Through this project, I gained practical experience in:

- Building RESTful APIs using Spring Boot.
- Implementing layered architecture.
- Working with MySQL and Spring Data JPA.
- Writing custom database queries.
- Implementing pagination and filtering.
- Using DTOs for data transfer.
- Handling exceptions and validations.
- Writing unit tests using JUnit and Mockito.
- Documenting APIs using Swagger.

## 👩‍💻 Author

**Shruti Jha**

B.Tech – Information Technology

GitHub: [shrutijha-11](https://github.com/shrutijha-11)

---

⭐ If you find this project useful, feel free to explore the repository!