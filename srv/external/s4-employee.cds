namespace ext.s4;

service S4Employee {
  entity Employees {
    key employeeId   : String(20);
        employeeName : String(80);
  }
}
