package org.unilab.improfessorbe.domain.problem.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProblemGenerationResponse {
	private String fileName;
	private List<ProblemResponse> problems;
	private int problemCount;
	private String message;

	public static ProblemGenerationResponse of(String fileName, List<ProblemResponse> problems) {
		return ProblemGenerationResponse.builder()
			.fileName(fileName)
			.problems(problems)
			.problemCount(problems.size())
			.message("문제가 성공적으로 생성되었습니다.")
			.build();
	}
}