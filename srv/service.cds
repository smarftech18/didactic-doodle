using { local_db } from '../db/schema';

service AllocationHistoryService {

  // 値ヘルプ用
  entity AllocationAccounts as projection on local_db.AllocationAccounts;
  entity Materials          as projection on local_db.Materials;
  entity Employees          as projection on local_db.Employees;

  // 画面用（配分登録履歴）
  entity AllocationHistory as projection on local_db.AllocationHistory {
    *,
    // UI検索・表示用（カレンダー向け）
    @Core.Computed
    virtual reflectionDateDT : Timestamp
  };

  // 固定値（任意）
  @cds.persistence.skip
  entity AllocationTypes {
    key code : String(10);
        text : String(60);
  }
}

annotate AllocationHistoryService.AllocationHistory with @(
  UI: {
    SelectionFields: [
      executedAt,
      executedBy,
      allocationType,
      allocationDestCode,
      companyCode,
      companyName,
      reflectionDateDT
    ],
    LineItem: [
      { Value: executedAt },
      { Value: executedBy },
      { Value: allocationType },
      { Value: allocationDestCode },
      { Value: reflectionDateDT }
    ]
  },
  // 検索項目にDatepickerを設定
   Capabilities.FilterRestrictions : {
    FilterExpressionRestrictions : [
      {
        Property : 'reflectionDateDT',
        AllowedExpressions : 'SingleRange'
      }
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

  // 配分種別（固定少数ならドロップダウン）
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

  // UI用日時
  reflectionDateDT @Common.Label: '反映日時'
                   @Common.DisplayFormat: #DateTime
                   @cds.odata.Type: 'Edm.DateTimeOffset';
};

