package wurdal.api;

import org.springframework.web.bind.annotation.*;

@RestController
public class WurdalController {

    @PostMapping("/guess")
    public String guess() {
        return "hello";
    }
}