package Appliction;

import Domain.Company.Company;
import Domain.Domains.CompanyDomain;
import Domain.OwnerManagerTree.Manager;
import Domain.OwnerManagerTree.Owner;
import Domain.OwnerManagerTree.Permission;

import java.util.List;
import java.util.Set;

public class CompanyService {
    private CompanyDomain companyDomain;
    public CompanyService(CompanyDomain companyDomain) {
        this.companyDomain = companyDomain;
    }
    public String CreateCompany(String company, String token) {
        return companyDomain.CreateCompany(company,token);
    }
    public String AppointAManager(String manager, String company, Set<Permission> permissions, String token) {
        return companyDomain.AppointAManager(manager,company,permissions,token);
    }
    public String ApproveAppointmentForManager(String token, String company) {
        return companyDomain.ApproveAppointmentForManager(token,company);
    }
    public String RejectAppointmentForManager(String token, String company) {
        return companyDomain.RejectAppointmentForManager(token,company);
    }
    public String AppointOwner(String owner, String company,String token) {
        return companyDomain.AppointOwner(owner,company,token);
    }
    public String  ApproveAppointmentForOwner(String token, String company) {
        return companyDomain.ApproveAppointmentForOwner(token,company);
    }
    public String RejectAppointmentForOwner(String token, String company) {
        return companyDomain.RejectAppointmentForOwner(token,company);
    }
    public String FireOwner(String token, String company,String owner) {
        return companyDomain.FireOwner(token,company,owner);
    }
    public String FireManager(String token, String company,String manager) {
        return companyDomain.FireManager(token,company,manager);
    }
    public String ChangeManagerPermissions(String token, String company, String managerName, Set<Permission> newPermissions) {
        return companyDomain.ChangeManagerPermissions(token,company,managerName,newPermissions);
    }
    public String freezeCompany(String company, String token) {
        return companyDomain.freezeCompany(company,token);
    }
    public String unfreezeCompany(String company, String token) {
        return companyDomain.unfreezeCompany(company,token);
    }
    public Set<Permission> GetManagerPermissions(String token, String company, String managerName) {
        return companyDomain.GetManagerPermissions(token,company,managerName);
    }
    public String GetRoleTreeString(String token, String companyName) {
        return companyDomain.GetRoleTreeString(token,companyName);

    }

}
