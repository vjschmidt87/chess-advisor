package com.chessadvisor.repository;

import com.chessadvisor.entity.Opening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OpeningRepository extends JpaRepository<Opening, Long> {
    Optional<Opening> findByEco(String eco);
    Optional<Opening> findByMovesStartingWith(String moves);

    @Query("SELECT o FROM Opening o WHERE :moves LIKE CONCAT(o.moves, '%') ORDER BY LENGTH(o.moves) DESC")
    List<Opening> findBestMatchingOpening(@Param("moves") String moves);

    List<Opening> findByNameContainingIgnoreCase(String name);
}
