package hcmus.alumni.userservice.controller;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hcmus.alumni.userservice.model.UserModel;
import hcmus.alumni.userservice.repository.UserRepository;
import hcmus.alumni.userservice.utils.EmailSenderUtils;
import hcmus.alumni.userservice.utils.PasswordUtils;

@RestController
@RequestMapping("/user")
public class UserServiceController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public UserModel login(@RequestParam String email, @RequestParam String pass) {
        UserModel user = new UserModel();
        
        // Find user by email
        UserModel foundUser = userRepository.findByEmailAndPass(email,PasswordUtils.hashPassword(pass));
        if (foundUser != null) {
        	user = foundUser;
            user.setLastLogin(new Date());
            userRepository.save(user);
        }
        return user;
    }
    
    @PostMapping("/sendAuthorizeCode")
    public Boolean sendAuthorizeCode(@RequestParam String id) {
    	String email = getUserEmail(id);

        if (email == null) {
            return false; 
        }

        try {
        	EmailSenderUtils.sendEmail(email);
            return true; 
        } catch (MailException e) {
            e.printStackTrace();
            return false; 
        }
    }
    
    private String getUserEmail(String userId) {
        UserModel user = null;
        UserModel foundUser = userRepository.findUserById(userId);
        
        if (foundUser != null) {
            user = foundUser;
        }
        
        return user != null ? user.getEmail() : null;
    }

	/*
	 * @PostMapping("/verifyAuthorizeCode") public Boolean
	 * verifyAuthorizeCode(@RequestParam String authorizeCode) { UserModel user =
	 * new UserModel();
	 * 
	 * // Find user by email UserModel foundUser =
	 * userRepository.findByEmailAndPass(email,PasswordUtils.hashPassword(pass)); if
	 * (foundUser != null) { user = foundUser; user.setLastLogin(new Date());
	 * userRepository.save(user); } return false; }
	 * 
	 * @PostMapping("/signup") public UserModel signup(@RequestParam String
	 * email, @RequestParam String pass, @RequestParam String fullName,
	 * 
	 * @RequestParam String studentId, @RequestParam String beginningYear) { //
	 * Check if the user already exists if
	 * (userRepository.findByEmail(newUser.getEmail()) != null) { // User already
	 * exists return null; }
	 * 
	 * // Hash the password before saving it String hashedPassword =
	 * PasswordUtils.hashPassword(newUser.getPass());
	 * newUser.setPass(hashedPassword);
	 * 
	 * // Set the creation date newUser.setCreateAt(new Date());
	 * 
	 * // Save the new user return userRepository.save(newUser); }
	 */
}
