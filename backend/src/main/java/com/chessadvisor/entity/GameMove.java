package com.chessadvisor.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_moves")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GameMove {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "move_number", nullable = false)
    private int moveNumber;

    @Column(name = "white_move")
    private String whiteMove;

    @Column(name = "black_move")
    private String blackMove;

    @Column(columnDefinition = "TEXT")
    private String fen;

    private String evaluation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameMove gameMove)) return false;
        return id != null && id.equals(gameMove.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
