package AcceptanceTests;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@TestPropertySource(properties = {"repository.type=DB"})
public class ReseveTicketDbTests extends ReseveTicketTestsBase {
}