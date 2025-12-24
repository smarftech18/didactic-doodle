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
import com.sap.cds.ql.cqn.CqnStructuredTypeRef;   // ★追加
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
  private static final DateTimeFormatter STR14 =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  @Before(event = "READ", entity = ENTITY)
  public void beforeRead(CdsReadEventContext ctx) {

  System.out.println("=== BEFORE READ ===");
  System.out.println("target = " + ctx.getTarget().getName());
  // System.out.println("isCount = " + ctx.getCqn().isCount());
  System.out.println("where = " + ctx.getCqn().where());

    CqnSelect original = ctx.getCqn();
    if (original == null || original.where() == null) return;

    Modifier modifier = new Modifier() {

      @Override
      public CqnPredicate comparison(
          Value<?> lhs,
          CqnComparisonPredicate.Operator op,
          Value<?> rhs) {
          
        Value<?> newLhs = lhs;
        Value<?> newRhs = rhs;
          
        boolean isTarget =
            (lhs instanceof CqnElementRef ref && VIRTUAL_FIELD.equals(ref.lastSegment()))
         || (lhs instanceof CqnStructuredTypeRef ref && VIRTUAL_FIELD.equals(ref.lastSegment()));
          
        if (isTarget && rhs instanceof CqnLiteral lit && lit.value() instanceof OffsetDateTime odt) {
        
          newLhs = CQL.get(DB_FIELD);
        
          String str14 = odt.atZoneSameInstant(JST).format(STR14);
        
          // ★ ここが決定打
          newRhs = CQL.literal(str14, com.sap.cds.reflect.CdsBaseType.STRING);
        }
      
        return CQL.comparison(newLhs, op, newRhs);
      }


      // 共通の変換処理をメソッド化（ロジックは今までと同じ）
      private CqnPredicate convert(
          CqnComparisonPredicate.Operator op,
          Value<?> rhs) {

        Value<?> newLhs = CQL.get(DB_FIELD);

        if (rhs instanceof CqnLiteral lit
            && lit.value() instanceof OffsetDateTime odt) {

          String str14 = odt.atZoneSameInstant(JST).format(STR14);
          Value<?> newRhs = CQL.val(str14);

          return CQL.comparison(newLhs, op, newRhs);
        }

        return CQL.comparison(newLhs, op, rhs);
      }
    };

    CqnSelect modified = CQL.copy(original, modifier);
    System.out.println("[beforeRead] where(after)=" + modified.where());

    ctx.setCqn(modified);
  }
}
