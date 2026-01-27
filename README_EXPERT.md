# Hướng dẫn tạo Expert Account trong MySQL

## Cách 1: Dùng script SQL (Khuyến nghị)

Chạy file `create_expert_simple.sql` trong MySQL:

```bash
mysql -u root -p calmistry < create_expert_simple.sql
```

Hoặc copy nội dung và chạy trực tiếp trong MySQL Workbench/phpMyAdmin.

## Cách 2: Tạo thủ công từng bước

### Bước 1: Tạo role EXPERT
```sql
INSERT INTO roles (name) VALUES ('EXPERT');
```

### Bước 2: Tạo user mới
```sql
INSERT INTO users (username, password, email, full_name, is_active, created_at)
VALUES (
    'expert01',  -- Thay đổi username theo ý muốn
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- Password: "expert123"
    'expert01@calmistry.com',  -- Thay đổi email
    'Nguyễn Văn Chuyên Gia',  -- Thay đổi tên
    TRUE,
    NOW()
);
```

### Bước 3: Gán role EXPERT cho user
```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'expert01' AND r.name = 'EXPERT';
```

### Bước 4: Tạo ExpertProfile
```sql
INSERT INTO expert_profiles (user_id, specialty, degree, experience_years, bio, is_verified)
SELECT 
    u.id,
    'Tâm lý học lâm sàng',  -- Chuyên môn
    'Thạc sĩ Tâm lý học',  -- Bằng cấp
    5,  -- Số năm kinh nghiệm
    'Chuyên gia tư vấn tâm lý với nhiều năm kinh nghiệm.',  -- Mô tả
    FALSE  -- Chưa được verify
FROM users u
WHERE u.username = 'expert01';
```

## Thông tin đăng nhập mặc định

- **Username:** `expert01`
- **Password:** `expert123`
- **Email:** `expert01@calmistry.com`

## Tạo password hash mới (nếu muốn đổi password)

Nếu bạn muốn tạo password hash mới, có thể:

1. **Dùng Spring Boot:**
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
String hash = encoder.encode("your_password");
System.out.println(hash);
```

2. **Hoặc dùng online tool:** https://bcrypt-generator.com/

## Kiểm tra Expert đã tạo thành công

```sql
-- Kiểm tra user và role
SELECT u.id, u.username, u.email, u.full_name, r.name as role_name
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'expert01';

-- Kiểm tra ExpertProfile
SELECT ep.id, ep.user_id, u.username, ep.specialty, ep.degree, ep.experience_years, ep.is_verified
FROM expert_profiles ep
JOIN users u ON ep.user_id = u.id
WHERE u.username = 'expert01';
```

## Lưu ý

- Password đã được hash bằng BCrypt với strength = 10
- Expert mặc định sẽ có `is_verified = FALSE`, admin cần verify sau
- Đảm bảo username và email là unique (không trùng với user khác)

