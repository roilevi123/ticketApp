package AcceptanceTests;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@TestPropertySource(properties = {
        "repository.type=DB",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class InformationEventsDbTests extends InformationEventsTestsBase {
}