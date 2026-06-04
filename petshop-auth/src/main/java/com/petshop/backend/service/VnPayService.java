package com.petshop.backend.service;

import com.petshop.backend.config.VnPayProperties;
import com.petshop.backend.dto.PaymentRequest;
import com.petshop.backend.dto.PaymentResponse;
import com.petshop.backend.model.Order;
import com.petshop.backend.repository.OrderRepository;
import com.petshop.backend.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class VnPayService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnPayProperties properties;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public VnPayService(
            VnPayProperties properties,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            PaymentService paymentService
    ) {
        this.properties = properties;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    public PaymentResponse createPaymentUrl(PaymentRequest request, String clientIp) {
        assertConfigured();

        Order order = orderRepository.findById(request.getOrderId().longValue())
                .orElseThrow(() -> new RuntimeException("Khong tim thay don hang."));

        if (paymentRepository.findByOrderId(request.getOrderId()).stream()
                .anyMatch(payment -> "Completed".equals(payment.getStatus()))) {
            throw new RuntimeException("Don hang da thanh toan.");
        }

        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (order.getFinalAmount().setScale(2, RoundingMode.HALF_UP).compareTo(amount) != 0) {
            throw new RuntimeException("So tien khong khop voi don hang.");
        }

        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        String txnRef = "ORD" + request.getOrderId() + "T" + now.format(VNPAY_DATE);

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + request.getOrderId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", buildBackendReturnUrl(request.getFrontendReturnUrl()));
        params.put("vnp_IpAddr", (clientIp == null || clientIp.isBlank()) ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", now.format(VNPAY_DATE));
        params.put("vnp_ExpireDate", now.plusMinutes(properties.getExpireMinutes()).format(VNPAY_DATE));

        String signedData = buildQuery(params);
        String paymentUrl = properties.getPayUrl() + "?" + signedData + "&vnp_SecureHash=" + hmacSha512(signedData);

        PaymentResponse response = new PaymentResponse("Redirect", "Mo cong thanh toan VNPAY", null);
        response.setPaymentUrl(paymentUrl);
        return response;
    }

    public VnPayReturnResult handleReturn(Map<String, String> requestParams) {
        Map<String, String> vnpParams = requestParams.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().startsWith("vnp_"))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));

        String secureHash = vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        if (secureHash == null || !hmacSha512(buildQuery(vnpParams)).equalsIgnoreCase(secureHash)) {
            return VnPayReturnResult.failed("Chu ky VNPAY khong hop le.", null, "97", resolveFrontendReturnUrl(requestParams.get("returnTo")));
        }

        Integer orderId = parseOrderId(vnpParams.get("vnp_TxnRef"));
        String responseCode = vnpParams.get("vnp_ResponseCode");
        String transactionStatus = vnpParams.get("vnp_TransactionStatus");

        if (orderId == null) {
            return VnPayReturnResult.failed("Khong doc duoc ma don hang tu VNPAY.", null, responseCode, resolveFrontendReturnUrl(requestParams.get("returnTo")));
        }

        if (!"00".equals(responseCode) || !"00".equals(transactionStatus)) {
            return VnPayReturnResult.failed("Giao dich VNPAY khong thanh cong.", orderId, responseCode, resolveFrontendReturnUrl(requestParams.get("returnTo")));
        }

        var existingPayment = paymentRepository.findByOrderId(orderId).stream()
                .filter(payment -> "Completed".equals(payment.getStatus()))
                .findFirst();
        if (existingPayment.isPresent()) {
            return VnPayReturnResult.success("Don hang da duoc ghi nhan thanh toan.", orderId, existingPayment.get().getPaymentId(), resolveFrontendReturnUrl(requestParams.get("returnTo")));
        }

        BigDecimal amount = new BigDecimal(vnpParams.getOrDefault("vnp_Amount", "0"))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(orderId);
        paymentRequest.setAmount(amount);
        paymentRequest.setPaymentMethod("QRCode");
        paymentRequest.setTransactionRef(vnpParams.get("vnp_TransactionNo"));
        paymentRequest.setNotes(buildNotes(vnpParams));

        PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);
        return VnPayReturnResult.success("Thanh toan VNPAY thanh cong.", orderId, paymentResponse.getPaymentId(), resolveFrontendReturnUrl(requestParams.get("returnTo")));
    }

    public String buildReturnHtml(VnPayReturnResult result) {
        String status = result.success() ? "Thanh toan thanh cong" : "Thanh toan that bai";
        String frontendUrl = result.frontendReturnUrl();
        String backUrl = appendQuery(frontendUrl, result);
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta http-equiv="refresh" content="2;url=%s">
                  <title>Ket qua VNPAY</title>
                  <style>
                    body{font-family:Arial,sans-serif;background:#f8fafc;color:#0f172a;display:grid;place-items:center;min-height:100vh;margin:0}
                    main{width:min(440px,calc(100vw - 32px));background:#fff;border:1px solid #e2e8f0;border-radius:12px;padding:28px;box-shadow:0 20px 45px rgba(15,23,42,.08)}
                    h1{font-size:24px;margin:0 0 12px}
                    p{line-height:1.5;color:#475569}
                    small{color:#64748b}
                    a{display:inline-block;margin-top:18px;background:#007517;color:#fff;text-decoration:none;border-radius:8px;padding:10px 16px;font-weight:700}
                  </style>
                </head>
                <body>
                  <main>
                    <h1>%s</h1>
                    <p>%s</p>
                    <small>Ma phan hoi: %s. Tu dong quay lai sau 2 giay.</small><br>
                    <a href="%s">Quay lai trang thanh toan</a>
                  </main>
                </body>
                </html>
                """.formatted(
                escapeHtml(backUrl),
                escapeHtml(status),
                escapeHtml(result.message()),
                escapeHtml(result.responseCode() == null ? "" : result.responseCode()),
                escapeHtml(backUrl)
        );
    }

    private void assertConfigured() {
        if (isBlank(properties.getPayUrl()) || isBlank(properties.getTmnCode())
                || isBlank(properties.getHashSecret()) || isBlank(properties.getReturnUrl())) {
            throw new RuntimeException("Chua cau hinh VNPAY.");
        }
    }

    private Integer parseOrderId(String txnRef) {
        if (txnRef == null || !txnRef.startsWith("ORD") || !txnRef.contains("T")) {
            return null;
        }
        try {
            return Integer.parseInt(txnRef.substring(3, txnRef.indexOf('T')));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildNotes(Map<String, String> params) {
        return "VNPAY bank=" + params.getOrDefault("vnp_BankCode", "")
                + "; card=" + params.getOrDefault("vnp_CardType", "")
                + "; payDate=" + params.getOrDefault("vnp_PayDate", "")
                + "; txnRef=" + params.getOrDefault("vnp_TxnRef", "");
    }

    private String buildQuery(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String appendQuery(String url, VnPayReturnResult result) {
        String separator = url.contains("?") ? "&" : "?";
        return url + separator
                + "vnpayStatus=" + (result.success() ? "success" : "failed")
                + "&orderId=" + (result.orderId() == null ? "" : result.orderId())
                + "&paymentId=" + (result.paymentId() == null ? "" : result.paymentId())
                + "&responseCode=" + encode(result.responseCode() == null ? "" : result.responseCode());
    }

    private String buildBackendReturnUrl(String frontendReturnUrl) {
        String returnTo = resolveFrontendReturnUrl(frontendReturnUrl);
        if (returnTo.equals(properties.getFrontendReturnUrl())) {
            return properties.getReturnUrl();
        }
        return properties.getReturnUrl() + (properties.getReturnUrl().contains("?") ? "&" : "?")
                + "returnTo=" + encode(returnTo);
    }

    private String resolveFrontendReturnUrl(String frontendReturnUrl) {
        if (isAllowedFrontendReturnUrl(frontendReturnUrl)) {
            return frontendReturnUrl;
        }
        return properties.getFrontendReturnUrl();
    }

    private boolean isAllowedFrontendReturnUrl(String url) {
        if (isBlank(url)) {
            return false;
        }
        return url.startsWith("http://localhost:")
                || url.startsWith("http://127.0.0.1:")
                || url.startsWith("https://localhost:")
                || url.startsWith("https://127.0.0.1:");
    }

    private String hmacSha512(String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(properties.getHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hash.append(String.format("%02x", b));
            }
            return hash.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Khong the tao chu ky VNPAY.", ex);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public record VnPayReturnResult(
            boolean success,
            String message,
            Integer orderId,
            Integer paymentId,
            String responseCode,
            String frontendReturnUrl
    ) {
        static VnPayReturnResult success(String message, Integer orderId, Integer paymentId, String frontendReturnUrl) {
            return new VnPayReturnResult(true, message, orderId, paymentId, "00", frontendReturnUrl);
        }

        static VnPayReturnResult failed(String message, Integer orderId, String responseCode, String frontendReturnUrl) {
            return new VnPayReturnResult(false, message, orderId, null, responseCode, frontendReturnUrl);
        }
    }
}
