package hcmus.alumni.authservice.controller;

import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hcmus.alumni.authservice.config.CustomUserDetails;
import hcmus.alumni.authservice.model.UserModel;
import hcmus.alumni.authservice.repository.EmailActivationCodeRepository;
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

	private UserUtils userUtils = UserUtils.getInstance();
	private EmailSenderUtils emailSenderUtils = EmailSenderUtils.getInstance();

	@PostMapping("/login")
	public ResponseEntity<String> login(@RequestParam String email, @RequestParam String pass) {
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, pass));
        if (authenticate.isAuthenticated()) {
        	userRepository.setLastLogin(email, new Date());
        	CustomUserDetails cud = (CustomUserDetails) authenticate.getPrincipal();
        	return ResponseEntity.status(HttpStatus.OK).body(jwtUtils.generateToken(cud.getUser()));
        } else {
        	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email hoặc mật khẩu không đúng");
        }
		
	}

	@PostMapping("/signup")
	public ResponseEntity<String> signup(@RequestParam String email, @RequestParam String pass) {

		UserModel existingUser = userRepository.findByEmail(email);
		if (existingUser != null) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
		}

		UserModel newUser = new UserModel();

		// Generate UUID
		UUID uuid = UUID.randomUUID();
		String uuidString = uuid.toString();

		newUser.setId(uuidString);
		newUser.setEmail(email);

		newUser.setPass(passwordEncoder.encode(pass));

		newUser.setRoleId("8ea1665e-74b4-43ac-a966-bf10e938da42");
		newUser.setSexId("8ea1665e-74b4-43ac-a966-bf10e938da43");
		newUser.setStatusId("8ea1665e-74b4-43ac-a966-bf10e938da45");
		newUser.setEmailPrivacy(UserModel.Privacy.PUBLIC);
		newUser.setPhonePrivacy(UserModel.Privacy.PUBLIC);
		newUser.setSexPrivacy(UserModel.Privacy.PUBLIC);
		newUser.setDobPrivacy(UserModel.Privacy.PUBLIC);

		userRepository.save(newUser);

		return ResponseEntity.status(HttpStatus.CREATED).body("Signup successfully");
	}

	@PostMapping("/send-authorize-code")
	public ResponseEntity<String> sendAuthorizeCode(@RequestParam String email) {
		if (email == null || email.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is required");
		}

		try {
			emailSenderUtils.sendEmail(emailActivationCodeRepository, email);
			return ResponseEntity.status(HttpStatus.CREATED).body("Send activatino code successfully");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Send Authorize code failed: " + e.getMessage());
		}
	}

	@PostMapping("/verify-authorize-code")
	public ResponseEntity<String> verifyAuthorizeCode(@RequestParam String email, @RequestParam String activationCode) {
		if (email == null || email.isEmpty() || activationCode == null || activationCode.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email and activation code are required");
		}

		try {
			boolean isValid = userUtils.checkActivationCode(emailActivationCodeRepository, email, activationCode);
			if (isValid) {
				return ResponseEntity.status(HttpStatus.OK).body("Activate successfully");
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Activation code is invalid or expired");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Send Authorize code failed: " + e.getMessage());
		}
	}
}
