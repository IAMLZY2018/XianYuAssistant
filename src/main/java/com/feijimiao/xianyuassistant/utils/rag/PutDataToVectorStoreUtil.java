package com.feijimiao.xianyuassistant.utils.rag;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author IAMLZY
 * @date 2026/4/10 18:55
 * @description 网向量数据库存入数据
 */
@Component
public class PutDataToVectorStoreUtil {

    @Autowired
    public VectorStore vectorStore;


    public void putDataToVectorStore(Object object) {

    }


}
