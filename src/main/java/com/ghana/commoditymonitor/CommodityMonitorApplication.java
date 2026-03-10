package com.ghana.commoditymonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CommodityMonitorApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommodityMonitorApplication.class, args);
	}

}
