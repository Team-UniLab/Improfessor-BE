package org.unilab.improfessorbe.domain.problem.parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer; // PDF를 이미지로 렌더링하기 위해 추가
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.unilab.improfessorbe.global.exception.CustomException;
import org.unilab.improfessorbe.global.exception.ErrorCode;

import net.sourceforge.tess4j.Tesseract; // OCR 엔진
import net.sourceforge.tess4j.TesseractException;

@Component
public class PdfFileParser implements FileParser { // 클래스 이름 변경 권장 (기존과 구분)

	private static final int MIN_TEXT_LENGTH_THRESHOLD = 100;
	// private static final int OCR_DPI = 150; // 300 → 150으로 낮춤

	private final TextPreprocessor textPreprocessor;
	// Tesseract 인스턴스를 필드로 설정 (싱글턴으로 관리)
	private final Tesseract tesseract;

	// 생성자에서 Tesseract 설정 (Dependency Injection으로 관리할 수도 있습니다)
	public PdfFileParser(TextPreprocessor textPreprocessor, @Value("${tesseract.datapath}") String tesseractDataPath) {
		this.textPreprocessor = textPreprocessor;
		this.tesseract = new Tesseract();

		// OCR 엔진 경로 및 언어 설정
		this.tesseract.setDatapath(tesseractDataPath);
		this.tesseract.setLanguage("kor+eng");
		this.tesseract.setPageSegMode(1); // Automatic page segmentation with OSD
		this.tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only (빠름)
	}

	@Override
	public boolean supports(String extension) {
		return "pdf".equalsIgnoreCase(extension);
	}

	@Override
	public String parse(MultipartFile file) throws IOException {
		try (PDDocument document = PDDocument.load(file.getInputStream())) {

			// 1. **PDFBox (텍스트 레이어) 추출을 먼저 시도 (빠른 경로)**
			PDFTextStripper stripper = new PDFTextStripper();
			String rawText = stripper.getText(document).trim();

			// 텍스트가 충분히 추출되었다면 OCR 불필요
			if (rawText.length() > MIN_TEXT_LENGTH_THRESHOLD) {
				return textPreprocessor.preprocess(rawText);
			}else{
				throw new CustomException(ErrorCode.NO_EXTRACTABLE_TEXT);
			}

			/*// 2. **텍스트 추출 실패 시 OCR 시도 (이미지 기반 문서 처리)**
			System.out.println("Insufficient text extracted via PDFBox. Attempting OCR...");
			String ocrText = performOcr(document);
			return textPreprocessor.preprocess(ocrText);*/

		}catch (CustomException e) {
			throw e;
		} catch (IOException e) {
			throw new IOException("PDF 파일을 읽을 수 없습니다: " + e.getMessage(), e);
		} catch (Exception e) {
			throw new IOException("PDF 처리 중 예상치 못한 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	/*// PDF 페이지를 이미지로 렌더링하고 OCR을 수행하는 내부 메서드
	private String performOcr(PDDocument document) throws IOException, TesseractException {
		PDFRenderer pdfRenderer = new PDFRenderer(document);
		StringBuilder ocrText = new StringBuilder();
		File tempFile = null;

		for (int i = 0; i < document.getNumberOfPages(); i++) {
			// DPI 설정이 OCR 정확도에 중요합니다 (300 DPI 권장)
			BufferedImage bim = pdfRenderer.renderImageWithDPI(i, OCR_DPI);

			try {
				// 임시 파일 생성 및 이미지 저장
				tempFile = File.createTempFile("pdf-page-" + i + "-", ".png");
				ImageIO.write(bim, "png", tempFile);

				// Tess4J를 사용하여 이미지에서 텍스트 추출 (OCR)
				String pageText = tesseract.doOCR(tempFile);
				ocrText.append(pageText).append("\n\n");

			} finally {
				// 임시 파일 정리
				if (tempFile != null) {
					Files.deleteIfExists(tempFile.toPath());
				}
			}
		}
		return ocrText.toString();
	}*/
}