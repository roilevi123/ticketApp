package AcceptanceTests;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@TestPropertySource(properties = {
        "repository.type=MEMORY"
})
public class AdminJUnitMemoryTests extends AdminJUnitTestsBase {
}