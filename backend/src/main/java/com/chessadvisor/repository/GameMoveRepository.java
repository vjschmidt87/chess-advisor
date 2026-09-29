package com.chessadvisor.repository;

import com.chessadvisor.entity.GameMove;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameMoveRepository extends JpaRepository<GameMove, Long> {
    List<GameMove> findByGameIdOrderByMoveNumber(Long gameId);
    void deleteByGameId(Long gameId);
}
