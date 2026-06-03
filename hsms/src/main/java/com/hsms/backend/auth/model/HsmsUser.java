package com.hsms.backend.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "hsms_user")
public class HsmsUser {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hsms_user_id_gen")
    @SequenceGenerator(name = "hsms_user_id_gen", sequenceName = "hsms_user_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "phone_number", nullable = false, length = Integer.MAX_VALUE)
    private String phoneNumber;

    @NotNull
    @Column(name = "email", nullable = false, length = Integer.MAX_VALUE)
    private String email;

    @NotNull
    @Column(name = "login", nullable = false, length = Integer.MAX_VALUE)
    private String login;

    @NotNull
    @Column(name = "password", nullable = false, length = Integer.MAX_VALUE)
    private String password;

    @JsonIgnore
    @ManyToMany
    private Set<Role> roles = new LinkedHashSet<>();


}