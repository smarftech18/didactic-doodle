namespace ext.s4;

service S4AllocHistory {
  entity AllocationHistory {
    key ID                 : String(36);

    executedAt             : Timestamp;
    executedBy             : String(20);

    allocationType         : String(10);
    allocationDestCode     : String(20);

    companyCode            : String(20);
    companyName            : String(20);
    material               : String(40);
    // ... S4配分登録履歴CDSViewの列（=表示列）を必要分だけ追加
  }
}
