package com.frontend.view;

import java.util.ResourceBundle;

public enum FxmlView {
    MAIN{
        @Override
        String getTitle() {
            return getStringFromResourceBundle("main.app.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/Main.fxml";
        }
    },
    LOGIN {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("login.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/home/login.fxml";
        }
    },
    CREATE_SHOPE {
        @Override
        String getTitle() {
            return "Registration Form";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/home/create_shope.fxml";
        }
    },

    EMPLOYEE {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("login.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/create/AddEmployee.fxml";
        }
    },
    ADDUSER {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("login.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/create/AddUser.fxml";
        }
    },
    HOME {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/home/Home.fxml";
        }
    },
    DASHBOARD {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/home/Home.fxml";
        }
    },
    PURCHASEPARTY {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/create/AddPurchaseParty.fxml";
        }
    },
    ITEM {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/create/AddItem.fxml";
        }
    },
    PURCHASEINVOICE {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/transaction/PurchaseInvoice.fxml";
        }
    },
    BANK {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/create/AddBank.fxml";
        }
    },
    ITEMSTOCK {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/report/item/ItemStock.fxml";
        }
    },
    BILLING {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/transaction/BillingFrame.fxml";
        }
    },
    CUSTOMER {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/create/AddCustomer.fxml";
        }
    },
    CUSTOMER_FORM {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/master/customer.fxml";
        }
    },
    SALEREPORT {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/report/salesreport/DailySalesReport.fxml";
        }
    },
    STUDENT {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/report/salesreport/CustomerSalesReport.fxml";
        }
    },
    NEWHOME {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/home/HomeNew.fxml";
        }
    }
    ,
    DEMO {
        @Override
        String getTitle() {
            return getStringFromResourceBundle("dashboard.title");
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/home/pymol.fxml";
        }
    },
    MASTER_MENU {
        @Override
        String getTitle() {
            return "Master Data Management";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/master/MasterMenu.fxml";
        }
    },
    ADD_CATEGORY {
        @Override
        String getTitle() {
            return "Category Management";
        }
        @Override
        public String getFxmlFile() {
            return "/fxml/master/AddCategory.fxml";
        }
    }
    ;
    abstract String getTitle();
    public abstract String getFxmlFile();
    String getStringFromResourceBundle(String key){
        return ResourceBundle.getBundle("Bundle").getString(key);
    }
}
