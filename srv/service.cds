// using { ext.s4 as hist } from './external/s4-alloc-history';
// using { ext.s4 as acct } from './external/s4-alloc-account';
// using { ext.s4 as mat  } from './external/s4-material';
// using { ext.s4 as emp  } from './external/s4-employee';

using { local_db } from '../db/scheam';

service AllocationHistoryService {

  // ① 外部エンティティを公開（値ヘルプ用）
  entity S4AllocationHistory as projection on local_db.AllocationHistory;
  entity AllocationAccounts  as projection on local_db.AllocationAccounts;
  entity Materials           as projection on local_db.Materials;
  entity Employees           as projection on local_db.Employees;

  // ② 画面用（あなたの言う「配分登録履歴」）
  //    ※S4配分登録履歴CDSViewの項目名＝表示列名、なので基本はprojectionでOK
  entity AllocationHistory   as projection on local_db.AllocationHistory;

  // ③ 配分種別がコードリスト（固定少数）なら、永続化なしの候補エンティティを作る（任意）
  @cds.persistence.skip
  entity AllocationTypes {
    key code : String(10);
        text : String(60);
  }
}
annotate AllocationHistoryService.AllocationHistory with @(
  UI: {
    SelectionFields: [ executedAt, executedBy, allocationType, allocationDestCode, companyCode, companyName ],
    LineItem: [
      { Value: executedAt },
      { Value: executedBy },
      { Value: allocationType },
      { Value: allocationDestCode }
      // 必要ならS4履歴の列を追加
    ]
  }
);

annotate AllocationHistoryService.AllocationHistory with {

  // 配分実施者（社員マスタ）
  executedBy @(Common.ValueList: {
    CollectionPath: 'Employees',
    Parameters: [
      { $Type: 'Common.ValueListParameterInOut',
        LocalDataProperty: executedBy,
        ValueListProperty: 'employeeId' },
      { $Type: 'Common.ValueListParameterDisplayOnly',
        ValueListProperty: 'employeeName' }
    ]
  });

  // 配分先コード（配分アカウントマスタ）
  allocationDestCode @(Common.ValueList: {
    CollectionPath: 'AllocationAccounts',
    Parameters: [
      { $Type: 'Common.ValueListParameterInOut',
        LocalDataProperty: allocationDestCode,
        ValueListProperty: 'destCode' },
      { $Type: 'Common.ValueListParameterDisplayOnly',
        ValueListProperty: 'destName' }
    ]
  });

  // 配分種別（固定少数ならドロップダウン寄せ）
  allocationType @(
    Common.ValueListWithFixedValues: true,
    Common.ValueList: {
      CollectionPath: 'AllocationTypes',
      Parameters: [
        { $Type: 'Common.ValueListParameterInOut',
          LocalDataProperty: allocationType,
          ValueListProperty: 'code' },
        { $Type: 'Common.ValueListParameterDisplayOnly',
          ValueListProperty: 'text' }
      ]
    }
  );
};
