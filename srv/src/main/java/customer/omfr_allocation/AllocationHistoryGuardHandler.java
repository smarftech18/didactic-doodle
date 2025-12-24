package customer.omfr_allocation;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Value;
import com.sap.cds.ql.cqn.CqnComparisonPredicate;
import com.sap.cds.ql.cqn.CqnLiteral;
import com.sap.cds.ql.cqn.CqnElementRef;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.Modifier;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.ServiceName;

@Component
@ServiceName("AllocationHistoryService")
public class AllocationHistoryGuardHandler implements EventHandler {

  private static final String ENTITY = "AllocationHistoryService.AllocationHistory";
  private static final String VIRTUAL_FIELD = "reflectionDateDT";
  private static final String DB_FIELD = "reflectionDate";

  private static final ZoneId JST = ZoneId.of("Asia/Tokyo");
  private static final DateTimeFormatter STR14 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  @Before(event = "READ", entity = ENTITY)
  public void beforeRead(CdsReadEventContext ctx) {
    System.out.println("[beforeRead] called. entity=" + ctx.getTarget().getName());
    System.out.println("[beforeRead] where=" + ctx.getCqn().where());
    CqnSelect original = ctx.getCqn();
    if (original == null || original.where() == null) return;

    Modifier modifier = new Modifier() {

      @Override
      public CqnPredicate comparison(
          Value<?> lhs,
          CqnComparisonPredicate.Operator op,
          Value<?> rhs) {

        // lhs が「項目参照」で、その名前が reflectionDateDT の比較だけを対象にする
        
        if (lhs instanceof CqnElementRef ref && VIRTUAL_FIELD.equals(ref.lastSegment())) {
          // DB実体の列（reflectionDate）に置換
          Value<?> newLhs = CQL.get(DB_FIELD);

          // rhs（比較値）が OffsetDateTime なら String(14) に変換して置換
          if (rhs instanceof CqnLiteral lit && lit.value() instanceof OffsetDateTime odt) {
            String str14 = odt.atZoneSameInstant(JST).format(STR14);
            Value<?> newRhs = CQL.val(str14);
            return CQL.comparison(newLhs, op, newRhs);
          }
        }

        // それ以外はそのまま（壊さない）
        return CQL.comparison(lhs, op, rhs);
      }
    };

    ctx.setCqn(CQL.copy(original, modifier));
  }
}
