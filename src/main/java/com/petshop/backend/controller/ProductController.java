package com.petshop.backend.controller;

import com.petshop.backend.dto.ProductRequest;
import com.petshop.backend.dto.ProductResponse;
import com.petshop.backend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // GET /api/products              → tất cả sản phẩm
    // GET /api/products?active=true  → chỉ sản phẩm đang bán
    // GET /api/products?keyword=xxx  → tìm theo tên
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(required = false, defaultValue = "false") boolean active,
            @RequestParam(required = false) String keyword) {

        if (keyword != null && !keyword.isBlank()) {
            return ResponseEntity.ok(productService.searchByName(keyword));
        }
        if (active) {
            return ResponseEntity.ok(productService.getActiveProducts());
        }
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // GET /api/products/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // POST /api/products
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        return new ResponseEntity<>(productService.createProduct(request), HttpStatus.CREATED);
    }

    // PUT /api/products/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    // DELETE /api/products/{id} → soft delete (isActive = false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deactivateProduct(@PathVariable Long id) {
        productService.deactivateProduct(id);
        return ResponseEntity.ok(Map.of("message", "Đã ngừng kinh doanh sản phẩm id: " + id));
    }
}
