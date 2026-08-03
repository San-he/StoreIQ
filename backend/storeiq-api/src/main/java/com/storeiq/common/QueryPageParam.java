package com.storeiq.common;

import java.util.HashMap;

/**
 * 分页查询参数封装
 */
public class QueryPageParam {

    private int pageNum = 1;
    private int pageSize = 10;
    private HashMap<String, Object> param;

    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = pageNum; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public HashMap<String, Object> getParam() { return param; }
    public void setParam(HashMap<String, Object> param) { this.param = param; }
}
