namespace local_db;


//品目マスタ
entity Materials {
  key material     : String(20);
      materialName : String(20);
}

//社員マスタ
entity Employees {
  key employeeId   : String(20);
      employeeName : String(20);
}

//配分履歴
entity AllocationHistory {
  key ID                 : String(20);
      executedAt         : Timestamp;
      executedBy         : String(20);
      allocationType     : String(20);
      allocationDestCode : String(20);
      companyCode        : String(20);
      companyName        : String(20);
      material           : String(20);
}

//配分アカウント
entity AllocationAccounts {
  key destCode : String(20);
      destName : String(20);
}









