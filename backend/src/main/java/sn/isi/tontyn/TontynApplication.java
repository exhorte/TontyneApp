package sn.isi.tontyn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
public class TontynApplication {
    public static void main(String[] args) {
        SpringApplication.run(TontynApplication.class, args);
    }
}
