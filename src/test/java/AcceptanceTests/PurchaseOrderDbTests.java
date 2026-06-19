package AcceptanceTests;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@TestPropertySource(properties = {
        "repository.type=DB",
        "spring.datasource.url=jdbc:postgresql://136.115.146.17:5432/ticketapp_test_db",
        "spring.datasource.username=ticketapp_user",
        "spring.datasource.password=BGUticketapp1!",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=update"
})
public class PurchaseOrderDbTests extends PurchaseOrderTestsBase {
}