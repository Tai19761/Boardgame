# NHẬT KÝ LỆNH ĐÃ GIAO CHO ANTIGRAVITY

File này là nguồn sự thật độc lập, không phụ thuộc vào bất kỳ tài khoản Claude hay phiên
trò chuyện nào. Ghi lại đầy đủ: lệnh đã giao, quyết định thiết kế quan trọng đi kèm, và
trạng thái hoàn thành. Đọc file này trước khi tiếp tục dự án ở bất kỳ đâu.

**Dự án:** Web game cờ truyền thống (Ca-rô, Cờ gánh, Ô ăn quan) — đồ án tốt nghiệp, solo dev.
**Trạng thái tổng quan:** Bước 1 (Luật chơi 3 game) đã hoàn thành đầy đủ. Đang làm Bước 2
(tầng application / MatchEngineService) trước khi sang AI và WebSocket.

---

## Lệnh 1 — Khởi tạo Base Models
**Trạng thái:** Hoàn thành, đã commit (`f410702`).
Tạo các Java Record trong `domain/models`: `Position`, `Piece` (enum `Player`), `BoardState`,
`MoveStep`. Java thuần, không import Spring/JPA.

## Lệnh 1.5 — Dựng tường lửa kiến trúc (ArchUnit)
**Trạng thái:** Hoàn thành, đã commit (`8620a48`).
Tạo `ArchitectureTest.java`: package `domain..` không được phụ thuộc
`org.springframework..`, `jakarta.persistence..`, `java.sql..`, `..infrastructure..`.
**Lưu ý quan trọng:** dùng `.allowEmptyShould(true)` vì rule là dạng "phủ định" (`noClasses`)
— nếu không có class nào vi phạm, ArchUnit coi rule là "rỗng" và tự fail nếu thiếu cờ này.
Đã tự kiểm chứng bằng cách tạo file vi phạm giả (`@Component`), xác nhận test FAIL đúng lúc
có vi phạm, PASS lại sau khi xóa — tường lửa hoạt động thật, không phải pass rỗng.

## Lệnh 2 — Luật cờ cơ bản (Cờ gánh)
**Trạng thái:** Hoàn thành, đã commit (`04beaee`).
Tạo interface `GameRuleEngine`, class `GanhRuleEngine`. 3 Unit Test: di chuyển hợp lệ, sai
đường, vào ô đã có quân.
```java
public interface GameRuleEngine {
    boolean isValidMove(BoardState board, MoveStep move, Player player);
    BoardState executeMove(BoardState board, MoveStep move, Player player);
}
```

## Lệnh 3 — Logic "Gánh"
**Trạng thái:** Hoàn thành, đã commit (`b8b8778`).
Khi 1 quân di chuyển vào ô trống nằm giữa 2 quân đối phương trên 1 đường thẳng hợp lệ
(ngang/dọc/chéo), 2 quân đối phương đó đổi sang phe mình.
**Quyết định thiết kế của người dùng:** thêm luật **"bắt buộc Gánh"** — nếu người chơi có ít
nhất 1 nước đi dẫn tới Gánh, mọi nước đi khác đều bị coi là không hợp lệ (tương tự luật "bắt
buộc ăn" trong cờ đam). Đây là chủ đích, không phải Agent tự thêm.

## Lệnh 4 — Logic "Chẹt" (Bao vây) và nước "Mở"
**Trạng thái:** Hoàn thành, đã commit (`204891d`, `0138a15`).
Nếu 1 quân/nhóm quân bị đối phương vây kín (không còn ô trống liền kề — hết "khí", dùng
BFS/flood-fill), toàn bộ nhóm đổi sang phe đối phương.
**Luật "Mở" (thế tự sát):** nếu người chơi chủ động đi vào thế tự vây mình, KHÔNG bị bắt —
đây là ngoại lệ bắt buộc để phân biệt "bị bắt" và "tự đưa mình vào thế vây".

## Lệnh 5 — Logic Cờ Caro
**Trạng thái:** Hoàn thành, đã commit qua 2 lần (`3b64057` sơ khởi, `a9df96b` bản vá đầy đủ).
Tạo `CaroRuleEngine`: đặt quân vào ô trống, thắng khi có 5 quân liên tiếp
(ngang/dọc/chéo, chấp nhận overline).

**3 vấn đề phát hiện khi review, đã sửa trong `a9df96b`:**
1. Thiếu kiểm tra biên bàn cờ (`isWithinBounds`, bàn 15x15, tọa độ 0–14) — vi phạm nguyên
   tắc Server Authoritative nếu bỏ qua.
2. `checkWinCondition` cũ (nhận `Player` làm tham số, dễ gọi sai) → thống nhất thành
   `Optional<Player> checkWinner(BoardState board, Position lastMove)` cho cả 3 game, tự
   suy ra `player` từ quân cờ tại `lastMove`.
3. **ArchUnit 1.3.0 không đọc được bytecode Java 25** (`Unsupported class file major version
   69`) — khiến `ArchitectureTest` PASS "rỗng" vì không quét được bất kỳ class nào trong
   `domain` (100% class bị lỗi import). Đã nâng cấp lên **ArchUnit 1.4.2**, tự kiểm chứng lại
   bằng bài test vi phạm giả sau khi nâng cấp.

## (Chưa đánh số) Turn Validation
**Trạng thái:** Hoàn thành, đã commit (`60d5267`).
Thêm `currentPlayer` vào `BoardState` (record, giữ nguyên vị trí tham số cuối). Cả
`CaroRuleEngine` và `GanhRuleEngine`: bước đầu tiên trong `isValidMove` là
`if (player != board.currentPlayer()) return false;`. `executeMove` phải đảo `currentPlayer`
sang người chơi kia khi trả `BoardState` mới.

## (Chưa đánh số) Luật thắng Cờ gánh
**Trạng thái:** Hoàn thành, đã commit (`0648f92` — commit riêng, tách ra sau khi phát hiện
lỗi quy trình, xem mục "Bài học" bên dưới).
`GanhRuleEngine.checkWinner`: đối phương hết sạch quân (đếm = 0) → người còn lại thắng.
**Quyết định:** KHÔNG áp dụng luật giới hạn thời gian ("hết giờ thì so quân nhiều hơn thắng")
— dự án không cần cơ chế đếm giờ, giữ đúng phạm vi MVP.
Trường hợp biên (bàn trống hoàn toàn 2 bên) → `Optional.empty()` (không dùng Exception, vì
trạng thái này không thể xảy ra qua luồng chơi hợp lệ do quân bị bắt chỉ đổi màu, không bao
giờ bị xóa khỏi bàn).

## Lệnh 6 — Logic Cờ Ô ăn quan
**Trạng thái:** Hoàn thành, đã commit (`git commit -m "feat: implement o an quan logic and
tests"` — xem log để lấy hash chính xác).

**Các quyết định thiết kế quan trọng (bắt buộc đọc trước khi sửa `OAnQuanRuleEngine`):**
- **Biểu diễn nước đi:** tái sử dụng `MoveStep(from, to)` có sẵn — không sửa model dùng
  chung. `to` chỉ đóng vai trò "vector chỉ hướng" (ô liền kề `from` theo hướng rải), không
  phải đích đến cuối cùng.
- **Tọa độ bàn cờ:** dùng **index tuyến tính 0–11** chạy vòng quanh chu vi (không dùng lưới
  2D x,y suy hướng — đã chứng minh lưới 2D gây sai ở ranh giới 2 hàng và tại ô Quan). Mảng
  canonical `POSITIONS[]` map index → `Position` thật.
  ```
  Index: 0      1     2     3     4     5     6      7     8     9    10    11
  Ô:   Quan-T Dân1  Dân2  Dân3  Dân4  Dân5  Quan-P Dân5  Dân4  Dân3  Dân2  Dân1
  ```
- **Luật rải qua ô Quan — Biến thể A (đã chọn):** rải qua ô Quan bình thường như ô thường,
  chỉ ăn Quan khi quân cuối dừng ngay trước nó. KHÔNG dừng giữa chừng khi đi ngang qua Quan.
- **Ăn Quan trong chuỗi "ăn liên hoàn":** ĐƯỢC PHÉP — nếu ô Quan rơi đúng vị trí trong chuỗi
  trống-có xen kẽ, vẫn ăn được dù là mắt xích giữa chuỗi, không chỉ mắt xích đầu tiên.
- **Luật ăn quân dân & hết lượt:** quân cuối rơi vào ô có quân → bốc tiếp, rải tiếp (chưa
  hết lượt). Quân cuối rơi vào ô trống → xét ăn liên hoàn (ô kế tiếp có quân thì ăn, chuỗi
  tiếp diễn qua các cặp trống-có xen kẽ), sau đó LUÔN hết lượt bất kể có ăn được gì không.
- **`BoardState` mở rộng** (ảnh hưởng TOÀN BỘ 3 game, không chỉ Ô ăn quan):
  ```java
  public record BoardState(
      Map<Position, Piece> pieces,
      Map<Position, Integer> pieceCounts,
      int player1Score,
      int player2Score,
      Player currentPlayer
  ) {
      // Constructor cũ giữ nguyên qua delegation — Caro/Gánh KHÔNG cần sửa gì
      public BoardState(Map<Position, Piece> pieces, Player currentPlayer) {
          this(pieces, Map.of(), 0, 0, currentPlayer);
      }
  }
  ```
- Đã xóa 1 đoạn "dead code" (fallback tra cứu 1D trong `getPieceCount`) không còn cần thiết
  sau khi chuẩn hóa dùng `POSITIONS[]` làm key duy nhất.

## (Chưa đánh số) isGameOver — phân biệt DRAW và ONGOING
**Trạng thái:** Hoàn thành, đã commit.
**Vấn đề phát hiện:** `checkWinner` trả `Optional.empty()` cho CẢ 2 trường hợp "Hòa" và
"Chưa kết thúc" — tầng điều phối không phân biệt được. **Giải pháp đã chọn (Phương án 1,
không phá vỡ chữ ký cũ):** thêm `boolean isGameOver(BoardState board)` vào interface.
**Quy ước bắt buộc cho tầng gọi:** LUÔN gọi `isGameOver()` trước; chỉ khi `true` mới được
diễn giải `checkWinner() == Optional.empty()` là "Hòa" — nếu `isGameOver() == false`, kết
quả `empty()` của `checkWinner` KHÔNG có ý nghĩa gì, ván vẫn đang tiếp diễn.
Có `GameRuleEngineIntegrationTest.java` mô phỏng đúng luồng gọi này cho cả 3 game, đặc biệt
kiểm chứng nếu ai gọi sai thứ tự (bỏ qua `isGameOver`) sẽ bị bài test bắt lỗi sớm.

## Lệnh 7 — Hoàn thiện trạng thái khởi tạo (initializeBoard)
**Trạng thái:** Hoàn thành, đã commit.
Thêm `BoardState initializeBoard()` vào interface.
- Caro: bàn rỗng.
- Cờ gánh: 16 quân theo sơ đồ đối xứng cụ thể qua tâm (2,2):
  - Player 1: `(0,0)(1,0)(2,0)(3,0)(4,0)(0,1)(4,1)(0,2)`
  - Player 2: `(0,4)(1,4)(2,4)(3,4)(4,4)(0,3)(4,3)(4,2)`
- Ô ăn quan: **ủy quyền gọi lại `createInitialBoard()`** đã có sẵn từ Lệnh 6 — không viết
  logic mới, tránh trùng lặp.

## Lệnh 8 — MatchEngineService (tầng application) & ArchUnit Guard (Lệnh 8.5)
**Trạng thái:** Hoàn thành, đã commit (`6262b5f`).
Tạo `MatchEngineService` trong `application/services`: điều phối vòng đời ván đấu, gọi qua
interface `GameRuleEngine` (không chứa logic luật). Bổ sung rule ArchUnit kiểm soát tầng
`application` không phụ thuộc trực tiếp vào các class implementation cụ thể của luật cờ.

**Quyết định thiết kế quan trọng:**
- KHÔNG lưu `currentPlayer` riêng trong service — luôn lấy từ `board.currentPlayer()`.
- Tạo `enum MatchStatus` chính thức (`ONGOING`, `WIN_PLAYER_1`, `WIN_PLAYER_2`, `DRAW`) trong
  `application/services`, thay cho enum cục bộ tạm thời từng dùng trong
  `GameRuleEngineIntegrationTest`.
- `InvalidMoveException` (RuntimeException) khi `isValidMove` trả `false` hoặc đi sai lượt.
- **CẢNH BÁO BẢO MẬT TẠM THỜI (bắt buộc nhớ ở Bước 3 — WebSocket):** hiện tại
  `executeMove(MoveStep move, Player player)` TIN TƯỞNG tham số `player` do phía gọi truyền
  vào. Đây là lỗ hổng Server Authoritative nếu không sửa khi có WebSocket thật — client có
  thể tự khai sai danh tính. Code có comment TODO nhắc việc này. **Khi làm Bước 3, PHẢI thay
  bằng danh tính lấy từ session WebSocket đã xác thực, không nhận trực tiếp từ client.**
- Không dùng Mockito — test tích hợp trực tiếp với `CaroRuleEngine` cho đủ, tránh thêm thư
  viện không cần thiết (đúng tinh thần KHÔNG OVER-ENGINEER).
- **ArchUnit 1.4.2:** vị trí static `are` nằm ở `com.tngtech.archunit.lang.conditions.ArchPredicates.are`,
  kết hợp `are(not(INTERFACES))` để cấm phụ thuộc class cụ thể nhưng cho phép phụ thuộc interface `GameRuleEngine`.

## (Chưa đánh số) Bất biến hoá BoardState Collections
**Trạng thái:** Hoàn thành, đã commit (`9739e9b`).
Bổ sung compact constructor trong record `BoardState` (`pieces = Map.copyOf(pieces); pieceCounts = Map.copyOf(pieceCounts);`) để đảm bảo các collection luôn bất biến bất kể caller truyền map loại gì.
**Quyết định thiết kế quan trọng:** Toàn bộ code hiện tại không truyền null cho pieces/pieceCounts nên không phát sinh rủi ro NullPointerException với `Map.copyOf()`.

## Lệnh 9 — Quản lý phòng chơi (Room/Matchmaking)
**Trạng thái:** Hoàn thành, đã commit (`49a35bc`).
Tạo các model `GameType`, `RoomStatus`, `Room` trong `application/models`, exception `RoomJoinException` và `RoomManagerService` trong `application/services` quản lý phòng bằng `ConcurrentHashMap`. Sử dụng `computeIfPresent` đảm bảo tính nguyên tử khi nhiều luồng cùng tham gia phòng (thread-safe).
**Quyết định thiết kế quan trọng:**
- Để tuân thủ rule ArchUnit Lệnh 8.5 (không để `application` phụ thuộc vào các implementation cụ thể của `domain.rules`), tạo interface port `GameRuleEngineFactory` trong `application/ports` và cài đặt `DefaultGameRuleEngineFactory` trong `infrastructure/config`.
- `RoomManagerService` chỉ nhận `GameRuleEngineFactory` qua constructor, bảo vệ tính toàn vẹn của Dependency Inversion.
- Ván đấu chỉ chính thức khởi động (`matchEngine.startMatch()`) khi guest tham gia phòng (`joinRoom`), không start khi mới tạo phòng.

---

## Bài học quy trình quan trọng (đọc kỹ trước khi tin bất kỳ báo cáo "đã commit" nào)

**Sự cố:** Ở bước luật thắng Cờ gánh, Agent báo "đã commit" và người dùng đã xác nhận, nhưng
`git log` sau đó cho thấy **lệnh commit thực ra chưa từng chạy** — code vẫn nằm trong working
directory, bị gộp nhầm vào commit của lệnh kế tiếp (turn validation) dưới dạng "chưa staged".
Không mất code, nhưng là khoảng trống giữa "báo cáo đã làm" và "thực sự đã làm".

**Quy tắc đã bổ sung vĩnh viễn từ đó:** sau khi được xác nhận "đồng ý commit", Agent PHẢI:
1. Thực sự chạy `git commit` ngay lập tức.
2. Ngay sau đó chạy `git log --oneline -3` và dán kết quả để tự xác minh commit đã thực sự
   xuất hiện trong lịch sử — không chỉ báo cáo bằng lời.

---

## HƯỚNG DẪN CHO ANTIGRAVITY: TỰ ĐỘNG GHI NHẬT KÝ TỪ NAY VỀ SAU

Từ lệnh tiếp theo trở đi, sau khi một lệnh đạt đủ Definition of Done VÀ đã commit thành công
(đã xác minh bằng `git log`), PHẢI tự động thêm 1 mục mới vào cuối file `docs/NHAT_KY_LENH.md`
theo đúng khuôn mẫu sau, rồi báo cho người dùng biết đã cập nhật:

```markdown
## Lệnh <số hoặc mô tả ngắn> — <tên việc>
**Trạng thái:** Hoàn thành, đã commit (`<hash ngắn từ git log>`).
<Mô tả ngắn gọn 1-3 câu việc đã làm.>
**Quyết định thiết kế quan trọng (nếu có):** <chỉ ghi nếu có quyết định KHÔNG hiển nhiên từ
code, ví dụ: chọn biến thể luật nào, lý do thiết kế khác với đề xuất ban đầu, đánh đổi có
chủ đích. KHÔNG cần ghi lại những gì đã tự rõ trong code.>
```

Quy tắc ghi:
- Chỉ ghi SAU KHI đã commit thật và có bằng chứng `git log` — không ghi trước, không ghi
  "dự kiến sẽ làm".
- Không tự ý sửa hay xóa các mục nhật ký cũ đã có trong file — chỉ được PHỤ THÊM (append)
  mục mới vào cuối.
- Nếu 1 lệnh phát sinh sửa lỗi/thay đổi bổ sung trong lúc review (ví dụ nâng cấp thư viện,
  vá lỗ hổng phát hiện giữa chừng), ghi CHUNG vào cùng 1 mục với lệnh gốc đó (như cách mục
  "Lệnh 5" ở trên đã gộp cả phần vá lỗi ArchUnit/bounds check vào cùng 1 chỗ), không tách
  thành mục riêng gây rối nhật ký.

---

## Lệnh 10 — Thiết lập WebSocket, Server Authoritative & Xử lý Rủi ro Vận hành
**Trạng thái:** Hoàn thành, đã commit (`4910c70`).
Triển khai Spring WebSocket STOMP trong `infrastructure/web/websocket` bao gồm `UserHandshakeHandler` gán anonymous Principal UUID, cấu hình broker (`/topic`, `/queue`, `/app`, `/user`), `MatchController` điều phối tạo/join phòng và đi nước cờ theo nguyên tắc Server Authoritative, và `WebSocketEventListener` dọn dẹp RAM thông qua `RoomManagerService` khi rớt mạng. Toàn bộ 80 test case (trong đó có 19 test case mới bao quát WebSocket, dọn dẹp bộ nhớ và xử lý đa luồng) đều pass xanh.
**Quyết định thiết kế quan trọng:**
- **Chống race condition tạo/join phòng (Atomic Guard):** Thay vì check-then-act (`containsKey`), dùng `playerToRoomMap.putIfAbsent(playerId, "PENDING")` kết hợp khối `try-catch` rollback để đảm bảo nguyên tử, chống spam tạo nhiều phòng đồng thời trên cùng một người chơi.
- **Dọn Zombie Room triệt để:** Khi ván cờ kết thúc bình thường (`matchStatus != ONGOING`), `MatchController` tự động gọi `room.finishRoom()`, `removePlayerMappings` và `removeRoom(roomId)` để xóa sạch phòng và mapping khỏi RAM, ngăn chặn memory leak.
- **Tách lock khỏi I/O mạng (Chống Lock Contention):** Khối `synchronized(room)` chỉ bao quanh việc thực thi nước đi (`executeMove`) và chụp snapshot kết quả; lệnh gửi tin qua mạng (`convertAndSend`) nằm hoàn toàn ngoài lock.
- **Phạm vi Exception rộng:** Xử lý tập trung qua `@MessageExceptionHandler` gửi về `@SendToUser("/queue/errors")` cho toàn bộ các ngoại lệ: `InvalidMoveException`, `UnauthorizedSpectatorException`, `RoomNotFoundException`, `RoomJoinException`, `IllegalStateException` và `Exception` tổng quát.

---

## Lệnh 11 — Khởi tạo Khung gầm Frontend & Kết nối STOMP
**Trạng thái:** Hoàn thành, đã commit (`932149c`).
Khởi tạo React SPA bằng Vite (TypeScript) tại thư mục `frontend/`, thiết lập Zustand store `useGameStore.ts` lưu toàn bộ `GameStateResponse`, và triển khai Singleton `WebSocketService.ts` kết nối trực tiếp WebSocket thuần (`@stomp/stompjs`, tuyệt đối không dùng SockJS) trên cổng 3000. Xây dựng giao diện Wireframe mộc mạc phục vụ debug luồng dữ liệu tạo phòng, vào phòng và nhận broadcast trạng thái bàn cờ.
**Quyết định thiết kế quan trọng:**
- **Không dùng SockJS:** Do backend là WebSocket thuần RFC 6455, frontend dùng `brokerURL: 'ws://localhost:8080/ws-boardgame'` của `@stomp/stompjs` và loại bỏ hoàn toàn `sockjs-client`.
- **An toàn React 19 StrictMode:** Khử lỗi "WebSocket closed before connection established" bằng cách dùng cờ và debounce hủy kết nối nhẹ khi unmount, cho phép chu kỳ mount -> unmount -> mount của StrictMode giữ nguyên kết nối mà không bị ngắt quãng vô cớ.
- **Khôi phục Subscription khi Reconnect:** Trong `onConnect`, tự động kiểm tra `roomId` hiện có trong Zustand store; nếu tồn tại thì tức thời khôi phục lại subscription `/topic/match/{roomId}` để không làm gián đoạn luồng cập nhật bàn cờ khi mạng chập chờn.
- **Gửi nước đi có Type an toàn:** Định nghĩa interface `Position(x, y)` và `MoveRequest(from, to)` chặt chẽ trong `useGameStore.ts` và hàm `sendMove(moveRequest)` trong `WebSocketService.ts` để gửi nước đi đến `/app/match/{roomId}/move`.

---

## Lệnh 12 & 12.5 — Giao diện Bàn cờ Dân gian (Anti-SaaS) & Hoàn thiện UX/UI & State Consistency
**Trạng thái:** Hoàn thành, đã commit (`a297889`).
Xây dựng giao diện bàn cờ Caro 15x15 mộc mạc (`CaroBoard.tsx`), phòng đấu cờ (`GameRoom.tsx`) và nâng cấp sảnh chờ (`App.tsx`) đồng bộ theo triết lý Anti-SaaS truyền thống (nền gỗ/đất `bg-[#e7dfd5]`, viền đá `border-4 border-stone-800`, không bo tròn rounded-lg/xl, không bóng shadow, không dải màu gradient). Vá triệt để các lỗ hổng UX như kẹt giao diện, banner tuyên bố thắng/thua cảm xúc, và bảo vệ state chống cập nhật lạc quan.
**Quyết định thiết kế quan trọng:**
- **Giải mã Key Tọa độ Map Spring Boot:** Khắc phục sự khác biệt khi Spring Boot tuần tự hóa `Map<Position, Piece>` thành key dạng chuỗi `"Position[x=..., y=...]"` thay vì JSON lồng nhau; hàm tra cứu quân cờ trong `CaroBoard.tsx` chuẩn hóa định dạng key để đọc chính xác từng ô cờ.
- **Bảo vệ State `myPlayer` (Chống Optimistic Update):** `createRoom` tuyệt đối không gán trước `myPlayer`; chỉ gán `PLAYER_1` bên trong callback `/user/queue/room/created` khi server xác nhận thành công. Đối với Guest, `PLAYER_2` chỉ được gán ở cuối hàm `joinRoom` sau khi các bước validate và publish thành công.
- **Cơ chế Thoát & Reset STOMP Sạch sẽ:** Trang bị nút "⬅ Quay lại Sảnh" (màn hình chờ) và "⬅ Rời phòng" (bảng trạng thái) với hành vi `window.location.reload()`, giải phóng triệt để subscription STOMP và reset Zustand store về trạng thái nguyên bản.
- **Tuyên bố Thắng/Thua Trực quan:** So sánh trực tiếp `gameState.winner` với `myPlayer` để hiển thị banner to rõ ràng: "🏆 BẠN ĐÃ CHIẾN THẮNG!" (đỏ đậm), "💀 BẠN ĐÃ THUA!" (xám đậm) hoặc "🤝 HÒA CỜ!".

---

## GHI CHÚ — Hạn chế đã biết: myPlayer phía Guest chưa xác thực qua server
**Trạng thái:** Chấp nhận được cho MVP, CHƯA sửa. Cân nhắc sửa nếu mở rộng giao thức xác nhận join có phản hồi riêng cho Guest (ví dụ thêm `@SendToUser("/queue/room/joined")` tương tự Host).
- **Vấn đề:** Trong `WebSocketService.joinRoom`, lệnh `setMyPlayer('PLAYER_2')` hiện được gọi ngay sau `this.client.publish('/app/room/join', ...)`. Nghĩa là state được gán trước khi biết chắc server có xử lý join thành công hay không (khác với Host đã bảo vệ qua `/user/queue/room/created`).
- **Lý do chưa sửa ngay:** Kiến trúc WebSocket hiện tại (Lệnh 10) không có kênh `@SendToUser` riêng để xác nhận join thành công cho Guest mà chỉ broadcast trạng thái trận đấu qua `/topic/match/{roomId}`. Do đó, không có điểm tựa để chờ xác nhận mà không phải tái thiết kế giao thức backend.
- **Mức độ rủi ro & Biện pháp giảm thiểu:** Nếu `joinRoom` thất bại (nhập sai mã phòng hoặc phòng đầy), dù `myPlayer` bị gán là `'PLAYER_2'` nhưng vô hại: `gameState` vẫn là `null` (do không nhận được broadcast), giao diện dừng an toàn ở màn hình "Đang tải dữ liệu" kèm thông báo lỗi và đã có nút "⬅ Quay lại Sảnh" để reset sạch sẽ; nếu người dùng nhập mã khác và join thành công sau đó, `myPlayer` sẽ được ghi đè chính xác.

---

## Lệnh 14 — Tách Topic Events, Bọc ErrorBoundary & Khôi Phục Dọn Dẹp Phòng Khi Rời Ván
**Trạng thái:** Hoàn thành, đã commit (`173e075`).
Khắc phục xung đột kiểu dữ liệu trên WebSocket bằng cách tách riêng topic thông báo trận đấu, bọc ErrorBoundary chống văng ứng dụng React, chuẩn hóa quy trình tham gia phòng phía Guest và khôi phục logic dọn dẹp tài nguyên phòng khi người chơi thoát giữa chừng.
**Quyết định thiết kế quan trọng:**
- **Tách riêng Topic `/topic/match/{roomId}/events`:** Trước đây các event thông báo chuỗi (như người chơi thoát phòng) gửi chung vào `/topic/match/{roomId}` - nơi client mong đợi nhận `GameStateResponse` (JSON Object) - dẫn đến lỗi parse JSON và crash UI. Nay tách hẳn ra topic `/events` độc lập với payload `{ type: string, message: string }`, đồng thời cập nhật `WebSocketEventListener` để gửi đúng topic.
- **Bọc ErrorBoundary cấp toàn cục:** Bổ sung `ErrorBoundary.tsx` quanh component gốc `<App />` tại `frontend/src/main.tsx` theo phong cách Anti-SaaS mộc mạc, có nút "Thử tải lại" (`window.location.reload()`), đảm bảo khi có lỗi render phát sinh không bao giờ bị màn hình trắng (WSOD).
- **Cơ chế `pendingRoomId` cho `joinRoom`:** Loại bỏ việc gán lạc quan `roomId` và `myPlayer` trước khi server phản hồi. Giờ đây `joinRoom` lưu tạm `pendingRoomId`, chỉ khi server broadcast `GameStateResponse` hợp lệ đầu tiên thì client mới chính thức commit `roomId`, `myPlayer = 'PLAYER_2'` và xóa `pendingRoomId`.
- **Cơ chế `handleExit` (Chính sách A - Rời ván = Bỏ cuộc):** Khi người chơi nhấn nút "Rời phòng" / "Quay lại Sảnh":
  - Nếu ván đang diễn ra (`matchStatus === 'ONGOING'`), thực hiện `window.location.reload()` để chủ động ngắt kết nối WebSocket; `WebSocketEventListener` ở backend bắt sự kiện ngắt kết nối, dọn dẹp phòng khỏi RAM và xử thua/thông báo cho đối thủ theo Chính sách A.
  - Nếu ván đã kết thúc hoặc chưa bắt đầu (phòng chờ), chỉ cần reset Zustand store (`resetGame()`) và chuyển view về sảnh mà không cần reload trang.
- **Khôi phục logic dọn dẹp phòng trong `RoomManagerService`:** Bổ sung phương thức `removeRoom(roomId)` và cập nhật `WebSocketEventListener` gọi dọn sạch mapping phòng khi người chơi ngắt kết nối, ngăn chặn rò rỉ bộ nhớ (memory leak).
- **Việt hóa thông báo hệ thống:** Chuẩn hóa các thông báo sang tiếng Việt thân thiện, bao gồm thông báo đối thủ ngắt kết nối ("Đối thủ đã ngắt kết nối. Ván đấu kết thúc.") và thông báo lỗi kết nối.

---

### Lệnh 14.5 (Hotfix): Gỡ bỏ luật bắt buộc Gánh (Commit: c55fe46)
- **Vấn đề:** Luật "bắt buộc Gánh" làm mất đi chiến thuật thí quân (Mở) và bẫy Chẹt.
- **Giải pháp:** Gỡ bỏ các hàm kiểm tra `hasAnyGanhMove` và `moveResultsInGanh` trong `GanhRuleEngine.isValidMove`. Sửa đổi `GanhRuleEngineTest` để cho phép các nước đi bình thường ngay cả khi trên bàn có thế Gánh.

---

### Lệnh 15: Giao diện và Tích hợp Ô Ăn Quan (Commit: ea0d753)
- **Frontend:**
  - Xây dựng `OAnQuanBoard.tsx` tuân thủ thiết kế Anti-SaaS (ô quan bo tròn 1 bên, ô dân vuông góc viền dày).
  - Tích hợp tương tác 2 bước: Chọn ô -> Hiển thị số sỏi và 2 nút điều hướng (Trái/Phải) -> Gửi lệnh.
  - Vô hiệu hóa hoàn toàn Optimistic Update, giao diện chỉ render lại khi nhận `GameStateResponse` từ server.
- **Backend:** 
  - Mở rộng DTO `MoveRequest` thêm tham số `direction` (tùy chọn) để tương thích ngược. 
  - Tự động dịch hướng di chuyển (LEFT/RIGHT) sang tọa độ `to` (ô liền kề) trước khi đưa vào `OAnQuanRuleEngine`.

---

### Lệnh 16: Triển khai chế độ Đấu với Máy (Caro Minimax + Alpha-Beta) (Commit: c5a8ae7)
- **Thuật toán (Domain):** Xây dựng `CaroMinimax` (depth 3) và `CaroHeuristicEvaluator` thuần Java. Áp dụng cắt tỉa Alpha-Beta và tối ưu không gian duyệt bằng cách giới hạn tìm kiếm trong bán kính 2 ô quanh các quân cờ hiện có.
- **Đa luồng (Infrastructure & Application):** Khởi tạo `AiWorkerConfig` cấp phát luồng động (`Math.max(2, CPU_CORES - 1)`). Tách `AiMoveService` để xử lý AI bất đồng bộ. Giải quyết triệt để race-condition thông qua cơ chế snapshot lịch sử và đẩy lệnh I/O mạng (STOMP broadcast) ra khỏi monitor lock.
- **Frontend & UX:** Bổ sung luồng tạo phòng Đấu Máy (vào ván ngay không cần chờ). Khóa tương tác bàn cờ (`pointer-events-none`), hiển thị trạng thái "Máy đang suy nghĩ" và chặn hoàn toàn Optimistic Update để đảm bảo tính nhất quán với Server Authoritative.

