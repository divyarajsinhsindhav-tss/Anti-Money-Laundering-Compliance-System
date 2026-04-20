package org.tss.tm.entity.system;

import jakarta.persistence.*;
import lombok.*;
import org.tss.tm.entity.common.BaseEntity;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "system_admin", schema = "public")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemAdmin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "system_admin_id")
    @ToString.Include
    private UUID systemAdminId;

    @Column(name = "system_admin_code", unique = true, nullable = false)
    @ToString.Include
    private String systemAdminCode;

    @Column(name = "first_name", nullable = false)
    @ToString.Include
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name", nullable = false)
    @ToString.Include
    private String lastName;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "email", unique = true, nullable = false)
    @ToString.Include
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}