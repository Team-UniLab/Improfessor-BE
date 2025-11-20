package org.unilab.improfessorbe.domain.problem.dto;

import org.unilab.improfessorbe.domain.problem.domain.Problem;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoundProblemResponse {
	private Long problemId;
	private String type;
	private String content;
	private String description;
	private String answer;
	private Boolean isSaved;

	public static RoundProblemResponse from(Problem problem) {
		return RoundProblemResponse.builder()
			.problemId(problem.getId())
			.type(problem.getType())
			.content(problem.getContent())
			.description(problem.getDescription())
			.answer(problem.getAnswer())
			.isSaved(problem.isSaved())
			.build();
	}
}