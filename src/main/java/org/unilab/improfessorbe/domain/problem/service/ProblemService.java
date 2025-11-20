package org.unilab.improfessorbe.domain.problem.service;

import static java.util.stream.Collectors.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.unilab.improfessorbe.domain.problem.domain.Problem;
import org.unilab.improfessorbe.domain.problem.dto.ConceptExtractionResult;
import org.unilab.improfessorbe.domain.problem.dto.ProblemGenerationResponse;
import org.unilab.improfessorbe.domain.problem.dto.ProblemResponse;
import org.unilab.improfessorbe.domain.problem.dto.RoundProblemResponse;
import org.unilab.improfessorbe.domain.problem.dto.SavedProblemResponse;
import org.unilab.improfessorbe.domain.problem.infrastructure.external.ai.AiService;
import org.unilab.improfessorbe.domain.problem.infrastructure.external.gemini.GeminiApiClient;
import org.unilab.improfessorbe.domain.problem.infrastructure.repository.ProblemRepository;
import org.unilab.improfessorbe.domain.problem.service.input.ConceptExtractorService;
import org.unilab.improfessorbe.domain.problem.service.input.FileParseService;
import org.unilab.improfessorbe.domain.problem.service.output.ProblemTextParser;
import org.unilab.improfessorbe.domain.round.domain.Round;
import org.unilab.improfessorbe.domain.round.service.RoundService;
import org.unilab.improfessorbe.domain.user.service.UserService;
import org.unilab.improfessorbe.global.exception.CustomException;
import org.unilab.improfessorbe.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProblemService {

	private final FileParseService fileParseService;
	private final GeminiApiClient geminiApiClient;
	private final ConceptExtractorService conceptExtractorService;
	private final ProblemTextParser problemTextParser;
	private final UserService userService;
	private final AiService aiService;
	private final RoundService roundService;
	private final ProblemRepository problemRepository;

	public ProblemGenerationResponse createProblem(Long userId, List<MultipartFile> conceptFiles,
		List<MultipartFile> formatFiles) {
		try {
			// 1. 문제 생성
			List<ProblemResponse> responses = createProblemWithMl(conceptFiles, formatFiles);

			// 2. 저장
			String roundName = conceptFiles.get(0).getOriginalFilename() + '_' + LocalDateTime.now()
				.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

			// 3. 회차 생성
			Round round = Round.create(userId, roundName);
			roundService.save(round);

			// 4. 문제들을 DB에 저장
			List<Problem> problems = responses.stream()
				.map(response -> Problem.create(
					round.getId(),
					response.getType(),
					response.getContent(),
					response.getDescription(),
					response.getAnswer()
				))
				.collect(toList());
			problemRepository.saveAll(problems);

			// 5. 저장된 Problem을 ProblemResponse로 변환
			List<ProblemResponse> problemResponses = problems.stream()
				.map(ProblemResponse::from)
				.collect(toList());

			//유저 문제 생성 카운트 감소
			userService.decrementFreeCount(userId);

			return ProblemGenerationResponse.of(roundName, problemResponses);

		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error("문제 생성 및 캐시 저장 중 에러", e);
			throw new CustomException(ErrorCode.PROBLEM_CREATION_FAILED);
		}
	}

	public ProblemGenerationResponse createProblemWithAiPipeLine(Long userId, List<MultipartFile> conceptFiles,
		List<MultipartFile> formatFiles) {
		try {
			List<ProblemResponse> responses = aiService.aiPipeLineService(conceptFiles, formatFiles);

			String roundName = conceptFiles.get(0).getOriginalFilename() + '_' + LocalDateTime.now()
				.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

			Round round = Round.create(userId, roundName);
			roundService.save(round);

			List<Problem> problems = responses.stream()
				.map(response -> Problem.create(
					round.getId(),
					response.getType(),
					response.getContent(),
					response.getDescription(),
					response.getAnswer()
				))
				.collect(toList());
			problemRepository.saveAll(problems);

			List<ProblemResponse> problemResponses = problems.stream()
				.map(ProblemResponse::from)
				.collect(toList());

			userService.decrementFreeCount(userId);

			return ProblemGenerationResponse.of(roundName, problemResponses);

		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error("문제 생성 및 캐시 저장 중 에러", e);
			throw new CustomException(ErrorCode.PROBLEM_CREATION_FAILED);
		}
	}

	public List<ProblemResponse> createProblemWithMl(List<MultipartFile> conceptFiles,
		List<MultipartFile> formatFiles) {
		try {
			String conceptContent = fileParseService.parseFileList(conceptFiles, "개념");
			String formatContent = "";
			if (formatFiles != null) {
				formatContent = fileParseService.parseFileList(formatFiles, "형식");
			}

			log.info("개념 파일 글자수: {}개 / 형식 파일 글자수: {}개",
				conceptContent.length(), formatContent.length());

			ConceptExtractionResult result = conceptExtractorService.extractConcepts(conceptContent);
			String conceptExtraction = result.toFormattedString();

			String problemText = geminiApiClient.generateProblems(conceptExtraction, formatContent);
			List<ProblemResponse> responses = problemTextParser.parseProblemText(problemText);

			return responses;

		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw new CustomException(ErrorCode.PROBLEM_CREATION_FAILED);
		}
	}

	@Transactional(readOnly = true)
	public List<RoundProblemResponse> getProblemsByRoundId(Long roundId) {
		List<Problem> problems = problemRepository
			.findByRoundIdAndDeletedAtIsNullOrderByCreatedAtAsc(roundId);

		return problems.stream()
			.map(RoundProblemResponse::from)
			.collect(toList());
	}

	@Transactional(readOnly = true)
	public List<SavedProblemResponse> getSavedProblems(Long userId) {
		List<Problem> problems = problemRepository.findSavedProblemsByUserId(userId);

		List<Long> roundIds = problems.stream()
			.map(Problem::getRoundId)
			.distinct()
			.collect(toList());

		Map<Long, Round> roundMap = roundService.findRoundsByIds(roundIds);

		return problems.stream()
			.map(problem -> {
				Round round = roundMap.get(problem.getRoundId());
				return SavedProblemResponse.of(problem, round);
			})
			.collect(toList());
	}

	@Transactional
	public void toggleProblemSave(Long problemId) {
		Problem problem = problemRepository.findById(problemId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROBLEM_NOT_FOUND));

		if (problem.isSaved()) {
			problem.unsave();
			log.info("문제 저장 취소: ID={}", problemId);
		} else {
			problem.save();
			log.info("문제 저장: ID={}", problemId);
		}
	}

}