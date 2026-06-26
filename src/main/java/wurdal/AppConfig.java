package wurdal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import wurdal.game.GameEngine;

@Configuration
public class AppConfig {

    @Bean
    public GameEngine gameEngine() {
        return new GameEngine();
    }
}
