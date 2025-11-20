package org.unilab.improfessorbe.domain.problem.infrastructure.external.ai;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.unilab.improfessorbe.domain.problem.dto.ProblemResponse;
import org.unilab.improfessorbe.domain.problem.service.output.ProblemTextParser;
import org.unilab.improfessorbe.global.exception.CustomException;
import org.unilab.improfessorbe.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

	private final RestTemplate restTemplate;
	private final ProblemTextParser problemTextParser;

	@Value("${ai.onpremise.url}")
	private String aiServerUrl;

	@Value("${ai.onpremise.timeout}")
	private int timeout;

	public List<ProblemResponse> aiPipeLineService(List<MultipartFile> conceptFiles, List<MultipartFile> formatFiles) {
		try {
			String url = aiServerUrl + "/problems";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);

			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

			for (MultipartFile file : conceptFiles) {
				ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
					@Override
					public String getFilename() {
						return file.getOriginalFilename();
					}
				};
				body.add("file", resource);
			}

			if (formatFiles != null) {
				for (MultipartFile file : formatFiles) {
					ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
						@Override
						public String getFilename() {
							return file.getOriginalFilename();
						}
					};
					body.add("reference_file", resource);
				}
			}

			HttpEntity<MultiValueMap<String, Object>> requestEntity =
				new HttpEntity<>(body, headers);

			ResponseEntity<AiResponse> response = restTemplate.exchange(
				url,
				HttpMethod.POST,
				requestEntity,
				AiResponse.class
			);

			if (response.getBody() == null) {
				log.error("Empty response from AI server");
				throw new CustomException(ErrorCode.AI_SERVER_ERROR);
			}

			log.info("Successfully received response from AI server");

			String problemText = response.getBody().getResult();
			List<ProblemResponse> responses = problemTextParser.parseProblemText(problemText);

			return responses;

		} catch (Exception e) {
			log.error("Failed to communicate with AI server: {}", e.getMessage(), e);
			throw new CustomException(ErrorCode.AI_SERVER_CONNECTION_FAILED);
		}
	}
}