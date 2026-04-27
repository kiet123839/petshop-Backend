package com.petshop.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "Users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Integer id;

    @Column(name = "Username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "Email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "PasswordHash", nullable = false)
    private String password;

    @Column(name = "FullName", nullable = false, length = 200)
    private String fullName;

    @Column(name = "Role", nullable = false, length = 20)
    @Builder.Default
    private String role = "Customer";

    @Column(name = "Status", nullable = false, length = 20)
    @Builder.Default
    private String status = "Active";

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}