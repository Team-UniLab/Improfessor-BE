package org.unilab.improfessorbe.domain.round.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.unilab.improfessorbe.domain.round.dto.RoundResponse;
import org.unilab.improfessorbe.domain.round.service.RoundService;
import org.unilab.improfessorbe.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rounds")
@RequiredArgsConstructor
@Tag(name = "Round", description = "회차 관리 API")
public class RoundController {

	private final RoundService roundService;

	@GetMapping("/{userId}")
	@Operation(summary = "내 회차 목록 조회", description = "사용자의 모든 회차 목록을 조회합니다 (7일 이내)")
	public ResponseEntity<ApiResponse<List<RoundResponse>>> getMyRounds(
		@PathVariable Long userId) {
		List<RoundResponse> rounds = roundService.getUserRounds(userId);
		return ResponseEntity.ok(ApiResponse.success(rounds));
	}
}