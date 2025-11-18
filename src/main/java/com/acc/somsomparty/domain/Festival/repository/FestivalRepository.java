package com.acc.somsomparty.domain.Festival.repository;

import com.acc.somsomparty.domain.Festival.entity.Festival;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FestivalRepository extends JpaRepository<Festival, Long> {
    Page<Festival> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT f FROM Festival f " +
            "WHERE (f.createdAt < :lastCreatedAt) OR " +
            "      (f.createdAt = :lastCreatedAt AND f.id < :lastId) " +
            "ORDER BY f.createdAt DESC, f.id DESC")
    List<Festival> findNextPage(
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            @Param("lastId") Long lastId,
            Pageable pageable
    );

    @Query("SELECT f FROM Festival f WHERE (f.nameLower LIKE CONCAT('%', :keyword, '%') " +
            "OR f.descriptionLower LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:lastId = 0 OR f.id < :lastId) " +
            "ORDER BY f.id DESC")
    Page<Festival> searchByKeyword(Long lastId, String keyword, Pageable pageable);
}
