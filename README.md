# 🎵 MusicForMe - Personal Android Music Player (Offline & Online Streaming)

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-1.9.23-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/AndroidX-Media3_ExoPlayer-3DDC84?logo=android&logoColor=white" alt="ExoPlayer" />
  <img src="https://img.shields.io/badge/Architecture-MVVM_Clean-blue" alt="MVVM" />
  <img src="https://img.shields.io/badge/Room-Database-orange" alt="Room" />
  <img src="https://img.shields.io/badge/UI-Material_3_Spotify_Theme-1DB954" alt="Spotify UI" />
</p>

**MusicForMe** là ứng dụng nghe nhạc cá nhân hiện đại dành cho hệ điều hành Android, kết hợp kiến trúc **Hybrid (All-in-One)**:
1. **Nghe nhạc offline từ bộ nhớ máy**: Quét toàn bộ file MP3/FLAC/WAV trong máy thông qua `MediaStore API`.
2. **Nghe nhạc trực tuyến (Online Streaming)**: Phát trực tiếp các bài hát chất lượng cao qua Internet với công nghệ đệm nhạc đệm âm thanh của `AndroidX Media3 (ExoPlayer)`.
3. **Tải nhạc về máy (One-tap Download)**: Tải bất kỳ bài hát online nào về điện thoại chỉ bằng 1 chạm, tự động đồng bộ ngay vào Thư viện Offline.

---

## ✨ Điểm nổi bật & Tính năng chính

* 🎧 **Giao diện Spotify Dark Mode & Neumorphism**:
  * Tông màu tối tiết kiệm pin (Amoled Black).
  * **Mini Player** ghim cố định ở đáy màn hình, vuốt chạm chuyển bài tức thì.
  * **Trang phát nhạc chi tiết (Full Player Screen)** với bìa đĩa bo tròn, hiệu ứng đổ bóng, thanh tua thời gian thực, chế độ phát xáo trộn (Shuffle) và lặp bài (Repeat).
* ⚙️ **AndroidX Media3 (ExoPlayer) Background Service**:
  * Chạy nền bền bỉ, không bị tắt khi khóa màn hình hoặc đa nhiệm ứng dụng khác.
  * Hỗ trợ điều khiển qua **Thanh thông báo (Notification Media Controls)** và **Màn hình khóa (Lockscreen)**.
  * Tự động giảm âm lượng hoặc tạm dừng khi có cuộc gọi đến (`Audio Focus`) hoặc khi rút tai nghe (`Audio Becoming Noisy`).
* 📦 **Quản lý Kho nhạc & Yêu thích (Room Database)**:
  * Đánh dấu bài hát yêu thích (Favorite) lưu bền vững vào SQLite Room.
  * Tab "Yêu thích" phân loại riêng các bài bạn nghe thường xuyên nhất.
* 🔎 **Tìm kiếm tức thì (Instant Search)**: Lọc bài hát theo tên bài, nghệ sĩ hoặc album trong thời gian thực.
* 📥 **Tải nhạc ngầm (Android DownloadManager)**: Tải bài hát trực tiếp vào thư mục `Music/MusicForMe` với thanh tiến trình hệ thống.

---

## 🏗️ Kiến trúc & Công nghệ (Tech Stack)

* **Ngôn ngữ**: **Kotlin** (chính) + **Java** (tương thích đa ngôn ngữ, xử lý MediaMetadataRetriever).
* **Mô hình kiến trúc**: **MVVM (Model - View - ViewModel)** + Repository Pattern.
* **Audio Engine**: `androidx.media3:media3-exoplayer` & `androidx.media3:media3-session`.
* **Cơ sở dữ liệu**: `Room Database` (SQLite ORM) với Coroutines Flow.
* **Mạng & API**: `Retrofit 2` + `Gson` + `OkHttp Logging Interceptor`.
* **Hiển thị hình ảnh**: `Glide 4` (load ảnh bìa album, bo góc mượt mà, caching).
* **Quản lý luồng**: `Kotlin Coroutines` & `StateFlow / SharedFlow`.

---

## 📱 Cấu trúc Thư mục Dự án

```text
com.musicforme.app/
│
├── data/
│   ├── local/            # Room Database, DAO, Entity, MediaStoreScanner
│   ├── model/            # Song Model
│   ├── remote/           # Retrofit Service, DTO, Catalog API
│   └── repository/       # MusicRepository (Bridge giữa Local, Room & Remote)
│
├── service/
│   ├── MusicService.kt   # MediaSessionService (chạy nền ExoPlayer)
│   └── PlayerManager.kt  # Singleton điều khiển phát nhạc toàn app
│
├── ui/
│   ├── adapter/          # SongAdapter (RecyclerView ListAdapter)
│   ├── fragments/        # LibraryFragment, OnlineFragment, FavoritesFragment
│   ├── player/           # PlayerActivity (Màn hình phát nhạc lớn)
│   ├── MainActivity.kt   # Điều hướng Tab & Mini-player
│   └── MainViewModel.kt  # ViewModel quản lý State
│
└── utils/
    ├── AudioMetadataHelper.java  # Java class trích xuất thông số bitrate, audio tags
    ├── DownloadHelper.kt         # Tải file nhạc về máy
    └── TimeUtils.kt              # Định dạng mm:ss
```

---

## 🚀 Hướng dẫn mở và kiểm thử trên Android Studio

1. **Mở dự án**:
   * Khởi động **Android Studio**.
   * Chọn **Open** -> Điều hướng tới thư mục `c:\Users\admin\Downloads\MusicForMe`.
2. **Đồng bộ Gradle**:
   * Android Studio sẽ tự động tải các dependencies qua Gradle Wrapper (`Sync Project with Gradle Files`).
3. **Chạy ứng dụng (Run App)**:
   * Chọn máy ảo (Emulator) hoặc điện thoại Android thật cắm cáp USB (đã bật *USB Debugging*).
   * Nhấn nút **Run ▶️** (hoặc `Shift + F10`).
4. **Kiểm thử các chức năng**:
   * **Tab Thư viện**: Cấp quyền đọc bộ nhớ để app quét nhạc có sẵn trên điện thoại.
   * **Tab Khám phá**: Bấm phát ngay các bài hát trực tuyến hoặc bấm biểu tượng **Tải về** để lưu file vào máy.
   * **Thanh Mini Player**: Chạm vào để phóng to toàn màn hình nghe nhạc.
