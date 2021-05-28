package com.wayfarer.boosterupgrade.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class GsonConfiguration {
    @Primary
    @Bean
    fun regularGson(): Gson {
        return GsonBuilder()
            .setPrettyPrinting()
            .create()
    }
}

