package org.unilab.improfessorbe.domain.problem.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.unilab.improfessorbe.domain.problem.domain.Problem;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

	// 특정 회차의 모든 문제 조회 (저장 여부 상관없이)
	List<Problem> findByRoundIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long roundId);

	// 특정 유저의 저장된 문제만 조회
	@Query(value =
		"SELECT p.* FROM problem p " +
			"JOIN round r ON p.round_id = r.id " +
			"WHERE r.user_id = :userId " +
			"AND p.saved_at IS NOT NULL " +
			"AND p.deleted_at IS NULL " +
			"ORDER BY p.saved_at DESC",
		nativeQuery = true)
	List<Problem> findSavedProblemsByUserId(@Param("userId") Long userId);

	//소프트 삭제된 라운드에 속하며, savedAt이 NULL인 (저장되지 않은) 문제들을 조회합니다.
	@Query("SELECT p FROM Problem p " +
		"JOIN Round r ON p.roundId = r.id " +
		"WHERE p.savedAt IS NULL " +
		"AND r.deletedAt IS NOT NULL " +
		"AND p.deletedAt IS NULL")
	List<Problem> findUnsavedProblemsInDeletedRounds();
}