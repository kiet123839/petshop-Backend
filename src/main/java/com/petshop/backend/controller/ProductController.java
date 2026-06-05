package com.petshop.backend.controller;

import com.petshop.backend.dto.ProductRequest;
import com.petshop.backend.dto.ProductResponse;
import com.petshop.backend.service.ProductService;

import jakarta.validation.Valid;


import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    public ProductController(ProductService productService) {
        this.productService = productService;
    }


    /**
     * GET ALL PRODUCTS
     * /api/products
     * /api/products?active=true
     * /api/products?keyword=abc
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(required = false, defaultValue = "false")
            boolean active,

            @RequestParam(required = false)
            String keyword
    ) {

        // Search by keyword
        if (keyword != null && !keyword.trim().isEmpty()) {
            return ResponseEntity.ok(
                    productService.searchByName(keyword)
            );
        }

        // Get active products only
        if (active) {
            return ResponseEntity.ok(
                    productService.getActiveProducts()
            );
        }

        // Get all products
        return ResponseEntity.ok(
                productService.getAllProducts()
        );
    }

    /**
     * GET PRODUCT BY ID
     * /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                productService.getProductById(id)
        );
    }

    /**
     * CREATE PRODUCT
     * POST /api/products
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request
    ) {

        ProductResponse response =
                productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * UPDATE PRODUCT
     * PUT /api/products/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {

        return ResponseEntity.ok(
                productService.updateProduct(id, request)
        );
    }

    /**
     * SOFT DELETE PRODUCT
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deactivateProduct(
            @PathVariable Long id
    ) {

        productService.deactivateProduct(id);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Đã ngừng kinh doanh sản phẩm id: " + id
                )
        );
    }

    /**
     * UPLOAD PRODUCT IMAGE
     * POST /api/products/{id}/image
     */
    @PostMapping(
            value = "/{id}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProductResponse> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {

        return ResponseEntity.ok(
                productService.uploadImage(id, file)
        );
    }

    /**
     * HARD DELETE PRODUCT
     * DELETE /api/products/{id}/hard
     */
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Map<String, String>> hardDeleteProduct(
            @PathVariable Long id
    ) {

        productService.hardDeleteProduct(id);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Đã xóa vĩnh viễn sản phẩm id: " + id
                )
        );
    }
}