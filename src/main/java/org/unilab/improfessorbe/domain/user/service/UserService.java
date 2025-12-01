package org.unilab.improfessorbe.domain.user.service;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unilab.improfessorbe.domain.user.domain.User;
import org.unilab.improfessorbe.domain.user.dto.request.EmailVerificationResponse;
import org.unilab.improfessorbe.domain.user.dto.request.UserLoginRequest;
import org.unilab.improfessorbe.domain.user.dto.request.UserRegisterRequest;
import org.unilab.improfessorbe.domain.user.dto.request.UserUpdateRequest;
import org.unilab.improfessorbe.domain.user.dto.response.UserLoginResponse;
import org.unilab.improfessorbe.domain.user.dto.response.UserResponse;
import org.unilab.improfessorbe.domain.user.infrastructure.repository.UserRepository;
import org.unilab.improfessorbe.global.exception.CustomException;
import org.unilab.improfessorbe.global.exception.ErrorCode;
import org.unilab.improfessorbe.global.security.jwt.JwtToken;
import org.unilab.improfessorbe.global.security.jwt.JwtTokenProvider;
import org.unilab.improfessorbe.global.util.RedisUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

	private final EmailService emailService;
	private final UserRepository userRepository;
	private final RedisUtil redisUtil;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthenticationManager authenticationManager;
	private final UserRecommendationService userRecommendationService;

	private final Long EXPIRATION = 10 * 60L;
	private final Long REFRESH_TOKEN_EXPIRE_SECONDS = 7 * 24 * 60 * 60L;

	public void sendVerificationEmail(String email) {
		validateDuplicateEmail(email);

		String title = "나는 교수다 서비스 회원가입 인증 메일";
		String code = generateRandomCode();
		String text = "인증번호: " + code;

		redisUtil.setDataExpire(email, code, EXPIRATION);

		try {
			emailService.sendEmail(email, title, text);
		} catch (Exception e) {
			log.error("Error: {}", e);
			throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
		}
	}

	public EmailVerificationResponse verifyEmail(String email, String code) {
		if (redisUtil.existData(email)) {
			String result = redisUtil.getData(email);
			if (result.equals(code)) {
				return EmailVerificationResponse.builder().verified(true).message("인증 성공하였습니다.").build();
			} else {
				return EmailVerificationResponse.builder()
					.verified(false)
					.message(result)
					.message("인증번호가 일치하지 않습니다")
					.build();
			}
		} else {
			return EmailVerificationResponse.builder().verified(false).message("인증번호가 만료되었습니다. 다시 시도해주세요.").build();
		}
	}

	@Transactional
	public void register(UserRegisterRequest userRegisterRequest) {
		validateDuplicateNickname(userRegisterRequest.getNickname());

		String encodedPassword = passwordEncoder.encode(userRegisterRequest.getPassword());
		User user = UserRegisterRequest.toEntity(userRegisterRequest, encodedPassword);
		User savedUser = userRepository.save(user);

		if (userRegisterRequest.getRecommendNickname() != null &&
			!userRegisterRequest.getRecommendNickname().trim().isEmpty()) {
			userRecommendationService.processRecommendation(savedUser.getUserId(),
				userRegisterRequest.getRecommendNickname());
		}
	}

	@Transactional
	public UserLoginResponse login(UserLoginRequest userLoginRequest) {
		User user = userRepository.findByEmailAndDeletedAtIsNull(userLoginRequest.getEmail())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		UsernamePasswordAuthenticationToken authenticationToken =
			new UsernamePasswordAuthenticationToken(user.getUserId(), userLoginRequest.getPassword());

		try {
			Authentication authentication = authenticationManager.authenticate(authenticationToken);
			JwtToken jwtToken = jwtTokenProvider.generateToken(authentication);
			redisUtil.setDataExpire(authentication.getName(), jwtToken.getRefreshToken(), REFRESH_TOKEN_EXPIRE_SECONDS);

			return UserLoginResponse.of(jwtToken);
		} catch (BadCredentialsException e) {
			log.error("login error: not valid password");
			throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
		} catch (Exception e) {
			log.error("login error");
			throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
		}
	}

	@Transactional
	public void logout(String accessToken) {
		if (accessToken == null)
			throw new CustomException(ErrorCode.INVALID_TOKEN);

		jwtTokenProvider.validateToken(accessToken);

		Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
		String name = authentication.getName();

		if (redisUtil.existData(name)) {
			redisUtil.deleteData(name);
		} else {
			log.warn("logout: not exist refeshtoken");
		}

		Long remainingExpirationMillis = jwtTokenProvider.getExpiration(accessToken);
		if (remainingExpirationMillis > 0) {
			redisUtil.setDataExpire(accessToken, "logout", remainingExpirationMillis / 1000);
		}

		SecurityContextHolder.clearContext();
	}

	@Transactional
	public UserLoginResponse refreshToken(String refreshToken) {
		if (refreshToken == null)
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		JwtToken newJwtToken = jwtTokenProvider.refreshToken(refreshToken);
		return UserLoginResponse.of(newJwtToken);
	}

	@Transactional
	public void updateUser(UserUpdateRequest userUpdateRequest) {
		User user = userRepository.findByUserIdAndDeletedAtIsNull(userUpdateRequest.getId())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		//String encodedPassword = passwordEncoder.encode(userUpdateRequest.getPassword());

		user.updateUser(
			//encodedPassword,
			userUpdateRequest.getNickname(),
			userUpdateRequest.getUniversity(),
			userUpdateRequest.getMajor()
		);

		if (userUpdateRequest.getRecommendNickname() != null &&
			!userUpdateRequest.getRecommendNickname().trim().isEmpty()) {
			userRecommendationService.processRecommendation(user.getUserId(), userUpdateRequest.getRecommendNickname());
		}
	}

	@Transactional(readOnly = true)
	public UserResponse getUser(Long userId) {
		User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		return UserResponse.of(user);
	}

	@Transactional
	public void deleteUser(Long userId) {
		User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		user.markAsDeleted();
	}

	@Transactional
	public void decrementFreeCount(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		if (user.getFreeCount() <= 0) {
			throw new CustomException(ErrorCode.INSUFFICIENT_FREE_COUNT);
		}

		user.decrementFreeCount();
		userRepository.save(user);
	}

	private void validateDuplicateEmail(String email) {
		Optional<User> user = userRepository.findByEmailAndDeletedAtIsNull(email);
		if (user.isPresent()) {
			throw new CustomException(ErrorCode.EMAIL_DUPLICATION);
		}
	}

	private void validateDuplicateNickname(String nickname) {
		Optional<User> user = userRepository.findByNicknameAndDeletedAtIsNull(nickname);
		if (user.isPresent()) {
			throw new CustomException(ErrorCode.NICKNAME_DUPLICATION);
		}
	}

	public boolean checkFreeCount(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		return user.getFreeCount() > 0;
	}

	private String generateRandomCode() {
		final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final String NUMBERS = "0123456789";
		SecureRandom random = new java.security.SecureRandom();
		List<Character> chars = new java.util.ArrayList<>();

		for (int i = 0; i < 3; i++) {
			chars.add(LETTERS.charAt(random.nextInt(LETTERS.length())));
		}
		for (int i = 0; i < 3; i++) {
			chars.add(NUMBERS.charAt(random.nextInt(NUMBERS.length())));
		}

		Collections.shuffle(chars, random);

		StringBuilder sb = new StringBuilder();
		for (char c : chars) {
			sb.append(c);
		}

		return sb.toString();
	}
}
