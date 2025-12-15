namespace ext.s4;

service S4AllocAccount {
  entity AllocationAccounts {
    key destCode : String(20);
        destName : String(80);
  }
}
