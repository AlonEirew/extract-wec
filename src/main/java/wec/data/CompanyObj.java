package wec.data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Objects;

@Entity
public class CompanyObj {

    @Id
    private int companyId;
    private String companyName;
    private int empCount;
    private String tradeAs;
    private String type;
    private String revenue;

    protected CompanyObj() {
    }

    public CompanyObj(int companyId, String companyName, int empCount, String tradeAs, String type, String revenue) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.empCount = empCount;
        this.tradeAs = tradeAs;
        this.type = type;
        this.revenue = revenue;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public int getEmpCount() {
        return empCount;
    }

    public void setEmpCount(int empCount) {
        this.empCount = empCount;
    }

    public String getTradeAs() {
        return tradeAs;
    }

    public void setTradeAs(String tradeAs) {
        this.tradeAs = tradeAs;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRevenue() {
        return revenue;
    }

    public void setRevenue(String revenue) {
        this.revenue = revenue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompanyObj that = (CompanyObj) o;
        return empCount == that.empCount &&
                Objects.equals(companyName, that.companyName) &&
                Objects.equals(tradeAs, that.tradeAs) &&
                Objects.equals(type, that.type) &&
                Objects.equals(revenue, that.revenue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyName, empCount, tradeAs, type, revenue);
    }

    @Override
    public String toString() {
        return "CompanyObj{" +
                "companyId=" + companyId +
                ", companyName='" + companyName + '\'' +
                ", empCount=" + empCount +
                ", tradeAs='" + tradeAs + '\'' +
                ", type='" + type + '\'' +
                ", revenue='" + revenue + '\'' +
                '}';
    }
}
