package org.unilab.improfessorbe.domain.round.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.unilab.improfessorbe.domain.round.domain.Round;

public interface RoundRepository extends JpaRepository<Round, Long> {

	// 특정 유저의 삭제 안된 회차 조회
	List<Round> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

	// 만료된 회차 조회 (7일 이전 생성 + 삭제 안됨)
	@Query("SELECT r FROM Round r " +
		"WHERE r.createdAt < :weekAgo " +
		"AND r.deletedAt IS NULL")
	List<Round> findExpiredRounds(@Param("weekAgo") LocalDateTime weekAgo);
}