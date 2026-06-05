package com.petshop.backend.service;

import com.petshop.backend.dto.ProductRequest;
import com.petshop.backend.dto.ProductResponse;
import com.petshop.backend.model.Product;
import com.petshop.backend.repository.ProductRepository;


import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.time.LocalDateTime;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }


    /**
     * Lấy tất cả sản phẩm
     */
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy sản phẩm đang hoạt động
     */
    public List<ProductResponse> getActiveProducts() {
        return productRepository.findByIsActiveTrue()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy sản phẩm theo ID
     */
    public ProductResponse getProductById(Long id) {

        Product p = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy sản phẩm id: " + id
                        )
                );

        return toResponse(p);
    }

    /**
     * Tìm kiếm theo tên
     */
    public List<ProductResponse> searchByName(String keyword) {

        return productRepository
                .findByProductNameContainingIgnoreCase(keyword)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tạo sản phẩm
     */
    public ProductResponse createProduct(ProductRequest request) {

        Product p = new Product();

        p.setCategoryId(request.getCategoryId());
        p.setProductName(request.getProductName());
        p.setDescription(request.getDescription());
        p.setPrice(request.getPrice());

        p.setStockQuantity(
                request.getStockQuantity() != null
                        ? request.getStockQuantity()
                        : 0
        );

        p.setUnit(
                request.getUnit() != null
                        ? request.getUnit()
                        : "cái"
        );

        p.setImageUrl(request.getImageUrl());

        p.setIsActive(
                request.getIsActive() != null
                        ? request.getIsActive()
                        : true
        );

        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());

        return toResponse(productRepository.save(p));
    }

    /**
     * Cập nhật sản phẩm
     */
    public ProductResponse updateProduct(
            Long id,
            ProductRequest request
    ) {

        Product p = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy sản phẩm id: " + id
                        )
                );

        if (request.getCategoryId() != null) {
            p.setCategoryId(request.getCategoryId());
        }

        if (request.getProductName() != null) {
            p.setProductName(request.getProductName());
        }

        if (request.getDescription() != null) {
            p.setDescription(request.getDescription());
        }

        if (request.getPrice() != null) {
            p.setPrice(request.getPrice());
        }

        if (request.getStockQuantity() != null) {
            p.setStockQuantity(request.getStockQuantity());
        }

        if (request.getUnit() != null) {
            p.setUnit(request.getUnit());
        }

        if (request.getImageUrl() != null) {
            p.setImageUrl(request.getImageUrl());
        }

        if (request.getIsActive() != null) {
            p.setIsActive(request.getIsActive());
        }

        p.setUpdatedAt(LocalDateTime.now());

        return toResponse(productRepository.save(p));
    }

    /**
     * Xóa mềm sản phẩm
     */
    public void deactivateProduct(Long id) {

        Product p = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy sản phẩm id: " + id
                        )
                );

        p.setIsActive(false);
        p.setUpdatedAt(LocalDateTime.now());

        productRepository.save(p);
    }

    /**
     * Upload ảnh sản phẩm
     */
    public ProductResponse uploadImage(
            Long id,
            MultipartFile file
    ) {

        Product p = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy sản phẩm id: " + id
                        )
                );

        String fileName =
                "product_" +
                id +
                "_" +
                System.currentTimeMillis() +
                getExtension(file.getOriginalFilename());

        Path uploadDir = Paths.get("uploads/products");

        try {

            Files.createDirectories(uploadDir);

            Files.copy(
                    file.getInputStream(),
                    uploadDir.resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Không thể lưu ảnh: " + e.getMessage()
            );
        }

        p.setImageUrl("/uploads/products/" + fileName);
        p.setUpdatedAt(LocalDateTime.now());

        return toResponse(productRepository.save(p));
    }

    /**
     * Xóa cứng sản phẩm
     */
    public void hardDeleteProduct(Long id) {

        Product p = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy sản phẩm id: " + id
                        )
                );

        productRepository.delete(p);
    }

    /**
     * Lấy extension file
     */
    private String getExtension(String filename) {

        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }

        return filename.substring(
                filename.lastIndexOf(".")
        );
    }

    /**
     * Entity -> DTO
     */
    private ProductResponse toResponse(Product p) {

        ProductResponse res = new ProductResponse();

        res.setId(p.getId());
        res.setCategoryId(p.getCategoryId());
        res.setProductName(p.getProductName());
        res.setDescription(p.getDescription());
        res.setPrice(p.getPrice());
        res.setStockQuantity(p.getStockQuantity());
        res.setUnit(p.getUnit());
        res.setImageUrl(p.getImageUrl());
        res.setIsActive(p.getIsActive());
        res.setCreatedAt(p.getCreatedAt());
        res.setUpdatedAt(p.getUpdatedAt());

        return res;
    }
}