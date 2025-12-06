# Webhook thông tin thanh toán

[![payOS Logo](https://payos.vn/wp-content/uploads/2025/06/Casso-payOSLogo-1.svg)](https://payos.vn)

* Webhook thanh toán
  * EventWebhook nhận thông tin thanh toán

[API docs by Redocly](https://redocly.com/redoc/)

# payOS Payment Webhook API<!-- --> (<!-- -->latest<!-- -->)

payOS support

<!-- -->

:

<!-- -->

<support@payos.vn> URL: <https://payos.vn>

<!-- -->

[Terms of Service](https://payos.vn/thoa-thuan-su-dung/)

Webhook API cho hệ thống thanh toán payOS.

### Trước khi bắt đầu

* Bạn đã tạo một tài khoản <https://my.payos.vn>.
* Bạn đã xác thực một doanh nghiệp hoặc cá nhân trên <https://my.payos.vn>, [xem hướng dẫn](https://payos.vn/docs/huong-dan-su-dung/xac-thuc-to-chuc/)
* Bạn đã tạo một kênh thanh toán, [xem hướng dẫn](https://payos.vn/docs/huong-dan-su-dung/tao-kenh-thanh-toan/).

### Môi trường

* Production: <https://api-merchant.payos.vn>

Đăng ký chương trình đối tác tích hợp payOS [Tại đây](https://payos.vn/chuong-trinh-doi-tac-tich-hop/)

## [](#tag/payment-webhook)Webhook thanh toán

Webhook thanh toán

## [](#tag/payment-webhook/operation/payment-webhook)Webhook nhận thông tin thanh toán<!-- --> <!-- -->Webhook

Webhook của cửa hàng dùng để nhận dữ liệu thanh toán từ payOS, [Dữ liệu mẫu](https://payos.vn/docs/tich-hop-webhook/kiem-tra-du-lieu-voi-signature/)

##### Request Body schema: application/json

|                   |                                                                                                                                    |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| coderequired      | stringMã lỗi                                                                                                                       |
| descrequired      | stringThông tin lỗi                                                                                                                |
| successrequired   | boolean                                                                                                                            |
| datarequired      | object                                                                                                                             |
| signaturerequired | stringChữ kí để kiểm tra thông tin, [chi tiết dữ liệu mẫu](https://payos.vn/docs/tich-hop-webhook/kiem-tra-du-lieu-voi-signature/) |

### Responses

**200<!-- -->**

Phản hồi trạng thái mã 2XX để xác nhận webhook gửi thành công

### <!-- -->Request samples<!-- -->

* Payload

Content type

application/json

Copy

Expand all  Collapse all

`{
"code": "00",
"desc": "success",
"success": true,
"data": {
"orderCode": 123,
"amount": 3000,
"description": "VQRIO123",
"accountNumber": "12345678",
"reference": "TF230204212323",
"transactionDateTime": "2023-02-04 18:25:00",
"currency": "VND",
"paymentLinkId": "124c33293c43417ab7879e14c8d9eb18",
"code": "00",
"desc": "Thành công",
"counterAccountBankId": "",
"counterAccountBankName": "",
"counterAccountName": "",
"counterAccountNumber": "",
"virtualAccountName": "",
"virtualAccountNumber": ""
},
"signature": "8d8640d802576397a1ce45ebda7f835055768ac7ad2e0bfb77f9b8f12cca4c7f"
}`


Tạo link thanh toán
API dùng để tạo link thanh toán đơn hàng

Authorizations:
(x-client-idx-api-key)
header Parameters
x-partner-code	
string
Partner Code tham gia chương trình tích hợp đối tác payOS Tại đây

Request Body schema: application/json
orderCode
required
integer
Mã đơn hàng

amount
required
integer
Số tiền thanh toán

description
required
string
Mô tả thanh toán, với tài khoản ngân hàng không phải liên kết qua payOS thì giới hạn ký tự là 9

buyerName	
string
Tên của người mua hàng. Thông tin dùng trong trường hợp tích hợp tạo hoá đơn điện tử.

buyerCompanyName	
string
Tên đơn vị mua hàng. Thông tin dùng trong trường hợp tích hợp tạo hoá đơn điện tử.

buyerTaxCode	
string
Mã số thuế của đơn vị mua hàng. Thông tin dùng trong trường hợp tích hợp tạo hoá đơn điện tử.

buyerAddress	
string
Địa chỉ của đơn vị mua hàng. Thông tin dùng trong trường hợp tích hợp tạo hoá đơn điện tử.

buyerEmail	
string <email>
Email của người mua hàng. Thông tin dùng trong trường hợp tích hợp tạo hoá đơn điện tử.

buyerPhone	
string
Số điện thoại người mua hàng. Thông tin dùng trong trường hợp tích hợp tạo hoá đơn điện tử.

items	
Array of objects
Danh sách các sản phẩm thanh toán

cancelUrl
required
string <uri>
URL nhận dữ liệu khi người dùng chọn Huỷ đơn hàng.

returnUrl
required
string <uri>
URL nhận dữ liệu khi đơn hàng thanh toán thành công

invoice	
object
Thông tin hóa đơn

expiredAt	
number <timestamp>
Thời gian hết hạn của link thanh toán, là Unix Timestamp và kiểu Int32

signature
required
string
Chữ ký kiểm tra thông tin không bị thay đổi trong qua trình chuyển dữ liệu từ hệ thống của bạn sang payOS. Bạn cần dùng checksum key từ Kênh thanh toán và HMAC_SHA256 để tạo signature và data theo định dạng được sort theo alphabet: amount=$amount&cancelUrl=$cancelUrl&description=$description&orderCode=$orderCode&returnUrl=$returnUrl.

Responses
200 Thành công
401 Unauthorized
429 Too Many Request - Gọi API quá nhiều

post
/v2/payment-requests
https://api-merchant.payos.vn/v2/payment-requests
Request samples
Payload
Content type
application/json

Copy
Expand allCollapse all
{
"orderCode": 0,
"amount": 0,
"description": "string",
"buyerName": "string",
"buyerCompanyName": "string",
"buyerTaxCode": "string",
"buyerAddress": "string",
"buyerEmail": "user@example.com",
"buyerPhone": "string",
"items": [
{
"name": "string",
"quantity": 0,
"price": 0,
"unit": "string",
"taxPercentage": -2
}
],
"cancelUrl": "http://example.com",
"returnUrl": "http://example.com",
"invoice": {
"buyerNotGetInvoice": true,
"taxPercentage": -2
},
"expiredAt": 0,
"signature": "string"
}
Response samples
200401
Content type
application/json
Example

Tạo link thanh toán thành công
Tạo link thanh toán thành công

Copy
Expand allCollapse all
{
"code": "00",
"desc": "success",
"data": {
"bin": "970422",
"accountNumber": "113366668888",
"accountName": "QUY VAC XIN PHONG CHONG COVID",
"amount": 10000,
"description": "THANH TOAN DON HANG 123",
"orderCode": 123,
"currency": "VND",
"paymentLinkId": "124c33293c934a85be5b7f8761a27a07",
"status": "PENDING",
"checkoutUrl": "https://pay.payos.vn/web/124c33293c934a85be5b7f8761a27a07",
"qrCode": "00020101021238570010A000000727012700069704220113113366668888020899998888530370454061000005802VN62230819THANH TOAN DON HANG6304BE36"
},
"signature": "aec38349957f1a6c22ded683d06477ac5dfe047cf5f23c70dc8e048759ef1234"
}
Lấy thông tin link thanh toán
API dùng để lấy thông tin của link thanh toán

Lưu ý: Hiện tại thông tin tài khoản đối ứng trong các trường counterAccount chỉ được hỗ trợ bởi các ngân hàng sau:

MB Bank
ACB
KienlongBank
Authorizations:
(x-client-idx-api-key)
path Parameters
id
required
number or string
Example: 3019
Mã đơn hàng của cửa hàng hoặc mã link thanh toán của payOS

Responses
200 Thành công
401 Unauthorized
429 Too Many Request - Gọi API quá nhiều