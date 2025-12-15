package customer.omfr_allocation;


import org.springframework.beans.factory.annotation.Qualifier;
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

  private final CqnService s4AllocHist;
  private final CqnService s4Emp;
  private final CqnService s4Acct;

  public AllocationHistoryServiceHandler(
      @Qualifier("S4_ALLOC_HIST") CqnService s4AllocHist,
      @Qualifier("S4_EMPLOYEE") CqnService s4Emp,
      @Qualifier("S4_ALLOC_ACCOUNT") CqnService s4Acct
  ) {
    this.s4AllocHist = s4AllocHist;
    this.s4Emp = s4Emp;
    this.s4Acct = s4Acct;
  }

  @On(event = CqnService.EVENT_READ, entity = "AllocationHistoryService.AllocationHistory")
  public Result onReadAllocationHistory(CdsReadEventContext context) {
    return s4AllocHist.run(context.getCqn());
  }

  // 値ヘルプ（F4）は結局 READ が飛んでくるので、各マスタもS/4へ委譲する
  @On(event = CqnService.EVENT_READ, entity = "AllocationHistoryService.Employees")
  public Result onReadEmployees(CdsReadEventContext context) {
    return s4Emp.run(context.getCqn());
  }

  @On(event = CqnService.EVENT_READ, entity = "AllocationHistoryService.AllocationAccounts")
  public Result onReadAccounts(CdsReadEventContext context) {
    return s4Acct.run(context.getCqn());
  }
}
