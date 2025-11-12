# 🔧 FIX BUILD ERROR: Maven Profile Issue

## ❌ LỖI:
```
ERROR: failed to build: failed to solve: process "./mvnw -DoutputFile=target/mvn-dependency-list.log -B -DskipTests clean dependency:list install -Pproduction" did not complete successfully: exit code: 1
```

## 🔍 NGUYÊN NHÂN:
Railway đang tự động thêm `-Pproduction` (Maven profile) vào build command, nhưng project không có Maven profile này trong `pom.xml`.

**Lưu ý:** 
- Spring Boot profile (`prod`) ≠ Maven profile (`production`)
- Spring Boot profile được set qua `SPRING_PROFILES_ACTIVE=prod` (đã có)
- Maven profile không cần thiết ở đây

---

## ✅ GIẢI PHÁP: Sửa Build Command trong Railway

### Cách 1: Sửa Build Command (KHUYẾN NGHỊ)

1. **Vào Railway Dashboard**
2. **Click vào service "Hokori_BE"**
3. **Click tab "Settings"**
4. **Scroll xuống phần "Build & Deploy"** hoặc **"Build Command"**
5. **Tìm field "Build Command"** hoặc **"NIXPACKS_BUILD_COMMAND"**
6. **Sửa build command từ:**
   ```
   ./mvnw -DoutputFile=target/mvn-dependency-list.log -B -DskipTests clean dependency:list install -Pproduction
   ```
   
   **Thành:**
   ```
   ./mvnw -DoutputFile=target/mvn-dependency-list.log -B -DskipTests clean dependency:list install
   ```
   
   (Bỏ `-Pproduction` đi)

7. **Click "Save"** hoặc **"Update"**
8. Railway sẽ tự động trigger deploy lại

### Cách 2: Thêm Environment Variable (Nếu không tìm thấy Build Command)

1. **Vào Variables tab**
2. **Thêm variable mới:**
   ```
   Key: NIXPACKS_BUILD_COMMAND
   Value: ./mvnw -DoutputFile=target/mvn-dependency-list.log -B -DskipTests clean dependency:list install
   ```
3. Railway sẽ dùng command này thay vì command mặc định

### Cách 3: Tạo Maven Profile (KHÔNG KHUYẾN NGHỊ)

Nếu muốn giữ `-Pproduction`, có thể thêm vào `pom.xml`:

```xml
<profiles>
    <profile>
        <id>production</id>
        <activation>
            <activeByDefault>false</activeByDefault>
        </activation>
    </profile>
</profiles>
```

Nhưng cách này **KHÔNG CẦN THIẾT** vì Spring Boot profile đã được set qua environment variable.

---

## 🎯 KHUYẾN NGHỊ

**Dùng Cách 1 hoặc Cách 2** - Bỏ `-Pproduction` khỏi build command.

**Lý do:**
- Spring Boot profile (`prod`) đã được set qua `SPRING_PROFILES_ACTIVE=prod`
- Maven profile không cần thiết cho Spring Boot app
- Build command đơn giản hơn sẽ ít lỗi hơn

---

## ✅ SAU KHI SỬA

1. Railway sẽ tự động trigger deploy lại
2. Xem tab **"Deployments"** để theo dõi build
3. Xem tab **"Logs"** để kiểm tra app start

**Build command đúng sẽ là:**
```
./mvnw -DoutputFile=target/mvn-dependency-list.log -B -DskipTests clean dependency:list install
```

---

## 📝 LƯU Ý

- **Spring Boot Profile:** Set qua `SPRING_PROFILES_ACTIVE=prod` (đã có ✅)
- **Maven Profile:** Không cần thiết, bỏ `-Pproduction` đi
- **Build Command:** Railway tự động detect, nhưng có thể override

**Chúc bạn fix thành công! 🚀**

