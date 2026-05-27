package com.ticketing.ticketapp.Domain;

import com.ticketing.ticketapp.Domain.Company.Company;
import com.ticketing.ticketapp.Domain.User.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {

    // ── User ─────────────────────────────────────────────────────────────────

    @Test
    void user_ThreeArgConstructor_DefaultsEmailToEmpty() {
        User user = new User("alice", "pass", 25);
        assertEquals("alice", user.getName());
        assertEquals("pass", user.getPassword());
        assertEquals(25, user.getAge());
        assertEquals("", user.getEmail());
        assertNotNull(user.getID());
        assertEquals(0, user.getVersion());
    }

    @Test
    void user_FourArgConstructor_SetsEmail() {
        User user = new User("alice", "pass", 25, "alice@test.com");
        assertEquals("alice@test.com", user.getEmail());
    }

    @Test
    void user_FourArgConstructor_NullEmail_DefaultsToEmpty() {
        User user = new User("alice", "pass", 25, null);
        assertEquals("", user.getEmail());
    }

    @Test
    void user_CopyConstructor_CopiesAllFields() {
        User original = new User("alice", "pass", 25, "alice@test.com");
        original.setVersion(3);
        User copy = new User(original);

        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getPassword(), copy.getPassword());
        assertEquals(original.getID(), copy.getID());
        assertEquals(original.getAge(), copy.getAge());
        assertEquals(original.getEmail(), copy.getEmail());
        assertEquals(original.getVersion(), copy.getVersion());
    }

    @Test
    void user_SetName_UpdatesName() {
        User user = new User("alice", "pass", 25);
        user.setName("bob");
        assertEquals("bob", user.getName());
    }

    @Test
    void user_SetPassword_UpdatesPassword() {
        User user = new User("alice", "pass", 25);
        user.setPassword("newpass");
        assertEquals("newpass", user.getPassword());
    }

    @Test
    void user_SetEmail_UpdatesEmail() {
        User user = new User("alice", "pass", 25);
        user.setEmail("new@test.com");
        assertEquals("new@test.com", user.getEmail());
    }

    @Test
    void user_SetEmail_Null_DefaultsToEmpty() {
        User user = new User("alice", "pass", 25, "original@test.com");
        user.setEmail(null);
        assertEquals("", user.getEmail());
    }

    @Test
    void user_SetVersion_UpdatesVersion() {
        User user = new User("alice", "pass", 25);
        user.setVersion(5);
        assertEquals(5, user.getVersion());
    }

    @Test
    void user_GetUserInfo_ContainsName() {
        User user = new User("alice", "pass", 25);
        String info = user.getUserInfo();
        assertTrue(info.contains("alice"));
        assertTrue(info.contains("name="));
    }

    @Test
    void user_TwoInstances_HaveUniqueIds() {
        User u1 = new User("alice", "pass", 25);
        User u2 = new User("alice", "pass", 25);
        assertNotEquals(u1.getID(), u2.getID());
    }

    // ── Company ──────────────────────────────────────────────────────────────

    @Test
    void company_Constructor_SetsFieldsCorrectly() {
        Company company = new Company("AcmeCorp", "founder1");
        assertEquals("AcmeCorp", company.getCompanyName());
        assertEquals("founder1", company.getFounderID());
        assertTrue(company.getActive());
        assertEquals(0, company.getVersion());
        assertEquals(0.0, company.getRating());
    }

    @Test
    void company_CopyConstructor_CopiesAllFields() {
        Company original = new Company("AcmeCorp", "founder1");
        original.setRating(4.5);
        original.setVersion(2);
        Company copy = new Company(original);

        assertEquals(original.getCompanyName(), copy.getCompanyName());
        assertEquals(original.getFounderID(), copy.getFounderID());
        assertEquals(original.getActive(), copy.getActive());
        assertEquals(original.getVersion(), copy.getVersion());
        assertEquals(original.getRating(), copy.getRating());
    }

    @Test
    void company_FreezeCompany_ByFounder_DeactivatesCompany() {
        Company company = new Company("AcmeCorp", "founder1");
        company.freezeCompany("founder1");
        assertFalse(company.getActive());
    }

    @Test
    void company_FreezeCompany_ByNonFounder_ThrowsRuntimeException() {
        Company company = new Company("AcmeCorp", "founder1");
        assertThrows(RuntimeException.class, () -> company.freezeCompany("other"));
    }

    @Test
    void company_FreezeCompany_WhenAlreadyFrozen_ThrowsRuntimeException() {
        Company company = new Company("AcmeCorp", "founder1");
        company.freezeCompany("founder1");
        assertThrows(RuntimeException.class, () -> company.freezeCompany("founder1"));
    }

    @Test
    void company_UnfreezeCompany_ByFounder_ActivatesCompany() {
        Company company = new Company("AcmeCorp", "founder1");
        company.freezeCompany("founder1");
        company.unfreezeCompany("founder1");
        assertTrue(company.getActive());
    }

    @Test
    void company_UnfreezeCompany_ByNonFounder_ThrowsRuntimeException() {
        Company company = new Company("AcmeCorp", "founder1");
        company.freezeCompany("founder1");
        assertThrows(RuntimeException.class, () -> company.unfreezeCompany("other"));
    }

    @Test
    void company_UnfreezeCompany_WhenAlreadyActive_ThrowsRuntimeException() {
        Company company = new Company("AcmeCorp", "founder1");
        assertThrows(RuntimeException.class, () -> company.unfreezeCompany("founder1"));
    }

    @Test
    void company_SetCompanyName_UpdatesName() {
        Company company = new Company("AcmeCorp", "founder1");
        company.setCompanyName("NewCorp");
        assertEquals("NewCorp", company.getCompanyName());
    }

    @Test
    void company_SetActive_UpdatesActiveStatus() {
        Company company = new Company("AcmeCorp", "founder1");
        company.setActive(false);
        assertFalse(company.getActive());
        company.setActive(true);
        assertTrue(company.getActive());
    }

    @Test
    void company_SetVersion_UpdatesVersion() {
        Company company = new Company("AcmeCorp", "founder1");
        company.setVersion(3);
        assertEquals(3, company.getVersion());
    }

    @Test
    void company_SetRating_UpdatesRating() {
        Company company = new Company("AcmeCorp", "founder1");
        company.setRating(3.7);
        assertEquals(3.7, company.getRating());
    }

    @Test
    void company_ToString_ContainsCompanyInfo() {
        Company company = new Company("AcmeCorp", "founder1");
        String str = company.toString();
        assertTrue(str.contains("AcmeCorp"));
        assertTrue(str.contains("Active"));
    }

    @Test
    void company_ToString_FrozenCompany_ContainsFrozen() {
        Company company = new Company("AcmeCorp", "founder1");
        company.freezeCompany("founder1");
        String str = company.toString();
        assertTrue(str.contains("Frozen"));
    }
}
