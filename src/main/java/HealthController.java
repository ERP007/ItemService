import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/items")
@RestController
public class HealthController {
    @GetMapping("/health")
    String health() {
        return "item-service ok";
    }
}
