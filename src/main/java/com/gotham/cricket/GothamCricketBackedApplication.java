package com.gotham.cricket;

import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.UserStatus;
import com.gotham.cricket.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class GothamCricketBackedApplication {

	public static void main(String[] args) {
		SpringApplication.run(GothamCricketBackedApplication.class, args);
	}





    @Bean
    public CommandLineRunner createAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {

            if (userRepository.findByEmail("shiva_prem14@hotmail.com").isEmpty()) {

                User admin = new User();
                admin.setFirstName("shiva");
                admin.setLastName("prasad");
                admin.setFullName("shiva prasad");
                admin.setEmail("shiva_prem14@hotmail.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                admin.setStatus(UserStatus.APPROVED);

                userRepository.save(admin);

                System.out.println("✅ Admin user created successfully");
            } else {
                System.out.println("ℹ️ Admin already exists");
            }
        };
    }

}
