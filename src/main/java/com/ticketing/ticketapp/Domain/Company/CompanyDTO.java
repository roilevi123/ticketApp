package com.ticketing.ticketapp.Domain.Company;

public record CompanyDTO(
        String companyName,
        String founderID,
        double rating
) {
    public static CompanyDTO fromEntity(Company company) {
        if (company == null) return null;
        return new CompanyDTO(
                company.getCompanyName(),
                company.getFounderID(),
                company.getRating()
        );
    }
}
