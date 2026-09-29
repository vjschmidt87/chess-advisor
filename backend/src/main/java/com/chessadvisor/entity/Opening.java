package com.chessadvisor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "openings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Opening {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String eco;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String moves;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Opening opening)) return false;
        return id != null && id.equals(opening.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
