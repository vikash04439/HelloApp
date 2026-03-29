## Spring Security - Static Password Configuration Guide

### Changes Made

#### 1. **application.properties** - Added Static Credentials
```properties
# Spring Security Configuration
spring.security.user.name=admin
spring.security.user.password=admin123
```

**Default Username:** `admin`
**Default Password:** `admin123`

#### 2. **SecurityConfig.java** - Created New Security Configuration Class

A new file has been created at: `src/main/java/com/learn/rest/HelloApp/config/SecurityConfig.java`

**Features:**
- ✅ Static username: `admin`
- ✅ Static password: `admin123` (BCrypt encoded)
- ✅ Roles: ADMIN and USER
- ✅ H2 Console access enabled (no authentication required)
- ✅ Public API endpoints configured
- ✅ CSRF protection disabled for development
- ✅ HTTP Basic Authentication enabled

---

### How It Works

**Method 1: Using application.properties** (Simple approach)
The `spring.security.user.name` and `spring.security.user.password` properties automatically create an in-memory user during startup.

**Method 2: Using SecurityConfig.java** (Advanced approach)
Provides more control over:
- Custom user roles (ADMIN, USER)
- Password encoding (BCrypt)
- Which endpoints require authentication
- H2 console security
- CSRF configuration

---

### Testing the Configuration

#### 1. **Start the Application**
```bash
mvn clean install
mvn spring-boot:run
```

No more random password will be generated in the console!

#### 2. **Access the Application**

**For REST APIs:**
```bash
# Using curl with Basic Authentication
curl -u admin:admin123 http://localhost:8080/allemployee

# Using Postman
- Select "Basic Auth" in the Authorization tab
- Username: admin
- Password: admin123
```

**For H2 Console:**
```
URL: http://localhost:8080/h2-console
Username: sa
Password: (leave blank)
```

#### 3. **Expected Response**
When accessing the API with correct credentials:
```
Status: 200 OK
Headers include: Authorization: Basic YWRtaW46YWRtaW4xMjM=
```

---

### Customizing the Credentials

**Option A: Change in application.properties**
Edit `src/main/resources/application.properties`:
```properties
spring.security.user.name=myusername
spring.security.user.password=mypassword123
```

**Option B: Change in SecurityConfig.java**
Edit the `userDetailsService()` method:
```java
UserDetails user = User.builder()
        .username("myusername")
        .password(passwordEncoder().encode("mypassword123"))
        .roles("ADMIN", "USER")
        .build();
```

---

### Allowing Public Access to Endpoints

If you want certain endpoints to NOT require authentication, modify the `securityFilterChain()` method in `SecurityConfig.java`:

```java
.authorizeHttpRequests(authz -> authz
        // Add your public endpoints here
        .requestMatchers("/allemployee", "/addemployee", "/allemployee-notactive").permitAll()
        // All others require authentication
        .anyRequest().authenticated()
)
```

---

### Important Notes

⚠️ **For Production:**
- Never hardcode passwords in code
- Use environment variables or external configuration servers
- Use strong passwords (min 12 characters, mix of uppercase, lowercase, numbers, special chars)
- Enable HTTPS/SSL
- Use password hashing (BCrypt is already configured)
- Consider using OAuth2 or JWT for stateless authentication

---

### Troubleshooting

**Issue:** Getting 401 Unauthorized
- Solution: Ensure you're sending the correct Basic Auth header with username and password

**Issue:** Still seeing random password in console
- Solution: Restart the application after code changes. The SecurityConfig will take precedence.

**Issue:** Cannot access H2 console
- Solution: Use credentials from application.properties:
  - Username: `sa`
  - Password: (leave blank)

---

