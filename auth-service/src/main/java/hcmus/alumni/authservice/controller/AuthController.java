package hcmus.alumni.authservice.controller;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hcmus.alumni.authservice.config.CustomUserDetails;
import hcmus.alumni.authservice.dto.PermissionNameOnly;
import hcmus.alumni.authservice.dto.ResetPasswordRequestDto;
import hcmus.alumni.authservice.exception.AppException;
import hcmus.alumni.authservice.model.PasswordHistoryModel;
import hcmus.alumni.authservice.model.RoleModel;
import hcmus.alumni.authservice.model.UserModel;
import hcmus.alumni.authservice.repository.EmailActivationCodeRepository;
import hcmus.alumni.authservice.repository.PasswordHistoryRepository;
import hcmus.alumni.authservice.repository.PermissionRepository;
import hcmus.alumni.authservice.repository.UserRepository;
import hcmus.alumni.authservice.utils.EmailSenderUtils;
import hcmus.alumni.authservice.utils.JwtUtils;
import hcmus.alumni.authservice.utils.UserUtils;

@RestController
@RequestMapping("/auth")
public class AuthController {

	@Autowired
	private JwtUtils jwtUtils;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private EmailActivationCodeRepository emailActivationCodeRepository;
	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private PasswordHistoryRepository passwordHistoryRepository;
	@Autowired
	private PermissionRepository permissionRepository;

	private UserUtils userUtils = UserUtils.getInstance();
	private EmailSenderUtils emailSenderUtils = EmailSenderUtils.getInstance();

	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestParam String email, @RequestParam String pass) {
		try {
			Authentication authenticate = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(email, pass));
			if (authenticate.isAuthenticated()) {
				UserModel user = userRepository.findByEmail(email);

				PasswordHistoryModel currentPasswordHistory = passwordHistoryRepository.findByUserId(user.getId());

				if (currentPasswordHistory != null && currentPasswordHistory.isAutoGenerated()) {
					return ResponseEntity.status(HttpStatus.OK)
							.body(Collections.singletonMap("forcePasswordChange", true));
				}

				userRepository.setLastLogin(email, new Date());
				CustomUserDetails cud = (CustomUserDetails) authenticate.getPrincipal();

				Set<RoleModel> roles = cud.getUser().getRoles();
				List<Integer> roleIds = roles.stream().map(RoleModel::getId).collect(Collectors.toList());

				List<PermissionNameOnly> permissionNamesOnly = permissionRepository
						.findPermissionNamesByRoleIds(roleIds);
				List<String> permissionNames = permissionNamesOnly.stream().map(PermissionNameOnly::getName).distinct()
						.collect(Collectors.toList());

				Map<String, Object> response = new HashMap<>();
				response.put("jwt", jwtUtils.generateToken(cud.getUser()));
				response.put("permissions", permissionNames);

				return ResponseEntity.status(HttpStatus.OK).body(response);
			} else {
				throw new AppException(10100, "Xác thực không thành công", HttpStatus.UNAUTHORIZED);
			}
		} catch (BadCredentialsException e) {
			throw new AppException(10100, "Email hoặc mật khẩu không hợp lệ", HttpStatus.UNAUTHORIZED);
		} catch (Exception e) {
			throw new AppException(10102, "Lỗi đăng nhập", HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@PostMapping("/signup")
	public ResponseEntity<String> signup(@RequestParam String email, @RequestParam String pass) {
		UserModel newUser = new UserModel(email, passwordEncoder.encode(pass));

		try {
			userRepository.save(newUser);
		} catch (IllegalArgumentException e) {
			throw new AppException(10200, "Email hoặc mật khẩu không hợp lệ", HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AppException(10201, "Email đã tồn tại", HttpStatus.CONFLICT);
		}
		return ResponseEntity.status(HttpStatus.OK).body("");
	}

	@PostMapping("/send-authorize-code")
	public ResponseEntity<String> sendAuthorizeCode(@RequestParam String email) {
		UserModel existingUser = userRepository.findByEmail(email);
		if (existingUser != null) {
			throw new AppException(10300, "Email đã tồn tại", HttpStatus.CONFLICT);
		}

		if (email == null || email.isEmpty()) {
			throw new AppException(10301, "Vui lòng cung cấp email", HttpStatus.BAD_REQUEST);
		}

		try {
			emailSenderUtils.sendEmail(emailActivationCodeRepository, email);
			return ResponseEntity.status(HttpStatus.OK).body("");
		} catch (Exception e) {
			e.printStackTrace();
			throw new AppException(10302, "Gửi mã xác thực thất bại", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/verify-authorize-code")
	public ResponseEntity<String> verifyAuthorizeCode(@RequestParam String email, @RequestParam String activationCode) {
		if (email == null || email.isEmpty() || activationCode == null || activationCode.isEmpty()) {
			throw new AppException(10400, "Vui lòng cung cấp email và mã xác thực", HttpStatus.BAD_REQUEST);
		}

		try {
			boolean isValid = userUtils.checkActivationCode(emailActivationCodeRepository, email, activationCode);
			if (isValid) {
				return ResponseEntity.status(HttpStatus.OK).body("");
			} else {
				throw new AppException(10401, "Mã xác thực không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new AppException(10402, "Xác minh mã xác thực thất bại", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/reset-password")
	public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequestDto req) {
		UserModel user = userRepository.findByEmail(req.getEmail());
		if (user == null) {
			throw new AppException(10500, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND);
		}

		if (!passwordEncoder.matches(req.getOldPassword(), user.getPass())) {
			throw new AppException(10501, "Mật khẩu cũ không chính xác", HttpStatus.BAD_REQUEST);
		}

		user.setPass(passwordEncoder.encode(req.getNewPassword()));
		user.setUpdateAt(new Date());

		PasswordHistoryModel currentPasswordHistory = passwordHistoryRepository.findByUserId(user.getId());

		if (currentPasswordHistory != null) {
			currentPasswordHistory.setPassword(user.getPass());
			currentPasswordHistory.setAutoGenerated(false);
			passwordHistoryRepository.save(currentPasswordHistory);
		} else {
			PasswordHistoryModel passwordHistory = new PasswordHistoryModel(user.getId(), user.getPass(), false,
					new Date());
			passwordHistoryRepository.save(passwordHistory);
		}

		userRepository.save(user);

		return ResponseEntity.status(HttpStatus.OK).body("");
	}

}