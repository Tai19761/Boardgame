# QUY TRÌNH LÀM VIỆC VỚI ANTIGRAVITY (Chống hao token, chống scope creep)

## Bước 0 — Setup 1 lần duy nhất
1. Đặt `.antigravityrules` ở thư mục gốc dự án.
2. Đặt `docs/MASTER_PLAN.md` (đã có sẵn).
3. `git init`, commit khởi tạo rỗng: `git commit -m "chore: init project" --allow-empty`.

## Bước 1 — Prompt khởi động MỖI KHI MỞ CHAT MỚI

Không bao giờ hỏi "bạn đã hiểu chưa" — LLM luôn trả lời "đã hiểu" bất kể có đúng không.
Thay vào đó, ép nó nhắc lại thông tin cụ thể để buộc nó thực sự đọc file:

```
Hãy đọc kỹ file docs/MASTER_PLAN.md và .antigravityrules. Sau đó liệt kê chính xác:
1. Tên 3 thư viện/package tuyệt đối không được import vào domain/
2. Tên interface dùng cho state management
3. Công thức tính pool size cho AI thread
4. 2 công nghệ hạ tầng tuyệt đối không được dùng trong phạm vi MVP này
Không làm gì thêm cho tới khi tôi xác nhận câu trả lời đúng.
```

Nếu liệt kê sai hoặc thiếu (đặc biệt mục 4) → yêu cầu đọc lại trước khi giao việc.

**Quản lý context drift:** 1–2 task lớn = 1 New Chat. Dán lại prompt khởi động trên mỗi lần
mở chat mới để reset ngữ cảnh sạch, tránh AI "trôi" dần khỏi rule sau hội thoại dài.

## Bước 2 — Micro-tasking: chỉ giao 1 class/interface tại 1 thời điểm

Mỗi lệnh giao việc PHẢI kết thúc bằng khối DoD (Definition of Done) sau:

```
**Tiêu chí hoàn thành (DoD):**
- Code compile không lỗi.
- [tiêu chí riêng của task, ví dụ: không import org.springframework vào domain/]
- Toàn bộ Unit Test liên quan PASS.
- Dán nguyên văn output console của lệnh test đã chạy (không tóm tắt, không diễn giải).
- Chỉ khi đạt đủ các điều trên mới được: git commit -m "feat: <tên task>"
```

### Lệnh 1 — Khởi tạo Base Models
```
Dựa trên Master Plan, tạo các Java Record trong domain/models:
Position (int x, int y), Piece (enum Player), BoardState, MoveStep.
Viết code Java 21 thuần.
[+ khối DoD ở trên, tiêu chí riêng: không có import từ org.springframework/jakarta.persistence]
```

### Lệnh 1.5 — Dựng tường lửa kiến trúc (BẮT BUỘC, làm ngay sau Lệnh 1)
```
Tạo file ArchitectureTest.java dùng ArchUnit. Test: package domain.. tuyệt đối không được
phụ thuộc vào org.springframework.., jakarta.persistence.., hay bất kỳ package nào trong
infrastructure..
[+ khối DoD, tiêu chí riêng: test này phải PASS ngay bây giờ]
```

### Lệnh 2 — Luật cờ cơ bản + Unit Test
```
Tạo interface GameRuleEngine và class GanhRuleEngine. Viết 3 Unit Test (JUnit 5):
di chuyển hợp lệ, di chuyển sai đường, di chuyển vào ô đã có quân.
[+ khối DoD, tiêu chí riêng: (a) compile, (b) Unit Test pass, (c) ArchitectureTest pass]
```

### Lệnh 3 — Logic khó (Gánh/Chẹt) — mô tả chi tiết bằng tiếng Việt
```
Implement logic "Gánh" trong GanhRuleEngine: khi một quân di chuyển vào giữa 2 quân đối
phương nằm cùng 1 đường thẳng ngang/dọc/chéo hợp lệ, 2 quân đó biến thành quân phe mình.
Cập nhật code và bổ sung Unit Test cho trường hợp này.
[+ khối DoD]
```

## Bước 3 — Xử lý khi AI "ngáo" (course correction)

**Vi phạm kiến trúc (thêm @Entity vào domain, dùng Redis, v.v.):**
```
Lỗi vi phạm kiến trúc: [mô tả cụ thể]. Đây là vi phạm điều [số điều trong .antigravityrules].
Rollback: nếu CHƯA commit → git reset --hard HEAD. Nếu ĐÃ lỡ commit → git reset --hard HEAD~1.
Sau đó thiết kế lại đúng theo rule.
```

**Test fail liên tục / code rối:**
```
Rollback toàn bộ thay đổi vừa rồi (dùng đúng lệnh git ở trên tuỳ đã commit hay chưa).
Trở về trạng thái sạch, tôi sẽ mô tả lại yêu cầu theo từng bước nhỏ hơn.
```

**Nghi ngờ AI sửa test để "cho qua":**
```
Kiểm tra: các test case cũ có bị sửa/xoá/nới lỏng so với lần commit trước không
(git diff HEAD~1 -- '*Test.java')? Nếu có, đây là vi phạm điều 6. Rollback ngay.
```

**Giới hạn retry:** nếu 1 task fail 2 lần liên tiếp sau khi đã rollback + mô tả lại rõ hơn,
DỪNG giao cho AI — tự đọc code, chẻ nhỏ task hơn nữa, hoặc tự code đoạn đó. Đây là nơi token
bị đốt nhiều nhất nếu cứ để AI thử lại vô hạn lần trên cùng 1 bài toán khó.

## Nguyên tắc tóm tắt
- Không bao giờ giao "code cho tôi trò Cờ Gánh" — chẻ thành từng file/hàm nhỏ.
- Luôn ép AI viết Unit Test trước — lỗi thì ném log lỗi lại, không tự đọc code bắt lỗi.
- Luôn đòi bằng chứng (log console thật), không nhận lời khai "đã pass".
- Mọi commit chỉ xảy ra sau DoD đạt đủ — không có commit "tạm".
