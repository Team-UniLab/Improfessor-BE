package org.unilab.improfessorbe.domain.round.dto;

import java.time.LocalDateTime;

import org.unilab.improfessorbe.domain.round.domain.Round;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoundResponse {
	private Long roundId;
	private String roundName;
	private LocalDateTime createdAt;

	public static RoundResponse from(Round round) {
		return RoundResponse.builder()
			.roundId(round.getId())
			.roundName(round.getName())
			.createdAt(round.getCreatedAt())
			.build();
	}
}