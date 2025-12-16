// このクラスは「配分登録履歴(AllocationHistoryService)」のREADリクエストが来たときに、
// どこからデータを返すか（ローカルDB or S/4リモート or 固定データ）を切り替えるためのイベントハンドラです。

package customer.omfr_allocation; // Javaの「所属フォルダ名（名前空間）」を宣言。クラスの重複を避けるため。

import java.time.Instant;         // 日時(タイムスタンプ)を扱うクラスを使うため
import java.util.List;            // List（配列みたいな入れ物）を使うため
import java.util.Map;             // Map（キー→値の辞書）を使うため

import org.springframework.beans.factory.ObjectProvider;          // Beanが「ある時だけ使う」ための仕組み（無い時でも落ちない）
import org.springframework.beans.factory.annotation.Qualifier;    // 同じ型のBeanが複数あるとき「名前で指定」するため
import org.springframework.beans.factory.annotation.Value;        // application.ymlの設定値をフィールドへ注入するため
import org.springframework.stereotype.Component;                  // Springに「このクラスをBeanとして管理して」と伝える

import com.sap.cds.Result;                          // CAP Javaでクエリ結果を表す型（DB/ODataの結果）
import com.sap.cds.services.cds.CdsReadEventContext;// READイベントのコンテキスト（CQNやレスポンス設定が入る）
import com.sap.cds.services.cds.CqnService;         // CQN(クエリ)を実行するためのサービス（DB/リモートどちらにもなり得る）
import com.sap.cds.services.handler.EventHandler;   // 「イベントハンドラですよ」という印
import com.sap.cds.services.handler.annotations.On; // READ/CREATEなどどのイベントに反応するかを宣言するアノテーション
import com.sap.cds.services.handler.annotations.ServiceName; // どのCAPサービスに対するハンドラかを指定
import com.sap.cds.services.persistence.PersistenceService;   // ローカルDB（H2等）に対してCQNを実行するためのサービス

@Component // Springがこのクラスを自動で見つけて、DI(依存性注入)できるようにする
@ServiceName("AllocationHistoryService") // CAPのサービス名 AllocationHistoryService のイベントをこのクラスが扱う
public class AllocationHistoryServiceHandler implements EventHandler { // CAPのイベントハンドラクラスとして定義

  @Value("${omfr.mock}") // application.yml の omfr.mock を読み込む。無ければ false
  boolean mock;                // trueなら「開発用（ローカル/固定データ）」モードにするフラグ

  // ローカルDB用（ただし環境によってはDBが存在しない/設定されていないことがある）
  // そのため ObjectProvider で「存在する時だけ使う」ようにして起動エラーを避ける
  private final ObjectProvider<PersistenceService> dbProvider;

  // S/4 リモート用（Destination等が未設定でも起動できるように ObjectProvider を使う）
  private final ObjectProvider<CqnService> s4AllocHistProvider; // 配分履歴（S/4）を読む用
  private final ObjectProvider<CqnService> s4EmpProvider;       // 従業員（S/4）を読む用
  private final ObjectProvider<CqnService> s4AcctProvider;      // 配分先（S/4）を読む用

  // コンストラクタ：Springがここに必要な部品（Bean）を入れてくれる
  public AllocationHistoryServiceHandler(
      ObjectProvider<PersistenceService> dbProvider, // ローカルDB用サービス（無い場合もある）
      @Qualifier("S4_ALLOC_HIST") 
      ObjectProvider<CqnService> s4AllocHistProvider, // 名前(S4_ALLOC_HIST)で特定のCqnServiceを指定
      @Qualifier("S4_EMPLOYEE") 
      ObjectProvider<CqnService> s4EmpProvider,         // 名前(S4_EMPLOYEE)のCqnService
      @Qualifier("S4_ALLOC_ACCOUNT") 
      ObjectProvider<CqnService> s4AcctProvider    // 名前(S4_ALLOC_ACCOUNT)のCqnService
  ) {
    this.dbProvider = dbProvider;                 // フィールドに保存（このクラス内で使えるように）
    this.s4AllocHistProvider = s4AllocHistProvider;
    this.s4EmpProvider = s4EmpProvider;
    this.s4AcctProvider = s4AcctProvider;
  }

  // 「ローカルDBで実行できるなら実行してResultを返す。できないならnullを返す」補助メソッド
  private Result tryRunLocalDb(CdsReadEventContext ctx) {
    PersistenceService db = dbProvider.getIfAvailable(); // ローカルDBのBeanが存在するなら取得。無ければ null
    if (db == null) return null;                         // 無いなら「ローカルDBでは実行できません」
    return db.run(ctx.getCqn());                         // READで要求されたCQNをローカルDBで実行
  }

  // 「指定されたリモートサービス（ObjectProvider）で実行できるなら実行してResultを返す。できないならnull」
  private Result tryRunRemote(ObjectProvider<CqnService> provider, CdsReadEventContext ctx) {
    CqnService remote = provider.getIfAvailable(); // リモート用CqnServiceがあるなら取得。無ければ null
    if (remote == null) return null;               // 無いなら「リモートでは実行できません」
    return remote.run(ctx.getCqn());               // READで要求されたCQNをリモート(S/4)に対して実行
  }

  // ---------- AllocationHistory ----------
  // 「AllocationHistoryService.AllocationHistory を READ したとき」にこのメソッドを呼ぶ
  @On(event = CqnService.EVENT_READ, entity = "AllocationHistoryService.AllocationHistory")
  public void onReadAllocationHistory(CdsReadEventContext context) {
      System.out.println("モックのBOOL値");
      System.out.println(mock);
    if (mock) { // 開発用モードの場合
      System.out.println("DEV_MODE_LOCAL_DB");
      // mock=true: まずローカルDB（CSV/H2）にデータがあるならそれを返す（UI開発が楽）
      Result local = tryRunLocalDb(context); // ローカルDBで実行を試す
      if (local != null) {                  // 実行できた（DBが存在した）なら
        context.setResult(local);           // その結果をレスポンスとしてセット
        return;                             // ここで終了
      }

      // ローカルDBが無い/使えない場合は、固定データを返す（最低限UIが動く）
      context.setResult(List.of( // List（複数行のデータ）を返す
          Map.of(               // 1行分のデータ（列名→値）
              "ID", "00000000-0000-0000-0000-000000000001",
              "executedAt", Instant.parse("2025-12-15T01:00:00Z"), // Timestampとして扱える形式（Z = UTC）
              "executedBy", "E0001",
              "allocationType", "A",
              "allocationDestCode", "D001",
              "companyCode", "1000",
              "companyName", "テスト会社A",
              "material", "MAT001"
          ),
          Map.of(
              "ID", "00000000-0000-0000-0000-000000000002",
              "executedAt", Instant.parse("2025-12-14T02:30:00Z"),
              "executedBy", "E0002",
              "allocationType", "B",
              "allocationDestCode", "D002",
              "companyCode", "2000",
              "companyName", "テスト会社B",
              "material", "MAT002"
          )
      ));
      return; // mock=true の処理はここで終了
    }

    // mock=false: 本番想定。まずはS/4のリモートサービスを優先する
    Result remote = tryRunRemote(s4AllocHistProvider, context); // S/4で実行を試す
      System.out.println("REMOTE_MODE");
      System.out.println(mock);
    if (remote != null) {              // S/4が設定されていて実行できたら
      context.setResult(remote);       // 結果を返す
      return;
    }

    // リモートが未設定/落ちてる場合、開発が止まらないようローカルDBへフォールバック
    Result local = tryRunLocalDb(context);
    if (local != null) {
      System.out.println("ODataService can not use");
      throw new IllegalStateException("S/4 OData is not available in this environment");
      // context.setResult(local);
    }

    // どちらも無いなら、どうにもならないのでエラーで止める
    throw new IllegalStateException("Neither remote service (S4_ALLOC_HIST) nor local PersistenceService is available");
  }

  // ---------- Employees ----------
  // 「Employees を READ したとき」にこのメソッドを呼ぶ
  @On(event = CqnService.EVENT_READ, entity = "AllocationHistoryService.Employees")
  public void onReadEmployees(CdsReadEventContext context) {

    if (mock) { // 開発用モード
      Result local = tryRunLocalDb(context); // ローカルDBがあるなら使う
      if (local != null) {
        context.setResult(local);
        return;
      }

      // 無いなら固定データ
      context.setResult(List.of(
          Map.of("employeeId", "E0001", "employeeName", "山田 太郎"),
          Map.of("employeeId", "E0002", "employeeName", "佐藤 花子")
      ));
      return;
    }

    // 本番想定：S/4（従業員）を優先
    Result remote = tryRunRemote(s4EmpProvider, context);
    if (remote != null) {
      context.setResult(remote);
      return;
    }

    // 失敗したらローカルDB
    Result local = tryRunLocalDb(context);
    if (local != null) {
      context.setResult(local);
      return;
    }

    throw new IllegalStateException("Neither remote service (S4_EMPLOYEE) nor local PersistenceService is available");
  }

  // ---------- AllocationAccounts ----------
  // 「AllocationAccounts を READ したとき」にこのメソッドを呼ぶ
  @On(event = CqnService.EVENT_READ, entity = "AllocationHistoryService.AllocationAccounts")
  public void onReadAccounts(CdsReadEventContext context) {

    if (mock) { // 開発用モード
      Result local = tryRunLocalDb(context);
      if (local != null) {
        context.setResult(local);
        return;
      }

      // 固定データ（配分先マスタ）
      context.setResult(List.of(
          Map.of("destCode", "D001", "destName", "配分先A"),
          Map.of("destCode", "D002", "destName", "配分先B")
      ));
      return;
    }

    // 本番想定：S/4（配分先）を優先
    Result remote = tryRunRemote(s4AcctProvider, context);
    if (remote != null) {
      context.setResult(remote);
      return;
    }

    // 失敗したらローカルDB
    Result local = tryRunLocalDb(context);
    if (local != null) {
      context.setResult(local);
      return;
    }

    throw new IllegalStateException("Neither remote service (S4_ALLOC_ACCOUNT) nor local PersistenceService is available");
  }

  // ---------- AllocationTypes（固定でOK） ----------
  // 「AllocationTypes を READ したとき」にこのメソッドを呼ぶ
  @On(event = CqnService.EVENT_READ, entity = "AllocationHistoryService.AllocationTypes")
  public void onReadAllocationTypes(CdsReadEventContext context) {
    // 種別はS/4が未完成でも動くよう、常に固定で返してOK（設計判断）
    context.setResult(List.of(
        Map.of("code", "A", "text", "種別A"),
        Map.of("code", "B", "text", "種別B")
    ));
  }
}
