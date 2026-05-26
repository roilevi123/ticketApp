package Infastructure;

import com.ticketing.ticketapp.Domain.Company.Company;
import com.ticketing.ticketapp.Infastructure.CompanyRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompanyRepositoryImplTest {

    private CompanyRepositoryImpl repo;

    @BeforeEach
    void setUp() {
        repo = new CompanyRepositoryImpl();
    }

    @Test
    void store_AndGetCompany_ReturnsCompany() {
        repo.store("TechCorp", "founder1");
        Company c = repo.getCompany("TechCorp");
        assertNotNull(c);
        assertEquals("TechCorp", c.getCompanyName());
    }

    @Test
    void getCompanyDescription_ReturnsString() {
        repo.store("TechCorp", "founder1");
        String desc = repo.getCompanyDescription("TechCorp");
        assertNotNull(desc);
        assertTrue(desc.contains("TechCorp"));
    }

    @Test
    void getCompanyFounder_ReturnsFounderId() {
        repo.store("TechCorp", "founder1");
        assertEquals("founder1", repo.getCompanyFounder("TechCorp"));
    }

    @Test
    void isCompanyActive_TrueByDefault() {
        repo.store("TechCorp", "founder1");
        assertTrue(repo.isCompanyActive("TechCorp"));
    }

    @Test
    void getActiveCompanies_ReturnsOnlyActive() {
        repo.store("Active1", "f1");
        repo.store("Active2", "f2");
        repo.store("Frozen", "f3");

        Company frozen = repo.getCompany("Frozen");
        frozen.freezeCompany("f3");
        repo.save(frozen);

        List<Company> active = repo.getActiveCompanies();
        assertEquals(2, active.size());
        assertTrue(active.stream().noneMatch(c -> c.getCompanyName().equals("Frozen")));
    }

    @Test
    void save_UpdatesCompany() {
        repo.store("TechCorp", "founder1");
        Company c = repo.getCompany("TechCorp");
        c.setRating(4.5);
        repo.save(c);
        assertEquals(4.5, repo.getCompany("TechCorp").getRating(), 0.001);
    }

    @Test
    void save_NotFound_Throws() {
        Company phantom = new Company("Ghost", "founder1");
        assertThrows(RuntimeException.class, () -> repo.save(phantom));
    }

    @Test
    void deleteCompany_RemovesCompany() {
        repo.store("TechCorp", "founder1");
        repo.deleteCompany("TechCorp");
        assertNull(repo.getCompany("TechCorp"));
    }

    @Test
    void deleteAllCompany_ClearsAll() {
        repo.store("C1", "f1");
        repo.store("C2", "f2");
        repo.deleteAllCompany();
        assertNull(repo.getCompany("C1"));
        assertNull(repo.getCompany("C2"));
    }
}
