// package customer.omfr_allocation;

// import com.sap.cds.Result;
// import com.sap.cds.ql.Select;
// import com.sap.cds.ql.cqn.CqnSelect;

// import com.sap.cds.services.cds.CdsReadEventContext;
// import com.sap.cds.services.handler.EventHandler;
// import com.sap.cds.services.handler.annotations.On;
// import com.sap.cds.services.persistence.PersistenceService;

// import org.springframework.stereotype.Component;
// import org.springframework.beans.factory.annotation.Autowired;

// @Component
// public class AllocationHistoryGuardHandler implements EventHandler {

//   private static final String THRESHOLD_CONST_ID = "ALLOC_HIST_THRESHOLD";
//   private static final String KEY_COL = "ID"; // ← AllocationHistory の key項目名に置換

//   @Autowired
//   PersistenceService db; // ★ これでDBに直接run（再帰回避）

//   @On(event = "READ", entity = "AllocationHistoryService.AllocationHistory")
//   public void onReadAllocationHistory(CdsReadEventContext ctx) {

//     int threshold = loadThreshold();
//     if (threshold > 0) {
//       long totalCount = countTotalBySameFilter(ctx.getCqn());
//       if (totalCount > threshold) {
//         ctx.reject(String.format(
//           "検索結果が %d 件のため上限（%d件）を超えました。検索条件を絞り込んでください。",
//           totalCount, threshold
//         ));
//         return;
//       }
//     }

//     // 閾値OK → 本来のREAD（ListReportが投げたCQN）をDB実行して結果を返す
//     Result data = db.run(ctx.getCqn());
//     ctx.setResult(data);
//   }

//   private int loadThreshold() {
//     CqnSelect q = Select.from("db.ConstMaster")
//         .columns("Value1")
//         .where(w -> w.get("ConstId").eq(THRESHOLD_CONST_ID));

//     Result r = db.run(q);

//     Number n = r.first()
//         .map(row -> (Number) row.get("Value1"))
//         .orElse(null);

//     return n != null ? n.intValue() : 0;
//   }

//   private long countTotalBySameFilter(CqnSelect original) {
//     // COUNT用クエリを作る（4.4.1では count() は引数必須）
//     var builder = Select.from(original.ref())
//         .columns(c -> c.count(x -> x.get(KEY_COL)).as("cnt"));

//     // where は Optional<CqnPredicate> なので ifPresent で付与
//     original.where().ifPresent(builder::where);

//     Result r = db.run(builder);

//     Number n = r.first()
//         .map(row -> (Number) row.get("cnt"))
//         .orElse(0);

//     return n.longValue();
//   }
// }
