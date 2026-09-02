


ProjectZest
A production-style RESTful Product Management API built with Spring Boot, featuring JWT-based authentication, refresh-token rotation, role-based authorization, validation, pagination, Swagger/OpenAPI documentation, testing, and Docker support.

Features
Product CRUD operations

Nested product-item retrieval

Pagination and sorting for products

JWT access-token authentication

Refresh tokens with rotation and revocation

Role-based authorization with USER and ADMIN

BCrypt password hashing

Centralized JSON exception handling

Jakarta Bean Validation

Swagger/OpenAPI documentation with Bearer authentication

Unit tests with JUnit 5 and Mockito

Controller testing with @WebMvcTest

Integration testing with Spring Boot Test and H2

MySQL persistence using Spring Data JPA / Hibernate

Docker and Docker Compose support

Spring Boot Actuator health endpoint

Asynchronous audit logging for product mutations

Technology Stack
Category	Technology
Language	Java 21
Framework	Spring Boot
Security	Spring Security
Authentication	JWT / JJWT
Persistence	Spring Data JPA / Hibernate
Database	MySQL 8
Validation	Jakarta Bean Validation
API Documentation	Springdoc OpenAPI / Swagger UI
Testing	JUnit 5, Mockito, Spring Boot Test, H2
Build Tool	Maven
Containerization	Docker, Docker Compose
Architecture
Client
  |
  v
Controller
  |  validation / HTTP response
  v
Service
  |  business logic / transactions
  v
Repository
  |
  v
MySQL
The REST layer uses DTOs instead of exposing JPA entities directly. This keeps the API contract separate from the persistence model.

Project Structure
projectzest/
├── .mvn/
├── .env.example
├── .gitignore
├── Dockerfile
├── docker-compose.yml
├── mvnw
├── mvnw.cmd
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/example/projectzest/
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   │   ├── request/
    │   │   │   └── response/
    │   │   ├── entity/
    │   │   ├── exception/
    │   │   ├── repository/
    │   │   ├── security/
    │   │   └── service/
    │   │       └── impl/
    │   └── resources/
    │       └── application.properties
    └── test/
        ├── java/
        └── resources/
Authentication Flow
Register
   |
   v
Login
   |
   +--> Access Token (short-lived)
   |
   +--> Refresh Token
            |
            v
      Access token expires
            |
            v
      POST /api/v1/auth/refresh
            |
            +--> old refresh token revoked
            |
            +--> new refresh token issued
            |
            +--> new access token issued
JWT Access Token
Protected endpoints require:

Authorization: Bearer <access-token>
The JWT is validated by the JWT authentication filter before the request reaches protected controllers.

Refresh Token Rotation
Refresh tokens are designed to be single-use. On a successful refresh operation, the existing refresh token is revoked and a new refresh token is issued.

This reduces the usefulness of a previously used/stolen refresh token and provides replay protection.

API Endpoints
Authentication
Method	Endpoint	Access	Description
POST	/api/v1/auth/register	Public	Register a new user
POST	/api/v1/auth/login	Public	Authenticate and receive tokens
POST	/api/v1/auth/refresh	Public	Rotate refresh token and issue new tokens
POST	/api/v1/auth/logout	Public	Revoke refresh token
Products
Method	Endpoint	Access	Description
GET	/api/v1/products	USER, ADMIN	Get paginated products
GET	/api/v1/products/{id}	USER, ADMIN	Get a product
GET	/api/v1/products/{id}/items	USER, ADMIN	Get items belonging to a product
POST	/api/v1/products	USER, ADMIN	Create a product
PUT	/api/v1/products/{id}	USER, ADMIN	Update a product
DELETE	/api/v1/products/{id}	ADMIN	Delete a product
Database Design
The main domain relationship is:

Product 1 ───────── * Item
The application also uses authentication-related tables for users and refresh tokens.

Conceptually:

product
-------
id
product_name
created_by
created_on
modified_by
modified_on

item
----
id
product_id  -> product.id
quantity

app_user
--------
id
username
email
password
role

refresh_token
-------------
id
user_id
token_hash
expiry_date
revoked
created_at
Configuration
Database and JWT configuration is controlled through environment variables.

Example .env values:

DB_HOST=localhost
DB_PORT=3306
DB_NAME=zestdb
DB_USERNAME=root
DB_PASSWORD=your-password

JWT_SECRET=your-long-random-secret
JWT_EXPIRATION=900000
REFRESH_TOKEN_EXPIRATION=604800000
An .env.example file is included as a template.

Never commit your real .env file, database password, or production JWT secret.

Running Locally
Prerequisites
JDK 21

MySQL 8

Git

Docker (optional)

1. Clone the repository
git clone <your-github-repository-url>
cd projectzest
2. Configure environment variables
Create a local .env file using .env.example as a template, or configure the variables directly in your environment.

3. Create/start MySQL
The application expects a MySQL database named:

zestdb
The configured connection can create the database automatically when the application connects.

4. Run the application
Using the Maven wrapper:

./mvnw spring-boot:run
On Windows:

mvnw.cmd spring-boot:run
The application runs on:

http://localhost:8080
Running with Docker
Build and start the application and MySQL:

docker compose up --build
Stop the containers:

docker compose down
To remove the database volume as well:

docker compose down -v
Swagger / OpenAPI
After starting the application, open:

http://localhost:8080/swagger-ui.html
OpenAPI JSON:

http://localhost:8080/v3/api-docs
Use the Authorize button in Swagger UI to provide the JWT access token for protected endpoints.

Actuator
Spring Boot Actuator is used for application monitoring.

The configured health endpoint is:

http://localhost:8080/actuator/health
A healthy application returns a response similar to:

{
  "status": "UP"
}
Only the required Actuator endpoints should be exposed in production.

Testing
Run all tests:

./mvnw clean test
Windows:

mvnw.cmd clean test
Build the application:

./mvnw clean package
Windows:

mvnw.cmd clean package
The test suite includes unit, controller, and integration tests.

Security
The project follows several security practices:

Passwords are stored using BCrypt hashing.

Access tokens are short-lived.

Refresh tokens are revocable and rotated.

Refresh tokens are stored as hashes rather than plaintext.

Protected API endpoints require JWT authentication.

Role-based authorization restricts administrative operations.

Secrets are supplied through environment variables.

.env is excluded from Git through .gitignore.

Entities are not exposed directly through REST responses.

Design Decisions
DTOs
Request and response DTOs are used at the REST boundary instead of exposing JPA entities directly.

Global Exception Handling
A centralized exception handler provides consistent API error responses for validation failures, missing resources, authentication/authorization failures, and other application errors.

Pagination
Product listing supports pagination and sorting through Spring Data's pagination mechanisms.

Example:

GET /api/v1/products?page=0&size=10&sort=productName,asc
Role-Based Authorization
The application supports:

USER
ADMIN
Normal product operations are available to authenticated users with the required role, while product deletion is restricted to administrators.

Asynchronous Audit Logging
Product mutations can be recorded through asynchronous audit logging so audit work does not unnecessarily block the main request.

Environment and Secrets
The repository intentionally contains only placeholder configuration.

Do not commit:

.env
real database passwords
production JWT secrets
private credentials
If a secret is accidentally committed, rotating the secret is necessary; simply deleting the file in a later commit does not remove it from Git history.

Author
Yash

Built as a Java/Spring Boot backend project demonstrating REST API development, authentication, authorization, persistence, testing, API documentation, and containerization
