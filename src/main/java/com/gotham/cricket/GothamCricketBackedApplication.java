package com.gotham.cricket;

import com.gotham.cricket.entity.MemberProfile;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.enums.Role;
import com.gotham.cricket.enums.UserStatus;
import com.gotham.cricket.repository.MemberProfileRepository;
import com.gotham.cricket.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class GothamCricketBackedApplication {

	public static void main(String[] args) {
		SpringApplication.run(GothamCricketBackedApplication.class, args);
	}





    @Bean
    public CommandLineRunner createAdmin(
            UserRepository userRepository,
            MemberProfileRepository memberProfileRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            User admin = userRepository.findByEmail("shiva_prem14@hotmail.com")
                    .orElseGet(() -> {
                        User newAdmin = new User();
                        newAdmin.setFirstName("shiva");
                        newAdmin.setLastName("prasad");
                        newAdmin.setFullName("shiva prasad");
                        newAdmin.setEmail("shiva_prem14@hotmail.com");
                        newAdmin.setPassword(passwordEncoder.encode("admin123"));
                        newAdmin.setRole(Role.ADMIN);
                        newAdmin.setStatus(UserStatus.APPROVED);

                        User savedAdmin = userRepository.save(newAdmin);
                        System.out.println("✅ Admin user created successfully");
                        return savedAdmin;
                    });

            if (memberProfileRepository.findByUserId(admin.getId()).isEmpty()) {
                MemberProfile profile = new MemberProfile();
                profile.setUser(admin);
                profile.setNickname("Shiva");
                profile.setPhone("");
                profile.setBattingStyle("");
                profile.setBowlingStyle("");
                profile.setPlayerType("");
                profile.setJerseyNumber(null);

                memberProfileRepository.save(profile);
                System.out.println("✅ Admin member profile created successfully");
            } else {
                System.out.println("ℹ️ Admin member profile already exists");
            }
        };
    }

}
