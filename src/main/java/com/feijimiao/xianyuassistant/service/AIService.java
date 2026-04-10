package com.feijimiao.xianyuassistant.service;

import org.antlr.v4.runtime.TokenStream;
import reactor.core.publisher.Flux;

/**
 * @author IAMLZY
 * @date 2026/4/10 22:26
 * @description
 */

public interface AIService {

    Flux<String> chatByRAG(String prompt);
}
