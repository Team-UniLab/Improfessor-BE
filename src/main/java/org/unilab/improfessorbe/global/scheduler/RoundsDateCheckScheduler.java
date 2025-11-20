package org.unilab.improfessorbe.global.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.unilab.improfessorbe.domain.problem.domain.Problem;
import org.unilab.improfessorbe.domain.problem.infrastructure.repository.ProblemRepository;
import org.unilab.improfessorbe.domain.round.domain.Round;
import org.unilab.improfessorbe.domain.round.infrastructure.repository.RoundRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoundsDateCheckScheduler {

	private final RoundRepository roundRepository;
	private final ProblemRepository problemRepository;

	// 매일 새벽 3시에 실행
	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	public void cleanupExpiredData() {
		LocalDateTime weekAgo = LocalDateTime.now().minusDays(3); //유효기간 3일

		// 1. 만료된 회차 소프트 삭제
		List<Round> expiredRounds = roundRepository.findExpiredRounds(weekAgo);
		expiredRounds.forEach(Round::markAsDeleted);
		roundRepository.saveAll(expiredRounds);
		log.info("삭제된 회차 수: {}", expiredRounds.size());

		// 2. 삭제된 회차의 저장 안된 문제 소프트 삭제
		List<Problem> unsavedProblems = problemRepository.findUnsavedProblemsInDeletedRounds();
		unsavedProblems.forEach(Problem::markAsDeleted);
		problemRepository.saveAll(unsavedProblems);
		log.info("삭제된 문제 수: {}", unsavedProblems.size());
	}
}