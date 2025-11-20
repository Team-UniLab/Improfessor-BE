package org.unilab.improfessorbe.domain.problem.service.output;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.stereotype.Component;
import org.unilab.improfessorbe.domain.problem.dto.ProblemResponse;
import org.unilab.improfessorbe.global.exception.CustomException;
import org.unilab.improfessorbe.global.exception.ErrorCode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProblemTextParser {

	public List<ProblemResponse> parseProblemText(String problemText) {
		List<ProblemResponse> responses = new ArrayList<>();

		try {
			// 1. 기본 검증
			if (problemText == null || problemText.trim().isEmpty()) {
				log.error("파싱할 문제 텍스트가 비어있습니다.");
				throw new CustomException(ErrorCode.PROBLEM_TEXT_EMPTY);
			}

			// 2. 텍스트 전처리
			String cleanText = cleanProblemText(problemText);
			log.info("파싱할 문제 텍스트 길이: {}", cleanText.length());

			// 3. XML 파싱
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			// 문자열을 InputStream으로 변환
			ByteArrayInputStream input = new ByteArrayInputStream(cleanText.getBytes(StandardCharsets.UTF_8));
			Document document = builder.parse(input);

			// 4. problems 루트 요소 확인
			Element root = document.getDocumentElement();
			if (!"problems".equals(root.getNodeName())) {
				log.error("XML 루트 요소가 'problems'가 아닙니다: {}", root.getNodeName());
				throw new CustomException(ErrorCode.PROBLEM_TEXT_INVALID_FORMAT);
			}

			// 5. problem 요소들 파싱
			NodeList problemNodes = root.getElementsByTagName("problem");

			if (problemNodes.getLength() == 0) {
				log.warn("파싱된 문제가 없습니다.");
				throw new CustomException(ErrorCode.PROBLEM_TEXT_NO_PROBLEMS);
			}

			// 6. 각 문제 파싱
			for (int i = 0; i < problemNodes.getLength(); i++) {
				Element problemElement = (Element)problemNodes.item(i);
				ProblemResponse problemResponse = parseSingleProblem(problemElement, i + 1);
				responses.add(problemResponse);
			}

		} catch (CustomException e) {
			throw e;
		} catch (ParserConfigurationException | SAXException e) {
			log.error("XML 파싱 에러: {}", e.getMessage());
			log.info("응답 내용: {}", problemText);
			log.info("=== Gemini 응답 전체 내용 끝 ===");
			throw new CustomException(ErrorCode.PROBLEM_XML_PARSING_ERROR);
		} catch (IOException e) {
			log.error("XML 읽기 에러: {}", e.getMessage());
			throw new CustomException(ErrorCode.PROBLEM_XML_PARSING_ERROR);
		} catch (Exception e) {
			log.error("문제 텍스트 파싱 중 예상치 못한 에러", e);
			throw new CustomException(ErrorCode.PROBLEM_CREATION_FAILED);
		}

		log.info("총 {}개의 문제가 파싱되었습니다.", responses.size());
		return responses;
	}

	private String cleanProblemText(String problemText) {
		try {
			String cleanText = problemText.trim();

			// XML 코드 블록 제거
			if (cleanText.startsWith("```xml")) {
				cleanText = cleanText.substring(6).trim();
			}
			if (cleanText.endsWith("```")) {
				cleanText = cleanText.substring(0, cleanText.length() - 3).trim();
			}

			cleanText = cleanText.trim();

			// 전처리 후 빈 텍스트 체크
			if (cleanText.isEmpty()) {
				throw new CustomException(ErrorCode.PROBLEM_TEXT_EMPTY);
			}

			// XML 기본 구조 검증
			if (!cleanText.contains("<problems>") || !cleanText.contains("</problems>")) {
				log.error("XML에 필수 루트 요소 <problems>가 없습니다.");
				throw new CustomException(ErrorCode.PROBLEM_TEXT_INVALID_FORMAT);
			}

			return cleanText;

		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error("텍스트 전처리 중 에러 발생", e);
			throw new CustomException(ErrorCode.PROBLEM_TEXT_INVALID_FORMAT);
		}
	}

	private ProblemResponse parseSingleProblem(Element problemElement, int index) {
		try {
			// 필수 필드 추출
			String type = getElementText(problemElement, "type");
			String content = getElementText(problemElement, "content");
			String description = getElementText(problemElement, "description");
			String answer = getElementText(problemElement, "answer");

			// 필수 필드 존재 검증
			if (type == null || type.trim().isEmpty()) {
				log.error("문제 {}에서 필수 필드 'type' 누락 또는 비어있음", index);
				throw new CustomException(ErrorCode.PROBLEM_REQUIRED_FIELD_MISSING);
			}

			if (content == null || content.trim().isEmpty()) {
				log.error("문제 {}에서 필수 필드 'content' 누락 또는 비어있음", index);
				throw new CustomException(ErrorCode.PROBLEM_CONTENT_EMPTY);
			}

			if (description == null) {
				description = ""; // description은 빈 값 허용
			}

			if (answer == null || answer.trim().isEmpty()) {
				log.error("문제 {}에서 필수 필드 'answer' 누락 또는 비어있음", index);
				throw new CustomException(ErrorCode.PROBLEM_CONTENT_EMPTY);
			}

			return ProblemResponse.of(
				type.trim(),
				content.trim(),
				description.trim(),
				answer.trim()
			);

		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error("문제 {} 파싱 중 에러 발생", index, e);
			throw new CustomException(ErrorCode.PROBLEM_CREATION_FAILED);
		}
	}

	/**
	 * XML 요소에서 텍스트 내용을 추출하는 헬퍼 메서드
	 */
	private String getElementText(Element parent, String tagName) {
		NodeList nodeList = parent.getElementsByTagName(tagName);
		if (nodeList.getLength() == 0) {
			return null;
		}

		Node node = nodeList.item(0);
		if (node == null) {
			return null;
		}

		String textContent = node.getTextContent();
		return textContent != null ? textContent.trim() : null;
	}
}