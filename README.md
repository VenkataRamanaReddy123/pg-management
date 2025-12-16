# pg-management

This is a Spring Boot + Thymeleaf project configured to use MySQL.

## Quick setup

1. Make sure MySQL is running and you created a database:
```sql
CREATE DATABASE pgdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Update MySQL credentials if needed in `src/main/resources/application.properties`.
Current settings:
- username: root
- password: pass
- port: 8181

3. Build and run:
```
mvn clean package
mvn spring-boot:run
```

4. Open:
- App: http://localhost:8181
