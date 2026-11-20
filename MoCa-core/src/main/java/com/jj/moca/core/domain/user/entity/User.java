package com.jj.moca.core.domain.user.entity;

import com.jj.moca.core.common.base.BaseUpdateEntity;
import com.jj.moca.core.common.util.EmailFormatter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUpdateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    public User(String email, String name, String password) {
        this.email = EmailFormatter.normalize(email);
        this.name = name.trim();
        this.password = password;
        this.role = UserRole.USER;
    }
}
