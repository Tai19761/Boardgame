# TÀI LIỆU KIẾN TRÚC MVP — Nền Tảng Web Game Cờ Truyền Thống

**Dự án:** Ca-rô, Ô ăn quan, Cờ gánh — đồ án tốt nghiệp, quy mô solo developer.
**Mục tiêu:** Bảo vệ đồ án xuất sắc, demo chạy mượt, không sập. Khả năng chịu tải lớn
là bài toán của TƯƠNG LAI, không phải phạm vi hiện tại. Xem điều 3 trong `.antigravityrules`.

## 1. Sơ Đồ Kiến Trúc Tổng Thể (Monolith In-Memory)

```text
[ React SPA Client ] <====== STOMP / WebSocket ======> [ Spring Boot Backend (1 Node) ]
 (Vite + Zustand)                                                |
                                                                 v
                                         +-----------------------------------------------+
                                         | 1. Interface-Driven State (Caffeine Cache)    |
                                         |  - Interface: GameStateStore                  |
                                         |  - Impl: CaffeineGameStateStore (RAM)         |
                                         +-----------------------------------------------+
                                         | 2. Core Game Engine (Clean Architecture)      |
                                         |  - Java Thuần (POJO), Không Spring/JPA        |
                                         |  - Xử lý luật Cờ gánh, Ca-rô, Ô ăn quan       |
                                         +-----------------------------------------------+
                                         | 3. AI Async Worker (CPU-Bound Pool)           |
                                         |  - Minimax/Alpha-Beta                         |
                                         |  - Sizing: (Số Core CPU - 1) Threads          |
                                         +-----------------------------------------------+
                                                                 |
                                                                 v (Chỉ lưu khi HẾT VÁN)
                                         +-----------------------------------------------+
                                         | PostgreSQL 16 (Spring Data JPA)               |
                                         |  - Bảng: users, matches (lưu JSON lịch sử)    |
                                         +-----------------------------------------------+
```

## 2. Các Trụ Cột Kỹ Thuật

### Trụ cột 1: Đảo ngược Phụ thuộc cho State Management
Không gọi thẳng Caffeine trong logic game. Interface `GameStateStore` ở tầng `application`.
- **Hiện tại:** `CaffeineGameStateStore` — lưu RAM.
  - TTL kép: `expireAfterAccess` ngắn (~2 phút) kết hợp heartbeat/ping WebSocket để phát
    hiện rớt mạng và chủ động xử thua; `expireAfterWrite` dài (~2 giờ) chỉ là lưới an toàn
    dọn rác cho session mồ côi, KHÔNG dùng để xử thua.
- **Tương lai (ngoài phạm vi MVP):** `RedisGameStateStore` — chỉ cần implement Interface,
  không sửa logic game. KHÔNG triển khai trong đồ án này.

### Trụ cột 2: Định cỡ Thread Pool chuẩn cho AI (CPU-Bound)
Minimax là tác vụ CPU-bound, không phải I/O-bound. Pool size:
`poolSize = Math.max(2, Runtime.getRuntime().availableProcessors() - 1)`.
Chừa lại core cho OS và WebSocket handling. Không dùng 10-20 threads tuỳ tiện.

### Trụ cột 3: Clean Architecture cho Lõi Game
`Board`, `Piece`, `RuleEngine` chỉ là Java thuần. Unit Test (JUnit) quét mọi góc khuất luật
cờ, chạy mili-giây (không cần Spring/DB). ArchUnit test enforce ranh giới này tự động
(xem PROMPT_WORKFLOW.md, Lệnh 1.5) — viết NGAY từ đầu, không để "làm sau".

### Trụ cột 4: Lưu trữ Cuối ván
DB "nghỉ" trong suốt ván đấu. Chỉ khi kết thúc (thắng/thua/hoà/bỏ cuộc), đóng gói toàn bộ
lịch sử thành JSON, insert 1 lần vào PostgreSQL. Đánh đổi chấp nhận: crash giữa ván = mất
lịch sử của ván đó — nêu rõ đây là quyết định có chủ đích khi bảo vệ đồ án.

## 3. Cấu Trúc Thư Mục Chuẩn

```text
src/main/java/com/project/boardgame/
│
├── domain/                  <-- [LÕI] CHỈ JAVA THUẦN, KHÔNG SPRING
│   ├── models/               (Board, Piece, Move, TurnExecutionResult)
│   ├── rules/                (CaroRule, GanhRule, OAnQuanRule)
│   └── ai/                   (Minimax, HeuristicEvaluator)
│
├── application/              <-- [USE CASE] Chỉ dùng cơ bản của Spring như @Service
│   ├── ports/                (GameStateStore interface)
│   └── services/              (MatchEngineService — CHỈ điều phối, KHÔNG chứa logic luật.
│                               Nếu thấy mình viết if-luật-chơi ở đây → chuyển xuống domain/rules)
│
└── infrastructure/           <-- [HẠ TẦNG]
    ├── config/                (AiWorkerConfig, WebSocketConfig)
    ├── persistence/
    │   ├── entity/            (MatchEntity, UserEntity cho JPA)
    │   ├── repository/        (Spring Data JPA Repositories)
    │   └── cache/              (CaffeineGameStateStore implements GameStateStore)
    └── web/
        └── websocket/          (GameController — chỉ gọi Interface, không chứa logic)
```

## 4. Lộ Trình Phát Triển (không nhảy cóc)

1. **Luật chơi 3 game (Domain Logic)** — quan trọng nhất, viết Unit Test tới 100% các thế cờ.
2. **AI PvE** — Minimax/Alpha-Beta trong `domain/ai`, thread pool trong `infrastructure/config`.
3. **WebSocket PvP cơ bản** — STOMP, `CaffeineGameStateStore`, test 2 tab trình duyệt.
4. **UI/UX (React)** — Canvas/SVG, animation mượt cho rải quân Ô ăn quan.
5. **Database & Deploy** — PostgreSQL, `docker-compose.yml` để đem đi bảo vệ.

## 5. Trả lời mẫu khi hội đồng hỏi về khả năng scale

> "Phạm vi đồ án tập trung vào độ phức tạp thuật toán AI và tính chính xác luật cờ dân
> gian. Em thiết kế theo hướng Interface (Dependency Inversion) cho phần lưu trạng thái —
> hiện tại implement trên RAM (Caffeine) để tiết kiệm tài nguyên và đơn giản hoá vận hành.
> Nếu thương mại hoá, chỉ cần thêm implementation mới cho interface đó mà không sửa logic
> game." — Chuẩn bị sẵn khả năng DEMO TRỰC TIẾP: gửi 1 WebSocket message giả mạo nước đi
> không hợp lệ qua Postman/DevTools, cho hội đồng xem server từ chối đúng cách
> (Server Authoritative).
