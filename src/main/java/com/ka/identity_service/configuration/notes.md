1. Tại sao lại dùng signerKey.getBytes()?
- Do máy tính và thuật toán này chỉ làm việc với các con số -> Sử dụng bytes để máy có thể dễ hiểu
2. Cú pháp :: có nghĩa là sao ?
- Cú pháp đó đồng nghĩa với truyền lamda vào nhưng đã được xây dựng sẵn rồi
3. Mapstruct có tác dụng gì ?
- Clean code làm việc chuyển một entity thành một response nào đó nhanh chóng
4. Configuration có tác dụng gì ?
- Đánh dấu đây là một nhà xưởng cung cấp các bean cho Spring
5. ApplicationRunner ?
- Là một nơi sẽ được thực thi khi spring vừa start
- args là các đối số dòng lệnh
6. Tại sao args -> {} lại code như thế ?
- Vì application runner là một interface và trong nó có duy nhất hàm run(ApplicationArguments args) 
- Cho nên việc code như thế giống như là implement cái interface
7. Cú pháp var trong java là sao ?
- Nó tự hiểu kiểu dữ liệu bên phải và gán vào,
- Nó giải quyết được vấn đề tên kiểu dữ liệu quá dài
- Không thể thay đổi kiểu dữ liệu
8. Bycrypt là gì ?
- Là một passwordEncoder 
- Dùng để mã hóa mật khẩu
- Dù cùng mật khẩu nhưng có cơ chế salt cho nên chuỗi mã hóa là khác nhau
9. Việc Inject Bean vào bằng Autowire hay constructor?
- Constructor được recommend hơn
10. Slf4j có tác dụng gì ?
- Đây là một tiện ít giúp giảm nhẹ việc define log
- Log chia làm các cấp độ, nếu ở cấp độ trên thì cấp độ dưới sẽ bị ẩn đi:
  - ERROR: Lỗi nghiêm trọng (Ứng dụng hỏng, DB sập).

  - WARN: Cảnh báo (Có gì đó lạ, nhưng chưa hỏng).

  - INFO: Thông tin (User đăng nhập, Server khởi động - Đây là mức mặc định).

  - DEBUG: Chi tiết kỹ thuật (Dành cho lập trình viên soi lỗi).

  - TRACE: Chi tiết tận "chân tơ kẽ tóc".
- Log có thể được ghi ra console hoặc file
11. Tại sao lại sử dụng Converter
- Spring security chỉ nhìn các token như một chuỗi kì lạ -> Cần người phiên dịch
12. Cả đoạn filterChain có nghĩa là sao ?
- Là chúng ta đang khai báo một chuỗi bảo mật, từ phân quyền đến Jwt,..
- oauth2ResourceServer chính là khai báo tòa nhà bảo mật bằng thẻ từ jwt
- csrf là một cơ chế chống tấn công giả mạo nhưng trong jwt có rồi
- tấn công giả mạo dựa trên cookie. Mỗi lần đăng nhập cookie sẽ được lưu lại và server tin tưởng cookie trên trình duyệt -> Hacker giả mạo cookie
- 