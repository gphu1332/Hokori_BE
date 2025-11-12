# ✅ CHECKLIST SAU KHI THÊM VARIABLES

## 🔍 BƯỚC 1: KIỂM TRA VARIABLES ĐÃ ĐƯỢC THÊM ĐÚNG CHƯA

### 1.1. Vào Variables Tab
1. Vào Railway Dashboard
2. Click vào service **"hokori-web"**
3. Click tab **"Variables"**

### 1.2. Kiểm tra danh sách Variables
Bạn cần thấy **TỐI THIỂU 11 variables** (1 tự động + 10 bạn thêm):

✅ **Variables bắt buộc:**
- [ ] `DATABASE_URL` (Railway tự động thêm khi add PostgreSQL)
- [ ] `SPRING_PROFILES_ACTIVE` = `prod`
- [ ] `JWT_SECRET` = `K8mN2pQ7rT5vW9yZ1cD4fG6hJ0kL3nM5qR7sT9uV2wX4yZ6aB8cD0eF2gH4iJ6kL8mN0pQ2rS4tU6vW8xY0zA2bC4dE6fG8hJ0`
- [ ] `FIREBASE_ENABLED` = `true`
- [ ] `FIREBASE_PROJECT_ID` = `hokori-web`
- [ ] `FIREBASE_PRIVATE_KEY_ID` = `528ba4982eff2ecd16f072e9bdb8553d04938a91`
- [ ] `FIREBASE_PRIVATE_KEY` = `-----BEGIN PRIVATE KEY-----...` (toàn bộ key)
- [ ] `FIREBASE_CLIENT_EMAIL` = `firebase-adminsdk-fbsvc@hokori-web.iam.gserviceaccount.com`
- [ ] `FIREBASE_CLIENT_ID` = `109435122069591085921`
- [ ] `FIREBASE_CLIENT_X509_CERT_URL` = `https://www.googleapis.com/robot/v1/metadata/x509/...`
- [ ] `GOOGLE_CLOUD_ENABLED` = `false`

**⚠️ LƯU Ý:** 
- Nếu bạn paste vào "Import from .env", Railway có thể đã parse và thêm các variables
- Nếu bạn paste vào form "Add Variable", có thể chỉ có 1 variable được thêm
- **Cần kiểm tra kỹ** xem tất cả 10 variables đã có chưa

---

## 📋 BƯỚC 2: CÁC BƯỚC TIẾP THEO ĐỂ DEPLOY

### ✅ Đã hoàn thành:
- [x] Tạo project trên Railway
- [x] Connect GitHub repo
- [x] Set branch thành `dev`
- [x] Add PostgreSQL database
- [x] Thêm environment variables

### 🔄 Cần làm tiếp:

#### 2.1. Kiểm tra Deploy Status
1. Click tab **"Deployments"**
2. Xem deployment mới nhất:
   - ✅ **Màu xanh** = Build thành công
   - ❌ **Màu đỏ** = Build failed (cần xem logs)

#### 2.2. Xem Build Logs (nếu đang build)
1. Click vào deployment mới nhất
2. Xem **"Build Logs"**:
   - ✅ `[INFO] BUILD SUCCESS`
   - ✅ `Downloading dependencies...`
   - ✅ `Compiling...`
   - ✅ `Packaging...`

#### 2.3. Xem Runtime Logs (sau khi build xong)
1. Click tab **"Logs"**
2. Xem logs khi app start:
   - ✅ `Started HokoriWebApplication`
   - ✅ `Firebase initialized successfully with project: hokori-web`
   - ✅ `HikariPool-1 - Start completed` (database connection)
   - ✅ `Tomcat started on port(s): 8080`

**Nếu có lỗi:**
- Database connection failed? → Check PostgreSQL service đang chạy
- Firebase not initialized? → Check `FIREBASE_*` variables đã set đúng chưa
- JWT errors? → Check `JWT_SECRET` đã set chưa

---

## 📋 BƯỚC 3: GENERATE DOMAIN (Nếu chưa có)

### 3.1. Tạo Public Domain
1. Click tab **"Settings"**
2. Scroll xuống phần **"Networking"**
3. Click **"Generate Domain"** (nếu chưa có)
4. Railway sẽ tạo domain như: `hokori-web-production.up.railway.app`
5. **Copy domain này** để test

---

## 📋 BƯỚC 4: VERIFY DEPLOYMENT

### 4.1. Test Health Endpoint
Mở browser và truy cập:
```
https://your-app.railway.app/actuator/health
```
(Thay `your-app.railway.app` bằng domain của bạn)

**Kết quả mong đợi:**
```json
{
  "status": "UP"
}
```

### 4.2. Test Swagger UI
Truy cập:
```
https://your-app.railway.app/swagger-ui.html
```

**Kết quả mong đợi:**
- Swagger UI hiển thị
- Server URL là Railway domain (không phải localhost)

### 4.3. Test Firebase Authentication
1. Gọi API `/api/auth/firebase` với Firebase ID token
2. Check logs để xem Firebase initialized
3. Check database để xem user được tạo

---

## ⚠️ NẾU VARIABLES CHƯA ĐƯỢC THÊM ĐÚNG

### Cách 1: Thêm thủ công từng variable
1. Click **"+ New Variable"**
2. Nhập Key vào khung bên trái
3. Nhập Value vào khung bên phải
4. Click "Add"
5. Lặp lại cho từng variable

### Cách 2: Import từ .env file
1. Click **"Import from .env"**
2. Upload file `railway.env` (đã tạo sẵn)
3. Railway sẽ tự động parse và thêm các variables

---

## ✅ CHECKLIST CUỐI CÙNG

Trước khi kết thúc, đảm bảo:

- [ ] Tất cả 11 variables đã có trong Variables tab
- [ ] Build thành công (check Deployments)
- [ ] App start thành công (check Logs)
- [ ] Domain đã được generate
- [ ] Health endpoint trả về `{"status":"UP"}`
- [ ] Swagger UI accessible
- [ ] Firebase initialized (check logs)

---

## 🎯 TÓM TẮT CÁC BƯỚC CÒN LẠI

1. **Kiểm tra Variables** → Đảm bảo tất cả 11 variables đã có
2. **Xem Deployments** → Check build status
3. **Xem Logs** → Check app đã start chưa
4. **Generate Domain** → Nếu chưa có
5. **Test Health Endpoint** → Verify deployment
6. **Test Swagger UI** → Verify API docs
7. **Test Firebase** → Verify authentication

**Chúc bạn deploy thành công! 🚀**

