package org.unilab.improfessorbe.domain.round.service;

import static java.util.stream.Collectors.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unilab.improfessorbe.domain.round.domain.Round;
import org.unilab.improfessorbe.domain.round.dto.RoundResponse;
import org.unilab.improfessorbe.domain.round.infrastructure.repository.RoundRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoundService {

	private final RoundRepository roundRepository;

	@Transactional
	public Round save(Round round) {
		return roundRepository.save(round);
	}

	public Map<Long, Round> findRoundsByIds(List<Long> roundIds) {
		return roundRepository.findAllById(roundIds).stream()
			.collect(Collectors.toMap(Round::getId, round -> round));
	}

	public List<RoundResponse> getUserRounds(Long userId) {
		List<Round> rounds = roundRepository
			.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

		return rounds.stream()
			.map(RoundResponse::from)
			.collect(toList());
	}

}