package com.petshop.backend.service;

import com.petshop.backend.dto.PaymentRequest;
import com.petshop.backend.dto.PaymentResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.SqlParameter;          // ✅ thêm dòng này
import org.springframework.jdbc.core.SqlOutParameter;       // ✅ thêm dòng này
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.Map;
import java.sql.Types;  
@Service
public class PaymentService {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcCall processPaymentCall;

    public PaymentService(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
     // ✅ Sửa thành:
        this.processPaymentCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("sp_ProcessPayment")
                .declareParameters(
                    new SqlParameter("OrderID",        Types.INTEGER),
                    new SqlParameter("Amount",         Types.DECIMAL),
                    new SqlParameter("PaymentMethod",  Types.NVARCHAR),
                    new SqlParameter("TransactionRef", Types.NVARCHAR),
                    new SqlParameter("Notes",          Types.NVARCHAR),
                    new SqlOutParameter("NewPaymentID", Types.INTEGER)  // OUTPUT
                );
    }

    public PaymentResponse processPayment(PaymentRequest request) {
    	Map<String, Object> params = new HashMap<>();
    	params.put("OrderID",        request.getOrderId());
    	params.put("Amount",         request.getAmount());
    	params.put("PaymentMethod",  request.getPaymentMethod());
    	params.put("TransactionRef", request.getTransactionRef());
    	params.put("Notes",          request.getNotes());

    	Map<String, Object> result = processPaymentCall.execute(params);

        Integer paymentId = null;
        if (result.get("NewPaymentID") != null) {
            paymentId = ((Number) result.get("NewPaymentID")).intValue();
        }

        return new PaymentResponse("Completed", "Thanh toan thanh cong", paymentId);
    }
}
