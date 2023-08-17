package com.cngzh.traceability.biz.productgrowthcycle;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cngzh.traceability.biz.productionschedule.entity.ProductionSchedule;

import java.util.*;

public class HutoolHttpClient {

    public static void main(String[] args) {
        String apiUrl = "http://172.168.10.5:5000/predict";

        // 构建请求参数
        // 请根据您的API预期的输入格式和参数进行调整
        JSONObject rowData = new JSONObject();
        ProductionSchedule  productionSchedule = new ProductionSchedule();
        // 设置属性值
//        productionSchedule.setTenantId("tenant12345");
//        productionSchedule.setCreateBy("admin");
//        productionSchedule.setCreateTime(new Date());  // 设置当前日期和时间
//        productionSchedule.setUpdateBy("updater");
//        productionSchedule.setUpdateTime(new Date());  // 设置当前日期和时间
//        productionSchedule.setId("id12345");
//        productionSchedule.setNo("NO001");
//        productionSchedule.setInput("sampleInput");
//        productionSchedule.setUsageAndDosage("sampleUsageAndDosage");
//        productionSchedule.setProductionOperation("sampleOperation");
////        productionSchedule.setOperationDescription("sampleDescription");
//        productionSchedule.setJobStartTime(new Date());  // 设置当前日期和时间
//        productionSchedule.setJobDuration("5 days");
//        productionSchedule.setJobIntroduce("sampleJobIntroduce");
//        productionSchedule.setRandomJob("yes");
//        productionSchedule.setJobBy("operator");
//        productionSchedule.setStatus(1);
//        productionSchedule.setGrowPlanId("growPlanId001");


        productionSchedule.setProductionOperation("operationZ");
        productionSchedule.setInput("inputZ");


        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(productionSchedule));
        Iterator<Map.Entry<String, Object>> iterator = jsonObject.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> next = iterator.next();
                rowData.put(StrUtil.toUnderlineCase(next.getKey()), next.getValue());
        }


        HttpResponse response = HttpRequest.post(apiUrl)
                .body(rowData.toString())
                .header("Content-Type", "application/json")
                .timeout(20000)  // 设置超时时间为20秒
                .execute();

        if (response.isOk()) {
            String result = response.body();
            System.out.println("API Response: " + result);
        } else {
            System.err.println("Failed to get a response from the API. HTTP Status: " + response.getStatus());
        }
    }
}
