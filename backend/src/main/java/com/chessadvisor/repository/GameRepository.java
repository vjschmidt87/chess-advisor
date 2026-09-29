package com.chessadvisor.repository;

import com.chessadvisor.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Game> findByIdAndUserId(Long id, Long userId);
}
