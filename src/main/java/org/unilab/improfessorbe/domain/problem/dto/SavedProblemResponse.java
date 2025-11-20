package org.unilab.improfessorbe.domain.problem.dto;

import java.time.LocalDateTime;

import org.unilab.improfessorbe.domain.problem.domain.Problem;
import org.unilab.improfessorbe.domain.round.domain.Round;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SavedProblemResponse {
	private Long problemId;
	private String roundName;
	private String type;
	private String content;
	private String description;
	private String answer;
	private LocalDateTime savedAt;

	public static SavedProblemResponse of(Problem problem, Round round) {
		String roundName = (round != null) ? round.getName() : "Unknown";

		return SavedProblemResponse.builder()
			.problemId(problem.getId())
			.roundName(roundName)
			.type(problem.getType())
			.content(problem.getContent())
			.description(problem.getDescription())
			.answer(problem.getAnswer())
			.savedAt(problem.getSavedAt())
			.build();
	}
}