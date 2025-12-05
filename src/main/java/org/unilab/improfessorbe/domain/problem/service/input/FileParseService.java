package org.unilab.improfessorbe.domain.problem.service.input;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.unilab.improfessorbe.domain.problem.dto.FileParseResult;
import org.unilab.improfessorbe.domain.problem.parser.FileParserManager;
import org.unilab.improfessorbe.domain.problem.validator.FileValidator;
import org.unilab.improfessorbe.global.exception.CustomException;
import org.unilab.improfessorbe.global.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileParseService {

	private final FileParserManager fileParserManager;
	private final FileValidator fileValidator;

	public FileParseService(FileParserManager fileParserManager,
		FileValidator fileValidationService) {
		this.fileParserManager = fileParserManager;
		this.fileValidator = fileValidationService;
	}

	public String parseFileList(List<MultipartFile> files, String fileType) throws IOException {
		if (files.isEmpty()) {
			return fileType + " 파일이 없습니다.";
		}

		StringBuilder content = new StringBuilder();

		for (int i = 0; i < files.size(); i++) {
			MultipartFile file = files.get(i);
			FileParseResult parseResult = parseFile(file);

			if (i > 0) {
				content.append("\n\n--- ").append(fileType).append(" 파일 구분 ---\n\n");
			}
			content.append(parseResult.getContent());
		}

		return content.toString();
	}

	public FileParseResult parseFile(MultipartFile file) {
		try {
			fileValidator.validateFile(file);

			String parsedContent = fileParserManager.parse(file);

			return FileParseResult.of(
				file.getOriginalFilename(),
				parsedContent,
				file.getSize()
			);
		} catch (CustomException e) {
			throw e;
		} catch (IOException e) {
			throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
		}
	}
}