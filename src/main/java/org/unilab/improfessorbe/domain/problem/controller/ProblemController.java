package org.unilab.improfessorbe.domain.problem.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.unilab.improfessorbe.domain.problem.dto.ProblemGenerationResponse;
import org.unilab.improfessorbe.domain.problem.dto.RoundProblemResponse;
import org.unilab.improfessorbe.domain.problem.dto.SavedProblemResponse;
import org.unilab.improfessorbe.domain.problem.service.ProblemService;
import org.unilab.improfessorbe.domain.user.service.UserService;
import org.unilab.improfessorbe.global.common.ApiResponse;
import org.unilab.improfessorbe.global.exception.CustomException;
import org.unilab.improfessorbe.global.exception.ErrorCode;
import org.unilab.improfessorbe.global.util.FileLogUtil;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Slf4j
public class ProblemController {

	private final ProblemService problemService;
	private final FileLogUtil fileLogUtil;
	private final UserService userService;

	/*@PostMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "문제 생성", description = "(사용중)개념추출, llm, 캐시 적용 모델")
	public ResponseEntity<ApiResponse<ProblemGenerationResponse>> createProblemWithMl(
		@PathVariable Long userId,
		@RequestParam("conceptFiles") List<MultipartFile> conceptFiles,
		@RequestParam(value = "formatFiles", required = false) List<MultipartFile> formatFiles) {

		fileLogUtil.logFileUploadInfo(conceptFiles, formatFiles);

		if (!fileLogUtil.isValidRequest(conceptFiles)) {
			throw new CustomException(ErrorCode.MISSING_REQUIRED_FIELD);
		}

		boolean canCreateProblem = userService.checkFreeCount(userId);
		if (!canCreateProblem) {
			throw new CustomException(ErrorCode.INSUFFICIENT_FREE_COUNT);
		}

		ProblemGenerationResponse result = problemService.createProblem(userId, conceptFiles, formatFiles);

		return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
	}*/

	@PostMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "문제 생성", description = "(사용중)개념추출, llm, 캐시 적용 모델")
	public ResponseEntity<ApiResponse<ProblemGenerationResponse>> createProblemWithAiPipeLine(
		@PathVariable Long userId,
		@RequestParam("conceptFiles") List<MultipartFile> conceptFiles,
		@RequestParam(value = "formatFiles", required = false) List<MultipartFile> formatFiles) {

		fileLogUtil.logFileUploadInfo(conceptFiles, formatFiles);

		if (!fileLogUtil.isValidRequest(conceptFiles)) {
			throw new CustomException(ErrorCode.MISSING_REQUIRED_FIELD);
		}

		boolean canCreateProblem = userService.checkFreeCount(userId);
		if (!canCreateProblem) {
			throw new CustomException(ErrorCode.INSUFFICIENT_FREE_COUNT);
		}

		ProblemGenerationResponse result = problemService.createProblemWithAiPipeLine(userId, conceptFiles,
			formatFiles);

		return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
	}

	@GetMapping("/rounds/{roundId}")
	@Operation(summary = "특정 회차의 문제 조회", description = "특정 회차에 속한 모든 문제를 조회합니다")
	public ResponseEntity<ApiResponse<List<RoundProblemResponse>>> getRoundProblems(
		@PathVariable Long roundId) {
		List<RoundProblemResponse> problems = problemService.getProblemsByRoundId(roundId);
		return ResponseEntity.ok(ApiResponse.success(problems));
	}

	@GetMapping("/saved/{userId}")
	@Operation(summary = "저장된 문제 조회")
	public ResponseEntity<ApiResponse<List<SavedProblemResponse>>> getSavedProblems(
		@PathVariable Long userId) {
		List<SavedProblemResponse> problems = problemService.getSavedProblems(userId);
		return ResponseEntity.ok(ApiResponse.success(problems));
	}

	@PostMapping("/toggle-save/{problemId}")
	@Operation(summary = "문제 저장/취소 토글")
	public ResponseEntity<ApiResponse<Void>> toggleSave(
		@PathVariable Long problemId) {
		problemService.toggleProblemSave(problemId);
		return ResponseEntity.ok(ApiResponse.success(null));
	}
}