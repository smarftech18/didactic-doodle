package customer.omfr_allocation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

@Component
@ServiceName("AllocationHistoryService")
public class AllocationHistoryServiceHandler implements EventHandler {

  @Value("${omfr.mock:false}")
  boolean mock;

  private final ObjectProvider<CqnService> s4AllocHistProvider;
  private final ObjectProvider<CqnService> s4EmpProvider;
  private final ObjectProvider<CqnService> s4AcctProvider;

  public AllocationHistoryServiceHandler(
      @Qualifier("S4_ALLOC_HIST") ObjectProvider<CqnService> s4AllocHistProvider,
      @Qualifier("S4_EMPLOYEE") ObjectProvider<CqnService> s4EmpProvider,
      @Qualifier("S4_ALLOC_ACCOUNT") ObjectProvider<CqnService> s4AcctProvider
  ) {
    this.s4AllocHistProvider = s4AllocHistProvider;
    this.s4EmpProvider = s4EmpProvider;
    this.s4AcctProvider = s4AcctProvider;
  }

  @On(event = CqnService.EVENT_READ, entity = "AllocationHistoryService.AllocationHistory")
  public void onReadAllocationHistory(CdsReadEventContext context) {
    if (mock) {
      context.setResult(List.of(
        Map.of(
          "ID", "00000000-0000-0000-0000-000000000001",
          "executedAt", Instant.parse("2025-12-15T01:00:00Z"),
          "executedBy", "E0001",
          "allocationType", "A",
          "allocationDestCode", "D001",
          "material", "MAT001"
        ),
        Map.of(
          "ID", "00000000-0000-0000-0000-000000000002",
          "executedAt", Instant.parse("2025-12-14T02:30:00Z"),
          "executedBy", "E0002",
          "allocationType", "B",
          "allocationDestCode", "D002",
          "material", "MAT002"
        )
      ));
      return;
    }

    CqnService s4AllocHist = s4AllocHistProvider.getIfAvailable();
    if (s4AllocHist == null) throw new IllegalStateException("Remote service S4_ALLOC_HIST is not configured");
    Result result = s4AllocHist.run(context.getCqn());
    context.setResult(result);
  }

  @On(event = CqnService.EVENT_READ, entity = "AllocationHistoryService.Employees")
  public void onReadEmployees(CdsReadEventContext context) {
    if (mock) {
      context.setResult(List.of(
        Map.of("employeeId", "E0001", "employeeName", "山田 太郎"),
        Map.of("employeeId", "E0002", "employeeName", "佐藤 花子")
      ));
      return;
    }

    CqnService s4Emp = s4EmpProvider.getIfAvailable();
    if (s4Emp == null) throw new IllegalStateException("Remote service S4_EMPLOYEE is not configured");
    context.setResult(s4Emp.run(context.getCqn()));
  }

  @On(event = CqnService.EVENT_READ, entity = "AllocationHistoryService.AllocationAccounts")
  public void onReadAccounts(CdsReadEventContext context) {
    if (mock) {
      context.setResult(List.of(
        Map.of("destCode", "D001", "destName", "配分先A"),
        Map.of("destCode", "D002", "destName", "配分先B")
      ));
      return;
    }

    CqnService s4Acct = s4AcctProvider.getIfAvailable();
    if (s4Acct == null) throw new IllegalStateException("Remote service S4_ALLOC_ACCOUNT is not configured");
    context.setResult(s4Acct.run(context.getCqn()));
  }

  @On(event = CqnService.EVENT_READ, entity = "AllocationHistoryService.AllocationTypes")
  public void onReadAllocationTypes(CdsReadEventContext context) {
    // これはS/4未完成でも固定で返せる（@cds.persistence.skip のため）
    context.setResult(List.of(
      Map.of("code", "A", "text", "種別A"),
      Map.of("code", "B", "text", "種別B")
    ));
  }
}
