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

	// 추가: 생성률 정보
	private Integer requestedCount;    // 요청한 문제 수
	private Double successRate;        // 생성 성공률 (%)

	public static ProblemGenerationResponse of(String fileName, List<ProblemResponse> problems) {
		int requested = 5;  // Gemini 프롬프트에서 5개 요청
		int actual = problems.size();
		double rate = requested > 0 ? (double) actual / requested * 100 : 0.0;

		return ProblemGenerationResponse.builder()
			.fileName(fileName)
			.problems(problems)
			.problemCount(actual)
			.requestedCount(requested)
			.successRate(rate)
			.message(String.format("문제 %d개가 성공적으로 생성되었습니다. (성공률: %.1f%%)", actual, rate))
			.build();
	}
}