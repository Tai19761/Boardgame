# Web Game Cờ Dân Gian (Ca-rô, Cờ Gánh, Ô Ăn Quan)

Dự án Web Game chơi cờ truyền thống Việt Nam thời gian thực (Real-time Multiplayer) và Đấu với Máy (AI Bot), được thiết kế theo mô hình **Kiến trúc Lục giác (Hexagonal / Clean Architecture)** với Spring Boot và React.

---

## 🎮 Các Trò Chơi Hỗ Trợ

### 1. Cờ Ca-rô (Gomoku)
* **Kích thước bàn cờ linh hoạt:** 15x15 (Tiêu chuẩn), 20x20 (Mở rộng), 25x25 (Đại chiến).
* **Đấu người (PvP):** Tạo phòng chia sẻ mã UUID, hai người chơi thi đấu thời gian thực qua WebSocket.
* **Đấu với Máy (Bot Match):** Sử dụng thuật toán **Minimax kết hợp Cắt tỉa Alpha-Beta** với 4 cấp độ thông minh:
  * **Dễ (Easy):** Phù hợp tập sự, biết ăn thắng ngay và chặn nước thắng của đối thủ.
  * **Bình thường (Normal):** Độ sâu 3 nước, đánh chắc chắn, phản hồi tức thì.
  * **Khó (Hard):** Độ sâu 4 nước, tối ưu hóa thứ tự duyệt nước đi (Move Ordering).
  * **Cực khó (Expert):** Độ sâu 5 nước, kích hoạt bảng băm **Zobrist Hashing (Transposition Table)** tra cứu thế cờ $O(1)$, phản hồi nhanh dưới 2 giây.

### 2. Cờ Gánh (Quảng Nam)
* Đầy đủ bộ luật truyền thống:
  * **Nước đi hợp lệ:** Di chuyển 1 ô theo các đường kẻ ngang, dọc, chéo.
  * **Luật Gánh:** Tự di chuyển vào giữa 2 quân đối thủ để đổi màu cả 2 quân. Có cơ chế **bắt buộc Gánh** nếu thế cờ cho phép.
  * **Luật Chẹt / Vây:** Thuật toán loang (BFS) kiểm tra toàn bộ quân hết "khí" (không còn ô trống liền kề) để thu phục.
  * **Luật Mở (Thế tự sát):** Chủ động đi vào thế bị vây thì không bị bắt.

### 3. Cờ Ô Ăn Quan
* Bàn cờ 10 ô Dân và 2 ô Quan.
* Mô hình hóa tọa độ chu vi tuyến tính (index 0–11) chạy vòng quanh chu vi, triệt tiêu lỗi ranh giới 2 hàng.
* Rải quân 2 chiều thuận/nghịch kim đồng hồ, ăn quân liên hoàn khi gặp ô trống kế tiếp, tính điểm chuẩn xác.

---

## 🏛️ Kiến Trúc & Công Nghệ

### Backend (Java / Spring Boot)
* **Java:** Phiên bản 25 (tương thích JDK 21+).
* **Spring Boot:** 4.1.1 (Spring MVC, WebSocket STOMP qua SockJS).
* **Clean Architecture:**
  * `domain`: Java thuần 100%, không phụ thuộc Spring/Framework. Chứa toàn bộ Base Models, Rule Engines và Thuật toán AI.
  * `application`: Dịch vụ quản lý phòng (`RoomManagerService`), điều phối ván đấu (`MatchEngineService`), xử lý AI bất đồng bộ (`AiMoveService`).
  * `infrastructure`: Bộ điều khiển WebSocket (`MatchController`), cấu hình STOMP, quản lý Thread Pool riêng cho AI (`AiWorkerConfig`).
* **Tường lửa kiến trúc (ArchUnit):** `ArchitectureTest` giám sát nghiêm ngặt việc package `domain` không import bất kỳ package nào từ Spring, JPA hay Infrastructure.

### Frontend (React / TypeScript / Vite)
* **Framework:** React 19, TypeScript, Vite 8.
* **Styling:** Tailwind CSS 4 theo phong cách **Retro/Mộc Pixel** (sử dụng viền dày `border-4`, `border-2`, bảng màu gỗ/đất, tuyệt đối không dùng bo góc `rounded-*` và không bóng đổ `shadow-*`).
* **State Management:** Zustand 5.
* **Giao tiếp mạng:** `@stomp/stompjs` thời gian thực.
* **UX Sảnh chờ:** Contextual Settings (Cấu hình tại chỗ), tách riêng biệt cấu hình bàn cờ PvP và Bot Match để tránh nhầm lẫn.

---

## 📋 Yêu Cầu Môi Trường

* **Java Development Kit (JDK):** JDK 25 (hoặc JDK 21+ trở lên).
* **Node.js:** Node.js 18.x trở lên (khuyên dùng Node 20+ LTS).
* **Trình quản lý gói:** `npm` (đi kèm Node.js).
* **Trình duyệt:** Bất kỳ trình duyệt hiện đại nào (Chrome, Edge, Firefox, Safari).

---

## 🚀 Hướng Dẫn Cài Đặt & Khởi Chạy

### 1. Khởi động Backend (Spring Boot)

Mở terminal tại thư mục gốc của dự án:

```bash
# Trên Windows (PowerShell / CMD):
.\mvnw.cmd spring-boot:run

# Trên Linux / macOS:
./mvnw spring-boot:run
```

* Backend sẽ khởi động tại: `http://localhost:8080`
* WebSocket STOMP endpoint: `ws://localhost:8080/ws`

### 2. Khởi động Frontend (React / Vite)

Mở một cửa sổ terminal mới và chuyển vào thư mục `frontend`:

```bash
cd frontend

# Cài đặt thư viện:
npm install

# Khởi chạy máy chủ phát triển:
npm run dev
```

* Frontend sẽ khởi động tại: `http://localhost:5173`
* Mở trình duyệt và truy cập `http://localhost:5173` để trải nghiệm trò chơi!

---

## 🧪 Kiểm Thử Tự Động (Testing)

### Kiểm thử Backend (153 bài test Unit & Integration)
```bash
# Trên Windows:
.\mvnw.cmd clean test

# Trên Linux / macOS:
./mvnw clean test
```

### Kiểm thử & Đóng gói Frontend
```bash
cd frontend
npm run lint    # Kiểm tra cú pháp bằng Oxlint
npm run build   # Kiểm tra kiểu dữ liệu TypeScript & đóng gói Vite
```

---

## 📁 Cấu Trúc Thư Mục Dự Án

```
boardgame/
├── pom.xml                               # Quản lý dependency Maven
├── mvnw / mvnw.cmd                       # Maven Wrapper
├── README.md                             # Hướng dẫn dự án
├── src/
│   ├── main/
│   │   ├── java/com/project/boardgame/
│   │   │   ├── domain/                  # Lớp lõi: Models, Rule Engines, AI Minimax, Zobrist TT
│   │   │   ├── application/             # Lớp ứng dụng: RoomManager, MatchEngine, AiMoveService
│   │   │   └── infrastructure/          # Lớp hạ tầng: WebSocket STOMP, Controller, Thread Config
│   │   └── resources/
│   │       └── application.properties   # Cấu hình Spring Boot
│   └── test/                            # 153 Unit & Integration Tests, ArchUnit Tests
└── frontend/
    ├── package.json
    ├── vite.config.ts
    ├── index.html
    └── src/
        ├── components/                  # CaroBoard, GanhBoard, OAnQuanBoard, GameRoom
        ├── services/                    # WebSocketService (STOMP Client)
        ├── store/                       # Zustand useGameStore
        └── App.tsx                      # Sảnh chờ Contextual Settings
```

---

## 📜 Giấy Phép (License)
Dự án được phát triển phục vụ mục đích học tập và nghiên cứu học thuật.
