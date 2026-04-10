package com.feijimiao.xianyuassistant.utils;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.util.List;
import java.util.Map;

/**
 * @author IAMLZY
 * @date 2026/4/10 20:57
 * @description
 */
@Component
public class VectorStoreUtils {

    @Autowired
    private VectorStore vectorStore;

    /**
     * 添加数据，注意：id重复会报错
     *
     * @param id
     * @param dataText
     * @param dataMap  元数据，不会参与向量，但是会入库
     * @return
     */
    public void putDataToVectorStore(String id, String dataText, Map<String, Object> dataMap) {
        Document documented = new Document(id, dataText, dataMap);
        vectorStore.add(List.of(documented));
    }


    /**
     * new Document(content, metadata) 不传 id 时内部会自动生成 UUID，构造完就能直接 getId() 取到
     *
     * @param dataText
     * @param dataMap
     * @return
     */
    public String putDataToVectorStore(String dataText, Map<String, Object> dataMap) {
        Document document = new Document(dataText, dataMap);
        vectorStore.add(List.of(document));
        return document.getId();  // 自动生成的 UUID
    }

    /**
     * 搜索数据库
     *
     * @param dataText
     * @return
     */
    public List<Document> getDataFromVectorStore(String dataText) {
        SearchRequest request = SearchRequest
                .builder()
                .query(dataText)
                .topK(5)
                .build();
        return vectorStore.similaritySearch(request);
    }

}
