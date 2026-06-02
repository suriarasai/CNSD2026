package com.stc.tickit.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Customer who books tickets. Named AppUser to avoid clashing with the
 * reserved SQL keyword "user".
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;
}
