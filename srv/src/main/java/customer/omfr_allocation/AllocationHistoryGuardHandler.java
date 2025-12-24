package customer.omfr_allocation;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Value;
import com.sap.cds.ql.cqn.CqnComparisonPredicate;
import com.sap.cds.ql.cqn.CqnElementRef;
import com.sap.cds.ql.cqn.CqnLiteral;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnStructuredTypeRef;
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

    CqnSelect original = ctx.getCqn();
    if (original == null || original.where() == null) return;

    System.out.println("=== BEFORE READ ===");
    System.out.println("target = " + ctx.getTarget().getName());
    System.out.println("where(before) = " + original.where());

    Modifier modifier = new Modifier() {

      @Override
      public CqnPredicate comparison(
          Value<?> lhs,
          CqnComparisonPredicate.Operator op,
          Value<?> rhs) {

        // 1) lhs が reflectionDateDT を指している比較だけ対象
        boolean isTarget =
            (lhs instanceof CqnElementRef  e && VIRTUAL_FIELD.equals(e.lastSegment()))
         || (lhs instanceof CqnStructuredTypeRef s && VIRTUAL_FIELD.equals(s.lastSegment()));

        if (!isTarget) {
          return CQL.comparison(lhs, op, rhs);
        }

        // 2) lhs を DB列に差し替え
        Value<?> newLhs = CQL.get(DB_FIELD);

        // 3) rhs が timestamp literal なら JST→String(14) に変換して “文字列リテラル” で返す
        if (rhs instanceof CqnLiteral<?> lit) {
          Object v = lit.value();

          OffsetDateTime odt = null;

          // rhs.value() が OffsetDateTime のケース
          if (v instanceof OffsetDateTime x) {
            odt = x;
          }
          // rhs.value() が String(例: 2025-12-24T15:00:00Z) のケース
          else if (v instanceof String s) {
            // ODataのtimestamp literalがここに来る想定
            odt = OffsetDateTime.parse(s);
          }

          if (odt != null) {
            String str14 = odt.atZoneSameInstant(JST).format(STR14);
            Value<?> newRhs = CQL.val(str14); // ← これで timestamp literal を消して “文字列” にする
            return CQL.comparison(newLhs, op, newRhs);
          }
        }

        // rhs が想定外なら（壊さないため）lhsだけ置き換えて返す
        return CQL.comparison(newLhs, op, rhs);
      }
    };

    CqnSelect modified = CQL.copy(original, modifier);
    System.out.println("where(after)  = " + modified.where());

    ctx.setCqn(modified);
  }
}