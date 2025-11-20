package org.unilab.improfessorbe.domain.problem.infrastructure.external.gemini;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.unilab.improfessorbe.global.exception.CustomException;
import org.unilab.improfessorbe.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

	private final WebClient geminiWebClient;
	private final GeminiRateLimitManager rateLimitManager;

	@Value("${gemini.api.key}")
	private String apiKey;

	@Value("${gemini.api.timeout}")
	private int timeout;

	private static final String PROBLEM_GENERATION_PROMPT = """ 
		다음을 참고해서 XML 형식으로 대학교 시험 문제 5개를 생성하세요.
		    
		**응답 형식 (반드시 준수):**
		<problems>
		    <problem>
		        <number>1</number>
		        <type>객관식 or 단답형 or 주관식</type>
		        <content>문제내용</content>
		        <description>풀이과정</description>
		        <answer>답</answer>
		    </problem>
		    <problem>
		        <number>2</number>
		        <type>객관식 or 주관식 or 서술형</type>
		        <content>문제내용</content>
		        <description>풀이과정</description>
		        <answer>답</answer>
		    </problem>
		</problems>
		    
		**절대 규칙 (위반 시 응답 무효):**
		1. XML 태그 구조만 출력 (다른 텍스트, 설명, 주석 없이)
		2. 모든 내용은 CDATA 섹션 없이 일반 텍스트로 작성
		3. 특수문자는 XML 엔티티로 작성해줘 (&lt;, &gt;, &amp;, &quot;, &apos;)
		    
		**문제 생성 규칙:**
		- 문제 내용은 중요한 개념 텍스트를 참고해서 작성
		- 문제 스타일은 문제 형식을 참고해서 작성
		- (객관식, 주관식, 단답식) 비율을 문제 형식의 비율과 맞추고, 문제 형식 텍스트가 존재하지 않으면 2:2:1 비율로 생성
		- 객관식 생성 시 기호는 ①, ②, ③, ④, ⑤ 형식 사용
		- 비슷한 개념을 포함한 문제 중복 생성 금지
		- 풀이과정과 답을 구체적으로 작성
		- 문제 내용에 따옴표, 수식, 특수문자 자유롭게 사용 가능
		    
		- 중요한 개념: %s
		- 문제 형식: %s
		""";

	public String generateProblems(String conceptText, String formatText) {
		String prompt = String.format(PROBLEM_GENERATION_PROMPT, conceptText, formatText);
		return callGemini(prompt);
	}

	private String callGemini(String prompt) {

		rateLimitManager.checkRateLimit();

		GeminiDto.Request request = GeminiDto.Request.builder()
			.contents(List.of(
				GeminiDto.Request.Content.builder()
					.parts(List.of(
						GeminiDto.Request.Content.Part.builder()
							.text(prompt)
							.build()
					))
					.build()
			))
			.build();

		try {
			GeminiDto.Response geminiResponse = geminiWebClient.post()
				.uri("/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.bodyValue(request)
				.retrieve()
				.bodyToMono(GeminiDto.Response.class)
				.timeout(Duration.ofSeconds(timeout))
				.block();

			return extractResponseText(geminiResponse);

		} catch (WebClientResponseException.TooManyRequests e) {
			log.error("Gemini API 요청 제한 초과 (429): {}", e.getResponseBodyAsString());
			throw new CustomException(ErrorCode.GEMINI_RATE_LIMIT_EXCEEDED);

		} catch (WebClientResponseException.ServiceUnavailable e) {
			log.error("Gemini API 서비스 이용 불가 (503): {}", e.getResponseBodyAsString());
			throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);

		} catch (WebClientResponseException e) {
			log.error("Gemini API HTTP 에러: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);

		} catch (Exception e) {
			log.error("Gemini API 호출 중 예상치 못한 에러", e);
			throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
		}
	}

	private String extractResponseText(GeminiDto.Response response) {
		try {
			if (response == null) {
				log.warn("Gemini API 응답이 null입니다.");
				throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
			}

			logTokenUsage(response);

			if (response.getCandidates() == null || response.getCandidates().isEmpty()) {
				log.warn("Gemini API 응답에 candidates가 없습니다.");
				throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
			}

			String responseText = response.getCandidates().get(0)
				.getContent()
				.getParts().get(0)
				.getText();

			if (responseText == null || responseText.trim().isEmpty()) {
				log.warn("Gemini API 응답 텍스트가 비어있습니다.");
				throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
			}

			log.info("gpt 문제 생성 가공 전 텍스트: " + responseText);
			return responseText;

		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error("Gemini 응답 파싱 중 에러 발생", e);
			throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
		}
	}

	private void logTokenUsage(GeminiDto.Response response) {
		try {
			if (response.getUsageMetadata() != null) {
				GeminiDto.Response.UsageMetadata usage = response.getUsageMetadata();

				log.info("🔍 Gemini API 토큰 사용량 - 입력: {}개, 출력: {}개, 총합: {}개",
					usage.getPromptTokenCount(),
					usage.getCandidatesTokenCount(),
					usage.getTotalTokenCount());

			} else {
				log.warn("⚠️ Gemini API 응답에 토큰 사용량 정보가 없습니다.");
			}
		} catch (Exception e) {
			log.warn("토큰 사용량 로깅 중 에러 발생 (무시): {}", e.getMessage());
		}
	}

	/**
	 * 현재 토큰 버켓 상태 조회 (모니터링/디버깅 용도)
	 */
	public GeminiRateLimitManager.TokenBucketStatus getRateLimitStatus() {
		return rateLimitManager.getTokenBucketStatus();
	}
}