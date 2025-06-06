package com.org.kline.page;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;


public class PageContext {

    private static final ThreadLocal<Page> localPage = new ThreadLocal<Page>();
    private static final ThreadLocal<Boolean> isPage = new ThreadLocal<Boolean>();


    public static void init(HttpServletRequest reqeust){
        localPage.set(null);
        String rowStr = reqeust.getParameter("rows");//每页显示条数
        String pageStr = reqeust.getParameter("page");//请求页数
        if (StrUtil.isNotEmpty(rowStr) && StrUtil.isNotEmpty(rowStr)) {
            localPage.set(new Page(Integer.valueOf(pageStr).intValue(),Integer.valueOf(rowStr).intValue()));
            isPage.set(true);
        }
        else {
            isPage.set(false);
            localPage.set(new Page());
        }

    }

    public static void setPage(int rowNum,int rowSize) {
        localPage.set(new Page(rowNum,rowSize));
    }

    public static void setPage(Page page){
        localPage.set(page);
    }

    public static ThreadLocal<Page> getLocalPage(){
        return localPage;
    }
    public static void setPagination(boolean flag) {
        isPage.set(flag);
    }

    public static ThreadLocal<Boolean> getIsPage(){
        return isPage;
    }

    public static Page getPage(){
        Page p = localPage.get();
        localPage.remove();
        return p;
    }
}
