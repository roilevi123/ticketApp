package Appliction;

import Domain.Company.iCompanyRepository;
import Domain.OwnerManagerTree.*;

import Domain.User.IUserRepository;
import Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompanyServiceTest {

    @Mock
    private iCompanyRepository companyRepository;
    @Mock
    private IUserRepository userRepository;
    @Mock
    private iTreeOfRoleRepository treeOfRoleRepository;
    @Mock
    private TokenService tokenService;

    @InjectMocks
    private CompanyService companyService;

    private final String TOKEN = "test_token";
    private final String PASSWORD = "test_password";
    private final String USERNAME = "test_user";
    private final String COMPANY = "test_company";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createCompany_Success() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(userRepository.userExists(USERNAME)).thenReturn(true);

        String status=companyService.CreateCompany(COMPANY, TOKEN);
        assertEquals(status, "success");

        verify(companyRepository).store(COMPANY, USERNAME);
        verify(treeOfRoleRepository).storeOwner(USERNAME, COMPANY,"Administrator");
    }

    @Test
    void createCompany_InvalidToken() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        String status=companyService.CreateCompany(COMPANY, TOKEN);
        assertEquals(status, "failed");

        verifyNoInteractions(companyRepository);
        verifyNoInteractions(treeOfRoleRepository);
    }

    @Test
    void createCompany_UserNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        companyService.CreateCompany(COMPANY, TOKEN);
        String status=companyService.CreateCompany(COMPANY, TOKEN);
        assertEquals(status, "failed");

        verify(companyRepository, never()).store(anyString(), anyString());
        verify(treeOfRoleRepository, never()).storeOwner(anyString(), anyString(), anyString());
    }



}