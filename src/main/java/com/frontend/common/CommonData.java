package com.frontend.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class CommonData {
    // TODO: Replace with DTOs from API responses
    public static Object shopInfo; // Will be ShopInfoDto
    public static Object loginUser; // Will be LoginUserDto


    /*
    @Getter
    public static Login loginUser;
    @Getter
    public static List<String>ITEMNAMES = new ArrayList<>();
    public static List<String>customerNames = new ArrayList<>();
    @Getter
    public static ShopeInfo shopeeInfo;
    public CommonData() {
        super();
    }

    public static void setLoginUser(Login loginUser) {
        CommonData.loginUser = loginUser;
    }

    public static void setITEMNAMES(List<String> ITEMNAMES) {
        CommonData.ITEMNAMES.addAll(ITEMNAMES);
    }
    public static void setShopeeInfo(ShopeInfo shopeeInfo){
        CommonData.shopeeInfo = shopeeInfo;
    }*/
}
