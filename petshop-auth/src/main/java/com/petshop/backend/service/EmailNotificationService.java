package com.petshop.backend.service;

import com.petshop.backend.model.Booking;
import com.petshop.backend.model.Employee;
import com.petshop.backend.model.Order;
import com.petshop.backend.model.OrderDetail;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${notifications.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notifications.email.from:${spring.mail.username:}}")
    private String fromAddress;

    @Value("${notifications.email.from-name:PETSHOP}")
    private String fromName;

    public EmailNotificationService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendAssignedBookingNotification(Booking booking) {
        if (!emailEnabled) {
            return;
        }

        Employee employee = booking.getEmployee();
        if (employee == null || !StringUtils.hasText(employee.getEmail())) {
            return;
        }

        String subject = "PetShop | Lịch hẹn mới #" + booking.getId();
        String plainText = buildBookingPlainText(booking);
        String htmlText = buildBookingHtml(booking);

        try {
            sendHtmlEmail(employee.getEmail(), subject, plainText, htmlText);
        } catch (Exception ex) {
            log.error("Failed to send booking assignment email for booking id {}", booking.getId(), ex);
        }
    }

    public void sendOrderDeliveredNotification(Order order, String paymentInfo) {
        if (!emailEnabled || order == null || order.getCustomer() == null) {
            return;
        }

        String customerEmail = order.getCustomer().getEmail();
        if (!StringUtils.hasText(customerEmail)) {
            return;
        }

        String subject = "PetShop | Đơn hàng của bạn đã hoàn tất #" + order.getId();
        String plainText = buildOrderDeliveredPlainText(order, paymentInfo);
        String htmlText = buildOrderDeliveredHtml(order, paymentInfo);

        try {
            sendHtmlEmail(customerEmail, subject, plainText, htmlText);
        } catch (Exception ex) {
            log.error("Failed to send delivered order email for order id {}", order.getId(), ex);
        }
    }

    public void sendBookingCompletedNotification(Booking booking) {
        if (!emailEnabled || booking == null || booking.getCustomer() == null) {
            return;
        }

        String customerEmail = booking.getCustomer().getEmail();
        if (!StringUtils.hasText(customerEmail)) {
            return;
        }

        String subject = "PetShop | Lịch hẹn của bạn đã hoàn thành #" + booking.getId();
        String plainText = buildBookingCompletedPlainText(booking);
        String htmlText = buildBookingCompletedHtml(booking);

        try {
            sendHtmlEmail(customerEmail, subject, plainText, htmlText);
        } catch (Exception ex) {
            log.error("Failed to send completed booking email for booking id {}", booking.getId(), ex);
        }
    }

    public void sendTestEmail(String to, String subject, String messageBody) {
        if (!emailEnabled) {
            throw new RuntimeException("Email notification đang tắt. Hãy bật EMAIL_NOTIFICATIONS_ENABLED=true.");
        }

        if (!StringUtils.hasText(to)) {
            throw new RuntimeException("Email nhận không hợp lệ.");
        }

        String resolvedSubject = StringUtils.hasText(subject) ? subject : "Test email từ PetShop Backend";
        String resolvedMessage = StringUtils.hasText(messageBody)
                ? messageBody
                : "Đây là email test để kiểm tra cấu hình SMTP trong hệ thống PetShop.";

        try {
            sendHtmlEmail(
                    to,
                    resolvedSubject,
                    resolvedMessage,
                    buildTestEmailHtml(resolvedSubject, resolvedMessage)
            );
        } catch (Exception ex) {
            log.error("Failed to send test email to {}", to, ex);
            throw new RuntimeException("Gửi email test thất bại: " + ex.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String plainText, String htmlText) throws Exception {
        JavaMailSender mailSender = requireMailSender();
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        if (StringUtils.hasText(fromAddress)) {
            helper.setFrom(fromAddress, fromName);
        }
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(plainText, htmlText);
        mailSender.send(mimeMessage);
    }

    private JavaMailSender requireMailSender() {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new RuntimeException("JavaMailSender chưa được cấu hình.");
        }
        return mailSender;
    }

    private String buildBookingPlainText(Booking booking) {
        String customerName = booking.getCustomer() != null ? booking.getCustomer().getFullName() : "N/A";
        String customerPhone = booking.getCustomer() != null ? defaultText(booking.getCustomer().getPhone()) : "Chưa có";
        String petName = booking.getPet() != null ? booking.getPet().getName() : "N/A";
        String petSpecies = booking.getPet() != null ? defaultText(booking.getPet().getSpecies()) : "Chưa có";
        String serviceName = booking.getService() != null ? booking.getService().getServiceName() : "N/A";
        String bookingDate = booking.getBookingDate() != null
                ? booking.getBookingDate().toLocalDate().format(DATE_FORMAT)
                : "N/A";
        String bookingTime = booking.getBookingTime() != null
                ? booking.getBookingTime().format(TIME_FORMAT)
                : "N/A";
        String notes = defaultText(booking.getNotes());

        return String.join("\n",
                "PETSHOP - THÔNG BÁO LỊCH HẸN MỚI",
                "",
                "Xin chào " + booking.getEmployee().getFullName() + ",",
                "Bạn vừa được phân công một lịch hẹn mới.",
                "",
                "TÓM TẮT NHANH",
                "- Mã lịch hẹn: #" + booking.getId(),
                "- Ngày hẹn: " + bookingDate,
                "- Giờ hẹn: " + bookingTime,
                "- Dịch vụ: " + serviceName,
                "",
                "THÔNG TIN CHI TIẾT",
                "- Khách hàng: " + customerName,
                "- Số điện thoại: " + customerPhone,
                "- Thú cưng: " + petName + " (" + petSpecies + ")",
                "- Trạng thái: " + mapStatusLabel(booking.getStatus()),
                "- Ghi chú: " + notes,
                "",
                "Vui lòng kiểm tra lại lịch làm việc để chuẩn bị tốt nhất cho ca hẹn này.",
                "",
                "PetShop Admin"
        );
    }

    private String buildBookingHtml(Booking booking) {
        String employeeName = escapeHtml(booking.getEmployee().getFullName());
        String customerName = booking.getCustomer() != null ? escapeHtml(booking.getCustomer().getFullName()) : "N/A";
        String customerPhone = booking.getCustomer() != null ? escapeHtml(defaultText(booking.getCustomer().getPhone())) : "Chưa có";
        String petName = booking.getPet() != null ? escapeHtml(booking.getPet().getName()) : "N/A";
        String petSpecies = booking.getPet() != null ? escapeHtml(defaultText(booking.getPet().getSpecies())) : "Chưa có";
        String petBreed = booking.getPet() != null ? escapeHtml(defaultText(booking.getPet().getBreed())) : "Chưa có";
        String serviceName = booking.getService() != null ? escapeHtml(booking.getService().getServiceName()) : "N/A";
        String bookingDate = booking.getBookingDate() != null
                ? booking.getBookingDate().toLocalDate().format(DATE_FORMAT)
                : "N/A";
        String bookingTime = booking.getBookingTime() != null
                ? booking.getBookingTime().format(TIME_FORMAT)
                : "N/A";
        String notes = escapeHtml(defaultText(booking.getNotes()));
        String statusLabel = escapeHtml(mapStatusLabel(booking.getStatus()));
        String amount = escapeHtml(formatCurrency(booking.getTotalPrice()));

        return """
                <!doctype html>
                <html lang="vi">
                <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,'Segoe UI',sans-serif;color:#1f2937;">
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0;">
                    Bạn vừa được phân công lịch hẹn mới tại PetShop. Kiểm tra nhanh ngày, giờ và thông tin khách hàng trong email này.
                  </div>
                  <div style="max-width:680px;margin:0 auto;padding:28px 16px;">
                    <div style="background:linear-gradient(135deg,#0f766e,#14b8a6);border-radius:28px;padding:28px 24px;color:#ffffff;box-shadow:0 20px 44px rgba(15,118,110,0.18);">
                      <div style="font-size:12px;letter-spacing:1.8px;text-transform:uppercase;opacity:0.82;">PetShop Admin</div>
                      <h1 style="margin:10px 0 8px;font-size:30px;line-height:1.2;">Bạn có lịch hẹn mới</h1>
                      <p style="margin:0;font-size:15px;line-height:1.7;opacity:0.96;">
                        Xin chào <strong>%s</strong>, hệ thống vừa ghi nhận một lịch hẹn mới và đã phân công bạn phụ trách.
                      </p>
                    </div>

                    <div style="background:#ffffff;margin-top:-18px;border-radius:28px;padding:24px 22px;box-shadow:0 18px 38px rgba(15,23,42,0.08);border:1px solid #e5e7eb;">
                      <div style="display:inline-block;background:#ecfeff;color:#155e75;padding:8px 14px;border-radius:999px;font-size:13px;font-weight:700;">
                        Mã lịch hẹn #%d
                      </div>

                      <table role="presentation" style="width:100%%;border-collapse:separate;border-spacing:12px;margin-top:18px;">
                        <tr>
                          <td style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:18px;padding:16px;vertical-align:top;width:50%%;">
                            <div style="font-size:12px;text-transform:uppercase;letter-spacing:1.2px;color:#64748b;margin-bottom:8px;">Ngày hẹn</div>
                            <div style="font-size:24px;font-weight:700;color:#0f172a;line-height:1.2;">%s</div>
                          </td>
                          <td style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:18px;padding:16px;vertical-align:top;width:50%%;">
                            <div style="font-size:12px;text-transform:uppercase;letter-spacing:1.2px;color:#64748b;margin-bottom:8px;">Giờ hẹn</div>
                            <div style="font-size:24px;font-weight:700;color:#0f172a;line-height:1.2;">%s</div>
                          </td>
                        </tr>
                      </table>

                      <div style="margin-top:10px;border:1px solid #e2e8f0;border-radius:20px;overflow:hidden;">
                        <div style="padding:14px 16px;background:#f8fafc;font-size:13px;font-weight:700;color:#334155;text-transform:uppercase;letter-spacing:1px;">
                          Thông tin lịch hẹn
                        </div>
                        <table role="presentation" style="width:100%%;border-collapse:collapse;font-size:15px;">
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;">Khách hàng</td>
                            <td style="padding:14px 16px;font-weight:600;color:#0f172a;border-top:1px solid #e2e8f0;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;">Số điện thoại</td>
                            <td style="padding:14px 16px;color:#0f172a;border-top:1px solid #e2e8f0;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;">Thú cưng</td>
                            <td style="padding:14px 16px;color:#0f172a;border-top:1px solid #e2e8f0;">%s (%s - %s)</td>
                          </tr>
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;">Dịch vụ</td>
                            <td style="padding:14px 16px;color:#0f172a;border-top:1px solid #e2e8f0;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;">Tạm tính</td>
                            <td style="padding:14px 16px;color:#0f172a;border-top:1px solid #e2e8f0;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;">Trạng thái</td>
                            <td style="padding:14px 16px;border-top:1px solid #e2e8f0;">
                              <span style="display:inline-block;background:#f0fdf4;color:#166534;padding:7px 12px;border-radius:999px;font-weight:700;font-size:13px;">%s</span>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;vertical-align:top;">Ghi chú</td>
                            <td style="padding:14px 16px;color:#0f172a;line-height:1.7;border-top:1px solid #e2e8f0;">%s</td>
                          </tr>
                        </table>
                      </div>

                      <div style="margin-top:18px;padding:16px 18px;background:#fff7ed;border:1px solid #fed7aa;border-radius:18px;color:#9a3412;font-size:14px;line-height:1.7;">
                        Vui lòng kiểm tra lại lịch làm việc và chuẩn bị trước các bước cần thiết để phục vụ khách hàng tốt nhất.
                      </div>
                    </div>

                    <p style="margin:16px 4px 0;color:#64748b;font-size:12px;line-height:1.7;text-align:center;">
                      Email này được gửi tự động từ hệ thống PetShop. Vui lòng không trả lời trực tiếp email này.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(
                employeeName,
                booking.getId(),
                escapeHtml(bookingDate),
                escapeHtml(bookingTime),
                customerName,
                customerPhone,
                petName,
                petSpecies,
                petBreed,
                serviceName,
                amount,
                statusLabel,
                notes
        );
    }

    private String buildTestEmailHtml(String subject, String messageBody) {
        return """
                <!doctype html>
                <html lang="vi">
                <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,'Segoe UI',sans-serif;color:#1f2937;">
                  <div style="max-width:640px;margin:0 auto;padding:32px 20px;">
                    <div style="background:#ffffff;border-radius:24px;padding:28px 32px;box-shadow:0 16px 35px rgba(15,23,42,0.08);">
                      <div style="font-size:13px;letter-spacing:1.8px;text-transform:uppercase;color:#0f766e;font-weight:700;">PetShop Admin</div>
                      <h1 style="margin:12px 0 10px;font-size:26px;">%s</h1>
                      <p style="margin:0;color:#475569;line-height:1.8;font-size:15px;">%s</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(escapeHtml(subject), escapeHtml(messageBody).replace("\n", "<br>"));
    }

    private String buildOrderDeliveredPlainText(Order order, String paymentInfo) {
        String customerName = order.getCustomer() != null ? order.getCustomer().getFullName() : "Quý khách";
        String orderDate = order.getOrderDate() != null
                ? order.getOrderDate().toLocalDate().format(DATE_FORMAT)
                : "N/A";
        String productSummary = order.getOrderDetails() == null || order.getOrderDetails().isEmpty()
                ? "Không có sản phẩm"
                : order.getOrderDetails().stream()
                .map(this::formatOrderItemLine)
                .collect(Collectors.joining("\n"));

        return String.join("\n",
                "PETSHOP - ĐƠN HÀNG HOÀN TẤT",
                "",
                "Xin chào " + customerName + ",",
                "Đơn hàng của bạn đã được hoàn tất thành công.",
                "",
                "THÔNG TIN ĐƠN HÀNG",
                "- Mã đơn hàng: #" + order.getId(),
                "- Ngày đặt: " + orderDate,
                "- Trạng thái: Hoàn tất",
                "- Thanh toán: " + defaultText(paymentInfo),
                "- Tổng tiền: " + formatCurrency(order.getFinalAmount()),
                "",
                "SẢN PHẨM",
                productSummary,
                "",
                "Ghi chú: " + defaultText(order.getNotes()),
                "",
                "Cảm ơn bạn đã mua sắm tại PetShop.",
                "PetShop Admin"
        );
    }

    private String buildBookingCompletedPlainText(Booking booking) {
        String customerName = booking.getCustomer() != null ? booking.getCustomer().getFullName() : "Quý khách";
        String petName = booking.getPet() != null ? booking.getPet().getName() : "N/A";
        String serviceName = booking.getService() != null ? booking.getService().getServiceName() : "N/A";
        String bookingDate = booking.getBookingDate() != null
                ? booking.getBookingDate().toLocalDate().format(DATE_FORMAT)
                : "N/A";
        String bookingTime = booking.getBookingTime() != null
                ? booking.getBookingTime().format(TIME_FORMAT)
                : "N/A";
        String employeeName = booking.getEmployee() != null
                ? booking.getEmployee().getFullName()
                : "Nhân viên PetShop";

        return String.join("\n",
                "PETSHOP - LỊCH HẸN HOÀN THÀNH",
                "",
                "Xin chào " + customerName + ",",
                "Lịch hẹn của bạn tại PetShop đã được hoàn thành thành công.",
                "",
                "THÔNG TIN LỊCH HẸN",
                "- Mã lịch hẹn: #" + booking.getId(),
                "- Dịch vụ: " + serviceName,
                "- Thú cưng: " + petName,
                "- Ngày hẹn: " + bookingDate,
                "- Giờ hẹn: " + bookingTime,
                "- Nhân viên phụ trách: " + employeeName,
                "- Trạng thái: Hoàn thành",
                "- Tổng chi phí: " + formatCurrency(booking.getTotalPrice()),
                "",
                "Cảm ơn bạn đã sử dụng dịch vụ tại PetShop. Rất mong được đồng hành cùng bạn trong những lần hẹn tiếp theo.",
                "",
                "PetShop Admin"
        );
    }

    private String buildOrderDeliveredHtml(Order order, String paymentInfo) {
        String customerName = order.getCustomer() != null ? escapeHtml(order.getCustomer().getFullName()) : "Quý khách";
        String orderDate = order.getOrderDate() != null
                ? order.getOrderDate().toLocalDate().format(DATE_FORMAT)
                : "N/A";
        String itemRows = buildOrderItemRows(order);
        String notes = escapeHtml(defaultText(order.getNotes()));
        String paymentLabel = escapeHtml(defaultText(paymentInfo));
        String finalAmount = escapeHtml(formatCurrency(order.getFinalAmount()));

        return """
                <!doctype html>
                <html lang="vi">
                <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,'Segoe UI',sans-serif;color:#1f2937;">
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0;">
                    Đơn hàng #%d của bạn tại PetShop đã hoàn tất. Cảm ơn bạn đã mua sắm cùng chúng tôi.
                  </div>
                  <div style="max-width:680px;margin:0 auto;padding:28px 16px;">
                    <div style="background:linear-gradient(135deg,#1d4ed8,#0ea5e9);border-radius:28px;padding:28px 24px;color:#ffffff;box-shadow:0 20px 44px rgba(37,99,235,0.18);">
                      <div style="font-size:12px;letter-spacing:1.8px;text-transform:uppercase;opacity:0.82;">PetShop Admin</div>
                      <h1 style="margin:10px 0 8px;font-size:30px;line-height:1.2;">Đơn hàng của bạn đã hoàn tất</h1>
                      <p style="margin:0;font-size:15px;line-height:1.7;opacity:0.96;">
                        Xin chào <strong>%s</strong>, PetShop đã hoàn tất đơn hàng của bạn. Cảm ơn bạn đã tin tưởng mua sắm cùng chúng tôi.
                      </p>
                    </div>

                    <div style="background:#ffffff;margin-top:-18px;border-radius:28px;padding:24px 22px;box-shadow:0 18px 38px rgba(15,23,42,0.08);border:1px solid #e5e7eb;">
                      <div style="display:inline-block;background:#eff6ff;color:#1d4ed8;padding:8px 14px;border-radius:999px;font-size:13px;font-weight:700;">
                        Mã đơn hàng #%d
                      </div>

                      <table role="presentation" style="width:100%%;border-collapse:separate;border-spacing:12px;margin-top:18px;">
                        <tr>
                          <td style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:18px;padding:16px;vertical-align:top;width:50%%;">
                            <div style="font-size:12px;text-transform:uppercase;letter-spacing:1.2px;color:#64748b;margin-bottom:8px;">Ngày đặt</div>
                            <div style="font-size:24px;font-weight:700;color:#0f172a;line-height:1.2;">%s</div>
                          </td>
                          <td style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:18px;padding:16px;vertical-align:top;width:50%%;">
                            <div style="font-size:12px;text-transform:uppercase;letter-spacing:1.2px;color:#64748b;margin-bottom:8px;">Tổng thanh toán</div>
                            <div style="font-size:24px;font-weight:700;color:#0f172a;line-height:1.2;">%s</div>
                          </td>
                        </tr>
                      </table>

                      <div style="margin-top:10px;border:1px solid #e2e8f0;border-radius:20px;overflow:hidden;">
                        <div style="padding:14px 16px;background:#f8fafc;font-size:13px;font-weight:700;color:#334155;text-transform:uppercase;letter-spacing:1px;">
                          Thông tin đơn hàng
                        </div>
                        <table role="presentation" style="width:100%%;border-collapse:collapse;font-size:15px;">
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;">Trạng thái</td>
                            <td style="padding:14px 16px;border-top:1px solid #e2e8f0;">
                              <span style="display:inline-block;background:#ecfdf5;color:#047857;padding:7px 12px;border-radius:999px;font-weight:700;font-size:13px;">Hoàn tất</span>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;">Thanh toán</td>
                            <td style="padding:14px 16px;color:#0f172a;border-top:1px solid #e2e8f0;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;vertical-align:top;">Ghi chú</td>
                            <td style="padding:14px 16px;color:#0f172a;line-height:1.7;border-top:1px solid #e2e8f0;">%s</td>
                          </tr>
                        </table>
                      </div>

                      <div style="margin-top:18px;border:1px solid #e2e8f0;border-radius:20px;overflow:hidden;">
                        <div style="padding:14px 16px;background:#f8fafc;font-size:13px;font-weight:700;color:#334155;text-transform:uppercase;letter-spacing:1px;">
                          Sản phẩm trong đơn
                        </div>
                        <table role="presentation" style="width:100%%;border-collapse:collapse;font-size:15px;">
                          %s
                        </table>
                      </div>

                      <div style="margin-top:18px;padding:16px 18px;background:#eff6ff;border:1px solid #bfdbfe;border-radius:18px;color:#1d4ed8;font-size:14px;line-height:1.7;">
                        Nếu cần hỗ trợ thêm về đơn hàng, bạn có thể liên hệ lại PetShop bất cứ lúc nào. Rất mong được phục vụ bạn trong những lần tiếp theo.
                      </div>
                    </div>

                    <p style="margin:16px 4px 0;color:#64748b;font-size:12px;line-height:1.7;text-align:center;">
                      Email này được gửi tự động từ hệ thống PetShop. Vui lòng không trả lời trực tiếp email này.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(
                order.getId(),
                customerName,
                order.getId(),
                escapeHtml(orderDate),
                finalAmount,
                paymentLabel,
                notes,
                itemRows
        );
    }

    private String buildBookingCompletedHtml(Booking booking) {
        String customerName = booking.getCustomer() != null ? escapeHtml(booking.getCustomer().getFullName()) : "Quý khách";
        String petName = booking.getPet() != null ? escapeHtml(booking.getPet().getName()) : "N/A";
        String petSpecies = booking.getPet() != null ? escapeHtml(defaultText(booking.getPet().getSpecies())) : "Chưa có";
        String serviceName = booking.getService() != null ? escapeHtml(booking.getService().getServiceName()) : "N/A";
        String bookingDate = booking.getBookingDate() != null
                ? booking.getBookingDate().toLocalDate().format(DATE_FORMAT)
                : "N/A";
        String bookingTime = booking.getBookingTime() != null
                ? booking.getBookingTime().format(TIME_FORMAT)
                : "N/A";
        String employeeName = booking.getEmployee() != null
                ? escapeHtml(booking.getEmployee().getFullName())
                : "Nhân viên PetShop";
        String totalPrice = escapeHtml(formatCurrency(booking.getTotalPrice()));

        return """
                <!doctype html>
                <html lang="vi">
                <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,'Segoe UI',sans-serif;color:#1f2937;">
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0;">
                    Lịch hẹn #%d của bạn tại PetShop đã hoàn thành. Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi.
                  </div>
                  <div style="max-width:680px;margin:0 auto;padding:28px 16px;">
                    <div style="background:linear-gradient(135deg,#7c3aed,#2563eb);border-radius:28px;padding:28px 24px;color:#ffffff;box-shadow:0 20px 44px rgba(79,70,229,0.18);">
                      <div style="font-size:12px;letter-spacing:1.8px;text-transform:uppercase;opacity:0.82;">PetShop Admin</div>
                      <h1 style="margin:10px 0 8px;font-size:30px;line-height:1.2;">Lịch hẹn của bạn đã hoàn thành</h1>
                      <p style="margin:0;font-size:15px;line-height:1.7;opacity:0.96;">
                        Xin chào <strong>%s</strong>, PetShop đã hoàn tất lịch hẹn của bạn. Cảm ơn bạn đã tin tưởng sử dụng dịch vụ của chúng tôi.
                      </p>
                    </div>

                    <div style="background:#ffffff;margin-top:-18px;border-radius:28px;padding:24px 22px;box-shadow:0 18px 38px rgba(15,23,42,0.08);border:1px solid #e5e7eb;">
                      <div style="display:inline-block;background:#eef2ff;color:#4338ca;padding:8px 14px;border-radius:999px;font-size:13px;font-weight:700;">
                        Mã lịch hẹn #%d
                      </div>

                      <table role="presentation" style="width:100%%;border-collapse:separate;border-spacing:12px;margin-top:18px;">
                        <tr>
                          <td style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:18px;padding:16px;vertical-align:top;width:50%%;">
                            <div style="font-size:12px;text-transform:uppercase;letter-spacing:1.2px;color:#64748b;margin-bottom:8px;">Ngày hẹn</div>
                            <div style="font-size:24px;font-weight:700;color:#0f172a;line-height:1.2;">%s</div>
                          </td>
                          <td style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:18px;padding:16px;vertical-align:top;width:50%%;">
                            <div style="font-size:12px;text-transform:uppercase;letter-spacing:1.2px;color:#64748b;margin-bottom:8px;">Giờ hẹn</div>
                            <div style="font-size:24px;font-weight:700;color:#0f172a;line-height:1.2;">%s</div>
                          </td>
                        </tr>
                      </table>

                      <div style="margin-top:10px;border:1px solid #e2e8f0;border-radius:20px;overflow:hidden;">
                        <div style="padding:14px 16px;background:#f8fafc;font-size:13px;font-weight:700;color:#334155;text-transform:uppercase;letter-spacing:1px;">
                          Chi tiết lịch hẹn
                        </div>
                        <table role="presentation" style="width:100%%;border-collapse:collapse;font-size:15px;">
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;">Dịch vụ</td>
                            <td style="padding:14px 16px;color:#0f172a;border-top:1px solid #e2e8f0;font-weight:600;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;">Thú cưng</td>
                            <td style="padding:14px 16px;color:#0f172a;border-top:1px solid #e2e8f0;">%s (%s)</td>
                          </tr>
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;">Nhân viên phụ trách</td>
                            <td style="padding:14px 16px;color:#0f172a;border-top:1px solid #e2e8f0;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;">Chi phí</td>
                            <td style="padding:14px 16px;color:#0f172a;border-top:1px solid #e2e8f0;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:14px 16px;color:#64748b;width:38%%;border-top:1px solid #e2e8f0;">Trạng thái</td>
                            <td style="padding:14px 16px;border-top:1px solid #e2e8f0;">
                              <span style="display:inline-block;background:#ecfdf5;color:#047857;padding:7px 12px;border-radius:999px;font-weight:700;font-size:13px;">Hoàn thành</span>
                            </td>
                          </tr>
                        </table>
                      </div>

                      <div style="margin-top:18px;padding:16px 18px;background:#f5f3ff;border:1px solid #ddd6fe;border-radius:18px;color:#5b21b6;font-size:14px;line-height:1.7;">
                        Cảm ơn bạn đã sử dụng dịch vụ tại PetShop. Nếu cần hỗ trợ thêm hoặc muốn đặt lịch tiếp theo, PetShop luôn sẵn sàng đồng hành cùng bạn.
                      </div>
                    </div>

                    <p style="margin:16px 4px 0;color:#64748b;font-size:12px;line-height:1.7;text-align:center;">
                      Email này được gửi tự động từ hệ thống PetShop. Vui lòng không trả lời trực tiếp email này.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(
                booking.getId(),
                customerName,
                booking.getId(),
                escapeHtml(bookingDate),
                escapeHtml(bookingTime),
                serviceName,
                petName,
                petSpecies,
                employeeName,
                totalPrice
        );
    }

    private String buildOrderItemRows(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return """
                    <tr>
                      <td style="padding:14px 16px;color:#64748b;border-top:1px solid #e2e8f0;">Không có sản phẩm trong đơn.</td>
                    </tr>
                    """;
        }

        return order.getOrderDetails().stream()
                .map(item -> """
                        <tr>
                          <td style="padding:14px 16px;color:#0f172a;border-top:1px solid #e2e8f0;">
                            <div style="font-weight:600;">%s</div>
                            <div style="margin-top:4px;color:#64748b;font-size:13px;">Số lượng: %d x %s</div>
                          </td>
                          <td style="padding:14px 16px;color:#0f172a;border-top:1px solid #e2e8f0;text-align:right;font-weight:700;white-space:nowrap;">%s</td>
                        </tr>
                        """.formatted(
                        escapeHtml(getProductName(item)),
                        item.getQuantity(),
                        escapeHtml(formatCurrency(item.getUnitPrice())),
                        escapeHtml(formatCurrency(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))
                ))
                .collect(Collectors.joining());
    }

    private String formatOrderItemLine(OrderDetail item) {
        return "- " + getProductName(item)
                + " | SL: " + item.getQuantity()
                + " | Tạm tính: " + formatCurrency(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    private String getProductName(OrderDetail item) {
        if (item.getProduct() != null && StringUtils.hasText(item.getProduct().getProductName())) {
            return item.getProduct().getProductName();
        }
        return "Sản phẩm";
    }

    private String mapStatusLabel(String status) {
        return switch (status) {
            case "Pending" -> "Chờ xác nhận";
            case "Confirmed" -> "Đã xác nhận";
            case "InProgress" -> "Đang thực hiện";
            case "Completed" -> "Hoàn thành";
            case "Cancelled" -> "Đã hủy";
            case "NoShow" -> "Không đến";
            default -> defaultText(status);
        };
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "Chưa có";
        }
        return NumberFormat.getCurrencyInstance(VIETNAM).format(amount);
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : "Không có";
    }

    private String escapeHtml(String value) {
        return HtmlUtils.htmlEscape(defaultText(value));
    }
}
