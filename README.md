# Cách sử dụng
tải https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html và đổi môi truờng của bạn sang 
java 17 (Cách làm https://www.youtube.com/watch?v=YONvtseO574)
## Bước 1: Clone code
Mở terminal hoặc git bash trong folder bạn muốn đặt project, nhập từng lệnh sau:
- `git clone https://github.com/LeHoaiNam756/CT4J.git`
- `git checkout feature-greedy-path-finder`
## Bước 2: Cầu hình tool
Trong **/src/core/utils/FilePath.java**, sửa biến `JCIA_PROJECT_ROOT_PATH` thành nới bạn đặt project, ví dụ - 
*"C:\CIA\JCIA\CT4Jj"*
## Bước 3: Thêm z3
Do z3 trên maven repo đã cũ, vì vậy ta phải thêm thủ công z3 phiên bản mới hơn. File jar của z3 nằm trong `src/core/lib`.
Đầu tiên, thêm z3 jar vào local repo của maven trên máy bạn để maven có thể tải trực tiếp từ máy bạn. Chạy lệnh sau \
`mvn install:install-file -Dfile="C:\CIA\JCIA\CT4J\src\main\java\core\lib\com.microsoft.z3.jar" -DgroupId="com.microsoft" -DartifactId="z3" -Dversion="4.14.0" -Dpackaging=jar`.
 Nếu gặp lỗi mà không xử lý được, bạn hãy tìm .m2 folder trong máy (nếu window thì nó thuờng nằm trong ổ C\User\Admin). Bạn sẽ 
thấy folder repository. Hãy tạo và copy file jar của z3 vào đùng folder com\microsoft\z3\4.14.0 rồi load lại project

## Bước 4: Chạy tool
Mở file `src\main\java\core\Main.java`, chạy hàm main.