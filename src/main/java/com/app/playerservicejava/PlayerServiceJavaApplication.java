/*package com.app.playerservicejava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PlayerServiceJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlayerServiceJavaApplication.class, args);
    }

}*/

package com.app.playerservicejava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;  // ← ADD THIS IMPORT

@SpringBootApplication
@ComponentScan(basePackages = {"com.app.playerservicejava"})  // ← ADD THIS LINE
public class PlayerServiceJavaApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlayerServiceJavaApplication.class, args);
    }
}
